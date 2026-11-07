package com.verza.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

/** One line of synced lyrics: when it starts, and its words. */
data class LyricLine(val timeMs: Long, val text: String)

/** A song's lyrics: synced lines when there are some, else the plain text, else none. */
sealed interface Lyrics {
    data class Synced(val lines: List<LyricLine>) : Lyrics
    data class Plain(val text: String) : Lyrics
    data object None : Lyrics
}

@Serializable
private data class LrcLib(val plainLyrics: String? = null, val syncedLyrics: String? = null)

/**
 * Lyrics from LRCLIB, by title, artist and length (it matches the length to within a couple of
 * seconds). Only definite answers are remembered: a timeout isn't "this song has no lyrics", and
 * caching it once made a song lyric-less for good after a moment without signal.
 */
class LyricsSource(private val http: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }
    private val known = ConcurrentHashMap<String, Lyrics>()

    suspend fun get(song: Song): Lyrics {
        val key = "${song.title.lowercase()}|${song.artist.lowercase()}"
        known[key]?.let { return it }
        val url = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
            .addQueryParameter("artist_name", song.artist)
            .addQueryParameter("track_name", song.title)
            .apply { if (song.durationMs > 0) addQueryParameter("duration", (song.durationMs / 1000).toString()) }
            .build()
        val answer = withContext(Dispatchers.IO) {
            http.newCall(Request.Builder().url(url).header("User-Agent", "Verza (https://github.com/SambuddhaRoy/Verza)").build()).execute().use { resp ->
                when {
                    resp.code == 404 -> Lyrics.None
                    !resp.isSuccessful -> throw java.io.IOException("LRCLIB answered ${resp.code}")
                    else -> json.decodeFromString(LrcLib.serializer(), resp.body!!.string()).let { r ->
                        parseLrc(r.syncedLyrics.orEmpty()).takeIf { it.isNotEmpty() }?.let { Lyrics.Synced(it) }
                            ?: r.plainLyrics?.takeIf { it.isNotBlank() }?.let { Lyrics.Plain(it) } ?: Lyrics.None
                    }
                }
            }
        }
        known[key] = answer
        return answer
    }
}

/** "[mm:ss.xx] words" lines, in time order. Lines without a time are left out. */
fun parseLrc(synced: String): List<LyricLine> {
    val line = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")
    return synced.lines().mapNotNull { raw ->
        val m = line.matchEntire(raw.trim()) ?: return@mapNotNull null
        val ms = m.groupValues[3].takeIf { it.isNotEmpty() }?.padEnd(3, '0')?.take(3)?.toLong() ?: 0
        LyricLine(m.groupValues[1].toLong() * 60_000 + m.groupValues[2].toLong() * 1000 + ms, m.groupValues[4].trim())
    }.sortedBy { it.timeMs }
}
