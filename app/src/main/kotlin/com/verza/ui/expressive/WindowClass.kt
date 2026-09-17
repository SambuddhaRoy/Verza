package com.verza.ui.expressive

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much room the window has right now, by width.
 *
 * Material's own breakpoints, chosen around where a layout stops being comfortable rather than around
 * any particular device. This decides structure, meaning where navigation goes and whether content
 * splits into panes, so it follows the window: a phone turned sideways really does have a wide window
 * and a short one, and a bottom bar is the wrong answer there too.
 *
 * Read from the configuration rather than pulling in the window size class artifact. Same numbers,
 * and it recomposes on a fold, a rotation and a split-screen resize, which is every moment the answer
 * can change.
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

/**
 * How big the device is, whichever way up it is held.
 *
 * The counterpart to [windowClass], for sizes rather than structure. Cards and artwork should not
 * double in size because a phone was turned on its side, but they should grow when a fold opens into
 * something tablet-sized. The smallest width is the one number that tells those two apart.
 */
enum class DeviceSize { PHONE, SMALL_TABLET, TABLET }

@Composable
@ReadOnlyComposable
fun deviceSize(): DeviceSize {
    val smallest = LocalConfiguration.current.smallestScreenWidthDp
    return when {
        smallest < 600 -> DeviceSize.PHONE
        smallest < 840 -> DeviceSize.SMALL_TABLET
        else -> DeviceSize.TABLET
    }
}

/** True where navigation belongs down the side rather than along the bottom. */
@Composable
@ReadOnlyComposable
fun useNavigationRail(): Boolean = windowClass() != WindowClass.COMPACT

/**
 * True when the player should put the artwork beside its controls rather than above them.
 *
 * Any landscape window wide enough to hold two useful columns. A phone on its side is the case that
 * needed this most: stacked, the cover and the controls cannot both fit in four hundred points of
 * height, so something was always cut off.
 */
@Composable
@ReadOnlyComposable
fun useTwoPane(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp >= 560 && config.screenWidthDp > config.screenHeightDp
}

/**
 * A ceiling on how wide a column of text or list rows is allowed to get.
 *
 * A settings row stretched across a thirteen inch tablet is one enormous line with a switch marooned at
 * the far end, and a paragraph that wide is genuinely harder to read. Content stops growing and centres
 * instead. On a compact window the ceiling is simply the window, which keeps the value finite and so
 * lets it be animated when the window changes.
 */
@Composable
@ReadOnlyComposable
fun readableWidth(): Dp = when (windowClass()) {
    WindowClass.COMPACT -> LocalConfiguration.current.screenWidthDp.dp
    WindowClass.MEDIUM -> 640.dp
    WindowClass.EXPANDED -> 760.dp
}

/** [readableWidth], sprung, so a capped column glides to its new width on a rotation. */
@Composable
fun animatedReadableWidth(): Dp = animatedDp(readableWidth(), "readableWidth")

/** Any size that depends on the window, sprung rather than snapped when the window changes. */
@Composable
fun animatedDp(target: Dp, label: String): Dp {
    val value by animateDpAsState(
        targetValue = target,
        // Slow, to move in step with animateBoundsIn.
        animationSpec = ExpressiveMotion.spatialSlow(),
        label = label,
    )
    return value
}
