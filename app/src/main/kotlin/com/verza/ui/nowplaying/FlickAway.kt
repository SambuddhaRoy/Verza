package com.verza.ui.nowplaying

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.verza.data.Connect
import com.verza.graph
import com.verza.playback.Flick
import com.verza.ui.theme.Corner
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The computer a flick sends to: the first paired one in reach, with flicking on. Null with none.
 */
@Composable
fun flickTarget(): Connect.Computer? {
    val graph = LocalContext.current.graph
    val on by graph.settings.flick.flow.collectAsState(initial = true)
    val paired by graph.connect.paired.flow.collectAsState(initial = emptyList())
    val playing by graph.connect.playing.collectAsState()
    return if (on) paired.firstOrNull { it.id in playing.keys } else null
}

/** Calls [onFlick] for each flick of the phone while this is on screen and [target] isn't null. */
@Composable
fun ListenForFlick(target: Connect.Computer?, onFlick: () -> Unit) {
    if (target == null) return
    val context = LocalContext.current
    val fire by rememberUpdatedState(onFlick)
    DisposableEffect(target.id) {
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val flick = Flick()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { if (flick.feed(e.values[0], e.values[1], e.values[2], e.timestamp)) fire() }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (accel != null) sensors.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensors.unregisterListener(listener) }
    }
}

/**
 * The cover, and what a flick does to it: it tilts away and flies off the top of the screen, a trail of
 * itself behind it and a ring of the accent left where it was, and in its place, where it went.
 * [fly] runs 0 (here) to 1 (gone).
 */
@Composable
fun FlyingCover(art: String?, side: Dp, swell: Float, fly: Float, gone: String?, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(modifier, contentAlignment = Alignment.Center) {
        if (fly > 0f) Canvas(Modifier.size(side)) {
            // The ring: from the cover's edge outward, fading as it goes.
            val r = size.minDimension / 2 * (1f + fly * 1.6f)
            drawCircle(p.accent.copy(alpha = (1f - fly) * 0.8f), r, center, style = Stroke((10f * (1f - fly) + 2f) * density))
            drawCircle(p.accent.copy(alpha = (1f - fly) * 0.35f), r * 0.7f, center, style = Stroke(4f * density))
        }
        // The trail: three copies a little behind, fainter each.
        for (k in 3 downTo 0) {
            val t = (fly - k * 0.06f).coerceIn(0f, 1f)
            if (k > 0 && (t <= 0f || fly >= 1f)) continue
            AsyncImage(art, null, contentScale = ContentScale.Crop, modifier = Modifier.size(side).graphicsLayer {
                val e = t * t                                        // slow off the mark, then gone
                translationY = -e * size.height * 2.4f
                val k2 = (1f + swell * 0.04f) * (1f - 0.5f * t)
                scaleX = k2; scaleY = k2
                rotationX = 32f * t
                cameraDistance = 14f * density
                alpha = (if (k == 0) 1f else 0.35f / k) * (1f - t * 0.6f)
            }.clip(Corner))
        }
        if (gone != null && fly >= 1f) Text("playing on\n$gone", style = Type.Section, color = p.ink, textAlign = TextAlign.Center)
    }
}

/** What a flick sets off: a tap of the hand, the cover's flight, the queue to [target], then the cover back. */
class FlickState { val fly = Animatable(0f); var gone by mutableStateOf<String?>(null) }

@Composable
fun rememberFlick(target: Connect.Computer?): Pair<FlickState, () -> Unit> {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val state = remember { FlickState() }
    val to by rememberUpdatedState(target)
    val go = {
        val computer = to
        if (computer != null && !state.fly.isRunning && state.gone == null) scope.launch {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!context.graph.connect.playThere(computer, "flick")) { return@launch }
            state.gone = computer.name
            state.fly.animateTo(1f, tween(520, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)))
            delay(1800)
            state.gone = null
            state.fly.snapTo(0f)
        }
        Unit
    }
    return state to go
}
