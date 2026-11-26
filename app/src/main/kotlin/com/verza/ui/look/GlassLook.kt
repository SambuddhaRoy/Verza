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
import com.verza.ui.kit.Etched
import com.verza.ui.kit.SoftKey
import com.verza.ui.kit.bouncy
import com.verza.ui.kit.enter
import com.verza.ui.kit.glass
import com.verza.ui.kit.popOn
import com.verza.ui.kit.pressOf
import com.verza.ui.kit.raise
import com.verza.ui.kit.sink
import com.verza.ui.kit.soft
import com.verza.ui.kit.squish
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import com.verza.ui.theme.Type
import com.verza.ui.theme.coverSwatches
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Soft glass: hyper-minimal, after the quiet sites Httpster files under Minimal (Drams' Rams-inspired
 * pages, Middle Name, Heartbeat's drum machine, The Index). The page is one warm material, stone by
 * day and graphite by night; controls are that material raised out of it or pressed into it
 * (`ui/kit/Soft.kt`), and what floats over the pages (the dock, the buttons over a cover) is frosted
 * glass of the same material. Words are few and small: names in a light large face, sections as small
 * capitals on a line cut into the page, figures as figures. Colour is the song's, and it is spent
 * sparingly: the dot by a page's name, the lamp in the chosen tab, the played part of a song, and Home's
 * play key, as Braun's calculators had one orange key. Now Playing's play key is ink.
 */
object GlassLook : Look("soft", "Soft glass", "One soft material, glass over it, and one coloured key") {
    override fun palette(swatches: List<Color>, dark: Boolean): Palette {
        val canvas = if (dark) Color(0xFF1C1C1E) else Color(0xFFE9E7E3)
        val ink = if (dark) Color(0xFFEEEDEA) else Color(0xFF151515)
        // Rows, groups and menus are the page's own material, set apart by their shadows, not a fill.
        return Palette.make(canvas, ink, swatches, cell = canvas, grouped = canvas, surface = lerp(canvas, ink, 0.05f))
    }

    override fun cover(size: Dp): Shape = RoundedCornerShape(if (size in 1.dp..64.dp) 8.dp else 14.dp)
    override val group: Shape = RoundedCornerShape(24.dp)

    /** Now Playing is the page's material too, whatever the theme; the cover brings the colour. */
    override fun nowPlaying(page: Palette) = page

    /** The meter under the cover is ink: Now Playing's colour is the cover's own glow. */
    override fun visualizer(np: Palette, page: Palette) = np.ink

    @Composable override fun HomeHero() = PlayerPlate()
    @Composable override fun NowPlayingBackground(art: String?) = CoverGlow(art)

    private val figures = "tnum, ss01"

    // Inter. Names large and light (500, tight), sections small capitals spaced out, figures tabular.
    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.Medium, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-0.045).em),
        section = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.025).em),
        title = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.03).em),
        headline = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.011).em),
        rowTitle = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.011).em),
        rowSubtitle = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = (-0.006).em),
        body = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = (-0.011).em),
        small = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = figures),
        tab = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.02.em),
        label = TextStyle(fontFamily = Fonts.InterText, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.11.em, fontFeatureSettings = figures),
    )
}

/** A figure set large, for the plate's counts. */
private val Figure: TextStyle
    @Composable get() = Type.Section.copy(fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.03).em, fontFeatureSettings = "tnum")

/** A page's name, large and light, the song's colour in a dot after it; the dateline over it in small capitals. */
@Composable
internal fun GlassMasthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(start = 24.dp, end = 18.dp, top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (dateline != null) Text(dateline.uppercase(), style = Type.Label, color = p.inkMuted, modifier = Modifier.padding(bottom = 8.dp).semantics { contentDescription = dateline })
            Row(verticalAlignment = Alignment.Top) {
                Text(LocalLook.current.case.of(title), style = Type.Masthead, color = p.ink, modifier = Modifier.weight(1f, fill = false).semantics { heading() })
                Box(Modifier.padding(start = 6.dp, top = 10.dp).size(8.dp).background(p.accent, CircleShape).clearAndSetSemantics { })
            }
        }
        trailing?.invoke()
    }
}

/** Small capitals, then a line cut into the page to the edge: "RECENTLY PLAYED ————". */
@Composable
internal fun GlassSectionTitle(text: String, modifier: Modifier) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(top = 34.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = Type.Label, color = p.ink, maxLines = 1, modifier = Modifier.semantics { heading(); contentDescription = text })
        Etched(Modifier.padding(start = 14.dp).weight(1f))
    }
}

/**
 * A raised pill of the page's material with its words; pressed, it sinks into the page. The one to
 * press ([color] the accent) is ink, so it reads first without spending the song's colour.
 */
@Composable
internal fun GlassWordButton(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val primary = color == p.accent
    val ink = (if (primary) p.canvas else p.ink).copy(alpha = if (enabled) 1f else 0.4f)
    val source = remember { MutableInteractionSource() }
    val press = pressOf(source)
    Row(
        modifier.heightIn(min = 50.dp).soft(CircleShape, 5.dp, press, face = if (primary) p.ink else null).clip(CircleShape)
            .clickable(source, null, enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }.padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(LocalLook.current.case.of(text), style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun GlassHairline(start: Dp) = Etched(Modifier.padding(start = start))

/**
 * A shelf: covers across, each raised a little off the page, numbered under it as an index is. An
 * artist is round and unnumbered, its name centred.
 */
@Composable
internal fun GlassShelf(section: Section, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    // The padding is the shadows' room: a LazyRow cuts off whatever is drawn outside it.
    LazyRow(contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        itemsIndexed(section.cards, key = { i, _ -> i }) { i, card ->
            val round = card.page?.startsWith("UC") == true
            val source = remember { MutableInteractionSource() }
            val shape = if (round) CircleShape else RoundedCornerShape(14.dp)
            Column(
                Modifier.width(148.dp).squish(source, 0.97f).clickable(source, null, onClick = cardAction(card, onOpen)),
                horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                Box(Modifier.size(148.dp).soft(shape, 7.dp)) { Cover(card.art?.let { artAt(it, 544) }, 148.dp, round = round) }
                Row(Modifier.padding(top = 12.dp)) {
                    if (!round) Text("%02d".format(i + 1), style = Type.Label, color = p.inkMuted, modifier = Modifier.padding(top = 3.dp, end = 8.dp).clearAndSetSemantics { })
                    Column(horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start) {
                        val align = if (round) TextAlign.Center else TextAlign.Start
                        Text(cardTitle(card), style = Type.Headline.copy(fontSize = 14.sp, lineHeight = 19.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align)
                        Text(card.subtitle.substringBefore(" • "), style = Type.Small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align)
                    }
                }
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

/**
 * Home's one object, like a player on a shelf: a raised plate holding the song playing (or last
 * played) with the play key, the one coloured key, and under a cut line your figures: plays, days in a
 * row, and who you've played most lately.
 */
@Composable
private fun PlayerPlate() {
    val graph = LocalContext.current.graph
    val now by graph.player.state.collectAsState()
    val recent by graph.library.recent.collectAsState(initial = emptyList())
    val stats = remember { StatsRepository(graph.db.stats()) }
    val plays by stats.totalPlays.collectAsState(initial = 0)
    val days by stats.playDays.collectAsState(initial = emptyList())
    val artists by remember { stats.topArtists(1) }.collectAsState(initial = emptyList())
    val streak = com.verza.ui.library.streak(days)
    val p = LocalPalette.current
    val song = now.song ?: recent.firstOrNull()
    val art = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).enter(0)
            .soft(RoundedCornerShape(30.dp), 9.dp).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp).soft(art, 5.dp)) {
                if (song?.art != null) AsyncImage(artAt(song.art!!, 226), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(art))
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (now.playing) Box(Modifier.padding(end = 7.dp).size(6.dp).background(p.accent, CircleShape))
                    val state = when { now.playing -> "Now playing"; now.song != null -> "Paused"; song != null -> "Last played"; else -> "Nothing yet" }
                    Text(state.uppercase(), style = Type.Label, color = p.inkMuted, modifier = Modifier.semantics { contentDescription = state })
                }
                Text(song?.let { com.verza.data.shownTitle(it.title, it.artist) } ?: "Play something", style = Type.Headline.copy(fontSize = 18.sp, lineHeight = 23.sp), color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                if (song != null) Text(song.artist, style = Type.Small.copy(fontSize = 14.sp, lineHeight = 19.sp), color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (song != null) SoftKey(
                if (now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (now.playing) "Pause" else "Play",
                size = 56.dp, face = p.accent, iconModifier = Modifier.popOn(now.playing),
            ) { if (now.song != null) graph.player.toggle() else graph.player.play(listOf(song)) }
        }
        Etched(Modifier.padding(top = 18.dp, bottom = 16.dp))
        Row {
            // The count runs up to its figure when it loads.
            val shown by animateIntAsState(plays, tween(900), label = "plays")
            Stat("%,d".format(shown), "Plays", Modifier.weight(1f))
            Stat("$streak", if (streak == 1) "Day in a row" else "Days in a row", Modifier.weight(1f))
            Stat(artists.firstOrNull()?.artist ?: "–", "Most lately", Modifier.weight(1.3f))
        }
    }
}

/** A figure over its name in small capitals. */
@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier.padding(end = 8.dp).semantics(mergeDescendants = true) { contentDescription = "$label: $value" }) {
        Text(value, style = Figure, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label.uppercase(), style = Type.Label.copy(fontSize = 10.sp), color = p.inkMuted, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

private val BarHeight = 62.dp
private val PlayerHeight = 64.dp

/** What floats at the foot: how far downloads have got, the playing song, then the tabs, each in frosted glass. */
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
 * The tabs as symbols on a capsule of frosted glass. The chosen one sits in a well pressed into the
 * glass with a lamp of the song's colour under it; moving to another, the well's ends chase it at
 * different speeds, so it stretches toward the new tab and settles there, the way a drop runs.
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
    Row(
        Modifier.height(BarHeight).takesTouches().glass(CircleShape, BarHeight / 2).padding(6.dp)
            .drawBehind {
                if (shown.value <= 0f) return@drawBehind
                val w = size.width / tabs.size
                val inset = 2.dp.toPx()
                val well = Rect(left.value * w + inset, inset, right.value * w - inset, size.height - inset)
                val path = Path().apply { addRoundRect(RoundRect(well, CornerRadius(well.height / 2))) }
                drawPath(path, p.canvas.copy(alpha = 0.55f * shown.value))
                sink(path, 5.dp.toPx(), p, shown.value)
                drawCircle(p.accent.copy(alpha = shown.value), 2.5.dp.toPx(), Offset(well.center.x, well.bottom - 7.dp.toPx()))
            },
    ) {
        for (tab in tabs) {
            val on = tab.route == current
            Box(
                Modifier.size(width = 66.dp, height = BarHeight - 12.dp).clip(CircleShape).clickable { go(tab.route) }
                    .semantics { role = Role.Tab; selected = on; contentDescription = tab.label },
                contentAlignment = Alignment.Center,
            ) { Icon(tab.icon, null, tint = if (on) p.ink else p.inkMuted, modifier = Modifier.size(23.dp).offset(y = if (on) (-2).dp else 0.dp)) }
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
 * The playing song in a capsule of frosted glass over the tabs, its cover round; a tap raises Now
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
            song.id, label = "song", modifier = Modifier.weight(1f),
            transitionSpec = { (slideInVertically(bouncy()) { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
        ) { _ ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(song.art?.let { artAt(it, 226) }, 48.dp, round = true)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(com.verza.data.shownTitle(song.title, song.artist), style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = Type.Small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

/**
 * Now Playing's ground: the page's material, with the cover's colours glowing behind where the cover
 * sits and gone by the controls, so the keys stand on plain material as soft surfaces need.
 */
@Composable
private fun CoverGlow(art: String?) {
    val p = LocalPalette.current
    val colours = coverSwatches(art).ifEmpty { listOf(p.accent) }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(p.canvas)
        val strength = if (p.dark) 0.3f else 0.34f
        val spots = listOf(Offset(0.25f, 0.22f), Offset(0.8f, 0.3f), Offset(0.55f, 0.08f))
        for ((i, at) in spots.withIndex()) {
            val c = colours[i % colours.size]
            val centre = Offset(size.width * at.x, size.height * at.y)
            val r = size.width * 0.75f
            drawCircle(Brush.radialGradient(listOf(c.copy(alpha = strength), Color.Transparent), centre, r), r, centre)
        }
    }
}

@Composable
internal fun GlassNowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
    com.verza.ui.nowplaying.FlyingCover(art, side, swell, fly, label, Modifier.graphicsLayer { scaleX = rest; scaleY = rest })
}

/**
 * Play and pause on Now Playing: a larger key in ink, the page's own colours reversed, so it stands
 * apart from back and on without the song's colour (which the listener wanted off it).
 */
@Composable
internal fun GlassPlayButton(playing: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    SoftKey(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Play", size = 84.dp, iconSize = 42.dp, face = p.ink, tint = p.canvas, iconModifier = Modifier.popOn(playing), onClick = onClick)
}

/**
 * A groove cut into the page, the played part of it in the song's colour, and a bead of the material
 * riding in it at the playhead, larger under the finger.
 */
internal fun DrawScope.glassScrubber(shown: Float, dragging: Float, p: Palette) {
    val h = (6f + 2f * dragging) * density
    val top = size.height / 2 - h / 2
    val groove = Path().apply { addRoundRect(RoundRect(Rect(0f, top, size.width, top + h), CornerRadius(h / 2))) }
    val x = size.width * shown
    clipRect(right = x) { drawPath(groove, p.accent) }
    sink(groove, 3f * density, p)
    val r = (8f + 3f * dragging) * density
    val bead = Path().apply { addOval(Rect(Offset(x.coerceIn(r, size.width - r), size.height / 2), r)) }
    raise(bead, p.canvas, 3f * density, p)
}
