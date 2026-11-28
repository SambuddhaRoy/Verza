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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.verza.ui.look.LocalLook
import com.verza.ui.look.Look
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Rows, menus and the cards in a shelf. Covers take their shape from the look ([Look.cover]). */
val Corner = RoundedCornerShape(12.dp)

/** A cover's corner grows with it, as Apple's artwork does: a thumbnail's is small, a page's cover larger. */
fun artCorner(size: Dp) = RoundedCornerShape(if (size in 1.dp..64.dp) 6.dp else 10.dp)

/** An inset group of rows (Settings, the Library's index), the larger corner iOS gives them. */
val GroupCorner = RoundedCornerShape(22.dp)

val LocalPalette = staticCompositionLocalOf { Palette.from(emptyList(), dark = true) }

/** The current look's text styles, by the names the pages use. */
object Type {
    /** A page's name. */
    val Masthead: TextStyle @Composable @ReadOnlyComposable get() = LocalLook.current.type.masthead
    /** A heading inside a page. */
    val Section: TextStyle @Composable @ReadOnlyComposable get() = LocalLook.current.type.section
    /** An album's or a song's name at the top of its page. */
    val Title: TextStyle @Composable @ReadOnlyComposable get() = LocalLook.current.type.title
    /** A button's words, an emphasised row. */
    val Headline: TextStyle @Composable @ReadOnlyComposable get() = LocalLook.current.type.headline
    val RowTitle: TextStyle @Composable @ReadOnlyComposable get() = LocalLook.current.type.rowTitle
    val RowSubtitle: TextStyle @Composable @ReadOnlyComposable get() = LocalLook.current.type.rowSubtitle
    /** Prose. */
    val Body: TextStyle @Composable @ReadOnlyComposable get() = LocalLook.current.type.body
    val Small: TextStyle @Composable @ReadOnlyComposable get() = LocalLook.current.type.small
    /** A tab's label. */
    val Tab: TextStyle @Composable @ReadOnlyComposable get() = LocalLook.current.type.tab
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
 * The app's theme: [look]'s colours and type. [swatches] are the playing cover's colours; an accent
 * taken from them is eased, so a track change fades rather than jumps.
 */
@Composable
fun VerzaTheme(swatches: List<Color>, look: Look, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val target = remember(swatches, dark, look) { look.palette(swatches, dark) }
    val accent by animateColorAsState(target.accent, tween(700), label = "accent")
    val palette = target.copy(accent = accent)
    val t = look.type
    // Material components (dialogs, menus, text buttons) take their colours and type from the look.
    val scheme = (if (palette.dark) darkColorScheme() else lightColorScheme()).copy(
        primary = palette.accent, onPrimary = palette.onAccent,
        background = palette.canvas, onBackground = palette.ink,
        surface = palette.canvas, onSurface = palette.ink, onSurfaceVariant = palette.inkMuted,
        surfaceContainerHigh = palette.cell, surfaceContainerHighest = palette.surfaceHigh,
        // Menus are drawn on surfaceContainer, which Material leaves lavender in light.
        surfaceContainer = if (palette.dark) palette.cell.compositeOver(palette.canvas) else palette.cell,
        surfaceTint = Color.Transparent,
        outline = palette.line,
    )
    val typography = androidx.compose.material3.Typography().copy(
        headlineSmall = t.section, titleLarge = t.headline.copy(fontSize = 20.sp, lineHeight = 25.sp), titleMedium = t.headline,
        bodyLarge = t.rowTitle, bodyMedium = t.body, bodySmall = t.small, labelLarge = t.headline,
    )
    val shapes = androidx.compose.material3.Shapes(
        extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes) {
        CompositionLocalProvider(LocalPalette provides palette, LocalLook provides look, content = content)
    }
}
