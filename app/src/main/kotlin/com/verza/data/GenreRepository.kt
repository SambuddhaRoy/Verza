package com.verza.data

import android.content.Context
import com.verza.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An artist's genres and tags, from MusicBrainz.
 *
 * YouTube gives a title and an artist name and never a genre. MusicBrainz has a curated genre list
 * per artist (a real vocabulary, not free text) plus free-text tags, which is where mood words like
 * "melancholic" live. Same source and approach as the desktop app's Your Sound.
 *
 * Two requests per artist at MusicBrainz's one-a-second limit is slow, so every answer is cached on
 * disk for good (an artist's genres do not change week to week). An artist MusicBrainz does not know
 * is cached as empty; a failed request is not, so it is tried again next time.
 */
@Singleton
class GenreRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val client: OkHttpClient,
) {
    @Serializable
    data class Entry(val genres: List<Pair<String, Int>> = emptyList(), val tags: List<Pair<String, Int>> = emptyList())

    data class ArtistTags(val genres: List<Taste.Tag>, val tags: List<Taste.Tag>)

    private val file = File(context.filesDir, "artist-tags.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var cache: MutableMap<String, Entry>? = null
    private var lastRequestAt = 0L

    /** Null when MusicBrainz could not be reached; empty lists when it simply has nothing. */
    suspend fun tagsFor(artist: String): ArtistTags? = lock.withLock {
        val key = artist.trim().lowercase()
        if (key.isEmpty()) return@withLock ArtistTags(emptyList(), emptyList())
        val known = loadCache()
        val entry = known[key] ?: lookup(key)?.also {
            known[key] = it
            save(known)
        }
        entry?.let { e ->
            ArtistTags(e.genres.map { Taste.Tag(it.first, it.second) }, e.tags.map { Taste.Tag(it.first, it.second) })
        }
    }

    private suspend fun lookup(name: String): Entry? = withContext(Dispatchers.IO) {
        try {
            val search = get(
                "https://musicbrainz.org/ws/2/artist".toHttpUrl().newBuilder()
                    .addQueryParameter("query", "artist:\"${name.replace("\"", "")}\"")
                    .addQueryParameter("fmt", "json")
                    .addQueryParameter("limit", "5")
                    .build()
                    .toString(),
            ) ?: return@withContext null
            val candidates = (search["artists"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
            // The exact name first, and only a confident match otherwise: a wrong artist's genres are
            // worse than none, because they would put the listener's favourites in the wrong mix.
            val match = candidates.firstOrNull { it.string("name")?.equals(name, ignoreCase = true) == true }
                ?: candidates.firstOrNull { (it["score"]?.jsonPrimitive?.intOrNull ?: 0) >= 95 }
                ?: return@withContext Entry()
            val id = match.string("id") ?: return@withContext Entry()
            val detail = get("https://musicbrainz.org/ws/2/artist/$id?inc=genres+tags&fmt=json")
                ?: return@withContext null
            Entry(genres = detail.counts("genres"), tags = detail.counts("tags"))
        } catch (_: IOException) {
            null
        }
    }

    /** One request, spaced at least [SPACING_MS] after the last. Null on any failure worth retrying. */
    private suspend fun get(url: String): JsonObject? {
        val wait = lastRequestAt + SPACING_MS - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        lastRequestAt = System.currentTimeMillis()
        val request = Request.Builder()
            .url(url)
            // MusicBrainz asks every client to identify itself and blocks anonymous ones.
            .header("User-Agent", "Verza/${BuildConfig.VERSION_NAME} ( https://github.com/SambuddhaRoy/Verza )")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            json.parseToJsonElement(response.body!!.string()) as? JsonObject
        }
    }

    private fun JsonObject.string(name: String) = this[name]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.counts(name: String): List<Pair<String, Int>> =
        (this[name] as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .mapNotNull { o ->
                val tag = o.string("name")?.lowercase() ?: return@mapNotNull null
                val count = o["count"]?.jsonPrimitive?.intOrNull ?: 0
                (tag to count).takeIf { count > 0 }
            }
            .sortedByDescending { it.second }

    private fun loadCache(): MutableMap<String, Entry> = cache ?: runCatching {
        json.decodeFromString<Map<String, Entry>>(file.readText()).toMutableMap()
    }.getOrElse { mutableMapOf() }.also { cache = it }

    private fun save(map: Map<String, Entry>) {
        runCatching { file.writeText(json.encodeToString(map)) }
    }

    private companion object {
        /** MusicBrainz allows about one request a second per client. */
        const val SPACING_MS = 1_100L
    }
}
