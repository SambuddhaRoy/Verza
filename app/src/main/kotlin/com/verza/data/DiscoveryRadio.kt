package com.verza.data

import com.verza.innertube.models.MusicItem

/**
 * Discovery radio — an anti-monotony alternative to "Start radio", ported from the desktop build.
 *
 * A normal radio leans on tracks you already know and keeps returning to the seed's own artist.
 * Discovery keeps the genre but pushes outward: it hops the radio into a couple of *adjacent*
 * artists, drops everything you've already heard, and ranks unfamiliar artists ahead of new songs
 * by artists you already listen to.
 *
 * The ranking is pure so it can be reasoned about (and unit-tested) without a network.
 */
object DiscoveryRadio {

    /** Everything the listener already knows — used to filter and de-prioritise candidates. */
    data class Known(val videoIds: Set<String>, val artists: Set<String>)

    /** The primary artist of a credit like "A, B & C", lowercased for comparison. */
    fun primaryArtist(artist: String): String =
        artist.substringBefore(',').trim().lowercase()

    /**
     * Up to [limit] seeds for the second hop: the first tracks by *distinct* artists, so the hops
     * explore sideways instead of re-querying the same artist.
     */
    fun branchSeeds(base: List<MusicItem>, limit: Int = 2): List<String> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<String>()
        for (t in base) {
            val a = primaryArtist(t.artist)
            if (a.isNotEmpty() && seen.add(a)) out += t.id
            if (out.size >= limit) break
        }
        return out
    }

    /**
     * Dedupe [pool], drop the seed and anything already heard, then order unknown artists first
     * (each tier shuffled so repeat runs differ). Capped at [cap].
     */
    fun rank(
        pool: List<MusicItem>,
        seedId: String,
        known: Known,
        cap: Int = 40,
    ): List<MusicItem> {
        val seen = mutableSetOf(seedId)
        val fresh = pool.filter { t ->
            t.id.isNotEmpty() && seen.add(t.id) && t.id !in known.videoIds
        }
        val (unfamiliar, familiar) = fresh.partition { primaryArtist(it.artist) !in known.artists }
        return (unfamiliar.shuffled() + familiar.shuffled()).take(cap)
    }
}
