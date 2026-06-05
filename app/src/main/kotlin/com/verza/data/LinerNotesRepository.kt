package com.verza.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** Editorial "liner notes" for a track — the context streaming apps stripped away. */
data class LinerNotes(
    val album: String? = null,
    val year: String? = null,
    val genre: String? = null,
    val blurb: String? = null,
) {
    val hasAny: Boolean get() = album != null || year != null || genre != null || blurb != null
}

/**
 * Builds liner notes from free, no-auth sources: iTunes Search (album / year / genre — same
 * endpoint the artwork already uses) and a short Wikipedia summary of the artist. Cached per
 * (artist, title) for the process lifetime. No identifiers are sent.
 */
@Singleton
class LinerNotesRepository @Inject constructor(
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, LinerNotes?>()

    suspend fun fetch(title: String, artist: String): LinerNotes? {
        val key = "${artist.trim().lowercase()}|${title.trim().lowercase()}"
        if (key == "|") return null
        mutex.withLock { if (cache.containsKey(key)) return cache[key] }

        val notes = withContext(Dispatchers.IO) {
            val (album, year, genre) = fetchItunes(title, artist)
            val blurb = fetchWikiSummary(artist)
            LinerNotes(album, year, genre, blurb).takeIf { it.hasAny }
        }
        mutex.withLock { cache[key] = notes }
        return notes
    }

    private fun fetchItunes(title: String, artist: String): Triple<String?, String?, String?> = runCatching {
        val term = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        if (term.isBlank()) return@runCatching Triple(null, null, null)
        val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", "1")
            .build()
        client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
            if (!resp.isSuccessful) return@runCatching Triple(null, null, null)
            val body = json.parseToJsonElement(resp.body!!.string()) as? JsonObject
            val first = (body?.get("results") as? JsonArray)?.firstOrNull() as? JsonObject
                ?: return@runCatching Triple(null, null, null)
            Triple(
                first["collectionName"]?.jsonPrimitive?.contentOrNull,
                first["releaseDate"]?.jsonPrimitive?.contentOrNull?.take(4),
                first["primaryGenreName"]?.jsonPrimitive?.contentOrNull,
            )
        }
    }.getOrDefault(Triple(null, null, null))

    private fun fetchWikiSummary(artist: String): String? {
        if (artist.isBlank()) return null
        return runCatching {
            val slug = URLEncoder.encode(artist.trim().replace(' ', '_'), "UTF-8")
            val req = Request.Builder()
                .url("https://en.wikipedia.org/api/rest_v1/page/summary/$slug")
                .header("User-Agent", "Verza/1.0 (open-source music player)")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val obj = json.parseToJsonElement(resp.body!!.string()) as? JsonObject
                if (obj?.get("type")?.jsonPrimitive?.contentOrNull == "disambiguation") return@runCatching null
                obj?.get("extract")?.jsonPrimitive?.contentOrNull?.takeIf { it.length > 20 }
            }
        }.getOrNull()
    }
}
