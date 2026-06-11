package com.verza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.verza.ui.verso.ThreadLine
import com.verza.ui.verso.breathe
import com.verza.ui.verso.pebbleShape

/**
 * The floating "now" pebble. Verso renders the mini-player as a breathing, asymmetric pebble
 * with the played fraction of the track drawn as a living thread along its foot — the strip is
 * a creature at rest, not a toolbar.
 */
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
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current

    // Sleeve keeps its translucent editorial strip; Verso (standard) gets the pebble.
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
    val shape = if (sleeve) VerzaShape else remember(title) { pebbleShape(title, base = 22.dp, swing = 12.dp) }
    val artShape = if (sleeve) VerzaShape else remember(title) { pebbleShape(title.reversed(), base = 14.dp, swing = 8.dp) }
    val stripBackground = if (sleeve) Modifier.sleeveSurface(shape) else Modifier.background(colors.surface)
    val titleColor = if (sleeve) cover.ink else colors.onSurface
    val artistColor = if (sleeve) cover.sub else ext.muted
    val playTint = if (sleeve) cover.accent else colors.primary
    val titleFont = if (sleeve) FontSleeve else MaterialTheme.typography.titleSmall.fontFamily

    Column(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            // The resting breath — a touch deeper while the music moves.
            .breathe(seed = title.hashCode(), amount = if (isPlaying) 0.005f else 0.002f)
            // Standard mode is a solid pebble, so a soft drop shadow lifts it. In Sleeve the strip
            // is translucent glass over the glow — a Material shadow reads as a grey halo there.
            .then(if (sleeve) Modifier else Modifier.shadow(elevation = 6.dp, shape = shape, clip = false))
            .clip(shape)
            .then(stripBackground)
            .clickable(onClick = onExpand),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(artShape)
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
                    overflow = TextOverflow.Ellipsis,
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

            // Play/pause as a quiet accent node rather than a bare icon button.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(playTint.copy(alpha = if (sleeve) 0.18f else 0.14f))
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = playTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        // The track's progress as a living thread along the pebble's foot.
        ThreadLine(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            color = playTint,
            amplitude = 1.6.dp,
            wavelength = 34.dp,
            thickness = 1.6.dp,
            reveal = progress.coerceIn(0f, 1f),
            alive = isPlaying,
        )
        Spacer(Modifier.height(3.dp))
    }
}
