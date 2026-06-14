package com.verza.audio

import kotlin.math.ln

/**
 * Graphic-EQ presets, defined as gain control points across the frequency spectrum (Hz → millibels)
 * and mapped onto whatever band layout the device actually exposes via log-frequency interpolation.
 * That way a preset reads the same whether the phone's equaliser has 5 bands or 10 — we sample the
 * preset's curve at each real band's centre frequency rather than hard-coding per-band values.
 */
enum class EqPreset(val displayName: String, private val points: List<Pair<Int, Int>>) {
    FLAT       ("Flat",       listOf(60 to 0, 14000 to 0)),
    ACOUSTIC   ("Acoustic",   listOf(60 to 300, 230 to 150, 910 to 0, 3600 to 200, 14000 to 250)),
    BASS_BOOST ("Bass",       listOf(60 to 700, 150 to 500, 400 to 200, 1000 to 0, 14000 to 0)),
    BASS_CUT   ("Bass cut",   listOf(60 to -650, 150 to -400, 400 to -100, 1000 to 0, 14000 to 0)),
    TREBLE     ("Treble",     listOf(60 to 0, 1000 to 0, 3600 to 350, 8000 to 550, 14000 to 650)),
    VOCAL      ("Vocal",      listOf(60 to -200, 230 to -100, 910 to 300, 3600 to 400, 8000 to 150, 14000 to -100)),
    ROCK       ("Rock",       listOf(60 to 500, 230 to 300, 910 to -100, 3600 to 250, 14000 to 450)),
    POP        ("Pop",        listOf(60 to -100, 230 to 200, 910 to 450, 3600 to 200, 14000 to -100)),
    JAZZ       ("Jazz",       listOf(60 to 400, 230 to 200, 910 to -150, 3600 to 200, 14000 to 350)),
    CLASSICAL  ("Classical",  listOf(60 to 400, 230 to 300, 910 to -100, 3600 to 250, 8000 to 350, 14000 to 400)),
    ELECTRONIC ("Electronic", listOf(60 to 550, 230 to 250, 910 to -100, 3600 to 100, 8000 to 300, 14000 to 450)),
    LOUNGE     ("Lounge",     listOf(60 to -150, 230 to 100, 910 to 300, 3600 to 150, 14000 to 100)),
    LATE_NIGHT ("Late night", listOf(60 to -300, 230 to -150, 910 to 100, 3600 to 0, 14000 to -250)),
    ;

    /** The per-band millibel gains for this preset on the given device layout. */
    fun levelsFor(md: EqMetadata): List<Int> =
        md.bands.map { interpolateMb(it.centerFreqHz).coerceIn(md.minLevelMb, md.maxLevelMb) }

    private fun interpolateMb(freqHz: Int): Int {
        val sorted = points.sortedBy { it.first }
        val f = freqHz.coerceAtLeast(1).toDouble()
        if (f <= sorted.first().first) return sorted.first().second
        if (f >= sorted.last().first) return sorted.last().second
        for (i in 0 until sorted.size - 1) {
            val (f0, g0) = sorted[i]
            val (f1, g1) = sorted[i + 1]
            if (f >= f0 && f <= f1) {
                val t = (ln(f) - ln(f0.toDouble())) / (ln(f1.toDouble()) - ln(f0.toDouble()))
                return (g0 + t * (g1 - g0)).toInt()
            }
        }
        return 0
    }

    companion object {
        fun byName(name: String?): EqPreset? =
            name?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}
