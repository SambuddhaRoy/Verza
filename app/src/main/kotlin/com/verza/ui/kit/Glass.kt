package com.verza.ui.kit

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
 */

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
 * radius (half the height for a capsule), which the lens bends within.
 */
@Composable
fun Modifier.glass(shape: Shape, corner: Dp, pressed: Boolean = false): Modifier {
    val backdrop = LocalBackdrop.current
    val p = LocalPalette.current
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
                drawPath(path, tint)
                if (press > 0f) drawPath(path, Color.White.copy(alpha = 0.12f * press))
                drawPath(path, rim, style = Stroke(stroke))
                if (!p.dark) drawPath(path, Color.Black.copy(alpha = 0.05f), style = Stroke(stroke / 2))
            }
        }
}

/** A round glass button with one symbol: the way back, a page's menu, search. */
@Composable
fun GlassButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp, tint: Color = LocalPalette.current.ink, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier.size(size).glass(CircleShape, size / 2, pressed)
            .clickable(source, null, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

/** Blur, a lift in saturation, then (on 13 and up) the lens at the rim. */
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
        setFloatUniform("amount", 7f * density)
    }
    return RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(lens, "content"), vivid)
}

// Each point near the rim shows what is a little further in, most at the rim itself: the edge
// magnifies, the way the thick edge of a lens does. The inside past the band is left as it is.
private const val LENS = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float band;
uniform float amount;

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
    return content.eval(coord - n * amount * bend);
}
"""
