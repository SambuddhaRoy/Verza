package com.verza.ui.signin

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.verza.R
import com.verza.ui.kit.Word
import com.verza.ui.kit.bouncy
import com.verza.ui.kit.sentence
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type

/** Bump with new steps for the next release that has something worth walking through. */
const val TOUR = 27

private class Step(val title: String, val text: String, @DrawableRes val dark: Int, @DrawableRes val light: Int)

// 3D covers need Android 13 (data/deep).
private val Steps = listOfNotNull(
    if (android.os.Build.VERSION.SDK_INT < 33) null else
        Step("3D covers", "Some album covers now move in 3D. Tilt your phone in Now Playing and look around.", R.drawable.tour_depth_dark, R.drawable.tour_depth_light),
    Step("A cleaner look", "Simple glass buttons, bold type and more room for the cover.", R.drawable.tour_keys_dark, R.drawable.tour_keys_light),
)

/** A picture of the app itself, in the theme on show, over a step of the welcome or the tour. */
@Composable
internal fun Shot(@DrawableRes dark: Int, @DrawableRes light: Int, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    val edge = if (p.dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f)
    Image(
        painterResource(if (p.dark) dark else light), null, contentScale = ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth().clip(shape).border(0.5.dp, edge, shape),
    )
}

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
                Shot(step.dark, step.light, Modifier.padding(bottom = 28.dp))
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
