package com.verza.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

// Links into the app: a "listen along" session someone shared, and a YouTube song shared or opened
// here. Both arrive from outside, from anyone, so everything in them is checked before it's used.

/** What's playing, as a link: the queue, which song, and where in it. The old app's format. */
@Serializable
data class SharedSession(val v: Int = 1, val index: Int = 0, val positionMs: Long = 0, val tracks: List<SharedTrack> = emptyList())

@Serializable
data class SharedTrack(val id: String, val title: String = "", val artist: String = "", val art: String? = null)

object Sessions {
    const val PREFIX = "verza://session/"
    private const val MAX_TRACKS = 50
    private const val MAX_PAYLOAD = 32 * 1024
    private const val MAX_INFLATED = 256 * 1024
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * A link to [songs] from [index], at [positionMs]: gzipped JSON, URL-safe base64, so fifty songs
     * fit in a chat message. Songs on the phone are left out (their ids mean nothing on another
     * phone), and so is anything personal. Null when nothing is left to share.
     */
    fun link(songs: List<Song>, index: Int, positionMs: Long): String? {
        val kept = songs.withIndex().filter { isVideoId(it.value.id) }
        if (kept.isEmpty()) return null
        // The song playing, or the nearest kept one before it.
        val at = (kept.indexOfLast { it.index <= index }).coerceAtLeast(0).coerceAtMost(MAX_TRACKS - 1)
        val session = SharedSession(
            index = at,
            positionMs = if (kept.getOrNull(at)?.index == index) positionMs else 0,
            tracks = kept.take(MAX_TRACKS).map { (_, s) -> SharedTrack(s.id, s.title, s.artist, s.art) },
        )
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(json.encodeToString(SharedSession.serializer(), session).toByteArray()) }
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    /**
     * A session back from its link, or null. Capped before and after inflating (a small link can
     * inflate to gigabytes), and every id must be a YouTube id: a crafted `content://` or path would
     * otherwise have the player open a file in this app's storage.
     */
    fun read(link: String): SharedSession? = runCatching {
        val payload = link.trim().removePrefix(PREFIX).substringAfterLast('/').substringBefore('?')
        if (payload.length > MAX_PAYLOAD) return null
        val text = inflate(Base64.getUrlDecoder().decode(payload)) ?: return null
        val s = json.decodeFromString(SharedSession.serializer(), text)
        val tracks = s.tracks.filter { isVideoId(it.id) }.take(MAX_TRACKS)
        if (tracks.isEmpty()) null
        else s.copy(tracks = tracks, index = s.index.coerceIn(0, tracks.lastIndex), positionMs = s.positionMs.coerceAtLeast(0))
    }.getOrNull()

    private fun inflate(bytes: ByteArray): String? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        GZIPInputStream(bytes.inputStream()).use { input ->
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                if (out.size() + n > MAX_INFLATED) return null
                out.write(buf, 0, n)
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }
}

fun SharedTrack.toSong() = Song(id, title, artist, art = art)

private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
fun isVideoId(id: String) = VIDEO_ID.matches(id)

/**
 * The YouTube song in shared text ("Check this out: https://youtu.be/…"): youtu.be/<id>, watch?v=<id>,
 * and /shorts, /live, /embed on youtube.com, m.youtube.com and music.youtube.com. Null if there's none.
 */
fun videoIdIn(text: String?): String? {
    if (text.isNullOrBlank()) return null
    val urls = Regex("""https?://\S+""").findAll(text).map { it.value }.toList().ifEmpty { listOf(text.trim()) }
    for (raw in urls) {
        val uri = runCatching { URI(raw) }.getOrNull() ?: continue
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        val id = when {
            host == "youtu.be" -> path.firstOrNull()
            host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com") ->
                uri.rawQuery?.split('&')?.firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
                    ?: path.indexOfFirst { it in setOf("shorts", "live", "embed", "v") }.takeIf { it >= 0 }?.let { path.getOrNull(it + 1) }
            else -> null
        }
        if (id != null && isVideoId(id)) return id
    }
    return null
}
