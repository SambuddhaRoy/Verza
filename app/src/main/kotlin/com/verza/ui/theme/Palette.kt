package com.verza.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * The colours of a page: true black or white with the system's dark mode, the faintest breath of the
 * playing cover's hue in the surfaces, and one accent from the cover. Every ink is measured against
 * what it sits on and held above [MIN_CONTRAST]; the cover picks hues, never legibility.
 * A colourless cover, or none, gets the Zune's magenta: a grey accent reads as a disabled one.
 */
@Immutable
data class Palette(
    val canvas: Color,
    val ink: Color,
    val inkMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val line: Color,
) {
    val dark: Boolean get() = canvas.luminance() < 0.5f

    companion object {
        const val MIN_CONTRAST = 4.5f
        val Magenta = Color(0xFFE6007E)

        /** [swatches] are the cover's colours (Palette swatches), empty for no cover or a colourless one. */
        fun from(swatches: List<Color>, dark: Boolean): Palette {
            val vivid = swatches.maxByOrNull { val c = hsv(it); c[1] * (0.4f + c[2]) }
                ?.takeIf { hsv(it)[1] > 0.12f }
            val seed = hsv(vivid ?: Magenta)
            val hue = seed[0]
            val tint = if (vivid == null) 0f else 1f
            fun neutral(sat: Float, value: Float) = fromHsv(hue, sat * tint, value)
            val canvas = if (dark) Color.Black else Color.White
            val ink = if (dark) Color.White else Color.Black
            val accent = accentFor(hue, if (vivid == null) 0.85f else seed[1], canvas, dark)
            return Palette(
                canvas = canvas,
                ink = ink,
                inkMuted = secondaryOn(canvas, ink),
                accent = accent,
                onAccent = readableOn(accent),
                surface = if (dark) neutral(0.16f, 0.12f) else neutral(0.035f, 0.955f),
                surfaceHigh = if (dark) neutral(0.14f, 0.17f) else neutral(0.04f, 0.915f),
                line = ink.copy(alpha = if (dark) 0.14f else 0.10f),
            )
        }
    }
}

/**
 * An accent of [hue] that reads on [canvas]: value first, then saturation, which keeps blues blue on
 * black (they go paler before darker) and yellows warm on white.
 */
private fun accentFor(hue: Float, sat: Float, canvas: Color, dark: Boolean): Color {
    var s = sat.coerceIn(0.55f, 0.9f)
    while (s >= 0.2f) {
        var v = if (dark) 0.98f else 0.86f
        while (if (dark) v >= 0.6f else v >= 0.25f) {
            val c = fromHsv(hue, s, v)
            if (contrastRatio(c, canvas) >= Palette.MIN_CONTRAST) return c
            v -= 0.02f
        }
        s -= 0.05f
    }
    return if (dark) Color.White else Color.Black
}

/** Secondary text: the ink taken about 40% of the way to the ground, never past the floor. */
private fun secondaryOn(bg: Color, ink: Color): Color {
    var t = 0.42f
    while (t > 0f) {
        val c = lerp(ink, bg, t)
        if (contrastRatio(bg, c) >= Palette.MIN_CONTRAST) return c
        t -= 0.02f
    }
    return ink
}

/** White if it clears the floor on [bg], else a dark shade of [bg]'s own hue that does. */
fun readableOn(bg: Color): Color {
    if (contrastRatio(bg, Color.White) >= Palette.MIN_CONTRAST) return Color.White
    val h = hsv(bg)
    var v = 0.30f
    while (v > 0.02f) {
        val c = fromHsv(h[0], (h[1] * 0.8f).coerceAtMost(0.9f), v)
        if (contrastRatio(bg, c) >= Palette.MIN_CONTRAST) return c
        v -= 0.02f
    }
    return Color.Black
}

fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

// In plain Kotlin rather than android.graphics, so the contrast rules run in JVM tests.
internal fun hsv(c: Color): FloatArray {
    val max = maxOf(c.red, c.green, c.blue)
    val d = max - minOf(c.red, c.green, c.blue)
    val h = when {
        d == 0f -> 0f
        max == c.red -> 60f * (((c.green - c.blue) / d) % 6f)
        max == c.green -> 60f * (((c.blue - c.red) / d) + 2f)
        else -> 60f * (((c.red - c.green) / d) + 4f)
    }
    return floatArrayOf(if (h < 0f) h + 360f else h, if (max == 0f) 0f else d / max, max)
}

internal fun fromHsv(h: Float, s: Float, v: Float): Color {
    val sat = s.coerceIn(0f, 1f)
    val value = v.coerceIn(0f, 1f)
    val hh = ((h % 360f) + 360f) % 360f
    val c = value * sat
    val x = c * (1f - kotlin.math.abs((hh / 60f) % 2f - 1f))
    val m = value - c
    val (r, g, b) = when ((hh / 60f).toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
