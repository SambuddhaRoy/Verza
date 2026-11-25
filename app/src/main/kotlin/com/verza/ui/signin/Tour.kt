package com.verza.ui.signin

import com.verza.ui.kit.sentence
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.ui.kit.Word
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type

/** Bump with new steps for the next release that has something worth walking through. */
const val TOUR = 24

private enum class Art { LINK, OVER, BACK, READY, ASK }
private class Step(val art: Art, val title: String, val text: String)

private val Steps = listOf(
    Step(Art.LINK, "verza connect", "This phone and Verza Next on your computer, as one player. Turn on Verza Connect on the computer, then add it here in Settings, under devices, with the code it shows. This phone sees what plays there, controls it, and hands songs over either way."),
    Step(Art.OVER, "flick it over", "With music playing, flick the phone toward your computer. The song moves there from the moment it was at, with the rest of the queue. It works from any page."),
    Step(Art.BACK, "flick it back", "Flick the phone back toward you, and what the computer plays comes here, picking up where it was."),
    Step(Art.READY, "no waiting", "Whatever this phone starts, the computer quietly loads too, so a song handed over starts at once."),
    Step(Art.ASK, "keep which?", "Already playing something here? What's flicked in wins. If you'd rather choose, turn on Ask which to keep in Settings, under devices."),
)

/**
 * What an update brought, a step at a time, once: the welcome's page, with a drawing of this phone and
 * the computer over each step. [paired] false makes the last step offer Settings, where a computer is added.
 */
@Composable
fun Tour(paired: Boolean, onDone: () -> Unit, onSetUp: () -> Unit) {
    val p = LocalPalette.current
    var i by rememberSaveable { mutableIntStateOf(0) }
    val step = Steps[i]
    val last = i == Steps.lastIndex
    Column(
        Modifier.fillMaxSize().takesTouches().background(p.canvas).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp).padding(top = 48.dp, bottom = 28.dp),
    ) {
        Drawing(step.art, Modifier.fillMaxWidth().aspectRatio(2f).padding(bottom = 28.dp))
        Text("${i + 1} of ${Steps.size}", style = Type.Small, color = p.inkMuted)
        Text(step.title.sentence(), style = Type.Masthead, color = p.ink, modifier = Modifier.padding(top = 4.dp))
        Text(step.text, style = Type.RowTitle, color = p.inkMuted, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.padding(top = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Word(if (last) "done" else "next", p.accent) { if (last) onDone() else i++ }
            if (!last) Word("skip", onClick = onDone)
            else if (!paired) Word("add a computer", onClick = onSetUp)
        }
    }
}

/** The phone on the left, the computer on the right, and what moves between them. In units of a 360x180 box. */
@Composable
private fun Drawing(art: Art, modifier: Modifier) {
    val p = LocalPalette.current
    val t by rememberInfiniteTransition(label = "tour").animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Canvas(modifier) {
        val u = size.width / 360f
        val line = Stroke(2f * u)
        val ink = p.ink.copy(alpha = 0.5f)
        val phone = Offset(65f, 117f) * u
        val screen = Offset(255f, 69f) * u
        fun square(c: Offset, side: Float, color: Color, scale: Float = 1f) {
            val s = side * u * scale
            drawRoundRect(color, Offset(c.x - s / 2, c.y - s / 2), Size(s, s), CornerRadius(3f * u * scale))
        }
        fun ease(a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
        fun ring(c: Offset) {
            val k = ((t - 0.5f) / 0.35f).coerceIn(0f, 1f)
            if (t in 0.5f..0.85f) drawCircle(p.accent.copy(alpha = 0.9f * (1 - k)), 22f * u * (0.9f + 1.3f * k), c, style = line)
        }
        // The phone tips toward where the music goes, then back.
        val tip = when (art) { Art.OVER -> 12f; Art.BACK -> -12f; else -> 0f } *
            (if (t < 0.25f) ease(0.15f, 0.25f) else 1 - ease(0.25f, 0.4f))
        rotate(tip, pivot = Offset(65f, 164f) * u) {
            drawRoundRect(ink, Offset(40f, 70f) * u, Size(50f, 94f) * u, CornerRadius(9f * u), style = line)
        }
        drawRoundRect(ink, Offset(170f, 14f) * u, Size(170f, 110f) * u, CornerRadius(8f * u), style = line)
        drawLine(ink, Offset(255f, 124f) * u, Offset(255f, 146f) * u, line.width)
        drawLine(ink, Offset(225f, 146f) * u, Offset(285f, 146f) * u, line.width)
        when (art) {
            Art.LINK -> {
                val path = Path().apply { moveTo(90f * u, 117f * u); cubicTo(130f * u, 117f * u, 140f * u, 69f * u, 170f * u, 69f * u) }
                drawPath(path, p.accent.copy(alpha = 0.8f), style = Stroke(2f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * u, 6f * u), -t * 33f * u)))
                square(phone, 26f, p.accent)
                square(screen, 34f, p.accent, 1f + 0.12f * kotlin.math.sin(t * 2 * Math.PI.toFloat()).let { it * it })
            }
            Art.OVER, Art.BACK -> {
                val k = ease(0.2f, 0.55f).let { if (art == Art.OVER) it else 1 - it }
                val at = phone + (screen - phone) * k
                square(at, 26f, p.accent.copy(alpha = 1 - ease(0.85f, 1f)), 1f + 0.9f * k)
                ring(if (art == Art.OVER) screen else phone)
            }
            Art.READY -> {
                square(phone, 26f, p.accent)
                drawRoundRect(p.ink.copy(alpha = 0.15f), Offset(200f, 94f) * u, Size(110f, 4f) * u, CornerRadius(2f * u))
                drawRoundRect(p.accent, Offset(200f, 94f) * u, Size(110f * ease(0f, 0.6f), 4f) * u, CornerRadius(2f * u))
            }
            Art.ASK -> {
                square(phone, 26f, p.accent)
                val a = 0.5f + 0.5f * kotlin.math.cos(t * 2 * Math.PI.toFloat())
                square(Offset(234f, 61f) * u, 34f, p.accent.copy(alpha = 0.35f + 0.65f * a), 1f + 0.08f * a)
                square(Offset(276f, 61f) * u, 34f, p.inkMuted.copy(alpha = 0.35f + 0.65f * (1 - a)), 1f + 0.08f * (1 - a))
            }
        }
    }
}
