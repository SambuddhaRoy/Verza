package com.verza.ui.nowplaying

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlin.math.max
import kotlin.math.min

/**
 * The cover, blurred and slowly warped with simplex noise: the desktop's Now Playing background
 * (renderer.js, drawFluid), the same shader in AGSL. Dark, it's dimmed so white type reads over a white
 * cover; light, it's washed pale for black type. How far follows the cover's brightness. [fluted]: it's
 * seen through fluted glass (`flutes` in Glass.kt), wide flutes over the blur. Before Android 13 there
 * are no runtime shaders, and the still blur stands in. (Until 2.8.0 the bass could stir it, measured by
 * playback/Spectrum.kt, which also fed a level meter under the cover; the listener found the meter too
 * busy, and both went.)
 */
@Composable
fun FlowingCover(art: String?, dark: Boolean, modifier: Modifier = Modifier, fluted: Boolean = false, ambient: Boolean = false) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, art) {
        if (art == null) { value = null; return@produceState }
        val request = ImageRequest.Builder(context).data(art).size(96).allowHardware(false).build()
        value = SingletonImageLoader.get(context).execute(request).image?.toBitmap()
    }
    if (Build.VERSION.SDK_INT < 33 || bitmap == null) {
        AsyncImage(art, null, contentScale = ContentScale.Crop, modifier = modifier.fillMaxSize().blur(60.dp), alpha = if (dark) 0.45f else 0.35f)
        return
    }
    Flowing(bitmap!!, dark, modifier, fluted, ambient)
}

@RequiresApi(33)
@Composable
private fun Flowing(bitmap: Bitmap, dark: Boolean, modifier: Modifier, fluted: Boolean, ambient: Boolean) {
    val shader = remember { RuntimeShader(FLUID) }
    val lum = remember(bitmap) { brightness(bitmap) }
    remember(bitmap) {
        shader.setInputShader("cover", BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { filterMode = BitmapShader.FILTER_MODE_LINEAR })
        shader.setFloatUniform("img", bitmap.width.toFloat(), bitmap.height.toFloat())
    }
    var time by remember { mutableFloatStateOf(0f) }
    // Thirty frames a second while it's on screen, fifteen and slower behind the pages (where every
    // glass pane over it redraws with it); the composition leaving stops it.
    LaunchedEffect(ambient) {
        val start = withFrameNanos { it }
        val step = if (ambient) 66_000_000L else 33_000_000L
        var last = 0L
        while (true) withFrameNanos { t ->
            if (t - last >= step) { last = t; time = (t - start) / 1e9f * (if (ambient) 0.6f else 1f) }
        }
    }
    val brush = remember(shader) { object : ShaderBrush() { override fun createShader(size: Size) = shader } }
    val density = LocalDensity.current.density
    val effect = remember(fluted, density) {
        val blur = android.graphics.RenderEffect.createBlurEffect(24f * density, 24f * density, Shader.TileMode.CLAMP)
        // No added white in the flutes: they only shade the colour, so it keeps its saturation.
        (if (fluted) com.verza.ui.kit.flutes(GroundFlutes.RIB * density, GroundFlutes.BEND, GroundFlutes.SHADE, blur, sheen = 0f) else blur).asComposeRenderEffect()
    }
    Box(modifier.fillMaxSize().graphicsLayer { renderEffect = effect; clip = true }.drawBehind {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("time", time * 1.25f)
        shader.setFloatUniform("intensity", 0.9f)
        // Dark: dimmed a little further than it was, the colour kept, so it doesn't pull the eye. Light:
        // washed toward white about half as hard as it was (0.7 to 0.9 had nearly lost the cover), which
        // still holds black type at 4.5:1 over a dark cover.
        shader.setFloatUniform("dim", if (dark) (0.62f - 0.28f * lum) * 0.85f else 0.92f)
        shader.setFloatUniform("veil", if (dark) 0f else 0.42f + 0.16f * (1f - lum))
        drawRect(brush)
    })
}

/**
 * The fluted glass over Now Playing's ground: flutes [RIB] dp wide, bent and shaded so. A 3D cover
 * (DeepCover.kt) draws the same flutes in the same places over its blur, so the two meet unbroken.
 */
internal object GroundFlutes {
    const val RIB = 22f
    const val BEND = -1.6f
    const val SHADE = 0.14f
}

/** How bright a picture is, 0 to 1. */
private fun brightness(b: Bitmap): Float {
    var sum = 0f; var n = 0
    val step = max(1, min(b.width, b.height) / 16)
    for (y in 0 until b.height step step) for (x in 0 until b.width step step) {
        val c = b.getPixel(x, y)
        sum += (0.2126f * ((c shr 16) and 255) + 0.7152f * ((c shr 8) and 255) + 0.0722f * (c and 255)) / 255f; n++
    }
    return if (n == 0) 0.5f else sum / n
}

private const val FLUID = """
uniform shader cover;
uniform float2 size;
uniform float2 img;
uniform float time;
uniform float intensity;
uniform float dim;
uniform float veil;
float3 mod289(float3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
float2 mod289(float2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
float3 permute(float3 x) { return mod289(((x * 34.0) + 1.0) * x); }
float snoise(float2 v) {
  const float4 C = float4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
  float2 i = floor(v + dot(v, C.yy));
  float2 x0 = v - i + dot(i, C.xx);
  float2 i1 = (x0.x > x0.y) ? float2(1.0, 0.0) : float2(0.0, 1.0);
  float4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod289(i);
  float3 p = permute(permute(i.y + float3(0.0, i1.y, 1.0)) + i.x + float3(0.0, i1.x, 1.0));
  float3 m = max(0.5 - float3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
  m = m * m; m = m * m;
  float3 x = 2.0 * fract(p * C.www) - 1.0;
  float3 h = abs(x) - 0.5;
  float3 ox = floor(x + 0.5);
  float3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
  float3 g;
  g.x = a0.x * x0.x + h.x * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}
half4 main(float2 fc) {
  float2 uv = fc / size;
  float2 z = (uv - 0.5) / 1.3 + 0.5;
  float t = time * 0.05;
  float cw = 1.0 - smoothstep(0.0, 0.7, length(z - 0.5));
  float n1 = snoise(z * 0.35 + float2(t, t * 0.7));
  float n2 = snoise(z * 0.35 + float2(-t * 0.8, t * 0.5) + float2(50.0, 50.0));
  float n3 = snoise(z * 0.9 + float2(t * 1.2, -t) + float2(100.0, 0.0));
  float n4 = snoise(z * 0.9 + float2(-t, t * 1.1) + float2(0.0, 100.0));
  float2 warp = float2(n1 * 0.65 + n3 * 0.35, n2 * 0.65 + n4 * 0.35) * cw;
  float2 w = clamp(z + warp * intensity, 0.0, 1.0);
  float3 col = cover.eval(w * img).rgb;
  float gray = dot(col, float3(0.299, 0.587, 0.114));
  col = mix(float3(gray), col, 1.5);
  float2 c = uv - 0.5;
  col *= 1.0 - dot(c, c) * 0.6;
  col = mix(col * dim, float3(1.0), veil);
  return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
}
"""
