package com.verza.ui.look

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.graph
import com.verza.playback.NowPlaying
import com.verza.ui.kit.Cover
import com.verza.ui.kit.GlassButton
import com.verza.ui.kit.glass
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.Corner
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import com.verza.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Liquid glass, after Apple Music (HIG, Materials and Tab bars): plain pages, and glass only for the
 * controls over them, stained with the playing song's colour where Apple's has none.
 */
object GlassLook : Look("glass", "Liquid glass", "Glass controls over plain pages, coloured by the song") {
    override fun palette(swatches: List<Color>, dark: Boolean) = Palette.from(swatches, dark)

    // Apple's text styles at their default size (HIG, Typography), set in Inter: SF Pro may not ship on Android.
    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.022).em),
        section = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.02).em),
        title = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.021).em),
        headline = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.013).em),
        rowTitle = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.013).em),
        rowSubtitle = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.009).em),
        body = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.011).em),
        small = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.003).em),
        tab = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.004.em),
        label = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    )
}

/** A page's name, large and bold, the Zune's dateline over it in the accent, [trailing] level with it. */
@Composable
internal fun GlassMasthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (dateline != null) Text(dateline, style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = p.accent)
            Text(LocalLook.current.case.of(title), style = Type.Masthead, color = p.ink, modifier = Modifier.semantics { heading() })
        }
        trailing?.invoke()
    }
}

@Composable
internal fun GlassSectionTitle(text: String, modifier: Modifier) {
    Text(LocalLook.current.case.of(text), style = Type.Section, color = LocalPalette.current.ink, modifier = modifier.padding(top = 26.dp, bottom = 10.dp).semantics { heading() })
}

/** Its words (and [icon]) in [color] on a capsule of the fill, as Apple Music's Play and Shuffle are. */
@Composable
internal fun GlassWordButton(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val ink = if (enabled) color else color.copy(alpha = 0.4f)
    Row(
        modifier.heightIn(min = 48.dp).clip(CircleShape).background(p.fill).clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(LocalLook.current.case.of(text), style = Type.Headline, color = ink, maxLines = 1)
    }
}

@Composable
internal fun GlassHairline(start: Dp) {
    Box(Modifier.fillMaxWidth().padding(start = start).height(0.5.dp).background(LocalPalette.current.line))
}

/** A row of covers across the page, each opening its page or playing its song. */
@Composable
internal fun GlassShelf(section: Section, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val player = LocalContext.current.graph.player
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(section.cards, key = { it.song?.id ?: it.page ?: it.title }) { card ->
            val round = card.page?.startsWith("UC") == true
            Column(Modifier.width(156.dp).clip(Corner).clickable { card.song?.let(player::playWithRadio) ?: card.page?.let(onOpen) }, horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start) {
                Cover(card.art?.let { artAt(it, 544) }, 156.dp, round = round)
                val small = Type.RowSubtitle.copy(fontSize = 14.sp, lineHeight = 18.sp)
                Text(cardTitle(card), style = small, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                Text(card.subtitle.replace(" • ", " · "), style = small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private val PlayerHeight = 56.dp
private val Gap = 8.dp

/** What floats at the foot: how far downloads have got, the playing song, then the tabs and search. */
@Composable
internal fun GlassDock(state: DockState, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = Gap), verticalArrangement = Arrangement.spacedBy(Gap)) {
        state.downloads?.let { DownloadsLine(it) }
        state.now.song?.let { GlassMiniPlayer(state.now, state.openPlayer) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassTabBar(state.tabs.filterNot { it.search }, state.current, state.go, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            // On the search page its circle is stained like a chosen tab.
            val search = state.tabs.first { it.search }
            GlassButton(search.icon, search.label, size = BarHeight, stain = if (state.current == search.route) p.accent else null, modifier = Modifier.takesTouches()) { state.go(search.route) }
        }
    }
}

/** One quiet line: "Downloading 3 of 12 · Yellow". */
@Composable
internal fun DownloadsLine(text: String, color: Color = LocalPalette.current.inkMuted) {
    Text(text, style = Type.Small, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))
}

/**
 * The tabs on a capsule of glass. The chosen one sits in a drop of glass stained with the accent, and
 * moving to another, the drop's ends chase it at different speeds: it stretches toward the new tab and
 * settles round there, the way a drop of water runs.
 */
@Composable
private fun GlassTabBar(tabs: List<Tab>, current: String?, go: (String) -> Unit, modifier: Modifier) {
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
    Row(
        modifier.height(BarHeight).takesTouches().glass(CircleShape, BarHeight / 2).padding(4.dp)
            .drawBehind {
                if (shown.value <= 0f) return@drawBehind
                val w = size.width / tabs.size
                val start = Offset(left.value * w, 0f)
                val drop = Size((right.value - left.value) * w, size.height)
                val round = CornerRadius(size.height / 2)
                drawRoundRect(Brush.verticalGradient(
                    0f to p.accent.copy(alpha = (if (p.dark) 0.5f else 0.3f) * shown.value),
                    1f to p.accent.copy(alpha = (if (p.dark) 0.3f else 0.16f) * shown.value),
                ), start, drop, round)
                drawRoundRect(p.accent.copy(alpha = 0.45f * shown.value), start, drop, round, style = Stroke(1.dp.toPx()))
            },
    ) {
        for (tab in tabs) {
            val on = tab.route == current
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape).clickable { go(tab.route) }
                    .semantics(mergeDescendants = true) { role = Role.Tab; selected = on },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(tab.icon, null, tint = p.ink, modifier = Modifier.size(25.dp))
                Text(tab.label, style = Type.Tab, color = p.ink, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
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
 * The playing song in a capsule of glass over the tabs; a tap raises Now Playing. The glass holds the
 * cover's colours, and its rim fills with the accent as the song plays, from the top left round.
 */
@Composable
private fun GlassMiniPlayer(now: NowPlaying, onOpen: () -> Unit) {
    val song = now.song ?: return
    val p = LocalPalette.current
    val context = LocalContext.current
    val player = context.graph.player
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // A cover twelve pixels wide, stretched across the capsule, is its colours and nothing else.
    // Exactly twelve: a larger copy from the memory cache would show the picture.
    val colours = coil3.compose.rememberAsyncImagePainter(
        coil3.request.ImageRequest.Builder(context).data(song.art?.let { artAt(it, 60) }).size(12).precision(coil3.size.Precision.EXACT).build(),
    )
    val played by playedFraction(now)
    Row(
        Modifier.fillMaxWidth().height(PlayerHeight).takesTouches()
            .glass(CircleShape, PlayerHeight / 2, pressed, inside = { with(colours) { draw(size, alpha = if (p.dark) 0.45f else 0.38f) } })
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
        Cover(song.art?.let { artAt(it, 226) }, 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(com.verza.data.shownTitle(song.title, song.artist), style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = Type.Small, color = p.ink.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PlayPauseNext(now, p.ink)
    }
}

/** Play or pause, then next: the mini player's two buttons, in every look. */
@Composable
internal fun PlayPauseNext(now: NowPlaying, tint: Color, size: Dp = 30.dp) {
    val player = LocalContext.current.graph.player
    IconButton(onClick = player::toggle) {
        Icon(if (now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (now.playing) "Pause" else "Play", tint = tint, modifier = Modifier.size(size))
    }
    IconButton(onClick = player::next) { Icon(Icons.Rounded.SkipNext, "Next", tint = tint, modifier = Modifier.size(size)) }
}

@Composable
internal fun GlassNowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
    com.verza.ui.nowplaying.FlyingCover(art, side, swell, fly, label, Modifier.graphicsLayer { scaleX = rest; scaleY = rest })
}

/** Play and pause on a disc of glass stained with the song's colour: the one control that matters most. */
@Composable
internal fun GlassPlayButton(playing: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    GlassButton(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Play", size = 84.dp, iconSize = 50.dp, tint = p.ink, stain = p.accent, onClick = onClick)
}

/** A capsule that fills as the song plays; the played part ends square against the rest, as Apple's does. */
internal fun DrawScope.glassScrubber(shown: Float, dragging: Float, p: Palette) {
    val w = (7f + 5f * dragging) * density
    val top = Offset(0f, size.height / 2 - w / 2)
    val track = Size(size.width, w)
    val round = CornerRadius(w / 2)
    drawRoundRect(p.line, top, track, round)
    clipRect(right = size.width * shown) { drawRoundRect(p.ink.copy(alpha = 0.86f), top, track, round) }
}

/** A look's card in Settings when it draws none of its own: its ground, its type, its accent. */
@Composable
internal fun DefaultPreview(look: Look, modifier: Modifier) {
    val p = LocalPalette.current
    Box(modifier.background(p.canvas)) {
        look.Backdrop(Modifier.fillMaxSize())
        Column(Modifier.padding(12.dp)) {
            Text(look.case.of("Aa"), style = look.type.masthead.copy(fontSize = 34.sp, lineHeight = 36.sp), color = p.ink)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(36.dp).clip(look.cover(36.dp)).background(p.accent))
        }
    }
}
