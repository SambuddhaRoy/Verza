package com.verza.ui.expressive

import androidx.compose.foundation.layout.statusBars
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.verza.audio.AudioOutputs
import com.verza.audio.OutputKind
import com.verza.audio.rememberAudioOutput
import com.verza.player.QueueItem
import com.verza.ui.theme.FontBody
import com.verza.ui.theme.LocalGlassBackdrop
import com.verza.ui.theme.glassSource
import com.verza.ui.theme.liquidGlass
import com.verza.ui.theme.rememberGlassBackdrop
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Now Playing in the Glass design.
 *
 * The cover is the room: blurred out to a field of its own colours behind everything, drifting
 * slowly if album-art motion is on. On it, the cover itself, which shrinks back a little while
 * paused; the title and artist with like and more beside them; a thin scrubber that thickens under
 * a finger; the three transport glyphs; the system volume; and lyrics, output and queue along the
 * foot. Ink is white throughout, whatever the cover: the field under it is darkened enough to
 * carry it.
 *
 * The queue opens in place of the cover rather than below it, with the playing song shrunk into a
 * header, and shuffle and repeat with it, since that is the list they act on.
 *
 * Drag the cover sideways to change song; drag down (or tap the grabber) to close.
 */
@Composable
fun NowPlayingGlass(
    onBack: () -> Unit,
    title: String,
    artist: String,
    artworkUrl: String?,
    albumArtMotion: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    queue: List<QueueItem>,
    currentIndex: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LightStatusIcons()
    val wash = rememberGlassBackdrop()
    var showQueue by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // Pulling down moves the page with the finger, against some resistance, and closes it past a
    // threshold; short of it, it springs back.
    val pull = remember { Animatable(0f) }
    val closeAt = with(density) { 110.dp.toPx() }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        CoverWash(artworkUrl, albumArtMotion, Modifier.fillMaxSize().glassSource(wash))
        CompositionLocalProvider(LocalGlassBackdrop provides wash) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = pull.value }
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 26.dp),
            ) {
                val landscape = maxWidth > maxHeight
                val haptics = rememberHaptics()
                val dismissDrag = Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (pull.value > closeAt * 0.45f) onBack()
                                else pull.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
                            }
                        },
                        onDragCancel = { scope.launch { pull.animateTo(0f) } },
                    ) { change, drag ->
                        if (drag.y > 0f || pull.value > 0f) {
                            change.consume()
                            val before = pull.value
                            val after = (before + drag.y * 0.45f).coerceAtLeast(0f)
                            val line = closeAt * 0.45f
                            if ((before < line) != (after < line)) haptics.threshold()
                            scope.launch { pull.snapTo(after) }
                        }
                    }
                }
                val header: @Composable () -> Unit = {
                    Grabber(onBack, dismissDrag)
                }
                val cover: @Composable (Modifier) -> Unit = { m ->
                    Artwork(artworkUrl, isPlaying, onNext, onPrevious, m)
                }
                val queuePane: @Composable (Modifier) -> Unit = { m ->
                    QueuePane(
                        title = title,
                        artist = artist,
                        artworkUrl = artworkUrl,
                        isLiked = isLiked,
                        queue = queue,
                        currentIndex = currentIndex,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        onToggleLike = onToggleLike,
                        onOpenMore = onOpenMore,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onPlayQueueItem = onPlayQueueItem,
                        onRemoveQueueItem = onRemoveQueueItem,
                        modifier = m,
                    )
                }
                val controls: @Composable ColumnScope.(Boolean) -> Unit = { withTitle ->
                    if (withTitle) TitleRow(title, artist, isLiked, onToggleLike, onOpenMore)
                    Spacer(Modifier.height(18.dp))
                    Scrubber(positionMs, durationMs, onSeek)
                    Spacer(Modifier.height(14.dp))
                    Transport(isPlaying, onTogglePlay, onNext, onPrevious)
                    Spacer(Modifier.height(14.dp))
                    Volume()
                    Spacer(Modifier.height(14.dp))
                    FootRow(
                        showQueue = showQueue,
                        onOpenLyrics = onOpenLyrics,
                        onToggleQueue = { showQueue = !showQueue },
                    )
                }

                if (!landscape) {
                    Column(Modifier.fillMaxSize()) {
                        header()
                        AnimatedContent(
                            targetState = showQueue,
                            transitionSpec = {
                                (fadeIn(tween(220, delayMillis = 60)) + scaleIn(initialScale = 0.96f))
                                    .togetherWith(fadeOut(tween(140)) + scaleOut(targetScale = 0.96f))
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            label = "queueOrCover",
                        ) { queueOpen ->
                            if (queueOpen) queuePane(Modifier.fillMaxSize())
                            else Box(Modifier.fillMaxSize().then(dismissDrag), contentAlignment = Alignment.Center) {
                                cover(Modifier.fillMaxWidth().aspectRatio(1f, matchHeightConstraintsFirst = true))
                            }
                        }
                        Spacer(Modifier.height(if (showQueue) 8.dp else 26.dp))
                        controls(!showQueue)
                        Spacer(Modifier.height(10.dp))
                    }
                } else {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        Box(
                            Modifier.fillMaxHeight().weight(1f).padding(vertical = 18.dp).then(dismissDrag),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (showQueue) queuePane(Modifier.fillMaxSize())
                            else cover(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true))
                        }
                        Column(
                            Modifier.weight(1f).fillMaxHeight().widthIn(max = 480.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            controls(true)
                        }
                    }
                }
            }
        }
    }
}

private val Ink = Color.White
private val InkSoft = Color.White.copy(alpha = 0.62f)
private val InkFaint = Color.White.copy(alpha = 0.28f)

private val TitleStyle = TextStyle(fontFamily = com.verza.ui.theme.FontDisplayCut, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.01).sp)
private val ArtistStyle = TextStyle(fontFamily = FontBody, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 24.sp)
private val SmallStyle = TextStyle(fontFamily = FontBody, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp, fontFeatureSettings = "tnum")

/** Glass for the buttons over the wash: clear and white-edged. */
@Composable
private fun Modifier.washGlass(shape: Shape): Modifier = liquidGlass(
    shape = shape,
    tint = Color.White.copy(alpha = 0.10f),
    fallback = Color.White.copy(alpha = 0.16f),
    blur = 6.dp,
    bend = 10.dp,
)

/**
 * The cover, blurred into a field of its colours. The blur is drawn once; only the layer holding it
 * turns and breathes, which moves a finished texture instead of re-blurring the screen every frame.
 * Two copies, turning opposite ways at different sizes, is what keeps it from looking like a picture
 * being rotated.
 */
@Composable
private fun CoverWash(artworkUrl: String?, motion: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    // Small on purpose: a blur this wide needs no detail, and below Android 12, where there is no
    // blur, a tiny picture stretched is the blur.
    val px = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 160 else 24
    val request = remember(artworkUrl, px) {
        ImageRequest.Builder(context).data(artworkUrl).size(px).crossfade(700).build()
    }
    // With motion off nothing runs at all, rather than an animation from 0 to 0 asking for frames.
    // Read only inside graphicsLayer below, so a frame of drift is a redraw, not a recomposition.
    val spin: State<Float>
    val breathe: State<Float>
    if (motion) {
        val turn = rememberInfiniteTransition(label = "wash")
        spin = turn.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(120_000, easing = LinearEasing)),
            label = "washSpin",
        )
        breathe = turn.animateFloat(
            initialValue = 1f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(tween(14_000), RepeatMode.Reverse),
            label = "washBreathe",
        )
    } else {
        spin = remember { mutableFloatStateOf(0f) }
        breathe = remember { mutableFloatStateOf(1f) }
    }
    BoxWithConstraints(modifier.clipToBounds().background(Color(0xFF101012)), contentAlignment = Alignment.Center) {
        val side = hypot(maxWidth.value, maxHeight.value).dp
        for (layer in 0..1) {
            Box(
                // Each copy is wider than the screen's diagonal, so no edge of it ever turns into view.
                Modifier
                    .requiredSize(side * 1.1f)
                    .graphicsLayer {
                        rotationZ = if (layer == 0) spin.value else -spin.value * 1.4f + 140f
                        scaleX = if (layer == 0) breathe.value else 2.2f - breathe.value
                        scaleY = scaleX
                        alpha = if (layer == 0) 1f else 0.55f
                    },
            ) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(72.dp, BlurredEdgeTreatment.Rectangle),
                )
            }
        }
        // Dark enough for white type on any cover, darker still at the foot, where the controls are.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.30f),
                    0.5f to Color.Black.copy(alpha = 0.30f),
                    1f to Color.Black.copy(alpha = 0.52f),
                ),
            ),
        )
    }
}

@Composable
private fun Grabber(onBack: () -> Unit, drag: Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .then(drag)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack)
            .semantics { contentDescription = "Close Now Playing" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 40.dp, height = 5.dp).clip(CircleShape).background(InkFaint))
    }
}

/**
 * The cover. Full size while playing, drawn back while paused, on a spring with a little bounce:
 * the one place a pause shows itself without a word.
 */
@Composable
private fun Artwork(
    artworkUrl: String?,
    isPlaying: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.84f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 260f),
        label = "artScale",
    )
    val scope = rememberCoroutineScope()
    val swipe = remember { Animatable(0f) }
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    val haptics = rememberHaptics()
    val shape = SquircleShape(16.dp)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                val threshold = 72.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val x = swipe.value
                        if (x < -threshold) next() else if (x > threshold) previous()
                        scope.launch { swipe.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f)) }
                    },
                    onDragCancel = { scope.launch { swipe.animateTo(0f) } },
                ) { change, dx ->
                    change.consume()
                    val before = swipe.value
                    val after = before + dx * 0.6f
                    if ((kotlin.math.abs(before) < threshold) != (kotlin.math.abs(after) < threshold)) haptics.threshold()
                    scope.launch { swipe.snapTo(after) }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = swipe.value
                rotationZ = swipe.value / size.width * 4f
            }
            .shadow(elevation = 30.dp, shape = shape, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.55f))
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(artworkUrl).crossfade(300).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleRow(title: String, artist: String, isLiked: Boolean, onToggleLike: () -> Unit, onOpenMore: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TitleStyle,
                color = Ink,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2_000),
            )
            Text(artist, style = ArtistStyle, color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        val haptics = rememberHaptics()
        WashButton(
            icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isLiked) "Unlike" else "Like",
            onClick = { haptics.tick(); onToggleLike() },
        )
        Spacer(Modifier.width(10.dp))
        WashButton(icon = Icons.Rounded.MoreHoriz, contentDescription = "More", onClick = onOpenMore)
    }
}

@Composable
private fun WashButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit, size: Dp = 36.dp) {
    Box(Modifier.size(size)) {
        GlassCircleButton(
            icon = icon,
            contentDescription = contentDescription,
            onClick = onClick,
            tint = Ink,
            glass = { washGlass(it) },
            iconSize = 20.dp,
        )
    }
}

/**
 * A thin track that thickens while it is held, so the finger has something to hold, and goes back
 * to a hairline once it is let go. Tap anywhere on it to jump.
 */
@Composable
private fun GlassTrack(
    fraction: Float,
    onChange: (Float) -> Unit,
    onRelease: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableFloatStateOf(0f) }
    val change by rememberUpdatedState(onChange)
    val release by rememberUpdatedState(onRelease)
    val thick by animateDpAsState(if (dragging) 12.dp else 7.dp, spring(dampingRatio = 0.7f, stiffness = 700f), label = "trackThick")
    val stretch by animateFloatAsState(if (dragging) 1.025f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "trackStretch")
    val shown = if (dragging) local else fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .graphicsLayer { scaleX = stretch }
            .pointerInput(Unit) {
                detectTapGestures { o -> release((o.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        dragging = true
                        local = (o.x / size.width).coerceIn(0f, 1f)
                        change(local)
                    },
                    onDragEnd = { dragging = false; release(local) },
                    onDragCancel = { dragging = false },
                ) { c, dx ->
                    c.consume()
                    local = (local + dx / size.width).coerceIn(0f, 1f)
                    change(local)
                }
            }
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                setProgress { release(it.coerceIn(0f, 1f)); true }
            }
            .drawBehind {
                val h = thick.toPx()
                val top = (size.height - h) / 2f
                drawRoundRect(InkFaint, Offset(0f, top), Size(size.width, h), CornerRadius(h / 2f))
                if (shown > 0f) drawRoundRect(
                    if (dragging) Ink else Ink.copy(alpha = 0.78f),
                    Offset(0f, top),
                    Size(size.width * shown, h),
                    CornerRadius(h / 2f),
                )
            },
    )
}

@Composable
private fun Scrubber(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    // Where the finger is while scrubbing, and for a moment after letting go, so the bar does not
    // jump back to the old position while the player is still getting to the new one.
    var held by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(held) { if (held != null) { delay(900); held = null } }
    val live = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val at = held ?: live
    Column {
        GlassTrack(
            fraction = at,
            onChange = { held = it },
            onRelease = {
                held = it
                if (durationMs > 0) onSeek((it * durationMs).toLong())
            },
            label = "Position",
        )
        Row(Modifier.fillMaxWidth()) {
            val shownMs = (at * durationMs).toLong()
            Text(formatDuration(shownMs), style = SmallStyle, color = InkSoft)
            Spacer(Modifier.weight(1f))
            Text(
                if (durationMs > 0) "-" + formatDuration((durationMs - shownMs).coerceAtLeast(0)) else "--:--",
                style = SmallStyle,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun Transport(isPlaying: Boolean, onTogglePlay: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIconButton(Icons.Rounded.FastRewind, "Previous track", onPrevious, Ink, size = 64.dp, iconSize = 42.dp)
        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(120)) + scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f)))
                        .togetherWith(fadeOut(tween(90)) + scaleOut(targetScale = 0.6f))
                },
                label = "playPause",
            ) { playing ->
                PlainIconButton(
                    icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    onClick = { haptics.click(); onTogglePlay() },
                    tint = Ink,
                    size = 80.dp,
                    iconSize = 56.dp,
                )
            }
        }
        PlainIconButton(Icons.Rounded.FastForward, "Next track", onNext, Ink, size = 64.dp, iconSize = 42.dp)
    }
}

/** The system's media volume, the same one the volume keys move, and kept in step with them. */
@Composable
private fun Volume() {
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(AudioManager::class.java) }
    val max = remember(audio) { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var level by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()) }
    DisposableEffect(audio) {
        // A volume change, by the keys or anything else, is written to system settings; there is no
        // public broadcast for it.
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                level = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val haptics = rememberHaptics()
    val set: (Float) -> Unit = { v ->
        // Volume is in steps; setting the same step again is a no-op, so this is cheap to call per frame.
        val step = (v * max).roundToInt()
        val was = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (step != was && (step == 0 || step == max)) haptics.tick()
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, step, 0) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null, tint = InkSoft, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        GlassTrack(fraction = level, onChange = set, onRelease = set, label = "Volume", modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = InkSoft, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FootRow(showQueue: Boolean, onOpenLyrics: () -> Unit, onToggleQueue: () -> Unit) {
    val context = LocalContext.current
    val output by rememberAudioOutput()
    val external = output.kind != OutputKind.SPEAKER && output.kind != OutputKind.EARPIECE
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIconButton(Icons.Rounded.Lyrics, "Lyrics", onOpenLyrics, InkSoft, size = 48.dp, iconSize = 24.dp)
        PlainIconButton(
            icon = when (output.kind) {
                OutputKind.BLUETOOTH -> Icons.Rounded.Bluetooth
                OutputKind.WIRED -> Icons.Rounded.Headphones
                OutputKind.USB -> Icons.Rounded.Usb
                OutputKind.HEARING_AID -> Icons.Rounded.Hearing
                OutputKind.REMOTE -> Icons.Rounded.Cast
                else -> Icons.Rounded.Speaker
            },
            contentDescription = "Playing on ${output.name}. Change output.",
            onClick = { AudioOutputs.openSwitcher(context) },
            tint = if (external) Ink else InkSoft,
            size = 48.dp,
            iconSize = 24.dp,
        )
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            // Open, it sits in glass, the state shown the way a selected tab is.
            if (showQueue) Box(Modifier.size(40.dp).washGlass(CircleShape))
            PlainIconButton(
                Icons.AutoMirrored.Rounded.QueueMusic,
                if (showQueue) "Hide queue" else "Queue",
                onToggleQueue,
                if (showQueue) Ink else InkSoft,
                size = 48.dp,
                iconSize = 24.dp,
            )
        }
    }
}

@Composable
private fun QueuePane(
    title: String,
    artist: String,
    artworkUrl: String?,
    isLiked: Boolean,
    queue: List<QueueItem>,
    currentIndex: Int,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onToggleLike: () -> Unit,
    onOpenMore: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    modifier: Modifier,
) {
    val upNext = queue.withIndex().filter { it.index > currentIndex }
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .shadow(12.dp, SquircleShape(10.dp))
                    .clip(SquircleShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = TitleStyle.copy(fontSize = 17.sp, lineHeight = 22.sp), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, style = ArtistStyle.copy(fontSize = 15.sp, lineHeight = 20.sp), color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            WashButton(
                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                onClick = onToggleLike,
            )
            Spacer(Modifier.width(10.dp))
            WashButton(icon = Icons.Rounded.MoreHoriz, contentDescription = "More", onClick = onOpenMore)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Playing Next", style = TitleStyle.copy(fontSize = 17.sp), color = Ink)
                Text(
                    if (upNext.isEmpty()) "Nothing after this song" else "${upNext.size} song${if (upNext.size == 1) "" else "s"}",
                    style = SmallStyle.copy(fontSize = 13.sp),
                    color = InkSoft,
                )
            }
            QueueToggle(Icons.Rounded.Shuffle, "Shuffle", shuffleEnabled, onToggleShuffle)
            Spacer(Modifier.width(8.dp))
            QueueToggle(
                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> "Repeat one"
                    Player.REPEAT_MODE_ALL -> "Repeat all"
                    else -> "Repeat off"
                },
                repeatMode != Player.REPEAT_MODE_OFF,
                onCycleRepeat,
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().fadeEdges(),
            contentPadding = PaddingValues(vertical = 10.dp),
        ) {
            itemsIndexed(upNext, key = { _, it -> "q-${it.index}-${it.value.mediaId}" }) { _, entry ->
                val item = entry.value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPlayQueueItem(entry.index) }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = item.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = TextStyle(fontFamily = FontBody, fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.artist, style = TextStyle(fontFamily = FontBody, fontSize = 13.sp), color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlainIconButton(
                        Icons.Rounded.Close,
                        "Remove ${item.title} from the queue",
                        { onRemoveQueueItem(entry.index) },
                        InkSoft,
                        size = 40.dp,
                        iconSize = 18.dp,
                    )
                }
            }
        }
    }
}

/** Shuffle and repeat: glass when off, a white lozenge with dark glyph when on, as Apple draws it. */
@Composable
private fun QueueToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val shape = SquircleShape(12.dp)
    val haptics = rememberHaptics()
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 34.dp)
            .then(if (on) Modifier.clip(shape).background(Ink.copy(alpha = 0.88f)) else Modifier.washGlass(shape))
            .clickable { haptics.tick(); onClick() }
            .semantics { contentDescription = label + if (on) ", on" else ", off" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (on) Color.Black.copy(alpha = 0.75f) else Ink, modifier = Modifier.size(19.dp))
    }
}

/** Fades a scrolling list out at its top and bottom edges instead of cutting it off. */
private fun Modifier.fadeEdges(edge: Dp = 18.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val f = edge.toPx() / size.height
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                f to Color.Black,
                1f - f to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

/**
 * A page that sits on the cover the way Now Playing does (Lyrics): the still wash behind it, and
 * the app's theme swapped for white ink on a clear ground, whatever the system's light or dark.
 */
@Composable
fun OnCoverWash(artworkUrl: String?, content: @Composable () -> Unit) {
    LightStatusIcons()
    val ink = remember {
        glassColors(cover = null, dark = true).copy(
            container = Color.Transparent,
            onContainerMuted = Color.White.copy(alpha = 0.62f),
            accent = Color.White,
            onAccent = Color.Black,
            surface = Color.White.copy(alpha = 0.12f),
            onSurfaceMuted = Color.White.copy(alpha = 0.62f),
        )
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CoverWash(artworkUrl, motion = false, Modifier.fillMaxSize())
        // The raw status-bar height: the NavHost has already consumed the inset, so the
        // consumption-aware statusBarsPadding() would give nothing here.
        val top = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
        Box(Modifier.fillMaxSize().padding(top = top)) {
            com.verza.ui.theme.VerzaTheme(scheme = expressiveColorScheme(ink), design = com.verza.ui.theme.DesignScheme.GLASS) {
                CompositionLocalProvider(LocalExpressiveColors provides ink, content = content)
            }
        }
    }
}

/**
 * The wash is dark whatever the theme, so the status bar's icons go light while Now Playing is up,
 * and back to what they were after.
 */
@Composable
private fun LightStatusIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val status = controller.isAppearanceLightStatusBars
        val nav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = status
            controller.isAppearanceLightNavigationBars = nav
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
