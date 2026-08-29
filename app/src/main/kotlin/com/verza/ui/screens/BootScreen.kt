package com.verza.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.verza.R
import com.verza.ui.expressive.BodyText
import com.verza.ui.expressive.CookieShape
import com.verza.ui.expressive.ExpressiveMotion
import com.verza.ui.expressive.HeroDisplay
import com.verza.ui.expressive.LocalExpressiveColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The launch sequence.
 *
 * The mark springs in inside a scalloped plate — the same silhouette the album art wears — so the
 * first thing the app shows is the shape language everything else is built from. Colours come from
 * the expressive palette, which on a cold start is Verza's own indigo and lime and on a warm start
 * is whatever was last playing.
 *
 * Everything is a spring rather than a timed curve, matching the rest of the app. Tap to skip.
 */
@Composable
fun BootScreen(onFinished: () -> Unit) {
    val colors = LocalExpressiveColors.current

    val markScale = remember { Animatable(0.6f) }
    val markAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val overallAlpha = remember { Animatable(1f) }

    var finished by remember { mutableStateOf(false) }
    val finish: suspend () -> Unit = {
        if (!finished) {
            finished = true
            overallAlpha.animateTo(0f, tween(180))
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        // Each beat on its own coroutine, so the stagger is start times rather than nested awaits.
        launch { markAlpha.animateTo(1f, tween(220)) }
        launch { markScale.animateTo(1f, ExpressiveMotion.spatialSlow()) }
        launch { delay(260); wordAlpha.animateTo(1f, tween(300)) }
        launch { delay(520); taglineAlpha.animateTo(1f, tween(260)) }
        delay(1450)
        finish()
    }

    val skipInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.container)
            .alpha(overallAlpha.value)
            // Tap anywhere to skip. No ripple: there is nothing else on screen, so an indication
            // would read as a control rather than as feedback.
            .clickable(interactionSource = skipInteraction, indication = null) {
                if (!finished) {
                    finished = true
                    onFinished()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .scale(markScale.value)
                    .alpha(markAlpha.value)
                    .clip(CookieShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_verza_glyph),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.onAccent),
                    modifier = Modifier.size(148.dp),
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "Verza",
                style = HeroDisplay,
                color = colors.onContainer,
                modifier = Modifier.alpha(wordAlpha.value),
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "A quieter way to listen.",
                style = BodyText,
                color = colors.onContainerMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value),
            )
        }
    }
}
