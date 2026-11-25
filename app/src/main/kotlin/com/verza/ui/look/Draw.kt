package com.verza.ui.look

import com.verza.graph
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Drawing that more than one look uses. Every mark here is drawn in code: nothing is traced from the
 * sites the looks learned from.
 */

/** A solid copy of [shape] behind the element, [x],[y] down and right: the hard shadow of neo-brutalism and stickers. */
fun Modifier.hardShadow(shape: Shape, color: Color, x: Dp = 4.dp, y: Dp = 4.dp) = drawBehind {
    translate(x.toPx(), y.toPx()) {
        when (val o = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rectangle -> drawRect(color, o.rect.topLeft, o.rect.size)
            is Outline.Rounded -> drawPath(Path().apply { addRoundRect(o.roundRect) }, color)
            is Outline.Generic -> drawPath(o.path, color)
        }
    }
}

/** [shape]'s outline in [color], [width] thick. */
fun Modifier.outline(shape: Shape, color: Color, width: Dp) = drawBehind {
    val stroke = Stroke(width.toPx())
    when (val o = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle -> drawRect(color, o.rect.topLeft + Offset(stroke.width / 2, stroke.width / 2), Size(o.rect.width - stroke.width, o.rect.height - stroke.width), style = stroke)
        is Outline.Rounded -> drawPath(Path().apply { addRoundRect(o.roundRect) }, color, style = stroke)
        is Outline.Generic -> drawPath(o.path, color, style = stroke)
    }
}

/**
 * A rounded rectangle drawn by hand: its edges wander a little, the same way every time for the same
 * [seed], so a card keeps its wobble as the list scrolls.
 */
class WobblyShape(private val seed: Int, private val corner: Dp = 14.dp, private val wander: Dp = 2.2.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { corner.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2)
        val j = with(density) { wander.toPx() }
        val rnd = java.util.Random(seed.toLong() * 7919)
        fun n() = (rnd.nextFloat() - 0.5f) * 2f * j
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(r + n(), n())
            quadraticTo(w / 2 + n(), n() * 1.6f, w - r + n(), n())
            quadraticTo(w + n(), n(), w + n(), r + n())
            quadraticTo(w + n() * 1.6f, h / 2 + n(), w + n(), h - r + n())
            quadraticTo(w + n(), h + n(), w - r + n(), h + n())
            quadraticTo(w / 2 + n(), h + n() * 1.6f, r + n(), h + n())
            quadraticTo(n(), h + n(), n(), h - r + n())
            quadraticTo(n() * 1.6f, h / 2 + n(), n(), r + n())
            quadraticTo(n(), n(), r + n(), n())
            close()
        }
        return Outline.Generic(path)
    }
}

/** Small crosses on a square lattice, [step] apart: the "+" marks of 3D and minimal sites. */
fun DrawScope.crosshairs(color: Color, step: Dp = 64.dp, arm: Dp = 4.dp, offset: Offset = Offset.Zero) {
    val s = step.toPx(); val a = arm.toPx(); val w = 1.dp.toPx()
    var y = (offset.y % s) - s
    while (y < size.height + s) {
        var x = (offset.x % s) - s
        while (x < size.width + s) {
            drawLine(color, Offset(x - a, y), Offset(x + a, y), w)
            drawLine(color, Offset(x, y - a), Offset(x, y + a), w)
            x += s
        }
        y += s
    }
}

/** Ruled lines: [columns] across and rows every [row], as a modular grid's cells are drawn. */
fun DrawScope.ruled(color: Color, columns: Int, row: Dp? = null) {
    val w = 1.dp.toPx()
    for (i in 1 until columns) { val x = size.width * i / columns; drawLine(color, Offset(x, 0f), Offset(x, size.height), w) }
    if (row != null) { var y = row.toPx(); while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), w); y += row.toPx() } }
}

/** A floor of lines running to a vanishing point at [horizon] of the height, and across it, nearer ones wider apart. */
fun DrawScope.perspectiveFloor(color: Color, horizon: Float = 0.62f, rays: Int = 18, rungs: Int = 12) {
    val vy = size.height * horizon
    val cx = size.width / 2
    val w = 1.dp.toPx()
    for (i in -rays..rays) {
        val bx = cx + i * size.width / rays * 1.4f
        drawLine(color, Offset(cx, vy), Offset(bx, size.height), w)
    }
    for (k in 1..rungs) {
        val t = k.toFloat() / rungs
        val y = vy + (size.height - vy) * t * t
        drawLine(color.copy(alpha = color.alpha * (0.3f + 0.7f * t)), Offset(0f, y), Offset(size.width, y), w)
    }
}

/** A zigzag edge across the width, teeth [tooth] wide, filling below it with [color]: ROBOT's section break. */
fun DrawScope.zigzag(color: Color, tooth: Dp = 28.dp, depth: Dp = 18.dp) {
    val t = tooth.toPx(); val d = depth.toPx()
    val path = Path().apply {
        moveTo(0f, d)
        var x = 0f
        var up = true
        while (x < size.width + t) { x += t / 2; lineTo(x, if (up) 0f else d); up = !up }
        lineTo(size.width + t, size.height); lineTo(0f, size.height); close()
    }
    drawPath(path, color)
}

/** [text] with a hard 3D extrusion under it: [depth] copies down and right in [side], the face on top. */
@Composable
fun ExtrudedText(text: String, style: TextStyle, face: Color, side: Color, modifier: Modifier = Modifier, depth: Dp = 6.dp, outline: Color? = null) {
    Box(modifier) {
        val steps = (depth.value / 0.75f).toInt().coerceAtLeast(1)
        for (i in steps downTo 1) {
            val o = depth * (i.toFloat() / steps)
            Text(text, style = style, color = side, modifier = Modifier.offset(o, o).clearAndSetSemantics { })
        }
        if (outline != null) Text(text, style = style.copy(drawStyle = Stroke(3f)), color = outline, modifier = Modifier.clearAndSetSemantics { })
        Text(text, style = style, color = face)
    }
}

/**
 * [text] as large as fits on one line in the width: brutalism's full-bleed words. [max] caps it for
 * short words.
 */
@Composable
fun FillText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, max: Float = 140f, extrude: Color? = null, depth: Dp = 5.dp) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val width = constraints.maxWidth
        val size = remember(text, width, style) {
            val probe = measurer.measure(text, style.copy(fontSize = 100.sp), maxLines = 1)
            (100f * width / probe.size.width.coerceAtLeast(1) * 0.98f).coerceAtMost(max)
        }
        val fitted = style.copy(fontSize = size.sp, lineHeight = (size * 0.92f).sp)
        if (extrude != null) ExtrudedText(text, fitted, color, extrude, depth = depth)
        else Text(text, style = fitted, color = color, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

/** [text] around a circle, turning slowly: a rotating badge. */
@Composable
fun CircularText(text: String, style: TextStyle, color: Color, size: Dp, modifier: Modifier = Modifier, seconds: Int = 14, centre: (@Composable () -> Unit)? = null) {
    val turn by rememberInfiniteTransition(label = "badge").animateFloat(0f, 360f, infiniteRepeatable(tween(seconds * 1000, easing = LinearEasing), RepeatMode.Restart), label = "turn")
    val measurer = rememberTextMeasurer()
    Box(modifier.size(size).clearAndSetSemantics { }, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(Modifier.size(size).graphicsLayer { rotationZ = turn }) {
            val r = this.size.minDimension / 2 - style.fontSize.toPx() * 0.7f
            val chars = text.toList()
            chars.forEachIndexed { i, c ->
                val a = 2 * PI * i / chars.size - PI / 2
                val layout = measurer.measure(c.toString(), style)
                val pos = Offset(center.x + (r * cos(a)).toFloat(), center.y + (r * sin(a)).toFloat())
                rotate(Math.toDegrees(a).toFloat() + 90f, pos) {
                    drawText(layout, color, Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
                }
            }
        }
        centre?.invoke()
    }
}

/** One of the Fluent Emoji 3D renders (MIT, Microsoft), [size] square. */
@Composable
fun Icon3D(res: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(res), null, modifier.size(size))
}

/** A gentle up-and-down bob, [px] high, [ms] a cycle: for things that float. */
@Composable
fun bob(ms: Int = 3200): Float {
    val t by rememberInfiniteTransition(label = "bob").animateFloat(0f, 1f, infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Restart), label = "t")
    return sin(t * 2 * PI).toFloat()
}

/**
 * Something to press that sits on a hard shadow and is pushed down into it under the finger: the
 * button of neo-brutalism, stickers and cartoons. [depth] is how far it stands off its shadow.
 */
@Composable
fun Pushable(
    shape: Shape,
    fill: Color,
    edge: Color,
    shadow: Color,
    modifier: Modifier = Modifier,
    depth: Dp = 4.dp,
    border: Dp = 2.5.dp,
    enabled: Boolean = true,
    label: String? = null,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val push by androidx.compose.animation.core.animateDpAsState(if (pressed) depth else 0.dp, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessHigh), label = "push")
    Box(
        modifier
            .padding(end = depth, bottom = depth)
            .hardShadow(shape, shadow, depth, depth)
            .offset(push, push)
            .clip(shape)
            .background(fill)
            .outline(shape, edge, border)
            .clickable(source, null, enabled = enabled, onClick = onClick)
            .semantics { role = androidx.compose.ui.semantics.Role.Button; if (label != null) contentDescription = label },
        contentAlignment = androidx.compose.ui.Alignment.Center,
        content = content,
    )
}

/**
 * Arches inside arches, the smaller further in, all standing on [base]: a tunnel in thin line, as
 * minimal sites draw one. [width] and [height] are the outermost's.
 */
fun DrawScope.arches(color: Color, centreX: Float, base: Float, width: Float, height: Float, count: Int = 9) {
    val stroke = Stroke(1.dp.toPx())
    for (i in 0 until count) {
        val t = 1f - i.toFloat() / count
        val w = width * t
        val top = base - height * (0.35f + 0.65f * t)
        val path = Path().apply {
            moveTo(centreX - w / 2, base)
            lineTo(centreX - w / 2, top + w / 2)
            arcTo(androidx.compose.ui.geometry.Rect(centreX - w / 2, top, centreX + w / 2, top + w), 180f, 180f, false)
            lineTo(centreX + w / 2, base)
        }
        drawPath(path, color, style = stroke)
    }
}

/** A winking smiley of polished chrome: a ring and a face lit by sweeping greys. Drawn, not traced. */
@Composable
fun ChromeSmiley(size: Dp, modifier: Modifier = Modifier, sway: Boolean = true) {
    val t = if (sway) bob(5200) else 0f
    Canvas(modifier.size(size).graphicsLayer { rotationZ = t * 9f }.clearAndSetSemantics { }) {
        val s = this.size.minDimension
        val c = center
        val metal = androidx.compose.ui.graphics.Brush.sweepGradient(
            listOf(Color(0xFFF7F8FA), Color(0xFF7D8494), Color(0xFFFFFFFF), Color(0xFF4F5563), Color(0xFFD9DEE7), Color(0xFF969DAB), Color(0xFFF7F8FA)), c,
        )
        val ring = s * 0.1f
        drawCircle(Color.Black.copy(alpha = 0.16f), s / 2 - ring / 2, c + Offset(s * 0.03f, s * 0.05f), style = Stroke(ring))
        drawCircle(metal, s / 2 - ring / 2, c, style = Stroke(ring))
        drawArc(Color.White.copy(alpha = 0.85f), 205f, 55f, false, Offset(ring / 2, ring / 2), Size(s - ring, s - ring), style = Stroke(ring * 0.22f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        // An open eye, and a wink.
        drawOval(metal, Offset(c.x - s * 0.2f, c.y - s * 0.2f), Size(s * 0.1f, s * 0.17f))
        drawArc(metal, 200f, 140f, false, Offset(c.x + s * 0.06f, c.y - s * 0.17f), Size(s * 0.16f, s * 0.12f), style = Stroke(ring * 0.62f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawArc(metal, 18f, 144f, false, Offset(c.x - s * 0.26f, c.y - s * 0.2f), Size(s * 0.52f, s * 0.44f), style = Stroke(ring * 0.8f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

/** Rays from [centre] out past the edges, every other one in [ray]: a cartoon sunburst. */
fun DrawScope.sunburst(centre: Offset, ray: Color, rays: Int = 28, turn: Float = 0f) {
    val r = maxOf(size.width, size.height) * 1.6f
    val step = 360f / rays
    for (i in 0 until rays step 2) {
        val a0 = Math.toRadians((i * step + turn).toDouble()); val a1 = Math.toRadians(((i + 1) * step + turn).toDouble())
        val path = Path().apply {
            moveTo(centre.x, centre.y)
            lineTo(centre.x + (r * cos(a0)).toFloat(), centre.y + (r * sin(a0)).toFloat())
            lineTo(centre.x + (r * cos(a1)).toFloat(), centre.y + (r * sin(a1)).toFloat())
            close()
        }
        drawPath(path, ray)
    }
}

/**
 * Seconds since this first composed, ticking [fps] times a second, and not at all while no frames
 * are drawn (the app in the background). For backgrounds that move: one behind every page that
 * redrew at the display's rate made every pane of glass over it redraw as often.
 */
@Composable
fun slowTime(fps: Int = 20): androidx.compose.runtime.State<Float> {
    val t = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(fps) {
        val start = androidx.compose.runtime.withFrameMillis { it }
        while (true) {
            androidx.compose.runtime.withFrameMillis { t.floatValue = (it - start) / 1000f }
            kotlinx.coroutines.delay(1000L / fps)
        }
    }
    return t
}

/**
 * The music's low end, 0 to 1, eased so it swells and falls rather than flickers; 0 while nothing
 * plays. Watches the spectrum while it's on screen.
 */
@Composable
fun rememberLevel(fps: Int = 30): androidx.compose.runtime.State<Float> {
    val graph = androidx.compose.ui.platform.LocalContext.current.graph
    val now by graph.player.state.collectAsState()
    val level = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.DisposableEffect(Unit) { com.verza.playback.Spectrum.watch(); onDispose { com.verza.playback.Spectrum.unwatch() } }
    androidx.compose.runtime.LaunchedEffect(now.playing) {
        while (true) {
            val target = if (now.playing) com.verza.playback.Spectrum.bass else 0f
            val v = level.floatValue
            level.floatValue = v + (target - v) * (if (target > v) 0.45f else 0.12f)
            if (!now.playing && level.floatValue < 0.01f) { level.floatValue = 0f; break }
            kotlinx.coroutines.delay(1000L / fps)
        }
    }
    return level
}
