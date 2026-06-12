package com.verza.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.verza.player.QueueItem
import com.verza.ui.components.rememberSongArtwork
import com.verza.ui.share.NowPlayingShareOverlay
import com.verza.ui.theme.LocalAudioSignal
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaShape

@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    videoId: String?,
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    queue: List<QueueItem>,
    currentIndex: Int,
    onTogglePlay: () -> Unit,
    onToggleLike: () -> Unit,
    onStartRadio: () -> Unit,
    onOpenLyrics: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onAddToPlaylist: () -> Unit,
    onEnterAmbient: () -> Unit,
    sleepTimerEndAt: Long?,
    onSetSleepTimer: (Long?) -> Unit,
    onWindDown: (Long) -> Unit,
    onSleepTimerEndOfTrack: () -> Unit,
    focusActive: Boolean,
    focusEndAt: Long?,
    onStartFocus: (Long?) -> Unit,
    onEndFocus: () -> Unit,
    focusCompleteMinutes: Int?,
    onConsumeFocusComplete: () -> Unit,
    onBuildSessionLink: () -> String?,
    albumArtMotion: Boolean = true,
    sleeveMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Opens the "share this track as a poster" card; used from both Sleeve and standard layouts.
    var showShareCard by remember { mutableStateOf(false) }
    // Opens the editorial liner-notes sheet for the current track.
    var showLinerNotes by remember { mutableStateOf(false) }
    // Opens the Focus/Flow session sheet (duration picker / active-session controls).
    var showFocusSheet by remember { mutableStateOf(false) }
    val focusRemaining = rememberSleepCountdown(focusEndAt)
    // Sleep-timer sheet + live countdown — hoisted so both Sleeve and standard layouts can open it.
    var showSleepSheet by remember { mutableStateOf(false) }
    val sleepRemaining = rememberSleepCountdown(sleepTimerEndAt)

    // Build + share the current queue as a verza:// "listen along" link (used from both layouts).
    val shareCtx = LocalContext.current
    val shareSession: () -> Unit = {
        val link = onBuildSessionLink()
        if (link != null) shareSessionLink(shareCtx, link)
        else android.widget.Toast.makeText(shareCtx, "Nothing to share yet", android.widget.Toast.LENGTH_SHORT).show()
    }

    // Editorial "Sleeve" poster surface fully replaces the standard layout when enabled.
    if (sleeveMode) {
        Box(modifier = modifier.fillMaxSize()) {
            com.verza.ui.sleeve.SleevePlayer(
                onBack = onBack,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                isPlaying = isPlaying,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                positionMs = positionMs,
                durationMs = durationMs,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                queue = queue,
                currentIndex = currentIndex,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onPlayQueueItem = onPlayQueueItem,
                onOpenLyrics = onOpenLyrics,
                onToggleLike = onToggleLike,
                onStartRadio = onStartRadio,
                onDownload = onDownload,
                onRemoveDownload = onRemoveDownload,
                onAddToPlaylist = onAddToPlaylist,
                onShare = { showShareCard = true },
                onAmbient = onEnterAmbient,
                onLinerNotes = { showLinerNotes = true },
                onFocus = { showFocusSheet = true },
                onShareSession = shareSession,
                onSleepTimer = { showSleepSheet = true },
                sleepRemaining = sleepRemaining,
                focusActive = focusActive,
                modifier = Modifier.fillMaxSize(),
            )
            if (showShareCard) {
                NowPlayingShareOverlay(
                    title = title,
                    artist = artist,
                    artworkUrl = artworkUrl,
                    onDismiss = { showShareCard = false },
                )
            }
            if (showLinerNotes) {
                LinerNotesSheet(
                    title = title,
                    artist = artist,
                    artworkUrl = artworkUrl,
                    onDismiss = { showLinerNotes = false },
                )
            }
            if (showFocusSheet) {
                FocusSheet(
                    active = focusActive,
                    remaining = focusRemaining,
                    onStart = { onStartFocus(it); showFocusSheet = false },
                    onEnd = { onEndFocus(); showFocusSheet = false },
                    onDismiss = { showFocusSheet = false },
                )
            }
            if (showSleepSheet) {
                SleepTimerSheet(
                    active = sleepTimerEndAt != null,
                    remaining = sleepRemaining,
                    onPick = { minutes -> onSetSleepTimer(minutes * 60_000L); showSleepSheet = false },
                    onWindDown = { minutes -> onWindDown(minutes * 60_000L); showSleepSheet = false },
                    onEndOfTrack = { onSleepTimerEndOfTrack(); showSleepSheet = false },
                    onCancel = { onSetSleepTimer(null); showSleepSheet = false },
                    onDismiss = { showSleepSheet = false },
                )
            }
            FocusCompleteBanner(
                minutes = focusCompleteMinutes,
                onConsume = onConsumeFocusComplete,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        return
    }
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val context = LocalContext.current
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    var showQueue by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val songUrl = videoId?.let { "https://music.youtube.com/watch?v=$it" }

    // Transparent root so the app-wide flowing GlowBackground (rendered behind the NavHost in
    // MainActivity) shows through on this screen too. The previous opaque background + local
    // radial wash hid it entirely.
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Close", tint = colors.onBackground)
                }
                // When a sleep timer is armed, the centre shows a live countdown chip; otherwise
                // the decorative drag handle.
                if (sleepRemaining != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.primary.copy(alpha = 0.14f))
                            .clickable { showSleepSheet = true }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        Icon(
                            Icons.Filled.Bedtime,
                            contentDescription = "Sleep timer",
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(sleepRemaining, style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ext.muted.copy(alpha = 0.3f)),
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = colors.onBackground)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            enabled = songUrl != null,
                            onClick = {
                                menuOpen = false
                                if (songUrl != null) shareSong(context, title, artist, songUrl)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy link") },
                            enabled = songUrl != null,
                            onClick = {
                                menuOpen = false
                                if (songUrl != null) copyToClipboard(context, songUrl)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share as image") },
                            onClick = { menuOpen = false; showShareCard = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Ambient display") },
                            onClick = { menuOpen = false; onEnterAmbient() },
                        )
                        DropdownMenuItem(
                            text = { Text("Liner notes") },
                            onClick = { menuOpen = false; showLinerNotes = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Lyrics") },
                            onClick = { menuOpen = false; onOpenLyrics() },
                        )
                        DropdownMenuItem(
                            text = { Text("Add to playlist…") },
                            onClick = { menuOpen = false; onAddToPlaylist() },
                        )
                        DropdownMenuItem(
                            text = { Text("Share listening session") },
                            onClick = { menuOpen = false; shareSession() },
                        )
                        DropdownMenuItem(
                            text = { Text("Start radio") },
                            onClick = { menuOpen = false; onStartRadio() },
                        )
                        DropdownMenuItem(
                            text = { Text(if (sleepRemaining != null) "Sleep timer · $sleepRemaining" else "Sleep timer") },
                            onClick = { menuOpen = false; showSleepSheet = true },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when {
                                        focusActive && focusRemaining != null -> "Focus · $focusRemaining"
                                        focusActive -> "Focus session · on"
                                        else -> "Focus session"
                                    }
                                )
                            },
                            onClick = { menuOpen = false; showFocusSheet = true },
                        )
                        HorizontalDivider()
                        if (isDownloaded) {
                            DropdownMenuItem(
                                text = { Text("Remove download") },
                                onClick = { menuOpen = false; onRemoveDownload() },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(if (isDownloading) "Downloading…" else "Download") },
                                enabled = !isDownloading,
                                onClick = { menuOpen = false; onDownload() },
                            )
                        }
                    }
                }
            }

            // ── Artwork ────────────────────────────────────────────────────
            // "Breathing" scale: 1.0 ↔ 1.012 on a 3-second loop while playback is active.
            // Drives the entire artwork box (shadow + clip + image) via a single graphicsLayer
            // so the shadow scales with the art instead of staying static under it. When paused
            // the infinite transition pauses too — Compose stops emitting values, the scale
            // freezes at whatever frame it was on.
            val breathing = rememberInfiniteTransition(label = "artBreath")
            val artScale by breathing.animateFloat(
                initialValue = 1f,
                targetValue = if (isPlaying && albumArtMotion) 1.012f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "artScale",
            )
            // Swipe the cover sideways to change tracks: it follows the finger with a damped
            // drag and a hint of tilt, springs back on release, and fires next/previous once
            // the pull crosses the threshold — the artwork itself becomes the skip control.
            val scope = rememberCoroutineScope()
            val artDrag = remember { Animatable(0f) }
            val skipThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
            // The cover also *grooves*: when reactivity is running, the live bass band gives the
            // art a gentle kick on every beat, layered on top of the slow breathing scale.
            val audioSignal = LocalAudioSignal.current
            val liveBass = audioSignal?.collectAsState()?.value?.bass ?: 0f
            val bassKick by animateFloatAsState(
                targetValue = if (isPlaying) 1f + 0.022f * liveBass else 1f,
                animationSpec = tween(durationMillis = 110, easing = LinearEasing),
                label = "artBassKick",
            )
            Box(
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 20.dp)
                    .size(280.dp)
                    .align(Alignment.CenterHorizontally)
                    .graphicsLayer {
                        scaleX = artScale * bassKick
                        scaleY = artScale * bassKick
                        translationX = artDrag.value * 0.55f
                        rotationZ = artDrag.value * 0.004f
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val pull = artDrag.value
                                scope.launch {
                                    if (pull <= -skipThresholdPx) onNext()
                                    else if (pull >= skipThresholdPx) onPrevious()
                                    artDrag.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    )
                                }
                            },
                            onDragCancel = { scope.launch { artDrag.animateTo(0f, spring()) } },
                        ) { change, delta ->
                            change.consume()
                            scope.launch { artDrag.snapTo(artDrag.value + delta) }
                        }
                    }
                    .shadow(elevation = 24.dp, shape = VerzaShape, clip = false)
                    .clip(VerzaShape)
                    .background(colors.surfaceVariant),
            ) {
                if (artworkUrl != null) {
                    AsyncImage(
                        // Crossfade so the cover dissolves between the YT thumbnail and the higher-res
                        // iTunes art (and between tracks) instead of flashing to black on the swap.
                        model = ImageRequest.Builder(context)
                            .data(artworkUrl)
                            .crossfade(320)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // Themed gradient placeholder so the canvas is never blank.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(colors.primary, colors.tertiary))
                            )
                    )
                }
            }

            // ── Track info ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // ── Progress ───────────────────────────────────────────────────
            // Real scrubbing: tap to jump, or press and drag anywhere on the bar — the fill,
            // thumb and elapsed label follow the finger live, and the seek fires on release.
            // The scrub fraction is local UI state so dragging never fights the position ticks.
            var scrubFrac by remember { mutableStateOf<Float?>(null) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Smooth progress interpolation: the underlying `progress` value updates in
                // discrete ~500 ms steps from the playback service. Animating the visible fill
                // with a linear 500 ms tween makes it glide continuously between updates.
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 500, easing = LinearEasing),
                    label = "seekBarFill",
                )
                val shownFrac = (scrubFrac ?: animatedProgress).coerceIn(0f, 1f)
                val thumbRadius by animateDpAsState(
                    targetValue = if (scrubFrac != null) 7.dp else 4.5.dp,
                    label = "seekThumb",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Generous touch target; the visible track stays slim.
                        .height(26.dp)
                        .pointerInput(durationMs) {
                            detectTapGestures { offset ->
                                if (durationMs > 0) {
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    onSeek((fraction * durationMs).toLong())
                                }
                            }
                        }
                        .pointerInput(durationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    scrubFrac = (offset.x / size.width).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    scrubFrac?.let { if (durationMs > 0) onSeek((it * durationMs).toLong()) }
                                    scrubFrac = null
                                },
                                onDragCancel = { scrubFrac = null },
                            ) { change, _ ->
                                change.consume()
                                scrubFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.outlineVariant),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(shownFrac)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.primary),
                    )
                    // Thumb — rides the fill's edge and swells under the finger while scrubbing.
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(
                            color = colors.primary,
                            radius = thumbRadius.toPx(),
                            center = Offset(size.width * shownFrac, size.height / 2f),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // While scrubbing, the elapsed label previews the target position in accent.
                    val previewMs = scrubFrac?.let { (it * durationMs).toLong() } ?: positionMs
                    Text(
                        formatTime(previewMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (scrubFrac != null) colors.primary else ext.muted,
                    )
                    Text(formatTime(durationMs), style = MaterialTheme.typography.labelSmall, color = ext.muted)
                }
            }

            // ── Controls ───────────────────────────────────────────────────
            // SpaceEvenly distributes the five controls (shuffle · prev · PLAY · next · repeat)
            // with equal gaps including edges — adaptive to width, always centered.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) colors.primary else ext.muted,
                        modifier = Modifier.size(22.dp),
                    )
                }
                // Skip buttons punch in their direction and spring back — kinetic confirmation
                // that matches the artwork sliding the same way.
                val prevNudge = remember { Animatable(0f) }
                IconButton(onClick = {
                    scope.launch {
                        prevNudge.snapTo(-9f)
                        prevNudge.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    onPrevious()
                }) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = colors.onBackground,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer { translationX = prevNudge.value.dp.toPx() },
                    )
                }
                // The play button is the page's heartbeat: it pops with a spring on every toggle,
                // swells with the live bass while music plays, and the play/pause glyph morphs
                // through a scale+fade instead of cutting.
                val playPop = remember { Animatable(1f) }
                LaunchedEffect(isPlaying) {
                    playPop.snapTo(0.84f)
                    playPop.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    )
                }
                val playBass by animateFloatAsState(
                    targetValue = if (isPlaying) 1f + 0.055f * liveBass else 1f,
                    animationSpec = tween(durationMillis = 110, easing = LinearEasing),
                    label = "playBass",
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            val s = playPop.value * playBass
                            scaleX = s
                            scaleY = s
                        }
                        .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.55f, animationSpec = tween(180)) + fadeIn(tween(120))) togetherWith
                                (scaleOut(targetScale = 0.55f, animationSpec = tween(140)) + fadeOut(tween(100)))
                        },
                        label = "playIconMorph",
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                val nextNudge = remember { Animatable(0f) }
                IconButton(onClick = {
                    scope.launch {
                        nextNudge.snapTo(9f)
                        nextNudge.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    onNext()
                }) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = colors.onBackground,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer { translationX = nextNudge.value.dp.toPx() },
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                            Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) colors.primary else ext.muted,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // ── Action row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Liking a song fires a small particle burst from behind the heart.
                Box(contentAlignment = Alignment.Center) {
                    LikeBurst(active = isLiked)
                    ActionButton(
                        icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        label = "Like",
                        tinted = isLiked,
                        onClick = onToggleLike,
                    )
                }
                ActionButton(icon = Icons.Filled.Radio, label = "Radio", onClick = onStartRadio)
                ActionButton(icon = Icons.Outlined.Lyrics, label = "Lyrics", onClick = onOpenLyrics)
                ActionButton(
                    icon = Icons.Filled.QueueMusic,
                    label = if (showQueue) "Hide" else "Queue",
                    tinted = showQueue,
                    onClick = { showQueue = !showQueue },
                )
            }

            // ── Up next (queue) ───────────────────────────────────────────
            if (showQueue && queue.size > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 20.dp),
                ) {
                    Text(
                        text = "Up next",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                    queue.forEachIndexed { index, item ->
                        QueueRow(
                            item = item,
                            isCurrent = index == currentIndex,
                            onClick = { onPlayQueueItem(index) },
                            onRemove = { onRemoveQueueItem(index) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        if (showShareCard) {
            NowPlayingShareOverlay(
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                onDismiss = { showShareCard = false },
            )
        }
        FocusCompleteBanner(
            minutes = focusCompleteMinutes,
            onConsume = onConsumeFocusComplete,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (showLinerNotes) {
        LinerNotesSheet(
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            onDismiss = { showLinerNotes = false },
        )
    }

    if (showSleepSheet) {
        SleepTimerSheet(
            active = sleepTimerEndAt != null,
            remaining = sleepRemaining,
            onPick = { minutes ->
                onSetSleepTimer(minutes * 60_000L)
                showSleepSheet = false
            },
            onWindDown = { minutes ->
                onWindDown(minutes * 60_000L)
                showSleepSheet = false
            },
            onEndOfTrack = {
                onSleepTimerEndOfTrack()
                showSleepSheet = false
            },
            onCancel = {
                onSetSleepTimer(null)
                showSleepSheet = false
            },
            onDismiss = { showSleepSheet = false },
        )
    }

    if (showFocusSheet) {
        FocusSheet(
            active = focusActive,
            remaining = focusRemaining,
            onStart = { onStartFocus(it); showFocusSheet = false },
            onEnd = { onEndFocus(); showFocusSheet = false },
            onDismiss = { showFocusSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    active: Boolean,
    remaining: String?,
    onPick: (Int) -> Unit,
    onWindDown: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Sleep timer", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
            Text(
                text = if (active && remaining != null) "Pausing in $remaining" else "Fade out and pause after…",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted,
            )
            Spacer(Modifier.height(12.dp))
            listOf(15, 30, 45, 60).forEach { minutes ->
                SleepOption(label = "$minutes minutes", onClick = { onPick(minutes) })
            }
            SleepOption(label = "End of track", onClick = onEndOfTrack)

            Spacer(Modifier.height(16.dp))
            Text("Wind down", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text(
                text = "A long, gradual fade across the final minutes — drift off without a hard cut.",
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
            )
            Spacer(Modifier.height(8.dp))
            listOf(30, 45, 60).forEach { minutes ->
                SleepOption(label = "Wind down over $minutes minutes", onClick = { onWindDown(minutes) })
            }

            if (active) {
                Spacer(Modifier.height(4.dp))
                SleepOption(label = "Turn off timer", tint = colors.primary, onClick = onCancel)
            }
        }
    }
}

@Composable
private fun SleepOption(label: String, tint: Color? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = tint ?: colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
    )
}

/**
 * Focus / Flow session sheet. Starts a "deep work" block where the queue is kept topped up so
 * music never breaks the flow, and a timed block fades out gently when it's up. When a session is
 * already running this shows the live status and an "End session" action instead of the picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusSheet(
    active: Boolean,
    remaining: String?,
    onStart: (Long?) -> Unit,
    onEnd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Focus session", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
            Text(
                text = "Uninterrupted flow for deep work — the music keeps going on its own, so silence never breaks your concentration.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted,
            )
            Spacer(Modifier.height(12.dp))
            if (active) {
                Text(
                    text = if (remaining != null) "In focus · $remaining left" else "In focus · open-ended",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                )
                Spacer(Modifier.height(8.dp))
                SleepOption(label = "End session", tint = colors.primary, onClick = onEnd)
            } else {
                listOf(25, 50, 90).forEach { minutes ->
                    SleepOption(label = "$minutes minutes", onClick = { onStart(minutes * 60_000L) })
                }
                SleepOption(label = "Open-ended", onClick = { onStart(null) })
            }
        }
    }
}

/**
 * A brief, self-dismissing banner shown when a Focus session finishes, e.g. "Focused for 50 min".
 * Renders nothing when [minutes] is null. Calls [onConsume] after a few seconds to clear the event.
 */
@Composable
private fun FocusCompleteBanner(
    minutes: Int?,
    onConsume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    // Latch the last non-null value so the label survives the slide-out after the event clears.
    var shown by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(minutes) {
        if (minutes != null) {
            shown = minutes
            kotlinx.coroutines.delay(4_000)
            onConsume()
        }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = minutes != null,
        enter = androidx.compose.animation.fadeIn() +
            androidx.compose.animation.slideInVertically { -it },
        exit = androidx.compose.animation.fadeOut() +
            androidx.compose.animation.slideOutVertically { -it },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.primaryContainer)
                .clickable(onClick = onConsume)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = shown?.let { "Focused for $it min" } ?: "Focus complete",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimaryContainer,
            )
        }
    }
}

/**
 * Ticks once a second while a sleep timer is armed, returning the remaining time formatted as
 * "m:ss" (or "h:mm:ss" past an hour). Returns null when no timer is set.
 */
@Composable
private fun rememberSleepCountdown(endAt: Long?): String? {
    if (endAt == null) return null
    var now by remember(endAt) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endAt) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
    }
    val remainingMs = (endAt - now).coerceAtLeast(0L)
    val totalSec = remainingMs / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * A one-shot particle burst fired when [active] flips to true (i.e. the song gets liked):
 * twelve dots in the theme's accent pair radiate out from behind the heart, shrinking and
 * fading as they fly. Doesn't fire for the initial state, only for a fresh like.
 */
@Composable
private fun LikeBurst(active: Boolean) {
    val colors = MaterialTheme.colorScheme
    val anim = remember { Animatable(1f) }
    var seen by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (active && !seen) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
        }
        seen = active
    }
    val t = anim.value
    if (t < 1f) {
        Canvas(Modifier.size(68.dp)) {
            val count = 12
            val maxReach = size.minDimension / 2f
            for (i in 0 until count) {
                val angle = i / count.toFloat() * 2f * Math.PI.toFloat() + 0.26f
                val reach = maxReach * (0.35f + 0.65f * t)
                drawCircle(
                    color = if (i % 2 == 0) colors.primary else colors.tertiary,
                    radius = (1f - t) * 2.6.dp.toPx() + 0.6.dp.toPx(),
                    center = center + Offset(kotlin.math.cos(angle) * reach, kotlin.math.sin(angle) * reach),
                    alpha = (1f - t).coerceIn(0f, 1f),
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tinted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val tint = if (tinted) colors.primary else colors.onBackground

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (tinted) colors.primary else ext.muted)
    }
}

@Composable
private fun QueueRow(
    item: QueueItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val bg = if (isCurrent) colors.surfaceVariant else Color.Transparent
    val art = rememberSongArtwork(item.title, item.artist, item.artworkUrl)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(VerzaShape)
                .background(colors.surface),
        ) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) colors.primary else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artist,
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!isCurrent) {
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove from queue",
                    tint = ext.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Pops the system share sheet with a YT Music URL prefilled. */
private fun shareSong(context: Context, title: String, artist: String, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title — $artist\n$url")
    }
    context.startActivity(Intent.createChooser(intent, "Share song"))
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Song link", text))
}

/** Shares a verza:// "listen along" session link via the system chooser. */
private fun shareSessionLink(context: Context, link: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Listen along on Verza")
        putExtra(
            Intent.EXTRA_TEXT,
            "Pick up where I'm at — open this in Verza to play the same set:\n$link",
        )
    }
    context.startActivity(Intent.createChooser(intent, "Share listening session"))
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
