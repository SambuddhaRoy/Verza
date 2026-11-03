package com.verza.ui.expressive

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.verza.innertube.TrackLinks
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.verza.player.QueueItem
import com.verza.ui.sleeve.grain
import com.verza.ui.sleeve.vignette
import com.verza.ui.theme.FontBody
import com.verza.ui.theme.LocalGlassBackdrop
import com.verza.ui.theme.glassSource
import com.verza.ui.theme.liquidGlass
import com.verza.ui.theme.rememberGlassBackdrop
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.launch

/**
 * Now Playing in the Glass design: the poster.
 *
 * The cover fills the screen, sharp, and dissolves at the top into the moving field of its own
 * colours and at the foot into near black. Over it, from the top: Close, Now Playing and Lyrics; the
 * title, large; the artist and album, which go to their pages; and a line of where you are. At the
 * foot: six actions, the scrubber, and the transport.
 *
 * Under the type there is a scrim as dark as this cover needs and no darker ([typeScrim]), so a dark
 * cover keeps its moving colour and white type still reads on a white one.
 *
 * The queue opens over the picture, as a numbered list with the playing song large.
 *
 * Drag the picture sideways to change song; drag it or the type above it down to close.
 */
@Composable
fun NowPlayingGlass(
    onBack: () -> Unit,
    title: String,
    artist: String,
    links: TrackLinks?,
    artworkUrl: String?,
    albumArtMotion: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    positionMs: Long,
    durationMs: Long,
    audioFormat: String?,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    queue: List<QueueItem>,
    currentIndex: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onStartRadio: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenLinerNotes: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LightStatusIcons()
    val wash = rememberGlassBackdrop()
    var showQueue by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    // Pulling down moves the page with the finger, against some resistance, and closes it past a
    // threshold; short of it, it springs back.
    val pull = remember { Animatable(0f) }
    val closeAt = with(density) { 110.dp.toPx() }
    val skip = rememberSkipSwipe(onNext, onPrevious)
    // Where the type ends, as a share of the screen's height: the scrim follows it, or a three-line
    // title puts its last lines out on the bare picture.
    var typeEnd by remember { mutableFloatStateOf(0.30f) }
    val scrim by produceState(0.4f, artworkUrl) {
        artworkUrl?.let { url -> coverBrightness(context, url) }?.let { value = typeScrim(it) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // A tablet or an open fold: the queue has the room to stay open beside the player. Tall as
        // well as wide, because a phone on its side is 900dp wide with no height for this layout.
        val wide = maxWidth >= 780.dp && maxHeight >= 600.dp
        val landscape = maxWidth > maxHeight
        val poster = !wide && !landscape
        val screenPx = constraints.maxHeight.toFloat()
        val queueWidth = (maxWidth * 0.36f).coerceIn(320.dp, 440.dp)
        Box(Modifier.fillMaxSize().glassSource(wash)) {
            CoverWash(artworkUrl, albumArtMotion, Modifier.fillMaxSize(), dim = !poster)
            if (poster) {
                Poster(artworkUrl, albumArtMotion, isPlaying, skip.offset, Modifier.fillMaxSize())
                PosterGrade(showQueue, typeEnd, scrim, Modifier.fillMaxSize())
            } else {
                // Type all over the field here, so the scrim is all over it too.
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.50f)))
            }
        }
        CompositionLocalProvider(LocalGlassBackdrop provides wash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = pull.value }
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 22.dp),
            ) {
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
                val pills: @Composable () -> Unit = {
                    PillRow(onBack, onNowPlaying = { showQueue = false }, onOpenLyrics, dismissDrag)
                }
                val masthead: @Composable (Modifier, Int, Float, Boolean) -> Unit = { m, lines, maxSp, line ->
                    Masthead(
                        title, artist, links, queue.size, currentIndex, audioFormat,
                        onOpenArtist, onOpenAlbum, onOpenLinerNotes,
                        m, lines, maxSp, line,
                    )
                }
                val cover: @Composable (Modifier) -> Unit = { m ->
                    Artwork(artworkUrl, isPlaying, skip, m)
                }
                val queuePane: @Composable (Modifier) -> Unit = { m ->
                    QueuePane(queue, currentIndex, onPlayQueueItem, onRemoveQueueItem, m)
                }
                val controls: @Composable ColumnScope.() -> Unit = {
                    ActionRow(
                        showQueue = showQueue,
                        // Null where the queue is always open beside the player.
                        onToggleQueue = if (wide) null else ({ showQueue = !showQueue }),
                        isLiked = isLiked,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        onStartRadio = onStartRadio,
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        onDownload = onDownload,
                        onRemoveDownload = onRemoveDownload,
                        onOpenMore = onOpenMore,
                    )
                    Spacer(Modifier.height(6.dp))
                    Scrubber(positionMs, durationMs, onSeek)
                    Spacer(Modifier.height(2.dp))
                    Transport(isPlaying, shuffleEnabled, repeatMode, onTogglePlay, onNext, onPrevious, onToggleShuffle, onCycleRepeat)
                }

                if (wide) {
                    // The player is a column of its own, the cover over the controls, and stops at a
                    // width that still reads as a player. The queue takes the rest of the window.
                    Row(
                        Modifier.fillMaxSize().padding(top = 16.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                    ) {
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Column(Modifier.fillMaxHeight().widthIn(max = 560.dp)) {
                                pills()
                                Spacer(Modifier.height(16.dp))
                                Box(Modifier.weight(1f).fillMaxWidth().then(dismissDrag), contentAlignment = Alignment.Center) {
                                    cover(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true))
                                }
                                Spacer(Modifier.height(16.dp))
                                masthead(dismissDrag, 2, 44f, true)
                                Spacer(Modifier.height(8.dp))
                                controls()
                            }
                        }
                        queuePane(Modifier.width(queueWidth).fillMaxHeight())
                    }
                } else if (!landscape) {
                    // A tablet held upright would otherwise get a phone's player at twice the size.
                    Column(Modifier.fillMaxHeight().widthIn(max = 620.dp).align(Alignment.Center)) {
                        Spacer(Modifier.height(24.dp))
                        pills()
                        Spacer(Modifier.height(20.dp))
                        masthead(
                            dismissDrag.onGloballyPositioned { typeEnd = it.boundsInRoot().bottom / screenPx },
                            3, 44f, true,
                        )
                        AnimatedContent(
                            targetState = showQueue,
                            transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(140))) },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            label = "queueOrPicture",
                        ) { queueOpen ->
                            if (queueOpen) queuePane(Modifier.fillMaxSize())
                            // Nothing to draw: the picture is behind all of this, and only moves.
                            else Box(Modifier.fillMaxSize().then(dismissDrag).then(skip.drag))
                        }
                        controls()
                        Spacer(Modifier.height(8.dp))
                    }
                } else {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Box(
                            Modifier.fillMaxHeight().weight(1f).padding(vertical = 14.dp).then(dismissDrag),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (showQueue) queuePane(Modifier.fillMaxSize())
                            // Not fillMaxHeight: a square the height of the window is wider than this
                            // half, and it ran off the left edge.
                            else cover(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true))
                        }
                        Column(
                            Modifier.weight(1f).fillMaxHeight().widthIn(max = 480.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            pills()
                            Spacer(Modifier.height(4.dp))
                            masthead(dismissDrag, 2, 30f, false)
                            controls()
                        }
                    }
                }
            }
        }
    }
}

private val Ink = Color.White
private val InkSoft = Color.White.copy(alpha = 0.86f)
private val InkFaint = Color.White.copy(alpha = 0.34f)

// The title: Inter's display cut, the Helvetica of what the app bundles, tracked in as a large
// grotesque wants to be.
private val TitleStyle = TextStyle(fontFamily = com.verza.ui.theme.FontDisplayCut, fontWeight = FontWeight.Medium, letterSpacing = (-0.022).em)
private val BylineStyle = TextStyle(fontFamily = FontBody, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp)
private val EyebrowStyle = TextStyle(fontFamily = FontBody, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.16.em)
private val TimeStyle = TextStyle(fontFamily = FontBody, fontSize = 13.sp, lineHeight = 17.sp, fontFeatureSettings = "tnum")

/** Glass for the buttons over the wash: clear and white-edged. */
@Composable
private fun Modifier.washGlass(shape: Shape): Modifier = liquidGlass(
    shape = shape,
    tint = Color.White.copy(alpha = 0.10f),
    fallback = Color.White.copy(alpha = 0.16f),
    blur = 6.dp,
    bend = 4.dp,
)

/**
 * The cover, blurred into a field of its colours. The blur is drawn once; only the layer holding it
 * turns and breathes, which moves a finished texture instead of re-blurring the screen every frame.
 * Two copies, turning opposite ways at different sizes, is what keeps it from looking like a picture
 * being rotated.
 */
@Composable
internal fun CoverWash(artworkUrl: Any?, motion: Boolean, modifier: Modifier, dim: Boolean = true) {
    Box(modifier.clipToBounds().background(Color(0xFF101012))) {
        // The desktop's flowing cover where there is AGSL to run it (13+), the drifting one below that.
        val flowing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            FluidWash(artworkUrl, motion, Modifier.fillMaxSize())
        if (!flowing) DriftWash(artworkUrl, motion, Modifier.fillMaxSize())
        // Dark enough for white type on any cover, darker still at the foot, where the controls are.
        if (dim) Box(
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

/** Two copies of the blurred cover turning against each other: the wash before Android 13. */
@Composable
private fun DriftWash(artworkUrl: Any?, motion: Boolean, modifier: Modifier) {
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
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
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
    }
}

/**
 * The cover, filling the screen, in film grain under a vignette, dissolving into the wash behind it.
 * The masks stay put while the picture moves under them, on a swipe or its slow drift.
 */
@Composable
private fun Poster(
    artworkUrl: String?,
    motion: Boolean,
    isPlaying: Boolean,
    swipe: Animatable<Float, *>,
    modifier: Modifier,
) {
    val dim by animateFloatAsState(if (isPlaying) 0f else 0.32f, tween(400), label = "posterDim")
    // With motion off nothing runs at all. Read only inside graphicsLayer: a redraw, not a recomposition.
    val drift: State<Float> = if (motion) {
        rememberInfiniteTransition(label = "poster").animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "posterDrift",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    Box(
        modifier
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
                // Eased through several stops: a straight ramp shows as a line across the picture.
                drawRect(
                    Brush.radialGradient(
                        0f to Color.White,
                        0.40f to Color.White,
                        0.60f to Color.White.copy(alpha = 0.90f),
                        0.75f to Color.White.copy(alpha = 0.62f),
                        0.88f to Color.White.copy(alpha = 0.30f),
                        1f to Color.Transparent,
                        center = Offset(size.width * 0.5f, size.height * 0.46f),
                        radius = size.width * 1.18f,
                    ),
                    blendMode = BlendMode.DstIn,
                )
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.07f to Color.White.copy(alpha = 0.18f),
                        0.15f to Color.White.copy(alpha = 0.45f),
                        0.24f to Color.White.copy(alpha = 0.72f),
                        0.36f to Color.White.copy(alpha = 0.92f),
                        0.50f to Color.White,
                        1f to Color.White,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        Box(Modifier.fillMaxSize().grain(0.08f).vignette(0.40f)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(artworkUrl).crossfade(320).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = drift.value
                    scaleY = drift.value
                    translationX = swipe.value
                    rotationZ = swipe.value / size.width * 3f
                },
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
    }
}

/**
 * What the type and controls sit on: [scrim] down to the end of the type, nothing over the middle of
 * the picture, and near black at the foot. With the queue open it reaches across the middle too.
 */
@Composable
private fun PosterGrade(showQueue: Boolean, typeEnd: Float, scrim: Float, modifier: Modifier) {
    val top by animateFloatAsState(scrim, tween(500), label = "gradeTop")
    val end by animateFloatAsState(typeEnd.coerceAtMost(0.44f), tween(260), label = "gradeEnd")
    val middle by animateFloatAsState(if (showQueue) (scrim + 0.12f).coerceAtMost(0.7f) else 0f, tween(260), label = "gradeMiddle")
    Box(
        modifier.drawBehind {
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = top),
                    end to Color.Black.copy(alpha = top),
                    end + 0.05f to Color.Black.copy(alpha = maxOf(top * 0.55f, middle)),
                    end + 0.12f to Color.Black.copy(alpha = middle),
                    0.60f to Color.Black.copy(alpha = middle),
                    0.70f to Color.Black.copy(alpha = maxOf(0.40f, middle)),
                    0.79f to Color.Black.copy(alpha = 0.66f),
                    1f to Color.Black.copy(alpha = 0.80f),
                ),
            )
        },
    )
}

/** Close, Now Playing and Lyrics. Now Playing is lit, and brings you back here from the queue. */
@Composable
private fun PillRow(onClose: () -> Unit, onNowPlaying: () -> Unit, onLyrics: () -> Unit, drag: Modifier) {
    Row(
        modifier = Modifier.fillMaxWidth().then(drag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterPill("Close", "Close Now Playing", onClick = onClose)
        PosterPill("Now Playing", "Now Playing", filled = true, onClick = onNowPlaying)
        PosterPill("Lyrics", "Lyrics", onClick = onLyrics)
    }
}

@Composable
private fun PosterPill(label: String, description: String, onClick: () -> Unit, filled: Boolean = false) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .then(if (filled) Modifier.clip(com.verza.ui.theme.CornerShape).background(Ink.copy(alpha = 0.92f)) else Modifier.washGlass(com.verza.ui.theme.CornerShape))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = FontBody, fontWeight = FontWeight.Medium, fontSize = 14.sp),
            color = if (filled) Color.Black else Ink,
            maxLines = 1,
        )
    }
}

/** A swiped cover: where it has got to, and the gesture that moves it and changes the song. */
private class SkipSwipe(val offset: Animatable<Float, *>, val drag: Modifier)

@Composable
private fun rememberSkipSwipe(onNext: () -> Unit, onPrevious: () -> Unit): SkipSwipe {
    val scope = rememberCoroutineScope()
    val swipe = remember { Animatable(0f) }
    val next by rememberUpdatedState(onNext)
    val previous by rememberUpdatedState(onPrevious)
    val haptics = rememberHaptics()
    return SkipSwipe(
        swipe,
        Modifier.pointerInput(Unit) {
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
                if ((abs(before) < threshold) != (abs(after) < threshold)) haptics.threshold()
                scope.launch { swipe.snapTo(after) }
            }
        },
    )
}

/**
 * The cover. Paused, it dims a little and settles back a step, the light going down rather than the
 * picture shrinking away.
 */
@Composable
private fun Artwork(
    artworkUrl: String?,
    isPlaying: Boolean,
    skip: SkipSwipe,
    modifier: Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.97f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 260f),
        label = "artScale",
    )
    val dim by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 0.32f,
        animationSpec = tween(400),
        label = "artDim",
    )
    val swipe = skip.offset
    val shape = com.verza.ui.theme.CornerShape
    Box(
        modifier = modifier
            .then(skip.drag)
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
            modifier = Modifier.fillMaxSize().drawWithContent {
                drawContent()
                if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
            },
        )
    }
}

/**
 * The title, large; under it the artist and the album, smaller, each of which goes to its page; then
 * the line that says where you are, and leads to the liner notes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Masthead(
    title: String,
    artist: String,
    links: TrackLinks?,
    queueSize: Int,
    currentIndex: Int,
    audioFormat: String?,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenLinerNotes: () -> Unit,
    modifier: Modifier,
    maxLines: Int,
    maxSp: Float,
    eyebrow: Boolean,
) {
    Column(Modifier.fillMaxWidth().then(modifier)) {
        FitTitle(text = title, style = TitleStyle, color = Ink, maxLines = maxLines, maxSp = maxSp, screenShare = 0.17f)
        // Each artist goes to their own page. What doesn't fit on a line wraps; a landscape phone has
        // room for the one line.
        FlowRow(maxLines = if (eyebrow) 2 else 1) {
            val artists = links?.artists.orEmpty()
            if (artists.isEmpty()) Byline(artist.ifBlank { "Unknown artist" }, "", null)
            artists.forEachIndexed { i, a ->
                if (i > 0) BylineGap(",  ")
                Byline(a.name, "Go to artist") { onOpenArtist(a.id) }
            }
            links?.album?.let { album ->
                BylineGap("  \u00B7  ")
                Byline(album.name, "Go to album") { onOpenAlbum(album.id) }
            }
        }
        if (eyebrow) {
            // Lossless says so in place of the name: the one fact here worth the room.
            val place = if (queueSize > 1) "${currentIndex + 1} / $queueSize  \u00B7  " else ""
            val source = audioFormat ?: "VERZA"
            Text(
                "$source  \u00B7  ${place}LINER NOTES",
                style = EyebrowStyle,
                color = InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable(onClick = onOpenLinerNotes)
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun BylineGap(text: String) {
    Text(text, style = BylineStyle, color = InkSoft, modifier = Modifier.padding(vertical = 5.dp))
}

/** An artist or the album: plain text with a page behind it, or just the text if there is none. */
@Composable
private fun Byline(text: String, action: String, onClick: (() -> Unit)?) {
    Text(
        text,
        style = BylineStyle,
        color = InkSoft,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(onClick = onClick).semantics { contentDescription = "$action $text" },
            )
            .padding(vertical = 5.dp),
    )
}

/** Queue, like, add to a playlist, radio, download, more: six bare glyphs. */
@Composable
private fun ActionRow(
    showQueue: Boolean,
    onToggleQueue: (() -> Unit)?,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onStartRadio: () -> Unit,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onOpenMore: () -> Unit,
) {
    val accent = LocalExpressiveColors.current.accent
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onToggleQueue != null) ActionIcon(
            Icons.AutoMirrored.Filled.QueueMusic,
            if (showQueue) "Hide queue" else "Queue",
            onToggleQueue,
            if (showQueue) accent else InkSoft,
        )
        ActionIcon(
            if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            if (isLiked) "Unlike" else "Like",
            { haptics.tick(); onToggleLike() },
            if (isLiked) accent else InkSoft,
        )
        ActionIcon(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist", onAddToPlaylist, InkSoft)
        ActionIcon(Icons.Filled.Radio, "Start radio", onStartRadio, InkSoft)
        ActionIcon(
            when {
                isDownloaded -> Icons.Filled.DownloadDone
                isDownloading -> Icons.Filled.Downloading
                else -> Icons.Filled.Download
            },
            when {
                isDownloaded -> "Remove download"
                isDownloading -> "Downloading"
                else -> "Download"
            },
            { if (isDownloaded) onRemoveDownload() else if (!isDownloading) onDownload() },
            if (isDownloaded) accent else InkSoft,
        )
        ActionIcon(Icons.Filled.MoreHoriz, "More", onOpenMore, InkSoft)
    }
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit, tint: Color) {
    PlainIconButton(icon, description, onClick, tint, size = 46.dp, iconSize = 26.dp)
}

/** The time either side of a thin line with a dot on it. The line thickens under a finger. */
@Composable
private fun Scrubber(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    val seek by rememberUpdatedState(onSeek)
    val duration by rememberUpdatedState(durationMs)
    val accent = LocalExpressiveColors.current.accent
    // While dragging, the finger owns the position, not the updates still arriving from the player.
    var dragged by remember { mutableStateOf<Float?>(null) }
    val live = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shown = dragged ?: live
    val thickness by animateDpAsState(if (dragged != null) 5.dp else 2.dp, label = "scrubThickness")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(formatDuration((shown * durationMs).toLong()), style = TimeStyle, color = InkSoft)
        Spacer(Modifier.width(12.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .pointerInput(Unit) {
                    detectTapGestures { o ->
                        if (duration > 0) seek(((o.x / size.width).coerceIn(0f, 1f) * duration).toLong())
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> dragged = (o.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            dragged?.let { if (duration > 0) seek((it * duration).toLong()) }
                            dragged = null
                        },
                        onDragCancel = { dragged = null },
                    ) { change, _ ->
                        change.consume()
                        dragged = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
                .semantics {
                    contentDescription = "Position"
                    progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                    setProgress { f ->
                        if (duration > 0) seek((f.coerceIn(0f, 1f) * duration).toLong())
                        duration > 0
                    }
                },
        ) {
            val y = size.height / 2f
            val x = size.width * shown
            val w = thickness.toPx()
            drawLine(InkFaint, Offset(0f, y), Offset(size.width, y), w, StrokeCap.Round)
            drawLine(Ink, Offset(0f, y), Offset(x, y), w, StrokeCap.Round)
            drawCircle(accent, radius = 5.dp.toPx() + w / 2f, center = Offset(x, y))
        }
        Spacer(Modifier.width(12.dp))
        Text(formatDuration(durationMs), style = TimeStyle, color = InkSoft)
    }
}

/** Shuffle, previous, play, next and repeat, as bare glyphs. Shuffle and repeat take the accent when on. */
@Composable
private fun Transport(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val haptics = rememberHaptics()
    val accent = LocalExpressiveColors.current.accent
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIconButton(
            Icons.Filled.Shuffle,
            if (shuffleEnabled) "Shuffle, on" else "Shuffle, off",
            { haptics.tick(); onToggleShuffle() },
            if (shuffleEnabled) accent else InkSoft,
            size = 48.dp,
            iconSize = 28.dp,
        )
        PlainIconButton(Icons.Filled.SkipPrevious, "Previous track", onPrevious, Ink, size = 56.dp, iconSize = 40.dp)
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (fadeIn(tween(120)) + scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f)))
                    .togetherWith(fadeOut(tween(90)) + scaleOut(targetScale = 0.6f))
            },
            label = "playPause",
        ) { playing ->
            PlainIconButton(
                icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                onClick = { haptics.click(); onTogglePlay() },
                tint = Ink,
                size = 76.dp,
                iconSize = 60.dp,
            )
        }
        PlainIconButton(Icons.Filled.SkipNext, "Next track", onNext, Ink, size = 56.dp, iconSize = 40.dp)
        PlainIconButton(
            if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            when (repeatMode) {
                Player.REPEAT_MODE_ONE -> "Repeat one"
                Player.REPEAT_MODE_ALL -> "Repeat all"
                else -> "Repeat off"
            },
            { haptics.tick(); onCycleRepeat() },
            if (repeatMode != Player.REPEAT_MODE_OFF) accent else InkSoft,
            size = 48.dp,
            iconSize = 28.dp,
        )
    }
}

/** The queue as a numbered list of type: the playing song large and near the top, the rest smaller. */
@Composable
private fun QueuePane(
    queue: List<QueueItem>,
    currentIndex: Int,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    modifier: Modifier,
) {
    // Start already framed, so it doesn't jump on the first frame, and follow the song after that.
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0))
    LaunchedEffect(currentIndex) { list.animateScrollToItem((currentIndex - 1).coerceAtLeast(0)) }
    LazyColumn(
        state = list,
        modifier = modifier.fadeEdges(),
        contentPadding = PaddingValues(vertical = 10.dp),
    ) {
        itemsIndexed(queue, key = { i, it -> "q-$i-${it.mediaId}" }) { index, item ->
            val current = index == currentIndex
            val size by animateFloatAsState(if (current) 30f else 22f, tween(380), label = "queueSize")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayQueueItem(index) }
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    "%02d".format(index + 1),
                    style = TimeStyle.copy(fontSize = 12.sp),
                    color = InkSoft,
                    modifier = Modifier.width(26.dp).padding(top = if (current) 8.dp else 4.dp),
                )
                Text(
                    item.title,
                    style = TitleStyle.copy(
                        fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = size.sp,
                        lineHeight = (size * 1.14f).sp,
                    ),
                    color = if (current) Ink else InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                )
                if (!current) PlainIconButton(
                    Icons.Filled.Close,
                    "Remove ${item.title} from the queue",
                    { onRemoveQueueItem(index) },
                    InkFaint,
                    size = 34.dp,
                    iconSize = 16.dp,
                )
            }
        }
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
internal fun LightStatusIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        if (lightIconPages++ == 0) {
            savedLightStatus = controller.isAppearanceLightStatusBars
            savedLightNav = controller.isAppearanceLightNavigationBars
        }
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            // Counted, because two of these overlap: Now Playing is kept until Lyrics has slid in
            // over it, so it left last and put back the dark icons it had saved, over Lyrics.
            if (--lightIconPages == 0) {
                controller.isAppearanceLightStatusBars = savedLightStatus
                controller.isAppearanceLightNavigationBars = savedLightNav
            }
        }
    }
}

// How many pages on the cover are showing, and the icons to put back when the last one goes.
// Main thread only, like everything that composes.
private var lightIconPages = 0
private var savedLightStatus = false
private var savedLightNav = false

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
