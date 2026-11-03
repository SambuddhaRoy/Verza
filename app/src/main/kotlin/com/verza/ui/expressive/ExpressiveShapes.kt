package com.verza.ui.expressive

import kotlin.math.tan
import kotlin.math.sqrt
import kotlin.math.min
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.Immutable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.verza.ui.theme.CornerShape
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Composable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shape and motion.
 *
 * Material 3 Expressive ships these as `MaterialShapes` and `MotionScheme`. Both are internal in
 * material3 1.4.0, and the release that exposes them needs an AGP 9 upgrade (proven on
 * chore/agp9-spike). A scalloped path and a table of spring constants are cheaper to draw than to
 * chase, so they are here.
 *
 * ponytail: swap for MaterialShapes/MotionScheme when the expressive API is public in a release that
 * does not force the toolchain jump.
 */

// Every token is the one corner (Shapes.kt). The names stay so the call sites read for what they are.
val CloudShape: Shape get() = CornerShape
val CookieShape: Shape get() = CornerShape
val ShapeExtraSmall: Shape get() = CornerShape
val ShapeSmall: Shape get() = CornerShape
val ShapeMedium: Shape get() = CornerShape
val ShapeLarge: Shape get() = CornerShape
val ShapeLargeIncreased: Shape get() = CornerShape
val ShapeExtraLarge: Shape get() = CornerShape
val ShapeExtraLargeIncreased: Shape get() = CornerShape
val ShapeExtraExtraLarge: Shape get() = CornerShape

/** Bottom sheets: rounded at the top, flat where they meet the edge of the screen. */
val ShapeBottomSheet: Shape = RoundedCornerShape(topStart = com.verza.ui.theme.VerzaCorner, topEnd = com.verza.ui.theme.VerzaCorner)
val PillShape: Shape get() = CornerShape

// Kept for call sites written against the first pass.
val ExpressiveCorner: Shape get() = CornerShape
val ExpressiveCornerSmall: Shape @Composable @ReadOnlyComposable get() = ShapeLargeIncreased

/**
 * Springs, not durations.
 *
 * M3 Expressive splits motion two ways. *Spatial* animations move something — position, size,
 * corner radius — and are allowed to overshoot, which is what gives the style its bounce. *Effects*
 * animate colour and opacity, where overshoot is meaningless and would show up as a flash, so they
 * are critically damped. Each has fast/default/slow.
 */
object ExpressiveMotion {
    // Spatial: damping below 1 so it overshoots and settles.
    fun <T> spatialFast() = spring<T>(dampingRatio = 0.75f, stiffness = 1400f)
    fun <T> spatialDefault() = spring<T>(dampingRatio = 0.72f, stiffness = 700f)
    fun <T> spatialSlow() = spring<T>(dampingRatio = 0.70f, stiffness = 300f)

    // Effects: critically damped. Colour must never overshoot — it reads as a flicker.
    fun <T> effectsFast() = spring<T>(dampingRatio = 1f, stiffness = 1400f)
    fun <T> effectsDefault() = spring<T>(dampingRatio = 1f, stiffness = 700f)
    fun <T> effectsSlow() = spring<T>(dampingRatio = 1f, stiffness = Spring.StiffnessLow)

    /** Slow ambient drift for the glow and the artwork's idle motion. */
    fun <T> ambient() = spring<T>(dampingRatio = 1f, stiffness = 40f)

    // Aliases used by the first pass.
    fun <T> snappy() = spatialFast<T>()
    fun <T> bouncy() = spatialDefault<T>()
}
