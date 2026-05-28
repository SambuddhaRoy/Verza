package com.verza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.verza.data.db.SongEntity
import com.verza.innertube.models.MusicItem
import com.verza.ui.components.TrackActionsMenu
import com.verza.ui.components.rememberSongArtwork
import com.verza.ui.theme.LocalVerzaExtendedColors

@Composable
fun LocalPlaylistScreen(
    onBack: () -> Unit,
    onPlayTracks: (List<MusicItem>, Int) -> Unit,
    onShuffle: (List<MusicItem>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val name by viewModel.name.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 64.dp, bottom = 16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), clip = false)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(listOf(colors.primary, colors.tertiary))
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            val coverUrl = tracks.firstOrNull()?.thumbnailUrl
                            if (coverUrl != null) {
                                AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                            } else {
                                Text(
                                    text = name?.firstOrNull()?.uppercase() ?: "♪",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = colors.onPrimary,
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Your playlist", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                            Text(
                                text = name ?: "Playlist",
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.onBackground,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("${tracks.size} tracks", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onPlayTracks(tracks.map { it.toMusicItem() }, 0) },
                            enabled = tracks.isNotEmpty(),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play all")
                        }
                        OutlinedButton(
                            onClick = { onShuffle(tracks.map { it.toMusicItem() }) },
                            enabled = tracks.isNotEmpty(),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete playlist", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (tracks.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No tracks yet. Add some from the ⋯ menu on any song.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.muted,
                        )
                    }
                }
            } else {
                itemsIndexed(tracks) { index, track ->
                    LocalPlaylistTrackRow(
                        index = index + 1,
                        track = track,
                        onClick = { onPlayTracks(tracks.map { it.toMusicItem() }, index) },
                        onRemove = { viewModel.removeTrack(track.id) },
                    )
                }
            }
        }

        // Floating back button.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surface),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this playlist?") },
            text = { Text("This removes the playlist. The songs themselves stay in your library.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deletePlaylist()
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LocalPlaylistTrackRow(
    index: Int,
    track: SongEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium,
            color = ext.muted,
            modifier = Modifier.width(24.dp),
        )
        val art = rememberSongArtwork(track.title, track.artist, track.thumbnailUrl)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant),
        ) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (track.artist.isNotBlank()) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TrackActionsMenu(item = track.toMusicItem())
    }
}
