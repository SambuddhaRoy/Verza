package com.verza.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware

// Matches the desktop pipeline exactly: a 160px texture with the cover drawn oversized (offset -16,
// size 192) so the 9px Gaussian blur has no hard edge.
private const val COVER_PX = 160
private const val OVERSCAN = 16

// AGSL port of the desktop "Kawarp" cover-flow shader: two pre-blurred cover textures, UVs
// domain-warped by 2-octave simplex noise (warped more toward the centre), saturated, vignetted,
// darkened for text legibility, dithered; uMix crossfades A→B on track change.
private const val COVER_FLOW_SRC = """
uniform shader uTexA;
uniform shader uTexB;
uniform float uMix;
uniform float uTime;
uniform float uIntensity;
uniform float uSat;
uniform float uDither;
uniform float uScale;
uniform float uTexSize;
uniform float2 uRes;

float3 mod289(float3 x){ return x - floor(x * (1.0/289.0)) * 289.0; }
float2 mod289(float2 x){ return x - floor(x * (1.0/289.0)) * 289.0; }
float3 permute(float3 x){ return mod289(((x*34.0)+1.0)*x); }
float snoise(float2 v){
  float4 C = float4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
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

float3 sampleWarp(float2 uv){
  float2 z = (uv - 0.5) / uScale + 0.5;
  float t = uTime * 0.05;
  float2 c = z - 0.5;
  float cw = 1.0 - smoothstep(0.0, 0.7, length(c));
  float n1 = snoise(z * 0.35 + float2(t, t * 0.7));
  float n2 = snoise(z * 0.35 + float2(-t * 0.8, t * 0.5) + float2(50.0, 50.0));
  float n3 = snoise(z * 0.9  + float2(t * 1.2, -t)       + float2(100.0, 0.0));
  float n4 = snoise(z * 0.9  + float2(-t, t * 1.1)        + float2(0.0, 100.0));
  float2 warp = float2(n1 * 0.65 + n3 * 0.35, n2 * 0.65 + n4 * 0.35) * cw;
  float2 wuv = clamp(z + warp * uIntensity, 0.0, 1.0);
  float2 px = wuv * uTexSize;
  float3 a = float3(uTexA.eval(px).rgb);
  float3 b = float3(uTexB.eval(px).rgb);
  return mix(a, b, uMix);
}

half4 main(float2 fragCoord){
  float2 uv = fragCoord / uRes;
  float3 col = sampleWarp(uv);
  float2 cen = uv - 0.5;
  float vig = 1.0 - dot(cen, cen) * 0.5;
  col *= vig;
  float gray = dot(col, float3(0.299, 0.587, 0.114));
  col = mix(float3(gray), col, uSat);
  col *= 0.82;
  float n = fract(sin(dot(floor(uv * uRes), float2(12.9898, 78.233)) + floor(uTime * 60.0)) * 43758.5453);
  col += (n - 0.5) * uDither;
  return half4(half3(col), 1.0);
}
"""

/**
 * The flowing, blurred, domain-warped wash of the current cover art — the app-wide [GlowStyle.COVER]
 * background. API 33+ only (AGSL); call-site guards on this. Animates continuously like the Fluid /
 * Halftone styles; the frame clock pauses on its own when the app is backgrounded.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun CoverFlowBackground(
    artworkUrl: String?,
    movement: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shader = remember { runCatching { RuntimeShader(COVER_FLOW_SRC) }.getOrNull() } ?: return

    var texA by remember { mutableStateOf<Bitmap?>(null) }
    var texB by remember { mutableStateOf<Bitmap?>(null) }
    val mix = remember { Animatable(0f) }

    LaunchedEffect(artworkUrl) {
        val bmp = artworkUrl?.let { loadBlurredCover(context, it) } ?: return@LaunchedEffect
        if (texA == null) {
            texA = bmp; texB = bmp; mix.snapTo(0f)
        } else {
            texB = bmp
            mix.snapTo(0f)
            mix.animateTo(1f, tween(700, easing = LinearEasing))  // linear 700ms, like desktop
            texA = bmp; mix.snapTo(0f)       // promote, ready for the next change
        }
    }

    val shaderA = remember(texA) { texA?.let { it.toClampShader() } }
    val shaderB = remember(texB) { texB?.let { it.toClampShader() } }
    if (shaderA == null || shaderB == null) return

    // Frame clock, throttled to ~35fps; pauses on its own when the app is backgrounded (no frames).
    // movement (0..1) scales speed, matching desktop's uTime = elapsed*(0.5 + movement*1.5).
    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(movement) {
        val speed = 0.5f + movement.coerceIn(0f, 1f) * 1.5f
        var prev = 0L
        var acc = 0f
        while (true) {
            withFrameNanos { now ->
                if (prev != 0L) acc += (now - prev) / 1e9f
                prev = now
                if (acc >= 0.028f) { timeSec += acc * speed; acc = 0f }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                shader.setInputShader("uTexA", shaderA)
                shader.setInputShader("uTexB", shaderB)
                shader.setFloatUniform("uRes", size.width, size.height)
                shader.setFloatUniform("uTexSize", COVER_PX.toFloat())
                shader.setFloatUniform("uTime", timeSec)
                shader.setFloatUniform("uMix", mix.value)
                shader.setFloatUniform("uIntensity", 0.9f)  // bass swell optional; constant is fine
                shader.setFloatUniform("uSat", 1.5f)
                shader.setFloatUniform("uDither", 0.02f)
                shader.setFloatUniform("uScale", 1.3f)
                drawRect(ShaderBrush(shader))
            },
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Bitmap.toClampShader() =
    BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        .apply { setFilterMode(BitmapShader.FILTER_MODE_LINEAR) }

private suspend fun loadBlurredCover(context: Context, url: String): Bitmap? {
    val loader = SingletonImageLoader.get(context)
    val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(288).build()
    val img = (runCatching { loader.execute(request) }.getOrNull() as? SuccessResult)?.image
    val src = (img as? BitmapImage)?.bitmap ?: return null
    // Desktop parity: draw the cover oversized (-16..176 = 192px) onto a 160px buffer, then blur.
    val out = Bitmap.createBitmap(COVER_PX, COVER_PX, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(
        src, null,
        Rect(-OVERSCAN, -OVERSCAN, COVER_PX + OVERSCAN, COVER_PX + OVERSCAN),
        Paint(Paint.FILTER_BITMAP_FLAG),
    )
    boxBlur(out, radius = 8, passes = 3)   // 3 box passes ≈ a 9px Gaussian (CLT)
    return out
}

// Separable box blur, repeated 3× (≈ Gaussian). Runs once per cover on a 160px buffer, so cost is
// trivial. ponytail: avoids a RenderScript/Toolkit dependency; swap in Toolkit.blur if exactness
// past visual parity is ever needed.
private fun boxBlur(bmp: Bitmap, radius: Int, passes: Int) {
    val w = bmp.width; val h = bmp.height
    val a = IntArray(w * h); bmp.getPixels(a, 0, w, 0, 0, w, h)
    val b = IntArray(w * h)
    repeat(passes) {
        blurTranspose(a, b, w, h, radius)   // rows of (w×h) → cols of b (h×w)
        blurTranspose(b, a, h, w, radius)   // rows of (h×w) → back to a (w×h)
    }
    bmp.setPixels(a, 0, w, 0, 0, w, h)
}

/** Blur each row of [src] (w×h) horizontally with a clamped moving average, writing transposed to [dst] (h×w). */
private fun blurTranspose(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
    val win = 2 * r + 1
    for (y in 0 until h) {
        val row = y * w
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        for (k in -r..r) {
            val p = src[row + k.coerceIn(0, w - 1)]
            sa += (p ushr 24) and 0xff; sr += (p ushr 16) and 0xff; sg += (p ushr 8) and 0xff; sb += p and 0xff
        }
        for (x in 0 until w) {
            dst[x * h + y] = ((sa / win) shl 24) or ((sr / win) shl 16) or ((sg / win) shl 8) or (sb / win)
            val pout = src[row + (x - r).coerceIn(0, w - 1)]
            val pin = src[row + (x + r + 1).coerceIn(0, w - 1)]
            sa += ((pin ushr 24) and 0xff) - ((pout ushr 24) and 0xff)
            sr += ((pin ushr 16) and 0xff) - ((pout ushr 16) and 0xff)
            sg += ((pin ushr 8) and 0xff) - ((pout ushr 8) and 0xff)
            sb += (pin and 0xff) - (pout and 0xff)
        }
    }
}
