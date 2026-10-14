package com.verza.data

import com.verza.innertube.InnerTube
import com.verza.innertube.SearchFilter
import com.verza.innertube.models.ArtistDetail
import com.verza.innertube.models.CollectionDetail
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.HomeSection
import com.verza.innertube.models.MusicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single seam between the UI and the InnerTube client. ViewModels depend on this
 * (never on raw JSON), which keeps parsing concerns out of the presentation layer and
 * gives us one place to add caching/Room later.
 */
@Singleton
class MusicRepository @Inject constructor() {

    suspend fun searchSongs(query: String): Result<List<MusicItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.searchSongs(query) } }

    suspend fun searchItems(query: String, filter: SearchFilter): Result<List<HomeItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.searchItems(query, filter) } }

    suspend fun searchSuggestions(query: String): Result<List<String>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.searchSuggestions(query) } }

    suspend fun radio(videoId: String): Result<List<MusicItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.radio(videoId) } }

    /**
     * Discovery radio: same genre as [videoId], but weighted toward music the listener hasn't heard.
     * Hops the radio into a couple of adjacent artists, then filters/ranks against [known].
     * Falls back to the plain radio if filtering leaves nothing (a very large library).
     */
    suspend fun discoveryRadio(
        videoId: String,
        known: DiscoveryRadio.Known,
        // Oldest first, so a neighbourhood that has truly run dry brings back the songs offered
        // longest ago instead of the same top forty.
        servedOrder: List<String> = emptyList(),
    ): Result<DiscoveryResult> = runCatching {
        val served = servedOrder.toSet()
        withContext(Dispatchers.IO) {
            // The seed's OWN radio defines the genre: the artist neighbourhood YouTube treats as
            // adjacent to it. Walking outward hop-by-hop (the earlier approach) bought fresh tracks
            // at the cost of drifting into a neighbouring genre the longer the queue ran, and worse
            // on repeat runs. So go WIDER inside that neighbourhood instead of deeper out of it.
            val base = InnerTube.radio(videoId)
            val genre = base.map { DiscoveryRadio.primaryArtist(it.artist) }.filter { it.isNotEmpty() }.toSet()

            val pool = mutableListOf<MusicItem>()
            pool += base

            fun usable() = pool.count { t ->
                t.id != videoId && t.id !in known.videoIds && t.id !in served &&
                    DiscoveryRadio.primaryArtist(t.artist) in genre
            }

            // Branch through the seed radio's own artists, in random order, until there are enough
            // unheard in-genre candidates. Different artists each run ⇒ new music, same genre.
            val groups = base
                .filter { it.id.isNotEmpty() && DiscoveryRadio.primaryArtist(it.artist).isNotEmpty() }
                .groupBy { DiscoveryRadio.primaryArtist(it.artist) }
                .values.shuffled()
            for (group in groups) {
                if (usable() >= 60) break
                val seed = group.random().id
                if (seed == videoId) continue
                pool += runCatching { InnerTube.radio(seed) }.getOrDefault(emptyList())
            }

            // Radios only surface an artist's best-known songs, and after a few runs on one seed those
            // are spent. The same artists' deeper catalogue is still this genre and is exactly what a
            // listener has not come across, so dig there before falling back to anything offered.
            if (usable() < 40) {
                val names = groups.associate { g ->
                    DiscoveryRadio.primaryArtist(g.first().artist) to g.first().artist.substringBefore(',').trim()
                }
                for ((key, name) in names.entries.shuffled().take(6)) {
                    pool += runCatching { InnerTube.searchSongs(name) }.getOrDefault(emptyList())
                        .filter { DiscoveryRadio.primaryArtist(it.artist) == key }
                }
            }

            // Keep only what belongs to this genre — an artist from the seed's radio, or a track that
            // recurred across several of those radios (strong evidence of the same corner of music).
            val counts = pool.groupingBy { it.id }.eachCount()
            val inGenre = pool.filter {
                DiscoveryRadio.primaryArtist(it.artist) in genre || (counts[it.id] ?: 0) >= 2
            }

            val ranked = DiscoveryRadio.rank(inGenre, videoId, known, served, counts = counts)
            if (ranked.size >= 10) return@withContext DiscoveryResult(ranked)
            // Nearly everything here has been offered before: top up with what was offered longest ago.
            // This used to forget the whole memory instead, which brought the same songs straight back.
            val position = servedOrder.withIndex().associate { it.value to it.index }
            val recycled = inGenre
                .filter { it.id != videoId && it.id !in known.videoIds && it.id in served }
                .distinctBy { it.id }
                .sortedBy { position[it.id] ?: Int.MAX_VALUE }
            val topped = (ranked + recycled).distinctBy { it.id }.take(40)
            DiscoveryResult(topped.ifEmpty { inGenre.filter { it.id != videoId }.distinctBy { it.id }.take(40) })
        }
    }

    /** [tracks] to play. */
    data class DiscoveryResult(val tracks: List<MusicItem>)

    /** The signed-in user's "Liked Music" playlist (VLLM). Empty when signed out. */
    suspend fun accountLikedSongs(): Result<List<MusicItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.collectionTracks(browseId = "VLLM") } }

    suspend fun collectionTracks(browseId: String?, playlistId: String?): Result<List<MusicItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.collectionTracks(browseId, playlistId) } }

    suspend fun collectionDetail(collectionId: String): Result<CollectionDetail> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.collectionDetail(collectionId) } }

    suspend fun artistPage(browseId: String): Result<ArtistDetail> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.artistPage(browseId) } }

    suspend fun libraryPlaylists(): Result<List<HomeItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.libraryPlaylists() } }

    suspend fun subscribedArtists(): Result<List<HomeItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.subscribedArtists() } }
}
