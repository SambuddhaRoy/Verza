package com.verza.ui.sleeve

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.player.QueueItem
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.FontSleeve
import com.verza.ui.theme.LocalCoverColors

/**
 * "Sleeve" — an editorial, poster-style Now Playing modelled on the UMBRA reference's lead
 * direction: the cover fills the screen behind a graded, grained, vignetted scrim; a thin
 * Newsreader-400 masthead names the artist and track (the title carries a faint chromatic-
 * aberration split); and the queue is set as a *flowing* numbered tracklist whose serif titles
 * wrap and share lines like printed type.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleevePlayer(
    onBack: () -> Unit,
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    queue: List<QueueItem>,
    currentIndex: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onOpenLyrics: () -> Unit,
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
        // The cover + its grain/vignette/tame-down are composited into an offscreen layer, then a
        // radial alpha mask (DstIn) dissolves the layer toward its edges. Solid through the centre
        // where the photo reads; melting into the surrounding reactive glow at the top, bottom and
        // corners — so there's no hard rectangle and no seam at the status bar.
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
                    // The radial alone still leaves a little opacity at the very top edge, which —
                    // because the cover can't bleed up into the status-bar region — reads as a hard
                    // line there. This second mask drives the top fully to zero so the cover
                    // dissolves completely into the glow above it, with no seam.
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
            // Gentle overall tame-down so bright covers don't blow out the ink — faded at the
            // edges along with the cover since it lives inside the masked layer.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }

        // Soft bottom grade (over the glow) — keeps the transport + time legible without
        // re-introducing a hard band; reads as a natural poster vignette at the foot.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.60f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.45f),
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

            Spacer(Modifier.height(26.dp))

            // ── Masthead: artist · "title" hero (chromatic) · catalogue line ──
            Text(
                text = artist.ifBlank { "Unknown artist" },
                style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em),
                color = sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ChromaticText(
                text = "“$title”",
                style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 46.sp, lineHeight = 48.sp, letterSpacing = (-0.02).em),
                color = ink,
                intensity = 0.013f,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Eyebrow(
                text = if (queue.size > 1) "Verza · ${currentIndex + 1} / ${queue.size}" else "Verza",
                color = sub,
            )

            // Breathing gap — lets the cover photograph show between masthead and tracklist.
            Spacer(Modifier.weight(1f))

            // ── Flowing tracklist (the queue) — capped + scrollable for long queues ──
            if (queue.size > 1) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    queue.forEachIndexed { index, item ->
                        val isCurrent = index == currentIndex
                        Row(
                            modifier = Modifier.clickable { onPlayQueueItem(index) },
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "%02d".format(index + 1),
                                style = TextStyle(fontFamily = FontMono, fontSize = 12.sp),
                                color = if (isCurrent) accent else sub,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = item.title,
                                style = TextStyle(
                                    fontFamily = FontSleeve,
                                    fontWeight = FontWeight.Normal,
                                    fontStyle = if (isCurrent) FontStyle.Italic else FontStyle.Normal,
                                    fontSize = 30.sp,
                                    lineHeight = 34.sp,
                                    letterSpacing = (-0.015).em,
                                ),
                                color = if (isCurrent) ink else ink.copy(alpha = 0.78f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            // ── Control strip ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                // Accent playhead dot, as in the reference transport.
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(7.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(accent))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(fmtTime(positionMs), style = TextStyle(fontFamily = FontMono, fontSize = 10.5.sp), color = sub)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
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
                }
                Text(fmtTime(durationMs), style = TextStyle(fontFamily = FontMono, fontSize = 10.5.sp), color = sub)
            }
        }
    }
}

private fun fmtTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
