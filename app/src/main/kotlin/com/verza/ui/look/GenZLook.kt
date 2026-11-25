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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
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
 * Gen Z, after Studio KARO (white, an ultra-tight heavy grotesk, a chrome winking smiley, bracketed
 * captions, pill buttons, electric blue blocks) and ROBOT (zigzag section breaks, pictures tossed in
 * tilted stacks). Every trend at once, turned up, never boring.
 */
object GenZLook : Look("genz", "Gen Z", "Huge tight type, electric blue, chrome") {
    val Blue = Color(0xFF1C1CFF)
    private val Ink = Color(0xFF0A0A0A)

    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Color.White, ink = Ink, accent = Blue, cell = Color(0xFFF3F3F3), fill = Ink.copy(alpha = 0.06f), line = Ink.copy(alpha = 0.14f))

    override fun cover(size: Dp): Shape = RectangleShape
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 64.dp + (if (playing) 66.dp else 0.dp)

    private val tight = (-0.055).em
    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Black, fontSize = 62.sp, lineHeight = 54.sp, letterSpacing = tight),
        section = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 32.sp, letterSpacing = (-0.05).em),
        title = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 38.sp, letterSpacing = tight),
        headline = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = (-0.02).em),
        rowTitle = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 21.sp, letterSpacing = (-0.025).em),
        rowSubtitle = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = (-0.01).em),
        body = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
        small = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp),
        tab = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp),
        label = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.02.em),
    )

    /** A bracketed caption, the name huge and squeezed tight, the chrome smiley rolling beside it. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        Box(modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)) {
            Column {
                Text("[ ${(dateline ?: "verza").lowercase()} — you live here too ]".uppercase(), style = type.label, color = Ink)
                Text(case.of(title), style = type.masthead, color = Ink, modifier = Modifier.padding(top = 14.dp).semantics { heading() })
            }
            Row(Modifier.align(Alignment.TopEnd), verticalAlignment = Alignment.CenterVertically) {
                ChromeSmiley(46.dp, Modifier.padding(end = 6.dp, top = 2.dp))
                trailing?.invoke()
            }
        }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        Column(modifier.padding(top = 30.dp, bottom = 12.dp).semantics { heading() }) {
            Text("( ${text.lowercase()} )".uppercase(), style = type.label, color = Blue)
            Text(case.of(text), style = type.section, color = Ink)
        }
    }

    /** A pill: an outline, or filled electric blue for the one to press. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        val ink = if (primary) Color.White else Ink
        Row(
            modifier.heightIn(min = 48.dp).background(if (primary) Blue else Color.White, CircleShape).border(1.5.dp, if (primary) Blue else Ink, CircleShape)
                .clickable(enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text(text.uppercase(), style = type.headline, color = if (enabled) ink else ink.copy(alpha = 0.4f), maxLines = 1)
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Box(
            modifier.size(size).background(if (stain != null) Blue else Color.White, CircleShape).border(1.5.dp, if (stain != null) Blue else Ink, CircleShape)
                .clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (stain != null) Color.White else Ink, modifier = Modifier.size(iconSize)) }
    }

    /** Pictures tossed into a stack: each a little turned, overlapping the one before, as prints on a desk. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        LazyRow(contentPadding = PaddingValues(start = 20.dp, end = 40.dp, top = 14.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy((-26).dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                val turn = listOf(-6f, 4f, -2f, 7f, -5f, 3f)[i % 6]
                Column(Modifier.width(158.dp).graphicsLayer { rotationZ = turn; translationY = (i % 3) * 6.dp.toPx() }.clickable(onClick = cardAction(card, onOpen))) {
                    Box(Modifier.size(158.dp).shadow(10.dp).background(Color.White).padding(6.dp)) {
                        AsyncImage(card.art?.let { artAt(it, 544) }, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Text("[ ${cardTitle(card)} ]".uppercase(), style = type.label, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp, start = 4.dp).width(118.dp))
                }
            }
        }
    }

    /** An electric blue block with what's playing set huge in white, broken off below in a zigzag. */
    @Composable
    override fun HomeHero() {
        val now by LocalContext.current.graph.player.state.collectAsState()
        Column(Modifier.padding(top = 10.dp)) {
            Box(Modifier.fillMaxWidth().background(Blue).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)) {
                Column(Modifier.padding(end = 70.dp)) {
                    Text("[ ON REPEAT IN YOUR HEAD ]", style = type.label, color = Color.White)
                    Text(now.song?.let { com.verza.data.shownTitle(it.title, it.artist) } ?: "Press play.", style = type.section.copy(fontSize = 38.sp, lineHeight = 36.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                }
                ChromeSmiley(64.dp, Modifier.align(Alignment.TopEnd))
            }
            Canvas(Modifier.fillMaxWidth().height(18.dp).graphicsLayer { rotationZ = 180f }) { zigzag(Blue, 30.dp, 18.dp) }
        }
    }

    /** Pills in a row on white, the chosen one filled blue; the song on a blue bar with its cover turned. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().background(Color.White).takesTouches().navigationBarsPadding()) {
            state.downloads?.let { DownloadsLine(it, Ink) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.fillMaxWidth().height(66.dp).background(Blue).drawBehind { drawRect(Color.White, Offset(0f, size.height - 3.dp.toPx()), Size(size.width * played, 3.dp.toPx())) }
                        .clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 44.dp, Modifier.graphicsLayer { rotationZ = -5f }.border(3.dp, Color.White))
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist), style = type.rowTitle.copy(fontWeight = FontWeight.Black, fontSize = 18.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("[ ${song.artist} ]".uppercase(), style = type.label, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlayPauseNext(state.now, Color.White)
                }
            }
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    Box(
                        Modifier.weight(1f).height(44.dp).background(if (on) Blue else Color.White, CircleShape).border(1.5.dp, if (on) Blue else Ink, CircleShape)
                            .clickable { state.go(tab.route) }.semantics { role = Role.Tab; selected = on; contentDescription = tab.label },
                        contentAlignment = Alignment.Center,
                    ) { Text(tab.label.substringAfterLast(' ').uppercase(), style = type.tab, color = if (on) Color.White else Ink, maxLines = 1) }
                }
            }
        }
    }

    override fun nowPlaying(page: Palette) = page

    /** Electric blue over the top, broken off in a zigzag, white under it for the controls. */
    @Composable
    override fun NowPlayingBackground(art: String?) {
        Canvas(Modifier.fillMaxSize().background(Color.White)) {
            val edge = size.height * 0.56f
            drawRect(Blue, size = Size(size.width, edge))
            translate(top = edge - 1f) { zigzagDown(Blue) }
        }
    }

    /** Teeth hanging down from the blue into the white. */
    private fun DrawScope.zigzagDown(colour: Color) {
        val t = 34.dp.toPx(); val d = 22.dp.toPx()
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f); var x = 0f; var down = true
            while (x < size.width + t) { x += t / 2; lineTo(x, if (down) d else 0f); down = !down }
            lineTo(size.width + t, 0f); close()
        }
        drawPath(path, colour)
    }

    /** The cover as a print stuck on at an angle, the chrome smiley rolling over its corner. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        Box(Modifier.graphicsLayer { scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX; rotationZ = 4f; translationY = -fly * size.height * 2f; alpha = 1f - fly }) {
            Box(Modifier.size(side * 0.86f).shadow(18.dp).background(Color.White).padding(8.dp)) {
                AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            ChromeSmiley(side * 0.26f, Modifier.align(Alignment.BottomEnd).offset(x = side * 0.08f, y = side * 0.08f))
        }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Box(
            Modifier.size(width = 120.dp, height = 72.dp).background(Blue, CircleShape).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
    }

    /** A thin black line, a big blue dot riding it. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val y = size.height / 2
        drawLine(Ink.copy(alpha = 0.15f), Offset(0f, y), Offset(size.width, y), 2 * density)
        drawLine(Ink, Offset(0f, y), Offset(size.width * shown, y), 2 * density)
        drawCircle(Blue, (9f + 4f * dragging) * density, Offset(size.width * shown, y))
    }

    @Composable
    override fun NowPlayingDecor() {
        Text(
            "[ NOW PLAYING — ON REPEAT IN YOUR HEAD ]", style = type.label, color = Color.White, maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(top = 62.dp, start = 24.dp),
        )
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Column(modifier.background(Color.White).padding(10.dp)) {
            Text("[ GEN Z ]", style = type.label, color = Ink)
            Text("Loud.", style = type.masthead.copy(fontSize = 40.sp, lineHeight = 38.sp), color = Ink, modifier = Modifier.padding(top = 10.dp))
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 54.dp, height = 28.dp).background(Blue, CircleShape))
                ChromeSmiley(40.dp, Modifier.padding(start = 10.dp), sway = false)
            }
        }
    }
}
