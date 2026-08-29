package com.verza.ui.expressive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * The seek bar, drawn as a live spectrum.
 *
 * The played portion is a bar visualiser fed from the FFT; the rest is a flat line. Progress is still
 * readable at a glance because the two halves look nothing alike, which was the point of the wave it
 * replaces — but the bars now mean something, where the wave was a decorative sine.
 *
 * Bars decay rather than tracking the signal directly. The capture runs at about 30 Hz and raw band
 * values jitter hard between frames; falling at a fixed rate and only ever jumping *up* instantly
 * gives the familiar peak-hold look and stops the bar from strobing.
 */
@Composable
fun VisualizerSeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    accent: Color,
    trackColor: Color,
    bands: List<Float>,
    modifier: Modifier = Modifier,
    animating: Boolean = true,
    height: Dp = 36.dp,
) {
    // While dragging, the finger owns the position — otherwise the playhead fights the position
    // updates still arriving from the player and stutters under the thumb.
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val shown = (dragProgress ?: progress).coerceIn(0f, 1f)

    // Peak-hold levels, one per bar, persisted across frames.
    val levels = remember { FloatArray(BAR_COUNT) }
    if (animating && bands.isNotEmpty()) {
        for (i in levels.indices) {
            // Map the bar index onto the spectrum; there are usually more bars than bands.
            val v = bands[(i * bands.size / BAR_COUNT).coerceIn(0, bands.lastIndex)]
            levels[i] = max(v, levels[i] - DECAY)
        }
    } else {
        for (i in levels.indices) levels[i] = max(0f, levels[i] - DECAY)
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
            val midY = size.height / 2f
            val playedEnd = size.width * shown
            val stroke = 3.dp.toPx()
            val barWidth = 3.dp.toPx()
            val gap = (size.width / BAR_COUNT)

            // Played: bars, mirrored about the centre line so the whole control reads as one object
            // rather than as a graph sitting on a rule.
            var i = 0
            while (i < BAR_COUNT) {
                val x = i * gap + gap / 2f
                if (x > playedEnd) break
                val h = (size.height / 2f - stroke) * (0.12f + levels[i] * 0.88f)
                drawLine(
                    color = accent,
                    start = Offset(x, midY - h),
                    end = Offset(x, midY + h),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
                i++
            }

            // Remaining: a flat line. Deliberately not bars — the contrast between the halves is what
            // communicates progress without a number.
            if (playedEnd < size.width) {
                drawLine(
                    color = trackColor,
                    start = Offset(playedEnd + 8.dp.toPx(), midY),
                    end = Offset(size.width, midY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            // Playhead.
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

/** Per-frame fall. Fast enough to follow a beat, slow enough not to strobe at 30 Hz capture. */
private const val DECAY = 0.055f
