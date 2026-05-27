package com.lstn.data

import com.lstn.data.db.SongEntity
import com.lstn.innertube.InnerTube
import com.lstn.innertube.models.HomeItem
import com.lstn.innertube.models.HomeSection
import com.lstn.innertube.models.MusicItem
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
 *   7+. Similar to <artist>       (radio mix for up to two distinct recent artists)
 *   N. Browse charts and trending (everything else — Trending, Languages, New releases, Charts,
 *                                  community playlists — flattened into one row at the bottom)
 *
 * Everything that goes over the network runs in parallel via `async` so the page is bounded by
 * the slowest single call, not their sum.
 */
@Singleton
class HomeFeedBuilder @Inject constructor(
    private val library: LibraryRepository,
) {
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

            // Pick up to two distinct recent artists; fetch a radio mix for each in parallel so
            // the user sees both "Similar to A" and "Similar to B" rather than just one.
            val seedTracks = recent
                .filter { it.artist.isNotBlank() }
                .distinctBy { it.artist.trim().lowercase() }
                .take(2)
            val similarAsyncs = seedTracks.map { seed ->
                async(Dispatchers.IO) {
                    val items = runCatching {
                        // Skip the seed itself (always first in the mix); cap to a tidy carousel.
                        InnerTube.radio(seed.id).drop(1).take(15).map { it.toHomeSong() }
                    }.getOrDefault(emptyList())
                    items to seed.artist
                }
            }

            val yt = ytAsync.await()
            val ytPl = ytPlaylistsAsync.await()
            val similarSections = similarAsyncs.awaitAll()
                .filter { it.first.isNotEmpty() }
                .map { (items, artist) -> HomeSection("Similar to $artist", items) }

            compose(yt, ytPl, recent, liked, similarSections)
        }
    }

    private fun compose(
        ytSections: List<HomeSection>,
        ytPlaylists: List<HomeItem>,
        recent: List<SongEntity>,
        liked: List<SongEntity>,
        similarSections: List<HomeSection>,
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

        // 7+. Similar to <artist1>, Similar to <artist2> — already built above.
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
