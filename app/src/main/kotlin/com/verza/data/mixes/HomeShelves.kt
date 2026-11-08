package com.verza.data.mixes

import com.verza.data.Song

/**
 * Home's shelves from the listening log, so everything on Home comes from what you play. YouTube's
 * own home feed was dropped: signed out, and for most accounts signed in, it is charts and trending.
 *
 * - On repeat: what you've played most in the last month, two plays or more.
 * - Forgotten favourites: songs you played three times or more, not in the last six weeks.
 * - Seeds for "because you listened to": your top artists of the last two weeks, each with the song
 *   of theirs you've listened to longest. Their radios are fetched by the caller.
 *
 * A shelf with fewer than [MIN_SONGS] songs is left out rather than shown half empty.
 */
object HomeShelves {
    data class Seed(val artist: String, val songId: String)
    data class Shelves(val onRepeat: List<Song>, val forgotten: List<Song>, val seeds: List<Seed>)

    const val MIN_SONGS = 4
    private const val DAY = 24 * 60 * 60 * 1000L

    fun from(plays: List<PlayWithSong>, now: Long): Shelves {
        fun song(p: PlayWithSong) = Song(p.songId, p.title, p.artist, art = p.thumbnailUrl)
        val bySong = plays.groupBy { it.songId }

        val onRepeat = plays.filter { now - it.playedAt <= 30 * DAY }.groupBy { it.songId }
            .filterValues { it.size >= 2 }
            .entries.sortedWith(compareByDescending<Map.Entry<String, List<PlayWithSong>>> { it.value.size }.thenByDescending { e -> e.value.sumOf { it.listenedMs } })
            .take(20).map { song(it.value.first()) }

        val forgotten = bySong.filterValues { ps -> ps.size >= 3 && ps.all { now - it.playedAt > 42 * DAY } }
            .entries.sortedByDescending { it.value.size }
            .take(20).map { song(it.value.first()) }

        val seeds = plays.filter { now - it.playedAt <= 14 * DAY && it.artist.isNotBlank() }
            .groupBy { DiscoveryRadio.primaryArtist(it.artist) }
            .entries.sortedByDescending { e -> e.value.sumOf { it.listenedMs } }
            .take(2)
            .map { (_, ps) ->
                val top = ps.groupBy { it.songId }.maxBy { (_, l) -> l.sumOf { it.listenedMs } }.value.first()
                Seed(top.artist.substringBefore(',').trim(), top.songId)
            }

        return Shelves(onRepeat.takeIf { it.size >= MIN_SONGS }.orEmpty(), forgotten.takeIf { it.size >= MIN_SONGS }.orEmpty(), seeds)
    }
}
