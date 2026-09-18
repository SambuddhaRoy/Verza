package com.verza.data

import com.verza.data.db.SongEntity
import com.verza.innertube.InnerTube
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.HomeSection
import com.verza.innertube.models.MusicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composes the Home feed from multiple sources, prioritising sections **tailored to the user's
 * listening** and pushing all generic / editorial chart shelves into a single carousel at the
 * bottom. Order (sections with no content are dropped silently):
 *
 *   1. Recently played            (local Room)
 *   2. Quick picks                (YT personalised home shelf, signed-in)
 *   3. Your daily discover        (YT personalised — Discover / Daily Mix / Mixed for you)
 *   4. Keep listening             (YT "Listen again", or a derived local fallback)
 *   5. From your liked songs      (local Room, deduped against #1)
 *   6. Your YouTube playlists     (the user's saved YT playlists, signed-in only)
 *   7+. Because you love <artist> (radio for the two artists played most, by Taste)
 *   N. Browse charts and trending (everything else — Trending, Languages, New releases, Charts,
 *                                  community playlists — flattened into one row at the bottom)
 *
 * Everything that goes over the network runs in parallel via `async` so the page is bounded by
 * the slowest single call, not their sum.
 */
@Singleton
class HomeFeedBuilder @Inject constructor(
    private val library: LibraryRepository,
    private val stats: StatsRepository,
) {
    /**
     * The half of Home that needs no network, for painting while the rest is still in flight.
     *
     * Both sections come straight out of Room and are ready in milliseconds, but they used to be
     * held behind the slowest of six network calls because everything was awaited before anything
     * was shown. Same sections, same titles, same order as [compose] — this only brings them
     * forward, so the feed folding in afterwards replaces like with like instead of reshuffling.
     */
    suspend fun localSections(): List<HomeSection> {
        val recent = library.recentlyPlayed().first().take(20)
        val liked = library.liked().first()
        return buildList {
            if (recent.isNotEmpty()) {
                add(HomeSection("Recently played", recent.take(15).map { it.toHomeSong() }))
            }
            val recentIds = recent.mapTo(mutableSetOf()) { it.id }
            val fresh = liked.filter { it.id !in recentIds }.take(15)
            if (fresh.isNotEmpty()) {
                add(HomeSection("From your liked songs", fresh.map { it.toHomeSong() }))
            }
        }
    }

    suspend fun build(): Result<List<HomeSection>> = runCatching {
        coroutineScope {
            val ytAsync = async(Dispatchers.IO) {
                runCatching { InnerTube.homeFeed() }.getOrDefault(emptyList())
            }
            val ytPlaylistsAsync = async(Dispatchers.IO) {
                runCatching { InnerTube.libraryPlaylists() }.getOrDefault(emptyList())
            }
            val recent = library.recentlyPlayed().first().take(20)
            val liked = library.liked().first()

            // Seeds come from taste, not from whatever played last. Every one of these rows used to be
            // seeded by the most recent artists, so trying one new song swung the whole page towards
            // it while the band on repeat for a month went unmentioned. See Taste for the scoring.
            // Local files (content:// ids) have no YouTube radio, so they cannot seed.
            val loved = runCatching { Taste.artists(stats.taste()) }.getOrDefault(emptyList())
                .map { a -> a.copy(songs = a.songs.filter { !it.id.startsWith("content://") }) }
                .filter { it.songs.isNotEmpty() }
                .take(FAVOURITE_SEEDS)
            // A brand-new install has plays too short or too few to score; fall back to recent artists
            // so it still gets something rather than nothing.
            val seeds: List<Pair<String, String>> = loved.map { it.songs.first().id to it.name }
                .ifEmpty {
                    recent.filter { Taste.cleanArtist(it.artist).isNotEmpty() && !it.id.startsWith("content://") }
                        .distinctBy { Taste.cleanArtist(it.artist).lowercase() }
                        .take(2)
                        .map { it.id to Taste.cleanArtist(it.artist) }
                }
            val radios = seeds.map { (id, _) ->
                async(Dispatchers.IO) {
                    // Skip the seed itself, which always comes first.
                    runCatching { InnerTube.radio(id).drop(1).map { it.toHomeSong() } }.getOrDefault(emptyList())
                }
            }.awaitAll()

            val yt = ytAsync.await()
            val ytPl = ytPlaylistsAsync.await()

            // "Because you love" for the top two artists, each its own radio.
            val label = if (loved.isNotEmpty()) "Because you love" else "Similar to"
            val similarSections = seeds.zip(radios).take(2)
                .filter { (_, items) -> items.isNotEmpty() }
                .map { (seed, items) -> HomeSection("$label ${seed.second}", items.take(15)) }

            // "More of what you love" draws on all of them, each in proportion to how much that artist
            // is played, minus what the two rows above already show and what was just played.
            val shown = similarSections.flatMap { it.items }.mapNotNullTo(HashSet()) { it.videoId }
            val skip = shown + recent.map { it.id } + seeds.map { it.first }
            val recommended = if (loved.isEmpty()) emptyList() else Taste.blend(
                lists = loved.zip(radios).map { (artist, items) -> items.filter { it.videoId !in skip } to artist.score },
                limit = 15,
            ) { it.videoId }

            compose(yt, ytPl, recent, liked, similarSections, recommended)
        }
    }

    private fun compose(
        ytSections: List<HomeSection>,
        ytPlaylists: List<HomeItem>,
        recent: List<SongEntity>,
        liked: List<SongEntity>,
        similarSections: List<HomeSection>,
        recommended: List<HomeItem>,
    ): List<HomeSection> {
        val out = mutableListOf<HomeSection>()
        val consumed = mutableSetOf<String>()

        fun take(section: HomeSection, asTitle: String? = null) {
            consumed += section.title
            out += if (asTitle != null) section.copy(title = asTitle) else section
        }

        // 1. Recently played — from Room (no network).
        if (recent.isNotEmpty()) {
            out += HomeSection("Recently played", recent.take(15).map { it.toHomeSong() })
        }

        // 1.5 More of what you love: a private, on-device blend of radio from the artists you
        //     actually play most. No account, no tracking.
        if (recommended.isNotEmpty()) {
            out += HomeSection("More of what you love", recommended)
        }

        // 2. Quick picks — YT personalised home shelf, present when signed in.
        find(ytSections, consumed, "Quick picks", "Top picks for you", "Picks for you")
            ?.let { take(it, asTitle = "Quick picks") }

        // 3. Your daily discover — YT personalised mixes.
        find(ytSections, consumed, "Daily Mix", "Discover Mix", "Mixed for you", "Mix for you", "Discover")
            ?.let { take(it, asTitle = "Your daily discover") }

        // 4. Keep listening — YT's "Listen again" if signed in, otherwise derived from Room so the
        //    section still shows something tailored to the user.
        val ytKeep = find(ytSections, consumed, "Listen again", "Continue listening")
        if (ytKeep != null) {
            take(ytKeep, asTitle = "Keep listening")
        } else if (recent.size >= 4) {
            // Use an older slice so it doesn't echo "Recently played" right above it.
            val derived = recent.drop(3).take(10)
            if (derived.isNotEmpty()) {
                out += HomeSection("Keep listening", derived.map { it.toHomeSong() })
            }
        }

        // 5. From your liked songs — local likes, excluding anything already in Recently played.
        if (liked.isNotEmpty()) {
            val recentIds = recent.mapTo(mutableSetOf()) { it.id }
            val fresh = liked.filter { it.id !in recentIds }.take(15)
            if (fresh.isNotEmpty()) {
                out += HomeSection("From your liked songs", fresh.map { it.toHomeSong() })
            }
        }

        // 6. Your YouTube playlists — signed-in only.
        if (ytPlaylists.isNotEmpty()) {
            out += HomeSection("Your YouTube playlists", ytPlaylists.take(20))
        }

        // 7+. Because you love <artist1>, <artist2>: already built above.
        out += similarSections

        // BOTTOM. Single consolidated section for everything generic / editorial — Trending,
        // Languages, Video charts, New albums & singles, Music videos, community playlists, etc.
        // We flatten their items into one carousel and cap the total so the personalised sections
        // above always dominate the page.
        val leftover = ytSections
            .filter { it.title !in consumed && it.items.isNotEmpty() }
            .flatMap { it.items }
            .distinctBy { it.browseId ?: it.videoId ?: it.playlistId ?: it.title }
            .take(30)
        if (leftover.isNotEmpty()) {
            out += HomeSection("Browse charts and trending", leftover)
        }

        return out.filter { it.items.isNotEmpty() }
    }

    private companion object {
        /** How many of the listener's favourite artists seed Home. One radio request each. */
        const val FAVOURITE_SEEDS = 5
    }

    private fun find(
        pool: List<HomeSection>,
        consumed: Set<String>,
        vararg keywords: String,
    ): HomeSection? = pool.firstOrNull { sec ->
        sec.title !in consumed && keywords.any { kw -> sec.title.contains(kw, ignoreCase = true) }
    }

    private fun SongEntity.toHomeSong(): HomeItem = HomeItem(
        title = title,
        subtitle = artist,
        thumbnailUrl = thumbnailUrl,
        videoId = id,
    )

    private fun MusicItem.toHomeSong(): HomeItem = HomeItem(
        title = title,
        subtitle = artist,
        thumbnailUrl = thumbnailUrl,
        videoId = id,
    )
}
