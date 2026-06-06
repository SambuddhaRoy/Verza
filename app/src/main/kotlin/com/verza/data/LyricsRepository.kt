package com.verza.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Free, no-auth lyrics provider — LRCLIB. Returns both plain text and (when available) an
 * LRC-format synced body so the UI can scroll line-by-line.
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Per-track result cache so re-opening Lyrics (or a prefetch arriving first) is instant rather
    // than re-hitting the network. Keyed by track, not duration — the lyrics for a title+artist are
    // the same regardless of the exact reported length, and this lets a prefetch and the screen
    // share one entry. Negative results are cached too (as a failure) to avoid re-querying misses.
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Result<LyricsResult>>()

    private fun keyOf(title: String, artist: String) =
        "${title.trim().lowercase()}|${artist.trim().lowercase()}"

    suspend fun fetch(title: String, artist: String, durationMs: Long): Result<LyricsResult> {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("blank title"))
        val key = keyOf(title, artist)
        cache[key]?.let { return it }

        val result = runCatching {
            val url = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
                .addQueryParameter("artist_name", artist)
                .addQueryParameter("track_name", title)
                .apply { if (durationMs > 0) addQueryParameter("duration", (durationMs / 1000).toString()) }
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Verza (https://github.com/Sambu/Verza)")
                .get()
                .build()
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    json.decodeFromString<LyricsResult>(resp.body!!.string())
                }
            }
        }
        cache[key] = result
        return result
    }

    /** Warms the cache for a track in the background (best-effort) so opening Lyrics is instant. */
    suspend fun prefetch(title: String, artist: String, durationMs: Long) {
        if (title.isBlank() || cache.containsKey(keyOf(title, artist))) return
        runCatching { fetch(title, artist, durationMs) }
    }
}
