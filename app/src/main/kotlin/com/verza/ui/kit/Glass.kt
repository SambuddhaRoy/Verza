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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
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
 * The rim is lit along the top. (2.5.0's glass split the light into rainbows at the rim, and until
 * 2.7.1 a glint slid round it as the phone tilted; both went, the second as decoration.)
 *
 * Since 2.7.0 (Soft glass) a pane is tinted with the page's own colour and lifted off the page by the
 * soft surfaces' two shadows ([raise]) rather than a drop shadow. Since 2.7.1 it is fluted, not frosted:
 * clear glass ribbed in narrow vertical flutes ([flutes]), texture where there was blur.
 */

/**
 * Glass on this page is clear: Now Playing and Lyrics provide it, so the flowing cover shows through
 * their buttons' flutes. Elsewhere the glass is tinted with the page, for the words on it.
 */
val LocalClearGlass = staticCompositionLocalOf { false }

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
                        if (ground.alpha > 0f) drawRect(ground)
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
    val clear = LocalClearGlass.current
    val layer = rememberGraphicsLayer()
    var at by remember { mutableStateOf(Offset.Zero) }
    // Pressed, the pane swells and brightens, and its flutes slide and turn over and its rim bends
    // harder (read in the cache below, so the effect is rebuilt only while the press moves). The spring
    // is loose, so on letting go the refraction wobbles back like something liquid settling.
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow), label = "press")
    val see = backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // Frosted in the page's own material, so a pane is the page lifted, not a sheet laid on it.
    val tint = p.canvas.copy(alpha = if (!see) 0.96f else if (clear) (if (p.dark) 0.2f else 0.26f) else if (p.dark) 0.58f else 0.55f)
    return this
        .graphicsLayer { val s = 1f + 0.06f * press; scaleX = s; scaleY = s }
        .onGloballyPositioned { at = it.positionInRoot() }
        .drawWithCache {
            val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
            if (see && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) layer.renderEffect = glassEffect(size.width, size.height, corner.toPx(), density, clear, press).asComposeRenderEffect()
            val stroke = 1.dp.toPx()
            // Lit along the top, fading round the sides, a little light again at the foot.
            val rim = Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (p.dark) 0.22f else 0.9f),
                0.45f to Color.White.copy(alpha = if (p.dark) 0.04f else 0.3f),
                1f to Color.White.copy(alpha = if (p.dark) 0.08f else 0.5f),
            )
            // Lifted off the page as the soft surfaces are: lit at the top left, shadowed under.
            val lift = minOf(7.dp.toPx(), minOf(size.width, size.height) * 0.12f)
            onDrawBehind {
                raise(path, p.canvas, lift, p)
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
            }
        }
}

/** A round glass button with one symbol: the way back, a page's menu, search. [inside] is drawn into its glass. */
@Composable
fun GlassButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp, tint: Color = LocalPalette.current.ink, stain: Color? = null, iconSize: Dp = size * 0.5f, iconModifier: Modifier = Modifier, inside: (DrawScope.() -> Unit)? = null, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val tick = rememberTick()
    Box(
        modifier.size(size).glass(CircleShape, size / 2, pressed, stain, inside)
            .clickable(source, null) { tick(); onClick() }
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint, modifier = iconModifier.size(iconSize)) }
}

/** The like's pink, a little deeper by day so the heart holds on a pale ground. */
fun likePink(dark: Boolean) = if (dark) Color(0xFFFF5C95) else Color(0xFFE5326F)

/**
 * Like, in glass: held down, the glass fills with pink light and glows past its rim; liking flares the
 * glow and fades it over most of a second, with a firmer tick. A liked heart is pink.
 */
@Composable
fun LikeButton(liked: Boolean, modifier: Modifier = Modifier, size: Dp = 40.dp, onClick: () -> Unit) {
    val p = LocalPalette.current
    val pink = likePink(p.dark)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val held by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "held")
    val flare = remember { androidx.compose.animation.core.Animatable(0f) }
    var was by remember { mutableStateOf(liked) }
    androidx.compose.runtime.LaunchedEffect(liked) {
        if (liked && !was) { flare.snapTo(1f); flare.animateTo(0f, androidx.compose.animation.core.tween(900)) }
        was = liked
    }
    val tick = rememberTick()
    val confirm = rememberTick(ConfirmTick)
    Box(
        modifier.size(size)
            .drawBehind {
                val glow = maxOf(held, flare.value)
                if (glow > 0f) drawCircle(
                    Brush.radialGradient(listOf(pink.copy(alpha = 0.55f * glow), pink.copy(alpha = 0.18f * glow), Color.Transparent), center, this.size.minDimension * 1.1f),
                    this.size.minDimension * 1.1f,
                )
            }
            .glass(CircleShape, size / 2, pressed, inside = { drawRect(pink, alpha = 0.5f * maxOf(held, flare.value)) })
            .clickable(source, null) { if (liked) tick() else confirm(); onClick() }
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (liked) androidx.compose.material.icons.Icons.Rounded.Favorite else androidx.compose.material.icons.Icons.Rounded.FavoriteBorder,
            if (liked) "Unlike" else "Like", tint = if (liked) pink else p.ink, modifier = Modifier.size(size * 0.5f).popOn(liked),
        )
    }
}

/**
 * A little blur and a lift in saturation, then (on 13 and up) the flutes and the lens at the rim. Below
 * 13 there are no runtime shaders, and a pane is frosted instead (more blur, no flutes).
 */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
private fun glassEffect(width: Float, height: Float, corner: Float, density: Float, clear: Boolean, press: Float): RenderEffect {
    val fluted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    // Clear glass is barely blurred, so the flowing cover keeps its shape in the flutes; the dock's a
    // little more, so the words on it stay readable over a busy cover.
    val blurBy = (if (!fluted) 18f else if (clear) 4f else 9f) * density
    val blur = RenderEffect.createBlurEffect(blurBy, blurBy, android.graphics.Shader.TileMode.CLAMP)
    val vivid = RenderEffect.createColorFilterEffect(
        android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(1.5f) }), blur,
    )
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || width <= 0f || height <= 0f) return vivid
    // Clear glass (Now Playing) shows its flutes fully; the dock's are quieter and wider, since words
    // sit on it and strong stripes cut through them.
    // Pressed, each flute turns over (bend runs through zero, magnifying, then flips the other way), the
    // flutes slide half a flute sideways and their curve deepens.
    val ground = if (clear) flutes(6f * density * (1f - 0.2f * press), -1.8f + 3.4f * press, 0.22f + 0.12f * press, vivid, phase = 0.5f * press)
        else flutes(9f * density, -1.4f + 2.8f * press, 0.07f + 0.1f * press, vivid, phase = 0.5f * press)
    val lens = RuntimeShader(LENS).apply {
        setFloatUniform("size", width, height)
        setFloatUniform("radius", corner.coerceAtMost(minOf(width, height) / 2f))
        setFloatUniform("band", minOf(corner, 16f * density))
        setFloatUniform("amount", 9f * density * (1f + 0.9f * press))
    }
    return RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(lens, "content"), ground)
}

/**
 * [under] seen through fluted glass: vertical flutes [rib] px wide, each showing what's behind it
 * flipped and squeezed by [bend] flutes' worth ([FLUTE]), [shade] how much each one's curve darkens
 * into its seams and catches the light on its left shoulder, [phase] how far they're slid sideways (in
 * flutes). [sheen] is how much of that light is added as white; with none it only brightens the colour
 * already there, so the colour keeps its saturation (Now Playing's ground). Android 13 and up.
 */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun flutes(rib: Float, bend: Float, shade: Float, under: RenderEffect?, phase: Float = 0f, sheen: Float = 0.55f): RenderEffect {
    val flute = RuntimeShader(FLUTE).apply {
        setFloatUniform("rib", rib)
        setFloatUniform("bend", bend)
        setFloatUniform("shade", shade)
        setFloatUniform("phase", phase)
        setFloatUniform("sheen", sheen)
    }
    val effect = RenderEffect.createRuntimeShaderEffect(flute, "content")
    return if (under == null) effect else RenderEffect.createChainEffect(effect, under)
}

// A point in a flute shows the point across its flute's middle, scaled by bend (negative flips it,
// as a convex rib does), so each flute holds a narrow, mirrored slice of what's behind. The curve:
// dark at the seams, a bright band on the left shoulder where the light catches it.
private const val FLUTE = """
uniform shader content;
uniform float rib;
uniform float bend;
uniform float shade;
uniform float phase;
uniform float sheen;

half4 main(float2 p) {
    float k = p.x / rib + phase;
    float u = fract(k);
    float middle = (floor(k) + 0.5) * rib;
    float4 c = float4(content.eval(float2(middle + (u - 0.5) * rib * bend, p.y)));
    float curve = sin(u * 3.14159265);
    float lit = smoothstep(0.12, 0.3, u) * (1.0 - smoothstep(0.3, 0.52, u));
    c.rgb = c.rgb * (1.0 - shade * (1.0 - curve) + shade * 0.9 * lit) + shade * sheen * lit * c.a;
    return half4(c);
}
"""

// Each point near the rim shows what is a little further in, most at the rim itself: the edge
// magnifies, the way the thick edge of a lens does.
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
