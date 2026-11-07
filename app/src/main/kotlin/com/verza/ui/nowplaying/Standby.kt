package com.verza.ui.nowplaying

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import com.verza.data.Lyrics
import com.verza.data.Song
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.theme.Corner
import com.verza.ui.theme.Type
import com.verza.ui.theme.coverSwatches
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

private const val FACES = 3
private const val FACE_CLOCK = 0
private const val FACE_COVER = 1

private val Ink = Color.White
private val InkSoft = Color.White.copy(alpha = 0.72f)
private val InkFaint = Color.White.copy(alpha = 0.30f)

private val Display = Type.Title.copy(fontWeight = FontWeight.Medium)
private val Digits = Type.Title.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
private val SmallType = Type.Body

/**
 * Standby: for a phone on a stand or a charger, as the StandBy apps do it. Three faces, which a
 * sideways swipe moves between and which it remembers:
 *  - a clock as large as the screen allows, its digits cut out of the cover;
 *  - the cover large, with a small clock;
 *  - the lyrics as they're sung, with both of those small.
 *
 * A tap brings up the controls for a few seconds. Back or the cross leaves, so a stray touch on a
 * nightstand doesn't. It follows the phone's rotation even with auto-rotate off, since a phone on a
 * stand is usually on its side, and takes the status bars away.
 *
 * It can be left on for hours, so the clock face is on true black, nothing moves on its own, everything
 * shifts a few dp each minute against burn-in, and it dims at night.
 */
@Composable
fun Standby(onExit: () -> Unit) {
    StandbyWindow()
    BackHandler(onBack = onExit)
    val context = LocalContext.current
    val graph = context.graph
    val player = graph.player
    val now by player.state.collectAsState()
    val song = now.song ?: run { LaunchedEffect(Unit) { onExit() }; return }
    val art = song.art?.let { artAt(it, 1200) }
    // Read where it's drawn: the position moves four times a second, and only the progress line and
    // the lyrics have any use for it.
    val pos by produceState(player.position) { while (true) { value = player.position; delay(250) } }
    val position = { pos }

    val minute = rememberMinute()
    val clock = remember(minute) {
        minute.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", Locale.getDefault()))
    }
    val date = remember(minute) { minute.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())).lowercase() }
    val night = minute.hour >= 22 || minute.hour < 7

    // How bright the cover is, by its colours. Lifts a dark cover's clock, and sets
    // how much the faces darken it under white type.
    val swatches = coverSwatches(song.art)
    val brightness = swatches.takeIf { it.isNotEmpty() }?.map { it.luminance() }?.average()?.toFloat()
    val lyrics by produceState<Lyrics?>(null, song.id) { value = runCatching { graph.lyrics.get(song) }.getOrDefault(Lyrics.None) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val saved by graph.settings.standbyFace.flow.collectAsState(initial = null)
    val start = saved ?: return
    val pager = rememberPagerState(initialPage = start.coerceIn(0, FACES - 1)) { FACES }
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.drop(1).collect { scope.launch { graph.settings.standbyFace.set(it) } } }

    // A tap shows the controls, and any tap on them keeps them up a little longer.
    var controls by remember { mutableStateOf(false) }
    var touched by remember { mutableIntStateOf(0) }
    LaunchedEffect(controls, touched) { if (controls) { delay(4_000); controls = false } }

    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { controls = !controls } }) {
        // The clock is on black and the other two on the cover, crossing over as you swipe.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, 1f) }) {
            AsyncImage(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(60.dp))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f + 0.4f * (brightness ?: 0.5f))))
        }
        // Moved in the layer, so a minute's shift is a redraw.
        val shift = with(LocalDensity.current) { 3.dp.toPx() }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout).graphicsLayer {
                translationX = (minute.minute % 5 - 2) * shift
                translationY = (minute.minute % 3 - 1) * shift
            },
        ) { face ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val landscape = maxWidth > maxHeight
                when (face) {
                    FACE_CLOCK -> ClockFace(clock, date, song, art, brightness, landscape)
                    FACE_COVER -> CoverFace(clock, date, song, art, position, now.durationMs, landscape)
                    else -> LyricsFace(clock, song, art, lyrics, position, now.durationMs, landscape)
                }
            }
        }
        // At night, down to what a dark room wants. Only over the faces: the controls stay readable.
        if (night) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

        AnimatedVisibility(controls, enter = fadeIn(tween(160)), exit = fadeOut(tween(320))) {
            Controls(
                playing = now.playing,
                pager = pager,
                onToggle = { touched++; player.toggle() },
                onNext = { touched++; player.next() },
                onPrevious = { touched++; player.previous() },
                onExit = onExit,
            )
        }
    }
}

/** The whole screen, in whichever way up the phone is, and awake, until this goes away. */
@Composable
private fun StandbyWindow() {
    val view = LocalView.current
    DisposableEffect(view) {
        var c = view.context
        while (c is ContextWrapper && c !is Activity) c = c.baseContext
        val activity = c as? Activity
        val bars = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val orientation = activity?.requestedOrientation
        view.keepScreenOn = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        bars?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        bars?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            view.keepScreenOn = false
            if (orientation != null) activity.requestedOrientation = orientation
            bars?.show(WindowInsetsCompat.Type.systemBars())
            ViewCompat.requestApplyInsets(view)
        }
    }
}

/** The time, changing on the minute rather than some seconds after it. */
@Composable
private fun rememberMinute(): LocalDateTime {
    val now by produceState(LocalDateTime.now()) {
        while (true) { value = LocalDateTime.now(); delay(60_000L - System.currentTimeMillis() % 60_000L) }
    }
    return now
}

/** The clock at its largest, with what's playing and the date along the foot. */
@Composable
private fun ClockFace(clock: String, date: String, song: Song, art: String?, brightness: Float?, landscape: Boolean) {
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 18.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // On its side one line, which is the wider. Upright the hours over the minutes.
            CoverClock(if (landscape) clock else clock.replace(":", "\n").padStart(5, '0'), clock, art, brightness)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(art, Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { SongNames(song) }
            if (landscape) {
                Spacer(Modifier.width(16.dp))
                Text(date, style = SmallType, color = InkSoft, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SongNames(song: Song) {
    Text(song.title, style = SmallType.copy(fontWeight = FontWeight.Medium), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(song.artist, style = SmallType.copy(fontSize = 13.sp, lineHeight = 17.sp), color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * [text], as large as fits, with the cover showing through it. A dark cover is lifted just far enough
 * to read on black, however dark it is, and keeps what colour it has.
 */
@Composable
private fun CoverClock(text: String, spoken: String, art: String?, brightness: Float?) {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val measurer = rememberTextMeasurer()
        val layout = remember(text, constraints.maxWidth, constraints.maxHeight) {
            val base = Digits.copy(fontSize = 100.sp, lineHeight = 88.sp, textAlign = TextAlign.Center)
            val probe = measurer.measure(text, base, constraints = Constraints())
            val scale = min(constraints.maxWidth * 0.98f / probe.size.width, constraints.maxHeight * 0.98f / probe.size.height)
            measurer.measure(text, base.copy(fontSize = 100.sp * scale, lineHeight = 88.sp * scale), constraints = Constraints())
        }
        val b = brightness ?: 0f
        val lift = ((0.62f - b) / (1f - b)).coerceIn(0f, 1f)
        val density = LocalDensity.current
        val mask = remember { Paint().apply { blendMode = BlendMode.DstIn } }
        Box(
            Modifier
                .size(with(density) { layout.size.width.toDp() }, with(density) { layout.size.height.toDp() })
                .semantics { contentDescription = spoken }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    if (lift > 0f) drawRect(Color.White.copy(alpha = lift))
                    // In a layer of its own: a blend mode on the text only reaches the pixels under its
                    // glyphs, and the rest of the picture has to go as well.
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(size.toRect(), mask)
                        drawText(layout, color = Color.Black)
                        canvas.restore()
                    }
                },
        ) {
            Box(Modifier.fillMaxSize().background(Color.White))
            AsyncImage(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/** The cover as large as the screen has room for, and a small clock. */
@Composable
private fun CoverFace(clock: String, date: String, song: Song, art: String?, position: () -> Long, durationMs: Long, landscape: Boolean) {
    val details: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            Text(song.title, style = Display.copy(fontSize = 34.sp, lineHeight = 39.sp), color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(song.artist, style = SmallType.copy(fontSize = 17.sp, lineHeight = 22.sp), color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            ProgressLine(position, durationMs)
        }
    }
    val time: @Composable () -> Unit = {
        Text(clock, style = Digits.copy(fontSize = 56.sp, lineHeight = 60.sp), color = Ink, maxLines = 1)
        Text(date, style = SmallType, color = InkSoft, maxLines = 1)
    }
    if (landscape) {
        Row(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 22.dp), horizontalArrangement = Arrangement.spacedBy(34.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(art, Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true))
            Column(Modifier.weight(1f)) { time(); Spacer(Modifier.height(22.dp)); details(Modifier) }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            time()
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                Cover(art, Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true))
            }
            details(Modifier.fillMaxWidth())
        }
    }
}

/** The line being sung, large, with the next one under it. The cover and clock go small to make room. */
@Composable
private fun LyricsFace(clock: String, song: Song, art: String?, lyrics: Lyrics?, position: () -> Long, durationMs: Long, landscape: Boolean) {
    val time: @Composable () -> Unit = { Text(clock, style = Digits.copy(fontSize = 34.sp, lineHeight = 38.sp), color = Ink, maxLines = 1) }
    if (landscape) {
        Row(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 22.dp), horizontalArrangement = Arrangement.spacedBy(34.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(150.dp)) {
                Cover(art, Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(12.dp)); time(); Spacer(Modifier.height(4.dp)); SongNames(song)
            }
            Sung(lyrics, song, position, durationMs, Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(art, Modifier.size(58.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { SongNames(song) }
                Spacer(Modifier.width(12.dp))
                time()
            }
            Sung(lyrics, song, position, durationMs, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun Sung(lyrics: Lyrics?, song: Song, position: () -> Long, durationMs: Long, modifier: Modifier) {
    val big = Display.copy(fontSize = 40.sp, lineHeight = 46.sp)
    when (lyrics) {
        is Lyrics.Synced -> Box(modifier, contentAlignment = Alignment.CenterStart) {
            val lines = lyrics.lines
            val line by remember(lines) { derivedStateOf { lines.indexOfLast { it.timeMs <= position() } } }
            AnimatedContent(
                targetState = line,
                transitionSpec = {
                    (fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 5 }).togetherWith(fadeOut(tween(260)) + slideOutVertically(tween(260)) { -it / 5 })
                },
                label = "standbyLine",
            ) { i ->
                Column(Modifier.fillMaxWidth()) {
                    // Before the first line, and through a gap: nothing is being sung.
                    Text(lines.getOrNull(i)?.text?.takeIf { it.isNotBlank() } ?: "♪", style = big, color = Ink, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    lines.drop(i + 1).firstOrNull { it.text.isNotBlank() }?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it.text, style = Display.copy(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal), color = InkFaint, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // Words with no timings: the page moves through them at the song's pace, rather than claim a line.
        is Lyrics.Plain -> {
            val scroll = rememberScrollState()
            LaunchedEffect(durationMs) {
                snapshotFlow { position() / 2_000 }.collect {
                    if (durationMs > 0) scroll.animateScrollTo((scroll.maxValue * position() / durationMs).toInt(), tween(1_800))
                }
            }
            Column(modifier.verticalScroll(scroll, enabled = false).padding(vertical = 8.dp)) {
                Text(lyrics.text, style = Display.copy(fontSize = 24.sp, lineHeight = 33.sp, fontWeight = FontWeight.Normal), color = InkSoft)
            }
        }
        else -> Box(modifier, contentAlignment = Alignment.CenterStart) {
            Column {
                Text(song.title, style = big, color = Ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(if (lyrics == null) "finding the lyrics" else "no lyrics for this song", style = SmallType, color = InkSoft)
            }
        }
    }
}

@Composable
private fun Cover(art: String?, modifier: Modifier) =
    AsyncImage(art, null, contentScale = ContentScale.Crop, modifier = modifier.clip(Corner).background(Color.White.copy(alpha = 0.08f)))

@Composable
private fun ProgressLine(position: () -> Long, durationMs: Long) {
    // Read while drawing: moving this isn't a recomposition.
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val y = size.height / 2f
        val x = if (durationMs > 0) size.width * (position().toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        drawLine(InkFaint, Offset(0f, y), Offset(size.width, y), 2.dp.toPx(), StrokeCap.Round)
        drawLine(Ink, Offset(0f, y), Offset(x, y), 2.dp.toPx(), StrokeCap.Round)
    }
}

/** Over any face for a few seconds after a tap: the transport, the way out, and which face this is. */
@Composable
private fun Controls(playing: Boolean, pager: PagerState, onToggle: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).windowInsetsPadding(WindowInsets.displayCutout)) {
        Button(Icons.Rounded.Close, "Leave standby", onExit, 48.dp, 28.dp, Modifier.align(Alignment.TopEnd).padding(14.dp))
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(Icons.Rounded.SkipPrevious, "Previous", onPrevious, 64.dp, 46.dp)
            Button(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Play", onToggle, 88.dp, 68.dp)
            Button(Icons.Rounded.SkipNext, "Next", onNext, 64.dp, 46.dp)
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(FACES) { i ->
                Box(
                    Modifier.size(7.dp)
                        .graphicsLayer { alpha = if (abs(pager.currentPage + pager.currentPageOffsetFraction - i) < 0.5f) 1f else 0.34f }
                        .clip(Corner).background(Ink),
                )
            }
        }
    }
}

@Composable
private fun Button(icon: ImageVector, label: String, onClick: () -> Unit, size: Dp, iconSize: Dp, modifier: Modifier = Modifier) =
    IconButton(onClick = onClick, modifier = modifier.size(size)) { Icon(icon, label, tint = Ink, modifier = Modifier.size(iconSize)) }
