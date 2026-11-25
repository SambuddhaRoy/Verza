package com.verza.ui.nowplaying

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.verza.playback.Spectrum
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The cover, blurred and slowly warped with simplex noise, stirred by the bass: the desktop's Now
 * Playing background (renderer.js, drawFluid), the same shader in AGSL. Dark, it's dimmed so white
 * type reads over a white cover; light, it's washed pale for black type. How far follows the cover's
 * brightness. Before Android 13 there are no runtime shaders, and the still blur stands in.
 */
@Composable
fun FlowingCover(art: String?, dark: Boolean, modifier: Modifier = Modifier) {
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
    Flowing(bitmap!!, dark, modifier)
}

@RequiresApi(33)
@Composable
private fun Flowing(bitmap: Bitmap, dark: Boolean, modifier: Modifier) {
    val shader = remember { RuntimeShader(FLUID) }
    val lum = remember(bitmap) { brightness(bitmap) }
    remember(bitmap) {
        shader.setInputShader("cover", BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { filterMode = BitmapShader.FILTER_MODE_LINEAR })
        shader.setFloatUniform("img", bitmap.width.toFloat(), bitmap.height.toFloat())
    }
    var time by remember { mutableFloatStateOf(0f) }
    var bass by remember { mutableFloatStateOf(0f) }
    // Thirty frames a second while it's on screen; the composition leaving stops it.
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        var last = 0L
        while (true) withFrameNanos { t ->
            if (t - last >= 33_000_000L) { last = t; time = (t - start) / 1e9f; bass = bass * 0.6f + Spectrum.bass * 0.4f }
        }
    }
    val brush = remember(shader) { object : ShaderBrush() { override fun createShader(size: Size) = shader } }
    Box(modifier.fillMaxSize().blur(24.dp).drawBehind {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("time", time * 1.25f)
        shader.setFloatUniform("intensity", 0.9f + bass * 0.9f)
        shader.setFloatUniform("dim", if (dark) 0.62f - 0.28f * lum else 1f)
        shader.setFloatUniform("veil", if (dark) 0f else 0.7f + 0.2f * (1f - lum))   // light: washed pale, not grey (a dark cover read murky at 0.72)
        drawRect(brush)
    })
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

/**
 * The music around the cover: bars stand out from its four sides, low notes at the top centre to
 * high ones at the bottom, mirrored, jumping up and falling back slowly. [cover] is the cover's side,
 * centred in this box. Bars only while it's playing; [onBass] gets how hard the bass hits, for the
 * cover to swell with.
 */
@Composable
fun CoverVisualizer(cover: Dp, color: Color, playing: Boolean, onBass: (Float) -> Unit, modifier: Modifier = Modifier) {
    DisposableEffect(Unit) { Spectrum.watch(); onDispose { Spectrum.unwatch() } }
    val smooth = remember { FloatArray(Spectrum.BANDS) }
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playing) {
        var last = 0L
        while (true) withFrameNanos { t ->
            if (t - last >= 33_000_000L) {
                last = t
                for (i in smooth.indices) {
                    val v = if (playing) Spectrum.bands[i].let { it.pow(1.5f) * (1f + i / smooth.size.toFloat() * 0.5f) }.coerceAtMost(1f) else 0f
                    smooth[i] = if (v > smooth[i]) smooth[i] * 0.25f + v * 0.75f else smooth[i] * 0.86f + v * 0.14f
                }
                onBass(if (playing) Spectrum.bass else 0f)
                tick = t
            }
        }
    }
    val density = LocalDensity.current
    Canvas(modifier.fillMaxSize()) {
        tick   // redraw each tick
        val side = with(density) { cover.toPx() }
        val cx = size.width / 2; val cy = size.height / 2
        val half = side / 2
        val gap = 6.dp.toPx()
        val room = min(cx, cy) - half - gap
        val maxLen = max(4.dp.toPx(), min(room, side * 0.16f))
        val inset = 12.dp.toPx()                 // the corner's rounding: bars stand on the straight part
        val edge = side - inset * 2
        val spacing = 7.dp.toPx()
        val perSide = max(6, (edge / spacing).toInt())
        val total = perSide * 4
        val width = spacing * 0.55f
        // Clockwise from the top centre: top right half, right, bottom, left, top left half.
        fun bar(k: Int, x: Float, y: Float, nx: Float, ny: Float) {
            val pos = k.toFloat() / total
            val mirrored = if (pos < 0.5f) pos * 2 else (1 - pos) * 2
            val v = smooth[min(smooth.size - 1, (mirrored * (smooth.size - 1)).toInt())]
            if (v < 0.02f) return   // a silent bar is nothing: paused, a dot per bar read as a dotted frame
            val len = 3.dp.toPx() + v * maxLen
            val sx = x + nx * gap; val sy = y + ny * gap
            drawLine(color.copy(alpha = 0.12f), Offset(sx, sy), Offset(sx + nx * len, sy + ny * len), width * 3, StrokeCap.Round)
            drawLine(color.copy(alpha = 0.4f + v * 0.6f), Offset(sx, sy), Offset(sx + nx * len, sy + ny * len), width, StrokeCap.Round)
        }
        var k = 0
        val step = edge / perSide
        for (i in 0 until perSide / 2) bar(k++, cx + step * (i + 0.5f), cy - half, 0f, -1f)
        for (i in 0 until perSide) bar(k++, cx + half, cy - half + inset + step * (i + 0.5f), 1f, 0f)
        for (i in 0 until perSide) bar(k++, cx + half - inset - step * (i + 0.5f), cy + half, 0f, 1f)
        for (i in 0 until perSide) bar(k++, cx - half, cy + half - inset - step * (i + 0.5f), -1f, 0f)
        for (i in 0 until perSide - perSide / 2) bar(k++, cx - half + inset + step * (i + 0.5f), cy - half, 0f, -1f)
    }
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
