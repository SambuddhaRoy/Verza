package com.verza.ui.nowplaying

import androidx.activity.compose.BackHandler
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
import com.verza.ui.theme.Corner
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import coil3.compose.AsyncImage
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

// Now Playing sits on the cover, so its ink is white whatever the system's dark mode says.
private val Ink = Color.White
private val InkSoft = Color.White.copy(alpha = 0.72f)
private val InkFaint = Color.White.copy(alpha = 0.28f)

/**
 * Now Playing: the cover as a square on a dimmed blur of itself, and under it the title, the artist
 * and album (each opens its page), the Lossless mark when it is, the scrubber and the transport. The
 * cover takes what the rest leaves, so a small phone gets a smaller cover, not controls off the foot of
 * the screen. The queue is below, on the same scrolling page.
 */
@Composable
fun NowPlayingPage(onClose: () -> Unit, onOpen: (String) -> Unit, onLyrics: () -> Unit, onStandby: () -> Unit) {
    val graph = LocalContext.current.graph
    val player = graph.player
    val now by player.state.collectAsState()
    val liked by graph.library.likedIds.collectAsState(initial = emptySet())
    val song = now.song ?: run { LaunchedEffect(Unit) { onClose() }; return }
    val art = song.art?.let { artAt(it, 1200) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sleep by player.sleep.collectAsState()
    var sleepPicker by remember { mutableStateOf(false) }
    if (sleepPicker) SleepPicker(sleep, onDone = { sleepPicker = false })
    BackHandler(onBack = onClose)
    com.verza.ui.kit.LightBarIcons()

    // The artist's and album's pages; a file on the phone's are its song's on YouTube Music.
    val links by produceState<Pair<List<Link>, Link?>?>(null, song.id) {
        value = runCatching { graph.youtube.twin(song)?.let { graph.youtube.links(it.id, it.title) } }.getOrNull()
    }

    Box(Modifier.fillMaxSize().takesTouches().background(Color.Black)) {
        AsyncImage(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(60.dp), alpha = 0.55f)
        // White type has to read on a white cover too.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
            val pageHeight = maxHeight
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().height(pageHeight - 40.dp).padding(horizontal = 24.dp)) {
                        IconButton(onClick = onClose, modifier = Modifier.offset(x = (-12).dp)) {
                            Icon(Icons.Rounded.KeyboardArrowDown, "Close", tint = Ink, modifier = Modifier.size(32.dp))
                        }
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AsyncImage(art, null, contentScale = ContentScale.Crop, modifier = Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).clip(Corner))
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(song.title, style = Type.Title.copy(fontSize = 34.sp, lineHeight = 39.sp), color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Names(song, links, onOpen = { onClose(); onOpen(it) })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            now.lossless?.let { LosslessMark(it) }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = onLyrics) { Icon(Icons.Rounded.Lyrics, "Lyrics", tint = Ink) }
                            val isLiked = song.id in liked
                            IconButton(onClick = { graph.like(song, !isLiked) }) {
                                Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (isLiked) "Unlike" else "Like", tint = Ink)
                            }
                            IconButton(onClick = { player.playWithRadio(song) }) { Icon(Icons.Rounded.Radio, "Radio", tint = Ink) }
                            androidx.compose.runtime.CompositionLocalProvider(LocalPalette provides LocalPalette.current.copy(inkMuted = Ink)) {
                                SongMenu(song) { close ->
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
                                    MenuItem("Standby") { close(); onStandby() }
                                    MenuItem("Share listening session") {
                                        close()
                                        Sessions.link(now.queue, now.index, player.position)?.let { shareSession(context, it) }
                                    }
                                }
                            }
                        }
                        Scrubber(now, onSeek = player::seek)
                        Transport(now)
                        Spacer(Modifier.navigationBarsPadding().height(8.dp))
                    }
                }
                item { Text("up next", style = Type.Section, color = Ink, modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 8.dp)) }
                val upNext = now.queue.withIndex().drop(now.index + 1)
                itemsIndexed(upNext, key = { _, (i, s) -> "$i${s.id}" }) { _, (i, s) ->
                    androidx.compose.runtime.CompositionLocalProvider(LocalPalette provides LocalPalette.current.copy(ink = Ink, inkMuted = InkSoft)) {
                        ListRow(s.title, s.artist, art = s.art?.let { artAt(it, 226) }, modifier = Modifier.padding(horizontal = 24.dp), onClick = { player.jumpTo(i) })
                    }
                }
            }
        }
    }
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
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        names.forEachIndexed { i, link ->
            if (i > 0) Text("·", style = Type.RowTitle, color = InkSoft)
            Text(
                link.name,
                style = Type.RowTitle,
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
            drawPath(wave, InkSoft, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.cornerPathEffect(step * 0.5f)))
        }
        Text(if (detail) format else "Lossless", style = Type.Small.copy(fontSize = 13.sp), color = InkSoft, maxLines = 1)
    }
}

/** A thin line with the time either side; drag or tap to seek. The finger owns the position while it's down. */
@Composable
private fun Scrubber(now: NowPlaying, onSeek: (Long) -> Unit) {
    val player = LocalContext.current.graph.player
    val accent = LocalPalette.current.accent
    var position by remember { mutableLongStateOf(player.position) }
    LaunchedEffect(now.song?.id, now.playing) { while (true) { position = player.position; if (!now.playing) break; delay(250) } }
    var dragged by remember { mutableStateOf<Float?>(null) }
    val duration by rememberUpdatedState(now.durationMs)
    val seek by rememberUpdatedState(onSeek)
    val shown = dragged ?: if (now.durationMs > 0) (position.toFloat() / now.durationMs).coerceIn(0f, 1f) else 0f
    val thickness by animateDpAsState(if (dragged != null) 5.dp else 2.dp, label = "scrub")
    Canvas(
        Modifier.fillMaxWidth().height(34.dp)
            .pointerInput(Unit) { detectTapGestures { o -> if (duration > 0) seek(((o.x / size.width).coerceIn(0f, 1f) * duration).toLong()) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragged = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragged?.let { if (duration > 0) seek((it * duration).toLong()) }; dragged = null },
                    onDragCancel = { dragged = null },
                ) { change, _ -> change.consume(); dragged = (change.position.x / size.width).coerceIn(0f, 1f) }
            },
    ) {
        val y = size.height / 2
        val x = size.width * shown
        val w = thickness.toPx()
        drawLine(InkFaint, Offset(0f, y), Offset(size.width, y), w, StrokeCap.Round)
        drawLine(Ink, Offset(0f, y), Offset(x, y), w, StrokeCap.Round)
        drawCircle(accent, 5.dp.toPx() + w / 2, Offset(x, y))
    }
    Row(Modifier.fillMaxWidth()) {
        Text(clock((shown * now.durationMs).toLong()), style = Type.Small, color = InkSoft)
        Spacer(Modifier.weight(1f))
        Text(clock(now.durationMs), style = Type.Small, color = InkSoft)
    }
}

@Composable
private fun Transport(now: NowPlaying) {
    val player = LocalContext.current.graph.player
    val accent = LocalPalette.current.accent
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = player::toggleShuffle) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (now.shuffle) accent else InkSoft) }
        IconButton(onClick = player::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Ink, modifier = Modifier.size(36.dp)) }
        IconButton(onClick = player::toggle, modifier = Modifier.size(72.dp)) {
            Icon(if (now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (now.playing) "Pause" else "Play", tint = Ink, modifier = Modifier.size(56.dp))
        }
        IconButton(onClick = player::next) { Icon(Icons.Rounded.SkipNext, "Next", tint = Ink, modifier = Modifier.size(36.dp)) }
        IconButton(onClick = player::cycleRepeat) {
            Icon(if (now.repeat == REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", tint = if (now.repeat == REPEAT_MODE_OFF) InkSoft else accent)
        }
    }
}

private fun clock(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}
