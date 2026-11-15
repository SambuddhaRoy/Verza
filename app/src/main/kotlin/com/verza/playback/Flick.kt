package com.verza.playback

import kotlin.math.abs
import kotlin.math.max

/**
 * A flick of the phone toward something: a sharp push forward that stops as sharply. Forward is the
 * phone's top edge (held flat, pointing at the computer, +y) or away from your face (held up to look
 * at, -z). Fed the accelerometer, gravity included; gravity is followed by a slow low-pass and taken
 * off, so this needs no linear-acceleration sensor (which many phones and the emulator fake).
 *
 * A flick is: forward over [PUSH] m/s², more than [DOMINANCE] times the strongest other axis, then a
 * stop (forward under -[STOP]) within [WINDOW_NS]. A shake side to side fails dominance, a bump in a
 * pocket has no stop, and one flick can't fire twice inside [COOLDOWN_NS].
 *
 * ponytail: thresholds from the physics of a wrist flick, not from a study; tune on a real phone.
 */
class Flick {
    private val gravity = FloatArray(3)
    private var settled = 0
    private var pushedAt = -1L
    private var towardTop = true
    private var lastFired = Long.MIN_VALUE / 2

    /** One accelerometer reading, in m/s², at [timeNs]. True when it completes a flick. */
    fun feed(x: Float, y: Float, z: Float, timeNs: Long): Boolean {
        if (settled < 10) { gravity[0] = x; gravity[1] = y; gravity[2] = z; settled++; return false }
        // Gravity moves slowly; a flick is over in a fifth of a second, so it barely shifts this.
        gravity[0] = gravity[0] * ALPHA + x * (1 - ALPHA)
        gravity[1] = gravity[1] * ALPHA + y * (1 - ALPHA)
        gravity[2] = gravity[2] * ALPHA + z * (1 - ALPHA)
        val lx = x - gravity[0]; val ly = y - gravity[1]; val lz = z - gravity[2]
        if (timeNs - lastFired < COOLDOWN_NS) return false
        // Forward is whichever of the two directions moves most this reading.
        val top = ly; val away = -lz
        val forward = max(top, away)
        val others = if (top >= away) max(abs(lx), abs(lz)) else max(abs(lx), abs(ly))
        if (forward > PUSH && forward > others * DOMINANCE) { if (pushedAt < 0) { pushedAt = timeNs; towardTop = top >= away }; return false }
        if (pushedAt >= 0) {
            if (timeNs - pushedAt > WINDOW_NS) { pushedAt = -1; return false }
            // The stop: the same axis, the other way.
            if ((if (towardTop) -ly else lz) > STOP) { pushedAt = -1; lastFired = timeNs; return true }
        }
        return false
    }

    companion object {
        const val PUSH = 14f
        const val STOP = 8f
        const val DOMINANCE = 1.6f
        const val ALPHA = 0.9f
        const val WINDOW_NS = 350_000_000L
        const val COOLDOWN_NS = 2_000_000_000L
    }
}
