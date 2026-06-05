package com.verza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.MusicItem
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaShape

/** Builds the playable [MusicItem] for a song home card (videoId-backed). */
internal fun HomeItem.toMusicItem(): MusicItem = MusicItem(
    id = videoId ?: browseId ?: playlistId ?: title,
    title = title,
    artist = subtitle,
    thumbnailUrl = thumbnailUrl,
)

/**
 * Long-press context menu for a Home feed card. Surfaces the right actions for the item's kind —
 * songs get the full set (play / play next / queue / radio / like / add-to-playlist / artist),
 * albums & playlists get play / queue / open, artists just open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeItemSheet(
    item: HomeItem,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onStartRadio: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToArtist: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isSong = item.isSong
    val isArtist = !isSong && item.browseId?.startsWith("UC") == true
    val isCollection = !isSong && !isArtist

    // Runs the action then closes the sheet.
    fun act(block: () -> Unit): () -> Unit = { block(); onDismiss() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            // ── Header: cover · title · subtitle ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(VerzaShape)
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.thumbnailUrl != null) {
                        AsyncImage(model = item.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = ext.muted)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sub = item.subtitle.ifBlank {
                        when {
                            isArtist -> "Artist"
                            isCollection -> "Album or playlist"
                            else -> "Song"
                        }
                    }
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = colors.outlineVariant)
            Spacer(Modifier.height(4.dp))

            // ── Actions (kind-aware) ──────────────────────────────────────────
            if (isSong || isCollection) {
                ActionRow(Icons.Filled.PlayArrow, "Play", onClick = act(onPlay))
            }
            if (isSong) {
                ActionRow(Icons.Filled.SkipNext, "Play next", onClick = act(onPlayNext))
            }
            if (isSong || isCollection) {
                ActionRow(Icons.Filled.AddToQueue, "Add to queue", onClick = act(onAddToQueue))
            }
            if (isSong) {
                ActionRow(Icons.Filled.Radio, "Start radio", onClick = act(onStartRadio))
                ActionRow(
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = if (isLiked) "Liked" else "Like",
                    tint = if (isLiked) colors.primary else colors.onSurface,
                    onClick = act(onToggleLike),
                )
                ActionRow(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist", onClick = act(onAddToPlaylist))
                ActionRow(Icons.Filled.Person, "Go to artist", onClick = act(onGoToArtist))
            }
            if (isCollection || isArtist) {
                ActionRow(Icons.AutoMirrored.Filled.ArrowForward, "Open", onClick = act(onOpen))
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint ?: colors.onSurface, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint ?: colors.onSurface)
    }
}
