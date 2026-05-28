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

    suspend fun fetch(title: String, artist: String, durationMs: Long): Result<LyricsResult> = runCatching {
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
}
