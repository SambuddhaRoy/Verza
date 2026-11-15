package com.verza.playback

import kotlin.math.abs
import kotlin.math.max

/**
 * A flick of the phone: a sharp push that stops as sharply, either away from you ([AWAY], toward the
 * computer) or back toward you ([TOWARD]). Away is the phone's top edge (held flat, pointing at the
 * computer, +y) or away from your face (held up to look at, -z); toward is the opposite of either. Fed
 * the accelerometer, gravity included; gravity is followed by a slow low-pass and taken off, so this
 * needs no linear-acceleration sensor (which many phones and the emulator fake).
 *
 * A flick is: a push over [PUSH] m/s² along one of those, more than [DOMINANCE] times the strongest
 * other axis, then a stop (the same axis the other way, over [STOP]) within [WINDOW_NS]. A shake side
 * to side fails dominance, a bump in a pocket has no stop, and one flick can't fire twice inside
 * [COOLDOWN_NS]. The stop of a flick away is a hard pull back: it lands inside the push's window, so it
 * ends that flick rather than starting one toward you.
 *
 * thresholds from the physics of a wrist flick, not from a study; tune on a real phone.
 */
class Flick {
    private val gravity = FloatArray(3)
    private var settled = 0
    private var pushedAt = -1L
    private var alongY = true      // the push's axis: y (flat, pointing) or z (upright, facing you)
    private var sign = 1f          // which way along it the push went
    private var lastFired = Long.MIN_VALUE / 2

    /** One accelerometer reading, in m/s², at [timeNs]. [AWAY] or [TOWARD] when it completes a flick, else 0. */
    fun feed(x: Float, y: Float, z: Float, timeNs: Long): Int {
        if (settled < 10) { gravity[0] = x; gravity[1] = y; gravity[2] = z; settled++; return 0 }
        // Gravity moves slowly; a flick is over in a fifth of a second, so it barely shifts this.
        gravity[0] = gravity[0] * ALPHA + x * (1 - ALPHA)
        gravity[1] = gravity[1] * ALPHA + y * (1 - ALPHA)
        gravity[2] = gravity[2] * ALPHA + z * (1 - ALPHA)
        val lx = x - gravity[0]; val ly = y - gravity[1]; val lz = z - gravity[2]
        if (timeNs - lastFired < COOLDOWN_NS) return 0
        if (pushedAt >= 0) {
            if (timeNs - pushedAt > WINDOW_NS) pushedAt = -1
            else {
                // The stop: the same axis, the other way.
                if (-sign * (if (alongY) ly else lz) > STOP) {
                    pushedAt = -1; lastFired = timeNs
                    // +y is away from you; on z, away is -z.
                    return if ((if (alongY) sign else -sign) > 0) AWAY else TOWARD
                }
                return 0
            }
        }
        // The push: whichever way along y or z moves most this reading.
        val onY = abs(ly) >= abs(lz)
        val push = if (onY) abs(ly) else abs(lz)
        val others = if (onY) max(abs(lx), abs(lz)) else max(abs(lx), abs(ly))
        if (push > PUSH && push > others * DOMINANCE) {
            pushedAt = timeNs; alongY = onY
            sign = if ((if (onY) ly else lz) > 0) 1f else -1f
        }
        return 0
    }

    companion object {
        const val AWAY = 1
        const val TOWARD = -1
        const val PUSH = 14f
        const val STOP = 8f
        const val DOMINANCE = 1.6f
        const val ALPHA = 0.9f
        const val WINDOW_NS = 350_000_000L
        const val COOLDOWN_NS = 2_000_000_000L
    }
}
