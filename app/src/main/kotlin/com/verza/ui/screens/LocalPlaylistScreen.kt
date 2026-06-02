package com.verza.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.verza.data.db.SongEntity
import com.verza.innertube.models.MusicItem
import com.verza.ui.components.TrackActionsMenu
import com.verza.ui.components.pressableScale
import com.verza.ui.components.rememberSongArtwork
import com.verza.ui.sleeve.Eyebrow
import com.verza.ui.sleeve.LocalSleeveMode
import com.verza.ui.sleeve.SleeveAccentPlay
import com.verza.ui.sleeve.SleeveOutlineAction
import com.verza.ui.sleeve.SleeveTrackRow
import com.verza.ui.sleeve.grain
import com.verza.ui.sleeve.moodyBackdrop
import com.verza.ui.theme.FontSleeve
import com.verza.ui.theme.LocalCoverColors
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
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
    val name by viewModel.name.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = if (sleeve) 0.dp else 64.dp, bottom = 28.dp),
        ) {
            if (sleeve) {
                // ── Editorial (UMBRA) layout ─────────────────────────────────────
                item { SleevePlaylistHeader(name, tracks) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SleeveAccentPlay(
                            cover = cover,
                            onClick = { if (tracks.isNotEmpty()) onPlayTracks(tracks.map { it.toMusicItem() }, 0) },
                            icon = Icons.Filled.PlayArrow,
                            contentDescription = "Play all",
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Play",
                                style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 18.sp),
                                color = cover.ink,
                            )
                            Eyebrow(text = "Curated by you", color = cover.faint)
                        }
                        SleeveOutlineAction(
                            cover = cover,
                            onClick = { if (tracks.isNotEmpty()) onShuffle(tracks.map { it.toMusicItem() }) },
                            icon = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                        )
                        SleeveOutlineAction(
                            cover = cover,
                            onClick = { showDeleteConfirm = true },
                            icon = Icons.Filled.Delete,
                            contentDescription = "Delete playlist",
                        )
                    }
                }
                if (tracks.isEmpty()) {
                    item { EmptyHint(cover.sub) }
                } else {
                    itemsIndexed(tracks) { index, track ->
                        SleeveTrackRow(
                            index = index + 1,
                            title = track.title,
                            cover = cover,
                            subtitle = track.artist.ifBlank { null },
                            duration = fmtDurPlaylist(track.durationMs),
                            onClick = { onPlayTracks(tracks.map { it.toMusicItem() }, index) },
                            trailing = { TrackActionsMenu(item = track.toMusicItem()) },
                        )
                    }
                }
            } else {
                // ── Standard layout ──────────────────────────────────────────────
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
                    item { EmptyHint(ext.muted) }
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
        }

        // Floating back button.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(if (sleeve) Color.Black.copy(alpha = 0.34f) else colors.surface),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = if (sleeve) cover.ink else colors.onSurface,
            )
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

/** UMBRA-style moody header band for a local playlist: a grained backdrop (the first track's
 *  cover when present, else a cover-coloured wash) grading into the canvas, with a mono eyebrow
 *  and a big Newsreader-400 title. */
@Composable
private fun SleevePlaylistHeader(name: String?, tracks: List<SongEntity>) {
    val cover = LocalCoverColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(252.dp),
    ) {
        Box(Modifier.fillMaxSize().grain(0.09f)) {
            val art = tracks.firstOrNull()?.thumbnailUrl
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize().moodyBackdrop(cover))
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.30f),
                    0.5f to Color.Transparent,
                    1.0f to cover.bg,
                ),
            ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Eyebrow(text = playlistSummary(tracks), color = cover.sub)
            Text(
                text = name ?: "Playlist",
                style = TextStyle(
                    fontFamily = FontSleeve,
                    fontWeight = FontWeight.Normal,
                    fontSize = 50.sp,
                    lineHeight = 50.sp,
                    letterSpacing = (-0.02).em,
                ),
                color = cover.ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyHint(color: Color) {
    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(
            "No tracks yet. Add some from the ⋯ menu on any song.",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

/** "PLAYLIST · N TRACKS · 1H 42M" summary line. */
private fun playlistSummary(tracks: List<SongEntity>): String {
    val total = tracks.sumOf { it.durationMs }
    val base = "Playlist · ${tracks.size} tracks"
    if (total <= 0L) return base
    val min = (total / 60000L).toInt()
    val runtime = if (min >= 60) "${min / 60}h ${min % 60}m" else "$min min"
    return "$base · $runtime"
}

private fun fmtDurPlaylist(ms: Long): String? {
    if (ms <= 0L) return null
    val s = ms / 1000L
    return "%d:%02d".format(s / 60, s % 60)
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressableScale(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
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
                    color = colors.onBackground,
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
        HorizontalDivider(
            thickness = 0.5.dp,
            color = ext.borderGlass,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
        )
    }
}
