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
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.verza.ui.expressive.ExpressiveMoreSheet
import com.verza.ui.expressive.ExpressiveQueueSheet
import com.verza.ui.expressive.NowPlayingExpressive
import com.verza.ui.theme.VerzaShape
import com.verza.ui.theme.glassSurface

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
    /** Radio weighted toward music you haven't heard (see DiscoveryRadio). Standard player only. */
    onStartDiscoveryRadio: () -> Unit,
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

    // One layout now. The standard screen and the Sleeve poster both wrote text over the artwork in
    // colours sampled from it, so contrast changed with every track and regularly failed outright.
    // NowPlayingExpressive keeps the cover for accent and glow and puts everything readable on a flat
    // near-black canvas at fixed contrast.
    //
    // sleeveMode stays in the signature because Settings still shows the switch; it no longer picks a
    // layout. Removing the preference and ui/sleeve is a separate change.
    var showQueue by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        NowPlayingExpressive(
            onBack = onBack,
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            isPlaying = isPlaying,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            positionMs = positionMs,
            durationMs = durationMs,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            sleepTimerActive = sleepTimerEndAt != null,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onToggleLike = onToggleLike,
            onAddToPlaylist = onAddToPlaylist,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onOpenQueue = { showQueue = true },
            onOpenLyrics = onOpenLyrics,
            onStartRadio = onStartRadio,
            onDownload = onDownload,
            onRemoveDownload = onRemoveDownload,
            onOpenSleepTimer = { showSleepSheet = true },
            onOpenMore = { showMore = true },
            onShare = { showShareCard = true },
            modifier = Modifier.fillMaxSize(),
        )

        if (showQueue) {
            ExpressiveQueueSheet(
                queue = queue,
                currentIndex = currentIndex,
                onPlay = { showQueue = false; onPlayQueueItem(it) },
                onRemove = onRemoveQueueItem,
                onDismiss = { showQueue = false },
            )
        }
        if (showMore) {
            ExpressiveMoreSheet(
                isDownloading = isDownloading,
                focusActive = focusActive,
                onDiscoveryRadio = { showMore = false; onStartDiscoveryRadio() },
                onAmbient = { showMore = false; onEnterAmbient() },
                onLinerNotes = { showMore = false; showLinerNotes = true },
                onFocus = { showMore = false; showFocusSheet = true },
                onShareSession = { showMore = false; shareSession() },
                onDismiss = { showMore = false },
            )
        }
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
    val art = rememberSongArtwork(item.title, item.artist, item.artworkUrl)
    // The playing item reads as a glass row floating over the wash; the rest stay as a clean list.
    val rowShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .then(if (isCurrent) Modifier.glassSurface(rowShape) else Modifier.clip(rowShape))
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
