package com.verza.ui.expressive

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * The mask around the album art, and the machinery to morph it between tracks.
 *
 * The scallop is defined parametrically rather than as a hand-built path:
 *
 *     r(θ) = 1 − depth/2 · (1 − cos(lobes · θ))
 *
 * which is 1 at every crest and 1−depth at every trough. Two properties fall out of writing it this
 * way and both matter. Depth 0 collapses to a plain ellipse, so "no shape" is the same equation
 * rather than a special case. And two scallops can be blended by lerping their radii at matching
 * angles, which is what makes a genuine morph possible — the alternative, cross-fading two clipped
 * images, shows both silhouettes at once through the middle of the transition.
 *
 * Lobes stay integral through the blend. A fractional lobe count leaves r(0) ≠ r(2π), so the curve
 * would not close and the seam shows as a notch.
 */
data class ScallopSpec(val lobes: Int, val depth: Float) {
    companion object {
        val Circle = ScallopSpec(1, 0f)
        val Cloud = ScallopSpec(7, 0.17f)
        val Cookie = ScallopSpec(9, 0.13f)
        val Flower = ScallopSpec(5, 0.24f)
        val Bloom = ScallopSpec(12, 0.10f)

        /**
         * The set SHUFFLE cycles through.
         *
         * Deliberately far apart. The first version included both Cookie (9 lobes, 0.13 deep) and
         * Bloom (12 lobes, 0.10 deep), which are near-identical to the eye — so a genuine change
         * between them looked like no change at all, and the shape appeared to repeat.
         */
        val Cycle = listOf(Circle, Flower, Cloud, Bloom)
    }
}

private fun radiusFactor(theta: Float, spec: ScallopSpec): Float =
    1f - spec.depth / 2f * (1f - cos(spec.lobes * theta))

/**
 * A scallop, or a blend of two. [t] runs 0 (all [from]) to 1 (all [to]).
 *
 * 240 samples is enough that the polyline reads as a smooth curve at any size a cover is drawn at,
 * and it avoids the control-point bookkeeping a Bézier construction would need to stay morphable.
 */
class MorphScallopShape(
    private val from: ScallopSpec,
    private val to: ScallopSpec = from,
    private val t: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val path = Path()
        val steps = 240
        for (i in 0..steps) {
            val theta = (i.toFloat() / steps) * 2f * PI.toFloat() - (PI / 2).toFloat()
            val rf = radiusFactor(theta, from) + (radiusFactor(theta, to) - radiusFactor(theta, from)) * t
            val x = cx + cx * rf * cos(theta)
            val y = cy + cy * rf * sin(theta)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** What the listener picked in Settings. */
enum class CoverShapeMode(val label: String) {
    NONE("Square"),
    CIRCLE("Circle"),
    CLOUD("Cloud"),
    COOKIE("Cookie"),
    FLOWER("Flower"),
    SHUFFLE("Change each track"),
    ;

    companion object {
        fun fromName(name: String?): CoverShapeMode =
            entries.firstOrNull { it.name == name } ?: SHUFFLE
    }
}

private fun CoverShapeMode.spec(): ScallopSpec? = when (this) {
    CoverShapeMode.NONE -> null
    CoverShapeMode.CIRCLE -> ScallopSpec.Circle
    CoverShapeMode.CLOUD -> ScallopSpec.Cloud
    CoverShapeMode.COOKIE -> ScallopSpec.Cookie
    CoverShapeMode.FLOWER -> ScallopSpec.Flower
    CoverShapeMode.SHUFFLE -> null
}

/**
 * The shape to clip the current cover with, morphing whenever [trackKey] changes.
 *
 * In SHUFFLE the shape is chosen from a hash of the track id, not from a counter, so a given song
 * always gets the same silhouette — going back a track returns you to the shape you just saw
 * instead of advancing to a third one.
 */
@Composable
fun rememberCoverShape(mode: CoverShapeMode, trackKey: String?): Shape {
    if (mode == CoverShapeMode.NONE) return ShapeExtraLarge

    // The shape we are currently showing, so SHUFFLE can guarantee the next one differs.
    var shown by remember { mutableStateOf<ScallopSpec?>(null) }

    val target = remember(mode, trackKey, shown) {
        mode.spec() ?: run {
            // Hash the track so the choice is stable, then pick from the shapes that are NOT the one
            // on screen. Hashing straight into the whole list gave a 1-in-4 chance of drawing the
            // same shape again, which reads as the feature being broken rather than as chance.
            val options = ScallopSpec.Cycle.filter { it != shown }
            val h = (trackKey ?: "").hashCode()
            options[Math.floorMod(h, options.size)]
        }
    }

    // Where the morph is coming from and going to, plus the 0..1 driver between them. Reset to 0 and
    // run to 1 on each change, so every transition starts from the silhouette actually on screen.
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    LaunchedEffect(Unit) { if (shown == null) shown = target }
    val t = remember { Animatable(1f) }

    LaunchedEffect(target) {
        if (to != target) {
            from = to
            to = target
            shown = target
            t.snapTo(0f)
            // A spatial spring, so it overshoots: the silhouette springs slightly past the new shape
            // and settles back, which is the bounce doing something the eye can actually read.
            t.animateTo(1f, ExpressiveMotion.spatialSlow())
        }
    }

    return remember(from, to, t.value) { MorphScallopShape(from = from, to = to, t = t.value) }
}

/** Kept so existing call sites keep compiling; the cover now uses [rememberCoverShape]. */
val CoverShapeFallback: Shape = RoundedCornerShape(28.dp)
