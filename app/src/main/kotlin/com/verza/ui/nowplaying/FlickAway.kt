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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.verza.data.Connect
import com.verza.data.shownTitle
import com.verza.graph
import com.verza.playback.Flick
import com.verza.ui.theme.Corner
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Flicks, both ways, while the app is on screen. Away from you (toward the computer) sends what plays
 * here to the first paired computer in reach; back toward you takes what a computer plays (one that's
 * playing before one that's paused) and stops it there. What arrives wins over what was playing, or,
 * with "ask" on in Settings, the listener picks which to keep and the other stops.
 *
 * [fly] runs 0 (the cover in place) to 1 (gone off the top): out for a send, in from there for a take.
 * [label] says where it went or where it came from, under the cover.
 */
class FlickState {
    val fly = Animatable(0f)
    var label by mutableStateOf<String?>(null)
    /** A take waiting on the listener's choice. */
    var asking by mutableStateOf<Connect.Computer?>(null)
    /** Where a flick away would go now, and sending there by hand (Now Playing's line under the controls). */
    var sendTo by mutableStateOf<Connect.Computer?>(null)
    var send: () -> Unit = {}
}

/** Listens for flicks, and does what they ask; [openPlayer] brings Now Playing up for the animation. */
@Composable
fun FlickHandoff(playerOpen: Boolean, openPlayer: () -> Unit): FlickState {
    val context = LocalContext.current
    val graph = context.graph
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val state = remember { FlickState() }
    val on by graph.settings.flick.flow.collectAsState(initial = true)
    val ask by graph.settings.flickAsk.flow.collectAsState(initial = false)
    val paired by graph.connect.paired.flow.collectAsState(initial = emptyList())
    val playing by graph.connect.playing.collectAsState()
    val now by graph.player.state.collectAsState()
    val inReach = if (on) paired.filter { it.id in playing.keys } else emptyList()
    val sendTo = inReach.firstOrNull()?.takeIf { now.playing }
    val takeFrom = inReach.filter { playing[it.id]?.track != null }.maxByOrNull { if (playing[it.id]?.playing == true) 1 else 0 }
    val busy = { state.fly.isRunning || state.label != null || state.asking != null }

    val send = { computer: Connect.Computer ->
        if (!busy()) scope.launch {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val wasOpen = playerOpen
            if (!graph.connect.playThere(computer, "flick")) return@launch
            openPlayer()
            if (!wasOpen) delay(320)   // Now Playing rising
            state.label = "playing on\n${computer.name}"
            state.fly.animateTo(1f, tween(520, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)))
            delay(1800)
            state.label = null
            state.fly.snapTo(0f)
        }
        Unit
    }
    val take = { computer: Connect.Computer ->
        scope.launch {
            if (!graph.connect.playHere(computer, "flick")) return@launch
            // The cover comes down from the top onto "from <computer>", where it was sent off from.
            state.fly.snapTo(1f)
            state.label = "from\n${computer.name}"
            openPlayer()
            delay(520)
            state.fly.animateTo(0f, tween(640, easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)))
            delay(200)
            state.label = null
        }
        Unit
    }
    state.sendTo = sendTo
    state.send = { sendTo?.let(send) }

    ListenForFlick(sendTo != null || takeFrom != null) { way ->
        if (busy()) return@ListenForFlick
        when (way) {
            Flick.AWAY -> sendTo?.let(send)
            Flick.TOWARD -> takeFrom?.let { c ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (ask && now.playing) state.asking = c else take(c)
            }
        }
    }

    state.asking?.let { c ->
        val here = now.song
        val there = playing[c.id]?.track
        val p = LocalPalette.current
        AlertDialog(
            onDismissRequest = { state.asking = null },
            title = { Text("Keep which?") },
            text = {
                Text(
                    "“${here?.let { shownTitle(it.title, it.artist) } ?: "this"}” playing here, or “${there?.let { shownTitle(it.title, it.artist) } ?: "that"}” from ${c.name}. The other one stops.",
                    color = p.inkMuted,
                )
            },
            // Lowercase words, as every word you press is; a name keeps its own.
            confirmButton = { TextButton(onClick = { state.asking = null; take(c) }) { Text("from ${c.name}") } },
            dismissButton = { TextButton(onClick = { state.asking = null; graph.connect.tell(c, "pause") }) { Text("keep this one") } },
        )
    }
    return state
}

/** Calls [onFlick] with [Flick.AWAY] or [Flick.TOWARD] for each flick while [active]. */
@Composable
private fun ListenForFlick(active: Boolean, onFlick: (Int) -> Unit) {
    if (!active) return
    val context = LocalContext.current
    val fire by rememberUpdatedState(onFlick)
    DisposableEffect(Unit) {
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val flick = Flick()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { flick.feed(e.values[0], e.values[1], e.values[2], e.timestamp).let { if (it != 0) fire(it) } }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (accel != null) sensors.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensors.unregisterListener(listener) }
    }
}

/**
 * The cover, and what a flick does to it: it tilts away and flies off the top of the screen, a trail of
 * itself behind it and a ring of the accent left where it was ([fly] 0 to 1), or comes back down the
 * same way (1 to 0). [label] sits under it, seen while the cover is away.
 */
@Composable
fun FlyingCover(art: String?, side: Dp, swell: Float, fly: Float, label: String?, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(modifier, contentAlignment = Alignment.Center) {
        if (label != null) Text(label, style = Type.Section, color = p.ink, textAlign = TextAlign.Center)
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
            if (k == 0 && fly >= 1f) continue
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
    }
}
