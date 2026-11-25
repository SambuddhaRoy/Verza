package com.verza.ui.look

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
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
import androidx.compose.ui.text.font.FontFamily
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
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

/**
 * Anti-design, after LAX Space: no usual navigation, a reel of work that unwinds from a roll of tape,
 * two words at the ends of a line for a menu, huge counters. Here the rules are broken on purpose,
 * and the same way every time: page names in clashing faces letter by letter, buttons printed out of
 * register, shelves on a ribbon of tape, the tabs as words on a line, and the cover bleeding off the
 * page on Now Playing. It all still works: everything has its name and its 48dp.
 */
object AntiLook : Look("anti", "Anti-design", "Rules broken on purpose, a reel of tape") {
    private val Grey = Color(0xFFD9D9D5)
    private val Ink = Color(0xFF0F0F0F)
    private val Tape = Color(0xFFF7F7F3)
    /** The faces a page's name is cut from, a letter at a time. */
    private val Cut: List<Pair<FontFamily, FontWeight>> by lazy {
        listOf(Fonts.Fraunces to FontWeight.Black, Fonts.Anton to FontWeight.Normal, Fonts.SpaceMono to FontWeight.Bold, Fonts.Unbounded to FontWeight.Black, Fonts.Caveat to FontWeight.Bold, Fonts.ArchivoWide to FontWeight.Black)
    }

    // The accent is the cover's, searched against the grey.
    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Grey, ink = Ink, swatches = swatches, cell = Color(0xFFE9E9E5), fill = Ink.copy(alpha = 0.07f), line = Ink.copy(alpha = 0.45f))

    override val case = Case.Lower
    override fun cover(size: Dp): Shape = RectangleShape
    override val group: Shape = RectangleShape
    override val panel = androidx.compose.foundation.BorderStroke(1.dp, Ink)
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 56.dp + (if (playing) 64.dp else 0.dp) + 8.dp

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.Fraunces, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 52.sp),
        section = TextStyle(fontFamily = Fonts.SpaceMono, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
        title = TextStyle(fontFamily = Fonts.Fraunces, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 34.sp),
        headline = TextStyle(fontFamily = Fonts.SpaceMono, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
        rowTitle = TextStyle(fontFamily = Fonts.Archivo, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 12.sp, lineHeight = 17.sp),
        body = TextStyle(fontFamily = Fonts.Archivo, fontSize = 15.sp, lineHeight = 21.sp),
        small = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 11.sp, lineHeight = 15.sp),
        tab = TextStyle(fontFamily = Fonts.SpaceMono, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp),
        label = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 11.sp, lineHeight = 15.sp),
    )

    /** Grey paper with crop marks where nothing is to be cut, and a big × someone struck through it. */
    @Composable
    override fun Backdrop(modifier: Modifier) {
        Canvas(modifier.background(Grey)) {
            val rnd = java.util.Random(13)
            repeat(7) { cropMark(Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height), Ink.copy(alpha = 0.18f)) }
            val c = Offset(size.width * 0.78f, size.height * 0.58f); val r = size.width * 0.2f
            drawLine(Ink.copy(alpha = 0.06f), c - Offset(r, r), c + Offset(r, r), 2 * density)
            drawLine(Ink.copy(alpha = 0.06f), c + Offset(-r, r), c + Offset(r, -r), 2 * density)
        }
    }

    /** A printer's crop mark: a cross in a ring. */
    private fun DrawScope.cropMark(o: Offset, c: Color) {
        val a = 9 * density
        drawLine(c, o - Offset(a, 0f), o + Offset(a, 0f), density); drawLine(c, o - Offset(0f, a), o + Offset(0f, a), density)
        drawCircle(c, a * 0.55f, o, style = Stroke(density))
    }

    /** The name cut out letter by letter from clashing faces, each turned and dropped off the line; the time of day sideways. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        Row(modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dateline != null) Text(dateline.lowercase(), style = type.label, color = Ink, modifier = Modifier.vertical(clockwise = false).padding(end = 6.dp))
            Box(Modifier.weight(1f).padding(start = 6.dp).semantics(mergeDescendants = true) { heading(); contentDescription = title }) {
                CutUp(title.lowercase(), 50f)
            }
            trailing?.invoke()
        }
    }

    /** [text] with each letter from another face, size, turn and height: the same for the same words. */
    @Composable
    private fun CutUp(text: String, size: Float, colour: Color = Ink) {
        val rnd = java.util.Random(text.hashCode().toLong())
        Row(Modifier.clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
            for (ch in text) {
                val (face, weight) = Cut[rnd.nextInt(Cut.size)]
                val s = size * (0.72f + rnd.nextFloat() * 0.5f)
                val turn = (rnd.nextFloat() - 0.5f) * 22f
                val drop = (rnd.nextFloat() - 0.5f) * 14f
                val boxed = rnd.nextInt(7) == 0 && ch != ' '
                Text(
                    ch.toString(), style = TextStyle(fontFamily = face, fontWeight = weight, fontSize = s.sp, lineHeight = (s * 1.05f).sp), color = if (boxed) Grey else colour,
                    modifier = Modifier.graphicsLayer { rotationZ = turn; translationY = drop * density }.then(if (boxed) Modifier.background(colour).padding(horizontal = 2.dp) else Modifier),
                )
            }
        }
    }

    /** Lowercase mono, now on the left and now indented far in, a rule that runs on past it. */
    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        val right = text.hashCode() % 2 == 0
        Row(modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
            if (right) Box(Modifier.weight(1f).height(1.dp).background(Ink))
            Text(if (right) "  ↙ ${text.lowercase()}" else "${text.lowercase()} ↘  ", style = type.section, color = Ink)
            if (!right) Box(Modifier.weight(1f).height(1.dp).background(Ink))
        }
    }

    /** Printed out of register: the colour landed a few points off the outline it belongs in. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        val fill = if (primary) LocalPalette.current.accent.copy(alpha = 0.85f) else Ink.copy(alpha = 0.12f)
        // The colour is drawn from the button itself, a few points down and right, so it always matches its outline.
        Row(
            modifier.heightIn(min = 50.dp).drawBehind { translate(5.dp.toPx(), 5.dp.toPx()) { drawRect(fill) } }.border(1.5.dp, Ink)
                .clickable(enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) { Icon(icon, null, tint = Ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text("${text.lowercase()} ↗", style = type.headline, color = if (enabled) Ink else Ink.copy(alpha = 0.4f), maxLines = 1)
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Box(modifier.size(size + 5.dp).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = label }) {
            Box(Modifier.size(size).offset(5.dp, 5.dp).background(stain ?: Ink.copy(alpha = 0.15f)))
            Box(Modifier.size(size).background(Grey).border(1.5.dp, Ink), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Ink, modifier = Modifier.size(iconSize)) }
        }
    }

    /** A rule that stops short and starts again: a line nobody finished. */
    @Composable
    override fun Divider(start: Dp) {
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            val gap = size.width * 0.62f
            drawLine(Ink.copy(alpha = 0.45f), Offset(start.toPx(), 0f), Offset(gap, 0f), size.height)
            drawLine(Ink.copy(alpha = 0.45f), Offset(gap + 18 * density, 0f), Offset(size.width, 0f), size.height)
        }
    }

    /** The reel: a ribbon of tape waving across the page, each cover riding it, tipped to its slope. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        val w = 136.dp
        LazyRow(contentPadding = PaddingValues(start = 4.dp, end = 30.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                // Where the ribbon's middle sits, as a fraction of the box's height, at a point along the reel.
                fun wave(x: Float) = 0.45f + 0.16f * sin(x * 1.1f)
                val y = wave(i + 0.5f)
                val slope = Math.toDegrees(atan(0.16f * 1.1f * cos((i + 0.5f) * 1.1f) * 230f / 136f).toDouble()).toFloat()
                Box(
                    Modifier.width(w).height(230.dp).drawBehind {
                        // This item's stretch of tape, its edges following the wave.
                        val band = 128 * density
                        val path = Path()
                        val steps = 12
                        for (k in 0..steps) { val x = size.width * k / steps; val yy = size.height * wave(i + k.toFloat() / steps) - band / 2; if (k == 0) path.moveTo(x, yy) else path.lineTo(x, yy) }
                        for (k in steps downTo 0) { val x = size.width * k / steps; path.lineTo(x, size.height * wave(i + k.toFloat() / steps) + band / 2) }
                        path.close()
                        drawPath(path, Tape)
                        drawPath(path, Ink.copy(alpha = 0.25f), style = Stroke(density))
                    }.clickable(onClick = cardAction(card, onOpen)),
                ) {
                    Column(
                        Modifier.offset(y = 230.dp * y - 64.dp).padding(horizontal = 12.dp).graphicsLayer { rotationZ = slope },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Cover(card.art?.let { artAt(it, 544) }, 96.dp)
                        Text("%02d".format(i + 1) + " ${cardTitle(card).lowercase()}", style = type.label, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }

    /** A black slab with a huge counter: which song of the queue this is, of how many. */
    @Composable
    override fun HomeHero() {
        val now by LocalContext.current.graph.player.state.collectAsState()
        val at = if (now.song != null) now.index + 1 else 0
        Row(
            Modifier.padding(start = 40.dp, end = 14.dp, top = 6.dp).graphicsLayer { rotationZ = -1.2f }.fillMaxWidth().background(Ink).padding(horizontal = 14.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { contentDescription = if (at == 0) "Nothing playing" else "Song $at of ${now.queue.size}" },
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("%02d".format(at), style = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Light, fontSize = 88.sp, lineHeight = 84.sp, letterSpacing = (-0.06).em), color = Grey)
            Text("  %02d".format(now.queue.size), style = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 44.sp), color = Grey.copy(alpha = 0.5f))
            Spacer(Modifier.weight(1f))
            Text(now.song?.let { com.verza.data.shownTitle(it.title, it.artist).lowercase() } ?: "nothing yet", style = type.label, color = Grey, maxLines = 2, modifier = Modifier.width(110.dp).padding(bottom = 10.dp))
        }
    }

    /** No tab bar: the places as words strung along one line, the one you're at in a black slab. The song above, off-kilter. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().takesTouches().background(Grey).navigationBarsPadding()) {
            state.downloads?.let { DownloadsLine(it.lowercase(), Ink) }
            val accent = LocalPalette.current.accent
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.padding(start = 30.dp, end = 10.dp, top = 6.dp).graphicsLayer { rotationZ = -1.5f }.fillMaxWidth().height(56.dp).background(Ink)
                        .drawBehind { drawRect(accent, Offset(0f, size.height - 3 * density), Size(size.width * played, 3 * density)) }
                        .clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 6.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 44.dp)
                    Text(
                        "${com.verza.data.shownTitle(song.title, song.artist)} / ${song.artist}".lowercase(), style = type.label, color = Grey, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                    )
                    PlayPauseNext(state.now, Grey, 26.dp)
                }
            }
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                state.tabs.forEachIndexed { i, tab ->
                    val on = tab.route == state.current
                    Text(
                        tab.label.lowercase(), style = type.tab, color = if (on) Grey else Ink, maxLines = 1,
                        modifier = Modifier.heightIn(min = 48.dp).wrapContentHeight().clickable { state.go(tab.route) }
                            .semantics { role = Role.Tab; selected = on }.background(if (on) Ink else Color.Transparent).padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                    // Lines of different lengths between them, as DESIGN ——— CODE has one.
                    if (i < state.tabs.lastIndex) Box(Modifier.weight(listOf(2f, 0.6f, 1.4f)[i % 3]).height(1.5.dp).background(Ink))
                }
            }
        }
    }

    override fun nowPlaying(page: Palette) = page

    /** Grey, crop marks, and the counter again, enormous and outlined, behind everything. */
    @Composable
    override fun NowPlayingBackground(art: String?) {
        val now by LocalContext.current.graph.player.state.collectAsState()
        Box(Modifier.fillMaxSize().background(Grey)) {
            Canvas(Modifier.fillMaxSize()) {
                val rnd = java.util.Random(5)
                repeat(6) { cropMark(Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height), Ink.copy(alpha = 0.22f)) }
            }
            Text(
                "%02d".format(now.index + 1), style = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Black, fontSize = 260.sp, lineHeight = 240.sp, letterSpacing = (-0.08).em, drawStyle = Stroke(2f)),
                color = Ink.copy(alpha = 0.2f), modifier = Modifier.align(Alignment.TopEnd).offset(x = 30.dp, y = 30.dp).clearAndSetSemantics { },
            )
        }
    }

    /** The cover too big for its place, run off the left edge, tipped, its colour printed off register behind. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        Box(Modifier.offset(x = -side * 0.14f).graphicsLayer { scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX; rotationZ = 2.5f; translationY = -fly * size.height * 2f; alpha = 1f - fly }) {
            Box(Modifier.size(side * 0.98f).offset(12.dp, 12.dp).background(LocalPalette.current.accent.copy(alpha = 0.8f)))
            AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(side * 0.98f).border(2.dp, Ink))
            Text("now", style = type.masthead.copy(fontSize = 40.sp), color = Grey, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 20.dp, y = 14.dp).background(Ink).padding(horizontal = 8.dp).clearAndSetSemantics { })
        }
    }

    /** A black diamond with the symbol set upright in it. */
    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Box(Modifier.size(90.dp).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(64.dp).graphicsLayer { rotationZ = 45f }.background(Ink))
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Grey, modifier = Modifier.size(38.dp))
        }
    }

    /** A line, heavier where it's been, and an × where it is. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val y = size.height / 2
        drawLine(Ink.copy(alpha = 0.4f), Offset(0f, y), Offset(size.width, y), 1.5f * density)
        drawLine(Ink, Offset(0f, y), Offset(size.width * shown, y), 4f * density)
        val x = size.width * shown; val a = (8f + 3f * dragging) * density
        drawLine(Ink, Offset(x - a, y - a), Offset(x + a, y + a), 2.5f * density)
        drawLine(Ink, Offset(x - a, y + a), Offset(x + a, y - a), 2.5f * density)
    }

    @Composable
    override fun NowPlayingDecor() {
        Box(Modifier.fillMaxSize().padding(top = 70.dp, end = 4.dp), contentAlignment = Alignment.TopEnd) {
            Text("now playing — now playing", style = type.label.copy(fontSize = 12.sp), color = Ink, modifier = Modifier.vertical().clearAndSetSemantics { })
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Box(modifier.background(Grey)) {
            Canvas(Modifier.fillMaxSize()) { cropMark(Offset(size.width * 0.8f, size.height * 0.2f), Ink.copy(alpha = 0.5f)) }
            Box(Modifier.padding(10.dp)) { CutUp("anti", 30f) }
            Text("design ——— code", style = type.label, color = Ink, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
        }
    }
}
