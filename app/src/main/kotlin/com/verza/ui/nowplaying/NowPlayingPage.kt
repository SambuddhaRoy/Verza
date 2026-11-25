package com.verza.ui.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import com.verza.ui.kit.ChromeButton
import com.verza.ui.kit.Glassed
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import com.verza.ui.kit.takesTouches
import com.verza.data.Link
import com.verza.data.Song
import com.verza.data.artAt
import com.verza.graph
import com.verza.playback.NowPlaying
import com.verza.playback.Sleep
import com.verza.data.Sessions
import com.verza.ui.kit.MenuItem
import com.verza.ui.kit.SongMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.verza.ui.kit.ListRow
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Now Playing's inks, from its own palette: the look makes it (Look.nowPlaying), dark for most.
private val Ink: Color @Composable get() = LocalPalette.current.ink
private val InkSoft: Color @Composable get() = LocalPalette.current.inkMuted
private val InkFaint: Color @Composable get() = LocalPalette.current.line

/**
 * Now Playing: a sheet over the app, its ground the cover's colours flowing slowly. The cover, rounded
 * and lifted, settles smaller while paused. Under it the title and who made it, like and ⋯ in glass,
 * the scrubber, the transport, then lyrics, radio, shuffle and repeat. The queue follows on the same
 * page. Pulled down from the top, it goes.
 */
@Composable
fun NowPlayingPage(flick: FlickState, onClose: () -> Unit, onOpen: (String) -> Unit, onLyrics: () -> Unit, onStandby: () -> Unit) {
    val graph = LocalContext.current.graph
    val player = graph.player
    val now by player.state.collectAsState()
    val liked by graph.library.likedIds.collectAsState()
    val song = now.song ?: run { LaunchedEffect(Unit) { onClose() }; return }
    val art = song.art?.let { artAt(it, 1200) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sleep by player.sleep.collectAsState()
    var sleepPicker by remember { mutableStateOf(false) }
    if (sleepPicker) SleepPicker(sleep, onDone = { sleepPicker = false })
    var devices by remember { mutableStateOf(false) }
    if (devices) com.verza.ui.kit.DevicesDialog(onDone = { devices = false })
    BackHandler(onBack = onClose)
    val look = com.verza.ui.look.LocalLook.current
    val visualizer by graph.settings.visualizer.flow.collectAsState(initial = true)
    val target = flick.sendTo
    val p = LocalPalette.current
    // The page's own palette: white ink, and an accent measured against the dark.
    val dark = remember(p, look) { look.nowPlaying(p) }
    com.verza.ui.kit.BarIcons(light = dark.dark)

    // The artist's and album's pages; a file on the phone's are its song's on YouTube Music.
    val links by produceState<Pair<List<Link>, Link?>?>(null, song.id) {
        value = runCatching { graph.youtube.twin(song)?.let { graph.youtube.links(it.id, it.title) } }.getOrNull()
    }
    val pull = remember { Animatable(0f) }
    val shut = with(LocalDensity.current) { 140.dp.toPx() }
    val sheet = remember(onClose) { PullToClose(pull, shut, scope, onClose) }

    CompositionLocalProvider(LocalPalette provides dark) {
        Glassed(
            ground = Color.Black,
            modifier = Modifier.fillMaxSize().takesTouches().graphicsLayer { translationY = pull.value },
            content = { look.NowPlayingBackground(song.art?.let { artAt(it, 226) }) },
            glass = {
                BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
                    val pageHeight = maxHeight
                    LazyColumn(Modifier.nestedScroll(sheet), contentPadding = PaddingValues(bottom = 24.dp)) {
                        item {
                            Column(Modifier.fillMaxWidth().height(pageHeight - 32.dp).padding(horizontal = 24.dp)) {
                                // The grabber: says the sheet can be pulled down, and closes it when pressed.
                                Box(
                                    Modifier.fillMaxWidth().height(30.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
                                        .semantics { role = Role.Button; contentDescription = "Close" },
                                    contentAlignment = Alignment.Center,
                                ) { Box(Modifier.size(width = 38.dp, height = 5.dp).clip(CircleShape).background(InkFaint.copy(alpha = 0.45f))) }
                                // The cover leaves a tenth of its room each side for the visualiser's bars.
                                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    val side = minOf(maxWidth, maxHeight) * (if (visualizer) 0.8f else 0.96f)
                                    var swell by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                                    val rest by animateFloatAsState(if (now.playing) 1f else 0.84f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow), label = "rest")
                                    if (visualizer && flick.fly.value == 0f) CoverVisualizer(side, p.accent, now.playing, onBass = { swell = it })
                                    // A flick toward the computer sends the cover flying off the top, and the music with it;
                                    // one back toward you brings the computer's down from there.
                                    look.NowPlayingCover(art, side, rest, swell, flick.fly.value, flick.label)
                                }
                                Spacer(Modifier.height(18.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(com.verza.data.shownTitle(song.title, song.artist), style = Type.Section, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Names(song, links, onOpen = { onClose(); onOpen(it) })
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    val isLiked = song.id in liked
                                    ChromeButton(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (isLiked) "Unlike" else "Like", size = 40.dp) { graph.like(song, !isLiked) }
                                    Spacer(Modifier.width(10.dp))
                                    SongMenu(song, inPlayer = true) { close ->
                                        MenuItem(sleep?.let { "Sleep timer · ${sleepLeft(it)}" } ?: "Sleep timer") { close(); sleepPicker = true }
                                        MenuItem("Discovery radio") {
                                            close()
                                            android.widget.Toast.makeText(context, "Finding music you haven't heard…", android.widget.Toast.LENGTH_SHORT).show()
                                            scope.launch {
                                                val found = runCatching { graph.youtube.twin(song)?.let { graph.discovery.from(it.id) } }.getOrNull().orEmpty()
                                                if (found.isEmpty()) android.widget.Toast.makeText(context, "Couldn't find anything new for this one", android.widget.Toast.LENGTH_SHORT).show()
                                                else if (player.state.value.song?.id == song.id) player.replaceUpcoming(found)
                                            }
                                        }
                                        MenuItem("Clock screen") { close(); onStandby() }
                                        MenuItem("Devices") { close(); devices = true }
                                        MenuItem("Share listening session") {
                                            close()
                                            Sessions.link(now.queue, now.index, player.position)?.let { shareSession(context, it) }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                Scrubber(now, onSeek = player::seek)
                                Transport(now)
                                Extras(now, onLyrics = onLyrics, onRadio = { player.playWithRadio(song) })
                                // Said once there's somewhere to flick to: the gesture can't be seen otherwise.
                                if (target != null) Text("Flick toward ${target.name} to play there", style = Type.Small, color = InkSoft,
                                    modifier = Modifier.fillMaxWidth().clickable(onClick = flick.send).padding(vertical = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(Modifier.navigationBarsPadding().height(4.dp))
                            }
                        }
                        item { Text(com.verza.ui.look.LocalLook.current.case.of("Up next"), style = Type.Headline, color = Ink, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)) }
                        val upNext = now.queue.withIndex().drop(now.index + 1)
                        itemsIndexed(upNext, key = { _, (i, s) -> "$i${s.id}" }) { _, (i, s) ->
                            ListRow(s.title, s.artist, art = s.art?.let { artAt(it, 226) }, modifier = Modifier.padding(horizontal = 24.dp), onClick = { player.jumpTo(i) })
                        }
                    }
                }
            },
        )
    }
}

/**
 * Pulling the page down past its top moves it with the finger, and far enough (or fast enough) closes
 * it; let go short of that and it springs back.
 */
private class PullToClose(
    private val pull: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    private val shut: Float,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val onClose: () -> Unit,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Pushing back up while pulled: undo the pull before the list scrolls.
        if (pull.value > 0f && available.y < 0f) {
            val used = maxOf(available.y, -pull.value)
            scope.launch { pull.snapTo(pull.value + used) }
            return Offset(0f, used)
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y > 0f && source == NestedScrollSource.UserInput) {
            scope.launch { pull.snapTo(pull.value + available.y) }
            return Offset(0f, available.y)
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (pull.value <= 0f) return Velocity.Zero
        if (pull.value > shut || available.y > 2400f) onClose() else pull.animateTo(0f, spring(dampingRatio = 0.8f))
        return available
    }
}

/** Lyrics and radio, then shuffle and repeat, which light up while on. */
@Composable
private fun Extras(now: NowPlaying, onLyrics: () -> Unit, onRadio: () -> Unit) {
    val player = LocalContext.current.graph.player
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        Glyph(Icons.Rounded.Lyrics, "Lyrics", onClick = onLyrics)
        Glyph(Icons.Rounded.Radio, "Radio", onClick = onRadio)
        Glyph(Icons.Rounded.Shuffle, "Shuffle", on = now.shuffle, onClick = player::toggleShuffle)
        Glyph(if (now.repeat == REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", on = now.repeat != REPEAT_MODE_OFF, onClick = player::cycleRepeat)
    }
}

/**
 * A symbol to press, with no button drawn round it: pressed, a soft disc blooms behind it, as under
 * Apple's transport. [on] keeps a quieter disc, for a toggle that is on.
 */
@Composable
private fun Glyph(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, size: Dp = 48.dp, iconSize: Dp = 24.dp, on: Boolean? = null, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bloom by animateFloatAsState(if (pressed) 1f else 0f, spring(stiffness = Spring.StiffnessMedium), label = "bloom")
    Box(
        Modifier.size(size).clickable(source, null, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label; if (on != null) stateDescription = if (on) "On" else "Off" }
            .drawBehind {
                val ground = (if (on == true) 0.16f else 0f) + 0.14f * bloom
                if (ground > 0f) drawCircle(Color.White.copy(alpha = ground), radius = this.size.minDimension / 2 * (0.86f + 0.14f * bloom))
            },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (on == false) InkSoft else Ink, modifier = Modifier.size(iconSize).graphicsLayer { val s = 1f - 0.08f * bloom; scaleX = s; scaleY = s }) }
}

/** How long a sleep timer has left, "12:04", or "end of song". Ticks while it's on screen. */
@Composable
private fun sleepLeft(sleep: Sleep): String {
    val at = sleep.atMs ?: return "end of song"
    var now by remember { mutableStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(at) { while (true) { now = android.os.SystemClock.elapsedRealtime(); delay(1_000) } }
    val s = ((at - now) / 1000).coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun SleepPicker(sleep: Sleep?, onDone: () -> Unit) {
    val player = LocalContext.current.graph.player
    fun pick(set: () -> Unit) { set(); onDone() }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (sleep == null) "Sleep timer" else "Sleep timer · ${sleepLeft(sleep)}") },
        text = {
            Column {
                for (m in listOf(15, 30, 45, 60)) MenuItem(if (m == 60) "1 hour" else "$m minutes") { pick { player.sleepIn(m) } }
                MenuItem("At the end of this song") { pick { player.sleep.value = Sleep(atMs = null) } }
                MenuItem("Wind down over 30 minutes") { pick { player.sleepIn(30, windDown = true) } }
            }
        },
        confirmButton = { if (sleep != null) TextButton(onClick = { pick { player.sleep.value = null } }) { Text("Turn off") } },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
    )
}

/** Sends a "listen along" link through the system's share sheet. */
private fun shareSession(context: android.content.Context, link: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
        .putExtra(android.content.Intent.EXTRA_SUBJECT, "Listen along on Verza")
        .putExtra(android.content.Intent.EXTRA_TEXT, "Open this in Verza to hear what I'm playing:\n$link")
    context.startActivity(android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Every artist, then the album, each opening its page when there is one. */
@Composable
private fun Names(song: Song, links: Pair<List<Link>, Link?>?, onOpen: (String) -> Unit) {
    val names = links?.first.orEmpty().ifEmpty { listOf(Link(song.artist, "")) } + listOfNotNull(links?.second)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
        names.forEachIndexed { i, link ->
            if (i > 0) Text("·", style = Type.RowTitle.copy(fontSize = 18.sp, lineHeight = 23.sp), color = InkSoft)
            Text(
                link.name,
                style = Type.RowTitle.copy(fontSize = 18.sp, lineHeight = 23.sp),
                color = InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (link.page.isNotEmpty()) Modifier.clickable { onOpen(link.page) } else Modifier,
            )
        }
    }
}

/** Two square waves and "Lossless"; a tap shows the format itself. */
@Composable
private fun LosslessMark(format: String) {
    var detail by remember(format) { mutableStateOf(false) }
    Row(
        Modifier.clickable(remember { MutableInteractionSource() }, null) { detail = !detail }
            .semantics(mergeDescendants = true) { contentDescription = "Lossless, $format" }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val soft = InkSoft
        Canvas(Modifier.size(width = 22.dp, height = 13.dp)) {
            val step = size.width / 5f
            val swing = size.height * 0.13f
            val wave = Path()
            for ((y, startHigh) in listOf(size.height * 0.27f to true, size.height * 0.73f to false)) {
                var high = startHigh
                wave.moveTo(0f, if (high) y - swing else y + swing)
                for (i in 1..5) {
                    wave.lineTo(i * step, if (high) y - swing else y + swing)
                    if (i < 5) { high = !high; wave.lineTo(i * step, if (high) y - swing else y + swing) }
                }
            }
            drawPath(wave, soft, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.cornerPathEffect(step * 0.5f)))
        }
        Text(if (detail) format else "Lossless", style = Type.Small.copy(fontSize = 13.sp), color = InkSoft, maxLines = 1)
    }
}

/** A capsule that fills as the song plays, thicker under the finger; the times under it, and Lossless between them. */
@Composable
private fun Scrubber(now: NowPlaying, onSeek: (Long) -> Unit) {
    val player = LocalContext.current.graph.player
    var position by remember { mutableLongStateOf(player.position) }
    LaunchedEffect(now.song?.id, now.playing) { while (true) { position = player.position; if (!now.playing) break; delay(250) } }
    var dragged by remember { mutableStateOf<Float?>(null) }
    val duration by rememberUpdatedState(now.durationMs)
    val seek by rememberUpdatedState(onSeek)
    val shown = dragged ?: if (now.durationMs > 0) (position.toFloat() / now.durationMs).coerceIn(0f, 1f) else 0f
    val drag by animateFloatAsState(if (dragged != null) 1f else 0f, label = "scrub")
    val look = com.verza.ui.look.LocalLook.current
    val palette = LocalPalette.current
    Canvas(
        Modifier.fillMaxWidth().height(30.dp)
            .semantics { contentDescription = "Position, ${clock((shown * now.durationMs).toLong())} of ${clock(now.durationMs)}" }
            .pointerInput(Unit) { detectTapGestures { o -> if (duration > 0) seek(((o.x / size.width).coerceIn(0f, 1f) * duration).toLong()) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragged = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragged?.let { if (duration > 0) seek((it * duration).toLong()) }; dragged = null },
                    onDragCancel = { dragged = null },
                ) { change, _ -> change.consume(); dragged = (change.position.x / size.width).coerceIn(0f, 1f) }
            },
    ) {
        with(look) { scrubber(shown, drag, palette) }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val at = (shown * now.durationMs).toLong()
        Text(clock(at), style = Type.Small.copy(fontSize = 12.sp), color = InkSoft, modifier = Modifier.width(56.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { now.lossless?.let { LosslessMark(it) } }
        Text(clock(now.durationMs), style = Type.Small.copy(fontSize = 12.sp), color = InkSoft, modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

/** Back and on as large symbols with nothing round them; play and pause on stained glass between. */
@Composable
private fun Transport(now: NowPlaying) {
    val player = LocalContext.current.graph.player
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        Glyph(Icons.Rounded.SkipPrevious, "Previous", size = 72.dp, iconSize = 44.dp, onClick = player::previous)
        // Play and pause on a disc of glass stained with the song's colour: the one control that matters most.
        com.verza.ui.look.LocalLook.current.PlayButton(now.playing, player::toggle)
        Glyph(Icons.Rounded.SkipNext, "Next", size = 72.dp, iconSize = 44.dp, onClick = player::next)
    }
}

private fun clock(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}
