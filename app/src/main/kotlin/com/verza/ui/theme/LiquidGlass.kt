package com.verza.ui.theme

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.min

/**
 * Glass that shows what is behind it, the way Apple's Liquid Glass does: blurred, a little more
 * saturated, and bent at the rim, so a cover scrolling under the tab bar visibly curves around its
 * edge before it passes beneath.
 *
 * Compose has no backdrop filter, so the backdrop is recorded rather than read back. The thing the
 * glass floats over is drawn into a [GlassBackdrop] ([glassSource]); each pane ([liquidGlass])
 * redraws its own patch of that recording, offset to where the pane sits, through a RenderEffect:
 * blur, saturation, then (API 33+) an AGSL lens that samples outward near the rim. The recording is
 * a reference to the same display list the screen draws, so a scroll underneath updates the glass
 * without either side re-recording anything.
 *
 * - API 31-32: blur and saturation, no bending.
 * - Below 31: no RenderEffect at all; the pane is [liquidGlass]'s `fallback` fill.
 *
 * **A pane must not be inside the source it shows.** Glass inside the recorded content would draw
 * the recording of itself. That is why the NavHost is the source for the bottom chrome only, and
 * Now Playing records its own background for its buttons. Without a source in scope, a pane falls
 * back to its plain fill, which is what any glass inside a page gets.
 */
@Stable
class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
}

/** The backdrop glass in this subtree shows. Null: glass draws its fallback fill. */
val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/** Records this node's drawing into [backdrop] (and draws it as usual). */
fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/**
 * A pane of glass in [shape].
 *
 * @param tint laid over the blurred backdrop. Light glass in light mode, a smoky one in dark.
 * @param fallback the fill where there is nothing to blur (no source, or Android 11 and older):
 *   opaque enough to read on its own.
 * @param base drawn under the backdrop, for a source with transparent regions (pages that leave
 *   the window's own colour showing). Without it the sharp page would show through the blur.
 * @param rim the colour of the highlight that runs round the edge.
 * @param edge a hairline all the way round, for glass on a page as light as its rim.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color,
    fallback: Color,
    base: Color = Color.Unspecified,
    rim: Color = Color.White,
    edge: Color = Color.Transparent,
    blur: Dp = 10.dp,
    bend: Dp = 18.dp,
): Modifier {
    val backdrop = LocalGlassBackdrop.current
    val layer = rememberGraphicsLayer()
    val pane = remember { GlassPane() }
    val live = backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return this
        .onGloballyPositioned { pane.position = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            if (live) {
                if (base.isSpecified) drawRect(base)
                pane.drawBackdrop(this, backdrop!!, layer, outline, (shape as? com.verza.ui.expressive.SquircleShape)?.radius?.toPx(), blur.toPx(), bend.toPx())
                drawRect(tint)
            } else {
                drawRect(fallback)
            }
            drawRim(outline, rim)
            if (edge.alpha > 0f) drawOutline(outline, edge, style = Stroke(width = 2.dp.toPx() / 1.5f))
        }
}

/**
 * The specular edge: bright along the top-left where the light is, fainter bottom-right, nothing
 * in the middle. Twice the hairline wide because the pane clips it to its outline, which keeps the
 * inner half.
 */
private fun DrawScope.drawRim(outline: Outline, rim: Color) {
    val hair = 1.dp.toPx()
    drawOutline(
        outline,
        Brush.linearGradient(
            0f to rim.copy(alpha = 0.50f),
            0.3f to rim.copy(alpha = 0.10f),
            0.7f to rim.copy(alpha = 0.04f),
            1f to rim.copy(alpha = 0.26f),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        style = Stroke(width = hair * 2f),
    )
    // A soft inner glow so the edge reads as thickness rather than as a line drawn on.
    drawOutline(outline, rim.copy(alpha = 0.05f), style = Stroke(width = hair * 7f))
}

private class GlassPane {
    var position by mutableStateOf(Offset.Zero)
    private var key: List<Float>? = null
    private var effect: RenderEffect? = null

    fun drawBackdrop(
        scope: DrawScope,
        backdrop: GlassBackdrop,
        layer: GraphicsLayer,
        outline: Outline,
        squircleRadius: Float?,
        blurPx: Float,
        bendPx: Float,
    ) = with(scope) {
        // The recording runs past the pane on every side, so the blur and the lens have real
        // pixels to pull in at the edge instead of smearing the border or reading transparency.
        val margin = ceil(blurPx * 2f + bendPx)
        val radius = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: squircleRadius ?: 0f
        val bezel = min(bendPx, min(size.width, size.height) * 0.45f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            layer.renderEffect = effectFor(size.width, size.height, radius, margin, blurPx, bezel)
        }
        layer.record(IntSize((size.width + margin * 2).toInt(), (size.height + margin * 2).toInt())) {
            translate(backdrop.origin.x - position.x + margin, backdrop.origin.y - position.y + margin) {
                drawLayer(backdrop.layer)
            }
        }
        translate(-margin, -margin) { drawLayer(layer) }
    }

    /** Built once per size: the lens's uniforms are fixed into the RenderEffect when it is made. */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun effectFor(w: Float, h: Float, radius: Float, margin: Float, blur: Float, bezel: Float): RenderEffect {
        val k = listOf(w, h, radius, margin, blur, bezel)
        effect?.let { if (k == key) return it }
        // Bend first, then frost. Blurring last is what real frosted glass does, and it is what
        // keeps the bent rim soft: blurred first, the bent band kept a sharp inner contour, a
        // mirrored copy of the outline that read as the effect stopping short of the edge.
        val bent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && bezel > 1f) {
            val lens = RuntimeShader(LENS).apply {
                setFloatUniform("origin", margin, margin)
                setFloatUniform("size", w, h)
                setFloatUniform("radius", min(radius, min(w, h) / 2f))
                setFloatUniform("bezel", bezel)
                setFloatUniform("strength", bezel * 0.45f)
                setFloatUniform("shine", 0.08f)
            }
            android.graphics.RenderEffect.createRuntimeShaderEffect(lens, "content")
        } else {
            null
        }
        val vivid = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.45f) })
        val tinted = if (bent != null) android.graphics.RenderEffect.createColorFilterEffect(vivid, bent)
        else android.graphics.RenderEffect.createColorFilterEffect(vivid)
        val fx = android.graphics.RenderEffect.createBlurEffect(blur, blur, tinted, Shader.TileMode.CLAMP)
        return fx.asComposeRenderEffect().also { effect = it; key = k }
    }
}

/**
 * The lens. Inside the rim (the last [bezel] pixels before the edge of a rounded rectangle) each
 * pixel samples further out along the edge's normal, growing with the square of how close it is to
 * the edge: content from just beyond the pane is pulled in and bent round it. Not a true circular
 * bevel, whose slope is vertical at the edge and squeezed the rim into a hard line. Red and blue
 * are bent by slightly different amounts, which is the faint colour fringe real glass has, and the
 * top-left of the rim is lifted a little, as if lit.
 */
private const val LENS = """
uniform shader content;
uniform float2 origin;
uniform float2 size;
uniform float radius;
uniform float bezel;
uniform float strength;
uniform float shine;

float sdf(float2 p) {
    float2 h = size * 0.5;
    float2 q = abs(p - origin - h) - (h - float2(radius));
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

half4 main(float2 p) {
    float d = sdf(p);
    if (d >= 0.0 || d <= -bezel) {
        return content.eval(p);
    }
    float2 e = float2(0.75, 0.0);
    float2 n = float2(sdf(p + e.xy) - sdf(p - e.xy), sdf(p + e.yx) - sdf(p - e.yx));
    float len = length(n);
    if (len > 0.0001) {
        n = n / len;
    }
    float t = 1.0 + d / bezel;
    float k = t * t;
    float2 off = n * (k * strength);
    half4 c = content.eval(p + off);
    half4 cr = content.eval(p + off * 1.12);
    half4 cb = content.eval(p + off * 0.88);
    if (cr.a > 0.01) { c.r = cr.r / cr.a * c.a; }
    if (cb.a > 0.01) { c.b = cb.b / cb.a * c.a; }
    float lit = clamp(-n.y * 0.7 - n.x * 0.3, 0.0, 1.0);
    c.rgb = c.rgb + half3(shine * k * lit) * c.a;
    return c;
}
"""
