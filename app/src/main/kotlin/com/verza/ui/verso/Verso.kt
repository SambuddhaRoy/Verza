package com.verza.ui.verso

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.sin

/**
 * ── VERSO ─────────────────────────────────────────────────────────────────────
 * The design language of the ground-up UI redesign. Three ideas, applied everywhere:
 *
 *  1. **The thread** — one continuous, gently undulating line is the app's signature mark.
 *     It is the navigation horizon, the seek bar, the progress underline and the masthead
 *     flourish. It idles with a slow drift and quickens when music plays.
 *  2. **Pebbles** — no two surfaces share the same silhouette. Every card and tile gets a
 *     deterministic, slightly-asymmetric rounded shape seeded from its content key, so pages
 *     feel hand-laid rather than grid-stamped.
 *  3. **Breath** — surfaces are subtly alive: a barely-perceptible scale/drift oscillation on
 *     desynchronised phases, so the whole page shimmers like something resting, not a static
 *     screenshot.
 */

private const val TWO_PI = (2.0 * Math.PI).toFloat()
private const val BREATH_MS = 5600
private const val THREAD_MS = 4200

// ── Pebbles ──────────────────────────────────────────────────────────────────

/**
 * A deterministic, organic rounded shape: four different corner radii derived from [key]'s hash,
 * so the same item always gets the same silhouette but no two items match. [base] ± [swing].
 */
fun pebbleShape(key: Any?, base: Dp = 20.dp, swing: Dp = 12.dp): RoundedCornerShape {
    val h = key?.hashCode() ?: 0
    fun corner(shift: Int): Dp {
        val u = ((h ushr shift) and 0xFF) / 255f          // 0..1, stable per key + corner
        return (base + swing * (u * 2f - 1f)).coerceAtLeast(7.dp)
    }
    return RoundedCornerShape(corner(0), corner(8), corner(16), corner(24))
}

// ── Breath ───────────────────────────────────────────────────────────────────

/**
 * The "subtly alive" idle motion: a slow, tiny scale + vertical-drift oscillation. [seed]
 * desynchronises neighbours so a page of cards shimmers organically instead of pulsing in
 * lockstep. Draw-phase only (graphicsLayer lambda), so it never recomposes the tree.
 */
@Composable
fun Modifier.breathe(seed: Int = 0, amount: Float = 0.006f, drift: Dp = 0.dp): Modifier {
    val transition = rememberInfiniteTransition(label = "versoBreath")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(BREATH_MS, easing = LinearEasing)),
        label = "versoBreathPhase",
    )
    val offset = remember(seed) { (seed * 1.618f) % TWO_PI }
    val driftPx = with(LocalDensity.current) { drift.toPx() }
    return this.graphicsLayer {
        val s = 1f + amount * sin(phase + offset)
        scaleX = s
        scaleY = s
        translationY = driftPx * sin(phase + offset * 1.31f)
    }
}

// ── The thread ───────────────────────────────────────────────────────────────

/** Continuous slow phase for living thread lines; frozen at 0 when [alive] is false. */
@Composable
fun threadPhase(alive: Boolean): State<Float> = if (alive) {
    val transition = rememberInfiniteTransition(label = "versoThread")
    transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(THREAD_MS, easing = LinearEasing)),
        label = "versoThreadPhase",
    )
} else {
    remember { mutableFloatStateOf(0f) }
}

/** Draws one undulating thread segment between [fromX] and [toX] at the vertical centre. */
fun DrawScope.drawThread(
    color: Color,
    amplitudePx: Float,
    wavelengthPx: Float,
    strokePx: Float,
    phase: Float,
    fromX: Float = 0f,
    toX: Float = size.width,
    yCenter: Float = size.height / 2f,
) {
    if (toX - fromX <= 1f) return
    fun wave(x: Float) = yCenter + amplitudePx * sin(TWO_PI * x / wavelengthPx + phase)
    val path = Path()
    path.moveTo(fromX, wave(fromX))
    var x = fromX
    while (x < toX) {
        x = min(x + 6f, toX)
        path.lineTo(x, wave(x))
    }
    drawPath(path, color, style = Stroke(width = strokePx, cap = StrokeCap.Round))
}

/**
 * A standalone thread line — the underline / divider / progress mark of the language.
 * [reveal] draws only the leading fraction (animate it for the draw-itself-in entrance).
 */
@Composable
fun ThreadLine(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    amplitude: Dp = 2.5.dp,
    wavelength: Dp = 42.dp,
    thickness: Dp = 1.8.dp,
    reveal: Float = 1f,
    alive: Boolean = true,
) {
    val phase by threadPhase(alive)
    Canvas(modifier = modifier.height(amplitude * 2 + thickness * 2)) {
        drawThread(
            color = color,
            amplitudePx = amplitude.toPx(),
            wavelengthPx = wavelength.toPx(),
            strokePx = thickness.toPx(),
            phase = phase,
            toX = size.width * reveal.coerceIn(0f, 1f),
        )
    }
}

/**
 * The Verso page masthead: an optional lowercase eyebrow, a big lowercase display title, and a
 * thread flourish that draws itself in on entry (and keeps drifting while [alive]).
 */
@Composable
fun VersoMasthead(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    alive: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 850, delayMillis = 120),
        label = "versoMastheadReveal",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(eyebrow.lowercase(), style = MaterialTheme.typography.labelSmall, color = colors.primary)
                Spacer(Modifier.height(6.dp))
            }
            Text(title.lowercase(), style = MaterialTheme.typography.displayMedium, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            ThreadLine(
                modifier = Modifier.width(116.dp),
                color = colors.primary,
                reveal = reveal,
                alive = alive,
            )
        }
        trailing?.invoke()
    }
}

/**
 * The thread as a seek bar: the played side is a living wave, the remainder a near-still faint
 * thread, and a node dot rides the seam. Tap or drag anywhere to seek (fraction-based).
 */
@Composable
fun ThreadSeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    alive: Boolean = true,
    played: Color = MaterialTheme.colorScheme.primary,
    rest: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    var dragFrac by remember { mutableStateOf<Float?>(null) }
    // Glide between the service's ~500 ms position ticks so the thread flows, not steps.
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "versoSeek",
    )
    val shown = (dragFrac ?: animated).coerceIn(0f, 1f)
    val phase by threadPhase(alive)
    Canvas(
        modifier = modifier
            .height(30.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragFrac = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragFrac?.let(onSeek); dragFrac = null },
                    onDragCancel = { dragFrac = null },
                ) { change, _ ->
                    change.consume()
                    dragFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            },
    ) {
        val seam = size.width * shown
        val amp = 2.8.dp.toPx()
        val wavelength = 40.dp.toPx()
        val cy = size.height / 2f
        // Remainder first (so the played thread overdraws the joint cleanly).
        drawThread(rest, amp * 0.22f, wavelength, 1.4.dp.toPx(), phase, fromX = seam)
        drawThread(played, amp, wavelength, 2.2.dp.toPx(), phase, toX = seam)
        // The node riding the seam, sitting exactly on the wave.
        val nodeY = cy + amp * sin(TWO_PI * seam / wavelength + phase)
        drawCircle(played, radius = 4.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(seam, nodeY))
    }
}
