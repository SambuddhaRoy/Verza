package com.verza.ui.look

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.kit.Cover
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.sin

/**
 * Hand-drawn, after Flying Papers (a mascot on every part of the page, peeking over the words,
 * heavy words in a few loud colours) and GM Meme (everything inked by hand: wobbly outlines, cards
 * with thick coloured offset borders, doodled sparkles, highlighter). Verza's mascot is its own:
 * Spin, a record with rubber-hose arms, who waves on Home and dances to the bass on Now Playing.
 */
object SketchLook : Look("sketch", "Hand-drawn", "Inked by hand, with Spin the record") {
    private val Paper = Color(0xFFFFFCF2)
    private val Ink = Color(0xFF161616)
    private val Marker = Color(0xFFFFE34A)
    private val Stickers = listOf(Color(0xFFFFE34A), Color(0xFFFF9CC2), Color(0xFF9BE564), Color(0xFF7FB2FF))
    private val Blue = Color(0xFF2B63F0)

    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Paper, ink = Ink, accent = Blue, cell = Color.White, fill = Color(0xFFFFF4C2), line = Ink.copy(alpha = 0.3f), inkMuted = Color(0xFF55524A))

    override fun cover(size: Dp): Shape = WobblyShape((size.value * 7).toInt(), if (size in 1.dp..64.dp) 7.dp else 12.dp, 1.6.dp)
    override val group: Shape = WobblyShape(41, 18.dp, 2.dp)
    override val panel = androidx.compose.foundation.BorderStroke(2.dp, Ink)
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 10.dp + 66.dp + (if (playing) 12.dp + 64.dp else 0.dp) + 12.dp

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, lineHeight = 54.sp, letterSpacing = (-0.02).em),
        section = TextStyle(fontFamily = Fonts.Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 31.sp, letterSpacing = (-0.01).em),
        title = TextStyle(fontFamily = Fonts.Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 36.sp),
        headline = TextStyle(fontFamily = Fonts.BricolageText, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp),
        rowTitle = TextStyle(fontFamily = Fonts.BricolageText, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.Caveat, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 20.sp),
        body = TextStyle(fontFamily = Fonts.BricolageText, fontSize = 15.sp, lineHeight = 21.sp),
        small = TextStyle(fontFamily = Fonts.BricolageText, fontSize = 13.sp, lineHeight = 17.sp),
        tab = TextStyle(fontFamily = Fonts.Caveat, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 18.sp),
        label = TextStyle(fontFamily = Fonts.Caveat, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp),
    )

    /** Paper with clouds, stars and squiggles doodled in pencil here and there. */
    @Composable
    override fun Backdrop(modifier: Modifier) {
        Canvas(modifier.background(Paper)) { doodles(Ink.copy(alpha = 0.13f), 7) }
    }

    private fun DrawScope.doodles(c: Color, seed: Int) {
        val rnd = java.util.Random(seed.toLong())
        val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
        repeat(9) { i ->
            val x = rnd.nextFloat() * size.width; val y = rnd.nextFloat() * size.height
            when (i % 3) {
                0 -> { // A cloud: three bumps on a line.
                    val r = (14 + rnd.nextInt(10)).dp.toPx()
                    val p = Path().apply {
                        moveTo(x, y)
                        arcTo(Rect(x, y - r, x + r * 1.4f, y + r * 0.4f), 180f, 180f, false)
                        arcTo(Rect(x + r * 1.2f, y - r * 1.5f, x + r * 2.8f, y + r * 0.3f), 180f, 180f, false)
                        arcTo(Rect(x + r * 2.6f, y - r * 0.8f, x + r * 3.8f, y + r * 0.4f), 180f, 180f, false)
                        lineTo(x, y)
                    }
                    drawPath(p, c, style = stroke)
                }
                1 -> sparkle(Offset(x, y), (8 + rnd.nextInt(6)).dp.toPx(), c, stroke.width)
                else -> { // A squiggle.
                    val p = Path().apply { moveTo(x, y); for (k in 1..6) quadraticTo(x + k * 12.dp.toPx() - 6.dp.toPx(), y + (if (k % 2 == 0) -8 else 8).dp.toPx(), x + k * 12.dp.toPx(), y) }
                    drawPath(p, c, style = stroke)
                }
            }
        }
    }

    /** A four-pointed sparkle, drawn as two crossed strokes curving in. */
    private fun DrawScope.sparkle(o: Offset, r: Float, c: Color, w: Float) {
        val p = Path().apply {
            moveTo(o.x, o.y - r); quadraticTo(o.x, o.y, o.x + r, o.y); quadraticTo(o.x, o.y, o.x, o.y + r)
            quadraticTo(o.x, o.y, o.x - r, o.y); quadraticTo(o.x, o.y, o.x, o.y - r)
        }
        drawPath(p, c, style = Stroke(w, cap = StrokeCap.Round))
    }

    /** The name, heavy, highlighted in yellow marker; the dateline in handwriting with an arrow at it. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (dateline != null) Text("$dateline ↘", style = type.label, color = Ink.copy(alpha = 0.75f))
                Text(case.of(title), style = type.masthead, color = Ink, modifier = Modifier.highlighter(Marker).semantics { heading() })
            }
            trailing?.invoke()
        }
    }

    /** A stroke of marker across the lower half of the words, tipped a little. */
    private fun Modifier.highlighter(c: Color) = drawBehind {
        rotate(-1.5f) { drawRoundRect(c, Offset(-6.dp.toPx(), size.height * 0.45f), Size(size.width + 12.dp.toPx(), size.height * 0.42f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())) }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        Column(modifier.padding(top = 28.dp, bottom = 12.dp).semantics { heading() }) {
            Text(case.of(text), style = type.section, color = Ink)
            Canvas(Modifier.width(120.dp).height(10.dp)) { squiggle(Blue, 2.5.dp.toPx()) }
        }
    }

    /** A wavy underline, as a pen dashes one off. */
    private fun DrawScope.squiggle(c: Color, w: Float) {
        val p = Path().apply {
            moveTo(0f, size.height / 2)
            var x = 0f; var up = true
            while (x < size.width) { val nx = x + size.height * 1.4f; quadraticTo((x + nx) / 2, if (up) 0f else size.height, nx, size.height / 2); x = nx; up = !up }
        }
        drawPath(p, c, style = Stroke(w, cap = StrokeCap.Round))
    }

    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        Pushable(WobblyShape(text.hashCode(), 22.dp), if (primary) Marker else Color.White, Ink, Ink, modifier.heightIn(min = 52.dp), depth = 4.dp, border = 2.5.dp, enabled = enabled, onClick = onClick) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = Ink, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)) }
                Text(case.of(text), style = type.headline, color = if (enabled) Ink else Ink.copy(alpha = 0.4f), maxLines = 1)
            }
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Pushable(WobblyShape(label.hashCode(), 22.dp, 1.8.dp), if (stain != null) Marker else Color.White, Ink, Ink, modifier.size(size + 4.dp), depth = 3.dp, label = label, onClick = onClick) {
            Icon(icon, null, tint = Ink, modifier = Modifier.size(iconSize))
        }
    }

    /** A line drawn freehand: it wanders a little. */
    @Composable
    override fun Divider(start: Dp) {
        Canvas(Modifier.fillMaxWidth().height(6.dp)) {
            val p = Path().apply {
                moveTo(start.toPx(), size.height / 2)
                var x = start.toPx()
                while (x < size.width) { x += 40.dp.toPx(); quadraticTo(x - 20.dp.toPx(), size.height / 2 + sin(x) * 2.5f, x, size.height / 2) }
            }
            drawPath(p, Ink.copy(alpha = 0.3f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        }
    }

    /** Stickers: a white card inked round, on a fat offset border of colour, a sparkle doodled on some. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        LazyRow(contentPadding = PaddingValues(start = 20.dp, end = 26.dp, top = 8.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                val shape = WobblyShape(i * 13 + 5, 16.dp)
                Box {
                    Column(
                        Modifier.width(150.dp).hardShadow(shape, Stickers[i % Stickers.size], 6.dp, 6.dp).background(Color.White, shape).border(2.5.dp, Ink, shape)
                            .clip(shape).clickable(onClick = cardAction(card, onOpen)).padding(9.dp),
                    ) {
                        Cover(card.art?.let { artAt(it, 544) }, 132.dp, Modifier.border(2.dp, Ink, cover(132.dp)))
                        Text(cardTitle(card), style = type.headline.copy(fontSize = 14.sp, lineHeight = 18.sp), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        Text(card.subtitle.substringBefore(" • "), style = type.rowSubtitle.copy(fontSize = 16.sp), color = Ink.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (i % 3 == 1) Canvas(Modifier.size(26.dp).align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp)) { sparkle(center, size.minDimension / 2, Ink, 2.dp.toPx()) }
                }
            }
        }
    }

    /** Spin waves from beside a speech bubble about what's on. */
    @Composable
    override fun HomeHero() {
        val now by LocalContext.current.graph.player.state.collectAsState()
        val say = now.song?.let { "ooh, ${com.verza.data.shownTitle(it.title, it.artist)}! good pick" } ?: "hi! what are we playing today?"
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Spin(110.dp, dance = false)
            Box(Modifier.padding(start = 4.dp).weight(1f).bubble().padding(start = 22.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
                Text(say, style = type.label, color = Ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    /** A speech bubble inked by hand, its tail pointing left to Spin. */
    private fun Modifier.bubble() = drawBehind {
        val shape = WobblyShape(77, 18.dp, 2.dp).createOutline(Size(size.width - 12.dp.toPx(), size.height), layoutDirection, this)
        val body = Path().apply { addOutline(shape); translate(Offset(12.dp.toPx(), 0f)) }
        val tail = Path().apply {
            moveTo(14.dp.toPx(), size.height * 0.45f); lineTo(0f, size.height * 0.62f); lineTo(16.dp.toPx(), size.height * 0.66f)
        }
        drawPath(body, Color.White); drawPath(tail, Color.White)
        drawPath(body, Ink, style = Stroke(2.5.dp.toPx()))
        drawPath(tail, Ink, style = Stroke(2.5.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }

    /** Paper with a freehand line over it; the chosen tab gets a loop scribbled round it. The song on a yellow sticker. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.downloads?.let { DownloadsLine(it, Ink) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                val shape = WobblyShape(99, 20.dp)
                Row(
                    Modifier.fillMaxWidth().height(64.dp).takesTouches().hardShadow(shape, Ink, 4.dp, 4.dp).background(Marker, shape).border(2.5.dp, Ink, shape).clip(shape)
                        .drawWithContentBar(played, Ink).clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 44.dp, Modifier.border(2.dp, Ink, cover(44.dp)))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist), style = type.headline, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = type.rowSubtitle.copy(fontSize = 16.sp), color = Ink.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlayPauseNext(state.now, Ink, 28.dp)
                }
            }
            val bar = WobblyShape(55, 24.dp)
            Row(
                Modifier.fillMaxWidth().height(66.dp).takesTouches().hardShadow(bar, Ink, 3.dp, 3.dp).background(Color.White, bar).border(2.5.dp, Ink, bar).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((i, tab) in state.tabs.withIndex()) {
                    val on = tab.route == state.current
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable { state.go(tab.route) }.semantics(mergeDescendants = true) { role = Role.Tab; selected = on }
                            .then(if (on) Modifier.drawBehind { loop(Blue, i) } else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(tab.icon, null, tint = Ink, modifier = Modifier.size(21.dp))
                        Text(tab.label.lowercase(), style = type.tab, color = Ink, maxLines = 1)
                    }
                }
            }
        }
    }

    /** A loop scribbled round something, twice round and not quite meeting, as a pen circles a word. */
    private fun DrawScope.loop(c: Color, seed: Int) {
        val rnd = java.util.Random(seed.toLong() + 3)
        for (k in 0..1) {
            val inset = (4 + k * 3).dp.toPx() + rnd.nextFloat() * 2
            val r = Rect(inset, inset * 0.8f, size.width - inset, size.height - inset * 0.8f)
            val p = Path().apply { addArc(r, 200f + k * 20f + rnd.nextFloat() * 10f, 330f) }
            drawPath(p, c, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }

    // Now Playing stays on paper, with more doodles.
    override fun nowPlaying(page: Palette) = page

    @Composable
    override fun NowPlayingBackground(art: String?) {
        Canvas(Modifier.fillMaxSize().background(Paper)) { doodles(Ink.copy(alpha = 0.16f), 21) }
    }

    /** The cover as a print taped in at an angle: an inked wobbly frame, two strips of tape, a note in pen. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        val shape = WobblyShape(1234, 14.dp, 2.4.dp)
        Box(Modifier.graphicsLayer { scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX; rotationZ = -3f; translationY = -fly * size.height * 2f; alpha = 1f - fly }) {
            Box(Modifier.size(side * 0.84f).hardShadow(shape, Stickers[3], 8.dp, 8.dp).background(Color.White, shape).border(3.dp, Ink, shape).padding(10.dp)) {
                AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(WobblyShape(4321, 10.dp, 1.8.dp)))
            }
            for ((x, turn) in listOf(-0.04f to -32f, 0.7f to 28f)) Box(
                Modifier.offset(x = side * x, y = (-10).dp).graphicsLayer { rotationZ = turn }.size(side * 0.26f, 26.dp).background(Marker.copy(alpha = 0.72f)),
            )
            Text("now spinning ↓", style = type.label, color = Ink, modifier = Modifier.offset(x = side * 0.5f, y = side * 0.86f).graphicsLayer { rotationZ = 4f }.clearAndSetSemantics { })
            // Spin dances at the cover's foot, half on it, to the music.
            Spin(side * 0.36f, dance = true, Modifier.align(Alignment.BottomStart).offset(x = -side * 0.12f, y = side * 0.14f))
        }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Pushable(WobblyShape(808, 46.dp, 2.dp), Marker, Ink, Ink, Modifier.size(90.dp), depth = 5.dp, border = 3.dp, label = if (playing) "Pause" else "Play", onClick = onClick) {
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Ink, modifier = Modifier.size(48.dp))
        }
    }

    /** A line drawn by hand, the played part heavier and blue, a star doodled where it's at. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val y = size.height / 2
        fun line(to: Float, w: Float, c: Color) {
            val path = Path().apply { moveTo(0f, y); var x = 0f; while (x < to) { val nx = minOf(x + 24 * density, to); quadraticTo((x + nx) / 2, y + sin(x / 9f) * 3 * density, nx, y); x = nx } }
            drawPath(path, c, style = Stroke(w, cap = StrokeCap.Round))
        }
        line(size.width, 2f * density, Ink.copy(alpha = 0.3f))
        line(size.width * shown, (4f + 2f * dragging) * density, Blue)
        sparkle(Offset(size.width * shown, y), (11f + 3f * dragging) * density, Ink, 2.5f * density)
    }

    /**
     * Spin: a record with a yellow label for a face, eyes over it, rubber-hose arms in white gloves,
     * sneakers. Waving, or dancing (a bounce, swinging arms) when [dance].
     */
    @Composable
    fun Spin(size: Dp, dance: Boolean, modifier: Modifier = Modifier) {
        val t by rememberInfiniteTransition(label = "spin").animateFloat(0f, 1f, infiniteRepeatable(tween(if (dance) 760 else 1600, easing = LinearEasing), RepeatMode.Restart), label = "t")
        val beat = sin(t * 2 * PI).toFloat()
        Canvas(modifier.size(size).clearAndSetSemantics { }) {
            val s = this.size.minDimension
            val lift = if (dance) -kotlin.math.abs(beat) * s * 0.06f else 0f
            val c = Offset(s * 0.5f, s * 0.44f + lift)
            val stroke = Stroke(s * 0.035f, cap = StrokeCap.Round)
            // Legs and sneakers.
            for (side in listOf(-1f, 1f)) {
                val hip = Offset(c.x + side * s * 0.1f, c.y + s * 0.3f)
                val foot = Offset(c.x + side * s * (0.16f + if (dance) beat * side * 0.04f else 0f), s * 0.93f)
                drawPath(Path().apply { moveTo(hip.x, hip.y); quadraticTo(hip.x + side * s * 0.06f, (hip.y + foot.y) / 2, foot.x, foot.y) }, Ink, style = stroke)
                drawOval(Ink, Offset(foot.x - s * 0.08f + side * s * 0.03f, foot.y - s * 0.03f), Size(s * 0.16f, s * 0.07f))
            }
            // Arms: one waves (or both swing, dancing), white gloves at their ends.
            for (side in listOf(-1f, 1f)) {
                val shoulder = Offset(c.x + side * s * 0.33f, c.y + s * 0.04f)
                val swing = if (dance) beat * side else if (side < 0) beat else 0f
                val hand = Offset(shoulder.x + side * s * 0.12f, shoulder.y - s * (0.16f + 0.1f * swing) + (if (side > 0 && !dance) s * 0.24f else 0f))
                drawPath(Path().apply { moveTo(shoulder.x, shoulder.y); quadraticTo(shoulder.x + side * s * 0.16f, (shoulder.y + hand.y) / 2, hand.x, hand.y) }, Ink, style = stroke)
                drawCircle(Color.White, s * 0.055f, hand); drawCircle(Ink, s * 0.055f, hand, style = Stroke(s * 0.02f))
            }
            // The record, its grooves, its label.
            drawCircle(Color(0xFF1B1B1B), s * 0.34f, c)
            for (k in 1..3) drawCircle(Color(0xFF3A3A3A), s * (0.34f - k * 0.055f), c, style = Stroke(s * 0.008f))
            drawCircle(Marker, s * 0.15f, c)
            drawCircle(Ink, s * 0.15f, c, style = Stroke(s * 0.012f))
            // Eyes over the label, and a grin on it.
            for (side in listOf(-1f, 1f)) {
                val e = Offset(c.x + side * s * 0.075f, c.y - s * 0.06f)
                drawOval(Color.White, Offset(e.x - s * 0.04f, e.y - s * 0.055f), Size(s * 0.08f, s * 0.11f))
                drawOval(Ink, Offset(e.x - s * 0.04f, e.y - s * 0.055f), Size(s * 0.08f, s * 0.11f), style = Stroke(s * 0.012f))
                drawCircle(Ink, s * 0.022f, Offset(e.x + s * 0.008f, e.y + s * 0.012f))
            }
            drawArc(Ink, 20f, 140f, false, Offset(c.x - s * 0.07f, c.y - s * 0.02f), Size(s * 0.14f, s * 0.1f), style = Stroke(s * 0.018f, cap = StrokeCap.Round))
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Box(modifier.background(Paper)) {
            Canvas(Modifier.fillMaxSize()) { doodles(Ink.copy(alpha = 0.18f), 3) }
            Text("Hi!", style = type.masthead.copy(fontSize = 34.sp, lineHeight = 36.sp), color = Ink, modifier = Modifier.padding(12.dp).highlighter(Marker))
            Spin(86.dp, dance = false, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
        }
    }
}
