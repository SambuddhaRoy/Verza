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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.verza.ui.kit.Word
import com.verza.ui.kit.bouncy
import com.verza.ui.kit.sentence
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type

/** Bump with new steps for the next release that has something worth walking through. */
const val TOUR = 27

private class Step(val title: String, val text: String, val art: @Composable (Modifier) -> Unit)

// 3D covers need Android 13 (data/deep).
private val Steps = listOfNotNull(
    if (android.os.Build.VERSION.SDK_INT < 33) null else Step("3D covers", "Some covers have depth now. In Now Playing, tilt your phone and the scene moves behind whoever stands in front of it. Settings can turn it off.", { DepthArt(it) }),
    Step("quieter", "The buttons are plain glass, names are set in a bold sans, and the level meter under the cover has gone.", { KeysArt(it) }),
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

/**
 * A cover with depth, drawn: a sky and a sun far off, a hill nearer, someone standing in front. The
 * phone tilts from side to side, so the sky and the hill slide behind them (the hill less) while they
 * hold still, as a 3D cover does.
 */
@Composable
private fun DepthArt(modifier: Modifier) {
    val p = LocalPalette.current
    val tilt by rememberInfiniteTransition(label = "tilt").animateFloat(
        -1f, 1f, infiniteRepeatable(tween(1600, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "t",
    )
    Box(modifier.aspectRatio(2f), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxHeight().aspectRatio(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))) {
            val s = size.width
            val sky = androidx.compose.ui.graphics.lerp(p.canvas, p.accent, 0.3f)
            drawRect(sky)
            drawCircle(p.accent, s * 0.12f, androidx.compose.ui.geometry.Offset(s * (0.72f - 0.07f * tilt), s * 0.3f))
            drawCircle(androidx.compose.ui.graphics.lerp(sky, p.ink, 0.35f), s * 0.75f, androidx.compose.ui.geometry.Offset(s * (0.3f - 0.035f * tilt), s * 1.45f))
            // Whoever's in front: a head and shoulders, still.
            val ink = p.ink
            drawCircle(ink, s * 0.07f, androidx.compose.ui.geometry.Offset(s * 0.5f, s * 0.55f))
            drawRoundRect(ink, androidx.compose.ui.geometry.Offset(s * 0.38f, s * 0.65f), androidx.compose.ui.geometry.Size(s * 0.24f, s * 0.4f), androidx.compose.ui.geometry.CornerRadius(s * 0.08f))
        }
    }
}

/** Now Playing's three keys as they are now: back and on in plain glass, play in ink, which gives under a press. */
@Composable
private fun KeysArt(modifier: Modifier) {
    val p = LocalPalette.current
    val press by rememberInfiniteTransition(label = "press").animateFloat(
        0f, 0f,
        infiniteRepeatable(keyframes { durationMillis = 2600; 0f at 900; 1f at 1040; 1f at 1400; 0f at 1800 }),
        label = "p",
    )
    val edge = if (p.dark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f)
    Row(modifier.aspectRatio(2f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        for ((k, icon) in listOf(Icons.Rounded.SkipPrevious, Icons.Rounded.PlayArrow, Icons.Rounded.SkipNext).withIndex()) {
            val play = k == 1
            Box(
                Modifier.size(if (play) 76.dp else 56.dp).graphicsLayer { val g = if (play) 1f - 0.06f * press else 1f; scaleX = g; scaleY = g }
                    .then(if (play) Modifier.background(p.ink, CircleShape) else Modifier.background(p.fill, CircleShape).border(1.dp, edge, CircleShape)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = if (play) p.canvas else p.ink, modifier = Modifier.size(if (play) 36.dp else 28.dp)) }
        }
    }
}
