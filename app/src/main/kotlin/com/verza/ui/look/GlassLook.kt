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
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.verza.ui.kit.GlassButton
import com.verza.ui.kit.Glassed
import com.verza.ui.kit.LocalTilt
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sign

/**
 * Liquid glass, after Apple Music (HIG, Materials and Tab bars): glass for the controls over the
 * pages, stained with the playing song's colour where Apple's has none.
 *
 * The pages are laid out as the Trend mix look was (after Brand Appart), which the listener kept the
 * layout of when that look went: a heavy name with a © after it, Home's bento of what's playing and
 * your figures with a charm hanging on a keychain, shelves as a deck of cards to swipe, buttons with
 * an arrow in a circle at their end, and the dock as two capsules, the song over the tabs. Now
 * Playing is glass's own.
 */
object GlassLook : Look("glass", "Liquid glass", "Glass over a bento of what you play, coloured by the song") {
    override fun palette(swatches: List<Color>, dark: Boolean) = Palette.from(swatches, dark)

    override fun cover(size: Dp): Shape = RoundedCornerShape(if (size in 1.dp..64.dp) 10.dp else 20.dp)
    override val group: Shape = RoundedCornerShape(26.dp)

    @Composable override fun HomeHero() = GlassBento()

    // Inter, as Apple's text styles (HIG, Typography; SF Pro may not ship on Android), with Trend mix's
    // heavy, tight names. The title stays Apple's: Now Playing sets the song in it.
    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-0.045).em),
        section = TextStyle(fontFamily = Fonts.InterDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.035).em),
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

private val Tile = RoundedCornerShape(26.dp)

/** A page's name, heavy and tight, a © in a bead of the accent after it; the dateline over it, [trailing] level with it. */
@Composable
internal fun GlassMasthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (dateline != null) Text(dateline, style = Type.Small, color = p.inkMuted)
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 2.dp)) {
                Text(LocalLook.current.case.of(title), style = Type.Masthead, color = p.ink, modifier = Modifier.weight(1f, fill = false).semantics { heading() })
                Box(Modifier.padding(start = 3.dp, top = 4.dp).size(20.dp).background(p.accent, CircleShape).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                    Text("©", style = Type.Label.copy(fontSize = 13.sp, lineHeight = 13.sp), color = p.onAccent)
                }
            }
        }
        trailing?.invoke()
    }
}

/** Two tones: the first word in ink, the rest quieter ("Recently played"). */
@Composable
internal fun GlassSectionTitle(text: String, modifier: Modifier) {
    val p = LocalPalette.current
    val words = LocalLook.current.case.of(text).split(' ', limit = 2)
    Text(
        buildAnnotatedString {
            append(words[0])
            if (words.size > 1) withStyle(SpanStyle(color = p.inkMuted)) { append(" " + words[1]) }
        },
        style = Type.Section, color = p.ink, modifier = modifier.padding(top = 30.dp, bottom = 14.dp).semantics { heading() },
    )
}

/**
 * A capsule with its words and [icon], and an arrow in a circle at its end: the one to press ([color]
 * the accent) in ink with the circle in the accent, the rest on the fill.
 */
@Composable
internal fun GlassWordButton(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val primary = color == p.accent
    val ink = (if (primary) p.canvas else p.ink).copy(alpha = if (enabled) 1f else 0.4f)
    val source = remember { MutableInteractionSource() }
    Row(
        modifier.heightIn(min = 50.dp).squish(source, 0.96f).clip(CircleShape).background(if (primary) p.ink else p.fill)
            .clickable(source, LocalIndication.current, enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }.padding(start = 18.dp, end = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
            Text(LocalLook.current.case.of(text), style = Type.Headline, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.padding(start = 10.dp).size(38.dp).background((if (primary) p.accent else p.ink).copy(alpha = if (enabled) 1f else 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = if (primary) p.onAccent else p.canvas, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
internal fun GlassHairline(start: Dp) {
    Box(Modifier.fillMaxWidth().padding(start = start).height(0.5.dp).background(LocalPalette.current.line))
}

/** How a card sits [depth] down the deck: lower, smaller, turned a little one way then the other. */
private fun deckTurn(depth: Float): Float {
    val turns = floatArrayOf(0f, 3.5f, -4f, -2f)
    val d = depth.coerceIn(0f, 3f)
    val i = d.toInt().coerceAtMost(2)
    return turns[i] + (turns[i + 1] - turns[i]) * (d - i)
}

/**
 * A shelf as a deck of cards: tap the top one to play or open it, swipe it away either way and the
 * next comes up, the ones under it rising into place.
 */
@Composable
internal fun GlassShelf(section: Section, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val cards = section.cards
    if (cards.isEmpty()) return
    var top by remember(section.title) { mutableIntStateOf(0) }
    val dx = remember { Animatable(0f) }
    // 1 the moment a card is thrown, springing back to 0: the cards under it rise into its place.
    val lift = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val next = {
        scope.launch {
            top = (top + 1) % cards.size
            dx.snapTo(0f)
            lift.snapTo(1f)
            lift.animateTo(0f, bouncy())
        }
        Unit
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(318.dp), contentAlignment = Alignment.TopCenter) {
            for (k in minOf(2, cards.size - 1) downTo 0) {
                val at = (top + k) % cards.size
                key(at) {
                    val card = cards[at]
                    val isTop = k == 0
                    val round = card.page?.startsWith("UC") == true
                    val source = remember { MutableInteractionSource() }
                    val go = cardAction(card, onOpen)
                    Column(
                        Modifier.padding(top = 8.dp).width(232.dp)
                            .graphicsLayer {
                                val d = k + lift.value
                                translationY = d * 12.dp.toPx()
                                val s = 1f - d * 0.05f
                                scaleX = s; scaleY = s
                                rotationZ = deckTurn(d)
                                alpha = (3f - d).coerceIn(0f, 1f)
                                if (isTop) { translationX = dx.value; rotationZ += dx.value / 30f }
                            }
                            .then(if (isTop) Modifier.squish(source, 0.97f) else Modifier)
                            .shadow(if (isTop) 18.dp else 6.dp, Tile).background(p.cell, Tile).clip(Tile)
                            .then(
                                if (isTop) Modifier
                                    .pointerInput(top) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                scope.launch {
                                                    if (abs(dx.value) > 110.dp.toPx()) { dx.animateTo(sign(dx.value) * 900.dp.toPx(), tween(220)); next() }
                                                    else dx.animateTo(0f, spring(dampingRatio = 0.6f))
                                                }
                                            },
                                            onDragCancel = { scope.launch { dx.animateTo(0f, spring()) } },
                                        ) { change, d -> change.consume(); scope.launch { dx.snapTo(dx.value + d) } }
                                    }
                                    .clickable(source, null, onClick = go)
                                    .semantics { customActions = listOf(CustomAccessibilityAction("Next card") { next(); true }) }
                                else Modifier.clearAndSetSemantics { },
                            )
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Cover(card.art?.let { artAt(it, 544) }, 212.dp, round = round)
                        val align = if (round) TextAlign.Center else TextAlign.Start
                        Text(cardTitle(card), style = Type.Headline.copy(fontSize = 16.sp, lineHeight = 21.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align, modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp))
                        Text(card.subtitle.substringBefore(" • "), style = Type.Small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 4.dp))
                    }
                }
            }
        }
        if (cards.size > 1) Text("${top + 1} / ${cards.size}  ·  swipe", style = Type.Small, color = p.inkMuted)
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
 * Home's bento: the song playing (or last played) large, under a caption and a play button of glass;
 * your plays in the accent; a charm of glass on a keychain; and the days in a row you've listened.
 */
@Composable
private fun GlassBento() {
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(250.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Glassed(
                ground = p.surface,
                modifier = Modifier.weight(1.25f).fillMaxHeight().enter(0).clip(Tile),
                content = {
                    if (song?.art != null) AsyncImage(artAt(song.art!!, 544), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(p.accent, p.surface))))
                },
                glass = {
                    if (song != null) {
                        GlassButton(
                            if (now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (now.playing) "Pause" else "Play",
                            Modifier.align(Alignment.TopEnd).padding(10.dp), size = 44.dp, stain = p.accent, iconModifier = Modifier.popOn(now.playing),
                        ) { if (now.song != null) graph.player.toggle() else graph.player.play(listOf(song)) }
                    }
                    Column(
                        Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth()
                            .glass(RoundedCornerShape(19.dp), 19.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(if (now.song != null) "Now playing" else if (song != null) "Last played" else "Nothing yet", style = Type.Small.copy(fontSize = 12.sp, lineHeight = 16.sp), color = p.ink.copy(alpha = 0.72f))
                        Text(song?.let { com.verza.data.shownTitle(it.title, it.artist) } ?: "Play something", style = Type.Headline.copy(fontSize = 16.sp, lineHeight = 20.sp), color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                },
            )
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // The count runs up to its figure when it loads.
                val shown by animateIntAsState(plays, tween(900), label = "plays")
                Column(Modifier.weight(1f).fillMaxWidth().enter(1).clip(Tile).background(p.accent).padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("Plays", style = Type.Label, color = p.onAccent)
                    Text("$shown", style = Type.Masthead.copy(fontSize = 44.sp, lineHeight = 46.sp), color = p.onAccent, maxLines = 1)
                }
                Charm(song?.art, Modifier.weight(1.1f).fillMaxWidth().enter(2).clip(Tile))
            }
        }
        Row(
            Modifier.fillMaxWidth().enter(3).clip(Tile).background(p.surface).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, null, tint = p.accent, modifier = Modifier.size(28.dp))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(if (streak == 1) "1 day in a row" else "$streak days in a row", style = Type.Headline.copy(fontSize = 16.sp, lineHeight = 21.sp), color = p.ink)
                Text(artists.firstOrNull()?.artist?.let { "Mostly $it lately" } ?: "Play something to start a streak", style = Type.Small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * A bead of glass on a keychain drawn in line, over the cover's colours. It hangs the way gravity
 * pulls as the phone leans, swings in when Home opens, and swings when tapped.
 */
@Composable
private fun Charm(art: String?, modifier: Modifier) {
    val p = LocalPalette.current
    val colours = coverSwatches(art).ifEmpty { listOf(p.accent) }
    val tilt = LocalTilt.current
    val swing = remember { Animatable(22f) }
    val scope = rememberCoroutineScope()
    // Plumb: the phone leaning right by an angle hangs the charm that much the other way on screen.
    fun plumb() = -Math.toDegrees(asin(tilt.value.x.coerceIn(-0.7f, 0.7f).toDouble())).toFloat()
    val loose = spring<Float>(dampingRatio = 0.14f, stiffness = 40f)
    LaunchedEffect(tilt) { snapshotFlow { plumb() }.collectLatest { swing.animateTo(it, loose) } }
    Glassed(
        ground = p.surface,
        modifier = modifier,
        content = {
            Box(Modifier.fillMaxSize().drawBehind {
                drawCircle(Brush.radialGradient(listOf(colours[0].copy(alpha = 0.85f), Color.Transparent), Offset(size.width * 0.25f, size.height * 0.8f), size.width * 0.75f), size.width * 0.75f, Offset(size.width * 0.25f, size.height * 0.8f))
                val second = colours.getOrElse(1) { p.accent }
                drawCircle(Brush.radialGradient(listOf(second.copy(alpha = 0.7f), Color.Transparent), Offset(size.width * 0.85f, size.height * 0.45f), size.width * 0.6f), size.width * 0.6f, Offset(size.width * 0.85f, size.height * 0.45f))
            })
        },
        glass = {
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
                    .graphicsLayer { rotationZ = swing.value; transformOrigin = TransformOrigin(0.5f, 0f) }
                    .clearAndSetSemantics { }
                    .clickable(remember { MutableInteractionSource() }, null) { scope.launch { swing.animateTo(plumb(), loose, initialVelocity = if (swing.value > plumb()) -240f else 240f) } },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.size(40.dp, 50.dp)) {
                    val w = 2.2.dp.toPx()
                    // The carabiner, its gate a line across its side, then the split ring.
                    drawRoundRect(p.ink, Offset(size.width * 0.3f, 0f), Size(size.width * 0.4f, size.height * 0.56f), CornerRadius(size.width * 0.2f), style = Stroke(w))
                    drawLine(p.ink, Offset(size.width * 0.66f, size.height * 0.12f), Offset(size.width * 0.58f, size.height * 0.4f), w, StrokeCap.Round)
                    drawCircle(p.ink, size.width * 0.17f, Offset(size.width / 2, size.height * 0.78f), style = Stroke(w))
                }
                Box(Modifier.offset(y = (-5).dp).size(58.dp).glass(CircleShape, 29.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, tint = p.ink, modifier = Modifier.size(28.dp))
                }
            }
        },
    )
}

private val BarHeight = 62.dp
private val PlayerHeight = 64.dp

/** What floats at the foot: how far downloads have got, the playing song, then the tabs in a capsule of their own. */
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
 * The tabs, search among them, as symbols on a capsule of glass. The chosen one sits in a drop of
 * glass stained with the accent, and moving to another, the drop's ends chase it at different speeds:
 * it stretches toward the new tab and settles round there, the way a drop of water runs.
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
                val start = Offset(left.value * w, 0f)
                val drop = Size((right.value - left.value) * w, size.height)
                val round = CornerRadius(size.height / 2)
                drawRoundRect(Brush.verticalGradient(
                    0f to p.accent.copy(alpha = (if (p.dark) 0.5f else 0.3f) * shown.value),
                    1f to p.accent.copy(alpha = (if (p.dark) 0.3f else 0.16f) * shown.value),
                ), start, drop, round)
                drawRoundRect(p.accent.copy(alpha = 0.35f * shown.value), start, drop, round, style = Stroke(1.dp.toPx()))
                drawRoundRect(Color.White.copy(alpha = (if (p.dark) 0.25f else 0.7f) * shown.value), start, drop, round, style = Stroke(1.dp.toPx()))
            },
    ) {
        for (tab in tabs) {
            val on = tab.route == current
            Box(
                Modifier.size(width = 66.dp, height = BarHeight - 12.dp).clip(CircleShape).clickable { go(tab.route) }
                    .semantics { role = Role.Tab; selected = on; contentDescription = tab.label },
                contentAlignment = Alignment.Center,
            ) { Icon(tab.icon, null, tint = p.ink, modifier = Modifier.size(24.dp)) }
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
 * The playing song in a capsule of glass over the tabs, its cover round; a tap raises Now Playing.
 * The glass holds the cover's colours, and its rim fills with the accent as the song plays, from the
 * top left round.
 */
@Composable
private fun GlassMiniPlayer(now: NowPlaying, onOpen: () -> Unit) {
    val song = now.song ?: return
    val p = LocalPalette.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // The cover's colours across the capsule, left to right. Not the cover itself stretched: a video's
    // frame has dark bars at its sides, and their edges showed through the glass.
    val colours = coverSwatches(song.art)
    val played by playedFraction(now)
    Row(
        Modifier.fillMaxWidth().height(PlayerHeight).takesTouches()
            .glass(CircleShape, PlayerHeight / 2, pressed, inside = colours.takeIf { it.isNotEmpty() }?.let { c -> { drawRect(Brush.horizontalGradient((c + c).take(3)), alpha = if (p.dark) 0.45f else 0.38f) } })
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
                    Text(song.artist, style = Type.Small, color = p.ink.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        PlayPauseNext(now, p.ink)
    }
}

/** Play or pause, then next: the mini player's two buttons, in every look. */
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

/** Play and pause on a disc of glass stained with the song's colour: the one control that matters most. */
@Composable
internal fun GlassPlayButton(playing: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    GlassButton(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Play", size = 84.dp, iconSize = 50.dp, tint = p.ink, stain = p.accent, iconModifier = Modifier.popOn(playing), onClick = onClick)
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
