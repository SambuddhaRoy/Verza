package com.verza.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.R
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.FontDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cold-launch brand reveal. Plays after the system splash hands off; lasts ~1800 ms then
 * fades into [onFinished]. Tap anywhere to skip.
 *
 * Choreography (every cold launch — the system splash carries the launch-to-Activity
 * bridge, this paints the personality):
 *
 *  0 ms   — icon sits where the OS splash placed it
 *  80 ms  — icon glides up ~40 dp (400 ms, FastOutSlowIn)
 *  200 ms — accent rule wipes in beneath the icon (200 ms, left-to-right)
 *  400 ms — "Verza" wordmark fades up; letter-spacing collapses from +8sp to -0.4sp (500 ms)
 *  900 ms — italic tagline fades in (200 ms)
 *  1200 ms — soft radial glow blooms behind the composition (400 ms)
 *  1600 ms — hold
 *  1800 ms — entire composition fades out (250 ms) → onFinished
 */
@Composable
fun BootScreen(onFinished: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    // Each animatable drives one visual property. Initial values describe frame zero —
    // everything except the icon is invisible / off-position.
    val iconOffsetDp = remember { Animatable(0f) }
    val ruleProgress = remember { Animatable(0f) }   // 0..1 width fraction of the 32 dp rule
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkSpacingSp = remember { Animatable(8f) }
    val taglineAlpha = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }
    val overallAlpha = remember { Animatable(1f) }

    var skipped by remember { mutableStateOf(false) }
    var finishedHandled by remember { mutableStateOf(false) }

    // Skip path: fade out fast and finish. Idempotent via `finishedHandled` so a tap
    // during the natural fade-out doesn't fire onFinished twice.
    LaunchedEffect(skipped) {
        if (!skipped) return@LaunchedEffect
        if (finishedHandled) return@LaunchedEffect
        finishedHandled = true
        overallAlpha.animateTo(0f, tween(150))
        onFinished()
    }

    // Natural choreography. Each beat is its own coroutine on the same scope so the
    // staggered start times don't have to be expressed as nested awaits.
    LaunchedEffect(Unit) {
        launch { delay(80);   iconOffsetDp.animateTo(-40f, tween(400, easing = FastOutSlowInEasing)) }
        launch { delay(200);  ruleProgress.animateTo(1f, tween(200, easing = LinearOutSlowInEasing)) }
        launch { delay(400);  wordmarkAlpha.animateTo(1f, tween(500)) }
        launch { delay(400);  wordmarkSpacingSp.animateTo(-0.4f, tween(500, easing = FastOutSlowInEasing)) }
        launch { delay(900);  taglineAlpha.animateTo(1f, tween(200)) }
        launch { delay(1200); glowAlpha.animateTo(0.18f, tween(400)) }

        delay(1800)
        if (finishedHandled) return@LaunchedEffect
        finishedHandled = true
        overallAlpha.animateTo(0f, tween(250))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .alpha(overallAlpha.value)
            // Tap-anywhere-to-skip. Plain clickable with no ripple — the boot screen has
            // no other affordances so ripple feedback would be visual noise.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { skipped = true },
            ),
    ) {
        // Layer 1: radial glow that blooms in late in the sequence. Drawn first so the
        // wordmark/icon sit on top.
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    if (glowAlpha.value <= 0f) return@drawBehind
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = glowAlpha.value),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.width * 0.7f,
                        ),
                    )
                },
        )

        // Layer 2: the composition (icon + rule + wordmark + tagline), stacked vertically
        // and centred. The icon's vertical translation is driven by `iconOffsetDp`.
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Tint the foreground vector to the current theme's onBackground so the V mark
            // adapts: ink-on-cream on Atelier light, cream-on-coffee on Atelier dark. The
            // 25 %-alpha stroke lines in the drawable preserve their transparency through
            // tint (ColorFilter.tint multiplies, doesn't override alpha), so the texture
            // lines remain whispered rather than solid.
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.onBackground),
                // Bumped from 132 dp → 180 dp to compensate for the launcher icon's new inset
                // geometry. The mark now occupies ~50 % of its canvas instead of ~75 %, so a
                // larger rendered size keeps the brand reveal feeling weighty rather than tiny.
                modifier = Modifier
                    .size(180.dp)
                    .offsetVerticalDp(iconOffsetDp.value),
            )

            Spacer(Modifier.height(24.dp))

            // Accent rule — width grows from 0 to 32 dp via ruleProgress. Implementing the
            // wipe via Modifier.layout rather than animating a Dp directly so we don't lose
            // sub-pixel precision on small displays.
            Box(
                Modifier
                    .height(2.dp)
                    .width((32 * ruleProgress.value).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.primary),
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Verza",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                    letterSpacing = wordmarkSpacingSp.value.sp,
                ),
                color = colors.onBackground,
                modifier = Modifier.alpha(wordmarkAlpha.value),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "A quieter way to listen.",
                style = CaptionItalic.copy(fontSize = 16.sp),
                color = colors.onSurfaceVariant,
                modifier = Modifier.alpha(taglineAlpha.value),
            )
        }
    }
}

/**
 * Translates a composable vertically by [offsetDp] without affecting layout — used so the
 * icon can glide up without pushing the rule/wordmark beneath it.
 */
@Composable
private fun Modifier.offsetVerticalDp(offsetDp: Float): Modifier {
    val density = LocalDensity.current
    val offsetPx = with(density) { offsetDp.dp.toPx() }
    return this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, offsetPx.toInt())
        }
    }
}
