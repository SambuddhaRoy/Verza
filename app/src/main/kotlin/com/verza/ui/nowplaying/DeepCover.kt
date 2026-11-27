package com.verza.ui.nowplaying

import android.content.Context
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.verza.data.deep.Deep
import com.verza.data.deep.Scene
import kotlin.math.abs
import kotlin.math.sqrt

/** Where Now Playing's cover is on the page and how much it's scaled: the 3D scene lines up with it. */
class CoverSpot {
    var rect by mutableStateOf(Rect.Zero)
    var scale: () -> Float = { 1f }
}

/**
 * A 3D cover (data/deep/Deep.kt), drawn over Now Playing's ground as if through a window in its fluted
 * glass: the cover where [spot] says it is, its near things moving with the phone's tilt over what's
 * behind them, and its colours flowing out around it, out of focus. Past the window's edge the ground's
 * flutes come back over that blur, and further out the blur gives way to the ground itself. Away from
 * the cover it's dimmed (dark) or washed (light) as the ground is, so the type over it reads; the cover
 * keeps its colour.
 */
@RequiresApi(33)
@Composable
fun DeepCover(scene: Scene, spot: CoverSpot, dark: Boolean, modifier: Modifier = Modifier) {
    val tilt = rememberTilt()
    val shader = remember { RuntimeShader(DEEP) }
    val density = LocalDensity.current.density
    remember(scene, dark, density) {
        fun bitmap(b: android.graphics.Bitmap) = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { filterMode = BitmapShader.FILTER_MODE_LINEAR }
        shader.setInputShader("image", bitmap(scene.image))
        // Data, not colour: sampled raw, so a wide-gamut screen's colour management can't bend the depths.
        shader.setInputBuffer("layers", bitmap(scene.layers))
        shader.setInputShader("plate", bitmap(scene.plate))
        shader.setInputShader("ext", bitmap(scene.ext))
        shader.setInputShader("extFar", bitmap(scene.extFar))
        shader.setFloatUniform("imageSize", scene.image.width.toFloat(), scene.image.height.toFloat())
        shader.setFloatUniform("layerSize", Deep.S.toFloat(), Deep.S.toFloat())
        shader.setFloatUniform("extSize", scene.ext.width.toFloat(), scene.ext.height.toFloat())
        shader.setFloatUniform("extBox", Deep.EXT_X, Deep.EXT_Y, Deep.EXT_W, Deep.EXT_H)
        shader.setFloatUniform("focus", scene.focus)
        shader.setFloatUniform("band", 0.07f)
        shader.setFloatUniform("dim", if (dark) (0.62f - 0.28f * scene.lum) * 0.85f else 0.92f)
        shader.setFloatUniform("veil", if (dark) 0f else 0.42f + 0.16f * (1f - scene.lum))
        shader.setFloatUniform("flute", GroundFlutes.RIB * density, GroundFlutes.BEND, GroundFlutes.SHADE)
        shader.setFloatUniform("hole", HOLE.start, HOLE.endInclusive)
        shader.setFloatUniform("fade", FADE.start, FADE.endInclusive)
    }
    val brush = remember(shader) { object : ShaderBrush() { override fun createShader(size: Size) = shader } }
    var origin by remember { mutableStateOf(Offset.Zero) }
    // Its own layer, kept between frames: the ground under it moves thirty times a second, and this is
    // redrawn only when the phone turns or the cover moves.
    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawBehind {
            val r = spot.rect
            if (r.width <= 0f) return@drawBehind
            val side = r.width * spot.scale()
            val at = Offset(r.center.x - side / 2 - origin.x, r.center.y - side / 2 - origin.y)
            shader.setFloatUniform("cover", at.x, at.y, side)
            val t = tilt.value
            shader.setFloatUniform("tilt", t.x * REACH, t.y * REACH)
            // Only as far out as the blur reaches; the ground is everything past it.
            val out = side * FADE.endInclusive
            drawRect(brush, at - Offset(out, out), Size(side + 2 * out, side + 2 * out))
        })
}

/** How far the eye moves at full tilt, in cover sides per unit of depth: the far wall slides about 3%. */
private const val REACH = 0.035f
/** Where the fluted glass comes back over the blur, in cover sides out from the cover: the window's soft edge. */
private val HOLE = 0.02f..0.14f
/** Where the blur gives way to the ground under the glass, in cover sides out from the cover. */
private val FADE = 0.08f..0.5f
/** The turn of the phone that's full tilt, in radians (10 degrees). */
private const val FULL = 0.1745f

/**
 * How the phone is turned from the way it's being held, -1 to 1 each way: x as its right edge goes
 * away, y as its top comes toward you. The way it's held is learned, and follows over a couple of
 * seconds, so a new grip settles to still. Off with animations off (Remove animations), and only read
 * while the app is in front.
 */
@Composable
fun rememberTilt(): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    val still = remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    LifecycleResumeEffect(still) {
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = if (still) null else sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: sensors.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val listener = sensor?.let { Follower(it.type, tilt) }
        if (listener != null) sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose { listener?.let(sensors::unregisterListener); tilt.value = Offset.Zero }
    }
    return tilt
}

/** Turns sensor readings into a tilt from a held pose that slowly follows the phone. */
private class Follower(private val type: Int, private val out: MutableState<Offset>) : SensorEventListener {
    private val held = FloatArray(4)          // the held pose: a quaternion (w, x, y, z), or gravity (x, y, z)
    private var started = false
    private val q = FloatArray(4)
    private val v = FloatArray(4)
    private val now = FloatArray(9)
    private val then = FloatArray(9)

    override fun onSensorChanged(e: SensorEvent) {
        val x: Float; val y: Float
        if (type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            SensorManager.getQuaternionFromVector(q, e.values)
            if (!started) { q.copyInto(held); started = true }
            if (dot(q, held) < 0f) for (i in 0..3) q[i] = -q[i]
            // The held pose eases toward this one: about two seconds to settle at fifty readings a second.
            for (i in 0..3) held[i] += (q[i] - held[i]) * 0.01f
            normalise(held)
            matrix(q, now); matrix(held, then)
            // The turn from the held pose, in the phone's own axes (then' * now, small angles): about its
            // y axis as the right edge goes away, about its x axis as the top comes toward you.
            fun rel(i: Int, j: Int) = then[i] * now[j] + then[3 + i] * now[3 + j] + then[6 + i] * now[6 + j]
            x = (rel(0, 2) - rel(2, 0)) / 2f
            y = (rel(2, 1) - rel(1, 2)) / 2f
        } else {
            val g = e.values
            if (!started) { g.copyInto(held, 0, 0, 3); started = true }
            for (i in 0..2) held[i] += (g[i] - held[i]) * 0.01f
            x = (kotlin.math.atan2(g[0], g[2]) - kotlin.math.atan2(held[0], held[2]))
            y = (kotlin.math.atan2(g[1], g[2]) - kotlin.math.atan2(held[1], held[2]))
        }
        val next = Offset((x / FULL).coerceIn(-1f, 1f), (y / FULL).coerceIn(-1f, 1f))
        // A hand is never quite still: small changes are let go, so a phone at rest doesn't redraw.
        if (abs(next.x - out.value.x) > 0.004f || abs(next.y - out.value.y) > 0.004f) out.value = next
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** The rotation matrix (row-major, phone to world) of quaternion [a] (w, x, y, z). */
    private fun matrix(a: FloatArray, into: FloatArray) {
        v[0] = a[1]; v[1] = a[2]; v[2] = a[3]; v[3] = a[0]
        SensorManager.getRotationMatrixFromVector(into, v)
    }

    private fun dot(a: FloatArray, b: FloatArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3]
    private fun normalise(a: FloatArray) { val n = sqrt(dot(a, a)); if (n > 0f) for (i in 0..3) a[i] /= n }
}

/*
 * The scene, per pixel. u is where the pixel is in cover sides (0 to 1 across the cover). The eye moved
 * by `tilt`: a point at depth e shows up shifted by tilt * (e - focus), so the subject (at focus) holds
 * still and what's behind it slides. Near things: march from nearest to farthest along where they could
 * have come from, and the first surface met is the one seen (parallax occlusion mapping). What's behind
 * them is smooth, so following its depth twice finds where it's seen; where the near layer used to be,
 * the plate stands in. Out from the cover, the colours flowing out of it, which move as the farthest
 * thing does, going out of focus with distance; and the cover's own edge fades into them.
 *
 * Past the window's edge (`hole`) the ground's flutes (Flow.kt's, the same width in the same places) come
 * back over that blur, each showing a mirrored, squeezed slice of what's behind it, shaded into its seams;
 * and from `fade.x` to `fade.y` the blur thins out, premultiplied, onto the ground under it. The flutes
 * line up, so where one gives way to the other they run on unbroken: the blur is behind the same glass.
 */
private const val DEEP = """
uniform shader image;
uniform shader layers;
uniform shader plate;
uniform shader ext;
uniform shader extFar;
uniform float2 imageSize;
uniform float2 layerSize;
uniform float2 extSize;
uniform float4 extBox;
uniform float3 cover;
uniform float2 tilt;
uniform float focus;
uniform float band;
uniform float dim;
uniform float veil;
uniform float3 flute;
uniform float2 hole;
uniform float2 fade;

float4 at(float2 u) { return float4(layers.eval(clamp(u, 0.0, 1.0) * layerSize)); }

// How far outside the cover (negative inside), as a true distance: rounded past its corners, where the
// larger of the two axes' distances drew a picture frame's mitred corners into the glow.
float outside(float2 u) {
  float2 o = max(-u, u - 1.0);
  return length(max(o, 0.0)) + min(max(o.x, o.y), 0.0);
}

// The cover's colours flowing out of it at u, out of focus and dimmed (or washed) with distance as the
// ground is.
float3 blurAt(float2 u) {
  float d = outside(u);
  float2 e = (u + tilt * focus - extBox.xy) / extBox.zw;
  float3 c = mix(float3(ext.eval(e * extSize).rgb), float3(extFar.eval(e * extSize).rgb), smoothstep(0.0, 0.45, d));
  return mix(c, mix(c * dim, float3(1.0), veil), smoothstep(0.0, 0.3, d));
}

half4 main(float2 p) {
  float2 u = (p - cover.xy) / cover.z;
  float d = outside(u);
  float shown = 1.0 - smoothstep(fade.x, fade.y, d);
  if (shown <= 0.0) return half4(0.0);
  float3 col = blurAt(u);
  float glass = smoothstep(hole.x, hole.y, d);
  if (glass > 0.0) {
    float k = p.x / flute.x;
    float f = fract(k);
    float x = (floor(k) + 0.5 + (f - 0.5) * flute.y) * flute.x;
    float3 c = blurAt(float2((x - cover.x) / cover.z, u.y));
    float curve = sin(f * 3.14159265);
    float lit = smoothstep(0.12, 0.3, f) * (1.0 - smoothstep(0.3, 0.52, f));
    col = mix(col, c * (1.0 - flute.z * (1.0 - curve) + flute.z * 0.9 * lit), glass);
  }
  if (d < 0.0) {
    float hit = 0.0;
    float prevL = 1.0;
    float prevGap = -1.0;
    float found = 0.0;
    for (int i = 0; i <= 14; i++) {
      float l = 1.0 - float(i) / 14.0;
      float gap = at(u - tilt * (l - focus)).r - l;
      if (found < 0.5 && gap >= 0.0) {
        float t = i == 0 ? 0.0 : prevGap / (prevGap - gap);
        hit = mix(prevL, l, clamp(t, 0.0, 1.0));
        found = 1.0;
      }
      prevGap = gap;
      prevL = l;
    }
    float2 s = clamp(u - tilt * (hit - focus), 0.0, 1.0);
    float near = at(s).g;
    float3 front = float3(image.eval(s * imageSize).rgb);
    float2 b = u;
    for (int k = 0; k < 2; k++) { b = u - tilt * (at(b).b - focus); }
    b = clamp(b, 0.0, 1.0);
    float3 back = mix(float3(image.eval(b * imageSize).rgb), float3(plate.eval(b * layerSize).rgb), at(b).g);
    col = mix(col, mix(back, front, near), smoothstep(0.0, band, -d));
  }
  return half4(half3(clamp(col, 0.0, 1.0) * shown), half(shown));
}
"""
