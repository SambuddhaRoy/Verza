package com.verza.data.deep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayersTest {
    private val n = 64

    /** A subject standing in front of a far wall, its outline softened a pixel the way the model's is. */
    private fun subject(): FloatArray {
        val d = FloatArray(n * n) { i -> if (i % n in 20..43 && i / n in 12..63) 0.9f else 0.1f }
        return blur(d, n, n, 1)
    }

    @Test fun otsuSplitsTwoDepthsBetweenThem() {
        val split = otsu(FloatArray(1000) { if (it < 600) 0.1f else 0.9f })
        assertTrue("split $split", split > 0.1f && split <= 0.9f)
    }

    @Test fun aSubjectInFrontOfAWallHasLayers() {
        val v = judge(subject(), n, n)
        assertTrue("$v", v.layered)
        assertEquals(0.30f, v.near, 0.05f)                               // 24 x 52 of 64 x 64
    }

    @Test fun aFloorFadingAwayHasNone() {
        // what the model gives a flat graphic: depth sloping smoothly from the bottom of the picture up
        val floor = FloatArray(n * n) { i -> (i / n) / (n - 1f) }
        assertFalse(judge(floor, n, n).layered)
    }

    @Test fun nothingInFrontIsNotLayered() {
        val wall = FloatArray(n * n) { 0.3f + 0.001f * (it % n) }
        assertFalse(judge(wall, n, n).layered)
    }

    @Test fun nearThingsGrowByTheRadius() {
        val d = FloatArray(n * n); d[30 * n + 30] = 1f
        val g = growNear(d, n, n, 2)
        assertEquals(25, g.count { it == 1f })
        assertEquals(1f, g[28 * n + 32], 0f)
        assertEquals(0f, g[27 * n + 30], 0f)
    }

    @Test fun pushPullFillsAHoleFromAroundItAndKeepsWhatItKnew() {
        val v = FloatArray(n * n) { i -> if (i % n < 32) 0.2f else 0.8f }
        val known = BooleanArray(n * n) { i -> !(i % n in 24..39 && i / n in 24..39) }
        val f = pushPull(v, n, n, known)
        for (i in v.indices) if (known[i]) assertEquals(v[i], f[i], 0f)
        val left = f[32 * n + 25]; val right = f[32 * n + 38]
        assertTrue("left $left right $right", left < right)                // the fill leans to each side's own value
        assertTrue(f.all { it in 0.19f..0.81f })                            // and never leaves the range around it
    }

    @Test fun pushPullWorksOnOddSizes() {
        val w = 37; val h = 23
        val v = FloatArray(w * h) { 0.5f }
        val known = BooleanArray(w * h) { it % 7 == 0 }
        assertTrue(pushPull(v, w, h, known).all { kotlin.math.abs(it - 0.5f) < 1e-5f })
    }
}
