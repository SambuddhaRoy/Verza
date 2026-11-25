package com.verza.ui.look

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.verza.ui.kit.BouncyLetters
import com.verza.ui.kit.Cover
import com.verza.ui.kit.bouncy
import com.verza.ui.kit.enter
import com.verza.ui.kit.popOn
import com.verza.ui.kit.squish
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Gen Z, after Studio KARO and ROBOT (huge type squeezed tight, bracketed captions, pills, colour in
 * blocks broken off in zigzags), with a few things from current music-app concepts on Dribbble: black
 * sticker tags, a word now and then in italic serif against the grotesk, progress shown on the card
 * that plays. Its colour is the playing cover's, in light or dark. Nothing is tilted; everything
 * moves: names drop in, cards rise, buttons squish, the tab pill springs, the hero's bars dance.
 */
object GenZLook : Look("genz", "Gen Z", "Loud type, the cover's colour, everything bounces") {
    /** With no cover playing: an electric blue. */
    private val Electric = Color(0xFF2B2BFF)

    override fun palette(swatches: List<Color>, dark: Boolean): Palette {
        val canvas = if (dark) Color(0xFF0C0C0E) else Color(0xFFFBFBF8)
        val ink = if (dark) Color(0xFFF6F6F2) else Color(0xFF0A0A0A)
        return Palette.make(
            canvas = canvas, ink = ink, swatches = swatches, fallback = Electric,
            cell = if (dark) Color(0xFF18181C) else Color(0xFFF0F0EC), fill = ink.copy(alpha = if (dark) 0.1f else 0.06f), line = ink.copy(alpha = if (dark) 0.16f else 0.12f),
        )
    }

    override fun cover(size: Dp): Shape = RoundedCornerShape(if (size in 1.dp..64.dp) 8.dp else 16.dp)
    override val group: Shape = RoundedCornerShape(24.dp)
    // The page's ground fades in under the floating dock, so the list doesn't show between its parts.
    override val edgeFade = true
    override fun dockHeight(playing: Boolean) = 10.dp + 56.dp + (if (playing) 10.dp + 64.dp else 0.dp) + 12.dp

    private val tight = (-0.055).em
    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Black, fontSize = 58.sp, lineHeight = 54.sp, letterSpacing = tight),
        section = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 30.sp, letterSpacing = (-0.045).em),
        title = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 36.sp, letterSpacing = (-0.05).em),
        headline = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = (-0.02).em),
        rowTitle = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.02).em),
        rowSubtitle = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = (-0.01).em),
        body = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
        small = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        tab = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = (-0.01).em),
        label = TextStyle(fontFamily = Fonts.InterTight, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.03.em),
    )
    /** The italic serif set against the grotesk, for the odd word. */
    private val Aside = TextStyle(fontFamily = Fonts.Serif, fontSize = 20.sp, lineHeight = 22.sp)

    /** A black sticker with a few words on it, as a festival lineup tags a drop. */
    @Composable
    private fun Tag(text: String, modifier: Modifier = Modifier, ground: Color = LocalPalette.current.ink, ink: Color = LocalPalette.current.canvas) {
        Text(text.uppercase(), style = type.label, color = ink, maxLines = 1, modifier = modifier.background(ground, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 3.dp))
    }

    /** A sticker with the day on it, then the name dropping in a letter at a time. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        val p = LocalPalette.current
        Row(modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (dateline != null) Tag(dateline)
                BouncyLetters(case.of(title), type.masthead, p.ink, Modifier.padding(top = 10.dp))
            }
            trailing?.invoke()
        }
    }

    /** The heading, and a slowly turning asterisk in the cover's colour before it. */
    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        val p = LocalPalette.current
        val t by slowTime(20)
        Row(modifier.padding(top = 30.dp, bottom = 14.dp).semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.CenterVertically) {
            Text("✱", style = type.section.copy(fontSize = 22.sp), color = p.accent, modifier = Modifier.graphicsLayer { rotationZ = t * 40f }.clearAndSetSemantics { })
            Spacer(Modifier.width(8.dp))
            Text(case.of(text), style = type.section, color = p.ink)
        }
    }

    /** A pill: filled with the cover's colour for the one to press, an outline for the rest. Squishes. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val p = LocalPalette.current
        val primary = color == p.accent
        val ink = if (primary) p.onAccent else p.ink
        val source = remember { MutableInteractionSource() }
        Row(
            modifier.heightIn(min = 50.dp).squish(source).clip(CircleShape).background(if (primary) p.accent else Color.Transparent)
                .then(if (primary) Modifier else Modifier.border(1.5.dp, p.ink, CircleShape))
                .clickable(source, null, enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)) }
            Text(case.of(text), style = type.headline, color = if (enabled) ink else ink.copy(alpha = 0.4f), maxLines = 1)
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        val p = LocalPalette.current
        val source = remember { MutableInteractionSource() }
        Box(
            modifier.size(size).squish(source, 0.88f).clip(CircleShape).background(if (stain != null) p.accent else p.canvas).border(1.5.dp, if (stain != null) p.accent else p.ink, CircleShape)
                .clickable(source, null, onClick = onClick).semantics { role = Role.Button; contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (stain != null) p.onAccent else p.ink, modifier = Modifier.size(iconSize)) }
    }

    /**
     * Covers in a row, straight, each rising into place one after another; a song's has a round play
     * button in its corner. Names under them in bold, who made them in brackets.
     */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        val p = LocalPalette.current
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                val source = remember { MutableInteractionSource() }
                Column(Modifier.width(154.dp).enter(i).squish(source, 0.94f).clickable(source, null, onClick = cardAction(card, onOpen))) {
                    Box {
                        Cover(card.art?.let { artAt(it, 544) }, 154.dp)
                        if (card.song != null) Box(
                            Modifier.align(Alignment.BottomEnd).padding(8.dp).size(34.dp).background(p.accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.PlayArrow, null, tint = p.onAccent, modifier = Modifier.size(20.dp)) }
                    }
                    Text(cardTitle(card), style = type.rowTitle.copy(fontSize = 15.sp, lineHeight = 19.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
                    Text("[ ${card.subtitle.substringBefore(" • ")} ]", style = type.small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    /** Bars that dance to the music, or lie still when nothing plays: [n] of them, [height] tall. */
    @Composable
    private fun LiveBars(color: Color, playing: Boolean, n: Int = 4, height: Dp = 18.dp, modifier: Modifier = Modifier) {
        val level by rememberLevel()
        val t by slowTime(24)
        Canvas(modifier.size(width = (n * 6).dp, height = height).clearAndSetSemantics { }) {
            val w = size.width / (n * 1.6f)
            for (i in 0 until n) {
                val wave = if (playing) 0.35f + 0.65f * ((sin(t * (5f + i * 1.7f) + i * 2.1f) + 1f) / 2f) * (0.45f + 0.55f * level.coerceAtLeast(0.35f)) else 0.18f
                val h = size.height * wave
                drawRoundRect(color, Offset(i * w * 1.6f, size.height - h), Size(w, h), CornerRadius(w / 2))
            }
        }
    }

    /**
     * A block of the cover's colour across the page, broken off below in a zigzag that slowly travels:
     * a sticker, the song huge, who made it in italic, bars dancing to it, and a pill to play or pause.
     */
    @Composable
    override fun HomeHero() {
        val p = LocalPalette.current
        val graph = LocalContext.current.graph
        val now by graph.player.state.collectAsState()
        val recent by graph.library.recent.collectAsState(initial = emptyList())
        val song = now.song ?: recent.firstOrNull()
        val t by slowTime(20)
        Column(Modifier.padding(top = 12.dp).enter()) {
            Column(Modifier.fillMaxWidth().background(p.accent).padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(if (now.song != null) "On repeat in your head" else "Pick up where you left off", ground = p.onAccent, ink = p.accent)
                    Spacer(Modifier.weight(1f))
                    LiveBars(p.onAccent, now.playing)
                }
                AnimatedContent(
                    song?.id, label = "hero",
                    transitionSpec = { (slideInVertically(bouncy()) { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
                ) { _ ->
                    Column {
                        Text(song?.let { com.verza.data.shownTitle(it.title, it.artist) } ?: "Press play.", style = type.title.copy(fontSize = 40.sp, lineHeight = 39.sp), color = p.onAccent, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
                        Text(song?.artist ?: "anything you like, loud", style = Aside, color = p.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                if (song != null) {
                    val source = remember { MutableInteractionSource() }
                    Row(
                        Modifier.padding(top = 14.dp).heightIn(min = 46.dp).squish(source).clip(CircleShape).background(p.onAccent)
                            .clickable(source, null) { if (now.song != null) graph.player.toggle() else graph.player.play(listOf(song)) }
                            .semantics { role = Role.Button }.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = p.accent, modifier = Modifier.size(20.dp).popOn(now.playing))
                        Text(if (now.playing) "  Pause" else "  Play", style = type.headline, color = p.accent)
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(16.dp)) { zigzagDown(p.accent, t * 18.dp.toPx()) }
        }
    }

    /** Teeth hanging down across the width, [phase] along, so they can travel. */
    private fun DrawScope.zigzagDown(colour: Color, phase: Float = 0f) {
        val t = 30.dp.toPx(); val d = size.height
        val start = -(phase % t) - t
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(start, 0f); var x = start; var down = true
            while (x < size.width + t) { x += t / 2; lineTo(x, if (down) d else 0f); down = !down }
            lineTo(size.width + t, 0f); close()
        }
        drawPath(path, colour)
    }

    /**
     * The song on a card of its colour that bounces as it changes, its progress along its foot; under it
     * the places as pills, the chosen one in a pill of colour that springs from one to the next.
     */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        val p = LocalPalette.current
        Column(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.downloads?.let { DownloadsLine(it, p.ink) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                val source = remember { MutableInteractionSource() }
                Row(
                    Modifier.fillMaxWidth().height(64.dp).takesTouches().squish(source, 0.97f).shadow(10.dp, RoundedCornerShape(20.dp), spotColor = p.accent).clip(RoundedCornerShape(20.dp)).background(p.accent)
                        .drawBehind { drawRect(p.onAccent.copy(alpha = 0.9f), Offset(0f, size.height - 3.dp.toPx()), Size(size.width * played, 3.dp.toPx())) }
                        .clickable(source, null, onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(song.id, label = "song", transitionSpec = { (slideInVertically(bouncy()) { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) }, modifier = Modifier.weight(1f)) { _ ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Cover(song.art?.let { artAt(it, 226) }, 44.dp)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(com.verza.data.shownTitle(song.title, song.artist), style = type.rowTitle.copy(fontWeight = FontWeight.Black, fontSize = 17.sp), color = p.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = Aside.copy(fontSize = 16.sp, lineHeight = 18.sp), color = p.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    PlayPauseNext(state.now, p.onAccent)
                }
            }
            TabPills(state)
        }
    }

    /** The places as pills on a capsule; the chosen one's pill of colour stretches across and settles, as a drop runs. */
    @Composable
    private fun TabPills(state: DockState) {
        val p = LocalPalette.current
        val index = state.tabs.indexOfFirst { it.route == state.current }
        val left = remember { Animatable(index.coerceAtLeast(0).toFloat()) }
        val right = remember { Animatable(index.coerceAtLeast(0) + 1f) }
        LaunchedEffect(index) {
            if (index < 0) return@LaunchedEffect
            val ahead = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
            val behind = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
            val forward = index >= left.value
            launch { left.animateTo(index.toFloat(), if (forward) behind else ahead) }
            launch { right.animateTo(index + 1f, if (forward) ahead else behind) }
        }
        Row(
            Modifier.fillMaxWidth().height(56.dp).takesTouches().clip(CircleShape).background(p.cell).border(1.dp, p.line, CircleShape).padding(5.dp)
                .drawBehind {
                    if (index < 0) return@drawBehind
                    val w = size.width / state.tabs.size
                    drawRoundRect(p.ink, Offset(left.value * w, 0f), Size((right.value - left.value) * w, size.height), CornerRadius(size.height / 2))
                },
        ) {
            for (tab in state.tabs) {
                val on = tab.route == state.current
                val ink by animateColorAsState(if (on) p.canvas else p.ink, label = "tab")
                val source = remember { MutableInteractionSource() }
                Row(
                    Modifier.weight(1f).fillMaxHeight().squish(source, 0.9f).clip(CircleShape).clickable(source, null) { state.go(tab.route) }
                        .semantics(mergeDescendants = true) { role = Role.Tab; selected = on; contentDescription = tab.label },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(tab.icon, null, tint = ink, modifier = Modifier.size(19.dp).popOn(on, 0.8f))
                    if (on) Text(" " + tab.label.substringAfterLast(' '), style = type.tab, color = ink, maxLines = 1)
                }
            }
        }
    }

    // Now Playing is the cover's colour, all of it: the page is the song.
    override fun nowPlaying(page: Palette) =
        Palette.make(canvas = page.accent, ink = page.onAccent, accent = page.onAccent, fill = page.onAccent.copy(alpha = 0.16f), line = page.onAccent.copy(alpha = 0.3f))

    override fun visualizer(np: Palette, page: Palette) = np.ink.copy(alpha = 0.45f)

    /** The colour, and the artist's name drifting across it huge and faint, two lines going opposite ways. */
    @Composable
    override fun NowPlayingBackground(art: String?) {
        val p = LocalPalette.current
        val now by LocalContext.current.graph.player.state.collectAsState()
        val name = (now.song?.artist ?: "Verza").uppercase() + "  ✱  "
        Box(Modifier.fillMaxSize().background(p.canvas)) {
            val ghost = type.masthead.copy(fontSize = 120.sp, lineHeight = 110.sp, drawStyle = Stroke(2.5f))
            Text(name.repeat(4), style = ghost, color = p.ink.copy(alpha = 0.1f), maxLines = 1, softWrap = false,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 70.dp).basicMarquee(iterations = Int.MAX_VALUE, velocity = 14.dp).clearAndSetSemantics { })
            Text(name.repeat(4), style = ghost, color = p.ink.copy(alpha = 0.1f), maxLines = 1, softWrap = false,
                modifier = Modifier.align(Alignment.Center).padding(bottom = 60.dp).graphicsLayer { scaleX = -1f }.basicMarquee(iterations = Int.MAX_VALUE, velocity = 10.dp).graphicsLayer { scaleX = -1f }.clearAndSetSemantics { })
        }
    }

    /** The cover as big as the room allows, straight, on a soft shadow, swelling a little on the bass. */
    override val bigCover = true

    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        Box(
            Modifier.graphicsLayer { scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX; translationY = -fly * size.height * 2f; alpha = 1f - fly }
                .size(side).shadow(28.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (label != null) Text(label, style = type.headline, color = Color.White)
        }
    }

    /** A wide pill of ink with the symbol in the page's colour; it squishes, and the symbol pops as it turns. */
    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        val p = LocalPalette.current
        val source = remember { MutableInteractionSource() }
        Box(
            Modifier.size(width = 124.dp, height = 76.dp).squish(source, 0.9f).clip(CircleShape).background(p.ink)
                .clickable(source, null, onClick = onClick).semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = p.canvas, modifier = Modifier.size(44.dp).popOn(playing)) }
    }

    /** A thick rounded track, filled as far as it's played, a fat dot riding it that grows under the finger. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val h = (6f + 4f * dragging) * density
        val top = Offset(0f, size.height / 2 - h / 2)
        drawRoundRect(p.ink.copy(alpha = 0.22f), top, Size(size.width, h), CornerRadius(h / 2))
        drawRoundRect(p.ink, top, Size(size.width * shown, h), CornerRadius(h / 2))
        val c = Offset(size.width * shown, size.height / 2)
        drawCircle(p.ink, (8f + 5f * dragging) * density, c)
        drawCircle(p.canvas, (3f + 2f * dragging) * density, c)
    }

    /** A sticker at the top: now playing, with bars dancing to it. */
    @Composable
    override fun NowPlayingDecor() {
        val p = LocalPalette.current
        val now by LocalContext.current.graph.player.state.collectAsState()
        Row(Modifier.padding(start = 24.dp, top = 50.dp), verticalAlignment = Alignment.CenterVertically) {
            Tag("Now playing", ground = p.ink, ink = p.canvas)
            Spacer(Modifier.width(8.dp))
            LiveBars(p.ink, now.playing, height = 14.dp)
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        val p = LocalPalette.current
        Column(modifier.background(p.canvas)) {
            Column(Modifier.fillMaxWidth().background(p.accent).padding(12.dp)) {
                Tag("On repeat", ground = p.onAccent, ink = p.accent)
                Text("Loud.", style = type.masthead.copy(fontSize = 40.sp, lineHeight = 40.sp), color = p.onAccent, modifier = Modifier.padding(top = 8.dp))
            }
            Canvas(Modifier.fillMaxWidth().height(12.dp)) { zigzagDown(p.accent) }
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 58.dp, height = 30.dp).background(p.ink, CircleShape))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(width = 40.dp, height = 30.dp).border(1.5.dp, p.ink, CircleShape))
            }
        }
    }
}
