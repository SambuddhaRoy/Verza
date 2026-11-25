package com.verza.ui.look

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.LocalContext
import com.verza.graph
import com.verza.playback.Spectrum
import kotlinx.coroutines.delay

/**
 * Seconds since this first composed, ticking [fps] times a second, and not at all while no frames
 * are drawn (the app in the background). For things that move slowly: one redrawn at the display's
 * rate behind the glass makes every pane over it redraw as often.
 */
@Composable
fun slowTime(fps: Int = 20): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fps) {
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { t.floatValue = (it - start) / 1000f }
            delay(1000L / fps)
        }
    }
    return t
}

/**
 * The music's low end, 0 to 1, eased so it swells and falls rather than flickers; 0 while nothing
 * plays. Watches the spectrum while it's on screen.
 */
@Composable
fun rememberLevel(fps: Int = 30): State<Float> {
    val graph = LocalContext.current.graph
    val now by graph.player.state.collectAsState()
    val level = remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) { Spectrum.watch(); onDispose { Spectrum.unwatch() } }
    LaunchedEffect(now.playing) {
        while (true) {
            val target = if (now.playing) Spectrum.bass else 0f
            val v = level.floatValue
            level.floatValue = v + (target - v) * (if (target > v) 0.45f else 0.12f)
            if (!now.playing && level.floatValue < 0.01f) { level.floatValue = 0f; break }
            delay(1000L / fps)
        }
    }
    return level
}
