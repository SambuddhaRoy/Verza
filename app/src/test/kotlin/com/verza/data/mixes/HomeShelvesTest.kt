package com.verza.data.mixes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeShelvesTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 1000 * day
    private fun play(id: String, artist: String, daysAgo: Int, ms: Long = 200_000) =
        PlayWithSong(id, "t$id", artist, null, now - daysAgo * day, ms)

    @Test fun onRepeatIsLastMonthsMostPlayed() {
        val plays = listOf("a", "b", "c", "d").flatMap { id -> List(3) { play(id, "X", 2) } } +
            List(5) { play("e", "X", 2) } +
            play("once", "X", 1) +
            List(9) { play("old", "X", 60) }
        val s = HomeShelves.from(plays, now)
        assertEquals("e", s.onRepeat.first().id)
        assertTrue(s.onRepeat.none { it.id == "once" || it.id == "old" })
    }

    @Test fun tooFewSongsLeavesTheShelfOut() {
        val plays = List(3) { play("a", "X", 1) } + List(3) { play("b", "X", 1) }
        assertTrue(HomeShelves.from(plays, now).onRepeat.isEmpty())
    }

    @Test fun forgottenIsPlayedOftenButNotLately() {
        val plays = listOf("a", "b", "c", "d").flatMap { id -> List(3) { play(id, "X", 90) } } +
            List(3) { play("recent", "X", 90) } + play("recent", "X", 3)
        val s = HomeShelves.from(plays, now)
        assertEquals(setOf("a", "b", "c", "d"), s.forgotten.map { it.id }.toSet())
    }

    @Test fun seedsAreTheTopRecentArtistsAndTheirMostPlayedSong() {
        val plays = listOf(
            play("r1", "Radiohead", 1, 100_000), play("r2", "Radiohead", 2, 900_000),
            play("q1", "Queen, David Bowie", 1, 500_000),
            play("z1", "Zed", 1, 10_000),
            play("old", "Oldie", 30, 9_000_000),
        )
        val seeds = HomeShelves.from(plays, now).seeds
        assertEquals(listOf(HomeShelves.Seed("Radiohead", "r2"), HomeShelves.Seed("Queen", "q1")), seeds)
    }
}
