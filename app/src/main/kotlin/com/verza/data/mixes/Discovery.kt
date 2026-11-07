package com.verza.data.mixes

import com.verza.data.JsonSetting
import com.verza.data.Song
import com.verza.data.library.LibraryDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Discovery radio: the seed's genre, without the songs you know. The seed's own radio defines the
 * genre (the artists YouTube treats as next to it); the radios of those artists, in a random order,
 * widen it; anything in your library or offered by an earlier run is dropped (see [DiscoveryRadio]).
 *
 * What it offered is remembered and never wiped ([served], oldest first, capped at 5000): wiping it
 * once a neighbourhood ran dry brought the same forty songs straight back. A dry neighbourhood digs
 * into the same artists' deeper catalogue, and only then recycles what was offered longest ago.
 */
class Discovery(private val music: MusicRepository, db: LibraryDb, private val served: JsonSetting<List<String>>) {
    private val songs = db.songs()

    suspend fun from(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        val known = songs.all().let { all ->
            DiscoveryRadio.Known(all.map { it.id }.toSet(), all.map { DiscoveryRadio.primaryArtist(it.artist) }.filter { it.isNotEmpty() }.toSet())
        }
        val order = served.get()
        val offered = order.toSet()
        val base = music.radio(videoId).getOrThrow()
        val genre = base.map { DiscoveryRadio.primaryArtist(it.artist) }.filter { it.isNotEmpty() }.toSet()
        val pool = base.toMutableList()
        fun usable() = pool.count { it.id != videoId && it.id !in known.videoIds && it.id !in offered && DiscoveryRadio.primaryArtist(it.artist) in genre }

        // Through the seed radio's own artists, in a random order, until there's enough unheard.
        val groups = base.filter { it.id.isNotEmpty() && DiscoveryRadio.primaryArtist(it.artist).isNotEmpty() }
            .groupBy { DiscoveryRadio.primaryArtist(it.artist) }.values.shuffled()
        for (group in groups) {
            if (usable() >= 60) break
            val seed = group.random().id
            if (seed != videoId) pool += music.radio(seed).getOrDefault(emptyList())
        }
        // Radios give an artist's best-known songs, spent after a few runs; their deeper catalogue
        // is still this genre.
        if (usable() < 40) {
            for (g in groups.shuffled().take(6)) {
                val key = DiscoveryRadio.primaryArtist(g.first().artist)
                pool += music.searchSongs(g.first().artist.substringBefore(',').trim()).getOrDefault(emptyList())
                    .filter { DiscoveryRadio.primaryArtist(it.artist) == key }
            }
        }
        // Only this genre: an artist from the seed's radio, or a song several of the radios had.
        val counts = pool.groupingBy { it.id }.eachCount()
        val inGenre = pool.filter { DiscoveryRadio.primaryArtist(it.artist) in genre || (counts[it.id] ?: 0) >= 2 }
        var picked = DiscoveryRadio.rank(inGenre, videoId, known, offered, counts = counts)
        if (picked.size < 10) {
            // Nearly all offered before: top up with what was offered longest ago.
            val age = order.withIndex().associate { it.value to it.index }
            val recycled = inGenre.filter { it.id != videoId && it.id !in known.videoIds && it.id in offered }
                .distinctBy { it.id }.sortedBy { age[it.id] ?: Int.MAX_VALUE }
            picked = (picked + recycled).distinctBy { it.id }.take(40)
                .ifEmpty { inGenre.filter { it.id != videoId }.distinctBy { it.id }.take(40) }
        }
        // Offered again means offered recently: to the back, so oldest-first stays true.
        val again = picked.map { it.id }.toSet()
        served.set((order.filterNot { it in again } + again).takeLast(5000))
        picked.map { Song(it.id, it.title, it.artist, it.album, it.thumbnailUrl, it.durationMs) }
    }
}
