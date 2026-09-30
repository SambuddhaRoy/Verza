package com.verza.ui.expressive

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.RectangleShape

/**
 * Shape and motion.
 *
 * Shape is simple now: there is one, and it is a rectangle (see Shapes.kt for why). The expressive
 * scale (4 to 48dp, plus pills, a scalloped "cloud" for the cover and a "cookie" for shuffle) is
 * gone. The names stay as aliases so the few dozen call sites did not have to be rewritten to say
 * the same thing, and so any one of them can be given a real shape again without hunting for it.
 */
val ShapeExtraSmall = RectangleShape
val ShapeSmall = RectangleShape
val ShapeMedium = RectangleShape
val ShapeLarge = RectangleShape
val ShapeLargeIncreased = RectangleShape
val ShapeExtraLarge = RectangleShape
val ShapeExtraLargeIncreased = RectangleShape
val ShapeExtraExtraLarge = RectangleShape
val ShapeBottomSheet = RectangleShape
val PillShape = RectangleShape
val CloudShape = RectangleShape
val CookieShape = RectangleShape

// Kept for call sites written against the first pass.
val ExpressiveCorner = ShapeExtraLarge
val ExpressiveCornerSmall = ShapeLargeIncreased

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
