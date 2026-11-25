package com.verza.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.compositeOver
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

/** Rows, menus and the cards in a shelf. Covers pick their own by size ([artCorner]). */
val Corner = RoundedCornerShape(12.dp)

/** A cover's corner grows with it, as Apple's artwork does: a thumbnail's is small, a page's cover larger. */
fun artCorner(size: androidx.compose.ui.unit.Dp) = RoundedCornerShape(if (size in 1.dp..64.dp) 6.dp else 10.dp)

/** An inset group of rows (Settings, the Library's index), the larger corner iOS gives them. */
val GroupCorner = RoundedCornerShape(22.dp)

val LocalPalette = staticCompositionLocalOf { Palette.from(emptyList(), dark = true) }

// Inter stands in for SF Pro, which may not ship on Android. It has an optical-size axis (14 to 32):
// titles use the 32 end, the display cut, so a large title isn't body text zoomed in.
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight, opsz: Float) = Font(
    R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), FontVariation.Setting("opsz", opsz)),
)

private val BodyFace = FontFamily(listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { inter(it, 14f) })
private val DisplayFace = FontFamily(listOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold).map { inter(it, 32f) })

/**
 * Apple's text styles at their default size (HIG, Typography, "Large (default)"), set in Inter. The
 * tracking is Inter's own: tighter as the size grows, a little open at the smallest.
 */
object Type {
    /** A page's name, "Library": Large Title, 34/41 bold. */
    val Masthead = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.022).em)
    /** A heading inside a page, "Recently played": Title 2, 22/28 bold. */
    val Section = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.02).em)
    /** An album's or a song's name at the top of its page: Title 1, 28/34 bold. */
    val Title = TextStyle(fontFamily = DisplayFace, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.021).em)
    /** Headline, 17/22 semibold: a button's words, an emphasised row. */
    val Headline = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.013).em)
    /** Body, 17/22. */
    val RowTitle = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.013).em)
    /** Subhead, 15/20. */
    val RowSubtitle = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.009).em)
    /** Callout, 16/21: prose. */
    val Body = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.011).em)
    /** Footnote, 13/18. */
    val Small = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.003).em)
    /** Caption 2, 11/13 semibold: a tab's label. */
    val Tab = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.004.em)
}

/** Material's own components (dialogs, menus, text buttons) in the same type. */
private val MaterialType = androidx.compose.material3.Typography().let { t ->
    t.copy(
        headlineSmall = Type.Section,
        titleLarge = Type.Headline.copy(fontSize = 20.sp, lineHeight = 25.sp),
        titleMedium = Type.Headline,
        bodyLarge = Type.RowTitle,
        bodyMedium = Type.Body,
        bodySmall = Type.Small,
        labelLarge = Type.Headline,
    )
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
        // Menus are drawn on surfaceContainer, which Material leaves lavender in light: black or white here.
        surfaceContainer = if (dark) palette.surface.compositeOver(palette.canvas) else palette.canvas,
        surfaceTint = androidx.compose.ui.graphics.Color.Transparent,
        outline = palette.line,
    )
    val shapes = androidx.compose.material3.Shapes(extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp))
    MaterialTheme(colorScheme = scheme, typography = MaterialType, shapes = shapes) {
        CompositionLocalProvider(LocalPalette provides palette, content = content)
    }
}
