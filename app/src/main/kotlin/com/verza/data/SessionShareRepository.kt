package com.verza.data

import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide hand-off for an incoming `verza://session/...` deep link. The Activity posts the raw
 * link the moment it arrives (cold start or onNewIntent); the playback owner observes, loads it, and
 * clears it. Decouples intent handling (Activity) from queue loading (ViewModel) without DI plumbing.
 */
object SessionInbox {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()
    fun post(link: String) { _pending.value = link }
    fun consume() { _pending.value = null }
}

/**
 * Process-wide hand-off for a YouTube song shared *into* Verza (Share → Verza, or opening a youtu.be
 * link). The Activity extracts the 11-char video id and posts it; the playback owner picks it up,
 * plays it, and clears it — same decoupling as [SessionInbox].
 */
object SharedSongInbox {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()
    fun post(videoId: String) { _pending.value = videoId }
    fun consume() { _pending.value = null }

    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

    /**
     * Pulls a YouTube video id out of arbitrary shared text (a pasted/shared link, possibly wrapped
     * in other words like "Check this out: <url>"). Handles youtu.be/<id>, watch?v=<id>, /shorts/<id>,
     * /live/<id>, /embed/<id> across youtube.com / m.youtube.com / music.youtube.com. Returns null if
     * no plausible id is found.
     */
    fun extractVideoId(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val urls = Regex("""https?://[^\s]+""").findAll(text).map { it.value }.toMutableList()
        if (urls.isEmpty()) urls += text.trim()   // the share might be a bare URL with no scheme prefix
        for (raw in urls) {
            val uri = runCatching { android.net.Uri.parse(raw) }.getOrNull() ?: continue
            val host = uri.host?.lowercase().orEmpty()
            val id = when {
                "youtu.be" in host -> uri.lastPathSegment
                "youtube.com" in host || "youtube-nocookie.com" in host -> {
                    uri.getQueryParameter("v") ?: run {
                        val segs = uri.pathSegments ?: emptyList()
                        val i = segs.indexOfFirst { it == "shorts" || it == "live" || it == "embed" || it == "v" }
                        if (i >= 0 && i + 1 < segs.size) segs[i + 1] else null
                    }
                }
                else -> null
            }
            if (id != null && VIDEO_ID.matches(id)) return id
        }
        return null
    }
}

// ── Shareable listening session ──────────────────────────────────────────────────
// A "local listening room" without a server: a snapshot of the current queue + position encoded
// into a compact verza:// link. A friend opening it picks up the exact same set at the same spot —
// asynchronous "listen along". Only streamable (YouTube) tracks travel; on-device files are dropped
// because their content:// ids are meaningless on another phone. No personal data, no sign-in cookie.

@Serializable
data class SharedSession(
    val v: Int = 1,
    val index: Int = 0,
    val positionMs: Long = 0,
    val tracks: List<SharedTrack> = emptyList(),
)

@Serializable
data class SharedTrack(
    val id: String,
    val title: String = "",
    val artist: String = "",
    val art: String? = null,
)

@Singleton
class SessionShareRepository @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Encodes [session] to a `verza://session/<payload>` link. The payload is gzipped JSON in
     * URL-safe base64, so a 50-track set stays a couple of kilobytes — small enough to paste into a
     * chat. Returns null if there's nothing streamable to share.
     */
    fun encodeLink(session: SharedSession): String? {
        val streamable = session.tracks.filter { isStreamable(it.id) }
        if (streamable.isEmpty()) return null
        // Re-base the current index onto the filtered list so it still points at the right track.
        val keptIds = streamable.map { it.id }.toHashSet()
        val newIndex = session.tracks.take(session.index + 1).count { it.id in keptIds }
            .minus(1).coerceIn(0, streamable.lastIndex)
        val trimmed = session.copy(tracks = streamable.take(MAX_TRACKS), index = newIndex.coerceAtMost(MAX_TRACKS - 1))
        val bytes = gzip(json.encodeToString(trimmed).toByteArray(Charsets.UTF_8))
        val payload = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return "$SCHEME_PREFIX$payload"
    }

    /**
     * Parses a `verza://session/<payload>` link (or a bare payload) back into a [SharedSession].
     *
     * Hardened against hostile links (the intent filter is exported, so anyone can craft one):
     *  - the compressed payload is length-capped, and decompression is bounded so a gzip bomb can't
     *    exhaust memory;
     *  - every track id is re-validated to a plausible YouTube video id, dropping anything that
     *    looks like a `content://` / `file://` URI or a path — otherwise a crafted id could make the
     *    player open a *local* file on the recipient's device;
     *  - the track list is re-capped regardless of what the payload claims.
     */
    fun decodeLink(linkOrPayload: String): SharedSession? = runCatching {
        val payload = linkOrPayload.trim()
            .removePrefix(SCHEME_PREFIX)
            .substringAfterLast('/')          // tolerate verza://session/<payload> and variants
            .substringBefore('?')
        if (payload.length > MAX_PAYLOAD_CHARS) return@runCatching null
        val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val text = gunzip(bytes, MAX_DECOMPRESSED_BYTES)?.toString(Charsets.UTF_8) ?: return@runCatching null
        val parsed = json.decodeFromString<SharedSession>(text)
        val safeTracks = parsed.tracks.filter { isValidVideoId(it.id) }.take(MAX_TRACKS)
        if (safeTracks.isEmpty()) return@runCatching null
        parsed.copy(
            tracks = safeTracks,
            index = parsed.index.coerceIn(0, safeTracks.lastIndex),
            positionMs = parsed.positionMs.coerceAtLeast(0L),
        )
    }.getOrNull()

    private fun isStreamable(id: String): Boolean = isValidVideoId(id)

    /** A real YouTube video id is 11 URL-safe chars; this also rejects URIs, paths, and whitespace. */
    private fun isValidVideoId(id: String): Boolean = VIDEO_ID.matches(id)

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    /** Decompresses up to [maxBytes]; returns null if the stream would exceed that (gzip-bomb guard). */
    private fun gunzip(data: ByteArray, maxBytes: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        GZIPInputStream(data.inputStream()).use { input ->
            var total = 0
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > maxBytes) return null
                out.write(buf, 0, n)
            }
        }
        return out.toByteArray()
    }

    companion object {
        const val SCHEME_PREFIX = "verza://session/"
        private const val MAX_TRACKS = 50
        private const val MAX_PAYLOAD_CHARS = 32 * 1024      // ~32 KB of base64 before we refuse it
        private const val MAX_DECOMPRESSED_BYTES = 256 * 1024 // hard ceiling on inflated JSON
        private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
    }
}
