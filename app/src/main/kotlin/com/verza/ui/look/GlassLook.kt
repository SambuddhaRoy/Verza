package com.verza.ui.look

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.data.mixes.StatsRepository
import com.verza.graph
import com.verza.playback.NowPlaying
import com.verza.ui.kit.Cover
import com.verza.ui.kit.bouncy
import com.verza.ui.kit.enter
import com.verza.ui.kit.glass
import com.verza.ui.kit.popOn
import com.verza.ui.kit.squish
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import com.verza.ui.theme.Type
import com.verza.ui.theme.coverSwatches
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Glass: hyper-minimal, after the quiet sites Httpster files under Minimal (Drams' Rams-inspired pages,
 * Middle Name, Mater, Aather). The page is one flat material, stone by day and black at night. What
 * floats over the pages (the dock, the buttons over a cover, all of Now Playing's) is fluted glass, its
 * flutes faint until pressed; everything else is flat: covers, groups of rows, and the buttons in a
 * page, which are a pill of faint ink (or solid ink, for the one to press).
 *
 * One face, Inter: bold at its display size for what names things (pages, sections, songs, records),
 * its text size for reading and pressing. Until 2.8.0 this was Soft glass: controls were the page's
 * material raised out of it and sinking under a finger (neumorphism), names were serifs, and a level
 * meter sat under the cover. The listener asked for the glass alone, a bold sans, and less going on.
 * The song's colour is spent in a few places only: Home's play key, the played part of the mini
 * player's rim, a toggle that's on. Since 2.7.1,
 * the spaced-out capitals, the dot after every name, the numbered shelves, the counting figures and the
 * light that followed the phone's tilt went.
 */
object GlassLook : Look("soft", "Glass", "Flat pages, fluted glass over them, one bold sans") {
    override fun palette(swatches: List<Color>, dark: Boolean): Palette {
        // Night is pitch black (the listener's ask). A shadow can't show on black, so there a raised
        // surface (cell) is a shade above it and lit at its rim; by day it is the page's own stone.
        val canvas = if (dark) Color.Black else Color(0xFFE9E7E3)
        val ink = if (dark) Color(0xFFEEEDEA) else Color(0xFF151515)
        return Palette.make(canvas, ink, swatches, cell = if (dark) Color(0xFF141416) else canvas, grouped = canvas, surface = lerp(canvas, ink, 0.05f))
    }

    override fun cover(size: Dp): Shape = RoundedCornerShape(if (size in 1.dp..64.dp) 8.dp else 14.dp)
    override val group: Shape = RoundedCornerShape(24.dp)

    /** Now Playing is the page's material too, whatever the theme; the cover brings the colour. */
    override fun nowPlaying(page: Palette) = page

    /**
     * Behind every page, the playing song's cover flowing through fluted glass as on Now Playing, but
     * faint and vignetted into the page's colour, so it tints the page without being looked at. Slower,
     * and at fifteen frames a second (`ambient`).
     */
    @Composable override fun Backdrop(modifier: Modifier) {
        val p = LocalPalette.current
        val now by LocalContext.current.graph.player.state.collectAsState()
        val art = now.song?.art ?: return
        Box(modifier) {
            com.verza.ui.nowplaying.FlowingCover(artAt(art, 226), p.dark, Modifier.graphicsLayer { alpha = 0.2f }, fluted = true, ambient = true)
            Box(Modifier.fillMaxSize().drawBehind {
                val centre = Offset(size.width / 2, size.height * 0.3f)
                drawRect(Brush.radialGradient(
                    0f to p.canvas.copy(alpha = 0.15f), 0.3f to p.canvas.copy(alpha = 0.5f), 0.65f to p.canvas.copy(alpha = 0.95f), 1f to p.canvas,
                    center = centre, radius = maxOf(size.width, size.height) * 0.65f,
                ))
            })
        }
    }
    /** The cover flowing, seen through fluted glass, slowly. (It used to stir with the bass when the meter was off.) */
    @Composable override fun NowPlayingBackground(art: String?) =
        com.verza.ui.nowplaying.FlowingCover(art, LocalPalette.current.dark, fluted = true)

    private val figures = "tnum"

    // One face: Inter bold at its display size for what names things (pages, sections, songs), at its
    // text size for everything you read or press. Five styles on a page at most; no spaced-out capitals.
    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.022).em),
        section = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.016).em),
        title = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em),
        headline = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.011).em),
        rowTitle = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.011).em),
        rowSubtitle = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = (-0.006).em),
        body = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = (-0.011).em),
        small = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = figures),
        tab = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.02.em),
    )
}

/** A page's name in the serif; Home's dateline ("Thursday evening") over it, as the Zune's pages had. */
@Composable
internal fun GlassMasthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(start = 24.dp, end = 18.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (dateline != null) Text(dateline, style = Type.Small.copy(fontSize = 15.sp, lineHeight = 20.sp), color = p.inkMuted)
            Text(LocalLook.current.case.of(title), style = Type.Masthead, color = p.ink, modifier = Modifier.semantics { heading() })
        }
        trailing?.invoke()
    }
}

/** A heading inside a page, in the serif: "Recently played". */
@Composable
internal fun GlassSectionTitle(text: String, modifier: Modifier) {
    Text(
        LocalLook.current.case.of(text), style = Type.Section, color = LocalPalette.current.ink, maxLines = 1,
        modifier = modifier.padding(top = 30.dp, bottom = 10.dp).semantics { heading() },
    )
}

/**
 * A flat pill with its words, of faint ink; the one to press ([color] the accent) is solid ink, so it
 * reads first without spending the song's colour. It gives a little under the finger.
 */
@Composable
internal fun GlassWordButton(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val primary = color == p.accent
    val ink = (if (primary) p.canvas else p.ink).copy(alpha = if (enabled) 1f else 0.4f)
    val source = remember { MutableInteractionSource() }
    val tick = com.verza.ui.kit.rememberTick()
    Row(
        modifier.heightIn(min = 46.dp).squish(source, 0.96f).clip(CircleShape).background(if (primary) p.ink else p.fill)
            .clickable(source, null, enabled = enabled) { tick(); onClick() }
            .semantics { role = Role.Button }.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(LocalLook.current.case.of(text), style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The line between rows: one hairline. (It was cut into the page, a shadow over a highlight.) */
@Composable
internal fun GlassHairline(start: Dp) =
    Box(Modifier.padding(start = start).fillMaxWidth().height(0.5.dp).background(LocalPalette.current.line))

/**
 * A shelf: covers across, flat on the page like prints laid on a table (shadows are for what you press),
 * the name under each. An artist is round, its name centred.
 */
@Composable
internal fun GlassShelf(section: Section, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(section.cards, key = { i, _ -> i }) { _, card ->
            val round = card.page?.startsWith("UC") == true
            val source = remember { MutableInteractionSource() }
            val align = if (round) TextAlign.Center else TextAlign.Start
            Column(
                Modifier.width(148.dp).squish(source, 0.97f).clickable(source, null, onClick = cardAction(card, onOpen)),
                horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                Cover(card.art?.let { artAt(it, 544) }, 148.dp, round = round)
                Text(cardTitle(card), style = Type.Headline.copy(fontSize = 14.sp, lineHeight = 19.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align, modifier = Modifier.padding(top = 10.dp))
                Text(card.subtitle.substringBefore(" • "), style = Type.Small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align)
            }
        }
    }
}

/** A card's name as shown: a song's title cleaned of "(Official Video)" and the like. */
internal fun cardTitle(card: com.verza.data.Card) =
    if (card.song != null) com.verza.data.shownTitle(card.title, card.subtitle.substringBefore(" • ")) else card.title

/** Plays what a card is: a song with its radio, anything else opens its page. */
@Composable
internal fun cardAction(card: com.verza.data.Card, onOpen: (String) -> Unit): () -> Unit {
    val player = LocalContext.current.graph.player
    return { card.song?.let(player::playWithRadio) ?: card.page?.let(onOpen); Unit }
}

private val BarHeight = 62.dp
private val PlayerHeight = 64.dp

/** What floats at the foot: how far downloads have got, the playing song, then the tabs, each in fluted glass. */
@Composable
internal fun GlassDock(state: DockState, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.downloads?.let { DownloadsLine(it) }
        AnimatedVisibility(
            state.now.song != null,
            enter = expandVertically(bouncy(), expandFrom = Alignment.Bottom) + fadeIn() + scaleIn(bouncy(), initialScale = 0.85f),
            exit = shrinkVertically() + fadeOut(),
        ) { GlassMiniPlayer(state.now, state.openPlayer) }
        GlassTabBar(state.tabs, state.current, state.go)
    }
}

/** One quiet line: "Downloading 3 of 12 · Yellow". */
@Composable
internal fun DownloadsLine(text: String, color: Color = LocalPalette.current.inkMuted) {
    Text(text, style = Type.Small, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))
}

/**
 * The tabs as symbols on a capsule of fluted glass. The chosen one sits on a pill of faint ink; moving
 * to another, the pill's ends chase it at different speeds, so it stretches toward the new tab and
 * settles there, the way a drop runs.
 */
@Composable
private fun GlassTabBar(tabs: List<Tab>, current: String?, go: (String) -> Unit) {
    val p = LocalPalette.current
    val index = tabs.indexOfFirst { it.route == current }
    val left = remember { Animatable(index.coerceAtLeast(0).toFloat()) }
    val right = remember { Animatable(index.coerceAtLeast(0) + 1f) }
    val shown = remember { Animatable(if (index >= 0) 1f else 0f) }
    LaunchedEffect(index) {
        if (index < 0) { shown.animateTo(0f, tween(180)); return@LaunchedEffect }
        if (shown.value == 0f) { left.snapTo(index.toFloat()); right.snapTo(index + 1f) }
        val ahead = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        val behind = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
        val forward = index >= left.value
        launch { shown.animateTo(1f, tween(180)) }
        launch { left.animateTo(index.toFloat(), if (forward) behind else ahead) }
        launch { right.animateTo(index + 1f, if (forward) ahead else behind) }
    }
    // One source for every tab: holding any of them presses the capsule's glass, so its flutes move.
    val source = remember { MutableInteractionSource() }
    val held by source.collectIsPressedAsState()
    val tick = com.verza.ui.kit.rememberTick(android.view.HapticFeedbackConstants.CLOCK_TICK)
    Row(
        Modifier.height(BarHeight).takesTouches().glass(CircleShape, BarHeight / 2, held).padding(6.dp)
            .drawBehind {
                if (shown.value <= 0f) return@drawBehind
                val w = size.width / tabs.size
                val inset = 2.dp.toPx()
                val well = Rect(left.value * w + inset, inset, right.value * w - inset, size.height - inset)
                drawRoundRect(p.fill, well.topLeft, well.size, CornerRadius(well.height / 2), alpha = shown.value)
            },
    ) {
        for (tab in tabs) {
            val on = tab.route == current
            Box(
                Modifier.size(width = 66.dp, height = BarHeight - 12.dp).clip(CircleShape).clickable(source, null) { if (!on) tick(); go(tab.route) }
                    .semantics { role = Role.Tab; selected = on; contentDescription = tab.label },
                contentAlignment = Alignment.Center,
            ) { Icon(tab.icon, null, tint = if (on) p.ink else p.inkMuted, modifier = Modifier.size(23.dp)) }
        }
    }
}

/** How far the playing song has got, 0 to 1, polled while it plays: a tick is a redraw only. */
@Composable
internal fun playedFraction(now: NowPlaying): androidx.compose.runtime.State<Float> {
    val player = LocalContext.current.graph.player
    val played = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(now.song?.id, now.playing, now.durationMs) {
        while (true) {
            played.floatValue = if (now.durationMs > 0) (player.position.toFloat() / now.durationMs).coerceIn(0f, 1f) else 0f
            if (!now.playing) break
            delay(500)
        }
    }
    return played
}

/**
 * The playing song in a capsule of fluted glass over the tabs, its cover round; a tap raises Now
 * Playing. The rim fills with the song's colour as it plays, from the top left round.
 */
@Composable
private fun GlassMiniPlayer(now: NowPlaying, onOpen: () -> Unit) {
    val song = now.song ?: return
    val p = LocalPalette.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val played by playedFraction(now)
    Row(
        Modifier.fillMaxWidth().height(PlayerHeight).takesTouches()
            .glass(CircleShape, PlayerHeight / 2, pressed)
            .drawWithCache {
                val inset = 1.dp.toPx()
                val h = size.height
                val rim = Path().apply {
                    moveTo(h / 2, inset)
                    lineTo(size.width - h / 2, inset)
                    arcTo(Rect(size.width - h + inset, inset, size.width - inset, h - inset), -90f, 180f, false)
                    lineTo(h / 2, h - inset)
                    arcTo(Rect(inset, inset, h - inset, h - inset), 90f, 180f, false)
                }
                val measure = PathMeasure().apply { setPath(rim, false) }
                val part = Path()
                val line = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                onDrawWithContent {
                    drawContent()
                    part.reset()
                    if (measure.getSegment(0f, measure.length * played, part, true)) drawPath(part, p.accent, style = line)
                }
            }
            .clickable(source, null, onClick = onOpen)
            .semantics { contentDescription = "Now playing: ${song.title}" }
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            song, label = "song", modifier = Modifier.weight(1f), contentKey = { it.id },
            transitionSpec = { (slideInVertically(bouncy()) { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
        ) { shown ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(shown.art?.let { artAt(it, 226) }, 48.dp, round = true)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(com.verza.data.shownTitle(shown.title, shown.artist), style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(shown.artist, style = Type.Small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        PlayPauseNext(now, p.ink)
    }
}

/** Play or pause, then next: the mini player's two buttons. */
@Composable
internal fun PlayPauseNext(now: NowPlaying, tint: Color, size: Dp = 30.dp) {
    val player = LocalContext.current.graph.player
    IconButton(onClick = player::toggle) {
        Icon(if (now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (now.playing) "Pause" else "Play", tint = tint, modifier = Modifier.size(size).popOn(now.playing))
    }
    IconButton(onClick = player::next) { Icon(Icons.Rounded.SkipNext, "Next", tint = tint, modifier = Modifier.size(size)) }
}

@Composable
internal fun GlassNowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
    com.verza.ui.nowplaying.FlyingCover(art, side, swell, fly, label, Modifier.graphicsLayer { scaleX = rest; scaleY = rest })
}

/**
 * Play and pause on Now Playing: a larger key of the same glass, smoked with ink, so it stands apart
 * from back and on without the song's colour (which the listener wanted off it).
 */
@Composable
internal fun GlassPlayButton(playing: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    com.verza.ui.kit.GlassButton(
        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Play",
        size = 74.dp, iconSize = 36.dp, tint = p.canvas, iconModifier = Modifier.popOn(playing),
        inside = { drawRect(p.ink, alpha = 0.92f) }, onClick = onClick,
    )
}

/**
 * A thin capsule over the moving ground, the played part in ink, thicker under the finger. (It was a
 * groove cut into the page, which a moving ground doesn't have.)
 */
internal fun DrawScope.glassScrubber(shown: Float, dragging: Float, p: Palette) {
    val h = (5f + 5f * dragging) * density
    val top = Offset(0f, size.height / 2 - h / 2)
    val track = Size(size.width, h)
    val round = CornerRadius(h / 2)
    drawRoundRect(p.ink.copy(alpha = 0.18f), top, track, round)
    clipRect(right = size.width * shown) { drawRoundRect(p.ink.copy(alpha = 0.9f), top, track, round) }
}
