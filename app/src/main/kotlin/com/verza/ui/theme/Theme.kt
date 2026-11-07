package com.verza.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.verza.R

/** The one corner radius, on covers, rows, buttons, fields, sheets and the dock. */
val Corner = RoundedCornerShape(12.dp)

val LocalPalette = staticCompositionLocalOf { Palette.from(emptyList(), dark = true) }

// Inter has an optical-size axis (14 to 32). Headings use the 32 end, the display cut, so a large
// title doesn't look like body text zoomed in.
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight, opsz: Float) = Font(
    R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), FontVariation.Setting("opsz", opsz)),
)

private val BodyFace = FontFamily(listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold).map { inter(it, 14f) })
private val DisplayFace = FontFamily(listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium).map { inter(it, 32f) })

object Type {
    /** A page's name: "library", very large and light. */
    val Masthead = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Light, fontSize = 46.sp, lineHeight = 52.sp, letterSpacing = (-0.03).em)
    /** A section or a word used as a button: "recently played", "play". */
    val Section = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Light, fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.02).em)
    /** An album's or a song's name at the top of its page. */
    val Title = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 37.sp, letterSpacing = (-0.018).em)
    val RowTitle = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp)
    val RowSubtitle = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp)
    val Body = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp)
    val Small = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em)
}

/** The colours in a cover, for [Palette.from]. */
fun swatchesOf(bitmap: android.graphics.Bitmap): List<Color> =
    androidx.palette.graphics.Palette.from(bitmap).generate().swatches.map { Color(it.rgb) }

/** The colours of the cover at [url], once it has loaded; none while it loads or with no cover. */
@Composable
fun coverSwatches(url: String?): List<Color> {
    val context = LocalContext.current
    return produceState(emptyList(), url) {
        if (url == null) { value = emptyList(); return@produceState }
        // Small and in software: Palette reads the pixels, and it only needs a few of them.
        val request = ImageRequest.Builder(context).data(url).size(128).allowHardware(false).build()
        val bitmap = SingletonImageLoader.get(context).execute(request).image?.toBitmap() ?: return@produceState
        value = withContext(Dispatchers.Default) { swatchesOf(bitmap) }
    }.value
}

/**
 * The app's theme. [swatches] are the playing cover's colours; the accent follows them, eased so a
 * track change fades rather than jumps.
 */
@Composable
fun VerzaTheme(swatches: List<Color>, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val target = remember(swatches, dark) { Palette.from(swatches, dark) }
    val accent by animateColorAsState(target.accent, tween(700), label = "accent")
    val palette = target.copy(accent = accent)
    // Material components (text fields, dialogs) take their colours from the same palette.
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = palette.accent, onPrimary = palette.onAccent,
        background = palette.canvas, onBackground = palette.ink,
        surface = palette.canvas, onSurface = palette.ink, onSurfaceVariant = palette.inkMuted,
        surfaceContainerHigh = palette.surface, surfaceContainerHighest = palette.surfaceHigh,
        outline = palette.line,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalPalette provides palette, content = content)
    }
}
