package com.verza.ui.expressive

import androidx.compose.foundation.border
import com.verza.ui.theme.FontPosterHead
import com.verza.R
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.gestures.detectTapGestures
import android.content.Context
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.verza.audio.AudioOutputs
import com.verza.audio.OutputKind
import com.verza.audio.rememberAudioOutput
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.verza.ui.theme.isPoster
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
    albumArtMotion: Boolean,
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
    val queueWidth = animatedReadableWidth()

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
                albumArtMotion = albumArtMotion,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth()
                    .widthIn(max = queueWidth)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
            ) {
                Text("UP NEXT", style = MetaLabel, color = colors.onContainerMuted)
                Spacer(Modifier.height(4.dp))
                Text("Queue", style = HeroTitle, color = colors.onContainer)
            }
        }

        itemsIndexed(queue, key = { i, item -> "q-$i-${item.mediaId}" }) { index, item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth()
                    .widthIn(max = queueWidth)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 20.dp, vertical = 2.dp),
            ) {
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

        // The foot of the queue clears the navigation bar, so the last track is not sitting under the
        // buttons on a device that has them.
        item(key = "queue-tail") {
            Column {
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
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
    albumArtMotion: Boolean,
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

    // The flow, not its value. Collecting here would recompose everything below on every capture.
    val stillSignal = remember { MutableStateFlow(VisualizerSignal()) }
    val signalFlow = LocalAudioSignal.current ?: stillSignal

    // The artwork's bass pulse, smoothed on the frame clock. Held in state that is only read inside
    // the graphicsLayer block below, so a new value invalidates the layer rather than the tree.
    val artScale = remember { mutableFloatStateOf(1f) }
    LaunchedEffect(signalFlow, isPlaying, albumArtMotion) {
        if (!albumArtMotion) {
            artScale.floatValue = 1f
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { }
            val target = if (isPlaying) 1f + signalFlow.value.bass * 0.035f else 1f
            // Ease toward the target instead of snapping; the capture is coarser than the frame rate.
            artScale.floatValue += (target - artScale.floatValue) * 0.12f
            // Paused, there is nothing to follow. Settle and stop asking for frames — this loop used
            // to keep waking on every vsync for the whole time the app sat paused on screen.
            if (!isPlaying && kotlin.math.abs(artScale.floatValue - 1f) < 0.0005f) {
                artScale.floatValue = 1f
                break
            }
        }
    }
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    // Which way the new track should come in from. Derived from the queue index rather than a
    // timestamp, so going back slides the other way instead of always sliding forward.
    var lastIndex by remember { mutableIntStateOf(currentIndex) }
    val forward = currentIndex >= lastIndex
    if (currentIndex != lastIndex) lastIndex = currentIndex


    // Three pieces, so the same content can stack or sit side by side. On a phone this is the
    // column it has always been; on a wide window the artwork takes one half and everything else
    // the other, because a tall column of controls on a landscape tablet leaves the cover small
    // and most of the screen empty.
    val twoPane = useTwoPane()
    // The boost lets the cover crowd the title on a phone, which is the look. Beside the controls on
    // a landscape screen it only runs the art off the top and bottom edges, and on a portrait tablet
    // it pushes the cover wider than the controls and into the header buttons. So it is phone-only,
    // and it eases out as the layout changes instead of switching off with a jump. Read inside the
    // graphics layer, so the animation repaints without recomposing.
    val boostCover = !twoPane && deviceSize() == DeviceSize.PHONE
    val coverBoost by animateFloatAsState(
        targetValue = if (boostCover) COVER_BOOST else 1f,
        animationSpec = ExpressiveMotion.spatialSlow(),
        label = "coverBoost",
    )
    val controlsScroll = rememberScrollState()

    // Everything from here to the layout that is new (the controls hiding, the face per song, the
    // poster arrangement) belongs to the Poster design. In Material the controls never hide, so
    // the layout only ever takes its first arrangement, which is the player as it always was.
    val poster = isPoster()
    // Picked here rather than in the cover's graphics layer below: shapes follow the design
    // setting, and that has to be read in composition.
    val coverShape = ShapeExtraLarge

    // ── the controls get out of the way ──────────────────────────────────────────────────────
    // While a song plays, the controls fade after a few seconds without a touch and leave a poster:
    // the cover, as large as the screen allows, with the title set over its lower edge. Any touch
    // brings them back. Paused, they stay, because the one thing you are about to want is play.
    val context = LocalContext.current
    val a11y = LocalAccessibilityManager.current
    // Buttons that vanish are hostile to anyone who reads the screen by touch: they would be
    // hunting for controls that are not there. With touch exploration on they never hide, and
    // otherwise the wait honours the system's "time to take action" setting.
    val touchExploring = remember(context) {
        (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager)
            ?.isTouchExplorationEnabled == true
    }
    val hideAfterMs = remember(a11y) {
        a11y?.calculateRecommendedTimeoutMillis(
            CONTROLS_HIDE_MS, containsIcons = true, containsText = true, containsControls = true,
        ) ?: CONTROLS_HIDE_MS
    }
    var controlsShown by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }
    LaunchedEffect(poster, isPlaying, touches, controlsShown, touchExploring) {
        if (!poster || !isPlaying || touchExploring) {
            controlsShown = true
            return@LaunchedEffect
        }
        if (!controlsShown) return@LaunchedEffect
        delay(hideAfterMs)
        controlsShown = false
    }
    // The layout only ever sits in one of two arrangements, shown or hidden, and animateBoundsIn
    // springs every piece from one to the other, so the cover growing, the title sliding over it
    // and the controls leaving are the same motion the player already uses on rotation. This value
    // is the fade on top of that, and it is what decides when the controls are gone enough to take
    // out of the layout.
    val controlsAlpha = animateFloatAsState(
        targetValue = if (controlsShown) 1f else 0f,
        animationSpec = tween(if (controlsShown) 180 else 420),
        label = "controlsAlpha",
    )

    // ── the title's typeface ─────────────────────────────────────────────────────────────────
    // A different display face for every song (see TitleFonts). Keyed on the track, and told which
    // face the previous track had, so two songs in a row never share one; when the title arrives
    // late for the same track it keeps the face it already has.
    val faceMemory = remember { FaceMemory() }
    val face = remember(trackKey, title, poster) {
        if (!poster) return@remember null
        if (trackKey != faceMemory.key) {
            faceMemory.before = faceMemory.face
            faceMemory.key = trackKey
        }
        pickTitleFace(trackKey ?: title, TitleFonts.faces.size, faceMemory.before) { i ->
            TitleFonts.covers(context, TitleFonts.faces[i], title)
        }.also { faceMemory.face = it }
    }
    val titleFamily = remember(face) { face?.let { FontFamily(Font(TitleFonts.faces[it])) } }
    // A hard shadow in the canvas colour, straight down and unblurred. The title now sits on the
    // cover as well as beside it, and a cover can be any colour at all, including the title's; the
    // slab of canvas behind each letter is what keeps it legible there, and it is the flat, printed
    // kind of shadow rather than a glow.
    val titleShadow = with(LocalDensity.current) {
        Shadow(color = colors.container, offset = Offset(0f, 4.dp.toPx()), blurRadius = 0f)
    }

    val header: @Composable () -> Unit = {
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

    }

    val artworkPane: @Composable (Modifier) -> Unit = { paneModifier ->
            // ── artwork ──────────────────────────────────────────────────────────────
            // Keyed on the track so a change animates. Slide plus fade, springing in from the side the
            // queue moved; the mask morphs underneath at the same time.
            BoxWithConstraints(
                modifier = paneModifier,
                // To the bottom of its slot, so the square meets the title however tall the slot is.
                contentAlignment = if (poster) Alignment.BottomCenter else Alignment.Center,
            ) {
            // One measurement of the slot, turned into an explicit side length. Everything below is
            // sized from this rather than from a fill modifier, so nothing the image does can change it.
            val side = if (maxWidth < maxHeight) maxWidth else maxHeight
            // The boost only grows into spare height. On a short screen (a folded Fold, a phone with
            // button navigation) the cover already fills the slot's height, and the full boost ran it
            // over the back button and behind the title.
            val boostRoom = maxHeight / side
            // Keyed on the track alone. Keyed on the url as well, this ran twice per skip: once when
            // the track changed and the metadata thumbnail arrived, and again when the high-resolution
            // art resolved a moment later — so the cover swiped in, then swiped in again.
            AnimatedContent(
                targetState = trackKey,
                transitionSpec = {
                    val dir = if (forward) 1 else -1
                    // Snap the size rather than animating it: an animating container plus fill-based
                    // children is what made the cover shrink a little more with every track.
                    ((slideInHorizontally(ExpressiveMotion.spatialDefault()) { w -> dir * w / 3 } +
                        fadeIn(ExpressiveMotion.effectsDefault())) togetherWith
                        (slideOutHorizontally(ExpressiveMotion.spatialDefault()) { w -> -dir * w / 3 } +
                            fadeOut(ExpressiveMotion.effectsFast())))
                        .using(SizeTransform(clip = false) { _, _ -> snap() })
                },
                modifier = Modifier.size(side),
                label = "artSwap",
            ) { _ ->
                // A cover that vanishes mid-song is worse than a slightly stale one.
                //
                // The URL can change under us while a track plays, because the iTunes cover arrives
                // after the YouTube thumbnail has already drawn. If the newcomer fails to load there is
                // nothing behind it, and Coil has no reason to retry, so the artwork went blank for the
                // rest of the song. Remembering the last URL that actually drew means a failure falls
                // back instead of erasing. Reset per track, so nothing carries across a change.
                var lastGood by remember(trackKey) { mutableStateOf<String?>(null) }
                var failed by remember(trackKey) { mutableStateOf<String?>(null) }
                val model = if (artworkUrl != null && artworkUrl == failed) lastGood else artworkUrl

                Box(
                    modifier = Modifier
                        .size(side)
                        // One rounded square, always. The mask used to morph between scalloped
                        // silhouettes on every track, which drew attention to itself rather than to
                        // the artwork, and the artwork is the thing worth looking at.
                        .graphicsLayer {
                            // The boost is how the cover crowds the title with the controls up.
                            // Hidden, the cover is already the width of the screen, and boosting it
                            // would only crop the sides.
                            val boost = (1f + (coverBoost - 1f) * controlsAlpha.value).coerceAtMost(boostRoom)
                            scaleX = boost
                            scaleY = boost
                            shape = coverShape
                            clip = true
                        }
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
                        // Read live rather than from the animation's key, so the high-resolution
                        // upgrade lands in place instead of starting a second transition.
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onSuccess = { lastGood = model },
                        // Only blame a URL that is not already the fallback, or a cover with genuinely
                        // no art would flip between the two forever.
                        onError = { if (model != lastGood) failed = model },
                        // COVER_BOOST pushes the art past its slot. Scaling is a draw-time transform, so
                        // it overlaps its neighbours instead of displacing them, and the bass pulse costs
                        // no relayout.
                        modifier = Modifier.fillMaxSize().background(colors.surface)
                            .graphicsLayer {
                                // The bass pulse only. COVER_BOOST is applied once, on the mask above —
                                // applying it here as well drew the art at 1.49x inside a 1.22x clip.
                                scaleX = artScale.floatValue
                                scaleY = artScale.floatValue
                            },
                    )
                }
            }
            }
    }

    val titleBlock: @Composable () -> Unit = {
            // ── title ────────────────────────────────────────────────────────────────
            AnimatedContent(
                targetState = Triple(title, artist, titleFamily),
                transitionSpec = {
                    val dir = if (forward) 1 else -1
                    ((slideInHorizontally(ExpressiveMotion.spatialDefault()) { w -> dir * w / 4 } +
                        fadeIn(ExpressiveMotion.effectsDefault())) togetherWith
                        fadeOut(ExpressiveMotion.effectsFast()))
                        .using(SizeTransform(clip = false) { _, _ -> snap() })
                },
                label = "titleSwap",
            ) { (t, a, family) ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!poster) {
                        Text(
                            text = t,
                            style = HeroDisplay,
                            color = colors.accent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else FitTitle(
                        text = t,
                        // The face is drawn as it was cut: most of these have one weight and no
                        // italic, and asking for either only gets a synthesised imitation.
                        style = if (family != null) {
                            HeroDisplay.copy(
                                fontFamily = family,
                                fontStyle = FontStyle.Normal,
                                fontWeight = FontWeight.Normal,
                                shadow = titleShadow,
                            )
                        } else {
                            HeroDisplay.copy(shadow = titleShadow)
                        },
                        color = colors.accent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = a,
                        style = if (poster) BodyStrong.copy(shadow = titleShadow) else BodyStrong,
                        color = colors.onContainerMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
    }

    val controls: @Composable ColumnScope.() -> Unit = controls@{
            if (poster) {
                PosterControls(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    isLiked = isLiked,
                    isDownloaded = isDownloaded,
                    sleepTimerActive = sleepTimerActive,
                    queueCount = queueCount,
                    onSeek = onSeek,
                    onTogglePlay = onTogglePlay,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onToggleLike = onToggleLike,
                    onOpenLyrics = onOpenLyrics,
                    onStartRadio = onStartRadio,
                    onAddToPlaylist = onAddToPlaylist,
                    onDownload = onDownload,
                    onRemoveDownload = onRemoveDownload,
                    onOpenSleepTimer = onOpenSleepTimer,
                    onOpenMore = onOpenMore,
                    onShowQueue = onShowQueue,
                )
                return@controls
            }
            Spacer(Modifier.height(10.dp))

            VisualizerSeekBar(
                progress = progress,
                onSeek = { f -> onSeek((f * durationMs).toLong()) },
                accent = colors.accent,
                trackColor = colors.accentMuted,
                signalFlow = signalFlow,
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

                Spacer(Modifier.weight(1f))

                // Where the sound is going. This row had a third of its width empty, and a label is
                // worth more here than another icon would be — the useful thing is knowing you are
                // about to play out loud before you press play, not having somewhere to press after.
                OutputChip(colors = colors)
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

    // One adaptive layout, not a Row in one orientation and a Column in the other.
    //
    // Switching between two different parents threw the header, the cover and the controls away and
    // built them again on every rotation, so nothing could move from where it was to where it was
    // going: the artwork reloaded and the screen cut to a new arrangement. Here the three pieces never
    // change parent, only where this layout places them, and each springs from its old bounds to its
    // new ones. Rotating a tablet or opening a fold rearranges the player in front of you.
    LookaheadScope {
        val headerSlot: @Composable () -> Unit = {
            Box(Modifier.animateBoundsIn(this@LookaheadScope)) { header() }
        }
        val artworkSlot: @Composable () -> Unit = {
            Box(Modifier.animateBoundsIn(this@LookaheadScope)) { artworkPane(Modifier.fillMaxSize()) }
        }
        val titleSlot: @Composable () -> Unit = {
            Box(Modifier.animateBoundsIn(this@LookaheadScope)) { titleBlock() }
        }
        val controlsSlot: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .animateBoundsIn(this@LookaheadScope)
                    // Only ever scrolls when the controls genuinely do not fit, which is a phone on
                    // its side. Everywhere else they are shorter than their slot and this is inert.
                    .verticalScroll(controlsScroll),
            ) { controls() }
        }

        Layout(
            contents = listOf(headerSlot, artworkSlot, titleSlot, controlsSlot),
            modifier = modifier
                .fillMaxSize()
                // safeDrawing rather than systemBars, so a camera cutout in landscape does not sit
                // on top of the artwork.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                // Any touch on the player is a sign of life: it brings the controls back and restarts
                // the wait. Watched on the Initial pass and never consumed, so the touch still reaches
                // whatever it landed on, dragging the cover to skip included.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        controlsShown = true
                        touches++
                    }
                }
                .padding(horizontal = 20.dp),
        ) { (headerMeasurables, artworkMeasurables, titleMeasurables, controlsMeasurables), constraints ->
            val width = constraints.maxWidth
            val height = if (constraints.hasBoundedHeight) constraints.maxHeight else (width * 1.8f).toInt()
            // 1 shown, 0 hidden. Not animated: see controlsAlpha.
            val shown = if (controlsShown) 1f else 0f
            val hidden = 1f - shown
            // Gone enough to take out of the layout. Left in, controls parked below the player
            // would still catch touches meant for the queue underneath them.
            val placeControls = controlsShown || controlsAlpha.value > 0.01f

            if (twoPane) {
                val gap = 24.dp.roundToPx()
                val half = ((width - gap) / 2).coerceAtLeast(0)
                // The controls stop growing at a comfortable width and centre in their half, so the
                // transport buttons do not spread to the edges of a thirteen inch screen.
                val column = minOf(half, 560.dp.roundToPx())
                val header = headerMeasurables.first().measure(Constraints.fixedWidth(column))
                val title = titleMeasurables.first().measure(Constraints(minWidth = column, maxWidth = column))
                val controls = controlsMeasurables.first().measure(
                    Constraints(
                        minWidth = column,
                        maxWidth = column,
                        maxHeight = (height - header.height - title.height).coerceAtLeast(0),
                    ),
                )
                // Shown, the cover has its half. Hidden, it has the width of the window and centres.
                val artworkWidth = lerpPx(half, width, hidden)
                val artwork = artworkMeasurables.first().measure(Constraints.fixed(artworkWidth, height))
                val side = minOf(artworkWidth, height)
                val coverLeft = (artworkWidth - side) / 2
                // Header, title and controls sit as one group, centred against the cover beside them.
                val group = header.height + title.height + controls.height
                val top = ((height - group) / 2).coerceAtLeast(0)
                val left = half + gap + (half - column) / 2
                // Shown, the title heads the controls. Hidden, it moves onto the foot of the cover.
                val titleX = lerpPx(left, coverLeft, hidden)
                val titleY = lerpPx(top + header.height, height - title.height, hidden)
                layout(width, height) {
                    artwork.place(0, 0)
                    title.place(titleX, titleY) // after the cover, so it draws over it
                    if (placeControls) {
                        header.placeWithLayer(left, top - (header.height * hidden).toInt()) { alpha = controlsAlpha.value }
                        controls.placeWithLayer(left, lerpPx(top + header.height + title.height, height, hidden)) {
                            alpha = controlsAlpha.value
                        }
                    }
                }
            } else {
                val gap = 8.dp.roundToPx()
                val margin = 20.dp.roundToPx()
                // On a phone this is the full width, exactly as before. On a tablet held upright the
                // cover and controls stop at a size that still reads as a player rather than a
                // poster, and centre.
                val column = minOf(width, 620.dp.roundToPx())
                val left = (width - column) / 2
                val header = headerMeasurables.first().measure(Constraints.fixedWidth(width))
                val title = titleMeasurables.first().measure(Constraints(minWidth = column, maxWidth = column))
                val controls = controlsMeasurables.first().measure(
                    Constraints(
                        minWidth = column,
                        maxWidth = column,
                        maxHeight = (height - header.height - title.height).coerceAtLeast(0),
                    ),
                )
                val artworkX: Int
                val artworkY: Int
                val artwork: Placeable
                val titleY: Int
                if (controlsShown) {
                    // The player as it has always been: header, then the cover taking whatever
                    // height is left, then the title, then the controls.
                    titleY = height - controls.height - title.height
                    val top = header.height + gap
                    artwork = artworkMeasurables.first().measure(
                        Constraints.fixed(column, (titleY - top).coerceAtLeast(0)),
                    )
                    artworkX = left
                    artworkY = top
                } else {
                    // The poster. On a phone the cover runs to the edges of the screen: the margin
                    // was there to line it up with the controls, and they are gone. The title is
                    // set over its lower part rather than under it, and the two sit together in
                    // the middle of the screen. Pinned to the bottom instead, a square cover on a
                    // tall phone left the top half of the screen an empty field.
                    val bleed = if (column == width) margin else 0
                    val coverWidth = column + 2 * bleed
                    val overlap = (title.height * TITLE_OVER_COVER).toInt()
                    val side = minOf(coverWidth, (height - title.height + overlap).coerceAtLeast(0))
                    val top = ((height - (side + title.height - overlap)) / 2).coerceAtLeast(0)
                    artwork = artworkMeasurables.first().measure(Constraints.fixed(coverWidth, side))
                    artworkX = left - bleed
                    artworkY = top
                    titleY = top + side - overlap
                }
                layout(width, height) {
                    artwork.place(artworkX, artworkY)
                    title.place(left, titleY) // after the cover, so it draws over it
                    if (placeControls) {
                        header.placeWithLayer(0, -(header.height * hidden).toInt()) { alpha = controlsAlpha.value }
                        controls.placeWithLayer(left, lerpPx(height - controls.height, height, hidden)) {
                            alpha = controlsAlpha.value
                        }
                    }
                }
            }
        }
    }
}

/** Remembers which face each track got, so the next one can be told to pick another. */
private class FaceMemory {
    var key: String? = null
    var face: Int? = null
    var before: Int? = null
}

private fun lerpPx(from: Int, to: Int, t: Float): Int = (from + (to - from) * t).toInt()

/**
 * The title, as large as it can be set in its width without splitting a word.
 *
 * Twelve faces is twelve different widths: the same title that sits on one line in a condensed
 * face runs to four in an extended one. A fixed size either wastes the space or overflows it, so
 * the size is found per title and face, starting large and stepping down until the title fits in
 * three lines, no word has had to break across two, and the whole block is no taller than
 * [TITLE_MAX_SCREEN_SHARE] of the screen. The height cap is what lets the ceiling be high: a short
 * title in a condensed face grows to fill the width, as "As It Was" in Outward should, and a short
 * title in a wide face stops before it buries the cover. Measured, not guessed, and only when the
 * title, the face or the width changes.
 */
@Composable
private fun FitTitle(text: String, style: TextStyle, color: Color, maxLines: Int = 3) {
    val measurer = rememberTextMeasurer()
    val maxHeightPx = with(LocalDensity.current) {
        (LocalConfiguration.current.screenHeightDp * TITLE_MAX_SCREEN_SHARE).dp.roundToPx()
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth
        val size = remember(text, style, widthPx, maxHeightPx) {
            var sp = TITLE_MAX_SP
            while (sp > TITLE_MIN_SP) {
                val result = measurer.measure(
                    text = text,
                    style = style.copy(fontSize = sp.sp, lineHeight = (sp * 0.95f).sp),
                    constraints = Constraints(maxWidth = widthPx),
                )
                val wordSplit = (0 until result.lineCount - 1).any { line ->
                    val end = result.getLineEnd(line)
                    end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace()
                }
                val fits = result.lineCount <= maxLines && result.size.height <= maxHeightPx
                if (fits && !wordSplit && !result.didOverflowWidth) break
                sp -= 2f
            }
            sp
        }
        Text(
            text = text,
            style = style.copy(fontSize = size.sp, lineHeight = (size * 0.95f).sp),
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A pill naming the current output, which opens the system output switcher.
 *
 * The name is the interesting half. "Playing on Kitchen speaker" answers a question people
 * actually have, and answers it before they hit play in a quiet room.
 */
@Composable
private fun OutputChip(colors: ExpressiveColors) {
    val context = LocalContext.current
    val output by rememberAudioOutput()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "outputPress",
    )

    // Anything that is not the built-in speaker is the notable state, so it takes the accent.
    val external = output.kind != OutputKind.SPEAKER && output.kind != OutputKind.EARPIECE
    val container = if (external) colors.accent else colors.surface
    val content = if (external) colors.onAccent else colors.onSurface

    Row(
        modifier = Modifier
            .scale(scale)
            .heightIn(min = 50.dp)
            // Room for "Phone speaker" and most headphone names; 168 cut even the built-in speaker off.
            .widthIn(max = 240.dp)
            .clip(PillShape)
            .background(container)
            .clickable(interactionSource = interaction, indication = null) {
                AudioOutputs.openSwitcher(context)
            }
            .padding(horizontal = 16.dp)
            .semantics { contentDescription = "Playing on ${output.name}. Change output." },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = when (output.kind) {
                OutputKind.BLUETOOTH -> Icons.Filled.Bluetooth
                OutputKind.WIRED -> Icons.Filled.Headphones
                OutputKind.USB -> Icons.Filled.Usb
                OutputKind.HEARING_AID -> Icons.Filled.Hearing
                OutputKind.REMOTE -> Icons.Filled.Cast
                else -> Icons.Filled.Speaker
            },
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = output.name,
            style = BodyStrong,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

/** How long the controls wait, untouched and playing, before they get out of the way. */
private const val CONTROLS_HIDE_MS = 4000L

/** How much of the title's height is set over the cover once the controls have gone. */
private const val TITLE_OVER_COVER = 0.6f

/** The title's size range. It starts at the top and steps down until it fits. */
private const val TITLE_MAX_SP = 120f

/**
 * The most of the screen's height the title may take, however short it is. 30% made a better
 * poster and a worse player: with the controls up it squeezed the cover to a thumbnail.
 */
private const val TITLE_MAX_SCREEN_SHARE = 0.22f
private const val TITLE_MIN_SP = 30f

// ── Poster controls ──────────────────────────────────────────────────────────────────────────────

/**
 * The Now Playing controls in the Poster design: flat slabs of the cover's colours, packed together
 * with no gaps, labelled in words in a display face rather than with icons on round buttons.
 *
 * The same controls as Material, in the same order, so nothing moves for anyone who switches:
 * the seek bar, then the transport, then the toggles, then the tools. A slab that is "on" fills
 * with the accent; one that is off sits in the canvas or a surface colour. Neighbouring slabs
 * alternate colour so the rows read as a printed grid, not as a toolbar.
 */
@Composable
private fun ColumnScope.PosterControls(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isLiked: Boolean,
    isDownloaded: Boolean,
    sleepTimerActive: Boolean,
    queueCount: Int,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenLyrics: () -> Unit,
    onStartRadio: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenMore: () -> Unit,
    onShowQueue: () -> Unit,
) {
    val colors = LocalExpressiveColors.current
    val numerals = TextStyle(fontFamily = FontPosterHead, fontSize = 30.sp, lineHeight = 30.sp)

    Spacer(Modifier.height(10.dp))
    PosterSeekBar(positionMs, durationMs, onSeek)
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(formatDuration(positionMs), style = numerals, color = colors.onContainer)
        Spacer(Modifier.weight(1f))
        Text(formatDuration(durationMs), style = numerals, color = colors.onContainerMuted)
    }
    Spacer(Modifier.height(10.dp))

    // ── transport: one strip, play twice the width of its neighbours ──
    Row(Modifier.fillMaxWidth().height(76.dp)) {
        Slab(
            onClick = onPrevious,
            background = colors.surfaceHighest,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            description = "Previous track",
        ) { Icon(Icons.Filled.SkipPrevious, null, tint = colors.onSurface, modifier = Modifier.size(36.dp)) }
        Slab(
            onClick = onTogglePlay,
            background = colors.accent,
            modifier = Modifier.weight(2f).fillMaxHeight(),
            description = if (isPlaying) "Pause" else "Play",
        ) {
            Text(
                if (isPlaying) "PAUSE" else "PLAY",
                style = TextStyle(fontFamily = TitleFonts.family(R.font.display_anton), fontSize = 40.sp, lineHeight = 40.sp),
                color = colors.onAccent,
            )
        }
        // Previous and next share a colour so play is the only filled block in the strip. Next
        // used to take the tertiary, which on some covers is as pale as the accent, and play and
        // next merged into one white bar.
        Slab(
            onClick = onNext,
            background = colors.surfaceHighest,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            description = "Next track",
        ) { Icon(Icons.Filled.SkipNext, null, tint = colors.onSurface, modifier = Modifier.size(36.dp)) }
    }

    // ── toggles ──
    Row(Modifier.fillMaxWidth().height(52.dp)) {
        WordSlab("SHUFFLE", shuffleEnabled, onToggleShuffle, colors.container, Modifier.weight(1f))
        WordSlab(
            when (repeatMode) { 1 -> "REPEAT 1"; 2 -> "REPEAT ALL"; else -> "REPEAT" },
            repeatMode != 0,
            onCycleRepeat,
            colors.surface,
            Modifier.weight(1.2f),
        )
        WordSlab(if (isLiked) "LIKED" else "LIKE", isLiked, onToggleLike, colors.container, Modifier.weight(1f))
    }

    // ── tools, two rows of three ──
    Row(Modifier.fillMaxWidth().height(52.dp)) {
        WordSlab("LYRICS", false, onOpenLyrics, colors.surface, Modifier.weight(1f))
        WordSlab("RADIO", false, onStartRadio, colors.container, Modifier.weight(1f))
        WordSlab("+ PLAYLIST", false, onAddToPlaylist, colors.surface, Modifier.weight(1.3f), description = "Add to playlist")
    }
    Row(Modifier.fillMaxWidth().height(52.dp)) {
        WordSlab(
            if (isDownloaded) "SAVED" else "DOWNLOAD",
            isDownloaded,
            if (isDownloaded) onRemoveDownload else onDownload,
            colors.container,
            Modifier.weight(1.3f),
            description = if (isDownloaded) "Remove download" else "Download",
        )
        WordSlab("SLEEP", sleepTimerActive, onOpenSleepTimer, colors.surface, Modifier.weight(1f), description = "Sleep timer")
        WordSlab("MORE", false, onOpenMore, colors.container, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    OutputChip(colors = colors)
    Spacer(Modifier.height(4.dp))
    QueueHint(count = queueCount, onClick = onShowQueue)
}

/**
 * A flat, clickable block. The press darkens it; there is no ripple to round its corners.
 *
 * Each carries a fine rule in the ink colour. A cover's palette can put two neighbouring surfaces
 * within a shade of each other (a teal canvas next to a teal surface), and without the rule the
 * grid melts into one slab; with it the controls read as a printed grid whatever the cover.
 */
@Composable
private fun Slab(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier,
    description: String,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .background(background)
            .border(1.dp, LocalExpressiveColors.current.onContainer.copy(alpha = 0.28f))
            .drawWithContent {
                drawContent()
                if (pressed) drawRect(Color.Black.copy(alpha = 0.18f))
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A block with a word on it: filled with the accent when it is on, [off] when it is not. */
@Composable
private fun WordSlab(
    word: String,
    on: Boolean,
    onClick: () -> Unit,
    off: Color,
    modifier: Modifier,
    description: String = word.lowercase().replaceFirstChar { it.uppercase() },
) {
    val colors = LocalExpressiveColors.current
    // The state is in the description as well as the colour, so TalkBack says it.
    val said = if (on) "$description, on" else description
    Slab(onClick, if (on) colors.accent else off, modifier.fillMaxHeight(), said) {
        Text(
            word,
            style = TextStyle(fontFamily = FontPosterHead, fontSize = 20.sp, lineHeight = 20.sp),
            color = if (on) colors.onAccent else colors.onContainer,
            maxLines = 1,
        )
    }
}

/**
 * The seek bar as a thick flat bar: played in the accent, the rest a surface, no thumb. Tap or drag
 * anywhere along it. While dragging it shows where the finger is and seeks on release, so a drag is
 * one seek rather than dozens.
 */
@Composable
private fun PosterSeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    val colors = LocalExpressiveColors.current
    var drag by remember { mutableStateOf<Float?>(null) }
    val played = drag ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(colors.surface)
            .pointerInput(durationMs) {
                detectTapGestures { o -> onSeek(((o.x / size.width).coerceIn(0f, 1f) * durationMs).toLong()) }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> drag = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { drag?.let { onSeek((it * durationMs).toLong()) }; drag = null },
                    onDragCancel = { drag = null },
                ) { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) }
            }
            .semantics { contentDescription = "Seek, ${formatDuration(positionMs)} of ${formatDuration(durationMs)}" },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(played).background(colors.accent))
    }
}
