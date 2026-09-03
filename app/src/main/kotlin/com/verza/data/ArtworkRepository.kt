package com.verza.data

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves real cover artwork for a track via the iTunes Search API. YouTube thumbnails are often
 * just frames lifted from a music video — using a music-metadata source means the artwork shown in
 * Now Playing / MiniPlayer / Queue is the actual album cover.
 *
 * Cheap and stateless: one HTTP GET per unique `(artist|title)` pair, results cached in memory for
 * the lifetime of the process. No auth.
 */
@Singleton
class ArtworkRepository @Inject constructor(
    private val client: OkHttpClient,
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val cache: MutableMap<String, String?> = mutableMapOf()

    /**
     * A better cover for this track, or null to keep whatever the track already had.
     *
     * The returned URL is one that has been loaded successfully, not merely one iTunes named. That
     * distinction is the whole point of this function: the caller replaces a working YouTube
     * thumbnail with whatever comes back, so handing over an unverified guess meant a cover that
     * appeared for a moment and then vanished for the rest of the track, with nothing to fall back
     * to and nothing to trigger a retry.
     *
     * Verifying costs nothing after the fact. Coil caches the bytes, so the AsyncImage that shows
     * this a moment later is reading from the disk cache rather than fetching again.
     */
    suspend fun resolve(title: String, artist: String): String? {
        val key = "${artist.trim().lowercase()}|${title.trim().lowercase()}"
        if (key == "|") return null
        mutex.withLock { if (cache.containsKey(key)) return cache[key] }

        val found = runCatching { fetch(title, artist) }
        // A lookup that could not reach iTunes is not the same as iTunes having nothing, and
        // caching the two the same way meant one moment without signal denied a track its real
        // cover for the rest of the process.
        if (found.isFailure) return null

        val url = found.getOrNull()
        val usable = url?.takeIf { loads(it) }
        mutex.withLock { cache[key] = usable }
        return usable
    }

    /** Whether [url] actually produces an image. Small on purpose: this is a check, not a render. */
    private suspend fun loads(url: String): Boolean = runCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(160)
            .build()
        SingletonImageLoader.get(context).execute(request) is SuccessResult
    }.getOrDefault(false)

    private suspend fun fetch(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val term = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        if (term.isBlank()) return@withContext null
        val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", "1")
            .build()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val body = json.parseToJsonElement(resp.body!!.string()) as? JsonObject ?: return@use null
            val first = (body["results"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return@use null
            val art100 = first["artworkUrl100"]?.jsonPrimitive?.contentOrNull
            // iTunes thumbnail URLs encode size — swap to 600x600 for retina-friendly artwork.
            art100?.replace("100x100bb", "600x600bb")
        }
    }
}
