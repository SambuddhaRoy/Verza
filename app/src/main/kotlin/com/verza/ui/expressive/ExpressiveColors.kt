package com.verza.ui.expressive

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.verza.ui.theme.CoverColors

/**
 * The colour contract for the expressive UI.
 *
 * The old player let the cover decide everything, including the colour of text, and that is why it
 * was hard to read: a cover with a dark navy vibrant swatch produced navy text on a near-black
 * background, and nothing in the pipeline noticed. Here the split is deliberate and absolute.
 *
 * The cover decides [accent] and [glow] — the parts that are meant to feel like the record.
 * It decides nothing about text. [ink], [inkMuted] and [inkFaint] are a fixed white ramp on a
 * near-black canvas, so the worst case is still about 19:1. Readability stops being a property of
 * the album art.
 *
 * [accent] itself is not the raw swatch either. It is pushed to a saturation and lightness floor,
 * so a muddy cover still yields something vivid enough to carry a control, and [onAccent] is then
 * chosen by measured contrast rather than assumed to be black.
 */
@Immutable
data class ExpressiveColors(
    /** Vivid, cover-derived. Fills the primary controls. */
    val accent: Color,
    /** Black or white, whichever actually contrasts with [accent]. */
    val onAccent: Color,
    /** Near-black, faintly tinted by the cover so the screen feels related to the record. */
    val canvas: Color,
    /** One step up from [canvas] for secondary controls and cards. */
    val elevated: Color,
    /** The ambient wash behind everything. Cover-derived, low alpha, never behind text. */
    val glow: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    /** Hairlines and inactive tracks. */
    val line: Color,
)

/** Used until a cover resolves. Verza's own green, so a cold start still looks like Verza. */
val DefaultExpressiveColors = ExpressiveColors(
    accent = Color(0xFFB9F227),
    onAccent = Color(0xFF10160A),
    canvas = Color(0xFF080A07),
    elevated = Color(0xFF1A1D18),
    glow = Color(0xFFB9F227),
    ink = Color(0xFFFFFFFF),
    inkMuted = Color(0xFFFFFFFF).copy(alpha = 0.68f),
    inkFaint = Color(0xFFFFFFFF).copy(alpha = 0.42f),
    line = Color(0xFFFFFFFF).copy(alpha = 0.14f),
)

val LocalExpressiveColors = staticCompositionLocalOf { DefaultExpressiveColors }

/** WCAG relative-luminance contrast ratio. 1.0 is identical, 21.0 is black on white. */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/**
 * Lift a cover swatch into something that can carry a control on a near-black screen.
 *
 * Two floors, both load-bearing. Saturation, because a desaturated swatch reads as grey and the
 * whole screen goes flat. Value, because the accent is a *fill* with glyphs on top — a dark accent
 * leaves no room for a legible symbol in either black or white, whatever we pick for [onAccent].
 */
private fun vivid(seed: Color): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(seed.toArgb(), hsv)
    hsv[1] = hsv[1].coerceIn(0.55f, 1f)
    hsv[2] = hsv[2].coerceIn(0.78f, 1f)
    return Color(AndroidColor.HSVToColor(hsv))
}

/** A near-black canvas that keeps a trace of the cover's hue without ever approaching the ink. */
private fun canvasFrom(seed: Color): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(seed.toArgb(), hsv)
    // Chroma is kept low and value very low on purpose: this sits under white text on every screen.
    return Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], (hsv[1] * 0.5f).coerceAtMost(0.35f), 0.045f)))
}

private fun elevatedFrom(seed: Color): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(seed.toArgb(), hsv)
    return Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], (hsv[1] * 0.35f).coerceAtMost(0.22f), 0.13f)))
}

/**
 * Build the expressive palette from the colours already sampled off the cover art
 * (see `extractCoverColors`), so this adds a contrast contract rather than a second extractor.
 */
fun expressiveColorsFrom(cover: CoverColors): ExpressiveColors {
    val accent = vivid(cover.accent)
    // Measured, not assumed. A cover that lands on deep violet gets white glyphs; a lime gets black.
    val onAccent = if (contrastRatio(accent, Color.Black) >= contrastRatio(accent, Color.White)) {
        Color(0xFF0B0D08)
    } else {
        Color.White
    }
    return ExpressiveColors(
        accent = accent,
        onAccent = onAccent,
        canvas = canvasFrom(cover.bg),
        elevated = elevatedFrom(cover.bg),
        glow = accent,
        ink = DefaultExpressiveColors.ink,
        inkMuted = DefaultExpressiveColors.inkMuted,
        inkFaint = DefaultExpressiveColors.inkFaint,
        line = DefaultExpressiveColors.line,
    )
}
