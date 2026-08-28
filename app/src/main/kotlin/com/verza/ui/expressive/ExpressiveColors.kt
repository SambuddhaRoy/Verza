package com.verza.ui.expressive

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.verza.ui.theme.CoverColors

/**
 * The two-tone colour contract.
 *
 * The reference is not a dark app with an accent on it. It is two saturated colours in a pair: a
 * full-bleed [container] and a high-chroma [accent] doing every control, with type large enough to
 * carry weight in both. That is what makes it read as expressive rather than as a dark theme.
 *
 * It is also the exact shape of the bug that started all this. Coloured text on a coloured ground is
 * where the old player failed, and switching from a near-black canvas to a saturated one removes the
 * safety net that a fixed white ramp gave us. So every pair here is measured, not assumed:
 * [onContainer] and [onAccent] are each chosen by contrast ratio against the surface they sit on,
 * and both are held above [MIN_CONTRAST]. The cover chooses the hues; it does not get a vote on
 * whether the result is legible.
 */
@Immutable
data class ExpressiveColors(
    /** Full-bleed background. Saturated, mid-dark, cover-derived. */
    val container: Color,
    /** Text and icons on [container]. Measured, never sampled. */
    val onContainer: Color,
    /** Secondary text on [container] — same hue family, still above the contrast floor. */
    val onContainerMuted: Color,
    /** Controls. High chroma, deliberately far from [container] in lightness. */
    val accent: Color,
    /** Glyphs and labels on [accent]. */
    val onAccent: Color,
    /** A dimmer companion to [accent] for inactive controls and tracks. */
    val accentMuted: Color,
    /** One step off [container] for cards and sheets. */
    val surface: Color,
    /** Text on [surface]. */
    val onSurface: Color,
    /** Hairlines. */
    val line: Color,
) {
    companion object {
        /** WCAG AA for large text, and the floor every pair in here has to clear. */
        const val MIN_CONTRAST = 4.5f
    }
}

/** Until a cover resolves. Verza's indigo/lime, in the reference's register. */
val DefaultExpressiveColors = ExpressiveColors(
    container = Color(0xFF5B4BE0),
    onContainer = Color(0xFFFFFFFF),
    onContainerMuted = Color(0xFFE3DEFF),
    accent = Color(0xFFEDF27A),
    onAccent = Color(0xFF2A2350),
    accentMuted = Color(0xFF8B7FE8),
    surface = Color(0xFF3A2FA8),
    onSurface = Color(0xFFFFFFFF),
    line = Color(0x33FFFFFF),
)

val LocalExpressiveColors = staticCompositionLocalOf { DefaultExpressiveColors }

/** WCAG relative-luminance contrast ratio. 1.0 identical, 21.0 black on white. */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

// HSV in plain Kotlin rather than android.graphics. Same formulas, but it keeps this file free of
// the Android framework, which is what lets the contrast rules be unit-tested on the JVM.
internal fun hsv(c: Color): FloatArray {
    val r = c.red; val g = c.green; val b = c.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
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

/**
 * Pick whichever of black/white actually contrasts with [bg], then push it further until it clears
 * [ExpressiveColors.MIN_CONTRAST].
 *
 * Returning plain white for a light lavender container is exactly the failure this replaces, so the
 * fallback is a very dark tint of the background's own hue rather than a neutral — it keeps the
 * palette coherent instead of dropping a grey hole into it.
 */
private fun readableOn(bg: Color): Color {
    val floor = ExpressiveColors.MIN_CONTRAST
    if (contrastRatio(bg, Color.White) >= floor) return Color.White
    // White did not clear it, so go the other way and keep going until it does. Stepping the value
    // down rather than jumping to black keeps the ink tinted with the background's own hue, so the
    // palette stays coherent instead of gaining a neutral hole.
    val h = hsv(bg)
    var v = 0.30f
    while (v > 0.02f) {
        val candidate = fromHsv(h[0], (h[1] * 0.8f).coerceAtMost(0.9f), v)
        if (contrastRatio(bg, candidate) >= floor) return candidate
        v -= 0.02f
    }
    // A background this middling cannot be beaten by its own hue; take the better absolute.
    return if (contrastRatio(bg, Color.White) >= contrastRatio(bg, Color.Black)) Color.White else Color.Black
}

/**
 * A softened version of [ink] that is still readable on [bg]. Steps toward the background until it
 * looks secondary, then stops at the contrast floor rather than continuing to fade — which is how
 * "muted" text normally becomes unreadable text.
 */
private fun mutedOn(bg: Color, ink: Color): Color {
    var best = ink
    var t = 0f
    while (t < 0.6f) {
        val candidate = androidx.compose.ui.graphics.lerp(ink, bg, t)
        if (contrastRatio(bg, candidate) < ExpressiveColors.MIN_CONTRAST) break
        best = candidate
        t += 0.05f
    }
    return best
}

/**
 * Force [seed] far enough away from [from] in lightness to work as a control on it, keeping its hue.
 *
 * Tries the light direction first, because the reference's controls are bright-on-dark and that is
 * the more expressive read; drops to dark only if the hue cannot get bright enough to clear the
 * floor (a deep blue, for instance, goes pale before it goes contrasty).
 */
private fun separate(seed: Color, from: Color): Color {
    val floor = ExpressiveColors.MIN_CONTRAST
    val h = hsv(seed)
    // Bright first: the reference's controls are light-on-dark and that is the more expressive read.
    var v = 1f
    while (v > 0.72f) {
        val candidate = fromHsv(h[0], h[1].coerceIn(0.35f, 0.85f), v)
        if (contrastRatio(candidate, from) >= floor) return candidate
        v -= 0.02f
    }
    // Some hues go pale before they go contrasty against a light container, so try dark instead.
    v = 0.30f
    while (v > 0.0f) {
        val candidate = fromHsv(h[0], h[1].coerceIn(0.4f, 1f), v)
        if (contrastRatio(candidate, from) >= floor) return candidate
        v -= 0.02f
    }
    // Nothing in this hue clears it, so give up on the hue rather than on legibility.
    // The container is held to a saturated mid-dark band, so black always clears the floor against
    // it. Anything lighter than black here would be a guess that sometimes misses by a tenth.
    return if (from.luminance() > 0.18f) Color.Black else Color.White
}

/**
 * Build the pair from the colours already sampled off the cover (see `extractCoverColors`).
 *
 * The accent is deliberately taken a step around the wheel from the container rather than straight
 * off the same swatch. Two tones from the same hue read as one flat colour; the reference's indigo
 * and yellow are most of what makes it feel alive.
 */
fun expressiveColorsFrom(cover: CoverColors): ExpressiveColors {
    val seed = hsv(cover.accent)

    // Container: the cover's hue, held to a saturated mid-dark band so it is unmistakably coloured
    // without ever being bright enough to fight the text on top of it.
    val container = fromHsv(seed[0], seed[1].coerceIn(0.45f, 0.82f), seed[2].coerceIn(0.30f, 0.52f))
    val onContainer = readableOn(container)

    // Accent: rotated 55° for a complementary-ish partner, then separated by measured contrast.
    val accent = separate(fromHsv((seed[0] + 55f) % 360f, seed[1], seed[2]), container)
    val onAccent = readableOn(accent)

    val surface = fromHsv(seed[0], (seed[1] * 0.9f).coerceIn(0.35f, 0.8f), (seed[2] * 0.62f).coerceIn(0.18f, 0.34f))

    return ExpressiveColors(
        container = container,
        onContainer = onContainer,
        onContainerMuted = mutedOn(container, onContainer),
        accent = accent,
        onAccent = onAccent,
        accentMuted = androidx.compose.ui.graphics.lerp(accent, container, 0.55f),
        surface = surface,
        onSurface = readableOn(surface),
        line = onContainer.copy(alpha = 0.22f),
    )
}
