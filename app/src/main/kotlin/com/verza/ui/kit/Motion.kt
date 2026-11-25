package com.verza.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * The app's motion, in a few pieces used everywhere: things give under a finger and spring back, a
 * heart or a play button pops when it changes, cards rise into place, Gen Z's names drop in a letter
 * at a time. Springs, not durations, so a movement interrupted carries on from where it was.
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
 * [text] whose letters drop in one after another on a spring, the first time it shows. Read as one
 * word by TalkBack. For short words: page names.
 */
@Composable
fun BouncyLetters(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, heading: Boolean = true) {
    val drop = with(LocalDensity.current) { (style.fontSize.toPx() * 0.6f) }
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = text; if (heading) heading() }, verticalAlignment = Alignment.Bottom) {
        text.forEachIndexed { i, ch ->
            val t = remember(text) { Animatable(0f) }
            LaunchedEffect(text) {
                delay(i * 28L)
                launch { t.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow)) }
            }
            Text(
                ch.toString(), style = style, color = color, softWrap = false,
                modifier = Modifier.clearAndSetSemantics { }.graphicsLayer { translationY = (1f - t.value) * -drop; alpha = t.value.coerceIn(0f, 1f) },
            )
        }
    }
}
