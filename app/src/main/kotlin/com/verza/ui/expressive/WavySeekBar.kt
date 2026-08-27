package com.verza.ui.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import kotlin.math.PI
import kotlin.math.sin

/**
 * The seek bar from the reference: the part you have already heard is a travelling wave, the part
 * you have not is a flat line, and the playhead is the join between them.
 *
 * Material 3 Expressive's own wavy indicator waves the *active* track and is not in the public API
 * of material3 1.4.0 anyway. This waves the *played* portion, which is the thing that makes the
 * control readable at a glance — the wave is a progress bar you can read without a number.
 *
 * [amplitude] is where the music gets in: pass a smoothed audio level and the wave breathes with
 * the track. It is clamped hard, because a seek bar that thrashes is a seek bar you cannot aim at.
 */
@Composable
fun WavySeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    accent: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    animating: Boolean = true,
    amplitude: Float = 0.5f,
    height: Dp = 40.dp,
) {
    // While dragging, the finger owns the position — otherwise the playhead would fight the
    // position updates still arriving from the player and stutter under the thumb.
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    var width by remember { mutableFloatStateOf(1f) }
    val shown = (dragProgress ?: progress).coerceIn(0f, 1f)

    // Phase drift, only while playing. A wave that keeps moving while paused reads as "still
    // loading" and is the kind of small lie that makes an interface feel untrustworthy.
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) phase -= (now - last) / 1_000_000_000f * 2.4f
                last = now
            }
        }
    }

    val amp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.ambient(),
        label = "waveAmplitude",
    )

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
            width = size.width
            val midY = size.height / 2f
            val playedEnd = size.width * shown
            val stroke = 3.dp.toPx()

            // Played: the wave. Amplitude eases to zero at the playhead so the wave resolves into
            // the line instead of being chopped off mid-crest.
            if (playedEnd > 1f) {
                val maxAmp = (size.height / 2f - stroke) * (0.35f + 0.65f * amp)
                val wavelength = 34.dp.toPx()
                val path = Path()
                var x = 0f
                var first = true
                while (x <= playedEnd) {
                    val taper = if (playedEnd <= 0f) 0f else (1f - (x / playedEnd)).coerceIn(0f, 1f)
                    // Only the last ~18% tapers, so most of the wave keeps full height.
                    val ease = if (taper > 0.18f) 1f else taper / 0.18f
                    val y = midY + sin((x / wavelength) * 2f * PI.toFloat() + phase) * maxAmp * ease
                    if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                    x += 2f
                }
                drawPath(path, accent, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }

            // Remaining: a flat line, dimmer. Deliberately not a wave — the contrast between the two
            // halves is what communicates progress.
            if (playedEnd < size.width) {
                drawLine(
                    color = trackColor,
                    start = Offset(playedEnd + 10.dp.toPx(), midY),
                    end = Offset(size.width, midY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            // Playhead.
            drawLine(
                color = accent,
                start = Offset(playedEnd.coerceIn(stroke, size.width - stroke), midY - 9.dp.toPx()),
                end = Offset(playedEnd.coerceIn(stroke, size.width - stroke), midY + 9.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
