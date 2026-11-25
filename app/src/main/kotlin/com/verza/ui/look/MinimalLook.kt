package com.verza.ui.look

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
 * Minimalism, after PORTAL-1 (thin line art, arches running into the distance, a wide heavy grotesk,
 * a ticker strip of small mono words) and Olha Lazarieva's portfolio (black and white, bracketed
 * navigation, mono captions, photographs in grey). Doing more with less: no colour at all on the
 * pages, covers in grey, and colour only where the music is, on Now Playing.
 */
object MinimalLook : Look("minimal", "Minimalism", "Black and white, thin lines, room to breathe") {
    private val Grey = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

    override fun palette(swatches: List<Color>, dark: Boolean): Palette {
        val canvas = if (dark) Color(0xFF0B0B0B) else Color(0xFFF3F3F1)
        val ink = if (dark) Color(0xFFF2F2F0) else Color(0xFF0E0E0E)
        return Palette.make(canvas = canvas, ink = ink, accent = ink, fill = ink.copy(alpha = 0.06f), line = ink.copy(alpha = 0.2f), cell = canvas, surface = canvas)
    }

    override val case = Case.Upper
    override fun cover(size: Dp): Shape = RectangleShape
    override val group: Shape = RectangleShape
    override val coverFilter = Grey
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 54.dp + (if (playing) 58.dp else 0.dp)

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.ArchivoWide, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = (-0.01).em),
        section = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
        title = TextStyle(fontFamily = Fonts.ArchivoWide, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 28.sp),
        headline = TextStyle(fontFamily = Fonts.SpaceMono, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.03.em),
        rowTitle = TextStyle(fontFamily = Fonts.Archivo, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 12.sp, lineHeight = 17.sp),
        body = TextStyle(fontFamily = Fonts.Archivo, fontSize = 15.sp, lineHeight = 21.sp),
        small = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 11.sp, lineHeight = 15.sp),
        tab = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.02.em),
        label = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.04.em),
    )

    /** Arches running into the distance at the top right, and nothing else. */
    @Composable
    override fun Backdrop(modifier: Modifier) {
        val line = LocalPalette.current.ink.copy(alpha = 0.09f)
        Canvas(modifier) { arches(line, size.width * 0.86f, size.height * 0.34f, size.width * 0.62f, size.height * 0.3f, count = 11) }
    }

    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        val p = LocalPalette.current
        Column(modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("[ ${(dateline ?: "Verza").uppercase()} ]", style = type.label, color = p.inkMuted, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            Text(title.uppercase(), style = type.masthead, color = p.ink, modifier = Modifier.padding(top = 18.dp, bottom = 14.dp).semantics { heading() })
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.ink))
        }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        val p = LocalPalette.current
        Row(modifier.padding(top = 34.dp, bottom = 14.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
            Text("[ ${text.uppercase()} ]", style = type.section, color = p.ink)
            Box(Modifier.weight(1f).padding(start = 12.dp).height(1.dp).background(p.line))
        }
    }

    /** Words and an arrow, in a thin frame; the one to press is filled. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val p = LocalPalette.current
        val primary = color == p.accent
        val ink = if (primary) p.canvas else p.ink
        Row(
            modifier.heightIn(min = 48.dp).background(if (primary) p.ink else Color.Transparent).border(1.dp, p.ink)
                .clickable(enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text.uppercase(), style = type.headline, color = if (enabled) ink else ink.copy(alpha = 0.4f), maxLines = 1)
            Text("  ↗", style = type.headline, color = ink)
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        val p = LocalPalette.current
        Box(
            modifier.size(size).background(p.canvas).border(1.dp, p.ink).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = p.ink, modifier = Modifier.size(iconSize * 0.9f)) }
    }

    @Composable
    override fun Divider(start: Dp) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(LocalPalette.current.line))
    }

    /** Covers in grey with an index over each and a mono caption, wide apart. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        val p = LocalPalette.current
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                Column(Modifier.width(138.dp).clickable(onClick = cardAction(card, onOpen))) {
                    Text("%02d".format(i + 1), style = type.label, color = p.inkMuted, modifier = Modifier.padding(bottom = 6.dp))
                    Cover(card.art?.let { artAt(it, 544) }, 138.dp)
                    Text(cardTitle(card).uppercase(), style = type.headline.copy(fontSize = 12.sp, lineHeight = 16.sp), color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                    Text(card.subtitle.substringBefore(" • "), style = type.small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    /** A strip between two rules, its small words running across: what plays, or that nothing does. */
    @Composable
    override fun HomeHero() {
        val p = LocalPalette.current
        val now by LocalContext.current.graph.player.state.collectAsState()
        val words = now.song?.let { "NOW PLAYING ✦ ${com.verza.data.shownTitle(it.title, it.artist).uppercase()} — ${it.artist.uppercase()} ✦ " } ?: "NOTHING PLAYING ✦ LESS, BUT BETTER ✦ "
        Text(
            words.repeat(4), style = type.label, color = p.ink, maxLines = 1,
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().drawBehind {
                drawRect(p.ink, size = Size(size.width, 1.dp.toPx()))
                drawRect(p.ink, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
            }.padding(vertical = 9.dp).basicMarquee(iterations = Int.MAX_VALUE, velocity = 30.dp),
        )
    }

    /** Bracketed words in a row on a rule; the chosen one inverted. The song above it, its words running. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        val p = LocalPalette.current
        Column(modifier.fillMaxWidth().background(p.canvas).takesTouches().navigationBarsPadding()) {
            state.downloads?.let { DownloadsLine(it.uppercase(), p.ink) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.fillMaxWidth().height(58.dp).drawBehind {
                        drawRect(p.line, size = Size(size.width, 1.dp.toPx()))
                        drawRect(p.ink, size = Size(size.width * played, 1.dp.toPx()))
                    }.clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        ("${com.verza.data.shownTitle(song.title, song.artist)} — ${song.artist}   ").uppercase().repeat(2), style = type.label, color = p.ink, maxLines = 1,
                        modifier = Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE, velocity = 24.dp),
                    )
                    PlayPauseNext(state.now, p.ink, 24.dp)
                }
            }
            Row(Modifier.fillMaxWidth().height(54.dp).drawBehind { drawRect(p.ink, size = Size(size.width, 1.dp.toPx())) }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { state.go(tab.route) }.semantics { role = Role.Tab; selected = on; contentDescription = tab.label }, contentAlignment = Alignment.Center) {
                        Text(
                            "[ ${tab.label.substringAfterLast(' ').uppercase()} ]", style = type.tab, color = if (on) p.canvas else p.ink, maxLines = 1,
                            modifier = Modifier.background(if (on) p.ink else Color.Transparent).padding(horizontal = 4.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }

    // Now Playing keeps the page's black or white: the cover is the only colour on it.
    override fun nowPlaying(page: Palette) = page

    @Composable
    override fun NowPlayingBackground(art: String?) {
        val p = LocalPalette.current
        Canvas(Modifier.fillMaxSize().background(p.canvas)) {
            arches(p.ink.copy(alpha = 0.12f), size.width / 2, size.height * 0.62f, size.width * 1.1f, size.height * 0.56f, count = 14)
        }
    }

    /** The cover in colour, in a thin frame stood off it, with marks at its corners. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        val ink = LocalPalette.current.ink
        Box(
            Modifier.graphicsLayer { scaleX = rest; scaleY = rest; translationY = -fly * size.height * 2f; alpha = 1f - fly }.size(side * 0.82f)
                .drawBehind {
                    val o = 12.dp.toPx(); val m = 14.dp.toPx(); val w = 1.dp.toPx()
                    drawRect(ink.copy(alpha = 0.35f), Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), style = Stroke(w))
                    for ((x, y) in listOf(-o to -o, size.width + o to -o, -o to size.height + o, size.width + o to size.height + o)) {
                        drawLine(ink, Offset(x - m / 2, y), Offset(x + m / 2, y), w * 1.5f)
                        drawLine(ink, Offset(x, y - m / 2), Offset(x, y + m / 2), w * 1.5f)
                    }
                },
        ) { AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        val p = LocalPalette.current
        Box(
            Modifier.size(78.dp).border(1.dp, p.ink, CircleShape).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = p.ink, modifier = Modifier.size(38.dp)) }
    }

    /** A hairline with a tick every tenth, the played part heavier, a small square where it's at. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val y = size.height / 2
        val w = density
        drawLine(p.line, Offset(0f, y), Offset(size.width, y), w)
        for (i in 0..10) { val x = size.width * i / 10; drawLine(p.inkMuted, Offset(x, y - 4 * w), Offset(x, y + 4 * w), w) }
        drawLine(p.ink, Offset(0f, y), Offset(size.width * shown, y), 2 * w)
        val s = (8f + 4f * dragging) * w
        drawRect(p.ink, Offset(size.width * shown - s / 2, y - s / 2), Size(s, s))
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        val p = LocalPalette.current
        Box(modifier.background(p.canvas)) {
            Canvas(Modifier.fillMaxSize()) { arches(p.ink.copy(alpha = 0.25f), size.width * 0.7f, size.height * 0.62f, size.width * 0.9f, size.height * 0.5f, count = 8) }
            Column(Modifier.padding(12.dp)) {
                Text("[ LESS ]", style = type.label, color = p.inkMuted)
                Text("MORE", style = type.masthead.copy(fontSize = 24.sp, lineHeight = 26.sp), color = p.ink, modifier = Modifier.padding(top = 70.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.ink))
            }
        }
    }
}
