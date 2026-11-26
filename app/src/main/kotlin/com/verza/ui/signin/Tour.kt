package com.verza.ui.signin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.verza.graph
import com.verza.ui.kit.Glassed
import com.verza.ui.kit.LocalTilt
import com.verza.ui.kit.Word
import com.verza.ui.kit.bouncy
import com.verza.ui.kit.glass
import com.verza.ui.kit.sentence
import com.verza.ui.kit.takesTouches
import com.verza.ui.look.GenZLook
import com.verza.ui.look.LocalLook
import com.verza.ui.look.LookPicker
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import com.verza.ui.theme.coverSwatches

/** Bump with new steps for the next release that has something worth walking through. */
const val TOUR = 25

private class Step(val title: String, val text: String, val art: @Composable (Modifier) -> Unit)

private val Steps = listOf(
    Step("pick a look", "Verza comes in two looks now. Tap one to wear it here, or change it later in Settings.", { LookPicker(it) }),
    Step("liquid glass", "Glass bends what's under it at its edge, as thick glass does. Tilt your phone and the light moves round the rim.", { GlassArt(it) }),
    Step("Gen Z", "Loud type in the colour of whatever's playing, a cover that fills the screen, and everything bounces when you touch it. In light and dark.", { GenZArt(it) }),
)

/** What an update brought, a step at a time, once: the welcome's page, with a picture of each thing over it. */
@Composable
fun Tour(onDone: () -> Unit) {
    val p = LocalPalette.current
    var i by rememberSaveable { mutableIntStateOf(0) }
    val last = i == Steps.lastIndex
    Column(
        Modifier.fillMaxSize().takesTouches().background(p.canvas).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp).padding(top = 48.dp, bottom = 28.dp),
    ) {
        // Each step slides in from the right and settles with a little give.
        AnimatedContent(i, label = "step", transitionSpec = {
            (slideInHorizontally(bouncy()) { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
        }) { at ->
            val step = Steps[at]
            Column {
                step.art(Modifier.fillMaxWidth().padding(bottom = 28.dp))
                Text("${at + 1} of ${Steps.size}", style = Type.Small, color = p.inkMuted)
                Text(step.title.sentence(), style = Type.Masthead, color = p.ink, modifier = Modifier.padding(top = 4.dp))
                Text(step.text, style = Type.RowTitle, color = p.inkMuted, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Row(Modifier.padding(top = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Word(if (last) "done" else "next", p.accent) { if (last) onDone() else i++ }
            if (!last) Word("skip", onClick = onDone)
        }
    }
}

/** A capsule of the glass over bars of colour, with the light going round its rim as if the phone were tilted. */
@Composable
private fun GlassArt(modifier: Modifier) {
    val p = LocalPalette.current
    val angle = rememberInfiniteTransition(label = "light").animateFloat(0f, 6.2832f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "a")
    val tilt = remember { derivedStateOf { Offset(1.3f * kotlin.math.cos(angle.value), 1f + 1.1f * kotlin.math.sin(angle.value)) } }
    Glassed(
        ground = p.canvas,
        modifier = modifier.aspectRatio(2f),
        content = {
            // Something to bend: stripes of the accent and the ink, under where the capsule sits.
            Box(Modifier.fillMaxSize().drawBehind {
                val bar = size.height / 9
                for (k in 0 until 9) if (k % 2 == 1) drawRoundRect(
                    if (k % 4 == 1) p.accent else p.ink.copy(alpha = 0.7f),
                    Offset(size.width * 0.08f, k * bar), Size(size.width * 0.84f, bar * 0.55f), CornerRadius(bar * 0.3f),
                )
            })
        },
        glass = {
            CompositionLocalProvider(LocalTilt provides tilt) {
                Box(
                    Modifier.align(Alignment.Center).fillMaxWidth(0.62f).height(76.dp).glass(CircleShape, 38.dp, stain = p.accent),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PlayArrow, null, tint = p.ink, modifier = Modifier.height(40.dp).aspectRatio(1f)) }
            }
        },
    )
}

/** Gen Z's own card, wide, in the playing song's colours. */
@Composable
private fun GenZArt(modifier: Modifier) {
    val graph = LocalContext.current.graph
    val swatches = coverSwatches(graph.player.state.collectAsState().value.song?.art)
    val dark = isSystemInDarkTheme()
    val p = remember(swatches, dark) { GenZLook.palette(swatches, dark) }
    CompositionLocalProvider(LocalLook provides GenZLook, LocalPalette provides p) {
        Box(modifier.aspectRatio(2f).clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).background(p.canvas)) {
            GenZLook.Preview(Modifier.fillMaxSize())
        }
    }
}
