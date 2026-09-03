package com.verza.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The cover matcher.
 *
 * This exists because the app used to take the first search result and show it. iTunes matches
 * loosely, so a common title returned somebody else's recording and the app displayed it with
 * complete confidence: right words, wrong record. Every rejection case below is a cover that would
 * previously have been shown.
 */
class ArtworkMatchTest {

    private fun candidate(track: String, artist: String, url: String = "art://$track") =
        ArtworkCandidate(track, artist, url)

    @Test
    fun `the same title by a different artist is refused`() {
        val results = listOf(
            candidate("Wonderwall", "Ryan Adams"),
            candidate("Wonderwall", "Oasis", "art://oasis"),
        )
        assertEquals("art://oasis", bestArtworkMatch(results, "Wonderwall", "Oasis")?.artworkUrl)
    }

    @Test
    fun `nothing by the right artist means no cover at all`() {
        // The important half. A wrong cover is worse than the YouTube still, so the answer here is
        // null rather than a best effort.
        val results = listOf(
            candidate("Wonderwall", "Ryan Adams"),
            candidate("Wonderwall", "Cat Power"),
        )
        assertNull(bestArtworkMatch(results, "Wonderwall", "Oasis"))
    }

    @Test
    fun `an unknown artist is never guessed at`() {
        val results = listOf(candidate("Wonderwall", "Ryan Adams"))
        assertNull(bestArtworkMatch(results, "Wonderwall", ""))
        assertNull(bestArtworkMatch(results, "", "Oasis"))
    }

    @Test
    fun `the sources are allowed to disagree about detail`() {
        // Real pairs. Neither side is wrong, they just annotate differently, and demanding equality
        // would reject almost every genuine match.
        val cases = listOf(
            Triple("Wonderwall (Remastered)", "Oasis", "Wonderwall"),
            Triple("Blinding Lights", "The Weeknd", "Blinding Lights (Official Video)"),
            Triple("Sunflower", "Post Malone & Swae Lee", "Sunflower"),
            Triple("Levitating", "Dua Lipa", "Levitating (feat. DaBaby)"),
        )
        for ((itunesTitle, itunesArtist, ytTitle) in cases) {
            val match = bestArtworkMatch(
                listOf(candidate(itunesTitle, itunesArtist, "art://ok")),
                ytTitle,
                itunesArtist,
            )
            assertEquals("$ytTitle by $itunesArtist should match", "art://ok", match?.artworkUrl)
        }
    }

    @Test
    fun `a YouTube topic channel still matches the artist`() {
        // Auto-generated YouTube artist channels are named "Oasis - Topic".
        val match = bestArtworkMatch(
            listOf(candidate("Champagne Supernova", "Oasis", "art://ok")),
            "Champagne Supernova",
            "Oasis - Topic",
        )
        assertEquals("art://ok", match?.artworkUrl)
    }

    @Test
    fun `a short name does not match everything containing it`() {
        // "Go" inside "Let It Go" is the failure mode containment invites, so short names have to
        // match exactly.
        assertNull(bestArtworkMatch(listOf(candidate("Let It Go", "Idina Menzel")), "Go", "Idina Menzel"))
    }

    @Test
    fun `an empty result set is not a match`() {
        assertNull(bestArtworkMatch(emptyList(), "Wonderwall", "Oasis"))
    }
}
