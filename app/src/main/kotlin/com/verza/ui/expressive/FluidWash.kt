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
import coil3.toBitmap
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.verza.ui.theme.LocalAudioSignal
import kotlin.math.exp
import kotlin.random.Random

/*
 * The desktop's flowing cover (renderer.js, COVER_FRAG) behind Glass's Now Playing: the cover,
 * blurred once, then domain-warped by two octaves of noise every frame, so it moves like
 * liquid rather than turning like a picture. Its uniforms are the desktop's; what is added is the
 * bass, which swells the warp, and the kick, which pumps it.
 */

// The texture is blurred as heavily as the Glass wash it replaces (a 72dp blur across the screen):
// a sigma of about 8% of the texture, which the 1.3x zoom stretches to ~10% of the screen's width.
private const val TEX_PX = 256
private const val OVERSCAN = 26        // drawn oversized, so the blur never pulls in a hard edge
private const val BLUR_RADIUS = 20     // 3 box passes of this ~ a Gaussian with sigma 20px

// The noise lattice: random values the noise is read from, NOISE_PX to a side and tiling, so the
// drift can be wrapped at NOISE_PX without a seam.
private const val NOISE_PX = 32

/*
 * Every number in here stays small. The first version ran simplex noise, whose hash multiplies its
 * way into the millions, plus a grain hash in the hundreds of thousands and a clock that only ever
 * grew. That needs full 32-bit floats, and on Pixels and other Mali GPUs the wash broke into blocks.
 * So the noise reads a small random texture instead, the drift is wrapped on the CPU, and every
 * texture read is at a texel's centre and blended here, which gives the same answer whatever
 * filtering the GPU applies to a child shader.
 */
private const val FLUID_SRC = """
uniform shader uTexA;
uniform shader uTexB;
uniform shader uNoise;
uniform float uMix;
uniform float uIntensity;
uniform float uScale;
uniform float uGain;
uniform float uTexSize;
uniform float2 uRes;
uniform float4 uDriftA;
uniform float4 uDriftB;
uniform float2 uGrain;

// Value noise, -1..1, smooth to the second derivative.
float vnoise(float2 p){
  float2 i = floor(p) + 0.5;
  float2 f = fract(p);
  f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
  float a = uNoise.eval(i).r;
  float b = uNoise.eval(i + float2(1.0, 0.0)).r;
  float c = uNoise.eval(i + float2(0.0, 1.0)).r;
  float d = uNoise.eval(i + float2(1.0, 1.0)).r;
  return mix(mix(a, b, f.x), mix(c, d, f.x), f.y) * 2.0 - 1.0;
}

float3 coverA(float2 i, float2 f){
  return mix(mix(float3(uTexA.eval(i).rgb), float3(uTexA.eval(i + float2(1.0, 0.0)).rgb), f.x),
             mix(float3(uTexA.eval(i + float2(0.0, 1.0)).rgb), float3(uTexA.eval(i + float2(1.0, 1.0)).rgb), f.x), f.y);
}
float3 coverB(float2 i, float2 f){
  return mix(mix(float3(uTexB.eval(i).rgb), float3(uTexB.eval(i + float2(1.0, 0.0)).rgb), f.x),
             mix(float3(uTexB.eval(i + float2(0.0, 1.0)).rgb), float3(uTexB.eval(i + float2(1.0, 1.0)).rgb), f.x), f.y);
}

half4 main(float2 fragCoord){
  float2 uv = fragCoord / uRes;
  float2 z = (uv - 0.5) / uScale + 0.5;
  float cw = 1.0 - smoothstep(0.0, 0.7, length(z - 0.5));
  float n1 = vnoise(z * 0.35 + uDriftA.xy);
  float n2 = vnoise(z * 0.35 + uDriftA.zw);
  float n3 = vnoise(z * 0.9  + uDriftB.xy);
  float n4 = vnoise(z * 0.9  + uDriftB.zw);
  float2 warp = float2(n1 * 0.65 + n3 * 0.35, n2 * 0.65 + n4 * 0.35) * cw;
  float2 q = clamp(z + warp * uIntensity, 0.0, 1.0) * uTexSize - 0.5;
  float2 i = floor(q) + 0.5;
  float2 f = q - floor(q);
  float3 col = coverA(i, f);
  if (uMix > 0.0) col = mix(col, coverB(i, f), uMix);
  float2 cen = uv - 0.5;
  col *= 1.0 - dot(cen, cen) * 0.5;
  float gray = dot(col, float3(0.299, 0.587, 0.114));
  col = mix(float3(gray), col, 1.5) * uGain;
  col += (float(uNoise.eval(floor(fragCoord) + 0.5 + uGrain).g) - 0.5) * 0.02;
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
internal fun FluidWash(artworkUrl: Any?, motion: Boolean, modifier: Modifier): Boolean {
    val shader = remember { runCatching { RuntimeShader(FLUID_SRC) }.getOrNull() } ?: return false
    val noise = remember { noiseShader() }
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

    val signal = if (com.verza.ui.theme.LocalWashBeatOn.current) LocalAudioSignal.current else null
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
                    shader.setInputShader("uNoise", noise)
                    shader.setFloatUniform("uRes", size.width, size.height)
                    shader.setFloatUniform("uTexSize", TEX_PX.toFloat())
                    // The desktop's drift, wrapped where the noise repeats. The constants keep
                    // the four fields apart, as the old +50 and +100 did.
                    val t = time * 0.05f
                    fun w(v: Float) = v.mod(NOISE_PX.toFloat())
                    shader.setFloatUniform("uDriftA", w(t), w(0.7f * t), w(11f - 0.8f * t), w(5f + 0.5f * t))
                    shader.setFloatUniform("uDriftB", w(3f + 1.2f * t), w(19f - t), w(23f - t), w(13f + 1.1f * t))
                    shader.setFloatUniform("uGrain", Random.nextInt(NOISE_PX).toFloat(), Random.nextInt(NOISE_PX).toFloat())
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

/** Random values to read the noise from; any filtering, since the shader reads texel centres. */
private fun noiseShader(): BitmapShader {
    val rnd = Random(7)
    val px = IntArray(NOISE_PX * NOISE_PX) { 0xff000000.toInt() or rnd.nextInt(0x1000000) }
    val bmp = Bitmap.createBitmap(px, NOISE_PX, NOISE_PX, Bitmap.Config.ARGB_8888)
    return BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
}

/** [url] is anything Coil loads: a cover's address, or a bundled drawable (onboarding's wash). */
private suspend fun blurredCover(context: Context, url: Any): Bitmap? {
    val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(TEX_PX).build()
    val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()
    // toBitmap, not a cast: a vector drawable comes back as a drawable, not a bitmap.
    val src = (result as? SuccessResult)?.image?.toBitmap() ?: return null
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
