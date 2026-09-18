package com.verza.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.verza.data.db.PlayEventDao
import com.verza.data.db.SongStat
import com.verza.di.ApplicationScope
import com.verza.innertube.SearchFilter
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.MusicItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// ── Curated mix model ────────────────────────────────────────────────────────

enum class MixKind { DAYLIST, DISCOVER, RELEASE_RADAR, GENRE, VIBE }

/**
 * A Verza-curated playlist generated *on device* from the user's own play history plus YouTube's
 * radio / artist data — no backend, no tracking. [items] are [HomeItem]s so a mix can mix playable
 * songs (Daylist, Discover) and browseable release cards (Release Radar) and reuse the standard
 * row + open routing.
 */
@Serializable
data class CuratedMix(
    val id: String,
    val kind: MixKind,
    val title: String,
    val subtitle: String,
    val items: List<HomeItem>,
    val generatedAt: Long,
) {
    /** The directly-playable songs in this mix (for a "Play all"). */
    val playableSongs: List<HomeItem> get() = items.filter { it.videoId != null }
}

private val Context.mixesStore by preferencesDataStore(name = "verza_mixes")

/**
 * Generates and caches Verza's on-device curated mixes (Daylist, Discover, Release Radar). Mixes are
 * regenerated only when stale (Daylist when the daypart turns or after a few hours; the others
 * weekly) and persisted so a cold start reuses them instead of re-hitting the network. Generation
 * runs in the background on the application scope; [mixes] updates as results land.
 */
@Singleton
class MixesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val dao: PlayEventDao,
    private val music: MusicRepository,
    private val genres: GenreRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    // v2: Daylist and Discover are seeded by Taste now. A new key regenerates them straight away
    // rather than showing the recency-seeded versions for up to a week.
    private val mixesKey = stringPreferencesKey("curated_mixes_v2")
    private val attemptKey = androidx.datastore.preferences.core.longPreferencesKey("curated_mixes_attempt_v2")

    private val _mixes = MutableStateFlow<List<CuratedMix>>(emptyList())
    val mixes: StateFlow<List<CuratedMix>> = _mixes.asStateFlow()

    init {
        scope.launch {
            _mixes.value = loadCached()
            refresh()
        }
    }

    fun getMix(id: String): CuratedMix? = _mixes.value.firstOrNull { it.id == id }

    /**
     * Regenerates any stale mix (keeping the fresh ones) and persists the result.
     *
     * A generator returns null when there is not enough listening history to build from, and
     * listOfNotNull drops it — so nothing was written to say the attempt happened, and every single
     * launch re-ran the whole generation, network calls included, for exactly the users who had the
     * least to gain from it. A missing mix is now retried on a cooldown rather than immediately.
     */
    suspend fun refresh() {
        val current = _mixes.value.associateBy { it.id }
        val sinceAttempt = System.currentTimeMillis() - lastAttemptAt()
        val retryMissing = sinceAttempt > MISSING_RETRY_MS

        suspend fun regenerate(id: String, generate: suspend () -> CuratedMix?): CuratedMix? {
            val cached = current[id]
            if (cached != null && !isStale(cached)) return cached
            // Stale is always worth redoing; absent only once the cooldown is up.
            if (cached == null && !retryMissing) return null
            return generate()
        }

        coroutineScope {
            val daylist = async { regenerate("daylist") { generateDaylist() } }
            val discover = async { regenerate("discover") { generateDiscover() } }
            val radar = async { regenerate("release_radar") { generateReleaseRadar() } }
            // Genre and vibe mixes are generated together, since they share one set of lookups, and
            // how many there are depends on the listener. Kept if a regeneration comes back empty
            // (offline, MusicBrainz down) rather than disappearing.
            val cachedTaste = current.values.filter { it.kind == MixKind.GENRE || it.kind == MixKind.VIBE }
            val taste = async {
                when {
                    cachedTaste.isNotEmpty() && cachedTaste.none(::isStale) -> cachedTaste
                    cachedTaste.isEmpty() && !retryMissing -> emptyList()
                    else -> generateTasteMixes().ifEmpty { cachedTaste }
                }
            }
            val list = listOfNotNull(daylist.await(), discover.await(), radar.await()) + taste.await()
            _mixes.value = list
            persist(list)
            if (retryMissing) recordAttempt()
        }
    }

    // ── Generators ────────────────────────────────────────────────────────────

    /** Daylist: the songs you favour in the current part of the day, expanded with a radio mix. */
    private suspend fun generateDaylist(): CuratedMix? {
        val (hours, label) = currentDaypart()
        var seeds = dao.topSongsInHours(hours, 8).filter { isStreamable(it.id) }
        if (seeds.isEmpty()) seeds = dao.topSongsOnce(8).filter { isStreamable(it.id) }
        if (seeds.isEmpty()) return null

        // The daypart's favourites first, then radio from up to three of them by different artists,
        // blended by how much each is played. Only the top seed's radio was used before, so one song
        // decided the whole mix.
        val radioSeeds = seeds.distinctBy { Taste.cleanArtist(it.artist).lowercase() }.take(3)
        val radios = coroutineScope {
            radioSeeds.map { seed ->
                async {
                    music.radio(seed.id).getOrDefault(emptyList()).drop(1)
                        .filter { isStreamable(it.id) }
                        .map { it.toHomeSong() } to seed.totalMs.toDouble()
                }
            }.map { it.await() }
        }
        val list = (seeds.map { it.toHomeSong() } + Taste.blend(radios, limit = 40) { it.videoId })
            .distinctBy { it.videoId }
            .take(40)
        if (list.size < 5) return null
        return CuratedMix(
            id = "daylist",
            kind = MixKind.DAYLIST,
            title = label,
            subtitle = "The songs that score your ${label.lowercase()}",
            items = list,
            generatedAt = System.currentTimeMillis(),
        )
    }

    /** Discover: radio seeded from your most-listened tracks, minus anything you've already heard. */
    private suspend fun generateDiscover(): CuratedMix? {
        // One seed per favourite artist, so a single band cannot fill it. The loop used to stop once
        // the first seed's radio had filled the mix, which meant the other four were never asked.
        val loved = favouriteArtists().take(5)
        if (loved.size < 2) return null
        val heard = dao.playedSongIds().toHashSet()
        val radios = coroutineScope {
            loved.map { artist ->
                async {
                    music.radio(artist.songs.first().id).getOrDefault(emptyList()).drop(1)
                        .filter { isStreamable(it.id) && it.id !in heard }
                        .map { it.toHomeSong() } to artist.score
                }
            }.map { it.await() }
        }
        val list = Taste.blend(radios, limit = 30) { it.videoId }
        if (list.size < 8) return null
        return CuratedMix(
            id = "discover",
            kind = MixKind.DISCOVER,
            title = "Discover weekly",
            subtitle = "Fresh tracks you haven't heard, picked from what you love",
            items = list,
            generatedAt = System.currentTimeMillis(),
        )
    }

    /** Release radar: the newest singles/albums from the artists you follow (or, if signed out, play most). */
    private suspend fun generateReleaseRadar(): CuratedMix? {
        val followed = music.subscribedArtists().getOrDefault(emptyList())
            .filter { it.browseId?.startsWith("UC") == true }
        val artists: List<HomeItem> = if (followed.isNotEmpty()) {
            followed.take(6)
        } else {
            dao.topArtistsOnce(6).mapNotNull { a ->
                music.searchItems(a.artist, SearchFilter.ARTISTS).getOrDefault(emptyList())
                    .firstOrNull { it.browseId?.startsWith("UC") == true }
            }
        }
        if (artists.isEmpty()) return null

        val out = LinkedHashMap<String, HomeItem>()
        for (artist in artists) {
            val bid = artist.browseId ?: continue
            val detail = music.artistPage(bid).getOrNull() ?: continue
            detail.sections
                .filter { sec -> RELEASE_SHELVES.any { sec.title.contains(it, ignoreCase = true) } }
                .flatMap { it.items }
                .filter { it.browseId != null || it.playlistId != null }
                .take(3)
                .forEach { rel -> out.putIfAbsent(rel.browseId ?: rel.playlistId!!, rel) }
            if (out.size >= 24) break
        }
        val list = out.values.toList().take(20)
        if (list.size < 4) return null
        return CuratedMix(
            id = "release_radar",
            kind = MixKind.RELEASE_RADAR,
            title = "Release radar",
            subtitle = "The latest from artists you love",
            items = list,
            generatedAt = System.currentTimeMillis(),
        )
    }

    /**
     * Playlists by genre and by vibe, built from the listener's own favourites.
     *
     * The top artists by [Taste] are looked up on MusicBrainz (cached for good after the first time),
     * grouped into broad genres and into vibes, and each group weighted by how much its artists are
     * actually played. The strongest few become mixes: the listener's favourite songs by those artists
     * blended with radio from each, so a mix is recognisably theirs and still has new things in it.
     */
    private suspend fun generateTasteMixes(): List<CuratedMix> {
        val artists = favouriteArtists().take(TASTE_ARTISTS)
        if (artists.size < 2) return emptyList()
        val tags = artists.associateWith { genres.tagsFor(it.name) }
        // Nothing came back at all: offline, or MusicBrainz is down. Try again later instead of
        // concluding the listener has no genres.
        if (tags.values.all { it == null }) return emptyList()

        val genreBuckets = Taste.buckets(artists) { a -> tags[a]?.let { Taste.genreShares(it.genres) }.orEmpty() }
        val vibeBuckets = Taste.buckets(artists) { a -> tags[a]?.let { Taste.vibeShares(it.genres, it.tags) }.orEmpty() }

        val now = System.currentTimeMillis()
        val used = mutableSetOf<Set<String>>()

        // The strongest buckets worth a mix. A bucket made of exactly the same artists as one already
        // chosen is skipped: one band tagged both "britpop" and "rock" would otherwise be two
        // identical playlists.
        fun <K> strongest(buckets: List<Taste.Bucket<K>>): List<Taste.Bucket<K>> {
            val top = buckets.firstOrNull()?.weight ?: return emptyList()
            return buckets
                .filter { it.weight >= top * MIN_BUCKET_SHARE }
                .filter { bucket -> used.add(bucket.artists.take(4).map { it.name }.toSet()) }
                .take(MAX_PER_KIND)
        }

        val genreMixes = strongest(genreBuckets).map { bucket ->
            suspend {
                mixFrom(
                    id = "genre_" + bucket.key.replace(' ', '_'),
                    kind = MixKind.GENRE,
                    title = Taste.displayName(bucket.key),
                    subtitle = "From ${names(bucket.artists)}, and more like them",
                    artists = bucket.artists,
                    generatedAt = now,
                )
            }
        }
        val vibeMixes = strongest(vibeBuckets).map { bucket ->
            suspend {
                mixFrom(
                    id = "vibe_" + bucket.key.name.lowercase(),
                    kind = MixKind.VIBE,
                    title = bucket.key.title,
                    subtitle = bucket.key.subtitle,
                    artists = bucket.artists,
                    generatedAt = now,
                )
            }
        }
        return coroutineScope {
            (genreMixes + vibeMixes).map { build -> async { build() } }.mapNotNull { it.await() }
        }
    }

    /** A mix from a group of artists: their best-loved songs, blended with radio from each. */
    private suspend fun mixFrom(
        id: String,
        kind: MixKind,
        title: String,
        subtitle: String,
        artists: List<Taste.ScoredArtist>,
        generatedAt: Long,
    ): CuratedMix? {
        val members = artists.take(4)
        val favourites = members.flatMap { it.songs.take(3) }
            .sortedByDescending { it.score }
            .map { it.toHomeSong() }
        val radios = coroutineScope {
            members.map { artist ->
                async {
                    music.radio(artist.songs.first().id).getOrDefault(emptyList()).drop(1)
                        .filter { isStreamable(it.id) }
                        .map { it.toHomeSong() } to artist.score
                }
            }.map { it.await() }
        }
        // Favourites carry a bit over a third of the weight until they run out, so the mix opens on
        // songs the listener knows and keeps finding new ones.
        val total = members.sumOf { it.score }
        val items = Taste.blend(listOf(favourites to total * 0.6) + radios, limit = 40) { it.videoId }
        if (items.size < 8) return null
        return CuratedMix(id, kind, title, subtitle, items, generatedAt)
    }

    /** The listener's artists by [Taste], keeping only songs that can seed a YouTube radio. */
    private suspend fun favouriteArtists(): List<Taste.ScoredArtist> =
        Taste.artists(Taste.songs(dao.playsWithSongs(), System.currentTimeMillis()))
            .map { a -> a.copy(songs = a.songs.filter { isStreamable(it.id) }) }
            .filter { it.songs.isNotEmpty() }

    /** "Oasis, Blur" or "Oasis", for a subtitle. */
    private fun names(artists: List<Taste.ScoredArtist>): String =
        artists.take(2).joinToString(", ") { it.name }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun isStale(mix: CuratedMix): Boolean {
        val age = System.currentTimeMillis() - mix.generatedAt
        return when (mix.kind) {
            // Daylist turns over with the day; also refresh if it's a few hours old.
            MixKind.DAYLIST -> mix.title != currentDaypart().second || age > 3 * 60 * 60_000L
            else -> age > 7 * 24 * 60 * 60_000L
        }
    }

    private fun currentDaypart(): Pair<List<Int>, String> =
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> (5..11).toList() to "Morning"
            in 12..16 -> (12..16).toList() to "Afternoon"
            in 17..21 -> (17..21).toList() to "Evening"
            else -> listOf(22, 23, 0, 1, 2, 3, 4) to "Late night"
        }

    private fun isStreamable(id: String): Boolean =
        id.isNotBlank() && !id.startsWith("content://") && !id.startsWith("file://")

    private fun SongStat.toHomeSong() = HomeItem(title = title, subtitle = artist, thumbnailUrl = thumbnailUrl, videoId = id)
    private fun Taste.ScoredSong.toHomeSong() = HomeItem(title = title, subtitle = artist, thumbnailUrl = thumbnailUrl, videoId = id)
    private fun MusicItem.toHomeSong() = HomeItem(title = title, subtitle = artist, thumbnailUrl = thumbnailUrl, videoId = id)

    private suspend fun persist(list: List<CuratedMix>) {
        runCatching { context.mixesStore.edit { it[mixesKey] = json.encodeToString(list) } }
    }

    private suspend fun loadCached(): List<CuratedMix> = runCatching {
        context.mixesStore.data.first()[mixesKey]?.let { json.decodeFromString<List<CuratedMix>>(it) }
    }.getOrNull() ?: emptyList()

    private suspend fun lastAttemptAt(): Long = runCatching {
        context.mixesStore.data.first()[attemptKey]
    }.getOrNull() ?: 0L

    private suspend fun recordAttempt() {
        runCatching { context.mixesStore.edit { it[attemptKey] = System.currentTimeMillis() } }
    }

    private companion object {
        val RELEASE_SHELVES = listOf("single", "album", "release", "new")

        /** How long to wait before trying again to build a mix there was too little history for. */
        const val MISSING_RETRY_MS = 6 * 60 * 60 * 1000L

        /** Favourite artists looked up for genres and vibes. Two MusicBrainz requests each, once ever. */
        const val TASTE_ARTISTS = 12

        /** Genre and vibe mixes each, at most. */
        const val MAX_PER_KIND = 3

        /** A genre or vibe needs this share of the strongest one's weight to get a mix. */
        const val MIN_BUCKET_SHARE = 0.2
    }
}
