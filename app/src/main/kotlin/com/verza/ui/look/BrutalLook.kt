package com.verza.ui.look

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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * Brutalism, after Gelato La Boca (condensed capitals with a hard 3D extrusion, yellow, blue and
 * black, a menu that drops in as loose shapes), Stiff (the turning "Comedy Gold" badge, charcoal and
 * cream) and General Condition Studio (a red slab serif). Huge words, square corners, thick black
 * borders, hard shadows that buttons are pushed down into.
 */
object BrutalLook : Look("brutal", "Brutalism", "Huge words, hard shadows, loud colour") {
    val Yellow = Color(0xFFFFC400)
    val Blue = Color(0xFF0F8BD6)
    val Red = Color(0xFFD11423)
    val Black = Color(0xFF121212)
    val Cream = Color(0xFFF2EEDF)
    private val Blocks = listOf(Blue, Red, Yellow)

    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Cream, ink = Black, accent = Red, cell = Color.White, fill = Black.copy(alpha = 0.07f), line = Black, inkMuted = Color(0xFF4A4740))

    override val case = Case.Upper
    override fun cover(size: Dp): Shape = RectangleShape
    override val group: Shape = RectangleShape
    override val panel = androidx.compose.foundation.BorderStroke(3.dp, Black)
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 62.dp + (if (playing) 64.dp else 0.dp)

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.Anton, fontSize = 96.sp, lineHeight = 88.sp),
        section = TextStyle(fontFamily = Fonts.AlfaSlab, fontSize = 26.sp, lineHeight = 30.sp),
        title = TextStyle(fontFamily = Fonts.Anton, fontSize = 40.sp, lineHeight = 40.sp),
        headline = TextStyle(fontFamily = Fonts.ArchivoNarrow, fontWeight = FontWeight.Black, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.02.em),
        rowTitle = TextStyle(fontFamily = Fonts.Archivo, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.Archivo, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp),
        body = TextStyle(fontFamily = Fonts.Archivo, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
        small = TextStyle(fontFamily = Fonts.Archivo, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
        tab = TextStyle(fontFamily = Fonts.Anton, fontSize = 18.sp, lineHeight = 20.sp, letterSpacing = 0.02.em),
        label = TextStyle(fontFamily = Fonts.ArchivoNarrow, fontWeight = FontWeight.Black, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.08.em),
    )

    /** A page's name as large as the width allows, extruded, on a yellow block; the dateline as a black tag. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        Column(modifier.fillMaxWidth().background(Yellow).drawBehind { drawRect(Black, Offset(0f, size.height - 3.dp.toPx()), Size(size.width, 3.dp.toPx())) }.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dateline != null) Text(dateline.uppercase(), style = type.label, color = Yellow, modifier = Modifier.background(Black).padding(horizontal = 8.dp, vertical = 4.dp))
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
            }
            FillText(title.uppercase(), type.masthead, Cream, Modifier.fillMaxWidth().padding(top = 4.dp).semantics { heading() }, max = 150f, extrude = Black, depth = 6.dp)
        }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        Row(modifier.padding(top = 28.dp, bottom = 12.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
            Text(text.uppercase(), style = type.section, color = Red, maxLines = 2)
            Text("  →", style = type.section, color = Black)
        }
    }

    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        Pushable(RectangleShape, if (primary) Yellow else Cream, Black, Black, modifier.heightIn(min = 52.dp), depth = 5.dp, border = 3.dp, enabled = enabled, onClick = onClick) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = Black, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(6.dp)) }
                Text(text.uppercase(), style = type.tab.copy(fontSize = 20.sp), color = if (enabled) Black else Black.copy(alpha = 0.4f), maxLines = 1)
            }
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Pushable(RectangleShape, stain ?: Yellow, Black, Black, modifier.size(size + 4.dp), depth = 4.dp, border = 3.dp, label = label, onClick = onClick) {
            Icon(icon, null, tint = Black, modifier = Modifier.size(iconSize))
        }
    }

    @Composable
    override fun Divider(start: Dp) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Black))
    }

    /** Covers in thick frames on shadows of the three colours, each a little askew, as cut-outs pasted down. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        LazyRow(contentPadding = PaddingValues(start = 18.dp, end = 24.dp, top = 6.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                val go = cardAction(card, onOpen)
                Column(Modifier.width(150.dp).graphicsLayer { rotationZ = if (i % 2 == 0) -2.5f else 2f }.clickable(onClick = go)) {
                    Box(Modifier.size(150.dp).hardShadow(RectangleShape, Blocks[i % 3], 7.dp, 7.dp).border(3.dp, Black)) {
                        AsyncImage(card.art?.let { artAt(it, 544) }, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize().padding(3.dp))
                    }
                    Text(cardTitle(card).uppercase(), style = type.tab.copy(fontSize = 19.sp, lineHeight = 21.sp), color = Black, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
                    Text(card.subtitle.substringBefore(" • ").uppercase(), style = type.label, color = Red, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    /** A black band with what's playing running across it, and the turning badge pinned over its end. */
    @Composable
    override fun HomeHero() {
        val now by LocalContext.current.graph.player.state.collectAsState()
        val words = now.song?.let { "NOW SPINNING ✺ ${com.verza.data.shownTitle(it.title, it.artist).uppercase()} ✺ ${it.artist.uppercase()} ✺ " }
            ?: "VERZA ✺ NEVER LISTEN SOLO ✺ PLAY SOMETHING LOUD ✺ "
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            Text(
                words.repeat(3), style = type.tab.copy(fontSize = 26.sp, lineHeight = 30.sp), color = Yellow, maxLines = 1,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().background(Black).padding(vertical = 10.dp).basicMarquee(iterations = Int.MAX_VALUE, velocity = 40.dp),
            )
            CircularText(
                "VERZA ✺ PLAY IT LOUD ✺ HIT PLAY ✺ ", type.label.copy(fontSize = 11.sp), Black, 92.dp,
                Modifier.align(Alignment.CenterEnd).offset(x = (-10).dp).background(Yellow, CircleShape).border(3.dp, Black, CircleShape),
            ) { Icon3D(com.verza.R.drawable.fx_disc, 44.dp) }
        }
    }

    /** A solid black bar across the foot, the chosen tab a yellow block; the song on a blue bar over it. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().takesTouches()) {
            state.downloads?.let { Text(it.uppercase(), style = type.label, color = Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().background(Yellow).border(2.dp, Black).padding(horizontal = 12.dp, vertical = 6.dp)) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.fillMaxWidth().height(64.dp).background(Blue).drawBehind {
                        drawRect(Black, size = Size(size.width, 3.dp.toPx()))
                        drawRect(Red, Offset(0f, 3.dp.toPx()), Size(size.width * played, 4.dp.toPx()))
                    }.clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 12.dp, end = 4.dp, top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 44.dp, Modifier.border(2.dp, Black))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist).uppercase(), style = type.tab.copy(fontSize = 19.sp), color = Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist.uppercase(), style = type.label, color = Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlayPauseNext(state.now, Cream)
                }
            }
            Row(Modifier.fillMaxWidth().background(Black).navigationBarsPadding().height(62.dp)) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    Box(
                        Modifier.weight(1f).fillMaxHeight().background(if (on) Yellow else Black).clickable { state.go(tab.route) }
                            .semantics { role = Role.Tab; selected = on; contentDescription = tab.label },
                        contentAlignment = Alignment.Center,
                    ) { Text(tab.label.substringAfterLast(' ').uppercase(), style = type.tab, color = if (on) Black else Cream, maxLines = 1) }
                }
            }
        }
    }

    override fun nowPlaying(page: Palette) = Palette.make(canvas = Yellow, ink = Black, accent = Red, line = Black.copy(alpha = 0.25f), fill = Black.copy(alpha = 0.1f), inkMuted = Color(0xFF3A3000))

    @Composable
    override fun NowPlayingBackground(art: String?) {
        Box(Modifier.fillMaxSize().background(Yellow).drawBehind {
            // A blue block under the controls, the page split as La Boca's are.
            drawRect(Blue, Offset(0f, size.height * 0.62f), Size(size.width, size.height))
            drawRect(Black, Offset(0f, size.height * 0.62f), Size(size.width, 3.dp.toPx()))
        })
    }

    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        Box(Modifier.graphicsLayer { scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX; rotationZ = -2f; translationY = -fly * size.height * 2f; alpha = 1f - fly }.size(side * 0.92f).hardShadow(RectangleShape, Black, 10.dp, 10.dp).border(4.dp, Black)) {
            AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize().padding(4.dp))
        }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Pushable(CircleShape, Red, Black, Black, Modifier.size(90.dp), depth = 6.dp, border = 3.dp, label = if (playing) "Pause" else "Play", onClick = onClick) {
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Cream, modifier = Modifier.size(50.dp))
        }
    }

    /** A bar with a black border, red as far as the song has played. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val h = (14f + 6f * dragging) * density
        val top = size.height / 2 - h / 2
        drawRect(Cream, Offset(0f, top), Size(size.width, h))
        drawRect(Red, Offset(0f, top), Size(size.width * shown, h))
        drawRect(Black, Offset(0f, top), Size(size.width, h), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f * density))
    }

    @Composable
    override fun NowPlayingDecor() {
        Box(Modifier.fillMaxSize().padding(top = 70.dp, end = 10.dp), contentAlignment = Alignment.TopEnd) {
            CircularText("NOW SPINNING ✺ VERZA ✺ TURN IT UP ✺ ", type.label.copy(fontSize = 11.sp), Black, 96.dp,
                Modifier.background(Cream, CircleShape).border(3.dp, Black, CircleShape)) { Icon3D(com.verza.R.drawable.fx_speaker, 40.dp) }
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Column(modifier.background(Cream)) {
            Box(Modifier.fillMaxWidth().background(Yellow).border(2.dp, Black).padding(8.dp)) {
                ExtrudedText("LOUD", type.masthead.copy(fontSize = 52.sp, lineHeight = 52.sp), Cream, Black, depth = 4.dp)
            }
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).hardShadow(RectangleShape, Blue, 5.dp, 5.dp).background(Red).border(2.dp, Black))
                Box(Modifier.size(40.dp).hardShadow(CircleShape, Black, 4.dp, 4.dp).background(Yellow, CircleShape).border(2.dp, Black, CircleShape))
            }
        }
    }
}
