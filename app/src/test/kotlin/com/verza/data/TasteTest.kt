package com.verza.data

import com.verza.data.db.PlayWithSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Taste scoring.
 *
 * The complaint that started this: recommendations followed whatever played last instead of what
 * the listener plays most. The first test is that complaint, stated as a failing case.
 */
class TasteTest {

    private val day = 86_400_000L
    private val now = 1_000L * day

    private fun play(id: String, artist: String, daysAgo: Double, minutes: Double = 3.5) =
        PlayWithSong(id, "Song $id", artist, null, now - (daysAgo * day).toLong(), (minutes * 60_000).toLong())

    @Test
    fun `a song on repeat for weeks outranks one played this morning`() {
        val favourite = (1..20).map { play("fav", "Oasis", daysAgo = 10.0 + it) }
        val justNow = listOf(play("new", "Someone Else", daysAgo = 0.0))
        val ranked = Taste.songs(favourite + justNow, now)
        assertEquals("fav", ranked.first().id)
        assertTrue(ranked.first().score > 10 * ranked.last().score)
    }

    @Test
    fun `a skip counts for far less than a full listen`() {
        val skipped = Taste.playWeight(listenedMs = 10_000, ageMs = 0)
        val heard = Taste.playWeight(listenedMs = 240_000, ageMs = 0)
        assertEquals(1.0, heard, 1e-9)
        assertTrue(skipped < 0.1)
    }

    @Test
    fun `a listen is worth half after one half-life`() {
        val fresh = Taste.playWeight(FULL, 0)
        val old = Taste.playWeight(FULL, (Taste.HALF_LIFE_DAYS * day).toLong())
        assertEquals(fresh / 2, old, 1e-9)
    }

    @Test
    fun `artists add up their songs`() {
        val plays = listOf(
            play("a1", "Blur", 1.0), play("a2", "Blur", 1.0),
            play("b1", "Pulp", 1.0),
        )
        val artists = Taste.artists(Taste.songs(plays, now))
        assertEquals("Blur", artists.first().name)
        assertEquals(2, artists.first().songs.size)
    }

    @Test
    fun `youtube noise is stripped from artist names`() {
        assertEquals("Oasis", Taste.cleanArtist("Oasis 9.4M views 4:07"))
        assertEquals("Oasis", Taste.cleanArtist("Oasis • 9.4M views • 4:07"))
        assertEquals("Oasis", Taste.cleanArtist("Oasis - Topic"))
        assertEquals("Radiohead", Taste.cleanArtist("Song Radiohead"))
        assertEquals("Songhoy Blues", Taste.cleanArtist("Songhoy Blues"))
        assertEquals("Calvin Harris", Taste.cleanArtist("Calvin Harris feat. Rihanna"))
        assertEquals("Simon & Garfunkel", Taste.cleanArtist("Simon & Garfunkel"))
        assertEquals("Daft Punk", Taste.cleanArtist("Daft Punk"))
        assertEquals("", Taste.cleanArtist("Unknown artist"))
    }

    @Test
    fun `the same band with noisy bylines is one artist`() {
        val plays = listOf(play("x", "Oasis 9.4M views 4:07", 1.0), play("y", "Oasis", 1.0))
        assertEquals(1, Taste.artists(Taste.songs(plays, now)).size)
    }

    @Test
    fun `specific genres roll up into broad ones`() {
        assertEquals("rock", Taste.parentOf("alternative rock"))
        assertEquals("pop", Taste.parentOf("indian pop"))
        assertEquals("britpop", Taste.parentOf("britpop"))
        assertEquals("hip hop", Taste.parentOf("Hip Hop"))
    }

    @Test
    fun `five kinds of rock do not make an artist five times as rock`() {
        val shares = Taste.genreShares(
            listOf(Taste.Tag("alternative rock", 10), Taste.Tag("indie rock", 8), Taste.Tag("hard rock", 2)),
        )
        assertEquals(1.0, shares.getValue("rock"), 1e-9)
    }

    @Test
    fun `vibes match whole words only`() {
        val moody = Taste.vibeShares(listOf(Taste.Tag("shoegaze", 5)), emptyList())
        assertEquals(1.0, moody.getValue(Taste.Vibe.MOODY), 1e-9)
        val notHouse = Taste.vibeShares(listOf(Taste.Tag("madhouse", 5)), emptyList())
        assertTrue(Taste.Vibe.ENERGY !in notHouse)
    }

    @Test
    fun `buckets are weighted by plays, not by artist count`() {
        val big = Taste.ScoredArtist("Big", 50.0, emptyList())
        val small1 = Taste.ScoredArtist("Small1", 1.0, emptyList())
        val small2 = Taste.ScoredArtist("Small2", 1.0, emptyList())
        val genres = mapOf("Big" to "rock", "Small1" to "jazz", "Small2" to "jazz")
        val buckets = Taste.buckets(listOf(big, small1, small2)) { mapOf(genres.getValue(it.name) to 1.0) }
        assertEquals("rock", buckets.first().key)
        assertEquals(listOf("Small1", "Small2"), buckets[1].artists.map { it.name })
    }

    @Test
    fun `blend gives heavier lists proportionally more and spreads them out`() {
        val heavy = (1..30).map { "h$it" }
        val light = (1..30).map { "l$it" }
        val out = Taste.blend(listOf(heavy to 3.0, light to 1.0), limit = 20) { it }
        assertEquals(15, out.count { it.startsWith("h") })
        assertEquals(5, out.count { it.startsWith("l") })
        // Spread, not a block of heavy then a block of light.
        assertTrue(out.take(8).any { it.startsWith("l") })
    }

    @Test
    fun `blend skips duplicates and keeps going when a list runs out`() {
        val out = Taste.blend(listOf(listOf("a", "b") to 1.0, listOf("a", "c", "d") to 1.0), limit = 10) { it }
        assertEquals(listOf("a", "b", "c", "d").sorted(), out.sorted())
    }

    @Test
    fun `genre names read properly`() {
        assertEquals("Hip Hop", Taste.displayName("hip hop"))
        assertEquals("R&B", Taste.displayName("r&b"))
    }

    private companion object {
        const val FULL = Taste.FULL_LISTEN_MS
    }
}
