package com.verza.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.verza.player.QueueItem
import com.verza.ui.components.rememberSongArtwork
import com.verza.ui.share.NowPlayingShareOverlay
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
    sleepTimerEndAt: Long?,
    onSetSleepTimer: (Long?) -> Unit,
    onSleepTimerEndOfTrack: () -> Unit,
    albumArtMotion: Boolean = true,
    sleeveMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Opens the "share this track as a poster" card; used from both Sleeve and standard layouts.
    var showShareCard by remember { mutableStateOf(false) }

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
        }
        return
    }
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val context = LocalContext.current
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    var showQueue by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    val sleepRemaining = rememberSleepCountdown(sleepTimerEndAt)

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
                            text = { Text("Lyrics") },
                            onClick = { menuOpen = false; onOpenLyrics() },
                        )
                        DropdownMenuItem(
                            text = { Text("Add to playlist…") },
                            onClick = { menuOpen = false; onAddToPlaylist() },
                        )
                        DropdownMenuItem(
                            text = { Text("Start radio") },
                            onClick = { menuOpen = false; onStartRadio() },
                        )
                        DropdownMenuItem(
                            text = { Text(if (sleepRemaining != null) "Sleep timer · $sleepRemaining" else "Sleep timer") },
                            onClick = { menuOpen = false; showSleepSheet = true },
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
            Box(
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 20.dp)
                    .size(280.dp)
                    .align(Alignment.CenterHorizontally)
                    .graphicsLayer { scaleX = artScale; scaleY = artScale }
                    .shadow(elevation = 24.dp, shape = VerzaShape, clip = false)
                    .clip(VerzaShape)
                    .background(colors.surfaceVariant),
            ) {
                if (artworkUrl != null) {
                    AsyncImage(
                        model = artworkUrl,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.outlineVariant)
                        .pointerInput(durationMs) {
                            detectTapGestures { offset ->
                                if (durationMs > 0) {
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    onSeek((fraction * durationMs).toLong())
                                }
                            }
                        },
                ) {
                    // Smooth progress interpolation: the underlying `progress` value updates in
                    // discrete ~500 ms steps from the playback service, which used to make the
                    // bar visibly tick. Animating the visible width with a linear 500 ms tween
                    // makes the fill glide continuously between updates.
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
                        label = "seekBarFill",
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.primary),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatTime(positionMs), style = MaterialTheme.typography.labelSmall, color = ext.muted)
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
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = colors.onBackground,
                        modifier = Modifier.size(28.dp),
                    )
                }
                // Big accent play/pause button.
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = colors.onBackground,
                        modifier = Modifier.size(28.dp),
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
                ActionButton(
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = "Like",
                    tinted = isLiked,
                    onClick = onToggleLike,
                )
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
    }

    if (showSleepSheet) {
        SleepTimerSheet(
            active = sleepTimerEndAt != null,
            remaining = sleepRemaining,
            onPick = { minutes ->
                onSetSleepTimer(minutes * 60_000L)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    active: Boolean,
    remaining: String?,
    onPick: (Int) -> Unit,
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

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
