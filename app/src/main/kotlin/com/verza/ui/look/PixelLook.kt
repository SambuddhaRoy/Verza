package com.verza.ui.look

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.R
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.ui.kit.Cover
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette

/**
 * Mixing 2D and 3D, after Diverso Studio: a cream page ruled like graph paper, a pixel display face,
 * an outlined wordmark, a staircase of orange pixels, and 3D things (figures, cubes, a keyboard)
 * set among the flat line work, with an electric blue section. Here the 3D things are rendered
 * objects standing on platforms drawn in line, and cubes drawn in isometric over flat blue.
 */
object PixelLook : Look("pixel", "2D + 3D", "Pixel type on graph paper, 3D things on line art") {
    private val Cream = Color(0xFFF3EEE1)
    private val Ink = Color(0xFF141414)
    private val Orange = Color(0xFFFF4A1C)
    private val Blue = Color(0xFF1F5BFF)
    private val Lemon = Color(0xFFFFE14D)

    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Cream, ink = Ink, accent = Color(0xFFC8320B), cell = Color(0xFFFBF8F0), fill = Ink.copy(alpha = 0.07f), line = Ink.copy(alpha = 0.3f), inkMuted = Color(0xFF4F4A40))

    override val case = Case.Lower
    override fun cover(size: Dp): Shape = PixelShape(if (size in 1.dp..64.dp) 2.dp else 4.dp)
    override val group: Shape = PixelShape(5.dp)
    override val panel = androidx.compose.foundation.BorderStroke(2.dp, Ink)
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 62.dp + (if (playing) 60.dp else 0.dp)

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.Pixelify, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 46.sp),
        section = TextStyle(fontFamily = Fonts.Pixelify, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 25.sp),
        title = TextStyle(fontFamily = Fonts.Pixelify, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp),
        headline = TextStyle(fontFamily = Fonts.Pixelify, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 21.sp),
        rowTitle = TextStyle(fontFamily = Fonts.SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 12.sp, lineHeight = 17.sp),
        body = TextStyle(fontFamily = Fonts.SpaceGrotesk, fontSize = 15.sp, lineHeight = 21.sp),
        small = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 11.sp, lineHeight = 15.sp),
        tab = TextStyle(fontFamily = Fonts.Pixelify, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 16.sp),
        label = TextStyle(fontFamily = Fonts.SpaceMono, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.02.em),
    )

    /** Graph paper: fine squares and every fourth line darker. */
    private fun DrawScope.graph(c: Color) {
        val minor = 12.dp.toPx()
        var x = 0f; var k = 0
        while (x < size.width) { drawLine(c.copy(alpha = if (k % 4 == 0) c.alpha * 2f else c.alpha), Offset(x, 0f), Offset(x, size.height), density * 0.7f); x += minor; k++ }
        var y = 0f; k = 0
        while (y < size.height) { drawLine(c.copy(alpha = if (k % 4 == 0) c.alpha * 2f else c.alpha), Offset(0f, y), Offset(size.width, y), density * 0.7f); y += minor; k++ }
    }

    /** A staircase of pixels down to the right from [from], each step [s]. */
    private fun DrawScope.stairs(from: Offset, s: Float, steps: Int, c: Color) {
        for (k in 0 until steps) for (j in 0..k) drawRect(c, Offset(from.x + k * s, from.y + j * s), Size(s, s))
    }

    /** An isometric cube, its three faces shaded, centred on [c]. */
    private fun DrawScope.isoCube(c: Offset, s: Float, top: Color, left: Color, right: Color) {
        val k = 0.866f * s
        drawPath(Path().apply { moveTo(c.x, c.y - s); lineTo(c.x + k, c.y - s / 2); lineTo(c.x, c.y); lineTo(c.x - k, c.y - s / 2); close() }, top)
        drawPath(Path().apply { moveTo(c.x - k, c.y - s / 2); lineTo(c.x, c.y); lineTo(c.x, c.y + s); lineTo(c.x - k, c.y + s / 2); close() }, left)
        drawPath(Path().apply { moveTo(c.x, c.y); lineTo(c.x + k, c.y - s / 2); lineTo(c.x + k, c.y + s / 2); lineTo(c.x, c.y + s); close() }, right)
    }

    @Composable
    override fun Backdrop(modifier: Modifier) {
        Box(modifier.background(Cream)) {
            Canvas(Modifier.fillMaxSize()) {
                graph(Ink.copy(alpha = 0.045f))
                stairs(Offset(size.width - 6 * 14.dp.toPx(), size.height * 0.52f), 14.dp.toPx(), 6, Orange.copy(alpha = 0.5f))
            }
            Text("verza", style = TextStyle(fontFamily = Fonts.Unbounded, fontWeight = FontWeight.Black, fontSize = 120.sp, lineHeight = 120.sp, drawStyle = Stroke(2.5f)),
                color = Ink.copy(alpha = 0.08f), maxLines = 1, softWrap = false, modifier = Modifier.align(Alignment.CenterStart).offset(x = (-12).dp, y = 40.dp).clearAndSetSemantics { })
        }
    }

    /** The name in pixels with a block cursor blinking after it, as on a terminal. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        val t by slowTime(2)
        Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("${(dateline ?: "verza").lowercase()} · verza studio", style = type.label, color = Ink.copy(alpha = 0.7f))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(title.lowercase(), style = type.masthead, color = Ink, modifier = Modifier.semantics { heading() })
                    Box(Modifier.padding(start = 6.dp).size(18.dp, 34.dp).background(if ((t * 2).toInt() % 2 == 0) Orange else Color.Transparent))
                }
            }
            trailing?.invoke()
        }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        Row(modifier.padding(top = 26.dp, bottom = 12.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(14.dp)) { val s = size.width / 2; drawRect(Orange, size = Size(s, s)); drawRect(Orange, Offset(s, s), Size(s, s)) }
            Text("  ${text.lowercase()}", style = type.section, color = Ink)
        }
    }

    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        Pushable(PixelShape(4.dp), if (primary) Orange else Color.White, Ink, Ink, modifier.heightIn(min = 52.dp), depth = 4.dp, border = 2.5.dp, enabled = enabled, onClick = onClick) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = Ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
                Text(text.lowercase(), style = type.headline, color = if (enabled) Ink else Ink.copy(alpha = 0.4f), maxLines = 1)
            }
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Pushable(PixelShape(4.dp), if (stain != null) Orange else Color.White, Ink, Ink, modifier.size(size + 4.dp), depth = 4.dp, label = label, onClick = onClick) {
            Icon(icon, null, tint = Ink, modifier = Modifier.size(iconSize))
        }
    }

    /** A row of pixels, one every few. */
    @Composable
    override fun Divider(start: Dp) {
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            var x = start.toPx(); val s = 2.dp.toPx()
            while (x < size.width) { drawRect(Ink.copy(alpha = 0.35f), Offset(x, 0f), Size(s, s)); x += 3 * s }
        }
    }

    /** Covers with their outline drawn again off to one side in line: the flat drawing and the picture together. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        LazyRow(contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 12.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                Column(Modifier.width(144.dp).clickable(onClick = cardAction(card, onOpen))) {
                    Box(Modifier.size(144.dp).drawBehind {
                        val o = (-8).dp.toPx()
                        drawRect(Ink, Offset(o, o), size, style = Stroke(1.5f * density))
                        drawRect(Orange, Offset(o - 3.dp.toPx(), o - 3.dp.toPx()), Size(6.dp.toPx(), 6.dp.toPx()))
                    }) { Cover(card.art?.let { artAt(it, 544) }, 144.dp) }
                    Text(cardTitle(card).lowercase(), style = type.headline.copy(fontSize = 15.sp, lineHeight = 18.sp), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                    Text("%02d · ${card.subtitle.substringBefore(" • ").lowercase()}".format(i + 1), style = type.small, color = Ink.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    /** 3D things (headphones, a joystick, a disc, a microphone) stood on a platform drawn in line. */
    @Composable
    override fun HomeHero() {
        val bobs = listOf(bob(3000), bob(3700), bob(4400), bob(3300))
        BoxWithConstraints(Modifier.fillMaxWidth().height(156.dp).padding(horizontal = 20.dp).semantics { contentDescription = "Headphones, a joystick, a disc and a microphone on a platform" }) {
            val w = maxWidth
            Canvas(Modifier.fillMaxSize()) {
                // An isometric slab: its top face, then its two near sides, in line.
                val top = size.height * 0.66f; val d = 26.dp.toPx(); val inset = 30.dp.toPx(); val thick = 14.dp.toPx()
                val face = Path().apply { moveTo(inset, top); lineTo(size.width - inset, top); lineTo(size.width - inset - d, top + d); lineTo(inset - d, top + d); close() }
                drawPath(face, Color.White)
                drawPath(Path().apply { moveTo(inset - d, top + d); lineTo(size.width - inset - d, top + d); lineTo(size.width - inset - d, top + d + thick); lineTo(inset - d, top + d + thick); close() }, Orange)
                drawPath(Path().apply { moveTo(size.width - inset - d, top + d); lineTo(size.width - inset, top); lineTo(size.width - inset, top + thick); lineTo(size.width - inset - d, top + d + thick); close() }, Color(0xFFC43A12))
                drawPath(face, Ink, style = Stroke(1.5f * density))
                drawRect(Ink, Offset(inset - d, top + d), Size(size.width - 2 * inset, thick), style = Stroke(1.5f * density))
            }
            listOf(R.drawable.fx_headphone, R.drawable.fx_joystick, R.drawable.fx_disc, R.drawable.fx_studio_mic).forEachIndexed { i, res ->
                // Stood on the slab's top face, which is at two thirds of the height.
                Icon3D(res, 64.dp, Modifier.offset(x = w * (0.08f + i * 0.22f), y = (46 + bobs[i] * 3).dp))
            }
            Text("create & play", style = type.label, color = Ink, modifier = Modifier.align(Alignment.TopEnd).background(Lemon).padding(horizontal = 6.dp, vertical = 2.dp).clearAndSetSemantics { })
        }
    }

    /** Pixel words on cream, the chosen one inverted with an orange pixel; the song on a blue bar of pixels. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().takesTouches().background(Cream).drawBehind { drawRect(Ink, size = Size(size.width, 2.dp.toPx())) }.navigationBarsPadding()) {
            state.downloads?.let { DownloadsLine(it.lowercase(), Ink) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.fillMaxWidth().height(60.dp).background(Blue).clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 40.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist).lowercase(), style = type.headline.copy(fontSize = 15.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Canvas(Modifier.fillMaxWidth().padding(top = 5.dp).height(6.dp)) { blocks(played, Color.White, Color.White.copy(alpha = 0.3f), Lemon) }
                    }
                    PlayPauseNext(state.now, Color.White, 26.dp)
                }
            }
            Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { state.go(tab.route) }.semantics { role = Role.Tab; selected = on; contentDescription = tab.label }, contentAlignment = Alignment.Center) {
                        Row(Modifier.clip(PixelShape(3.dp)).background(if (on) Ink else Color.Transparent).padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (on) { Box(Modifier.size(6.dp).background(Orange)); Spacer(Modifier.width(5.dp)) }
                            Text(tab.label.substringAfterLast(' ').lowercase(), style = type.tab, color = if (on) Cream else Ink, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    /** Square pixels across, [fraction] of them filled, the last one lit. */
    private fun DrawScope.blocks(fraction: Float, on: Color, off: Color, head: Color) {
        val s = size.height; val gap = s * 0.5f
        val n = ((size.width + gap) / (s + gap)).toInt().coerceAtLeast(1)
        val filled = (fraction * n).toInt()
        for (i in 0 until n) drawRect(if (i < filled) on else if (i == filled) head else off, Offset(i * (s + gap), 0f), Size(s, s))
    }

    // Now Playing is the blue section: white pixels, yellow for what's lit.
    override fun nowPlaying(page: Palette) = Palette.make(canvas = Blue, ink = Color.White, accent = Lemon, line = Color.White.copy(alpha = 0.3f), fill = Color.White.copy(alpha = 0.16f), inkMuted = Color.White.copy(alpha = 0.82f))

    /** Electric blue with a faint grid, and black cubes drawn in isometric floating at its edges. */
    @Composable
    override fun NowPlayingBackground(art: String?) {
        val t by slowTime(20)
        Canvas(Modifier.fillMaxSize().background(Blue)) {
            graph(Color.White.copy(alpha = 0.05f))
            val cubes = listOf(Triple(0.08f, 0.16f, 46f), Triple(0.92f, 0.1f, 30f), Triple(0.95f, 0.44f, 54f), Triple(0.04f, 0.5f, 26f), Triple(0.7f, 0.06f, 18f))
            cubes.forEachIndexed { i, (x, y, s) ->
                val lift = kotlin.math.sin(t * 0.8f + i * 1.7f) * 6 * density
                isoCube(Offset(size.width * x, size.height * y + lift), s * density, Color(0xFF3A3A3A), Color(0xFF151515), Color(0xFF262626))
            }
        }
    }

    /** The cover in a pixel frame on a hard shadow, a pair of 3D headphones sat on its corner. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        val b = bob(2600)
        val c = side * 0.8f
        Box(Modifier.graphicsLayer { scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX; translationY = -fly * size.height * 2f; alpha = 1f - fly }) {
            Box(Modifier.size(c).hardShadow(PixelShape(8.dp), Ink, 10.dp, 10.dp).clip(PixelShape(8.dp)).background(Color.White).padding(5.dp).clip(PixelShape(6.dp))) {
                AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Icon3D(R.drawable.fx_headphone, c * 0.36f, Modifier.align(Alignment.TopEnd).offset(x = c * 0.12f, y = -c * 0.16f + (b * 3).dp).graphicsLayer { rotationZ = 14f })
        }
    }

    /** A yellow pixel button on a hard shadow, its symbol drawn in pixels. */
    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Pushable(PixelShape(6.dp), Lemon, Ink, Ink, Modifier.size(88.dp), depth = 6.dp, border = 3.dp, label = if (playing) "Pause" else "Play", onClick = onClick) {
            Canvas(Modifier.size(35.dp)) {
                val px = size.width / 7
                val rows = if (playing) List(7) { ".XX.XX." }
                else listOf("X......", "XX.....", "XXX....", "XXXX...", "XXX....", "XX.....", "X......")
                rows.forEachIndexed { y, row -> row.forEachIndexed { x, ch -> if (ch == 'X') drawRect(Ink, Offset((x + if (playing) 0 else 1) * px, y * px), Size(px, px)) } }
            }
        }
    }

    /** Pixel blocks across, white where played, yellow where it is. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val s = (9f + 3f * dragging) * density
        val top = size.height / 2 - s / 2
        translate(0f, top) {
            val gap = s * 0.45f
            val n = ((size.width + gap) / (s + gap)).toInt().coerceAtLeast(1)
            val filled = (shown * n).toInt()
            for (i in 0 until n) drawRect(if (i < filled) Color.White else if (i == filled) Lemon else Color.White.copy(alpha = 0.25f), Offset(i * (s + gap), 0f), Size(s, s))
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Box(modifier.background(Cream)) {
            Canvas(Modifier.fillMaxSize()) {
                graph(Ink.copy(alpha = 0.06f))
                stairs(Offset(size.width - 4 * 12.dp.toPx(), size.height - 4 * 12.dp.toPx()), 12.dp.toPx(), 4, Orange)
                isoCube(Offset(size.width * 0.3f, size.height * 0.62f), 22.dp.toPx(), Color(0xFF3A3A3A), Color(0xFF151515), Color(0xFF262626))
            }
            Text("2d+3d", style = type.masthead.copy(fontSize = 26.sp, lineHeight = 30.sp), color = Ink, modifier = Modifier.padding(10.dp))
        }
    }
}
