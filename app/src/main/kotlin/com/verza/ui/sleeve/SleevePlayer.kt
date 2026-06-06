package com.verza.ui.sleeve

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.verza.player.QueueItem
import com.verza.ui.theme.CoverColors
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.FontSleeve
import com.verza.ui.theme.LocalCoverColors

/**
 * "Sleeve" — an editorial, poster-style Now Playing modelled on the UMBRA reference's lead
 * direction: the cover fills the screen behind a graded, grained, vignetted scrim that dissolves
 * into the reactive glow at its edges; a thin Newsreader-400 masthead names the artist and track;
 * and the queue is set as a contextual list that keeps the current song pinned near the top, set
 * large + bold, with the change animating between songs.
 */
@Composable
fun SleevePlayer(
    onBack: () -> Unit,
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
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onOpenLyrics: () -> Unit,
    onToggleLike: () -> Unit,
    onStartRadio: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onAmbient: () -> Unit,
    onLinerNotes: () -> Unit,
    onFocus: () -> Unit,
    onShareSession: () -> Unit,
    onSleepTimer: () -> Unit,
    sleepRemaining: String? = null,
    focusActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Colours come from the app-wide cover palette (sampled from this very art).
    val cover = LocalCoverColors.current
    val accent = cover.accent
    val ink = cover.ink
    val sub = cover.sub
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    // Slow cinematic drift on the cover — a gentle Ken-Burns zoom that keeps the poster alive.
    val drift = rememberInfiniteTransition(label = "sleeveDrift")
    val coverScale by drift.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "coverScale",
    )

    // No opaque background here — the root is transparent so the live reactive glow shows
    // through wherever the cover below feathers away to nothing.
    Box(modifier = modifier.fillMaxSize()) {
        // ── Feathered cover (the "photograph" layer) ──────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Organic edge melt (sides, bottom, corners).
                    drawRect(
                        brush = Brush.radialGradient(
                            0.0f to Color.White,
                            0.55f to Color.White,
                            1.0f to Color.Transparent,
                            center = Offset(size.width * 0.5f, size.height * 0.46f),
                            radius = size.width * 1.04f,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                    // Drive the very top fully to zero so the cover dissolves completely into the
                    // glow above it — no hard line at the status bar.
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.18f to Color.White,
                            1.0f to Color.White,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            Box(Modifier.fillMaxSize().grain(0.08f).vignette(0.40f)) {
                if (artworkUrl != null) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { scaleX = coverScale; scaleY = coverScale },
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(Modifier.fillMaxSize().moodyBackdrop(cover))
                }
            }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }

        // Soft bottom grade (over the glow) — keeps the transport + queue legible at the foot.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.45f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.55f),
                ),
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            // ── Pill tabs (filled centre, like the reference's active tab) ────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SleevePill(text = "Close", onClick = onBack, cover = cover)
                SleevePill(text = "Now Playing", filled = true, onClick = {}, cover = cover)
                SleevePill(text = "Lyrics", onClick = onOpenLyrics, cover = cover)
            }

            Spacer(Modifier.height(22.dp))

            // ── Masthead: artist · "title" hero (chromatic) · catalogue line ──
            Text(
                text = artist.ifBlank { "Unknown artist" },
                style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
                color = sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ChromaticText(
                text = "“$title”",
                style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-0.02).em),
                color = ink,
                intensity = 0.013f,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Eyebrow(
                text = (if (queue.size > 1) "Verza · ${currentIndex + 1} / ${queue.size}" else "Verza") + "  ·  Liner notes",
                color = sub,
                modifier = Modifier.clickable(onClick = onLinerNotes),
            )

            Spacer(Modifier.height(14.dp))

            // ── Queue ─────────────────────────────────────────────────────────
            // Fills the space between masthead and transport. The current song sits second from
            // the top, large + bold; switching songs animates both the type and the scroll.
            if (queue.size > 1) {
                SleeveQueue(
                    queue = queue,
                    currentIndex = currentIndex,
                    cover = cover,
                    onPlayQueueItem = onPlayQueueItem,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            // ── Secondary actions: core icons + a "more" menu ─────────────────
            // The five everyday actions sit on the row; occasional ones (focus, sleep, ambient,
            // share) live behind ⋯ so the poster stays uncluttered — and so every feature that the
            // standard player exposes is reachable in Sleeve too.
            var moreOpen by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SleeveActionIcon(
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) accent else sub,
                    onClick = onToggleLike,
                )
                SleeveActionIcon(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = sub,
                    onClick = onAddToPlaylist,
                )
                SleeveActionIcon(
                    icon = Icons.Filled.Radio,
                    contentDescription = "Start radio",
                    tint = sub,
                    onClick = onStartRadio,
                )
                val downloadIcon = when {
                    isDownloaded -> Icons.Filled.DownloadDone
                    isDownloading -> Icons.Filled.Downloading
                    else -> Icons.Filled.Download
                }
                SleeveActionIcon(
                    icon = downloadIcon,
                    contentDescription = if (isDownloaded) "Remove download" else "Download",
                    tint = if (isDownloaded) accent else sub,
                    enabled = !isDownloading,
                    onClick = { if (isDownloaded) onRemoveDownload() else onDownload() },
                )
                Box {
                    SleeveActionIcon(
                        icon = Icons.Filled.MoreHoriz,
                        contentDescription = "More",
                        // Tint accent while a focus block or sleep timer is running, so the row hints
                        // that something's armed even when its control is tucked away.
                        tint = if (focusActive || sleepRemaining != null) accent else sub,
                        onClick = { moreOpen = true },
                    )
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (focusActive) "Focus session · on" else "Focus session") },
                            onClick = { moreOpen = false; onFocus() },
                        )
                        DropdownMenuItem(
                            text = { Text(if (sleepRemaining != null) "Sleep timer · $sleepRemaining" else "Sleep timer") },
                            onClick = { moreOpen = false; onSleepTimer() },
                        )
                        DropdownMenuItem(
                            text = { Text("Ambient display") },
                            onClick = { moreOpen = false; onAmbient() },
                        )
                        DropdownMenuItem(
                            text = { Text("Share as image") },
                            onClick = { moreOpen = false; onShare() },
                        )
                        DropdownMenuItem(
                            text = { Text("Share listening session") },
                            onClick = { moreOpen = false; onShareSession() },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Progress (with flanking times, reference layout) ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(fmtTime(positionMs), style = TextStyle(fontFamily = FontMono, fontSize = 10.5.sp), color = sub)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .pointerInput(durationMs) {
                            detectTapGestures { offset ->
                                if (durationMs > 0) onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                            }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(Modifier.fillMaxWidth().height(1.5.dp).background(cover.line))
                    Box(Modifier.fillMaxWidth(progress).height(1.5.dp).background(ink))
                    Box(
                        Modifier.fillMaxWidth(progress).height(7.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(accent))
                    }
                }
                Text(fmtTime(durationMs), style = TextStyle(fontFamily = FontMono, fontSize = 10.5.sp), color = sub)
            }
            Spacer(Modifier.height(6.dp))
            // ── Transport: shuffle · prev · play · next · repeat ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Filled.Shuffle,
                        "Shuffle",
                        tint = if (shuffleEnabled) accent else sub,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", tint = ink, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (isPlaying) "Pause" else "Play",
                        tint = ink,
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, "Next", tint = ink, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) accent else sub,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * The contextual queue. On entry the list is positioned so the current track sits *second from
 * top* (the previous track above it for context); when the song changes it animates the scroll to
 * keep that framing. Each row's type animates between the small, dimmed "other" treatment and the
 * large bold "current" one.
 */
@Composable
private fun SleeveQueue(
    queue: List<QueueItem>,
    currentIndex: Int,
    cover: CoverColors,
    onPlayQueueItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Start already framed (no first-frame jump): the item before the current one at the top.
    val initialTop = remember { (currentIndex - 1).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialTop)
    // Re-frame with a smooth animated scroll whenever the current song changes.
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        itemsIndexed(queue) { index, item ->
            SleeveQueueItem(
                index = index,
                title = item.title,
                isCurrent = index == currentIndex,
                cover = cover,
                onClick = { onPlayQueueItem(index) },
            )
        }
    }
}

@Composable
private fun SleeveQueueItem(
    index: Int,
    title: String,
    isCurrent: Boolean,
    cover: CoverColors,
    onClick: () -> Unit,
) {
    // Animate the type between the two states so the current-song change reads as a smooth grow.
    val fontSize by animateFloatAsState(
        targetValue = if (isCurrent) 32f else 19f,
        animationSpec = tween(durationMillis = 380),
        label = "queueFontSize",
    )
    val titleColor by animateColorAsState(
        targetValue = if (isCurrent) cover.ink else cover.ink.copy(alpha = 0.50f),
        animationSpec = tween(durationMillis = 380),
        label = "queueColor",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "%02d".format(index + 1),
            style = TextStyle(fontFamily = FontMono, fontSize = 11.sp, letterSpacing = 0.04.em),
            color = if (isCurrent) cover.accent else cover.faint,
            modifier = Modifier.padding(top = if (isCurrent) 10.dp else 4.dp),
        )
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontSleeve,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.08f).sp,
                letterSpacing = (-0.015).em,
            ),
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SleeveActionIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

private fun fmtTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
