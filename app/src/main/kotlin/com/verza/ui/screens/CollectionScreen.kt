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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.verza.innertube.models.CollectionDetail
import com.verza.innertube.models.MusicItem
import com.verza.ui.components.TrackActionsMenu
import com.verza.ui.components.pressableScale
import com.verza.ui.components.rememberSongArtwork
import com.verza.ui.sleeve.ChromaticText
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
fun CollectionScreen(
    onBack: () -> Unit,
    onPlayTracks: (List<MusicItem>, Int) -> Unit,
    onShuffle: (List<MusicItem>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is CollectionUiState.Loading -> CircularProgressIndicator(
                color = colors.primary,
                modifier = Modifier.align(Alignment.Center),
            )
            is CollectionUiState.Error -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(s.message, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
                OutlinedButton(onClick = viewModel::load, shape = CircleShape) { Text("Retry") }
            }
            is CollectionUiState.Content -> CollectionContent(s.detail, onPlayTracks, onShuffle)
        }

        // Floating circular back affordance.
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
}

@Composable
private fun CollectionContent(
    detail: CollectionDetail,
    onPlayTracks: (List<MusicItem>, Int) -> Unit,
    onShuffle: (List<MusicItem>) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
    val tracks = detail.tracks

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Sleeve's full-bleed header runs to the top edge; the standard header clears the
        // floating back button.
        contentPadding = PaddingValues(top = if (sleeve) 0.dp else 64.dp, bottom = 28.dp),
    ) {
        if (sleeve) {
            // ── Editorial (UMBRA) layout ─────────────────────────────────────────
            item { SleeveCollectionHeader(detail, tracks.size) }
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
                        onClick = { if (tracks.isNotEmpty()) onPlayTracks(tracks, 0) },
                        icon = Icons.Filled.PlayArrow,
                        contentDescription = "Play all",
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Play",
                            style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 18.sp),
                            color = cover.ink,
                        )
                        Eyebrow(text = trackSummary(tracks.size, tracks), color = cover.faint)
                    }
                    SleeveOutlineAction(
                        cover = cover,
                        onClick = { if (tracks.isNotEmpty()) onShuffle(tracks) },
                        icon = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                    )
                }
            }
            itemsIndexed(tracks) { index, track ->
                SleeveTrackRow(
                    index = index + 1,
                    title = track.title,
                    cover = cover,
                    subtitle = track.artist.ifBlank { null },
                    duration = fmtDur(track.durationMs),
                    onClick = { onPlayTracks(tracks, index) },
                    trailing = { TrackActionsMenu(item = track) },
                )
            }
        } else {
            // ── Standard layout ──────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Album cover with soft shadow.
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), clip = false)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surfaceVariant),
                        ) {
                            if (detail.thumbnailUrl != null) {
                                AsyncImage(
                                    model = detail.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = detail.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.onBackground,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (detail.subtitle.isNotBlank()) {
                                Text(
                                    text = detail.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.muted,
                                )
                            }
                            Text(
                                text = "${tracks.size} tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.muted,
                            )
                        }
                    }

                    // Action pills
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onPlayTracks(tracks, 0) },
                            enabled = tracks.isNotEmpty(),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play all")
                        }
                        OutlinedButton(
                            onClick = { onShuffle(tracks) },
                            enabled = tracks.isNotEmpty(),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }

            itemsIndexed(tracks) { index, track ->
                TrackRow(index = index + 1, track = track) { onPlayTracks(tracks, index) }
            }
        }
    }
}

/** "N TRACKS · 19 MIN" summary line for the editorial header/action row. */
private fun trackSummary(count: Int, tracks: List<MusicItem>): String {
    val total = tracks.sumOf { it.durationMs }
    val base = "$count tracks"
    if (total <= 0L) return base
    val min = (total / 60000L).toInt()
    val runtime = if (min >= 60) "${min / 60}h ${min % 60}m" else "$min min"
    return "$base · $runtime"
}

/** Formats a track duration to m:ss, or null when unknown (so the row hides it). */
private fun fmtDur(ms: Long): String? {
    if (ms <= 0L) return null
    val s = ms / 1000L
    return "%d:%02d".format(s / 60, s % 60)
}

/** UMBRA-style full-bleed album header for Sleeve: a grained, vignetted cover photo grading into
 *  the canvas, with a mono catalogue eyebrow and a big Newsreader-400 title that carries a faint
 *  chromatic split. Falls back to a moody cover-coloured backdrop when there's no artwork. */
@Composable
private fun SleeveCollectionHeader(detail: CollectionDetail, trackCount: Int) {
    val cover = LocalCoverColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
    ) {
        // Photograph layer (grain lives here so the title above stays crisp).
        Box(Modifier.fillMaxSize().grain(0.09f)) {
            if (detail.thumbnailUrl != null) {
                AsyncImage(
                    model = detail.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize().moodyBackdrop(cover))
            }
        }
        // Grade to the cover canvas at the bottom so the header melts into the track list.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.32f),
                    0.45f to Color.Transparent,
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
            val eyebrow = buildString {
                if (detail.subtitle.isNotBlank()) append(detail.subtitle).append("  ·  ")
                append("$trackCount tracks")
            }
            Eyebrow(text = eyebrow, color = cover.sub)
            ChromaticText(
                text = detail.title,
                style = TextStyle(
                    fontFamily = FontSleeve,
                    fontWeight = FontWeight.Normal,
                    fontSize = 50.sp,
                    lineHeight = 50.sp,
                    letterSpacing = (-0.02).em,
                ),
                color = cover.ink,
                intensity = 0.012f,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrackRow(index: Int, track: MusicItem, onClick: () -> Unit) {
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
            TrackActionsMenu(item = track)
        }
        // Hairline between tracks — reads as a magazine track listing rather than a tile stack.
        HorizontalDivider(
            thickness = 0.5.dp,
            color = ext.borderGlass,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
        )
    }
}
