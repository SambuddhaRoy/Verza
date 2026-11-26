package com.verza.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

/*
 * The app's motion, in a few pieces used everywhere: things give under a finger and spring back, a
 * heart or a play button pops when it changes, cards rise into place. Springs, not durations, so a
 * movement interrupted carries on from where it was.
 */

/** A spring with a little overshoot: lively, but settled in a few hundred milliseconds. */
fun <T> bouncy(stiffness: Float = Spring.StiffnessMediumLow) = spring<T>(dampingRatio = 0.48f, stiffness = stiffness)

/** Gives a little under the finger and springs back past its size when let go. */
@Composable
fun Modifier.squish(source: InteractionSource, by: Float = 0.95f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) by else 1f, if (pressed) spring(stiffness = Spring.StiffnessHigh) else bouncy(), label = "squish")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/** Pops when [key] changes (not when it first shows): a heart filling, play turning to pause. */
@Composable
fun Modifier.popOn(key: Any?, from: Float = 0.72f): Modifier {
    val scale = remember { Animatable(1f) }
    var seen by remember { mutableStateOf<Any?>(Unseen) }
    LaunchedEffect(key) {
        if (seen === Unseen) { seen = key; return@LaunchedEffect }
        seen = key
        scale.snapTo(from)
        scale.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMedium))
    }
    return this.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

private object Unseen

/**
 * Rises into place the first time it's composed: from a little low and small, [order] steps after
 * the first, so a row of them arrives one after another.
 */
@Composable
fun Modifier.enter(order: Int = 0): Modifier {
    val t = remember { Animatable(0f) }
    val rise = with(LocalDensity.current) { 18f * density }
    LaunchedEffect(Unit) {
        delay(order.coerceAtMost(8) * 45L)
        t.animateTo(1f, bouncy())
    }
    return this.graphicsLayer {
        val v = t.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * rise
        val s = 0.94f + 0.06f * v
        scaleX = s; scaleY = s
    }
}

/**
 * A small physical tick, the kind the system's own keys give ([android.view.HapticFeedbackConstants]):
 * a key press by default, `CLOCK_TICK` for moving between tabs, `CONFIRM` for something kept (a like).
 * Nothing happens when the listener has turned touch feedback off; the system checks.
 */
@androidx.compose.runtime.Composable
fun rememberTick(kind: Int = android.view.HapticFeedbackConstants.KEYBOARD_TAP): () -> Unit {
    val view = androidx.compose.ui.platform.LocalView.current
    return androidx.compose.runtime.remember(view, kind) { { view.performHapticFeedback(kind); Unit } }
}

/** A tick for keeping something: `CONFIRM` where there is one (Android 11), else a key press. */
val ConfirmTick: Int = if (android.os.Build.VERSION.SDK_INT >= 30) android.view.HapticFeedbackConstants.CONFIRM else android.view.HapticFeedbackConstants.KEYBOARD_TAP
