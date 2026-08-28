package com.verza.ui.expressive

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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

/**
 * A scalloped blob — the artwork mask in the reference, and the shuffle button at a smaller size.
 *
 * Lobes are placed on an *ellipse* rather than a circle, so the same shape reads as a cookie in a
 * square box and as a cloud in a wide one. That is the single knob that makes it work for both.
 */
class ScallopShape(
    private val lobes: Int = 9,
    private val depth: Float = 0.12f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val innerScale = 1f - depth
        val path = Path()
        // Two samples per lobe (crest, trough) joined by a quadratic. A straight line between them
        // would give a cog; the curve is what makes it read as soft.
        val steps = lobes * 2
        val step = (2 * PI / steps).toFloat()
        fun px(a: Float, rx: Float, ry: Float) = cx + rx * cos(a)
        fun py(a: Float, rx: Float, ry: Float) = cy + ry * sin(a)

        for (i in 0..steps) {
            val crest = i % 2 == 0
            val rx = if (crest) cx else cx * innerScale
            val ry = if (crest) cy else cy * innerScale
            val a = i * step - (PI / 2).toFloat()
            val x = px(a, rx, ry)
            val y = py(a, rx, ry)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val midA = a - step / 2f
                val midRx = cx * (1f + innerScale) / 2f
                val midRy = cy * (1f + innerScale) / 2f
                path.quadraticTo(px(midA, midRx, midRy), py(midA, midRx, midRy), x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** The cover mask: few, deep lobes on a wide box reads as the reference's cloud. */
val CloudShape = ScallopShape(lobes = 7, depth = 0.17f)

/** The small scalloped control (shuffle). More, shallower lobes so it stays legible at 52dp. */
val CookieShape = ScallopShape(lobes = 9, depth = 0.13f)

// ── shape scale ──────────────────────────────────────────────────────────────
// M3's scale runs extraSmall 4 / small 8 / medium 12 / large 16 / extraLarge 24. Expressive pushes
// the top end much further — the reference's cards and sheets are far rounder than 24dp — so the
// large end is extended rather than replaced.
val ShapeSmall = RoundedCornerShape(12.dp)
val ShapeMedium = RoundedCornerShape(20.dp)
val ShapeLarge = RoundedCornerShape(28.dp)
val ShapeExtraLarge = RoundedCornerShape(36.dp)

/** Fully rounded. The play control and every chip. */
val PillShape = RoundedCornerShape(percent = 50)

// Kept for call sites written against the first pass.
val ExpressiveCorner = ShapeLarge
val ExpressiveCornerSmall = ShapeMedium

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
