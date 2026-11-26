package com.verza.ui.kit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import com.verza.ui.theme.readableOn

/*
 * Soft surfaces (neumorphism): the page is one material, and a control is that material pushed up out
 * of it or pressed into it. The light is the room's, from the top left, so a raised shape has a
 * highlight up there and a shadow down to the right, each a blurred copy of the shape; pressed in, the
 * two fall inside it the other way round. A press moves a control from one to the other.
 *
 * The light doesn't follow the phone's tilt, as the glass's glint does: a room's light stays put.
 * Blurred shadows on a hardware canvas need Android 9; below it a surface is its face alone.
 */

/** The page's material lit: the highlight on its upper edges and the shadow under its lower ones. */
fun Palette.light(): Color = if (dark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.92f)
fun Palette.shade(): Color = if (dark) Color.Black.copy(alpha = 0.66f) else lerp(canvas, Color.Black, 0.4f).copy(alpha = 0.5f)

/** 0 to 1 while [source] is pressed: in quickly, out on the springy [bouncy]. Read it while drawing. */
@Composable
fun pressOf(source: InteractionSource): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 1f else 0f, if (pressed) tween(90) else bouncy(), label = "press")
}

private val caster = Paint()

/** [path] in [colour] with its two shadows, raised [d] px out of the page as [up] goes to 1. */
fun DrawScope.raise(path: Path, colour: Color, d: Float, p: Palette, up: Float = 1f) {
    if (up > 0f) drawIntoCanvas { c ->
        val by = d * up
        caster.color = colour
        val f = caster.asFrameworkPaint()
        f.setShadowLayer(by * 2.2f, -by, -by, p.light().toArgb()); c.drawPath(path, caster)
        f.setShadowLayer(by * 2.2f, by, by, p.shade().toArgb()); c.drawPath(path, caster)
        f.clearShadowLayer()
    }
    drawPath(path, colour)
}

/** The two shadows inside [path], as if pressed [d] px into the page, as [down] goes to 1. */
fun DrawScope.sink(path: Path, d: Float, p: Palette, down: Float = 1f) {
    if (down <= 0f) return
    val b = path.getBounds()
    val m = d * 4
    // The world with a hole the shape of the path in it: its shadows fall into the hole.
    val frame = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(b.left - m, b.top - m, b.right + m, b.bottom + m))
        addPath(path)
    }
    clipPath(path) {
        drawIntoCanvas { c ->
            val by = d * 0.7f * down
            caster.color = Color.Black
            val f = caster.asFrameworkPaint()
            f.setShadowLayer(by * 1.8f, by, by, p.shade().toArgb()); c.drawPath(frame, caster)
            f.setShadowLayer(by * 1.8f, -by, -by, p.light().toArgb()); c.drawPath(frame, caster)
            f.clearShadowLayer()
        }
    }
}

/**
 * The page's material in [shape], raised [depth] out of the page, or pressed into it as [press] goes
 * to 1 ([inset] keeps it pressed). [face] colours it, the page's own by default.
 */
@Composable
fun Modifier.soft(shape: Shape, depth: Dp = 6.dp, press: State<Float>? = null, inset: Boolean = false, face: Color? = null): Modifier {
    val p = LocalPalette.current
    val colour = face ?: p.canvas
    return drawWithCache {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val d = depth.toPx()
        // Raised, the face catches the light a little at the top left, as a soft curve would.
        val sheen = Brush.linearGradient(
            listOf(Color.White.copy(alpha = if (p.dark) 0.04f else 0.2f), Color.Transparent, Color.Black.copy(alpha = if (p.dark) 0.1f else 0.03f)),
            Offset.Zero, Offset(size.width, size.height),
        )
        val rim = Brush.linearGradient(listOf(p.light(), Color.Transparent), Offset.Zero, Offset(size.width * 0.5f, size.height * 0.5f))
        val line = Stroke(1.dp.toPx())
        onDrawBehind {
            val down = if (inset) 1f else press?.value ?: 0f
            val up = 1f - down
            raise(path, colour, d, p, up)
            if (up > 0f) {
                drawPath(path, sheen, alpha = up)
                drawPath(path, rim, alpha = up, style = line)
            }
            sink(path, d, p, down)
        }
    }
}

/**
 * A round key of the page's material with one symbol on it; pressed, it goes into the page. [face]
 * colours it: the accent, for the one key that matters (Braun's calculators had one orange key).
 */
@Composable
fun SoftKey(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 56.dp, iconSize: Dp = size * 0.46f, face: Color? = null, tint: Color? = null, iconModifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val source = remember { MutableInteractionSource() }
    val press = pressOf(source)
    Box(
        modifier.size(size).soft(CircleShape, depth = size * 0.085f, press = press, face = face)
            .clip(CircleShape).clickable(source, null, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, null, tint = tint ?: face?.let { if (it == p.accent) p.onAccent else readableOn(it) } ?: p.ink,
            modifier = iconModifier.size(iconSize).graphicsLayer { val s = 1f - 0.07f * press.value; scaleX = s; scaleY = s },
        )
    }
}

/** A line cut into the page, a shadow over a highlight: where glass would draw a hairline. */
@Composable
fun Etched(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shade = p.shade().let { it.copy(alpha = it.alpha * 0.55f) }
    val light = p.light()
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        val w = 1.dp.toPx()
        drawLine(shade, Offset(0f, w / 2), Offset(size.width, w / 2), w)
        drawLine(light, Offset(0f, w * 1.5f), Offset(size.width, w * 1.5f), w)
    }
}
