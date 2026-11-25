package com.verza.ui.kit

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.runtime.State
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verza.ui.theme.LocalPalette

/*
 * Liquid glass, after Apple's (HIG, Materials): a layer for controls and navigation that floats over
 * the content, never part of it. A pane shows what is behind it, blurred and a little more saturated,
 * bent inward at its rim as a lens bends light at its edge, under a faint tint, with its top edge lit.
 *
 * Compose has no backdrop filter, so the content behind is recorded into a layer once a frame
 * ([backdrop]) and every pane in front draws that layer again under itself, with the blur and the
 * lens as a RenderEffect on a layer of its own ([glass]). A pane must not be inside the content it
 * shows: it would be drawing itself. So the content and the panes over it are siblings ([Glassed]).
 *
 * The blur needs Android 12, the lens 13. Below 12 a pane is the tint alone, nearly opaque.
 *
 * Where Verza's glass parts from Apple's: Apple's has no colour of its own. Verza's takes the playing
 * song's. A pane can be [stain]ed with the accent (the selected tab, play and pause), or hold
 * something of the song [inside] it (the mini player holds its cover's colours).
 *
 * And it disperses, as thick glass does: at the rim, red is bent further than green and blue less,
 * so what shows through splits into rainbow fringes; and where the light catches the rim there's a
 * thin spectral glint, with a fainter one opposite. The light follows the phone's tilt ([LocalTilt]):
 * tip it and the glint slides round the edge.
 */

/** Which way the phone leans, each axis -1 to 1; where the light sits on the glass. */
val LocalTilt = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

/**
 * The phone's lean from its gravity sensor, smoothed, while the app is in front; still otherwise. Small
 * changes are ignored, so a phone at rest doesn't keep the glass redrawing.
 */
@Composable
fun rememberTilt(): State<Offset> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        val sensors = context.getSystemService(android.hardware.SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(android.hardware.Sensor.TYPE_GRAVITY) ?: sensors?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        var gx = 0f; var gy = 9.81f * 0.75f
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(e: android.hardware.SensorEvent) {
                gx += (e.values[0] - gx) * 0.12f
                gy += (e.values[1] - gy) * 0.12f
                val next = Offset((-gx / 9.81f).coerceIn(-1f, 1f), ((0.75f - gy / 9.81f) * 2f).coerceIn(-1f, 1f))
                if ((next - tilt.value).getDistance() > 0.015f) tilt.value = next
            }
            override fun onAccuracyChanged(s: android.hardware.Sensor?, a: Int) {}
        }
        if (sensor != null) sensors.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
        onPauseOrDispose { sensors?.unregisterListener(listener) }
    }
    return tilt
}

/** The spectrum, for the glint on the rim. */
private val Rainbow = listOf(
    Color(0xFFFF2D55), Color(0xFFFF9500), Color(0xFFFFE600), Color(0xFF34C759), Color(0xFF00C7FF), Color(0xFF5856D6), Color(0xFFAF52DE), Color(0xFFFF2D55),
)

/** The content behind a layer of glass, as last drawn. */
@Stable
class Backdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
}

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }


/**
 * [content], with [glass] floating over it: anything in [glass] drawn with [Modifier.glass] shows
 * [content] through it. [ground] is drawn under the content, so the glass never shows a hole.
 */
@Composable
fun Glassed(ground: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit, glass: @Composable BoxScope.() -> Unit) {
    val layer = rememberGraphicsLayer()
    val backdrop = remember(layer) { Backdrop(layer) }
    Box(modifier) {
        Box(
            Modifier.matchParentSize()
                .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
                .drawWithContent {
                    layer.record {
                        drawRect(ground)
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(layer)
                },
            content = content,
        )
        CompositionLocalProvider(LocalBackdrop provides backdrop) { glass() }
    }
}

/**
 * A pane of glass in [shape] over the nearest [Glassed] content. [corner] is the shape's corner
 * radius (half the height for a capsule), which the lens bends within. [stain] colours the glass;
 * [inside] is drawn into it, under its tint.
 */
@Composable
fun Modifier.glass(shape: Shape, corner: Dp, pressed: Boolean = false, stain: Color? = null, inside: (DrawScope.() -> Unit)? = null): Modifier {
    val backdrop = LocalBackdrop.current
    val p = LocalPalette.current
    val tilt = LocalTilt.current
    val layer = rememberGraphicsLayer()
    var at by remember { mutableStateOf(Offset.Zero) }
    // Pressed, the pane swells and brightens, as Apple's does under a finger.
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "press")
    val see = backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val tint = when {
        !see -> if (p.dark) Color(0xFF1C1C1E).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f)
        p.dark -> Color(0xFF26262A).copy(alpha = 0.58f)
        else -> Color.White.copy(alpha = 0.6f)
    }
    return this
        .graphicsLayer { val s = 1f + 0.06f * press; scaleX = s; scaleY = s }
        .shadow(if (p.dark) 10.dp else 14.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = if (p.dark) 0.5f else 0.14f), spotColor = Color.Black.copy(alpha = if (p.dark) 0.6f else 0.2f))
        .onGloballyPositioned { at = it.positionInRoot() }
        .drawWithCache {
            val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
            if (see && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) layer.renderEffect = glassEffect(size.width, size.height, corner.toPx(), density).asComposeRenderEffect()
            val stroke = 1.dp.toPx()
            val glintWidth = 2.dp.toPx()
            val spectral = Brush.sweepGradient(Rainbow, Offset(size.width / 2, size.height / 2))
            // Lit along the top, fading round the sides, a little light again at the foot.
            val rim = Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (p.dark) 0.34f else 0.95f),
                0.45f to Color.White.copy(alpha = if (p.dark) 0.06f else 0.35f),
                1f to Color.White.copy(alpha = if (p.dark) 0.16f else 0.7f),
            )
            onDrawBehind {
                if (see) {
                    val shift = backdrop!!.origin - at
                    layer.record { translate(shift.x, shift.y) { drawLayer(backdrop.layer) } }
                    clipPath(path) { drawLayer(layer) }
                }
                if (inside != null) clipPath(path) { inside() }
                drawPath(path, tint)
                if (stain != null) drawPath(path, Brush.verticalGradient(
                    0f to stain.copy(alpha = if (p.dark) 0.62f else 0.34f),
                    1f to stain.copy(alpha = if (p.dark) 0.38f else 0.2f),
                ))
                if (press > 0f) drawPath(path, Color.White.copy(alpha = 0.12f * press))
                drawPath(path, rim, style = Stroke(stroke))
                if (!p.dark) drawPath(path, Color.Black.copy(alpha = 0.05f), style = Stroke(stroke / 2))
                // The glint: the spectrum round the rim, shown only near where the light falls, and
                // faintly opposite. Where the light falls moves as the phone tilts.
                val t = tilt.value
                val light = Offset(size.width * (0.28f + 0.4f * t.x).coerceIn(0f, 1f), size.height * (0.45f * t.y).coerceIn(0f, 1f))
                val reach = maxOf(size.width, size.height) * 0.42f
                // Inside the rim, a soft band of the spectrum: the light split by the glass's thick edge.
                clipPath(path) { glint(path, spectral, light, reach * 1.15f, glintWidth * 5f, if (p.dark) 0.3f else 0.24f) }
                glint(path, spectral, light, reach, glintWidth, if (p.dark) 0.95f else 0.8f)
                glint(path, spectral, Offset(size.width - light.x, size.height - light.y), reach * 0.7f, glintWidth, if (p.dark) 0.5f else 0.4f)
            }
        }
}

/** [path]'s rim in [brush], kept only within [reach] of [at]: a glint where light catches the edge. */
private fun DrawScope.glint(path: Path, brush: Brush, at: Offset, reach: Float, width: Float, alpha: Float) {
    drawIntoCanvas { canvas ->
        canvas.saveLayer(androidx.compose.ui.geometry.Rect(Offset(-width, -width), androidx.compose.ui.geometry.Size(size.width + 2 * width, size.height + 2 * width)), androidx.compose.ui.graphics.Paint())
        drawPath(path, brush, alpha = alpha, style = Stroke(width))
        drawRect(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0.35f), Color.Transparent), at, reach), Offset(-width, -width), androidx.compose.ui.geometry.Size(size.width + 2 * width, size.height + 2 * width), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
        canvas.restore()
    }
}

/** A round glass button with one symbol: the way back, a page's menu, search. */
@Composable
fun GlassButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp, tint: Color = LocalPalette.current.ink, stain: Color? = null, iconSize: Dp = size * 0.5f, iconModifier: Modifier = Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier.size(size).glass(CircleShape, size / 2, pressed, stain)
            .clickable(source, null, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint, modifier = iconModifier.size(iconSize)) }
}

/** Blur, a lift in saturation, then (on 13 and up) the lens at the rim, splitting the colours as it bends them. */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
private fun glassEffect(width: Float, height: Float, corner: Float, density: Float): RenderEffect {
    val blur = RenderEffect.createBlurEffect(18f * density, 18f * density, android.graphics.Shader.TileMode.CLAMP)
    val vivid = RenderEffect.createColorFilterEffect(
        android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(1.5f) }), blur,
    )
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || width <= 0f || height <= 0f) return vivid
    val lens = RuntimeShader(LENS).apply {
        setFloatUniform("size", width, height)
        setFloatUniform("radius", corner.coerceAtMost(minOf(width, height) / 2f))
        setFloatUniform("band", minOf(corner, 16f * density))
        setFloatUniform("amount", 9f * density)
        setFloatUniform("spread", 0.85f)
    }
    return RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(lens, "content"), vivid)
}

// Each point near the rim shows what is a little further in, most at the rim itself: the edge
// magnifies, the way the thick edge of a lens does. And it disperses: red is taken from further in
// than green, blue from less far, so edges inside the glass split into fringes of colour.
private const val LENS = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float band;
uniform float amount;
uniform float spread;

float edge(float2 p, float2 hs, float r) {
    float2 q = abs(p) - hs + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 coord) {
    float2 hs = size * 0.5;
    float2 p = coord - hs;
    float d = edge(p, hs, radius);
    float depth = clamp(-d / band, 0.0, 1.0);
    if (depth >= 1.0) return content.eval(coord);
    float2 n = float2(edge(p + float2(1.0, 0.0), hs, radius) - edge(p - float2(1.0, 0.0), hs, radius),
                      edge(p + float2(0.0, 1.0), hs, radius) - edge(p - float2(0.0, 1.0), hs, radius));
    n = n / max(length(n), 0.0001);
    float bend = (1.0 - depth) * (1.0 - depth);
    float2 o = n * amount * bend;
    half4 c = content.eval(coord - o);
    half r = content.eval(coord - o * (1.0 + spread)).r;
    half b = content.eval(coord - o * (1.0 - spread)).b;
    return half4(r, c.g, b, c.a);
}
"""
