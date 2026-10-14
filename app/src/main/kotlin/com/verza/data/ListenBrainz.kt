package com.verza.data

import android.content.Context
import com.verza.innertube.models.HomeItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Your listening elsewhere, Spotify included, as Verza mixes. Same source and approach as the
 * desktop app (src/main/listenbrainz.js there).
 *
 * ListenBrainz (listenbrainz.org, MetaBrainz) records what people play. Spotify connects to it
 * natively and a Last.fm history can be imported into it, and every week it publishes Weekly Jams
 * and Weekly Exploration for each user, its answer to Discover Weekly. Its API needs no key to read
 * a public profile. Each song is matched to YouTube Music by a search (cached on disk for good, so
 * the songs that recur week to week cost nothing) and plays through Verza like any other mix.
 *
 * Not Spotify directly: its API no longer gives new apps recommendations or its own playlists, and
 * its developer policy forbids pairing its data with another service's audio.
 */
@Singleton
class ListenBrainzRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val client: OkHttpClient,
    private val music: MusicRepository,
) {
    sealed interface Check {
        data class Found(val listens: Long) : Check
        data object NoSuchUser : Check
        data object Unreachable : Check
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile = File(context.filesDir, "listenbrainz-cache.json")
    private val cacheLock = Mutex()

    /**
     * Whether [user] exists, and how many recent listens they have (up to 100).
     *
     * Not /listen-count: that is a cached figure which stays at 0 for a freshly connected Spotify, so
     * a real account was refused as "no listens" while /listens already returned its plays. It also
     * answers 500 for a name whose case differs, where /listens is case-insensitive.
     */
    suspend fun check(user: String): Check = try {
        val r = get("/user/${enc(user)}/listens?count=100")
        if (r == null) Check.NoSuchUser
        else Check.Found((r.obj("payload")?.arr("listens")?.size ?: 0).toLong())
    } catch (_: IOException) {
        Check.Unreachable
    }

    /** Everything for [user] as mixes, matched to YouTube Music. Empty when unreachable. */
    suspend fun mixes(user: String): List<CuratedMix> {
        data class Set(val kind: Weekly, val pairs: List<Pair<String, String>>)
        val sets = mutableListOf<Set>()
        fun add(kind: Weekly, pairs: List<Pair<String, String>>) { if (pairs.isNotEmpty()) sets += Set(kind, pairs) }

        // What ListenBrainz makes for the user: weekly, daily and yearly lists.
        runCatching {
            val lists = get("/user/${enc(user)}/playlists/createdfor?count=100")
                ?.arr("playlists").orEmpty().mapNotNull { (it as? JsonObject)?.obj("playlist") }
            for ((kind, mbid) in pickWeekly(lists.map(::summary), System.currentTimeMillis())) {
                runCatching { add(kind, playlistPairs(mbid)) }
            }
        }
        runCatching { add(RECOMMENDED, recommendedPairs(user)) }
        runCatching {
            for ((mbid, title) in ownPlaylists(user, 2)) {
                runCatching {
                    add(Weekly("lb_pl_${mbid.take(8)}", "", title, "Your playlist on ListenBrainz"), playlistPairs(mbid))
                }
            }
        }
        for (ranges in STATS) {
            runCatching { topPairs(user, ranges).let { (kind, pairs) -> add(kind, pairs) } }
        }

        // Bounded, in priority order, so a very busy account cannot make the first build run for minutes.
        var budget = MAX_PAIRS
        val bounded = sets.map { s -> Set(s.kind, s.pairs.take(budget.coerceAtLeast(0)).also { budget -= it.size }) }

        val resolved = resolve(bounded.flatMap { it.pairs })
        val now = System.currentTimeMillis()
        val kept = mutableListOf<Pair<CuratedMix, kotlin.collections.Set<String>>>()
        for (s in bounded) {
            val items = s.pairs.mapNotNull { resolved[key(it)] }.distinctBy { it.videoId }
            if (items.size < MIN_TRACKS) continue
            val ids = items.mapNotNull { it.videoId }.toSet()
            // Mostly songs an earlier mix already has makes it a repeat of that one: a new account's
            // last month, last week and all time are the same few songs, and that is one mix.
            val repeat = kept.any { (_, other) ->
                ids.count { it in other }.toDouble() / minOf(ids.size, other.size).coerceAtLeast(1) >= OVERLAP
            }
            if (!repeat) {
                kept += CuratedMix(s.kind.id, MixKind.LISTENBRAINZ, s.kind.title, s.kind.subtitle, items, now) to ids
            }
        }
        return kept.map { it.first }
    }

    private suspend fun playlistPairs(mbid: String): List<Pair<String, String>> =
        get("/playlist/${enc(mbid)}")?.obj("playlist")?.arr("track").orEmpty().mapNotNull { t ->
            val o = t as? JsonObject ?: return@mapNotNull null
            pairOf(o.str("title"), o.str("creator"))
        }

    /**
     * Most played over the first of [ranges] that has enough songs. `month` and `week` are the
     * previous calendar month and week, which a new account does not have yet (it got no mixes at
     * all), so they fall back to `this_month` and `this_week`, titled for what they are.
     */
    private suspend fun topPairs(user: String, ranges: List<Pair<String, Weekly>>): Pair<Weekly, List<Pair<String, String>>> {
        for ((range, kind) in ranges) {
            val pairs = get("/stats/user/${enc(user)}/recordings?range=$range&count=30")
                ?.obj("payload")?.arr("recordings").orEmpty()
                .mapNotNull { r ->
                    val o = r as? JsonObject ?: return@mapNotNull null
                    pairOf(o.str("track_name"), o.str("artist_name"))
                }
            if (pairs.size >= MIN_TRACKS) return kind to pairs
        }
        return ranges.first().second to emptyList()
    }

    /** The playlists [user] made on ListenBrainz themselves, newest first: (mbid, title). */
    private suspend fun ownPlaylists(user: String, n: Int): List<Pair<String, String>> =
        get("/user/${enc(user)}/playlists?count=25")?.arr("playlists").orEmpty()
            .mapNotNull { (it as? JsonObject)?.obj("playlist") }
            .mapNotNull { p ->
                val id = p.str("identifier")?.substringAfterLast('/') ?: return@mapNotNull null
                Triple(id, p.str("title") ?: "Playlist", parseTime(p.str("date")) ?: 0L)
            }
            .sortedByDescending { it.third }.take(n).map { it.first to it.second }

    /**
     * ListenBrainz's collaborative-filtering picks: songs people with a similar history play. The
     * answer is recording ids with scores, so the names come from the metadata endpoint.
     */
    private suspend fun recommendedPairs(user: String): List<Pair<String, String>> {
        val ids = get("/cf/recommendation/user/${enc(user)}/recording?count=50")
            ?.obj("payload")?.arr("mbids").orEmpty()
            .mapNotNull { it as? JsonObject }
            .sortedByDescending { it["score"]?.jsonPrimitive?.doubleOrNull ?: 0.0 }
            .mapNotNull { it.str("recording_mbid") }.take(50)
        if (ids.isEmpty()) return emptyList()
        val meta = get("/metadata/recording/?recording_mbids=${ids.joinToString(",")}&inc=artist") ?: return emptyList()
        return ids.mapNotNull { id ->
            val v = meta.obj(id) ?: return@mapNotNull null
            pairOf(v.obj("recording")?.str("name"), v.obj("artist")?.str("name"))
        }
    }

    /** (title, artist) → the first YouTube Music song for it, misses cached too, four searches at a time. */
    private suspend fun resolve(pairs: List<Pair<String, String>>): Map<String, HomeItem> = cacheLock.withLock {
        val cache = loadCache()
        val todo = pairs.distinctBy(::key).filter { key(it) !in cache }
        val gate = Semaphore(4)
        coroutineScope {
            todo.map { p ->
                async {
                    gate.withPermit {
                        // A failed search (offline) is left out of the cache so it is tried again.
                        val found = music.searchSongs("${p.first} ${p.second}").getOrNull() ?: return@withPermit null
                        val hit = found.firstOrNull { it.id.isNotBlank() }
                        key(p) to hit?.let { HomeItem(title = it.title, subtitle = it.artist, thumbnailUrl = it.thumbnailUrl, videoId = it.id) }
                    }
                }
            }.awaitAll().filterNotNull().forEach { (k, v) -> cache[k] = v }
        }
        if (todo.isNotEmpty()) save(cache)
        cache.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
    }

    private fun loadCache(): MutableMap<String, HomeItem?> = runCatching {
        json.decodeFromString<Map<String, HomeItem?>>(cacheFile.readText()).toMutableMap()
    }.getOrElse { mutableMapOf() }

    private fun save(cache: Map<String, HomeItem?>) {
        runCatching { cacheFile.writeText(json.encodeToString(cache)) }
    }

    /** The parsed body, or null for a 404 (no such user) or 204 (no stats yet). */
    private suspend fun get(path: String): JsonObject? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(API + path).header("User-Agent", UA).build()
        client.newCall(req).execute().use { res ->
            if (res.code == 404 || res.code == 204) return@withContext null
            if (!res.isSuccessful) throw IOException("ListenBrainz ${res.code} for $path")
            json.parseToJsonElement(res.body?.string().orEmpty()) as? JsonObject
        }
    }

    /** One JSPF playlist reduced to what [pickWeekly] needs. */
    private fun summary(p: JsonObject): Summary {
        val meta = p.obj("extension")?.obj(JSPF)?.obj("additional_metadata")
        return Summary(
            mbid = p.str("identifier").orEmpty().substringAfterLast('/'),
            patch = meta?.obj("algorithm_metadata")?.str("source_patch").orEmpty(),
            date = parseTime(p.str("date")) ?: 0L,
            expires = parseTime(meta?.str("expires_at")) ?: Long.MAX_VALUE,
        )
    }

    data class Weekly(val id: String, val patch: String, val title: String, val subtitle: String)
    data class Summary(val mbid: String, val patch: String, val date: Long, val expires: Long)

    companion object {
        private const val API = "https://api.listenbrainz.org/1"
        private const val UA = "Verza/1 (https://github.com/SambuddhaRoy/Verza)"
        private const val JSPF = "https://musicbrainz.org/doc/jspf#playlist"

        val WEEKLY = listOf(
            Weekly("lb_exploration", "weekly-exploration", "Weekly Exploration", "New to you, from what you play everywhere"),
            Weekly("lb_jams", "weekly-jams", "Weekly Jams", "Your favourites from everywhere you listen"),
            Weekly("lb_daily", "daily-jams", "Daily Jams", "Fresh every day, from what you have been playing"),
        )

        /** Yearly lists never expire and one arrives each year, so the newest year of each is shown. */
        private class Yearly(val id: String, val re: Regex, val title: (Int) -> String, val subtitle: String)
        private val YEARLY = listOf(
            Yearly("lb_discoveries", Regex("^top-discoveries-of-(\\d{4})$"), { "Top Discoveries of $it" },
                "The new music you found that year"),
            Yearly("lb_missed", Regex("^top-missed-recordings-of-(\\d{4})$"), { "Top Missed Songs of $it" },
                "Songs you would have loved and never played"),
        )

        private val RECOMMENDED = Weekly("lb_recommended", "", "Recommended for you", "What listeners with your taste are playing")
        private fun stat(id: String, title: String, subtitle: String) = Weekly(id, "", title, subtitle)
        private val STATS = listOf(
            listOf(
                "month" to stat("lb_month", "Last month on repeat", "Your most played, Spotify and everywhere else"),
                "this_month" to stat("lb_month", "This month on repeat", "Your most played, Spotify and everywhere else"),
            ),
            listOf(
                "week" to stat("lb_week", "Last week on repeat", "What you could not stop playing"),
                "this_week" to stat("lb_week", "This week on repeat", "What you could not stop playing"),
            ),
            listOf("all_time" to stat("lb_alltime", "All-time favourites", "The songs you keep coming back to")),
        )

        /** More than this share of a mix already on the shelf makes it a repeat of that one. */
        private const val OVERLAP = 0.8

        /** Searches made on the first build, most important mixes first. */
        private const val MAX_PAIRS = 320

        /** A mix needs this many songs that resolved on YouTube Music to be worth a card. */
        private const val MIN_TRACKS = 5

        /** A username ListenBrainz could have, so nothing else reaches the URL. */
        val USERNAME = Regex("^[\\w.-]{1,64}$")

        /**
         * The newest live playlist of each weekly or daily kind, and the newest year of each yearly
         * kind. ListenBrainz keeps old weeks in the list and stops making new ones for someone who
         * stops listening, so an expired one is left out rather than shown as this week's.
         */
        fun pickWeekly(lists: List<Summary>, now: Long): List<Pair<Weekly, String>> {
            val live = WEEKLY.mapNotNull { kind ->
                lists.filter { it.patch == kind.patch && it.expires > now }.maxByOrNull { it.date }?.let { kind to it.mbid }
            }
            val yearly = YEARLY.mapNotNull { kind ->
                lists.mapNotNull { p -> kind.re.matchEntire(p.patch)?.let { p to it.groupValues[1].toInt() } }
                    .maxByOrNull { it.second }
                    ?.let { (p, year) -> Weekly(kind.id, "", kind.title(year), kind.subtitle) to p.mbid }
            }
            return live + yearly
        }

        private fun key(p: Pair<String, String>) = p.first.trim().lowercase() + "\u0000" + p.second.trim().lowercase()
        private fun pairOf(title: String?, artist: String?) =
            if (title.isNullOrBlank() || artist.isNullOrBlank()) null else title to artist
        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        private fun parseTime(s: String?) = s?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        private fun JsonObject.obj(k: String) = this[k] as? JsonObject
        private fun JsonObject.arr(k: String) = this[k] as? JsonArray
        private fun JsonObject.str(k: String) = this[k]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
    }
}
