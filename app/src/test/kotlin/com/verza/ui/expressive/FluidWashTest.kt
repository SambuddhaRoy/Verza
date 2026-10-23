package com.verza.ui.expressive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * When [BeatPulse] calls something a beat, at the wash's 30 frames a second. The inputs are shaped
 * like the visualizer's real power under 150Hz: 0.001 to 0.05 between kicks, 0.1 to 0.4 on one,
 * wobbling while it rings.
 */
class FluidWashTest {

    private val dt = 1f / 30f
    private val rng = Random(7)
    private fun jitter(max: Float) = rng.nextFloat() * max

    /** Feed [power] for [frames] frames and count the beats fired. */
    private fun BeatPulse.beats(frames: Int, power: (Int) -> Float): Int {
        var n = 0
        repeat(frames) { f ->
            val before = kick
            step(0.77f, power(f), dt)
            if (kick == 1f && before < 1f) n++
        }
        return n
    }

    /** A kick lasting [len] frames out of every [period], wobbling between [lo] and [hi]. */
    private fun kicks(period: Int, len: Int, lo: Float, hi: Float, floor: Float): (Int) -> Float =
        { f -> if (f % period < len) lo + jitter(hi - lo) else jitter(floor) }

    @Test
    fun `a kick every half second is a beat every half second`() {
        val p = BeatPulse()
        p.beats(60) { jitter(0.03f) }
        assertEquals(10, p.beats(150, kicks(15, 4, 0.1f, 0.4f, 0.03f)))
    }

    @Test
    fun `a kick that dips while it rings is still one beat`() {
        val p = BeatPulse()
        p.beats(60) { jitter(0.03f) }
        // Each kick: strong, a dip near the floor, strong again, over a fifth of a second.
        val ring = floatArrayOf(0.3f, 0.25f, 0.03f, 0.29f, 0.2f, 0.15f)
        assertEquals(5, p.beats(150) { f -> if (f % 30 < ring.size) ring[f % 30] else jitter(0.02f) })
    }

    @Test
    fun `a quiet song pulses on its kicks too`() {
        val p = BeatPulse()
        p.beats(60) { jitter(0.004f) }
        assertEquals(5, p.beats(75, kicks(15, 3, 0.03f, 0.05f, 0.004f)))
    }

    @Test
    fun `the noise between kicks is not a beat`() {
        val p = BeatPulse()
        p.beats(60) { jitter(0.03f) }
        assertEquals(0, p.beats(300) { jitter(0.03f) })
    }

    @Test
    fun `a held bass note fires once, not continuously`() {
        val p = BeatPulse()
        p.beats(60) { 0.01f }
        assertEquals(1, p.beats(90) { 0.3f })
    }

    @Test
    fun `silence never fires, and the pulse dies away`() {
        val p = BeatPulse()
        assertEquals(0, p.beats(60) { 0f })
        p.beats(3) { 0.3f }
        repeat(30) { p.step(0f, 0f, dt) }
        assertTrue(p.kick < 0.01f)
        assertTrue(p.level < 0.01f)
    }
}
