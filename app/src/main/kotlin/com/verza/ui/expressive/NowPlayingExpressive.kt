package com.verza.ui.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import com.verza.audio.VisualizerSignal
import com.verza.ui.theme.LocalAudioSignal

/**
 * Now Playing.
 *
 * The old screen put the controls *on* the artwork and tinted the text with colours sampled from
 * it, which is why it was unreadable: contrast changed with every track and nothing enforced a
 * floor. Here the artwork is a picture, in a box, with nothing written on it, and everything you
 * have to read sits below it on a near-black canvas at fixed contrast. The cover still drives the
 * colour — it just drives the parts where being wrong is a matter of taste rather than legibility.
 *
 * The second change is that the actions are on screen. Lyrics, radio, download and the sleep timer
 * used to be behind an overflow menu, which meant most of what the player could do was invisible
 * unless you went looking.
 */
@Composable
fun NowPlayingExpressive(
    onBack: () -> Unit,
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    sleepTimerActive: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
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

    // The glow breathes with the low end. Read straight off the signal the app already runs for the
    // background — no second visualizer, and it is null (so still) when nothing is playing.
    // A still fallback so the collect is unconditional — a composable call behind ?. changes the
    // call graph between recompositions, which Compose does not allow.
    val stillSignal = remember { MutableStateFlow(VisualizerSignal()) }
    val signal by (LocalAudioSignal.current ?: stillSignal).collectAsState()

    val bass = signal.bass
    val glowStrength by animateFloatAsState(
        targetValue = if (isPlaying) 0.14f + bass * 0.16f else 0.10f,
        animationSpec = ExpressiveMotion.ambient(),
        label = "glow",
    )

    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas),
    ) {
        // Ambient wash. Sits behind everything and never behind text — the copy below is on the
        // flat canvas, so the glow cannot eat into its contrast.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to colors.glow.copy(alpha = glowStrength * 0.35f),
                        1f to colors.glow.copy(alpha = glowStrength),
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp),
        ) {
            // ── artwork ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(0.82f)
                    .clip(ExpressiveCorner)
                    .background(colors.elevated),
            ) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                ExpressiveControl(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    container = Color.Black.copy(alpha = 0.42f),
                    content = Color.White,
                    iconSize = 22.dp,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(44.dp),
                )
                ExpressiveControl(
                    onClick = onShare,
                    icon = Icons.Filled.Share,
                    contentDescription = "Share",
                    container = Color.Black.copy(alpha = 0.42f),
                    content = Color.White,
                    iconSize = 20.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(44.dp),
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── title, artist, and the two per-track actions ──────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.ink,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = artist,
                        color = colors.inkMuted,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ExpressiveControl(
                    onClick = onToggleLike,
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isLiked) "Remove from liked songs" else "Add to liked songs",
                    container = colors.elevated,
                    content = if (isLiked) colors.accent else colors.ink,
                    iconSize = 21.dp,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(Modifier.width(8.dp))
                ExpressiveControl(
                    onClick = onAddToPlaylist,
                    icon = Icons.Filled.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    container = colors.elevated,
                    content = colors.ink,
                    iconSize = 22.dp,
                    modifier = Modifier.size(46.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── position ──────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                WavySeekBar(
                    progress = progress,
                    onSeek = { f -> onSeek((f * durationMs).toLong()) },
                    accent = colors.accent,
                    trackColor = colors.line,
                    animating = isPlaying,
                    amplitude = 0.35f + bass * 0.65f,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = formatDuration(durationMs - positionMs),
                    color = colors.inkFaint,
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── transport ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayPill(
                    playing = isPlaying,
                    onClick = onTogglePlay,
                    icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    container = colors.accent,
                    content = colors.onAccent,
                    modifier = Modifier.weight(1f),
                )
                ExpressiveControl(
                    onClick = onPrevious,
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous track",
                    container = colors.accent,
                    content = colors.onAccent,
                    iconSize = 32.dp,
                    modifier = Modifier.size(92.dp),
                )
                ExpressiveControl(
                    onClick = onNext,
                    icon = Icons.Filled.SkipNext,
                    contentDescription = "Next track",
                    container = colors.accent,
                    content = colors.onAccent,
                    iconSize = 32.dp,
                    modifier = Modifier.size(92.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── queue-shaping row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExpressiveControl(
                    onClick = onToggleShuffle,
                    icon = Icons.Filled.Shuffle,
                    contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
                    container = if (shuffleEnabled) colors.accent else colors.elevated,
                    content = if (shuffleEnabled) colors.onAccent else colors.inkMuted,
                    shape = CookieShape(),
                    iconSize = 20.dp,
                    modifier = Modifier.size(52.dp),
                )
                ExpressiveControl(
                    onClick = onCycleRepeat,
                    icon = if (repeatMode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = when (repeatMode) {
                        1 -> "Repeat one"
                        2 -> "Repeat all"
                        else -> "Repeat off"
                    },
                    container = if (repeatMode != 0) colors.accent else colors.elevated,
                    content = if (repeatMode != 0) colors.onAccent else colors.inkMuted,
                    iconSize = 20.dp,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.weight(1f))
                ExpressiveControl(
                    onClick = onOpenQueue,
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue",
                    container = colors.elevated,
                    content = colors.ink,
                    shape = ExpressiveCornerSmall,
                    iconSize = 22.dp,
                    modifier = Modifier.size(width = 62.dp, height = 52.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── everything that used to be in a menu ──────────────────────────────
            ExpressiveToolbar(
                items = listOf(
                    ToolbarItem(Icons.Filled.Article, "Lyrics", onOpenLyrics),
                    ToolbarItem(Icons.Filled.Radio, "Start radio", onStartRadio),
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
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(14.dp))
        }
    }
}

internal fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
