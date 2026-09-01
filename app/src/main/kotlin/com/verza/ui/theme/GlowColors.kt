package com.verza.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware

/**
 * The three colours the fluid glow shader mixes across its field. Keeping it a triad (rather
 * than a single colour) is what lets the effect read as multi-tonal and alive instead of a
 * flat monochrome wash.
 */
data class GlowTriad(val a: Color, val b: Color, val c: Color)

private fun hsv(h: Float, s: Float, v: Float): Color =
    Color(AndroidColor.HSVToColor(floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

/** Bumps a colour's saturation up to a floor so dull album swatches still produce a visible glow. */
private fun floorSaturation(color: Color, floor: Float = 0.42f): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    if (hsv[1] >= floor) return color
    hsv[1] = floor
    return Color(AndroidColor.HSVToColor(hsv))
}

/**
 * Extracts a [GlowTriad] from the album/song cover at [url] using AndroidX Palette.
 *
 * Loads a small (160 px) software bitmap via the shared Coil image loader (reusing its cache),
 * runs Palette, and picks the three most distinct vibrant swatches — preferring vibrant /
 * light-vibrant / dark-vibrant, then muted / dominant as backfill. Returns null on any failure
 * (no network, decode error, no swatches) so the caller can fall back to the theme triad.
 *
 * `allowHardware(false)` is required: Palette must read pixels, which hardware bitmaps forbid.
 */
// Shared cover-bitmap loader (software bitmap so Palette can read pixels; reuses Coil's cache).
private suspend fun loadCoverBitmap(context: Context, url: String): android.graphics.Bitmap? {
    val loader = SingletonImageLoader.get(context)
    val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(160).build()
    val result = runCatching { loader.execute(request) }.getOrNull() as? SuccessResult ?: return null
    return (result.image as? BitmapImage)?.bitmap
}

// ── Cover-derived full palette ────────────────────────────────────────────────
// A complete set of editorial tones sampled from the cover art: a near-black canvas
// *tinted* by the cover's dark tones, a warm near-white ink, and the vibrant accent.
// Used by the Sleeve appearance for every surface, and as the basis of the Adaptive theme.
data class CoverColors(
    val accent: Color,
    val bg: Color,
    val ink: Color,
    val sub: Color,
    val faint: Color,
    val line: Color,
    /**
     * Every colour the palette pulled out of the cover, most prominent first.
     *
     * Kept so the accent can be chosen from the artwork rather than computed off the wheel. Empty
     * for the built-in defaults, which is the signal to fall back to the complement.
     */
    val swatches: List<Color> = emptyList(),
)

/** UMBRA "Terracotta"-style defaults, used until a cover resolves. */
val DefaultCoverColors = CoverColors(
    accent = Color(0xFFCF6A3C),
    bg = Color(0xFF0B0705),
    ink = Color(0xFFF2E9DD),
    sub = Color(0xFFF2E9DD).copy(alpha = 0.62f),
    faint = Color(0xFFF2E9DD).copy(alpha = 0.34f),
    line = Color(0xFFF2E9DD).copy(alpha = 0.16f),
)

/**
 * Editorial surface palette for the **Sleeve chrome** (Home / Library / Settings / nav / mini-player).
 * Derived from the *active theme scheme* (see [coverColorsFromScheme]) so switching themes — and
 * light vs dark — actually recolours Sleeve. (The Now-Playing poster instead uses [LocalArtworkColors].)
 */
val LocalCoverColors = staticCompositionLocalOf { DefaultCoverColors }

/**
 * Palette sampled from the **current track's cover art**, used by the Now-Playing poster, the
 * ambient display and the share cards — surfaces that sit *on top of the artwork* and therefore need
 * light ink over a darkened cover for contrast regardless of the app's light/dark theme.
 */
val LocalArtworkColors = staticCompositionLocalOf { DefaultCoverColors }

/**
 * The chrome palette every not-yet-rewritten screen reads through LocalCoverColors.
 *
 * It used to come from the Material scheme, which is why those screens still looked like the old app
 * after the player was rebuilt. Mapping it onto the expressive palette instead means they pick up the
 * cover-derived colour and its measured contrast without each one being rewritten to say so.
 */
fun coverColorsFromExpressive(
    container: Color,
    onContainer: Color,
    onContainerMuted: Color,
    accent: Color,
    line: Color,
): CoverColors = CoverColors(
    accent = accent,
    bg = container,
    ink = onContainer,
    sub = onContainerMuted,
    faint = onContainerMuted.copy(alpha = 0.6f),
    line = line,
)

/** A near-black canvas that keeps a faint hint of the cover's hue. */
private fun darkCanvasFrom(c: Color): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(c.toArgb(), hsv)
    return Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], (hsv[1] * 0.6f).coerceAtMost(0.5f), 0.07f)))
}

/**
 * Builds a full [CoverColors] palette from the cover at [url]: vibrant swatch → accent,
 * a dark swatch → tinted near-black canvas, warm near-white ink. Returns null on failure.
 */
suspend fun extractCoverColors(context: Context, url: String): CoverColors? {
    val bitmap = loadCoverBitmap(context, url) ?: return null
    val palette = runCatching { Palette.from(bitmap).maximumColorCount(24).generate() }.getOrNull() ?: return null

    val accentSwatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch
        ?: palette.darkVibrantSwatch ?: palette.dominantSwatch ?: return null
    val darkSwatch = palette.darkMutedSwatch ?: palette.darkVibrantSwatch
        ?: palette.mutedSwatch ?: palette.dominantSwatch ?: accentSwatch

    val accent = floorSaturation(Color(accentSwatch.rgb))
    val bg = darkCanvasFrom(Color(darkSwatch.rgb))
    val ink = Color(0xFFF2E9DD)
    // Ordered by population so the first candidates are the colours the cover is actually made of,
    // not a stray pixel in a corner.
    val swatches = palette.swatches
        .sortedByDescending { it.population }
        .map { Color(it.rgb) }
    return CoverColors(
        swatches = swatches,
        accent = accent,
        bg = bg,
        ink = ink,
        sub = ink.copy(alpha = 0.62f),
        faint = ink.copy(alpha = 0.34f),
        line = ink.copy(alpha = 0.16f),
    )
}
