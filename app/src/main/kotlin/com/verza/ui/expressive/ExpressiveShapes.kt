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
import com.verza.ui.theme.squareOr
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

// The scalloped silhouettes and the scale below, as Material draws them. Each public token reads
// the design (see DesignScheme) and is a rectangle in the Poster one, so the few dozen call sites
// switch with the setting without having to know there is one.
private val Cloud = ScallopShape(lobes = 7, depth = 0.17f)
private val Cookie = ScallopShape(lobes = 9, depth = 0.13f)

/** The cover mask: few, deep lobes on a wide box reads as the reference's cloud. */
val CloudShape: Shape @Composable @ReadOnlyComposable get() = squareOr(Cloud)

/** The small scalloped control (shuffle). More, shallower lobes so it stays legible at 52dp. */
val CookieShape: Shape @Composable @ReadOnlyComposable get() = squareOr(Cookie)

// ── shape scale ──────────────────────────────────────────────────────────────
// The M3 baseline runs none 0 / xs 4 / s 8 / m 12 / l 16 / xl 28 / full. Expressive adds three
// larger steps for more dramatic silhouettes rather than replacing the scale: large-increased 20,
// extra-large-increased 32, and extra-extra-large 48.
private val RoundXs = RoundedCornerShape(4.dp)
private val RoundS = RoundedCornerShape(8.dp)
private val RoundM = RoundedCornerShape(12.dp)
private val RoundL = RoundedCornerShape(16.dp)
private val RoundLi = RoundedCornerShape(20.dp)
private val RoundXl = RoundedCornerShape(28.dp)
private val RoundXli = RoundedCornerShape(32.dp)
private val RoundXxl = RoundedCornerShape(48.dp)
private val RoundSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val RoundPill = RoundedCornerShape(percent = 50)

val ShapeExtraSmall: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundXs)
val ShapeSmall: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundS)
val ShapeMedium: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundM)
val ShapeLarge: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundL)
val ShapeLargeIncreased: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundLi)
val ShapeExtraLarge: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundXl)
val ShapeExtraLargeIncreased: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundXli)
val ShapeExtraExtraLarge: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundXxl)

/** Bottom sheets: rounded at the top, flat where they meet the edge of the screen. */
val ShapeBottomSheet: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundSheet)
/** Fully rounded. The play control and every chip. */
val PillShape: Shape @Composable @ReadOnlyComposable get() = squareOr(RoundPill)

// Kept for call sites written against the first pass.
val ExpressiveCorner: Shape @Composable @ReadOnlyComposable get() = ShapeExtraLarge
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
