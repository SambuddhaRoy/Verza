package com.verza.ui.look

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.ui.kit.Cover
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Variable fonts, after Figma's page on them: lavender and green, flat shapes (circles, stairs, a
 * crown, a squiggle), type set in one face whose axes slide. Here the axes follow the music: page
 * names breathe while nothing plays and swell with the bass when something does, the tabs are words
 * whose weight and width spring to the one chosen, and every name sits in a selection box with its
 * axes read out, as a design tool shows them. All in Roboto Flex.
 */
object FlexLook : Look("flex", "Variable fonts", "Type that moves with the music") {
    private val Lavender = Color(0xFFCBB9FF)
    private val Ink = Color(0xFF0D0D0D)
    private val Green = Color(0xFF14AE5C)
    private val Yellow = Color(0xFFFFC700)
    private val Pink = Color(0xFFFF7262)
    private val Blue = Color(0xFF4D49FC)
    /** A design tool's selection blue, dark enough for white words on it. */
    private val Select = Color(0xFF0A62E6)
    private val ShapeColours = listOf(Green, Pink, Yellow, Blue)

    /** A style in Roboto Flex at weight [w] (100 to 1000) and width [wd] (25 to 151). */
    fun flex(w: Int, wd: Float, size: Float, line: Float, spacing: Float = 0f) = w.coerceIn(100, 1000).let { cw ->
        TextStyle(fontFamily = Fonts.flex(cw, wd), fontWeight = FontWeight(cw), fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.em) }

    /** Snapped, so each value is a typeface made once: weight to 50s, width to 10s. */
    private fun axes(value: Float, wMin: Int = 250, wMax: Int = 1000, wdMin: Float = 70f, wdMax: Float = 140f): Pair<Int, Float> {
        // A spring overshoots: past 1 asked for weight 1050, and a weight over 1000 throws.
        val b = value.coerceIn(0f, 1f)
        val w = ((wMin + (wMax - wMin) * b) / 50f).roundToInt() * 50
        val wd = ((wdMin + (wdMax - wdMin) * b) / 10f).roundToInt() * 10f
        return w to wd
    }

    /** 0 to 1: a slow breath, and the bass on top of it while music plays. [phase] staggers words. */
    @Composable
    private fun breath(phase: Float = 0f): Float {
        val t by slowTime(12)
        val level by rememberLevel()
        return (0.3f + 0.22f * sin(t * 0.9f + phase) + level * 0.7f).coerceIn(0f, 1f)
    }

    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Lavender, ink = Ink, accent = Color(0xFF2522B8), cell = Color.White.copy(alpha = 0.55f), fill = Color.White.copy(alpha = 0.5f), line = Ink.copy(alpha = 0.22f), inkMuted = Color(0xFF3A3360))

    override fun cover(size: Dp): Shape = RoundedCornerShape(if (size in 1.dp..64.dp) 8.dp else 16.dp)
    override val group: Shape = RoundedCornerShape(18.dp)
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 64.dp + (if (playing) 60.dp else 0.dp)

    override val type = Types(
        masthead = flex(900, 100f, 48f, 52f, -0.01f),
        section = flex(800, 90f, 24f, 28f),
        title = flex(850, 100f, 30f, 34f, -0.01f),
        headline = flex(700, 100f, 16f, 20f),
        rowTitle = flex(500, 100f, 16f, 21f),
        rowSubtitle = flex(400, 90f, 14f, 18f),
        body = flex(400, 100f, 15f, 21f),
        small = flex(500, 90f, 12f, 16f),
        tab = flex(600, 100f, 13f, 16f),
        label = flex(600, 80f, 11f, 14f, 0.02f),
    )

    /** Figma's flat shapes: [kind] 0 three circles, 1 stairs, 2 a crown, 3 a squiggle, filling this box. */
    internal fun DrawScope.figmaShape(kind: Int, c: Color) {
        val w = size.width; val h = size.height
        when (kind % 4) {
            0 -> { val r = minOf(w / 6, h / 2); for (k in 0..2) drawCircle(c, r, Offset(r + k * (w - 2 * r) / 2, h / 2)) }
            1 -> {
                val p = Path().apply { moveTo(0f, h); var x = 0f; var y = h; val s = minOf(w, h) / 4; while (x < w - s && y > s) { lineTo(x, y - s); y -= s; lineTo(x + s, y); x += s } }
                drawPath(p, c, style = Stroke(minOf(w, h) * 0.16f, join = StrokeJoin.Miter))
            }
            2 -> drawPath(Path().apply { moveTo(0f, 0f); lineTo(w * 0.25f, h * 0.35f); lineTo(w * 0.5f, 0f); lineTo(w * 0.75f, h * 0.35f); lineTo(w, 0f); lineTo(w, h); lineTo(w * 0.75f, h * 0.65f); lineTo(w * 0.5f, h); lineTo(w * 0.25f, h * 0.65f); lineTo(0f, h); close() }, c)
            else -> drawPath(Path().apply { moveTo(h * 0.25f, h * 0.25f); lineTo(w * 0.7f, h * 0.25f); cubicTo(w, h * 0.25f, w, h * 0.75f, w * 0.7f, h * 0.75f); lineTo(h * 0.25f, h * 0.75f) }, c, style = Stroke(h * 0.32f, cap = StrokeCap.Round))
        }
    }

    /** A design tool's selection: a thin blue box with square handles at its corners and mid-edges. */
    private fun Modifier.selection() = drawBehind {
        val o = 4.dp.toPx(); val h = 6.dp.toPx()
        drawRect(Select, Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), style = Stroke(1.2f * density))
        for (x in listOf(-o, size.width / 2, size.width + o)) for (y in listOf(-o, size.height / 2, size.height + o)) {
            if (x == size.width / 2 && y == size.height / 2) continue
            drawRect(Color.White, Offset(x - h / 2, y - h / 2), Size(h, h))
            drawRect(Select, Offset(x - h / 2, y - h / 2), Size(h, h), style = Stroke(1.2f * density))
        }
    }

    /** A blue tag with the axes on it: "wght 650 · wdth 120". */
    @Composable
    private fun AxisTag(w: Int, wd: Float, modifier: Modifier = Modifier) {
        Text("wght $w  ·  wdth ${wd.toInt()}", style = flex(600, 90f, 10f, 13f), color = Color.White, modifier = modifier.background(Select, RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp).clearAndSetSemantics { })
    }

    /** The name breathing in a selection box, its axes on a tag under it. Its box keeps its height as it moves. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        val (w, wd) = axes(breath(), wdMax = 125f)
        Row(modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (dateline != null) Text(dateline, style = type.small, color = Ink.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 8.dp))
                Box(Modifier.height(58.dp).clipToBounds().padding(6.dp)) {
                    Text(title, style = flex(w, wd, 44f, 48f, -0.01f), color = Ink, maxLines = 1, softWrap = false, modifier = Modifier.selection().semantics { heading() })
                }
                AxisTag(w, wd, Modifier.padding(start = 2.dp, top = 6.dp))
            }
            trailing?.invoke()
        }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        Row(modifier.padding(top = 26.dp, bottom = 12.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
            Text("T", style = flex(700, 60f, 12f, 14f), color = Select, modifier = Modifier.padding(end = 8.dp).clearAndSetSemantics { })
            Text(case.of(text), style = type.section, color = Ink)
        }
    }

    /** A green slab with a black edge; the words get heavier and wider under the finger. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        val source = remember { MutableInteractionSource() }
        val pressed by source.collectIsPressedAsState()
        val push by animateFloatAsState(if (pressed) 1f else 0f, spring(stiffness = Spring.StiffnessMedium), label = "push")
        val (w, wd) = axes(push, wMin = 650, wMax = 1000, wdMin = 100f, wdMax = 140f)
        Row(
            modifier.heightIn(min = 50.dp).clip(RoundedCornerShape(12.dp)).background(if (primary) Green else Color.White).border(2.dp, Ink, RoundedCornerShape(12.dp))
                .clickable(source, null, enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = Ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text(case.of(text), style = flex(w, wd, 16f, 20f), color = if (enabled) Ink else Ink.copy(alpha = 0.4f), maxLines = 1, softWrap = false)
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Box(
            modifier.size(size).clip(CircleShape).background(if (stain != null) Green else Color.White).border(2.dp, Ink, CircleShape)
                .clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Ink, modifier = Modifier.size(iconSize)) }
    }

    @Composable
    override fun Divider(start: Dp) {
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(Ink.copy(alpha = 0.25f), Offset(start.toPx(), 0f), Offset(size.width, 0f), size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
        }
    }

    /** Covers with a shape stuck at a corner, each title a step heavier than the last: weight as a scale. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        val weights = listOf(300, 500, 700, 900, 1000, 800, 600, 400)
        LazyRow(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                Column(Modifier.width(146.dp).clickable(onClick = cardAction(card, onOpen))) {
                    Box {
                        Cover(card.art?.let { artAt(it, 544) }, 146.dp, Modifier.border(2.dp, Ink, cover(146.dp)))
                        Canvas(Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp).size(if (i % 4 == 0) 46.dp else 34.dp, if (i % 4 == 0) 18.dp else 34.dp)) { figmaShape(i, ShapeColours[i % 4]) }
                    }
                    Text(cardTitle(card), style = flex(weights[i % weights.size], 100f, 15f, 19f), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                    Text("wght ${weights[i % weights.size]}", style = type.label, color = Select)
                }
            }
        }
    }

    /** "Any ●●● way / you ♛ play / it ∿": the words breathing out of step with each other, among the shapes. */
    @Composable
    override fun HomeHero() {
        @Composable
        fun Word(s: String, phase: Float) {
            val (w, wd) = axes(breath(phase))
            Text(s, style = flex(w, wd, 46f, 50f, -0.02f), color = Ink, maxLines = 1, softWrap = false, modifier = Modifier.clearAndSetSemantics { })
        }
        @Composable
        fun Sticker(kind: Int, w: Dp, h: Dp) = Canvas(Modifier.padding(horizontal = 8.dp).size(w, h)) { figmaShape(kind, ShapeColours[kind % 4]) }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).semantics { contentDescription = "Any way you play it" }) {
            Row(Modifier.height(54.dp), verticalAlignment = Alignment.CenterVertically) { Word("Any", 0f); Sticker(0, 96.dp, 34.dp); Word("way", 1.3f) }
            Row(Modifier.height(54.dp), verticalAlignment = Alignment.CenterVertically) { Sticker(1, 44.dp, 44.dp); Word("you", 2.6f); Sticker(2, 50.dp, 40.dp); Word("play", 3.9f) }
            Row(Modifier.height(54.dp), verticalAlignment = Alignment.CenterVertically) { Word("it", 5.2f); Sticker(3, 80.dp, 30.dp) }
        }
    }

    /** The places as words only: the chosen one springs heavy and wide, the rest go light and narrow. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().takesTouches().background(Color.White).drawBehind { drawRect(Ink, size = Size(size.width, 2.dp.toPx())) }.navigationBarsPadding()) {
            state.downloads?.let { DownloadsLine(it, Ink) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                val (w, wd) = axes(breath(), wMin = 500, wMax = 1000, wdMin = 90f, wdMax = 130f)
                Row(
                    Modifier.fillMaxWidth().height(60.dp).background(Green).drawBehind { drawRect(Ink, Offset(0f, size.height - 3.dp.toPx()), Size(size.width * played, 3.dp.toPx())) }
                        .clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 40.dp, Modifier.border(2.dp, Ink, cover(40.dp)))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist), style = flex(w, wd, 16f, 20f), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = type.small, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlayPauseNext(state.now, Ink, 28.dp)
                }
            }
            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    val a by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow), label = "tab")
                    val (w, wd) = axes(a, wMin = 300, wMax = 1000, wdMin = 70f, wdMax = 130f)
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { state.go(tab.route) }.semantics { role = Role.Tab; selected = on; contentDescription = tab.label }, contentAlignment = Alignment.Center) {
                        Text(tab.label.substringAfterLast(' '), style = flex(w, wd, 17f, 20f), color = Ink, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                        if (on) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp).size(6.dp).background(Green, CircleShape))
                    }
                }
            }
        }
    }

    override fun nowPlaying(page: Palette) = page

    /** Lavender with the four shapes drifting at the top corners. */
    @Composable
    override fun NowPlayingBackground(art: String?) {
        val a = bob(4200); val b = bob(5600)
        Box(Modifier.fillMaxSize().background(Lavender)) {
            Canvas(Modifier.size(140.dp, 50.dp).offset(x = (-30).dp, y = (120 + a * 8).dp)) { figmaShape(0, Green) }
            Canvas(Modifier.align(Alignment.TopEnd).size(70.dp).offset(x = 10.dp, y = (150 + b * 10).dp)) { figmaShape(1, Pink) }
            Canvas(Modifier.align(Alignment.TopEnd).size(64.dp, 50.dp).offset(x = (-40).dp, y = (70 - a * 6).dp)) { figmaShape(2, Yellow) }
        }
    }

    /** The cover as a selected frame in a design tool: its name over it, its size under it. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        val c = side * 0.92f
        Column(Modifier.graphicsLayer { scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX; translationY = -fly * size.height * 2f; alpha = 1f - fly }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♪ Now playing", style = flex(600, 90f, 11f, 14f), color = Select, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp).clearAndSetSemantics { })
            AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(c).selection().clip(RoundedCornerShape(4.dp)))
            Text("${c.value.roundToInt()} × ${c.value.roundToInt()}", style = flex(600, 90f, 10f, 13f), color = Color.White,
                modifier = Modifier.padding(top = 12.dp).background(Select, RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp).clearAndSetSemantics { })
        }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(Ink).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Lavender, modifier = Modifier.size(44.dp)) }
    }

    /** A slider as a design tool draws one: a thin track, a white knob ringed in black. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val y = size.height / 2
        drawLine(Ink.copy(alpha = 0.2f), Offset(0f, y), Offset(size.width, y), 2 * density, StrokeCap.Round)
        drawLine(Ink, Offset(0f, y), Offset(size.width * shown, y), 2 * density, StrokeCap.Round)
        val c = Offset(size.width * shown, y)
        drawCircle(Color.White, (9f + 3f * dragging) * density, c)
        drawCircle(Ink, (9f + 3f * dragging) * density, c, style = Stroke(2 * density))
    }

    /** The axes, live: how heavy and wide the music is making the type right now. */
    @Composable
    override fun NowPlayingDecor() {
        val (w, wd) = axes(breath())
        Box(Modifier.fillMaxWidth().padding(top = 50.dp), contentAlignment = Alignment.TopCenter) { AxisTag(w, wd) }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Column(modifier.background(Lavender).padding(10.dp)) {
            for ((w, wd) in listOf(200 to 60f, 600 to 100f, 1000 to 150f)) Text("Aa", style = flex(w, wd, 30f, 32f), color = Ink)
            Canvas(Modifier.padding(top = 6.dp).size(80.dp, 24.dp)) { figmaShape(0, Green) }
        }
    }
}
