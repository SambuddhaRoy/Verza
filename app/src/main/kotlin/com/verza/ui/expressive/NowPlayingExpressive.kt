package com.verza.ui.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.verza.audio.VisualizerSignal
import com.verza.ui.theme.LocalAudioSignal
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Now Playing, laid out against the Material 3 Expressive reference.
 *
 * Four things carry the style, and all four are choices the old screen did not make. The background
 * is a saturated cover-derived colour rather than a dark neutral. The artwork is masked to a
 * scalloped cloud rather than a rectangle. The title is set enormous in an italic display serif and
 * is the largest thing on the screen by a wide margin. And the transport wraps asymmetrically —
 * a labelled PLAY pill beside one round skip, the other skip dropping to the next row beside the
 * seek bar — instead of sitting as three evenly spaced circles.
 *
 * Readability is not left to the cover. See ExpressiveColors: every text/background pair here is
 * chosen by measured contrast and held above 4.5:1, which is checked across the whole hue wheel by
 * ExpressiveColorsTest rather than trusted.
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

    // The artwork breathes with the low end, read off the signal the app already runs for the glow.
    // A still fallback keeps the collect unconditional — a composable call behind ?. would change
    // the call graph between recompositions, which Compose does not allow.
    val stillSignal = remember { MutableStateFlow(VisualizerSignal()) }
    val signal by (LocalAudioSignal.current ?: stillSignal).collectAsState()
    val bass = signal.bass

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f + bass * 0.035f else 1f,
        animationSpec = ExpressiveMotion.ambient(),
        label = "artPulse",
    )

    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.container)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        // ── top row ──────────────────────────────────────────────────────────────
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

        Spacer(Modifier.height(10.dp))

        // ── artwork, masked to a cloud ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .aspectRatio(1.18f)
                .clip(CloudShape),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.surface)
                    .scale(artScale),
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── the name, doing the shouting ─────────────────────────────────────────
        Text(
            text = title,
            style = HeroDisplay,
            color = colors.accent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = artist,
            style = BodyStrong,
            color = colors.onContainerMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(14.dp))

        // ── position ─────────────────────────────────────────────────────────────
        WavySeekBar(
            progress = progress,
            onSeek = { f -> onSeek((f * durationMs).toLong()) },
            accent = colors.accent,
            trackColor = colors.accentMuted,
            animating = isPlaying,
            amplitude = 0.35f + bass * 0.65f,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(formatDuration(positionMs), style = Timecode, color = colors.onContainerMuted)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(durationMs), style = Timecode, color = colors.onContainerMuted)
        }

        Spacer(Modifier.height(12.dp))

        // ── transport ────────────────────────────────────────────────────────────
        // Reading order left to right: back, play, forward. The shape contrast between the pill and
        // the two circles is what carries the style here — separating the skips onto different rows
        // read as a layout bug, not as expression.
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
                modifier = Modifier.size(72.dp),
            )
            PlayPill(
                playing = isPlaying,
                onClick = onTogglePlay,
                container = colors.accent,
                content = colors.onAccent,
                modifier = Modifier.weight(1f).height(72.dp),
            )
            ExpressiveControl(
                onClick = onNext,
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next track",
                container = colors.accent,
                content = colors.onAccent,
                iconSize = 30.dp,
                modifier = Modifier.size(72.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── queue shaping + everything that used to be in a menu ─────────────────
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
                container = if (repeatMode != 0) colors.accent else colors.surface,
                content = if (repeatMode != 0) colors.onAccent else colors.onSurface,
                iconSize = 20.dp,
                modifier = Modifier.size(52.dp),
            )
            ExpressiveControl(
                onClick = onToggleLike,
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isLiked) "Remove from liked songs" else "Add to liked songs",
                container = if (isLiked) colors.accent else colors.surface,
                content = if (isLiked) colors.onAccent else colors.onSurface,
                iconSize = 20.dp,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.weight(1f))
            ExpressiveControl(
                onClick = onOpenQueue,
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                container = colors.surface,
                content = colors.onSurface,
                shape = ShapeMedium,
                iconSize = 22.dp,
                modifier = Modifier.size(width = 64.dp, height = 52.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        ExpressiveToolbar(
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
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(12.dp))
    }
}

internal fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
