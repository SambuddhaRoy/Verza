package com.verza.ui.look

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import com.verza.R
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.kit.Cover
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * Playful design, after Concept Capers (a card game's site: illustrated character cards fanned out
 * to pick from, each with a number and a name plate, cards that flip, sunburst rays, a heavy
 * condensed face in black tags). Everything here wants touching: buttons push into their shadows,
 * covers are cards, and the one on Now Playing tilts under your finger and flips when tapped.
 */
object PlayfulLook : Look("playful", "Playful", "Covers as cards to pick, tilt and flip") {
    private val Cream = Color(0xFFFFF5E2)
    private val Navy = Color(0xFF1E1B4B)
    private val Sky = Color(0xFFA8D8F0)
    private val Pastels = listOf(Color(0xFFF7B3C8), Color(0xFFFFE07A), Color(0xFFB5E8C3), Color(0xFFCDB8F5), Sky)
    private val Berry = Color(0xFFC2185B)

    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Cream, ink = Navy, accent = Berry, cell = Color.White, fill = Color(0xFFFFE9C2), line = Navy.copy(alpha = 0.2f), inkMuted = Color(0xFF4E4A7A))

    override fun cover(size: Dp): Shape = RoundedCornerShape(if (size in 1.dp..64.dp) 8.dp else 14.dp)
    override val group: Shape = RoundedCornerShape(20.dp)
    override val panel = androidx.compose.foundation.BorderStroke(2.5.dp, Navy)
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 10.dp + 68.dp + (if (playing) 12.dp + 66.dp else 0.dp) + 14.dp

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.Baloo, fontWeight = FontWeight.ExtraBold, fontSize = 42.sp, lineHeight = 46.sp),
        section = TextStyle(fontFamily = Fonts.Anton, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = 0.02.em),
        title = TextStyle(fontFamily = Fonts.Baloo, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 36.sp),
        headline = TextStyle(fontFamily = Fonts.Baloo, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
        rowTitle = TextStyle(fontFamily = Fonts.Baloo, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.Baloo, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
        body = TextStyle(fontFamily = Fonts.Baloo, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
        small = TextStyle(fontFamily = Fonts.Baloo, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp),
        tab = TextStyle(fontFamily = Fonts.Baloo, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp),
        label = TextStyle(fontFamily = Fonts.Anton, fontSize = 13.sp, lineHeight = 15.sp, letterSpacing = 0.05.em),
    )

    /** Rays of butter yellow from above the top of the screen, very faint, turning slowly. */
    @Composable
    override fun Backdrop(modifier: Modifier) {
        Canvas(modifier.background(Cream)) { sunburst(Offset(size.width * 0.5f, -size.height * 0.15f), Color(0xFFFFE07A).copy(alpha = 0.32f), rays = 32) }
    }

    /** The page's name in a navy tag, tipped; the dateline on a pastel pill tipped the other way. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        Row(modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (dateline != null) Text(dateline, style = type.headline.copy(fontSize = 14.sp), color = Navy,
                    modifier = Modifier.graphicsLayer { rotationZ = 2f }.background(Pastels[2], CircleShape).border(2.dp, Navy, CircleShape).padding(horizontal = 12.dp, vertical = 2.dp))
                Text("${case.of(title)}!", style = type.masthead, color = Color.White,
                    modifier = Modifier.padding(top = 10.dp).graphicsLayer { rotationZ = -2.5f }.hardShadow(RoundedCornerShape(10.dp), Berry, 5.dp, 5.dp)
                        .background(Navy, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp).semantics { heading() })
            }
            trailing?.invoke()
        }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        Text(text.uppercase(), style = type.section, color = Color.White,
            modifier = modifier.padding(top = 28.dp, bottom = 14.dp).graphicsLayer { rotationZ = -1.5f }.background(Navy, RoundedCornerShape(6.dp)).padding(horizontal = 12.dp, vertical = 2.dp).semantics { heading() })
    }

    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        Pushable(CircleShape, if (primary) Pastels[1] else Color.White, Navy, Navy, modifier.heightIn(min = 54.dp), depth = 4.dp, border = 2.5.dp, enabled = enabled, onClick = onClick) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = Navy, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)) }
                Text(case.of(text), style = type.headline, color = if (enabled) Navy else Navy.copy(alpha = 0.4f), maxLines = 1)
            }
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Pushable(CircleShape, if (stain != null) Pastels[1] else Color.White, Navy, Navy, modifier.size(size + 4.dp), depth = 4.dp, label = label, onClick = onClick) {
            Icon(icon, null, tint = Navy, modifier = Modifier.size(iconSize))
        }
    }

    @Composable
    override fun Divider(start: Dp) {
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            drawLine(Navy.copy(alpha = 0.25f), Offset(start.toPx(), size.height / 2), Offset(size.width, size.height / 2), size.height, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        }
    }

    /** A hand of cards: each a pastel card with a number, its cover framed in white, its name on a navy plate. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        LazyRow(contentPadding = PaddingValues(start = 20.dp, end = 30.dp, top = 12.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                val turn = listOf(-5f, 3f, -2f, 5f, -3f)[i % 5]
                PlayCard(i + 1, card.art?.let { artAt(it, 544) }, cardTitle(card), Pastels[i % Pastels.size], 150.dp, Modifier.graphicsLayer { rotationZ = turn }, onClick = cardAction(card, onOpen))
            }
        }
    }

    /** One card: [number] in a badge, the cover in a white frame, [name] on a plate across its foot. */
    @Composable
    private fun PlayCard(number: Int, art: String?, name: String, colour: Color, width: Dp, modifier: Modifier, onClick: (() -> Unit)?) {
        val press = remember { Animatable(1f) }
        val scope = rememberCoroutineScope()
        Box(
            modifier.width(width).aspectRatio(0.74f).graphicsLayer { scaleX = press.value; scaleY = press.value }
                .hardShadow(RoundedCornerShape(16.dp), Navy, 5.dp, 5.dp).background(colour, RoundedCornerShape(16.dp)).border(2.5.dp, Navy, RoundedCornerShape(16.dp))
                .then(if (onClick != null) Modifier.pointerInput(onClick) {
                    detectTapGestures(
                        onPress = { scope.launch { press.animateTo(0.93f, spring(stiffness = Spring.StiffnessHigh)) }; tryAwaitRelease(); scope.launch { press.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow)) } },
                        onTap = { onClick() },
                    )
                }.semantics { role = Role.Button; contentDescription = name } else Modifier)
                .padding(9.dp),
        ) {
            AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 14.dp).background(Color.White, RoundedCornerShape(10.dp)).padding(4.dp).clip(RoundedCornerShape(7.dp)))
            Box(Modifier.size(26.dp).background(Color.White, CircleShape).border(2.dp, Navy, CircleShape), contentAlignment = Alignment.Center) {
                Text("$number", style = type.headline.copy(fontSize = 13.sp, lineHeight = 13.sp), color = Navy)
            }
            Text(name.uppercase(), style = type.label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { rotationZ = -2f }.background(Navy, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 3.dp))
        }
    }

    /** A sky panel with a fan of four cards, each holding an instrument, to say pick one. */
    @Composable
    override fun HomeHero() {
        val now by LocalContext.current.graph.player.state.collectAsState()
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).hardShadow(RoundedCornerShape(22.dp), Navy, 5.dp, 5.dp)
            .background(Sky, RoundedCornerShape(22.dp)).border(2.5.dp, Navy, RoundedCornerShape(22.dp)).padding(14.dp)) {
            Column(Modifier.align(Alignment.CenterStart).width(130.dp)) {
                Text("PICK A CARD", style = type.section.copy(fontSize = 20.sp), color = Navy)
                Text(now.song?.let { "Playing ${com.verza.data.shownTitle(it.title, it.artist)}" } ?: "Any song you like, or a mix below.", style = type.small, color = Navy, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.align(Alignment.CenterEnd).size(176.dp, 116.dp)) {
                listOf(R.drawable.fx_guitar, R.drawable.fx_sax, R.drawable.fx_drum, R.drawable.fx_studio_mic).forEachIndexed { i, res ->
                    val turn = -18f + i * 12f
                    Box(
                        Modifier.align(Alignment.BottomCenter).offset(x = (-54 + i * 36).dp).graphicsLayer { rotationZ = turn; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.4f) }
                            .size(64.dp, 88.dp).background(Pastels[i], RoundedCornerShape(10.dp)).border(2.dp, Navy, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon3D(res, 44.dp) }
                }
            }
        }
    }

    /** A chunky white bar on a hard shadow; the chosen tab sits on a yellow blob and bounces when picked. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.downloads?.let { DownloadsLine(it, Navy) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.fillMaxWidth().height(66.dp).takesTouches().hardShadow(RoundedCornerShape(20.dp), Navy, 4.dp, 4.dp).background(Pastels[0], RoundedCornerShape(20.dp))
                        .border(2.5.dp, Navy, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).drawWithContentBar(played, Navy)
                        .clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 44.dp, Modifier.border(3.dp, Color.White, RoundedCornerShape(8.dp)))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist), style = type.headline, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = type.small, color = Navy.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlayPauseNext(state.now, Navy, 30.dp)
                }
            }
            Row(
                Modifier.fillMaxWidth().height(68.dp).takesTouches().hardShadow(RoundedCornerShape(24.dp), Navy, 4.dp, 4.dp).background(Color.White, RoundedCornerShape(24.dp))
                    .border(2.5.dp, Navy, RoundedCornerShape(24.dp)).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    val pop by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow), label = "pop")
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).clickable { state.go(tab.route) }.semantics(mergeDescendants = true) { role = Role.Tab; selected = on },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Box(Modifier.size(38.dp, 30.dp), contentAlignment = Alignment.Center) {
                            if (pop > 0.01f) Box(Modifier.matchParentSize().graphicsLayer { scaleX = pop; scaleY = pop }.background(Pastels[1], CircleShape).border(2.dp, Navy, CircleShape))
                            Icon(tab.icon, null, tint = Navy, modifier = Modifier.size(20.dp))
                        }
                        Text(tab.label, style = type.tab, color = Navy, maxLines = 1)
                    }
                }
            }
        }
    }

    // Now Playing is a sky panel under the sun's rays, the cover a card to play with.
    override fun nowPlaying(page: Palette) = Palette.make(canvas = Sky, ink = Navy, accent = Berry, line = Navy.copy(alpha = 0.25f), fill = Color.White.copy(alpha = 0.6f), inkMuted = Color(0xFF34306A))

    @Composable
    override fun NowPlayingBackground(art: String?) {
        Canvas(Modifier.fillMaxSize().background(Sky)) { sunburst(Offset(size.width / 2, size.height * 0.32f), Color.White.copy(alpha = 0.3f), rays = 36) }
    }

    /** The cover as a card. Drag to tilt it toward you; let go and it springs back; tap to turn it over. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        val now by LocalContext.current.graph.player.state.collectAsState()
        var flipped by remember { mutableStateOf(false) }
        val turn by animateFloatAsState(if (flipped) 180f else 0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow), label = "flip")
        val tiltX = remember { Animatable(0f) }
        val tiltY = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        Box(
            Modifier.graphicsLayer {
                scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX
                rotationY = turn + tiltY.value; rotationX = tiltX.value; cameraDistance = 14f * density
                translationY = -fly * size.height * 2f; alpha = 1f - fly
            }
                .pointerInput(Unit) { detectTapGestures(onTap = { flipped = !flipped }) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { scope.launch { tiltX.animateTo(0f, spring(dampingRatio = 0.4f)) }; scope.launch { tiltY.animateTo(0f, spring(dampingRatio = 0.4f)) } },
                    ) { change, d ->
                        change.consume()
                        scope.launch { tiltY.snapTo((tiltY.value + d.x / 6f).coerceIn(-28f, 28f)) }
                        scope.launch { tiltX.snapTo((tiltX.value - d.y / 6f).coerceIn(-28f, 28f)) }
                    }
                }
                .semantics { contentDescription = "Cover. Tap to turn it over" },
        ) {
            if (turn <= 90f) PlayCard(1, art, now.song?.artist ?: "", Pastels[3], side * 0.78f, Modifier, null)
            else CardBack(side * 0.78f, Modifier.graphicsLayer { rotationY = 180f })
        }
    }

    /** The back of a card: navy, a ring of stars, VERZA across it. */
    @Composable
    private fun CardBack(width: Dp, modifier: Modifier) {
        Box(modifier.width(width).aspectRatio(0.74f).hardShadow(RoundedCornerShape(16.dp), Navy, 5.dp, 5.dp).background(Navy, RoundedCornerShape(16.dp))
            .border(6.dp, Color.White, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                drawRoundRect(Pastels[1], style = Stroke(3.dp.toPx()), cornerRadius = CornerRadius(10.dp.toPx()))
                sunburst(center, Pastels[3].copy(alpha = 0.25f), rays = 24)
            }
            Text("VERZA", style = type.section.copy(fontSize = 40.sp, lineHeight = 44.sp), color = Pastels[1], modifier = Modifier.graphicsLayer { rotationZ = -12f })
        }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Pushable(CircleShape, Pastels[1], Navy, Navy, Modifier.size(92.dp), depth = 6.dp, border = 3.dp, label = if (playing) "Pause" else "Play", onClick = onClick) {
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Navy, modifier = Modifier.size(50.dp))
        }
    }

    /** A chunky white track in a navy outline, filled yellow, a navy knob with a white ring. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val h = (14f + 4f * dragging) * density
        val top = Offset(0f, size.height / 2 - h / 2)
        drawRoundRect(Color.White, top, Size(size.width, h), CornerRadius(h / 2))
        drawRoundRect(Pastels[1], top, Size(size.width * shown, h), CornerRadius(h / 2))
        drawRoundRect(Navy, top, Size(size.width, h), CornerRadius(h / 2), style = Stroke(2.5f * density))
        val c = Offset(size.width * shown, size.height / 2)
        drawCircle(Color.White, (12f + 3f * dragging) * density, c)
        drawCircle(Navy, (8f + 3f * dragging) * density, c)
    }

    @Composable
    override fun NowPlayingDecor() {
        val a = bob(2600); val b = bob(3400)
        Box(Modifier.fillMaxSize()) {
            Icon3D(R.drawable.fx_party, 64.dp, Modifier.align(Alignment.TopStart).offset(x = 18.dp, y = (86 + a * 6).dp).graphicsLayer { rotationZ = -10f + a * 8f })
            Icon3D(R.drawable.fx_sparkles, 50.dp, Modifier.align(Alignment.TopEnd).offset(x = (-22).dp, y = (110 + b * 8).dp))
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Box(modifier.background(Cream)) {
            Canvas(Modifier.fillMaxSize()) { sunburst(Offset(size.width / 2, -40f), Color(0xFFFFE07A).copy(alpha = 0.5f), rays = 20) }
            Text("Play!", style = type.masthead.copy(fontSize = 26.sp, lineHeight = 30.sp), color = Color.White,
                modifier = Modifier.padding(12.dp).graphicsLayer { rotationZ = -3f }.background(Navy, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp))
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
                for (i in 0..2) Box(
                    Modifier.offset(x = (-30 + i * 28).dp).graphicsLayer { rotationZ = -12f + i * 12f }.size(44.dp, 60.dp)
                        .background(Pastels[i], RoundedCornerShape(8.dp)).border(2.dp, Navy, RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

/** A bar along an element's foot, [played] of its width, in [colour]. */
internal fun Modifier.drawWithContentBar(played: Float, colour: Color) = this.drawWithContent {
    drawContent()
    drawRect(colour, Offset(0f, size.height - 4.dp.toPx()), Size(size.width * played, 4.dp.toPx()))
}
