package com.verza.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sin

/** Synthetic accelerometer traces at 100Hz, gravity included (phone flat on a hand: z = 9.8). */
class FlickTest {
    private val g = 9.81f
    private val step = 10_000_000L

    /** Feeds [readings] (x, y, z extra over gravity), returns how many flicks fired. */
    private fun run(readings: List<Triple<Float, Float, Float>>, flat: Boolean = true): Int {
        val f = Flick()
        var t = 0L; var n = 0
        val base = if (flat) Triple(0f, 0f, g) else Triple(0f, g, 0f)   // flat, or held upright facing you
        repeat(30) { f.feed(base.first, base.second, base.third, t); t += step }
        for ((x, y, z) in readings) { if (f.feed(base.first + x, base.second + y, base.third + z, t)) n++; t += step }
        return n
    }
    private fun still(n: Int) = List(n) { Triple(0f, 0f, 0f) }

    @Test fun aFlickTowardTheTopEdgeFires() {
        val flick = still(5) + listOf(Triple(1f, 8f, 0f), Triple(1f, 20f, 1f), Triple(0f, 24f, 1f), Triple(0f, 6f, 0f), Triple(0f, -14f, 0f), Triple(0f, -6f, 0f)) + still(20)
        assertEquals(1, run(flick))
    }

    @Test fun aPushAwayFromYouFires() {
        val push = still(5) + listOf(Triple(0f, 0f, -10f), Triple(0f, 1f, -22f), Triple(0f, 0f, -18f), Triple(0f, 0f, 14f), Triple(0f, 0f, 4f)) + still(20)
        assertEquals(1, run(push, flat = false))
    }

    @Test fun shakingSideToSideDoesNot() {
        val shake = List(80) { i -> Triple(22f * sin(i * 0.9f), 4f * sin(i * 0.9f), 0f) }
        assertEquals(0, run(shake))
    }

    @Test fun aBumpWithNoStopDoesNot() {
        // Walking, or a knock in a pocket: a push that dies away without stopping hard.
        val bump = still(5) + listOf(Triple(0f, 16f, 2f), Triple(0f, 10f, 1f), Triple(0f, 4f, 0f), Triple(0f, 1f, 0f)) + still(60)
        assertEquals(0, run(bump))
    }

    @Test fun aGentleMoveDoesNot() {
        val gentle = still(5) + listOf(Triple(0f, 6f, 0f), Triple(0f, 9f, 0f), Triple(0f, -7f, 0f)) + still(20)
        assertEquals(0, run(gentle))
    }

    @Test fun twoFlicksInARowFireOnce() {
        val flick = listOf(Triple(0f, 20f, 0f), Triple(0f, 24f, 0f), Triple(0f, -14f, 0f)) + still(10)
        assertEquals(1, run(still(5) + flick + flick))
    }
}
