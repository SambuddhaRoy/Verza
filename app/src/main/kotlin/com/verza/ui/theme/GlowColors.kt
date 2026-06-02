package com.verza.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
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

/**
 * Builds a vibrant, harmonious triad from a single seed colour by spreading the hue ±~30° and
 * floor-boosting saturation when the seed is dull.
 *
 * This is the fix for the "Material You looks bland/monochrome" problem: the dynamic scheme's
 * primary is often low-saturation and tonally close to its tertiary, so feeding raw theme roles
 * into the shader collapsed it toward grey. Deriving an analogous triad from one seed — with a
 * saturation rescue that only triggers on dull seeds — keeps vivid presets (amber, teal) intact
 * while giving Material You and muted palettes a lively, colourful field.
 */
fun deriveGlowTriad(seed: Color): GlowTriad {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(seed.toArgb(), hsv)
    val h = hsv[0]
    // Rescue dull seeds (e.g. desaturated Material You primary) so the field isn't grey.
    val s = if (hsv[1] < 0.40f) 0.58f else hsv[1]
    // Keep the value in a band that reads on a dark background — too dark and the glow vanishes.
    val v = hsv[2].coerceIn(0.55f, 1f)

    return GlowTriad(
        a = hsv(h, s, v),
        b = hsv((h + 28f) % 360f, (s * 0.92f), (v * 1.08f).coerceAtMost(1f)),
        c = hsv((h - 34f + 360f) % 360f, (s * 1.12f).coerceAtMost(1f), v * 0.86f),
    )
}

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

suspend fun extractAlbumTriad(context: Context, url: String): GlowTriad? {
    val bitmap = loadCoverBitmap(context, url) ?: return null

    val palette = runCatching {
        Palette.from(bitmap).maximumColorCount(24).generate()
    }.getOrNull() ?: return null

    val swatches = listOfNotNull(
        palette.vibrantSwatch,
        palette.lightVibrantSwatch,
        palette.darkVibrantSwatch,
        palette.mutedSwatch,
        palette.lightMutedSwatch,
        palette.darkMutedSwatch,
        palette.dominantSwatch,
    ).map { Color(it.rgb) }.distinct()

    val a = swatches.getOrNull(0) ?: return null
    // Backfill missing slots with a hue-spread of the lead colour so we always have three.
    val derived by lazy { deriveGlowTriad(a) }
    val b = swatches.getOrNull(1) ?: derived.b
    val c = swatches.getOrNull(2) ?: derived.c

    return GlowTriad(floorSaturation(a), floorSaturation(b), floorSaturation(c))
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

val LocalCoverColors = staticCompositionLocalOf { DefaultCoverColors }

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
    return CoverColors(
        accent = accent,
        bg = bg,
        ink = ink,
        sub = ink.copy(alpha = 0.62f),
        faint = ink.copy(alpha = 0.34f),
        line = ink.copy(alpha = 0.16f),
    )
}

/** Best-contrast on-colour (black or white) for text/icons drawn on [bg]. */
private fun onColorFor(bg: Color): Color = if (bg.luminance() > 0.5f) Color(0xFF120A06) else Color.White

/**
 * A full dark M3 [ColorScheme] derived from [c] — the engine behind the "Adaptive · cover" theme.
 * Every role (background, surfaces, primary, text) is sampled from or tuned against the cover.
 */
fun coverColorScheme(c: CoverColors): ColorScheme = darkColorScheme(
    primary = c.accent,
    onPrimary = onColorFor(c.accent),
    primaryContainer = lerp(c.bg, c.accent, 0.30f),
    onPrimaryContainer = c.ink,
    secondary = c.accent,
    onSecondary = onColorFor(c.accent),
    secondaryContainer = lerp(c.bg, c.accent, 0.22f),
    onSecondaryContainer = c.ink,
    tertiary = c.accent,
    onTertiary = onColorFor(c.accent),
    background = c.bg,
    onBackground = c.ink,
    surface = lerp(c.bg, Color.White, 0.05f),
    onSurface = c.ink,
    surfaceVariant = lerp(c.bg, Color.White, 0.09f),
    onSurfaceVariant = c.sub,
    outline = c.line,
    outlineVariant = lerp(c.bg, Color.White, 0.12f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)
