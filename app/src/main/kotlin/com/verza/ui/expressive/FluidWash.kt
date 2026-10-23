package com.verza.ui.expressive

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.verza.ui.theme.LocalAudioSignal
import kotlin.math.exp

/*
 * The desktop's flowing cover (renderer.js, COVER_FRAG) behind Glass's Now Playing: the cover,
 * blurred once, then domain-warped by two octaves of simplex noise every frame, so it moves like
 * liquid rather than turning like a picture. Its uniforms are the desktop's; what is added is the
 * bass, which swells the warp, and the kick, which pumps it.
 */

// The texture is blurred as heavily as the Glass wash it replaces (a 72dp blur across the screen):
// a sigma of about 8% of the texture, which the 1.3x zoom stretches to ~10% of the screen's width.
private const val TEX_PX = 256
private const val OVERSCAN = 26        // drawn oversized, so the blur never pulls in a hard edge
private const val BLUR_RADIUS = 20     // 3 box passes of this ~ a Gaussian with sigma 20px

private const val FLUID_SRC = """
uniform shader uTexA;
uniform shader uTexB;
uniform float uMix;
uniform float uTime;
uniform float uIntensity;
uniform float uScale;
uniform float uGain;
uniform float uTexSize;
uniform float2 uRes;

float3 mod289(float3 x){ return x - floor(x * (1.0/289.0)) * 289.0; }
float2 mod289(float2 x){ return x - floor(x * (1.0/289.0)) * 289.0; }
float3 permute(float3 x){ return mod289(((x*34.0)+1.0)*x); }
float snoise(float2 v){
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

// The desktop's lattice-free hash: the sin() one lays a visible grid at these scales.
float hash21(float2 p){ p = fract(p * float2(443.897, 441.423)); p += dot(p, p + 19.19); return fract((p.x + p.y) * p.x); }

half4 main(float2 fragCoord){
  float2 uv = fragCoord / uRes;
  float2 z = (uv - 0.5) / uScale + 0.5;
  float t = uTime * 0.05;
  float cw = 1.0 - smoothstep(0.0, 0.7, length(z - 0.5));
  float n1 = snoise(z * 0.35 + float2(t, t * 0.7));
  float n2 = snoise(z * 0.35 + float2(-t * 0.8, t * 0.5) + float2(50.0, 50.0));
  float n3 = snoise(z * 0.9  + float2(t * 1.2, -t)       + float2(100.0, 0.0));
  float n4 = snoise(z * 0.9  + float2(-t, t * 1.1)        + float2(0.0, 100.0));
  float2 warp = float2(n1 * 0.65 + n3 * 0.35, n2 * 0.65 + n4 * 0.35) * cw;
  float2 px = clamp(z + warp * uIntensity, 0.0, 1.0) * uTexSize;
  float3 col = mix(float3(uTexA.eval(px).rgb), float3(uTexB.eval(px).rgb), uMix);
  float2 cen = uv - 0.5;
  col *= 1.0 - dot(cen, cen) * 0.5;
  float gray = dot(col, float3(0.299, 0.587, 0.114));
  col = mix(float3(gray), col, 1.5) * uGain;
  col += (hash21(floor(fragCoord) + fract(uTime * 0.37) * 311.0) - 0.5) * 0.02;
  return half4(half3(col), 1.0);
}
"""

/**
 * The bass as the wash feels it: [level] follows the low end, [kick] jumps to 1 on a beat and
 * dies away over a fifth of a second.
 *
 * The beat is the classic energy test: the power under 150Hz against its own average over the
 * last second. It needs the linear power ([VisualizerSignal.kick]); the log-scaled bass figure
 * barely moves (0.74 to 0.81 through a song with a steady kick), while the power runs from 0.001
 * between kicks to 0.4 on one. A ratio makes a quiet song as lively as a loud one.
 * Pure, so it is tested on the JVM ([FluidWashTest]).
 */
class BeatPulse {
    var level = 0f; private set
    var kick = 0f; private set
    private var average = 0f
    private var sinceBeat = 1f
    private var armed = true

    /**
     * Advance by [dt] seconds with the visualizer's smoothed [bass] and the raw [power] under
     * 150Hz (both 0..1, 0 when nothing plays).
     */
    fun step(bass: Float, power: Float, dt: Float) {
        level += (bass - level) * (1f - exp(-dt / 0.08f))
        kick *= exp(-dt / 0.2f)
        sinceBeat += dt
        // One beat per burst: a kick's power wobbles while it rings, so after firing it waits for
        // the power to fall back under the average before it may fire again. And 0.28s apart at
        // the least, 210bpm, past any real kick drum.
        if (power < average) armed = true
        if (armed && sinceBeat > 0.28f && power > 0.015f && power > average * 2f + 0.005f) {
            kick = 1f
            sinceBeat = 0f
            armed = false
        }
        average += (power - average) * (1f - exp(-dt / 1f))
    }
}

/**
 * The flowing wash, or nothing if this device's GPU will not compile the shader (the caller then
 * draws the still one). [motion] off stops it dead: no flow and no pulse, one frame drawn.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun FluidWash(artworkUrl: String?, motion: Boolean, modifier: Modifier): Boolean {
    val shader = remember { runCatching { RuntimeShader(FLUID_SRC) }.getOrNull() } ?: return false
    val context = LocalContext.current
    var texA by remember { mutableStateOf<BitmapShader?>(null) }
    var texB by remember { mutableStateOf<BitmapShader?>(null) }
    val mix = remember { Animatable(0f) }

    LaunchedEffect(artworkUrl) {
        val next = artworkUrl?.let { blurredCover(context, it) }?.toClampShader() ?: return@LaunchedEffect
        // A change that landed mid-fade: settle on whichever cover was further in, not the old one.
        if (mix.value > 0.5f) texA = texB
        if (texA == null) {
            texA = next; texB = next
        } else {
            texB = next
            mix.snapTo(0f)
            mix.animateTo(1f, tween(700, easing = LinearEasing))   // the desktop's crossfade
            texA = next
        }
        mix.snapTo(0f)
    }

    val signal = LocalAudioSignal.current
    var time by remember { mutableFloatStateOf(0f) }
    var intensity by remember { mutableFloatStateOf(0.9f) }
    var scale by remember { mutableFloatStateOf(1.3f) }
    var gain by remember { mutableFloatStateOf(0.82f) }
    if (motion) {
        LaunchedEffect(signal) {
            val pulse = BeatPulse()
            var prev = 0L
            var acc = 0f
            while (true) {
                withFrameNanos { now ->
                    if (prev != 0L) acc += (now - prev) / 1e9f
                    prev = now
                    // ~30fps, as on the desktop: every frame of this redraws each glass pane over it.
                    if (acc >= 0.03f) {
                        val dt = acc.coerceAtMost(0.1f)
                        acc = 0f
                        val now = signal?.value
                        pulse.step(now?.bass ?: 0f, now?.kick ?: 0f, dt)
                        time += dt
                        intensity = 0.9f + pulse.level * 0.5f + pulse.kick * 0.35f
                        scale = 1.3f + pulse.kick * 0.07f
                        gain = 0.82f * (1f + pulse.level * 0.10f + pulse.kick * 0.14f)
                    }
                }
            }
        }
    }

    val a = texA ?: return true
    val b = texB ?: return true
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        // Drawn at half the screen's size and stretched: the field is all soft gradients, so the
        // stretch cannot be seen, and it is a quarter of the pixels for the noise to run over.
        Box(
            Modifier
                .requiredSize(maxWidth / 2, maxHeight / 2)
                .graphicsLayer {
                    scaleX = 2f
                    scaleY = 2f
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawBehind {
                    shader.setInputShader("uTexA", a)
                    shader.setInputShader("uTexB", b)
                    shader.setFloatUniform("uRes", size.width, size.height)
                    shader.setFloatUniform("uTexSize", TEX_PX.toFloat())
                    shader.setFloatUniform("uTime", time)
                    shader.setFloatUniform("uMix", mix.value)
                    shader.setFloatUniform("uIntensity", intensity)
                    shader.setFloatUniform("uScale", scale)
                    shader.setFloatUniform("uGain", gain)
                    drawRect(ShaderBrush(shader))
                },
        )
    }
    return true
}

private fun Bitmap.toClampShader() =
    BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) filterMode = BitmapShader.FILTER_MODE_LINEAR
    }

private suspend fun blurredCover(context: Context, url: String): Bitmap? {
    val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(TEX_PX).build()
    val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()
    val src = ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap ?: return null
    val out = Bitmap.createBitmap(TEX_PX, TEX_PX, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(
        src, null,
        Rect(-OVERSCAN, -OVERSCAN, TEX_PX + OVERSCAN, TEX_PX + OVERSCAN),
        Paint(Paint.FILTER_BITMAP_FLAG),
    )
    boxBlur(out, BLUR_RADIUS, passes = 3)
    return out
}

// Once per cover on a 256px buffer, so its cost does not matter.
// ponytail: a box blur three times over instead of a blur library; it is a Gaussian to the eye.
private fun boxBlur(bmp: Bitmap, radius: Int, passes: Int) {
    val w = bmp.width; val h = bmp.height
    val a = IntArray(w * h); bmp.getPixels(a, 0, w, 0, 0, w, h)
    val b = IntArray(w * h)
    repeat(passes) {
        blurTranspose(a, b, w, h, radius)
        blurTranspose(b, a, h, w, radius)
    }
    bmp.setPixels(a, 0, w, 0, 0, w, h)
}

/** Blur each row of [src] (w×h) with a clamped moving average, writing it transposed into [dst] (h×w). */
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
