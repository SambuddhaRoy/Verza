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

    /** Parses a `verza://session/<payload>` link (or a bare payload) back into a [SharedSession]. */
    fun decodeLink(linkOrPayload: String): SharedSession? = runCatching {
        val payload = linkOrPayload.trim()
            .removePrefix(SCHEME_PREFIX)
            .substringAfterLast('/')          // tolerate verza://session/<payload> and variants
            .substringBefore('?')
        val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val text = gunzip(bytes).toString(Charsets.UTF_8)
        json.decodeFromString<SharedSession>(text).takeIf { it.tracks.isNotEmpty() }
    }.getOrNull()

    private fun isStreamable(id: String): Boolean =
        !id.startsWith("content://") && !id.startsWith("file://")

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray =
        GZIPInputStream(data.inputStream()).use { it.readBytes() }

    companion object {
        const val SCHEME_PREFIX = "verza://session/"
        private const val MAX_TRACKS = 50
    }
}
