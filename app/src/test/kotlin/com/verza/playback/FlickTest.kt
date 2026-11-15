package com.verza.playback

import com.verza.playback.Flick.Companion.AWAY
import com.verza.playback.Flick.Companion.TOWARD
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sin

/** Synthetic accelerometer traces at 100Hz, gravity included (phone flat on a hand: z = 9.8). */
class FlickTest {
    private val g = 9.81f
    private val step = 10_000_000L

    /** Feeds [readings] (x, y, z extra over gravity), returns the flicks that fired, in order. */
    private fun run(readings: List<Triple<Float, Float, Float>>, flat: Boolean = true): List<Int> {
        val f = Flick()
        var t = 0L
        val fired = mutableListOf<Int>()
        val base = if (flat) Triple(0f, 0f, g) else Triple(0f, g, 0f)   // flat, or held upright facing you
        repeat(30) { f.feed(base.first, base.second, base.third, t); t += step }
        for ((x, y, z) in readings) { f.feed(base.first + x, base.second + y, base.third + z, t).let { if (it != 0) fired += it }; t += step }
        return fired
    }
    private fun still(n: Int) = List(n) { Triple(0f, 0f, 0f) }

    @Test fun aFlickTowardTheTopEdgeSends() {
        val flick = still(5) + listOf(Triple(1f, 8f, 0f), Triple(1f, 20f, 1f), Triple(0f, 24f, 1f), Triple(0f, 6f, 0f), Triple(0f, -14f, 0f), Triple(0f, -6f, 0f)) + still(20)
        assertEquals(listOf(AWAY), run(flick))
    }

    @Test fun aPushAwayFromYouSends() {
        val push = still(5) + listOf(Triple(0f, 0f, -10f), Triple(0f, 1f, -22f), Triple(0f, 0f, -18f), Triple(0f, 0f, 14f), Triple(0f, 0f, 4f)) + still(20)
        assertEquals(listOf(AWAY), run(push, flat = false))
    }

    @Test fun aFlickBackTowardYouTakes() {
        // Flat, pointing at the computer: pulled back by the bottom edge, then stopped.
        val pull = still(5) + listOf(Triple(-1f, -8f, 0f), Triple(0f, -20f, 1f), Triple(0f, -24f, 0f), Triple(0f, -5f, 0f), Triple(0f, 14f, 0f), Triple(0f, 5f, 0f)) + still(20)
        assertEquals(listOf(TOWARD), run(pull))
    }

    @Test fun aPullToYourFaceTakes() {
        val pull = still(5) + listOf(Triple(0f, 0f, 10f), Triple(0f, -1f, 22f), Triple(0f, 0f, 18f), Triple(0f, 0f, -14f), Triple(0f, 0f, -4f)) + still(20)
        assertEquals(listOf(TOWARD), run(pull, flat = false))
    }

    @Test fun shakingSideToSideDoesNothing() {
        val shake = List(80) { i -> Triple(22f * sin(i * 0.9f), 4f * sin(i * 0.9f), 0f) }
        assertEquals(emptyList<Int>(), run(shake))
    }

    @Test fun aBumpWithNoStopDoesNothing() {
        // Walking, or a knock in a pocket: a push that dies away without stopping hard.
        val bump = still(5) + listOf(Triple(0f, 16f, 2f), Triple(0f, 10f, 1f), Triple(0f, 4f, 0f), Triple(0f, 1f, 0f)) + still(60)
        assertEquals(emptyList<Int>(), run(bump))
        val back = still(5) + listOf(Triple(0f, -16f, 2f), Triple(0f, -10f, 1f), Triple(0f, -4f, 0f), Triple(0f, -1f, 0f)) + still(60)
        assertEquals(emptyList<Int>(), run(back))
    }

    @Test fun aGentleMoveDoesNothing() {
        val gentle = still(5) + listOf(Triple(0f, 6f, 0f), Triple(0f, 9f, 0f), Triple(0f, -7f, 0f)) + still(20)
        assertEquals(emptyList<Int>(), run(gentle))
    }

    @Test fun twoFlicksInARowFireOnce() {
        val flick = listOf(Triple(0f, 20f, 0f), Triple(0f, 24f, 0f), Triple(0f, -14f, 0f)) + still(10)
        assertEquals(listOf(AWAY), run(still(5) + flick + flick))
    }

    @Test fun theStopOfAFlickAwayIsNotAFlickBack() {
        // A hard stop and a rebound after it: one flick away, nothing toward.
        val flick = still(5) + listOf(Triple(0f, 22f, 0f), Triple(0f, 26f, 0f), Triple(0f, -24f, 0f), Triple(0f, -20f, 0f), Triple(0f, 12f, 0f), Triple(0f, 4f, 0f)) + still(30)
        assertEquals(listOf(AWAY), run(flick))
    }
}
