package com.verza.ui.signin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verza.ui.kit.Word
import com.verza.ui.kit.bouncy
import com.verza.ui.kit.soft
import com.verza.ui.kit.sentence
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type

/** Bump with new steps for the next release that has something worth walking through. */
const val TOUR = 26

private class Step(val title: String, val text: String, val art: @Composable (Modifier) -> Unit)

private val Steps = listOf(
    Step("soft glass", "Verza is one soft material now. What you press rises out of the page and sinks under your finger. Under the cover in Now Playing, the music shows as a level meter.", { SoftArt(it) }),
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
                if (Steps.size > 1) Text("${at + 1} of ${Steps.size}", style = Type.Small, color = p.inkMuted)
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

/** Now Playing's three keys, play in ink, pressed in and springing back. */
@Composable
private fun SoftArt(modifier: Modifier) {
    val p = LocalPalette.current
    val press = rememberInfiniteTransition(label = "press").animateFloat(
        0f, 0f,
        infiniteRepeatable(keyframes { durationMillis = 2600; 0f at 900; 1f at 1040; 1f at 1400; 0f at 1800 }),
        label = "p",
    )
    Row(modifier.aspectRatio(2f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        for ((k, icon) in listOf(Icons.Rounded.SkipPrevious, Icons.Rounded.PlayArrow, Icons.Rounded.SkipNext).withIndex()) {
            val play = k == 1
            Box(
                Modifier.size(if (play) 88.dp else 60.dp).soft(CircleShape, if (play) 8.dp else 5.dp, press = if (play) press else null, face = if (play) p.ink else null),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = if (play) p.canvas else p.ink, modifier = Modifier.size(if (play) 42.dp else 28.dp)) }
        }
    }
}
