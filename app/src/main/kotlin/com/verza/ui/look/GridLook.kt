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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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

/**
 * Modular design, after ChainGPT Labs: a light grey page ruled into cells, every block of content
 * sitting in one, small mono labels with a dot before them, a squared display face, and one orange
 * for the thing to press. Clear, structured, and still all interactive.
 */
object GridLook : Look("grid", "Modular grid", "Ruled cells, mono labels, one orange") {
    val Orange = Color(0xFFFF4D12)
    private val Paper = Color(0xFFE9E9E6)
    private val Cell = Color(0xFFF6F6F3)
    private val Ink = Color(0xFF141414)
    private val Rule = Color(0xFFC6C6C0)

    // The orange is for fills, with black on it; words in the accent use a darker orange that reads on the grey.
    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Paper, ink = Ink, accent = Color(0xFFB23200), cell = Cell, surface = Cell, fill = Cell, line = Rule, inkMuted = Color(0xFF5E5E58))

    override val case = Case.Upper
    override fun cover(size: Dp): Shape = RectangleShape
    override val group: Shape = RectangleShape
    override val panel = androidx.compose.foundation.BorderStroke(1.dp, Rule)
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 62.dp + (if (playing) 58.dp else 0.dp)

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.ChakraPetch, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-0.01).em),
        section = TextStyle(fontFamily = Fonts.JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.06.em),
        title = TextStyle(fontFamily = Fonts.ChakraPetch, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 30.sp),
        headline = TextStyle(fontFamily = Fonts.JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.04.em),
        rowTitle = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.JetBrainsMono, fontSize = 12.sp, lineHeight = 17.sp),
        body = TextStyle(fontFamily = Fonts.InterText, fontSize = 15.sp, lineHeight = 21.sp),
        small = TextStyle(fontFamily = Fonts.JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
        tab = TextStyle(fontFamily = Fonts.JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.06.em),
        label = TextStyle(fontFamily = Fonts.JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.05.em),
    )

    /** Four columns ruled down the page, and a cross where every rule meets a row. */
    @Composable
    override fun Backdrop(modifier: Modifier) {
        Canvas(modifier) {
            ruled(Rule.copy(alpha = 0.6f), 4)
            val row = 180.dp.toPx(); val a = 5.dp.toPx(); var y = row
            while (y < size.height) {
                for (i in 1 until 4) { val x = size.width * i / 4; drawLine(Ink.copy(alpha = 0.35f), Offset(x - a, y), Offset(x + a, y), density) }
                y += row
            }
        }
    }

    /** The page's name in a cell: a mono label and index over the squared title, ruled underneath. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).background(Cell).border(1.dp, Rule)) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Rule, Offset(0f, size.height), Offset(size.width, size.height), density) }.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("● ${(dateline ?: "Verza").uppercase()}", style = type.label, color = Ink, modifier = Modifier.weight(1f))
                if (trailing != null) Box(Modifier.drawBehind { drawLine(Rule, Offset(0f, 0f), Offset(0f, size.height), density) }) { trailing() }
                else Text("[VRZ]", style = type.label, color = Ink.copy(alpha = 0.55f), modifier = Modifier.padding(12.dp))
            }
            Text(title.uppercase(), style = type.masthead, color = Ink, modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 14.dp).semantics { heading() })
        }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        Row(modifier.padding(top = 26.dp, bottom = 10.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(Orange))
            Text("  ${text.uppercase()}", style = type.section, color = Ink)
            Box(Modifier.weight(1f).padding(start = 10.dp).height(1.dp).background(Rule))
        }
    }

    /** A rectangle: orange with black words for the one to press, a ruled cell for the rest. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        Row(
            modifier.heightIn(min = 48.dp).background(if (primary) Orange else Cell).border(1.dp, if (primary) Orange else Rule)
                .clickable(enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = Ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text.uppercase(), style = type.headline, color = if (enabled) Ink else Ink.copy(alpha = 0.4f), maxLines = 1)
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Box(
            modifier.size(size).background(if (stain != null) Orange else Cell).border(1.dp, Rule).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Ink, modifier = Modifier.size(iconSize * 0.9f)) }
    }

    @Composable
    override fun Divider(start: Dp) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
    }

    /** Cells side by side, their rules shared: a cover set in each, a mono caption strip under it, numbered. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy((-1).dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                Column(Modifier.width(156.dp).background(Cell).border(1.dp, Rule).clickable(onClick = cardAction(card, onOpen))) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Text("%02d".format(i + 1), style = type.label, color = Color(0xFFB23200))
                        Spacer(Modifier.weight(1f))
                        Text("↗", style = type.label, color = Ink)
                    }
                    Box(Modifier.padding(horizontal = 10.dp)) { Cover(card.art?.let { artAt(it, 544) }, 136.dp) }
                    Column(Modifier.fillMaxWidth().padding(top = 10.dp).drawBehind { drawLine(Rule, Offset.Zero, Offset(size.width, 0f), density) }.padding(10.dp)) {
                        Text(cardTitle(card).uppercase(), style = type.headline.copy(fontSize = 12.sp, lineHeight = 15.sp), color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(card.subtitle.substringBefore(" • "), style = type.small, color = Ink.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    /** Three cells of figures from the play log, as the stat cells of a dashboard. */
    @Composable
    override fun HomeHero() {
        val l = rememberListening()
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).background(Cell).border(1.dp, Rule)) {
            for ((i, f) in listOf("PLAYS" to "${l.plays}", "HOURS" to "${l.minutes / 60}", "DAYS IN A ROW" to "${l.streak}").withIndex()) {
                Column(
                    Modifier.weight(1f).then(if (i > 0) Modifier.drawBehind { drawLine(Rule, Offset.Zero, Offset(0f, size.height), density) } else Modifier).padding(12.dp),
                ) {
                    Text(f.first, style = type.label.copy(fontSize = 10.sp), color = Ink.copy(alpha = 0.6f), maxLines = 1)
                    Text(f.second, style = type.title, color = if (i == 0) Color(0xFFB23200) else Ink)
                }
            }
        }
    }

    /** Equal cells across the foot, ruled apart; the chosen one dark with an orange mark. The song on a row of cells above. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().background(Cell).takesTouches().navigationBarsPadding()) {
            state.downloads?.let { DownloadsLine(it.uppercase(), Ink) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                val player = LocalContext.current.graph.player
                Row(Modifier.fillMaxWidth().height(58.dp).drawBehind {
                    drawLine(Rule, Offset.Zero, Offset(size.width, 0f), density)
                    drawRect(Orange, size = Size(size.width * played, 3.dp.toPx()))
                }) {
                    Row(Modifier.weight(1f).fillMaxHeight().clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Cover(song.art?.let { artAt(it, 226) }, 38.dp)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(com.verza.data.shownTitle(song.title, song.artist).uppercase(), style = type.headline.copy(fontSize = 12.sp), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = type.small, color = Ink.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Box(Modifier.width(58.dp).fillMaxHeight().background(Orange).clickable(onClick = player::toggle).semantics { role = Role.Button; contentDescription = if (state.now.playing) "Pause" else "Play" }, contentAlignment = Alignment.Center) {
                        Icon(if (state.now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Ink)
                    }
                    Box(Modifier.width(58.dp).fillMaxHeight().drawBehind { drawLine(Rule, Offset.Zero, Offset(0f, size.height), density) }.clickable(onClick = player::next).semantics { role = Role.Button; contentDescription = "Next" }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.SkipNext, null, tint = Ink)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(62.dp).drawBehind { drawLine(Ink, Offset.Zero, Offset(size.width, 0f), density) }) {
                state.tabs.forEachIndexed { i, tab ->
                    val on = tab.route == state.current
                    Column(
                        Modifier.weight(1f).fillMaxHeight().background(if (on) Ink else Cell)
                            .drawBehind {
                                if (i > 0) drawLine(Rule, Offset.Zero, Offset(0f, size.height), density)
                                if (on) drawRect(Orange, size = Size(size.width, 3.dp.toPx()))
                            }
                            .clickable { state.go(tab.route) }.semantics(mergeDescendants = true) { role = Role.Tab; selected = on },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(tab.icon, null, tint = if (on) Cell else Ink, modifier = Modifier.size(20.dp))
                        Text(tab.label.substringAfterLast(' ').uppercase(), style = type.tab, color = if (on) Cell else Ink, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }

    // Now Playing stays on the ruled grey.
    override fun nowPlaying(page: Palette) = page

    @Composable
    override fun NowPlayingBackground(art: String?) {
        Canvas(Modifier.fillMaxSize().background(Paper)) {
            ruled(Rule.copy(alpha = 0.7f), 4, 120.dp)
            crosshairs(Ink.copy(alpha = 0.25f), (size.width / 4 / density).dp, 5.dp)
        }
    }

    /** The cover in a cell, a mono strip over it, the cell's corners bracketed. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        Column(
            Modifier.graphicsLayer { scaleX = rest; scaleY = rest; translationY = -fly * size.height * 2f; alpha = 1f - fly }.width(side * 0.9f).background(Cell).border(1.dp, Rule)
                .drawBehind {
                    val m = 16.dp.toPx(); val w = 2.dp.toPx()
                    for ((x, y, dx, dy) in listOf(Quad(0f, 0f, 1f, 1f), Quad(size.width, 0f, -1f, 1f), Quad(0f, size.height, 1f, -1f), Quad(size.width, size.height, -1f, -1f))) {
                        drawLine(Orange, Offset(x, y), Offset(x + dx * m, y), w); drawLine(Orange, Offset(x, y), Offset(x, y + dy * m), w)
                    }
                },
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("● NOW PLAYING", style = type.label, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("[01]", style = type.label, color = Ink.copy(alpha = 0.55f))
            }
            AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth().height(side * 0.9f - 24.dp))
        }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Box(
            Modifier.size(84.dp).background(Orange).border(1.dp, Ink).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Ink, modifier = Modifier.size(44.dp)) }
    }

    /** Forty little cells, filled one by one as the song plays. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val n = 40
        val gap = 2 * density
        val w = (size.width - gap * (n - 1)) / n
        val h = (12f + 6f * dragging) * density
        val top = size.height / 2 - h / 2
        for (i in 0 until n) {
            val x = i * (w + gap)
            val filled = (i + 0.5f) / n <= shown
            if (filled) drawRect(Ink, Offset(x, top), Size(w, h)) else drawRect(Rule, Offset(x, top), Size(w, h), style = Stroke(density))
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Box(modifier.background(Paper)) {
            Canvas(Modifier.fillMaxSize()) { ruled(Rule, 3, 44.dp) }
            Column(Modifier.padding(10.dp)) {
                Text("● GRID", style = type.label, color = Ink)
                Text("MOD", style = type.masthead.copy(fontSize = 34.sp, lineHeight = 36.sp), color = Ink, modifier = Modifier.padding(top = 50.dp))
                Box(Modifier.padding(top = 8.dp).size(width = 70.dp, height = 24.dp).background(Orange))
            }
        }
    }
}

private data class Quad(val x: Float, val y: Float, val dx: Float, val dy: Float)
