package com.verza.innertube

import com.verza.innertube.models.MusicItem
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

// ── InnerTube API constants ───────────────────────────────────────────────────

private val defaultJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

// ── Per-client request helpers ─────────────────────────────────────────────────

/** Writes the `context.client` block for [client] into the request body. */
internal fun JsonObjectBuilder.putContext(client: YouTubeClient) {
    put("context", buildJsonObject {
        put("client", buildJsonObject {
            put("clientName", client.clientName)
            put("clientVersion", client.clientVersion)
            put("hl", "en")
            put("gl", "US")
            client.osName?.let { put("osName", it) }
            client.osVersion?.let { put("osVersion", it) }
            client.deviceMake?.let { put("deviceMake", it) }
            client.deviceModel?.let { put("deviceModel", it) }
            client.androidSdkVersion?.let { put("androidSdkVersion", it) }
        })
    })
}

/** Sets the client-identifying headers + key param for [client] on a request. */
private fun HttpRequestBuilder.applyClient(client: YouTubeClient) {
    url {
        parameters.append("key", client.apiKey)
        parameters.append("prettyPrint", "false")
    }
    header(HttpHeaders.ContentType, ContentType.Application.Json)
    header("X-YouTube-Client-Name", client.clientId)
    header("X-YouTube-Client-Version", client.clientVersion)
    header(HttpHeaders.UserAgent, client.userAgent)
    header("Origin", client.origin)
    header("Referer", "${client.origin}/")

    // Authenticated session (signed-in YouTube Music account) — enables personalized
    // home feed, recommendations and library. Streaming still goes through NewPipe.
    val cookie = InnerTube.cookie
    if (!cookie.isNullOrBlank()) {
        header("Cookie", cookie)
        header("X-Goog-AuthUser", "0")
        sapisidHashHeader(cookie, client.origin)?.let { header(HttpHeaders.Authorization, it) }
    }
}

/** Builds the `SAPISIDHASH` Authorization header Google uses for cookie-based auth. */
private fun sapisidHashHeader(cookie: String, origin: String): String? {
    val sapisid = cookie.split("; ", ";").firstNotNullOfOrNull { pair ->
        val idx = pair.indexOf('=')
        if (idx <= 0) return@firstNotNullOfOrNull null
        val key = pair.substring(0, idx).trim()
        val value = pair.substring(idx + 1)
        if (key == "__Secure-3PAPISID" || key == "SAPISID") value else null
    } ?: return null

    val timestamp = System.currentTimeMillis() / 1000
    val digest = sha1Hex("$timestamp $sapisid $origin")
    return "SAPISIDHASH ${timestamp}_$digest"
}

private fun sha1Hex(input: String): String =
    java.security.MessageDigest.getInstance("SHA-1")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }

// ── HTTP client ──────────────────────────────────────────────────────────────

object InnerTube {

    /** Current account cookie string ("NAME=VALUE; …") or null when signed out. */
    @Volatile
    var cookie: String? = null

    /** Preferred audio quality, mirrored from settings; read by the stream resolver. */
    @Volatile
    var audioQuality: AudioQuality = AudioQuality.HIGH

    val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(defaultJson)
        }
        install(ContentEncoding) {
            gzip()
            deflate()
        }
        install(Logging) {
            level = LogLevel.NONE // set to ALL for debugging
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    suspend fun search(
        query: String,
        params: String = SearchFilter.SONGS.params,
        continuation: String? = null,
    ): JsonElement {
        val c = YouTubeClient.WEB_REMIX
        return client.post("${c.apiBaseUrl}/search") {
            applyClient(c)
            setBody(buildJsonObject {
                putContext(c)
                put("query", query)
                put("params", params)
                continuation?.let { put("continuation", it) }
            }.toString())
        }.bodyAsText().let { defaultJson.parseToJsonElement(it) }
    }

    suspend fun browse(browseId: String, params: String? = null): JsonElement {
        val c = YouTubeClient.WEB_REMIX
        return client.post("${c.apiBaseUrl}/browse") {
            applyClient(c)
            setBody(buildJsonObject {
                putContext(c)
                put("browseId", browseId)
                params?.let { put("params", it) }
            }.toString())
        }.bodyAsText().let { defaultJson.parseToJsonElement(it) }
    }

    suspend fun next(videoId: String, playlistId: String? = null): JsonElement {
        val c = YouTubeClient.WEB_REMIX
        return client.post("${c.apiBaseUrl}/next") {
            applyClient(c)
            setBody(buildJsonObject {
                putContext(c)
                put("videoId", videoId)
                playlistId?.let { put("playlistId", it) }
                put("isAudioOnly", true)
            }.toString())
        }.bodyAsText().let { defaultJson.parseToJsonElement(it) }
    }

    /** Resolves playback info. [ytClient] defaults to a no-PoToken client that returns streams. */
    suspend fun player(
        videoId: String,
        ytClient: YouTubeClient = YouTubeClient.ANDROID_VR,
        playlistId: String? = null,
    ): JsonElement {
        return client.post("${ytClient.apiBaseUrl}/player") {
            applyClient(ytClient)
            setBody(buildJsonObject {
                putContext(ytClient)
                put("videoId", videoId)
                playlistId?.let { put("playlistId", it) }
                put("racyCheckOk", true)
                put("contentCheckOk", true)
            }.toString())
        }.bodyAsText().let { defaultJson.parseToJsonElement(it) }
    }

    // FYP / home feed
    suspend fun home(): JsonElement = browse("FEmusic_home")

    // "Explore" mood/genre grid
    suspend fun explore(): JsonElement = browse("FEmusic_explore")

    // ── High-level, parsed helpers ─────────────────────────────────────────────

    /** Runs a song-filtered search and returns parsed, playable songs. */
    suspend fun searchSongs(query: String): List<MusicItem> =
        parseSearchSongs(search(query))

    /** Searches within a [filter] scope, returning songs and/or browsable collections. */
    suspend fun searchItems(
        query: String,
        filter: SearchFilter,
    ): List<com.verza.innertube.models.HomeItem> =
        parseSearchItems(search(query, params = filter.params))

    /** Builds a song "radio" — an auto-generated mix seeded from [videoId]. */
    suspend fun radio(videoId: String): List<MusicItem> =
        parseWatchPlaylist(next(videoId, playlistId = "RDAMVM$videoId"))

    /** As-you-type search suggestions for [query]. */
    suspend fun searchSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val c = YouTubeClient.WEB_REMIX
        val response = client.post("${c.apiBaseUrl}/music/get_search_suggestions") {
            applyClient(c)
            setBody(buildJsonObject {
                putContext(c)
                put("input", query)
            }.toString())
        }.bodyAsText().let { defaultJson.parseToJsonElement(it) }
        return parseSearchSuggestions(response)
    }

    /** Fetches the home feed (FEmusic_home) parsed into titled sections of cards. */
    suspend fun homeSections(): List<com.verza.innertube.models.HomeSection> =
        parseHomeSections(home())

    /**
     * A richer home feed: the personalized/anonymous home plus Explore (new releases, trending,
     * music videos) and Charts, fetched concurrently and merged with duplicate titles removed.
     */
    suspend fun homeFeed(): List<com.verza.innertube.models.HomeSection> = coroutineScope {
        val home = async { runCatching { parseHomeSections(home()) }.getOrDefault(emptyList()) }
        val explore = async { runCatching { parseHomeSections(browse("FEmusic_explore")) }.getOrDefault(emptyList()) }
        val charts = async { runCatching { parseHomeSections(browse("FEmusic_charts")) }.getOrDefault(emptyList()) }
        (home.await() + explore.await() + charts.await())
            .filter { it.items.isNotEmpty() }
            .filterNot { it.title.isPromotionalShelf() }
            .distinctBy { it.title }
    }

    // Generic mood/promo shelves YT Music injects ("Romance Right Now", "Sleep Right Now", …) and
    // sponsored-feeling rows — dropped so the home page shows curated/editorial content only.
    private val promoShelfPatterns = listOf(
        Regex(""".*\bright now\b.*""", RegexOption.IGNORE_CASE),  // mood shelves
        Regex(""".*\bfeeling\b.*""", RegexOption.IGNORE_CASE),
        Regex(""".*\bget you started\b.*""", RegexOption.IGNORE_CASE),
    )

    private fun String.isPromotionalShelf(): Boolean =
        promoShelfPatterns.any { it.matches(this) }

    /** Fetches the signed-in user's saved/liked playlists. Requires auth; empty when signed out. */
    suspend fun libraryPlaylists(): List<com.verza.innertube.models.HomeItem> =
        parseLibraryPlaylists(browse("FEmusic_liked_playlists"))

    /** Fetches the signed-in user's subscribed/followed artists. Requires auth. */
    suspend fun subscribedArtists(): List<com.verza.innertube.models.HomeItem> =
        parseLibraryPlaylists(browse("FEmusic_library_corpus_artists"))

    /** Expands an album/playlist into its playable tracks. */
    suspend fun collectionTracks(browseId: String? = null, playlistId: String? = null): List<MusicItem> {
        val id = browseId ?: playlistId?.let { if (it.startsWith("VL")) it else "VL$it" } ?: return emptyList()
        return parseCollectionTracks(browse(id))
    }

    /** Fetches an album/playlist page (header + tracks) for [collectionId] (a browseId or VL-id). */
    suspend fun collectionDetail(collectionId: String): com.verza.innertube.models.CollectionDetail =
        parseCollectionDetail(browse(collectionId))

    /** Fetches an artist page (header + shelves) for a channel [browseId] (UC…). */
    suspend fun artistPage(browseId: String): com.verza.innertube.models.ArtistDetail =
        parseArtistPage(browse(browseId))

    /** Pushes a like/un-like for [videoId] to the signed-in account. No-op when signed out. */
    suspend fun setLikeStatus(videoId: String, liked: Boolean) {
        if (cookie.isNullOrBlank()) return
        val c = YouTubeClient.WEB_REMIX
        val endpoint = if (liked) "like/like" else "like/removelike"
        client.post("${c.apiBaseUrl}/$endpoint") {
            applyClient(c)
            setBody(buildJsonObject {
                putContext(c)
                put("target", buildJsonObject { put("videoId", videoId) })
            }.toString())
        }
    }

    /**
     * Resolves the best audio stream for [videoId] via NewPipeExtractor (handles signature/n
     * deciphering that the raw player endpoint can't do anonymously). Returns null if unplayable.
     */
    suspend fun resolveAudioStream(videoId: String): StreamInfo? =
        withContext(Dispatchers.IO) {
            runCatching { NewPipeStreamResolver.resolve(videoId) }.getOrNull()
        }
}
