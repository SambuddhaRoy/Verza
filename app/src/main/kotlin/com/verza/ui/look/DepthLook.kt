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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.ui.kit.Cover
import com.verza.ui.kit.glass
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D, after Alche: a near-black page with a floor running away to a vanishing point and "+" marks
 * on a lattice, a glassy iridescent triangle turning in the air, a carousel whose cards curve away
 * at its ends, wide white type, small mono notes. The prism is drawn in code, a turning wireframe:
 * nothing here is Alche's own mark.
 */
object DepthLook : Look("depth", "3D", "A turning prism, a curved carousel, depth") {
    private val Night = Color(0xFF06060A)
    private val Panel = Color(0xFF12121A)
    /** Oil on water: the prism's edges, the borders, the played part of a song. */
    private val Sheen = listOf(Color(0xFFB9F3FF), Color(0xFFC7A6FF), Color(0xFFFFB8E6), Color(0xFFFFF1B0), Color(0xFFB9F3FF))
    private val SheenLine = Brush.linearGradient(Sheen)

    // The accent is the cover's, searched against the dark.
    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Night, ink = Color.White, swatches = swatches, cell = Panel, fill = Color.White.copy(alpha = 0.09f), line = Color.White.copy(alpha = 0.14f))

    override fun cover(size: Dp): Shape = RoundedCornerShape(if (size in 1.dp..64.dp) 8.dp else 14.dp)
    override val group: Shape = RoundedCornerShape(18.dp)
    override val panel = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    // A dark ground under the floating dock, so the list doesn't show between its panes.
    override val edgeFade = true
    override fun dockHeight(playing: Boolean) = 10.dp + 64.dp + (if (playing) 10.dp + 58.dp else 0.dp) + 10.dp

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.Unbounded, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.01).em),
        section = TextStyle(fontFamily = Fonts.JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em),
        title = TextStyle(fontFamily = Fonts.Unbounded, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 33.sp),
        headline = TextStyle(fontFamily = Fonts.SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
        rowTitle = TextStyle(fontFamily = Fonts.SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.JetBrainsMono, fontSize = 12.sp, lineHeight = 17.sp),
        body = TextStyle(fontFamily = Fonts.SpaceGrotesk, fontSize = 15.sp, lineHeight = 21.sp),
        small = TextStyle(fontFamily = Fonts.JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
        tab = TextStyle(fontFamily = Fonts.JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.06.em),
        label = TextStyle(fontFamily = Fonts.JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.06.em),
    )

    /** The floor, the lattice of crosses, and the prism turning high on the right. */
    @Composable
    override fun Backdrop(modifier: Modifier) {
        val t by slowTime(20)
        Canvas(modifier.background(Night)) {
            drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xFF1A1033)))
            perspectiveFloor(Color.White.copy(alpha = 0.06f), horizon = 0.66f)
            crosshairs(Color.White.copy(alpha = 0.09f), 72.dp, 4.dp)
            prism(Offset(size.width * 0.8f, size.height * 0.25f), size.width * 0.13f, t * 0.35f, 0.5f + 0.1f * sin(t * 0.3f), 0.4f)
        }
    }

    /**
     * A triangular prism with a triangle set inside its face, as a wireframe in [SheenLine], turned by
     * [yaw] and [pitch] and seen in perspective.
     */
    internal fun DrawScope.prism(c: Offset, r: Float, yaw: Float, pitch: Float, alpha: Float) {
        val depth = r * 0.42f
        fun tri(scale: Float, z: Float) = (0 until 3).map { k ->
            val a = Math.toRadians((-90.0 + k * 120.0))
            floatArrayOf((r * scale * cos(a)).toFloat(), (r * scale * sin(a)).toFloat(), z)
        }
        fun project(p: FloatArray): Offset {
            val x1 = p[0] * cos(yaw) - p[2] * sin(yaw)
            val z1 = p[0] * sin(yaw) + p[2] * cos(yaw)
            val y2 = p[1] * cos(pitch) - z1 * sin(pitch)
            val z2 = p[1] * sin(pitch) + z1 * cos(pitch)
            val f = r * 3.2f
            val s = f / (f + z2)
            return Offset(c.x + x1 * s, c.y + y2 * s)
        }
        val front = tri(1f, -depth).map(::project)
        val back = tri(1f, depth).map(::project)
        val inner = tri(0.42f, -depth).map(::project)
        val w = 1.4.dp.toPx()
        val face = Path().apply { moveTo(front[0].x, front[0].y); lineTo(front[1].x, front[1].y); lineTo(front[2].x, front[2].y); close() }
        drawPath(face, Color.White.copy(alpha = 0.05f * alpha))
        for (ring in listOf(front, back, inner)) for (k in 0 until 3) drawLine(SheenLine, ring[k], ring[(k + 1) % 3], w, StrokeCap.Round, alpha = alpha)
        for (k in 0 until 3) drawLine(SheenLine, front[k], back[k], w, StrokeCap.Round, alpha = alpha * 0.7f)
    }

    /** A mono index over the name, and the name again behind it, enormous and only outlined: depth from type. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        Box(modifier.fillMaxWidth().clipToBounds().padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                case.of(title).uppercase(), style = type.masthead.copy(fontSize = 92.sp, lineHeight = 92.sp, drawStyle = Stroke(1.2f)), color = Color.White.copy(alpha = 0.1f), maxLines = 1, softWrap = false,
                modifier = Modifier.padding(top = 4.dp).clearAndSetSemantics { },
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f).padding(top = 30.dp)) {
                    Text("+ 0${title.length % 9 + 1} — ${(dateline ?: "Verza").uppercase()}", style = type.label, color = Color.White.copy(alpha = 0.6f))
                    Text(case.of(title), style = type.masthead, color = Color.White, modifier = Modifier.padding(top = 6.dp).semantics { heading() })
                }
                trailing?.invoke()
            }
        }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        Row(modifier.padding(top = 28.dp, bottom = 12.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
            Text("+", style = type.section.copy(fontSize = 16.sp), color = LocalPalette.current.accent)
            Text("  ${text.uppercase()}", style = type.section, color = Color.White)
            Box(Modifier.weight(1f).padding(start = 12.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent))))
        }
    }

    /** A dark pill with an iridescent edge; the one to press filled with the sheen itself. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        val ink = if (primary) Night else Color.White
        Row(
            modifier.heightIn(min = 48.dp).clip(CircleShape)
                .background(if (primary) Brush.linearGradient(Sheen) else Brush.linearGradient(listOf(Panel, Panel)))
                .border(1.2.dp, SheenLine, CircleShape).clickable(enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
            Text(case.of(text), style = type.headline, color = if (enabled) ink else ink.copy(alpha = 0.4f), maxLines = 1)
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Box(
            modifier.size(size).clip(CircleShape).background(if (stain != null) Brush.linearGradient(Sheen) else Brush.linearGradient(listOf(Panel.copy(alpha = 0.85f), Panel.copy(alpha = 0.85f))))
                .border(1.dp, SheenLine, CircleShape).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (stain != null) Night else Color.White, modifier = Modifier.size(iconSize)) }
    }

    @Composable
    override fun Divider(start: Dp) {
        Box(Modifier.fillMaxWidth().padding(start = start).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
    }

    /** A carousel bent round you: each card turns away and shrinks the further it is from the middle. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        val list = rememberLazyListState()
        LazyRow(state = list, contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                Column(
                    Modifier.width(164.dp).graphicsLayer {
                        // Read where the card sits as it's drawn, so scrolling turns the cards without recomposing them.
                        val info = list.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == i }
                        if (item != null) {
                            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            val d = ((item.offset + item.size / 2f - mid) / (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1) * 2f).coerceIn(-1.4f, 1.4f)
                            rotationY = -d * 34f
                            val s = 1f - abs(d) * 0.1f
                            scaleX = s; scaleY = s
                            alpha = 1f - abs(d) * 0.25f
                            cameraDistance = 9f * density
                        }
                    }.clickable(onClick = cardAction(card, onOpen)),
                ) {
                    Box(Modifier.size(164.dp).shadow(18.dp, cover(164.dp), spotColor = LocalPalette.current.accent).border(1.dp, SheenLine, cover(164.dp))) {
                        Cover(card.art?.let { artAt(it, 544) }, 164.dp)
                    }
                    Text("%02d".format(i + 1) + "  ${card.subtitle.substringBefore(" • ").uppercase()}", style = type.label, color = Color.White.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                    Text(cardTitle(card), style = type.headline, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    /** Glass in the dark: a pane of it across the foot, the tabs in mono, the chosen one lit with the sheen. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.downloads?.let { DownloadsLine(it.uppercase(), Color.White) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.fillMaxWidth().height(58.dp).takesTouches().glass(RoundedCornerShape(18.dp), 18.dp)
                        .drawBehind { drawLine(SheenLine, Offset(18.dp.toPx(), size.height - 1.dp.toPx()), Offset(18.dp.toPx() + (size.width - 36.dp.toPx()) * played, size.height - 1.dp.toPx()), 2.dp.toPx(), StrokeCap.Round) }
                        .clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 9.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 40.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist), style = type.headline, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist.uppercase(), style = type.small, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlayPauseNext(state.now, Color.White, 26.dp)
                }
            }
            Row(Modifier.fillMaxWidth().height(64.dp).takesTouches().glass(RoundedCornerShape(22.dp), 22.dp).padding(horizontal = 6.dp)) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable { state.go(tab.route) }.semantics(mergeDescendants = true) { role = Role.Tab; selected = on },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(tab.icon, null, tint = if (on) Color.White else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(21.dp))
                        Text(tab.label.substringAfterLast(' ').uppercase(), style = type.tab, color = if (on) Color.White else Color.White.copy(alpha = 0.55f), maxLines = 1, modifier = Modifier.padding(top = 3.dp))
                        Box(Modifier.padding(top = 4.dp).size(if (on) 22.dp else 0.dp, 2.dp).background(SheenLine, CircleShape))
                    }
                }
            }
        }
    }

    override fun nowPlaying(page: Palette) = page

    /** The floor and crosses again, the cover's colour as a glow, the prism large and faint behind the cover. */
    @Composable
    override fun NowPlayingBackground(art: String?) {
        val t by slowTime(24)
        val accent = LocalPalette.current.accent
        Canvas(Modifier.fillMaxSize().background(Night)) {
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.32f), Color.Transparent), Offset(size.width / 2, size.height * 0.3f), size.width * 0.8f), size.width * 0.8f, Offset(size.width / 2, size.height * 0.3f))
            perspectiveFloor(Color.White.copy(alpha = 0.07f), horizon = 0.55f, rays = 22)
            crosshairs(Color.White.copy(alpha = 0.08f), 64.dp, 4.dp)
            prism(Offset(size.width / 2, size.height * 0.3f), size.width * 0.48f, t * 0.22f, 0.35f, 0.32f)
        }
    }

    /** The cover turning slowly in the air over its own reflection on the floor. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        val turn by rememberInfiniteTransition(label = "turn").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "t")
        val yaw = sin(turn * 2 * Math.PI.toFloat()) * 12f
        val c = side * 0.76f
        Column(
            Modifier.graphicsLayer { scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX; translationY = -fly * size.height * 2f; alpha = 1f - fly },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(c).graphicsLayer { rotationY = yaw; rotationX = 6f; cameraDistance = 10f * density }.shadow(30.dp, RoundedCornerShape(18.dp), spotColor = LocalPalette.current.accent).border(1.2.dp, SheenLine, RoundedCornerShape(18.dp))) {
                AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)))
            }
            // The reflection: the cover upside down, fading into the floor.
            Box(Modifier.padding(top = 6.dp).size(c, side * 0.18f).clipToBounds().graphicsLayer { rotationY = yaw; cameraDistance = 10f * density }) {
                AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true).size(c).graphicsLayer { scaleY = -1f; alpha = 0.35f }.clip(RoundedCornerShape(18.dp)))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Night.copy(alpha = 0.3f), Night))))
            }
        }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Box(
            Modifier.size(84.dp).shadow(22.dp, CircleShape, spotColor = LocalPalette.current.accent).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF2A2A3A), Panel))).border(2.dp, SheenLine, CircleShape)
                .clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
    }

    /** A faint line with a cross at each quarter; the played part in the sheen, a glowing point where it's at. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val y = size.height / 2
        drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, y), Offset(size.width, y), density * 1.5f)
        for (k in 0..4) { val x = size.width * k / 4; val a = 4 * density; drawLine(Color.White.copy(alpha = 0.4f), Offset(x, y - a), Offset(x, y + a), density) }
        drawLine(SheenLine, Offset(0f, y), Offset(size.width * shown, y), (2.5f + 1.5f * dragging) * density, StrokeCap.Round)
        val head = Offset(size.width * shown, y)
        drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.6f), Color.Transparent), head, 16 * density), 16 * density, head)
        drawCircle(Color.White, (4.5f + 2f * dragging) * density, head)
    }

    @Composable
    override fun NowPlayingDecor() {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 52.dp)) {
            Text("+ NOW PLAYING", style = type.label, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.weight(1f))
            Text("[ 3D ]", style = type.label, color = Color.White.copy(alpha = 0.6f))
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Box(modifier.background(Night)) {
            Canvas(Modifier.fillMaxSize()) {
                perspectiveFloor(Color.White.copy(alpha = 0.12f), horizon = 0.6f, rays = 10, rungs = 6)
                crosshairs(Color.White.copy(alpha = 0.18f), 36.dp, 3.dp)
                prism(Offset(size.width * 0.5f, size.height * 0.4f), size.width * 0.3f, 0.6f, 0.4f, 1f)
            }
            Text("DEPTH", style = type.label, color = Color.White, modifier = Modifier.padding(10.dp))
        }
    }
}
