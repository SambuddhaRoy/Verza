package com.verza.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.verza.ui.expressive.CoverWash
import com.verza.ui.expressive.FitTitle
import com.verza.ui.expressive.LightStatusIcons
import com.verza.ui.expressive.PlainIconButton
import com.verza.ui.expressive.coverBrightness
import com.verza.ui.expressive.findActivity
import com.verza.ui.expressive.rememberHaptics
import com.verza.ui.expressive.tapHaptics
import com.verza.ui.expressive.typeScrim
import com.verza.ui.theme.CornerShape
import com.verza.ui.theme.FontBody
import com.verza.ui.theme.FontDisplayCut
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop

private const val FACES = 3
private const val FACE_CLOCK = 0
private const val FACE_COVER = 1

private val Ink = Color.White
private val InkSoft = Color.White.copy(alpha = 0.72f)
private val InkFaint = Color.White.copy(alpha = 0.30f)

private val TitleStyle = TextStyle(fontFamily = FontDisplayCut, fontWeight = FontWeight.Medium, letterSpacing = (-0.022).em)
private val ClockDigits = TextStyle(fontFamily = FontDisplayCut, fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
private val SmallType = TextStyle(fontFamily = FontBody, fontSize = 15.sp, lineHeight = 20.sp)

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
fun AmbientDisplay(
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExit: () -> Unit,
    settings: SettingsViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
) {
    StandbyWindow()
    BackHandler { onExit() }
    val context = LocalContext.current
    val haptics = rememberHaptics()
    // Through a lambda from here down: the position moves several times a second, and only the
    // progress line and the lyrics have any use for it.
    val latestPosition by rememberUpdatedState(positionMs)
    val position = remember { { latestPosition } }

    val now = rememberMinute()
    val clock = remember(now) {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        now.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }
    val date = remember(now) {
        now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())).lowercase()
    }
    val night = now.hour >= 22 || now.hour < 7

    val brightness by produceState<Float?>(null, artworkUrl) {
        value = artworkUrl?.let { coverBrightness(context, it) }
    }
    val lyrics by lyricsViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(title, artist, durationMs) { lyricsViewModel.load(title, artist, durationMs) }

    val pager = rememberPagerState(initialPage = settings.standbyFace.value.coerceIn(0, FACES - 1)) { FACES }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect {
            haptics.detent()
            settings.setStandbyFace(it)
        }
    }

    // A tap shows the controls, and any tap on them keeps them up a little longer.
    var controls by remember { mutableStateOf(false) }
    var touched by remember { mutableIntStateOf(0) }
    LaunchedEffect(controls, touched) {
        if (controls) {
            delay(4_000)
            controls = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .tapHaptics()
            .pointerInput(Unit) { detectTapGestures { controls = !controls } },
    ) {
        // The clock is on black and the other two on the cover's colours, crossing over as you swipe.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, 1f) },
        ) {
            CoverWash(artworkUrl, motion = false, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = typeScrim(brightness ?: 0.5f))))
        }
        // Moved in the layer, so a minute's shift is a redraw.
        val shift = with(LocalDensity.current) { 3.dp.toPx() }
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout)
                .graphicsLayer {
                    translationX = (now.minute % 5 - 2) * shift
                    translationY = (now.minute % 3 - 1) * shift
                },
        ) { face ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val landscape = maxWidth > maxHeight
                when (face) {
                    FACE_CLOCK -> ClockFace(clock, date, title, artist, artworkUrl, brightness, landscape)
                    FACE_COVER -> CoverFace(clock, date, title, artist, artworkUrl, position, durationMs, landscape)
                    else -> LyricsFace(clock, title, artist, artworkUrl, lyrics, position, durationMs, landscape)
                }
            }
        }
        // At night, down to what a dark room wants. Only over the faces: the controls stay readable.
        if (night) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

        AnimatedVisibility(controls, enter = fadeIn(tween(160)), exit = fadeOut(tween(320))) {
            Controls(
                isPlaying = isPlaying,
                pager = pager,
                onTogglePlay = { touched++; onTogglePlay() },
                onNext = { touched++; onNext() },
                onPrevious = { touched++; onPrevious() },
                onExit = onExit,
            )
        }
    }
}

/** The whole screen, in whichever way up the phone is, and awake, until this goes away. */
@Composable
private fun StandbyWindow() {
    LightStatusIcons()
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = view.context.findActivity()
        val window = activity?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
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
        while (true) {
            value = LocalDateTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    return now
}

/** The clock at its largest, with what's playing and the date along the foot. */
@Composable
private fun ClockFace(
    clock: String,
    date: String,
    title: String,
    artist: String,
    artworkUrl: String?,
    brightness: Float?,
    landscape: Boolean,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 18.dp)) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // On its side one line, which is the wider. Upright the hours over the minutes.
            val text = if (landscape) clock else clock.replace(":", "\n").padStart(5, '0')
            CoverClock(text, clock, artworkUrl, brightness)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(artworkUrl, Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = SmallType.copy(fontWeight = FontWeight.Medium), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, style = SmallType.copy(fontSize = 13.sp, lineHeight = 17.sp), color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (landscape) {
                Spacer(Modifier.width(16.dp))
                Text(date, style = SmallType, color = InkSoft, maxLines = 1)
            }
        }
    }
}

/**
 * [text], as large as fits, with the cover showing through it. A dark cover is lifted just far enough
 * to read on black, however dark it is, and keeps what colour it has.
 */
@Composable
private fun CoverClock(text: String, spoken: String, artworkUrl: String?, brightness: Float?) {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val measurer = rememberTextMeasurer()
        val layout = remember(text, constraints.maxWidth, constraints.maxHeight) {
            val unbounded = Constraints()
            val base = ClockDigits.copy(fontSize = 100.sp, lineHeight = 88.sp, textAlign = TextAlign.Center)
            val probe = measurer.measure(text, base, constraints = unbounded)
            val scale = min(constraints.maxWidth * 0.98f / probe.size.width, constraints.maxHeight * 0.98f / probe.size.height)
            measurer.measure(
                text,
                base.copy(fontSize = 100.sp * scale, lineHeight = 88.sp * scale),
                constraints = unbounded,
            )
        }
        val lift = ((0.62f - (brightness ?: 0f)) / (1f - (brightness ?: 0f))).coerceIn(0f, 1f)
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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(artworkUrl).crossfade(600).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** The cover as large as the screen has room for, and a small clock. */
@Composable
private fun CoverFace(
    clock: String,
    date: String,
    title: String,
    artist: String,
    artworkUrl: String?,
    position: () -> Long,
    durationMs: Long,
    landscape: Boolean,
) {
    val details: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            FitTitle(text = title, style = TitleStyle, color = Ink, maxLines = 2, maxSp = 36f, screenShare = if (landscape) 0.34f else 0.12f)
            Spacer(Modifier.height(2.dp))
            Text(artist, style = SmallType.copy(fontSize = 17.sp, lineHeight = 22.sp), color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            ProgressLine(position, durationMs)
        }
    }
    val time: @Composable () -> Unit = {
        Text(clock, style = ClockDigits.copy(fontSize = 56.sp, lineHeight = 60.sp), color = Ink, maxLines = 1)
        Text(date, style = SmallType, color = InkSoft, maxLines = 1)
    }
    if (landscape) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(artworkUrl, Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true))
            Column(Modifier.weight(1f)) {
                time()
                Spacer(Modifier.height(22.dp))
                details(Modifier)
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            time()
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                Cover(artworkUrl, Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true))
            }
            details(Modifier.fillMaxWidth())
        }
    }
}

/** The line being sung, large, with the next one under it. The cover and clock go small to make room. */
@Composable
private fun LyricsFace(
    clock: String,
    title: String,
    artist: String,
    artworkUrl: String?,
    lyrics: LyricsUiState,
    position: () -> Long,
    durationMs: Long,
    landscape: Boolean,
) {
    val time: @Composable () -> Unit = {
        Text(clock, style = ClockDigits.copy(fontSize = 34.sp, lineHeight = 38.sp), color = Ink, maxLines = 1)
    }
    val song: @Composable () -> Unit = {
        Text(title, style = SmallType.copy(fontWeight = FontWeight.Medium), color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(artist, style = SmallType.copy(fontSize = 13.sp, lineHeight = 17.sp), color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (landscape) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(150.dp)) {
                Cover(artworkUrl, Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(12.dp))
                time()
                Spacer(Modifier.height(4.dp))
                song()
            }
            Lyrics(lyrics, title, position, durationMs, Modifier.weight(1f).fillMaxHeight(), landscape)
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(artworkUrl, Modifier.size(58.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { song() }
                Spacer(Modifier.width(12.dp))
                time()
            }
            Lyrics(lyrics, title, position, durationMs, Modifier.weight(1f).fillMaxWidth(), landscape)
        }
    }
}

@Composable
private fun Lyrics(
    state: LyricsUiState,
    title: String,
    position: () -> Long,
    durationMs: Long,
    modifier: Modifier,
    landscape: Boolean,
) {
    val share = if (landscape) 0.46f else 0.34f
    when (state) {
        is LyricsUiState.Synced -> Box(modifier, contentAlignment = Alignment.CenterStart) {
            val lines = state.lines
            val line by remember(lines) { derivedStateOf { lines.indexOfLast { it.timeMs <= position() } } }
            AnimatedContent(
                targetState = line,
                transitionSpec = {
                    (fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 5 })
                        .togetherWith(fadeOut(tween(260)) + slideOutVertically(tween(260)) { -it / 5 })
                },
                label = "standbyLine",
            ) { i ->
                Column(Modifier.fillMaxWidth()) {
                    // Before the first line, and through a gap: nothing is being sung.
                    val sung = lines.getOrNull(i)?.text?.takeIf { it.isNotBlank() }
                    FitTitle(text = sung ?: "♪", style = TitleStyle, color = Ink, maxLines = 4, maxSp = 46f, screenShare = share)
                    lines.drop(i + 1).firstOrNull { it.text.isNotBlank() }?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it.text, style = TitleStyle.copy(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal), color = InkFaint, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // Words with no timings: the page moves through them at the song's pace, rather than claim a line.
        is LyricsUiState.Plain -> {
            val scroll = rememberScrollState()
            LaunchedEffect(durationMs) {
                snapshotFlow { position() / 2_000 }.collect {
                    if (durationMs > 0) scroll.animateScrollTo((scroll.maxValue * position() / durationMs).toInt(), tween(1_800))
                }
            }
            Column(modifier.verticalScroll(scroll, enabled = false).padding(vertical = 8.dp)) {
                Text(state.text, style = TitleStyle.copy(fontSize = 24.sp, lineHeight = 33.sp, fontWeight = FontWeight.Normal), color = InkSoft)
            }
        }
        else -> Box(modifier, contentAlignment = Alignment.CenterStart) {
            Column {
                FitTitle(text = title, style = TitleStyle, color = Ink, maxLines = 3, maxSp = 46f, screenShare = share)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (state is LyricsUiState.Loading || state is LyricsUiState.Idle) "finding the lyrics" else "no lyrics for this song",
                    style = SmallType,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun Cover(artworkUrl: String?, modifier: Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(artworkUrl).crossfade(600).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CornerShape).background(Color.White.copy(alpha = 0.08f)),
    )
}

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
private fun Controls(
    isPlaying: Boolean,
    pager: PagerState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExit: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).windowInsetsPadding(WindowInsets.displayCutout)) {
        PlainIconButton(
            Icons.Filled.Close,
            "Leave standby",
            onExit,
            Ink,
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
            size = 48.dp,
            iconSize = 28.dp,
        )
        Row(
            Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlainIconButton(Icons.Filled.SkipPrevious, "Previous track", onPrevious, Ink, size = 64.dp, iconSize = 46.dp)
            PlainIconButton(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (isPlaying) "Pause" else "Play",
                onTogglePlay,
                Ink,
                size = 88.dp,
                iconSize = 68.dp,
            )
            PlainIconButton(Icons.Filled.SkipNext, "Next track", onNext, Ink, size = 64.dp, iconSize = 46.dp)
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(FACES) { i ->
                Box(
                    Modifier
                        .size(7.dp)
                        .graphicsLayer {
                            alpha = if (abs(pager.currentPage + pager.currentPageOffsetFraction - i) < 0.5f) 1f else 0.34f
                        }
                        .clip(CornerShape)
                        .background(Ink),
                )
            }
        }
    }
}
