package com.verza.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
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
suspend fun extractAlbumTriad(context: Context, url: String): GlowTriad? {
    val loader = SingletonImageLoader.get(context)
    val request = ImageRequest.Builder(context)
        .data(url)
        .allowHardware(false)
        .size(160)
        .build()
    val result = runCatching { loader.execute(request) }.getOrNull() as? SuccessResult ?: return null
    val bitmap = (result.image as? BitmapImage)?.bitmap ?: return null

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
