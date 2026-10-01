package com.verza.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.ui.sleeve.grain
import com.verza.ui.sleeve.moodyBackdrop
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.FontSleeve
import com.verza.ui.theme.LocalArtworkColors
import com.verza.ui.theme.VerzaShape
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Holds the screen awake while composed — for the lean-back ambient display. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * A full-screen, keep-awake "music art" display for when the phone is set down, docked or charging:
 * a big editorial clock above the floating album cover, the now-playing masthead below, over a
 * moody cover-coloured canvas. Lean-back by design — tap anywhere (or press back) to leave.
 */
@Composable
fun AmbientDisplay(
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onExit: () -> Unit,
) {
    KeepScreenOn()
    BackHandler { onExit() }
    val cover = LocalArtworkColors.current
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    // A ticking clock — refreshed every 10s is enough to catch minute changes.
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            kotlinx.coroutines.delay(10_000)
        }
    }
    val timeText = now.format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault()))
    val dateText = now.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())).uppercase()

    // Gentle cover drift keeps the screen alive without burning in.
    val drift = rememberInfiniteTransition(label = "ambientDrift")
    val coverScale by drift.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(34_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ambientCoverScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cover.bg)
            .moodyBackdrop(cover)
            .grain(0.05f)
            // Tap anywhere except the play control to leave.
            .pointerInput(Unit) { detectTapGestures { onExit() } },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.8f))

            // ── Clock ─────────────────────────────────────────────────────────
            Text(
                text = timeText,
                style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 86.sp, lineHeight = 90.sp, letterSpacing = (-0.02).em),
                color = cover.ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = dateText,
                style = TextStyle(fontFamily = FontMono, fontSize = 11.sp, letterSpacing = 0.22.em),
                color = cover.sub,
            )

            Spacer(Modifier.weight(0.7f))

            // ── Floating cover ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(1f)
                    .graphicsLayer { scaleX = coverScale; scaleY = coverScale }
                    .shadow(elevation = 28.dp, shape = VerzaShape, clip = false)
                    .clip(VerzaShape)
                    .background(cover.line),
            ) {
                if (artworkUrl != null) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Masthead ──────────────────────────────────────────────────────
            Text(
                text = title,
                style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em),
                color = cover.ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = artist.uppercase(),
                style = TextStyle(fontFamily = FontMono, fontSize = 11.sp, letterSpacing = 0.14.em),
                color = cover.sub,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.weight(1f))

            // ── Minimal transport: a hairline progress + a quiet play/pause ───
            Box(Modifier.fillMaxWidth().height(1.5.dp).background(cover.line)) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(cover.accent))
            }
            Spacer(Modifier.height(8.dp))
            IconButton(onClick = onTogglePlay) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = cover.sub,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
