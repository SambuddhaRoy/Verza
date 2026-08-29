package com.verza.ui.expressive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verza.audio.VisualizerSignal
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

/**
 * The seek bar, drawn as a live spectrum.
 *
 * The played portion is a bar visualiser fed from the FFT; the rest is a flat line, so progress
 * reads at a glance without a number.
 *
 * **The audio never causes a recomposition.** The first version collected the signal with
 * `collectAsState()` up in the player, which meant every capture — about thirty a second —
 * recomposed the whole screen: both AnimatedContents, every control, and a re-clip of the 240-point
 * cover morph path. That is what made it run at roughly one frame a second.
 *
 * Instead the flow is passed in and sampled on the frame clock, and the levels it produces are read
 * *inside the Canvas draw lambda*. Compose records that as a draw-phase dependency, so a new level
 * invalidates drawing only — composition and layout are untouched. Same trick the old shader glow
 * relied on by keeping its collect inside a tiny leaf composable, but done at the draw phase so
 * nothing recomposes at all.
 */
@Composable
fun VisualizerSeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    accent: Color,
    trackColor: Color,
    signalFlow: StateFlow<VisualizerSignal>?,
    modifier: Modifier = Modifier,
    animating: Boolean = true,
    height: Dp = 36.dp,
) {
    // While dragging, the finger owns the position — otherwise the playhead fights the position
    // updates still arriving from the player and stutters under the thumb.
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val shown = (dragProgress ?: progress).coerceIn(0f, 1f)

    // Peak-hold levels. Held in state so the draw phase can observe them, but only ever read from
    // inside the Canvas below — never from the composable body, which is what keeps this off the
    // recomposition path.
    val levels = remember { mutableStateOf(FloatArray(BAR_COUNT)) }

    LaunchedEffect(signalFlow, animating) {
        val working = FloatArray(BAR_COUNT)
        while (true) {
            withFrameNanos { }
            // Read the flow's current value rather than collecting it: we want the latest sample at
            // *our* frame rate, not a recomposition per capture.
            val bands = if (animating) signalFlow?.value?.bands.orEmpty() else emptyList()
            for (i in working.indices) {
                val target = if (bands.isEmpty()) {
                    0f
                } else {
                    bands[(i * bands.size / BAR_COUNT).coerceIn(0, bands.lastIndex)]
                }
                // Jump up instantly, fall at a fixed rate: the usual peak hold. Capture is ~30 Hz,
                // so tracking raw values on a 60 Hz clock would strobe.
                working[i] = max(target, working[i] - DECAY)
            }
            levels.value = working.copyOf()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragProgress = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragProgress?.let(onSeek); dragProgress = null },
                    onDragCancel = { dragProgress = null },
                ) { change, _ ->
                    dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            // Read inside the draw lambda. This is the line that keeps the audio off the
            // recomposition path.
            val bars = levels.value

            val midY = size.height / 2f
            val playedEnd = size.width * shown
            val stroke = 3.dp.toPx()
            val barWidth = 3.dp.toPx()
            val gap = size.width / BAR_COUNT

            var i = 0
            while (i < BAR_COUNT) {
                val x = i * gap + gap / 2f
                if (x > playedEnd) break
                val h = (size.height / 2f - stroke) * (0.12f + bars[i] * 0.88f)
                drawLine(
                    color = accent,
                    start = Offset(x, midY - h),
                    end = Offset(x, midY + h),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
                i++
            }

            if (playedEnd < size.width) {
                drawLine(
                    color = trackColor,
                    start = Offset(playedEnd + 8.dp.toPx(), midY),
                    end = Offset(size.width, midY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            drawLine(
                color = accent,
                start = Offset(playedEnd.coerceIn(stroke, size.width - stroke), midY - 10.dp.toPx()),
                end = Offset(playedEnd.coerceIn(stroke, size.width - stroke), midY + 10.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Enough bars to read as a spectrum at phone width without turning into a smear. */
private const val BAR_COUNT = 48

/** Per-frame fall on a 60 Hz clock. Fast enough to follow a beat, slow enough not to flicker. */
private const val DECAY = 0.035f
