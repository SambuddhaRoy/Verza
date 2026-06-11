package com.verza.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.ui.sleeve.LocalSleeveMode
import com.verza.ui.sleeve.sleeveSurface
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.FontSleeve
import com.verza.ui.theme.LocalCoverColors
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    isPlaying: Boolean,
    artworkColor: Color,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    progress: Float = 0f,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val shape = VerzaShape

    // Sleeve renders the mini-player as a translucent editorial strip over the reactive glow,
    // recoloured to the cover and set in Newsreader.
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
    val stripBackground = if (sleeve) Modifier.sleeveSurface(shape) else Modifier.background(colors.surface)
    val titleColor = if (sleeve) cover.ink else colors.onSurface
    val artistColor = if (sleeve) cover.sub else ext.muted
    val playTint = if (sleeve) cover.accent else colors.primary
    val titleFont = if (sleeve) FontSleeve else MaterialTheme.typography.titleSmall.fontFamily

    // Swipe horizontally across the strip to skip: left → next, right → previous. The threshold
    // is generous (56 dp) so casual scroll-flicks over the strip don't change the track.
    val skipThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(56.dp)
            // Standard mode is a solid card, so a soft drop shadow lifts it. In Sleeve the strip is
            // a translucent glass panel over the glow — a Material shadow shows through it and reads
            // as a heavy grey halo (especially on a light background), so we drop it there.
            .then(if (sleeve) Modifier else Modifier.shadow(elevation = 6.dp, shape = shape, clip = false))
            .clip(shape)
            .then(stripBackground)
            .clickable(onClick = onExpand)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        when {
                            dragTotal <= -skipThresholdPx -> onNext()
                            dragTotal >= skipThresholdPx -> onPrevious()
                        }
                        dragTotal = 0f
                    },
                    onDragCancel = { dragTotal = 0f },
                ) { change, delta ->
                    change.consume()
                    dragTotal += delta
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(VerzaShape)
                    .background(artworkColor),
            ) {
                if (artworkUrl != null) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = titleFont),
                    color = titleColor,
                    maxLines = 1,
                    // Long titles glide past instead of cutting to an ellipsis.
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                )
                if (sleeve) {
                    Text(
                        text = artist.uppercase(),
                        style = TextStyle(fontFamily = FontMono, fontSize = 9.5.sp, letterSpacing = 0.06.em),
                        color = artistColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = artistColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = onTogglePlay, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = playTint,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // Track progress along the strip's foot — glides between the service's ~500 ms ticks.
        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 500, easing = LinearEasing),
            label = "miniProgress",
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp)
                .fillMaxWidth(animatedProgress)
                .height(2.dp)
                .background(playTint.copy(alpha = 0.85f)),
        )
    }
}
