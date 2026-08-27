package com.verza.ui.expressive

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
 * The shape and motion vocabulary.
 *
 * Material 3 Expressive ships these as `MaterialShapes` and `MotionScheme`, but in material3 1.4.0
 * both are internal, and the version that exposes them needs an AGP 9 upgrade. They are a path and
 * two spring specs, so they are cheaper to draw than to chase.
 *
 * ponytail: hand-rolled deliberately. Swap for MaterialShapes/MotionScheme once the expressive API
 * is public in a release that does not force the toolchain jump.
 */

/**
 * A scalloped circle — the shuffle button in the reference. The lobes are what stop a row of round
 * controls reading as a row of identical dots; shape is doing the work an extra colour would
 * otherwise have to.
 */
class CookieShape(private val lobes: Int = 9, private val depth: Float = 0.12f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = minOf(cx, cy)
        val inner = outer * (1f - depth)
        val path = Path()
        // Two samples per lobe (crest, trough) through a smooth curve reads as scalloped rather than
        // spiky; a straight lineTo between them would give a cog.
        val steps = lobes * 2
        val step = (2 * PI / steps).toFloat()
        for (i in 0 until steps) {
            val r = if (i % 2 == 0) outer else inner
            val a = i * step - (PI / 2).toFloat()
            val x = cx + r * cos(a)
            val y = cy + r * sin(a)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                // Control point on the midpoint angle at the mean radius rounds the lobe off.
                val pa = a - step / 2f
                val pr = (outer + inner) / 2f
                path.quadraticBezierTo(cx + pr * cos(pa), cy + pr * sin(pa), x, y)
            }
        }
        val a0 = -(PI / 2).toFloat()
        val pa = a0 - step / 2f
        val pr = (outer + inner) / 2f
        path.quadraticBezierTo(cx + pr * cos(pa), cy + pr * sin(pa), cx + outer * cos(a0), cy + outer * sin(a0))
        path.close()
        return Outline.Generic(path)
    }
}

/** The corner radius the layout is built on. Artwork, cards and sheets all share it. */
val ExpressiveCorner = RoundedCornerShape(28.dp)
val ExpressiveCornerSmall = RoundedCornerShape(20.dp)

/** A fully rounded rectangle. Used for the play control, which is a pill rather than a circle. */
val PillShape = RoundedCornerShape(percent = 50)

/**
 * Springs, not durations. The expressive motion system is characterised by overshoot — a control
 * that settles by bouncing slightly reads as physical, which is the whole point of the style.
 */
object ExpressiveMotion {
    /** For anything the finger is directly on: fast, barely any bounce. */
    fun <T> snappy() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** For things that appear or change size — the play/pause morph, toolbar reveals. */
    fun <T> bouncy() = spring<T>(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessLow,
    )

    /** Slow ambient drift: the glow, the artwork's idle motion. */
    fun <T> ambient() = spring<T>(
        dampingRatio = 1f,
        stiffness = 40f,
    )
}
