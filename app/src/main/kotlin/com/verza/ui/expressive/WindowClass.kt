package com.verza.ui.expressive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much room there is to work with.
 *
 * Material's own breakpoints, which are chosen around where a layout stops being comfortable rather
 * than around any particular device. A folded phone and a phone in portrait are both COMPACT; an
 * unfolded inner screen, a small tablet and a phone turned sideways are all MEDIUM; a real tablet is
 * EXPANDED.
 *
 * Read from the configuration rather than pulling in the window size class artifact. The value is
 * the same and it recomposes on a fold, a rotation and a resize in split screen, which is every
 * moment the answer can change.
 */
enum class WindowClass { COMPACT, MEDIUM, EXPANDED }

@Composable
@ReadOnlyComposable
fun windowClass(): WindowClass {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width < 600 -> WindowClass.COMPACT
        width < 840 -> WindowClass.MEDIUM
        else -> WindowClass.EXPANDED
    }
}

/** True where navigation belongs down the side rather than along the bottom. */
@Composable
@ReadOnlyComposable
fun useNavigationRail(): Boolean = windowClass() != WindowClass.COMPACT

/**
 * True when the window is wide enough to put two things beside each other.
 *
 * Deliberately about the ratio and not only the width. A tall narrow window at 700dp is technically
 * MEDIUM but splitting it in two gives two columns too thin to hold anything; a short wide one is
 * where a single column wastes most of the screen and a side-by-side layout earns its place.
 */
@Composable
@ReadOnlyComposable
fun useTwoPane(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp >= 720 && config.screenWidthDp > config.screenHeightDp
}

/**
 * A ceiling on how wide a column of text or list rows is allowed to get.
 *
 * A settings row stretched across a thirteen inch tablet is one enormous line with a switch marooned
 * at the far end, and a paragraph that wide is genuinely harder to read. Content stops growing and
 * centres instead.
 */
@Composable
@ReadOnlyComposable
fun readableWidth(): Dp = when (windowClass()) {
    WindowClass.COMPACT -> Dp.Unspecified
    WindowClass.MEDIUM -> 640.dp
    WindowClass.EXPANDED -> 760.dp
}
