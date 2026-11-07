package com.verza.data.youtube

import com.verza.data.Album
import com.verza.data.Artist
import com.verza.data.Card
import com.verza.data.Link
import com.verza.data.Section
import com.verza.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest

/** The search filters YouTube Music's web client sends. TOP sends none: the mixed top results. */
enum class Filter(val params: String) {
    TOP(""),
    SONGS("EgWKAQIIAWoKEAoQAxAEEAkQBQ%3D%3D"),
    ALBUMS("EgWKAQIYAWoKEAoQAxAEEAkQBQ%3D%3D"),
    ARTISTS("EgWKAQIgAWoKEAoQAxAEEAkQBQ%3D%3D"),
    PLAYLISTS("EgWKAQIoAWoKEAoQAxAEEAkQBQ%3D%3D"),
}

/**
 * YouTube Music, as its web client asks it: search, pages, radio, likes and playlists. Streams come
 * from NewPipe ([Streams]), which deciphers what this can't. Signed in when [cookie] returns one.
 */
class YouTube(private val http: OkHttpClient, private val cookie: () -> String?) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private suspend fun post(endpoint: String, body: JsonObjectBuilder.() -> Unit): JsonElement = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") { put("clientName", "WEB_REMIX"); put("clientVersion", VERSION); put("hl", "en"); put("gl", "US") }
            }
            body()
        }.toString()
        val request = Request.Builder()
            .url("$BASE/$endpoint?key=$KEY&prettyPrint=false")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", VERSION)
            .header("User-Agent", AGENT)
            .header("Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .apply {
                cookie()?.takeIf { it.isNotBlank() }?.let { c ->
                    header("Cookie", c)
                    header("X-Goog-AuthUser", "0")
                    sapisidHash(c)?.let { header("Authorization", it) }
                }
            }
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("YouTube answered ${resp.code}")
            json.parseToJsonElement(resp.body!!.string())
        }
    }

    suspend fun search(query: String, filter: Filter): List<Card> =
        parseResults(post("search") { put("query", query); if (filter.params.isNotEmpty()) put("params", filter.params) })

    suspend fun searchSongs(query: String): List<Song> =
        parseSongs(post("search") { put("query", query); put("params", Filter.SONGS.params) })

    suspend fun suggestions(input: String): List<String> =
        parseSuggestions(post("music/get_search_suggestions") { put("input", input) })

    private suspend fun browse(id: String) = post("browse") { put("browseId", id) }

    /**
     * Home: YouTube Music's own home, then Explore and Charts, fetched together. Mood shelves ("Sleep
     * right now", "Feeling…") are dropped as the promotions they are.
     */
    suspend fun home(): List<Section> = coroutineScope {
        listOf("FEmusic_home", "FEmusic_explore", "FEmusic_charts")
            .map { id -> async { runCatching { parseSections(browse(id)) }.getOrDefault(emptyList()) } }
            .flatMap { it.await() }
            .filterNot { s -> PROMO.any { it.containsMatchIn(s.title) } }
            .distinctBy { it.title }
    }

    suspend fun album(id: String): Album = parseAlbum(browse(id))
    suspend fun artist(id: String): Artist = parseArtist(browse(id))

    /** The signed-in account's saved playlists, and the artists it follows. */
    suspend fun accountPlaylists(): List<Card> = parsePages(browse("FEmusic_liked_playlists"))
    suspend fun accountArtists(): List<Card> = parsePages(browse("FEmusic_library_corpus_artists"))

    /** A song's radio: YouTube's endless mix seeded from it. */
    suspend fun radio(videoId: String): List<Song> =
        parseQueue(post("next") { put("videoId", videoId); put("playlistId", "RDAMVM$videoId"); put("isAudioOnly", true) })

    /**
     * A song's artists and album, for going to their pages. A video has no album, so its song is
     * searched for and the same title by one of the same artists lends its album.
     */
    suspend fun links(videoId: String, title: String): Pair<List<Link>, Link?>? {
        val (artists, album) = parseLinks(post("next") { put("videoId", videoId); put("isAudioOnly", true) }, videoId) ?: return null
        if (album != null || artists.isEmpty()) return artists to album
        val found = runCatching {
            parseAlbumOf(post("search") { put("query", "$title ${artists.first().name}"); put("params", Filter.SONGS.params) }, title, artists.map { it.name })
        }.getOrNull()
        return artists to found
    }

    suspend fun setLiked(videoId: String, liked: Boolean) {
        post(if (liked) "like/like" else "like/removelike") { putJsonObject("target") { put("videoId", videoId) } }
    }

    /** A private playlist on the account; its id, or null. */
    suspend fun createPlaylist(title: String): String? =
        (post("playlist/create") { put("title", title); put("privacyStatus", "PRIVATE") } as? JsonObject)
            ?.get("playlistId")?.jsonPrimitive?.contentOrNull

    /** Whether YouTube accepted it: an edit that says it worked when it didn't is worse than none. */
    suspend fun addToPlaylist(playlistId: String, videoId: String): Boolean =
        (post("browse/edit_playlist") {
            put("playlistId", playlistId.removePrefix("VL"))
            putJsonArray("actions") { add(buildJsonObject { put("action", "ACTION_ADD_VIDEO"); put("addedVideoId", videoId) }) }
        } as? JsonObject)?.get("status")?.jsonPrimitive?.contentOrNull == "STATUS_SUCCEEDED"

    private companion object {
        const val BASE = "https://music.youtube.com/youtubei/v1"
        const val ORIGIN = "https://music.youtube.com"
        const val KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-KKVH2hiOg"
        const val VERSION = "1.20240101.01.00"
        const val AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        val PROMO = listOf(Regex("""\bright now\b""", RegexOption.IGNORE_CASE), Regex("""\bfeeling\b""", RegexOption.IGNORE_CASE), Regex("""\bget you started\b""", RegexOption.IGNORE_CASE))

        /** Google's cookie auth: SAPISIDHASH of the time, the SAPISID cookie and the origin. */
        fun sapisidHash(cookie: String): String? {
            val sapisid = cookie.split(";").map { it.trim() }.firstNotNullOfOrNull { pair ->
                val (k, v) = pair.split("=", limit = 2).takeIf { it.size == 2 } ?: return@firstNotNullOfOrNull null
                v.takeIf { k == "__Secure-3PAPISID" || k == "SAPISID" }
            } ?: return null
            val now = System.currentTimeMillis() / 1000
            val digest = MessageDigest.getInstance("SHA-1").digest("$now $sapisid $ORIGIN".toByteArray()).joinToString("") { "%02x".format(it) }
            return "SAPISIDHASH ${now}_$digest"
        }
    }
}
