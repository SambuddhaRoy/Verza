package com.verza.data

import com.verza.innertube.models.MusicItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** What Discovery radio offers, and in what order. */
class DiscoveryRadioTest {
    private fun song(id: String, artist: String) = MusicItem(id = id, title = id, artist = artist)
    private val nobody = DiscoveryRadio.Known(emptySet(), emptySet())

    @Test fun famousSongsGoLastWithinATier() {
        // A song in four or more of the radios walked is the one everybody knows, the opposite of a
        // discovery. However the shuffle falls, it must not lead the run.
        val pool = listOf(song("hit", "Big Star"), song("cut1", "Small Band"), song("cut2", "Other Band"))
        val counts = mapOf("hit" to 6, "cut1" to 1, "cut2" to 2)
        repeat(200) {
            val ids = DiscoveryRadio.rank(pool, "SEED", nobody, counts = counts).map { it.id }
            assertEquals("hit", ids.last())
        }
    }

    @Test fun servedAndHeardSongsAreNeverOffered() {
        val pool = listOf(song("a", "A"), song("b", "B"), song("c", "C"), song("SEED", "S"))
        val known = DiscoveryRadio.Known(videoIds = setOf("b"), artists = emptySet())
        val ids = DiscoveryRadio.rank(pool, "SEED", known, served = setOf("a")).map { it.id }
        assertEquals(listOf("c"), ids)
        assertFalse("SEED" in ids)
    }
}
