package com.verza.ui.look

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/**
 * Trend mixing, after Brand Appart: no one trend but several at once. A heavy grotesk with a ©, 3D
 * charms hanging from keychains drawn in line, a bento grid of figures, a deck of cards to swipe
 * through as on a dating app, dark sections between the light ones, and work circling in a ring.
 * Its accent is the playing cover's, as Liquid glass's is: that's in the mix too.
 */
object MixLook : Look("mix", "Trend mix", "Bento, swipe cards, 3D charms, all at once") {
    private val Cream = Color(0xFFF5F3ED)
    private val Ink = Color(0xFF121212)
    private val Dark = Color(0xFF161616)

    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Cream, ink = Ink, swatches = swatches, cell = Color.White, fill = Ink.copy(alpha = 0.06f), line = Ink.copy(alpha = 0.12f))

    override fun cover(size: Dp): Shape = RoundedCornerShape(if (size in 1.dp..64.dp) 10.dp else 20.dp)
    override val group: Shape = RoundedCornerShape(26.dp)
    // The page's ground fades in under the floating dock, so the list doesn't show between its panels.
    override val edgeFade = true
    override fun dockHeight(playing: Boolean) = 12.dp + 62.dp + (if (playing) 10.dp + 64.dp else 0.dp) + 12.dp

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.PlusJakarta, fontWeight = FontWeight.ExtraBold, fontSize = 42.sp, lineHeight = 42.sp, letterSpacing = (-0.045).em),
        section = TextStyle(fontFamily = Fonts.PlusJakarta, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 28.sp, letterSpacing = (-0.035).em),
        title = TextStyle(fontFamily = Fonts.PlusJakarta, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = (-0.04).em),
        headline = TextStyle(fontFamily = Fonts.PlusJakarta, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.01).em),
        rowTitle = TextStyle(fontFamily = Fonts.PlusJakarta, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.PlusJakarta, fontSize = 14.sp, lineHeight = 19.sp),
        body = TextStyle(fontFamily = Fonts.PlusJakarta, fontSize = 15.sp, lineHeight = 22.sp),
        small = TextStyle(fontFamily = Fonts.PlusJakarta, fontSize = 12.sp, lineHeight = 16.sp),
        tab = TextStyle(fontFamily = Fonts.PlusJakarta, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp),
        label = TextStyle(fontFamily = Fonts.PlusJakarta, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.04.em),
    )

    /** The name heavy and tight, a © in a circle of the accent after it. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        val p = LocalPalette.current
        Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (dateline != null) Text(dateline, style = type.small, color = p.inkMuted)
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 2.dp)) {
                    Text(case.of(title), style = type.masthead, color = Ink, modifier = Modifier.semantics { heading() })
                    Box(Modifier.padding(start = 3.dp, top = 4.dp).size(20.dp).background(p.accent, CircleShape), contentAlignment = Alignment.Center) {
                        Text("©", style = type.label.copy(fontSize = 13.sp), color = p.onAccent, modifier = Modifier.clearAndSetSemantics { })
                    }
                }
            }
            trailing?.invoke()
        }
    }

    /** Two tones: the first word in ink, the rest quieter, as "Featured work" is set. */
    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        val muted = LocalPalette.current.inkMuted
        val words = case.of(text).split(' ', limit = 2)
        Text(
            buildAnnotatedString {
                append(words[0])
                if (words.size > 1) withStyle(SpanStyle(color = muted)) { append(" " + words[1]) }
            },
            style = type.section, color = Ink, modifier = modifier.padding(top = 30.dp, bottom = 14.dp).semantics { heading() },
        )
    }

    /** A pill, black for the one to press, with a little circle of the accent carrying an arrow at its end. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val p = LocalPalette.current
        val primary = color == p.accent
        Row(
            modifier.heightIn(min = 50.dp).clip(CircleShape).background(if (primary) Ink else Color.White).border(1.5.dp, Ink, CircleShape)
                .clickable(enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(start = 18.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = if (primary) Color.White else Ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
                Text(case.of(text), style = type.headline, color = (if (primary) Color.White else Ink).copy(alpha = if (enabled) 1f else 0.4f), maxLines = 1)
            }
            Box(Modifier.padding(start = 10.dp).size(36.dp).background(if (primary) p.accent else Ink, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = if (primary) p.onAccent else Color.White, modifier = Modifier.size(17.dp))
            }
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        Box(
            modifier.size(size).shadow(8.dp, CircleShape).background(stain ?: Color.White, CircleShape)
                .clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (stain != null) LocalPalette.current.onAccent else Ink, modifier = Modifier.size(iconSize)) }
    }

    /** A deck of cards: swipe the top one away and the next comes up; tap it to play or open it. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        val cards = section.cards
        if (cards.isEmpty()) return
        var top by remember(section.title) { mutableIntStateOf(0) }
        val dx = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val next = { top = (top + 1) % cards.size }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(318.dp), contentAlignment = Alignment.TopCenter) {
                for (k in minOf(2, cards.size - 1) downTo 0) {
                    val card = cards[(top + k) % cards.size]
                    val isTop = k == 0
                    val go = cardAction(card, onOpen)
                    Column(
                        Modifier.padding(top = 8.dp).width(232.dp)
                            .graphicsLayer {
                                if (isTop) { translationX = dx.value; rotationZ = dx.value / 30f }
                                else { translationY = k * 12.dp.toPx(); val s = 1f - k * 0.05f; scaleX = s; scaleY = s; rotationZ = if (k == 1) 3.5f else -4f }
                            }
                            .shadow(if (isTop) 18.dp else 6.dp, RoundedCornerShape(26.dp)).background(Color.White, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp))
                            .then(
                                if (isTop) Modifier
                                    .pointerInput(top) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                scope.launch {
                                                    if (abs(dx.value) > 110.dp.toPx()) { dx.animateTo(sign(dx.value) * 900.dp.toPx(), tween(220)); next(); dx.snapTo(0f) }
                                                    else dx.animateTo(0f, spring(dampingRatio = 0.6f))
                                                }
                                            },
                                            onDragCancel = { scope.launch { dx.animateTo(0f, spring()) } },
                                        ) { change, d -> change.consume(); scope.launch { dx.snapTo(dx.value + d) } }
                                    }
                                    .clickable(onClick = go)
                                    .semantics { customActions = listOf(CustomAccessibilityAction("Next card") { next(); true }) }
                                else Modifier,
                            )
                            .padding(10.dp),
                    ) {
                        Cover(card.art?.let { artAt(it, 544) }, 212.dp)
                        Text(cardTitle(card), style = type.headline.copy(fontSize = 16.sp), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
                        Text(card.subtitle.substringBefore(" • "), style = type.small, color = Ink.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    }
                }
            }
            Text("${top + 1} / ${cards.size}  ·  swipe", style = type.label, color = LocalPalette.current.inkMuted)
        }
    }

    /** A charm on a ring and a carabiner drawn in line, swinging from its clip. */
    @Composable
    private fun Keychain(charm: Int, height: Dp, modifier: Modifier = Modifier) {
        val t by slowTime(30)
        Column(
            modifier.graphicsLayer { rotationZ = sin(t * 1.6f) * 9f; transformOrigin = TransformOrigin(0.5f, 0f) }.clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(height * 0.5f, height * 0.62f)) {
                val w = 2.5f * density
                // The carabiner: a long rounded loop, its gate a gap in one side.
                drawRoundRect(Ink, Offset(size.width * 0.28f, 0f), Size(size.width * 0.44f, size.height * 0.56f), CornerRadius(size.width * 0.22f), style = Stroke(w))
                drawLine(Color.White, Offset(size.width * 0.72f, size.height * 0.16f), Offset(size.width * 0.72f, size.height * 0.36f), w * 1.6f)
                drawLine(Ink, Offset(size.width * 0.68f, size.height * 0.14f), Offset(size.width * 0.62f, size.height * 0.38f), w, StrokeCap.Round)
                // The split ring.
                drawCircle(Ink, size.width * 0.2f, Offset(size.width / 2, size.height * 0.76f), style = Stroke(w))
            }
            Icon3D(charm, height * 0.62f, Modifier.offset(y = (-6).dp))
        }
    }

    /**
     * A bento grid of what's going on: the song playing (or last played) large, plays in the accent,
     * a 3D charm swinging on its keychain, and the streak with the artist you've had on most.
     */
    @Composable
    override fun HomeHero() {
        val graph = LocalContext.current.graph
        val now by graph.player.state.collectAsState()
        val recent by graph.library.recent.collectAsState(initial = emptyList())
        val l = rememberListening()
        val p = LocalPalette.current
        val song = now.song ?: recent.firstOrNull()
        val tile = RoundedCornerShape(26.dp)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(250.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1.25f).fillMaxHeight().clip(tile).background(Dark)) {
                    if (song?.art != null) AsyncImage(artAt(song.art!!, 544), null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Icon3D(R.drawable.fx_headphone, 110.dp, Modifier.align(Alignment.Center))
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f))))
                    Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 60.dp, bottom = 14.dp)) {
                        Text(if (now.song != null) "Now playing" else "Last played", style = type.label, color = Color.White.copy(alpha = 0.8f))
                        Text(song?.let { com.verza.data.shownTitle(it.title, it.artist) } ?: "Nothing yet", style = type.headline.copy(fontSize = 18.sp, lineHeight = 22.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (song != null) Box(
                        Modifier.align(Alignment.BottomEnd).padding(12.dp).size(44.dp).background(Color.White, CircleShape)
                            .clickable { if (now.song != null) graph.player.toggle() else graph.player.play(listOf(song)) }
                            .semantics { role = Role.Button; contentDescription = if (now.playing) "Pause" else "Play" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Ink) }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f).fillMaxWidth().clip(tile).background(p.accent).padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("Plays", style = type.label, color = p.onAccent)
                        Text("${l.plays}", style = type.masthead.copy(fontSize = 44.sp), color = p.onAccent, maxLines = 1)
                    }
                    Box(Modifier.weight(1.1f).fillMaxWidth().clip(tile).background(Color.White), contentAlignment = Alignment.TopCenter) {
                        Keychain(R.drawable.fx_guitar, 110.dp, Modifier.padding(top = 6.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clip(tile).background(Color.White).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon3D(R.drawable.fx_fire, 30.dp)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(if (l.streak == 1) "1 day in a row" else "${l.streak} days in a row", style = type.headline, color = Ink)
                    Text(l.topArtist?.let { "Mostly $it lately" } ?: "Play something to start a streak", style = type.small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    /** A white pill with the song, its play button a black circle; under it a dark pill of four icons. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        val p = LocalPalette.current
        Column(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.downloads?.let { DownloadsLine(it, Ink) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.fillMaxWidth().height(64.dp).takesTouches().shadow(14.dp, CircleShape).background(Color.White, CircleShape).clip(CircleShape)
                        .drawWithContentBar(played, p.accent).clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 48.dp, Modifier.clip(CircleShape))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist), style = type.headline, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = type.small, color = Ink.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlayPauseNext(state.now, Ink, 26.dp)
                }
            }
            Row(Modifier.height(62.dp).takesTouches().shadow(14.dp, CircleShape).background(Dark, CircleShape).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    Box(
                        Modifier.size(width = 66.dp, height = 50.dp).clip(CircleShape).background(if (on) p.accent else Color.Transparent)
                            .clickable { state.go(tab.route) }.semantics { role = Role.Tab; selected = on; contentDescription = tab.label },
                        contentAlignment = Alignment.Center,
                    ) { Icon(tab.icon, null, tint = if (on) p.onAccent else Color.White, modifier = Modifier.size(23.dp)) }
                }
            }
        }
    }

    // Now Playing is a dark section, the cover's colour searched again against it.
    override fun nowPlaying(page: Palette) =
        Palette.make(canvas = Dark, ink = Color.White, swatches = listOf(page.accent), cell = Color(0xFF222222), fill = Color.White.copy(alpha = 0.12f), line = Color.White.copy(alpha = 0.2f))

    @Composable
    override fun NowPlayingBackground(art: String?) {
        val accent = LocalPalette.current.accent
        Canvas(Modifier.fillMaxSize().background(Dark)) {
            val c = Offset(size.width / 2, size.height * 0.32f)
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent), c, size.width * 0.75f), size.width * 0.75f, c)
        }
    }

    /**
     * The cover in the middle, and other songs circling round it: what comes next, then the rest of the
     * queue round to the start, then what was played lately, so the ring is there with a queue of one.
     */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        val graph = LocalContext.current.graph
        val now by graph.player.state.collectAsState()
        val recent by graph.library.recent.collectAsState(initial = emptyList())
        val coming = (now.queue.drop(now.index + 1) + now.queue.take(now.index) + recent).filter { it.id != now.song?.id && it.art != null }.distinctBy { it.id }.take(10)
            // A ring of two looks like a mistake: fewer than eight go round more than once.
            .let { c -> if (c.isEmpty()) c else List(maxOf(8, c.size)) { c[it % c.size] } }
        val t by slowTime(30)
        Box(Modifier.size(side).graphicsLayer { scaleX = rest; scaleY = rest; translationY = -fly * size.height * 2f; alpha = 1f - fly }, contentAlignment = Alignment.Center) {
            coming.forEachIndexed { i, s ->
                val a = t * 0.18f + i * (2 * Math.PI.toFloat() / coming.size.coerceAtLeast(1))
                val r = side * 0.44f
                AsyncImage(
                    s.art?.let { artAt(it, 226) }, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.offset(x = r * cos(a), y = r * sin(a)).size(42.dp).graphicsLayer { rotationZ = a * 20f % 24f - 12f }.clip(RoundedCornerShape(10.dp)),
                )
            }
            AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.size(side * 0.6f).graphicsLayer { scaleX = 1f + swell * 0.04f; scaleY = scaleX }.shadow(24.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)))
        }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        val accent = LocalPalette.current.accent
        Box(
            Modifier.size(88.dp).border(3.dp, accent, CircleShape).padding(7.dp).background(Color.White, CircleShape)
                .clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Ink, modifier = Modifier.size(40.dp)) }
    }

    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val y = size.height / 2
        drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, y), Offset(size.width, y), 3 * density, StrokeCap.Round)
        drawLine(Color.White, Offset(0f, y), Offset(size.width * shown, y), 3 * density, StrokeCap.Round)
        drawCircle(p.accent, (7f + 3f * dragging) * density, Offset(size.width * shown, y))
    }

    @Composable
    override fun NowPlayingDecor() {
        Text("Now playing ©", style = type.label, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(start = 24.dp, top = 52.dp))
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        val p = LocalPalette.current
        Column(modifier.background(Cream).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Mix©", style = type.masthead.copy(fontSize = 24.sp, lineHeight = 26.sp), color = Ink)
            Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(Dark), contentAlignment = Alignment.Center) { Icon3D(R.drawable.fx_headphone, 44.dp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.accent))
                    Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White))
                }
            }
        }
    }
}
