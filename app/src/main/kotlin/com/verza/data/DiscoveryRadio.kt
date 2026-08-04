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
     * Seeds for the next hop, chosen at RANDOM among artists not yet branched through.
     *
     * Picking the *first* distinct-artist tracks (the obvious approach) makes the whole walk
     * deterministic — YouTube's radio for an id never changes — so every Discovery run explored the
     * identical slice of the graph and only the shuffle order differed. Randomising the frontier is
     * what makes each run take a different path outward.
     */
    fun frontier(tracks: List<MusicItem>, branched: Set<String>, limit: Int = 3): List<String> =
        tracks
            .filter { it.id.isNotEmpty() && it.id !in branched && primaryArtist(it.artist).isNotEmpty() }
            .groupBy { primaryArtist(it.artist) }
            .values
            .shuffled()
            .take(limit)
            .map { group -> group.random().id }

    /**
     * Dedupe [pool], drop the seed, anything already heard, and anything a previous run already
     * offered ([served]); then order unknown artists first, each tier shuffled. Capped at [cap].
     */
    fun rank(
        pool: List<MusicItem>,
        seedId: String,
        known: Known,
        served: Set<String> = emptySet(),
        cap: Int = 40,
    ): List<MusicItem> {
        val seen = mutableSetOf(seedId)
        val fresh = pool.filter { t ->
            t.id.isNotEmpty() && seen.add(t.id) && t.id !in known.videoIds && t.id !in served
        }
        val (unfamiliar, familiar) = fresh.partition { primaryArtist(it.artist) !in known.artists }
        return (unfamiliar.shuffled() + familiar.shuffled()).take(cap)
    }
}
