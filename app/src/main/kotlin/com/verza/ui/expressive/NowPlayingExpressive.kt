package com.verza.ui.expressive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.verza.audio.VisualizerSignal
import com.verza.player.QueueItem
import com.verza.ui.theme.LocalAudioSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Now Playing.
 *
 * The player fills the viewport and the queue lives directly beneath it in the same scroll, so the
 * queue is somewhere you go rather than something that covers what you were looking at. The chevron
 * at the foot of the player says so; tapping it scrolls there.
 *
 * A track change animates rather than cutting: the artwork and title slide and spring in from the
 * side the queue moved, the mask morphs to the next silhouette, and the canvas colour cross-fades
 * (at the root, in MainActivity). Dragging the artwork sideways changes track, which is the gesture
 * the animation implies.
 *
 * Readability is not left to the cover. See ExpressiveColors: every text/background pair is chosen
 * by measured contrast and held above 4.5:1, swept across the hue wheel by ExpressiveColorsTest.
 */
@Composable
fun NowPlayingExpressive(
    onBack: () -> Unit,
    title: String,
    artist: String,
    artworkUrl: String?,
    trackKey: String?,
    coverShapeMode: CoverShapeMode,
    isPlaying: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    sleepTimerActive: Boolean,
    queue: List<QueueItem>,
    currentIndex: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onOpenLyrics: () -> Unit,
    onStartRadio: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenMore: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExpressiveColors.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().background(colors.container),
    ) {
        item(key = "player") {
            PlayerPane(
                modifier = Modifier.fillParentMaxHeight(),
                onBack = onBack,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                trackKey = trackKey,
                coverShapeMode = coverShapeMode,
                isPlaying = isPlaying,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                positionMs = positionMs,
                durationMs = durationMs,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                sleepTimerActive = sleepTimerActive,
                currentIndex = currentIndex,
                queueCount = queue.size,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onToggleLike = onToggleLike,
                onAddToPlaylist = onAddToPlaylist,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onOpenLyrics = onOpenLyrics,
                onStartRadio = onStartRadio,
                onDownload = onDownload,
                onRemoveDownload = onRemoveDownload,
                onOpenSleepTimer = onOpenSleepTimer,
                onOpenMore = onOpenMore,
                onShare = onShare,
                onShowQueue = { scope.launch { listState.animateScrollToItem(1) } },
            )
        }

        item(key = "queue-header") {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp)) {
                Text("UP NEXT", style = MetaLabel, color = colors.onContainerMuted)
                Spacer(Modifier.height(4.dp))
                Text("Queue", style = HeroTitle, color = colors.onContainer)
            }
        }

        itemsIndexed(queue, key = { i, item -> "q-$i-${item.mediaId}" }) { index, item ->
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
                ExpressiveListItem(
                    title = item.title,
                    subtitle = item.artist,
                    artworkUrl = item.artworkUrl,
                    onClick = { onPlayQueueItem(index) },
                    selected = index == currentIndex,
                    position = segmentPositionOf(index, queue.size),
                    trailing = {
                        ExpressiveControl(
                            onClick = { onRemoveQueueItem(index) },
                            icon = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Remove ${item.title} from the queue",
                            container = androidx.compose.ui.graphics.Color.Transparent,
                            content = if (index == currentIndex) colors.onAccent else colors.onSurfaceMuted,
                            iconSize = 18.dp,
                            modifier = Modifier.size(36.dp),
                        )
                    },
                )
            }
        }

        item(key = "queue-tail") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PlayerPane(
    modifier: Modifier,
    onBack: () -> Unit,
    title: String,
    artist: String,
    artworkUrl: String?,
    trackKey: String?,
    coverShapeMode: CoverShapeMode,
    isPlaying: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    sleepTimerActive: Boolean,
    currentIndex: Int,
    queueCount: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onStartRadio: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenMore: () -> Unit,
    onShare: () -> Unit,
    onShowQueue: () -> Unit,
) {
    val colors = LocalExpressiveColors.current

    val stillSignal = remember { MutableStateFlow(VisualizerSignal()) }
    val signal by (LocalAudioSignal.current ?: stillSignal).collectAsState()
    val bass = signal.bass

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f + bass * 0.035f else 1f,
        animationSpec = ExpressiveMotion.ambient(),
        label = "artPulse",
    )
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    // Which way the new track should come in from. Derived from the queue index rather than a
    // timestamp, so going back slides the other way instead of always sliding forward.
    var lastIndex by remember { mutableIntStateOf(currentIndex) }
    val forward = currentIndex >= lastIndex
    if (currentIndex != lastIndex) lastIndex = currentIndex

    val coverShape = rememberCoverShape(coverShapeMode, trackKey)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpressiveControl(
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                container = colors.surface,
                content = colors.onSurface,
                iconSize = 22.dp,
                modifier = Modifier.size(46.dp),
            )
            Spacer(Modifier.weight(1f))
            ExpressiveControl(
                onClick = onShare,
                icon = Icons.Filled.Share,
                contentDescription = "Share",
                container = colors.surface,
                content = colors.onSurface,
                iconSize = 20.dp,
                modifier = Modifier.size(46.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── artwork ──────────────────────────────────────────────────────────────
        // Keyed on the track so a change animates. Slide plus fade, springing in from the side the
        // queue moved; the mask morphs underneath at the same time.
        AnimatedContent(
            targetState = artworkUrl to trackKey,
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (slideInHorizontally(ExpressiveMotion.spatialDefault()) { w -> dir * w / 3 } +
                    fadeIn(ExpressiveMotion.effectsDefault())) togetherWith
                    (slideOutHorizontally(ExpressiveMotion.spatialDefault()) { w -> -dir * w / 3 } +
                        fadeOut(ExpressiveMotion.effectsFast()))
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = "artSwap",
        ) { (url, _) ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .scale(COVER_BOOST)
                    .clip(coverShape)
                    // Drag sideways to change track — the gesture the slide animation implies.
                    .pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if (total < -60f) onNext() else if (total > 60f) onPrevious()
                            },
                        ) { change, drag -> total += drag; change.consume() }
                    },
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // COVER_BOOST pushes the art past its slot. Scaling is a draw-time transform, so
                    // it overlaps its neighbours instead of displacing them, and the bass pulse costs
                    // no relayout.
                    modifier = Modifier.fillMaxSize().background(colors.surface)
                        .scale(artScale * COVER_BOOST),
                )
            }
            }
        }

        // No spacer: the boosted cover is meant to crowd the title slightly, which is what stops
        // the two reading as separate stacked blocks.

        // ── title ────────────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = title to artist,
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (slideInHorizontally(ExpressiveMotion.spatialDefault()) { w -> dir * w / 4 } +
                    fadeIn(ExpressiveMotion.effectsDefault())) togetherWith
                    fadeOut(ExpressiveMotion.effectsFast())
            },
            label = "titleSwap",
        ) { (t, a) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = t,
                    style = HeroDisplay,
                    color = colors.accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = a,
                    style = BodyStrong,
                    color = colors.onContainerMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        VisualizerSeekBar(
            progress = progress,
            onSeek = { f -> onSeek((f * durationMs).toLong()) },
            accent = colors.accent,
            trackColor = colors.accentMuted,
            bands = signal.bands,
            animating = isPlaying,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(formatDuration(positionMs), style = Timecode, color = colors.onContainerMuted)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(durationMs), style = Timecode, color = colors.onContainerMuted)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpressiveControl(
                onClick = onPrevious,
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous track",
                container = colors.accent,
                content = colors.onAccent,
                iconSize = 30.dp,
                modifier = Modifier.size(70.dp),
            )
            PlayPill(
                playing = isPlaying,
                onClick = onTogglePlay,
                container = colors.accent,
                content = colors.onAccent,
                modifier = Modifier.weight(1f).height(70.dp),
            )
            ExpressiveControl(
                onClick = onNext,
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next track",
                container = colors.accent,
                content = colors.onAccent,
                iconSize = 30.dp,
                modifier = Modifier.size(70.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpressiveControl(
                onClick = onToggleShuffle,
                icon = Icons.Filled.Shuffle,
                contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
                container = if (shuffleEnabled) colors.accent else colors.surface,
                content = if (shuffleEnabled) colors.onAccent else colors.onSurface,
                shape = CookieShape,
                iconSize = 20.dp,
                modifier = Modifier.size(50.dp),
            )
            ExpressiveControl(
                onClick = onCycleRepeat,
                icon = if (repeatMode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = when (repeatMode) {
                    1 -> "Repeat one"
                    2 -> "Repeat all"
                    else -> "Repeat off"
                },
                container = if (repeatMode != 0) colors.accent else colors.surface,
                content = if (repeatMode != 0) colors.onAccent else colors.onSurface,
                iconSize = 20.dp,
                modifier = Modifier.size(50.dp),
            )
            ExpressiveControl(
                onClick = onToggleLike,
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isLiked) "Remove from liked songs" else "Add to liked songs",
                container = if (isLiked) colors.accent else colors.surface,
                content = if (isLiked) colors.onAccent else colors.onSurface,
                iconSize = 20.dp,
                modifier = Modifier.size(50.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        // Its own full-width row. Sharing one with the toggles above left no room for six items, so
        // half of them were clipped off the edge rather than wrapped.
        Row(modifier = Modifier.fillMaxWidth()) {
            ExpressiveToolbar(
                modifier = Modifier.fillMaxWidth(),
                spread = true,
                items = listOf(
                    ToolbarItem(Icons.Filled.Lyrics, "Lyrics", onOpenLyrics),
                    ToolbarItem(Icons.Filled.Radio, "Start radio", onStartRadio),
                    ToolbarItem(Icons.Filled.PlaylistAdd, "Add to playlist", onAddToPlaylist),
                    ToolbarItem(
                        icon = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        label = if (isDownloaded) "Remove download" else "Download",
                        onClick = if (isDownloaded) onRemoveDownload else onDownload,
                        active = isDownloaded,
                    ),
                    ToolbarItem(Icons.Filled.Bedtime, "Sleep timer", onOpenSleepTimer, active = sleepTimerActive),
                    ToolbarItem(Icons.Filled.MoreHoriz, "More", onOpenMore),
                ),
                colors = colors,
            )
        }

        Spacer(Modifier.height(6.dp))

        // ── the hint that there is more below ────────────────────────────────────
        QueueHint(count = queueCount, onClick = onShowQueue)
    }
}

/**
 * A chevron and the word "Queue", nudging downward.
 *
 * This replaces a button that opened a sheet. A sheet hid the thing you were looking at to show you
 * a list; making the queue part of the same scroll means it is somewhere you move to, and the hint
 * has to say so — an unlabelled chevron would just look decorative.
 */
@Composable
private fun QueueHint(count: Int, onClick: () -> Unit) {
    val colors = LocalExpressiveColors.current
    val nudge by rememberInfiniteTransition(label = "queueNudge")
        .animateFloat(
            initialValue = 0f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "queueNudgeY",
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.onContainerMuted,
            modifier = Modifier.size(22.dp).graphicsLayer { translationY = nudge },
        )
        Text(
            text = if (count > 1) "Queue · $count" else "Queue",
            style = MetaLabel,
            color = colors.onContainerMuted,
            textAlign = TextAlign.Center,
        )
    }
}

internal fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * How far the artwork is allowed to grow past its layout slot. Chosen so it overlaps the back and
 * share buttons the way the reference does and just kisses the title.
 */
private const val COVER_BOOST = 1.22f
