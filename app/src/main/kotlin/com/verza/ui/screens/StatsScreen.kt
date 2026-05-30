package com.verza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.verza.data.db.ArtistStat
import com.verza.data.db.SongStat
import com.verza.ui.components.EditorialSectionHeader
import com.verza.ui.components.rememberSongArtwork
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.LocalVerzaExtendedColors

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current

    val totalPlays by viewModel.totalPlays.collectAsStateWithLifecycle()
    val totalMs by viewModel.totalListenedMs.collectAsStateWithLifecycle()
    val topSongs by viewModel.topSongs.collectAsStateWithLifecycle()
    val topArtists by viewModel.topArtists.collectAsStateWithLifecycle()
    val streak by viewModel.dayStreak.collectAsStateWithLifecycle()

    val isEmpty = totalPlays == 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(start = 12.dp, end = 20.dp, top = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onBackground)
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .width(40.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primary),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Insights",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your Sound",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onBackground,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        if (isEmpty) {
            item {
                Text(
                    "Play some music and your listening story will appear here.",
                    style = CaptionItalic,
                    color = ext.muted,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp),
                )
            }
            return@LazyColumn
        }

        // ── Headline numbers ─────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BigStat(value = formatDuration(totalMs), label = "listened", modifier = Modifier.weight(1f))
                BigStat(value = totalPlays.toString(), label = "tracks", modifier = Modifier.weight(1f))
                BigStat(value = streak.toString(), label = if (streak == 1) "day streak" else "day streak", modifier = Modifier.weight(1f))
            }
        }

        // ── Top artists ──────────────────────────────────────────────────────
        if (topArtists.isNotEmpty()) {
            item { EditorialSectionHeader("Top artists") }
            itemsIndexedArtists(topArtists)
        }

        // ── Top tracks ───────────────────────────────────────────────────────
        if (topSongs.isNotEmpty()) {
            item { EditorialSectionHeader("Top tracks") }
            itemsIndexedSongs(topSongs)
        }
    }
}

// LazyListScope extension helpers keep the main builder readable.
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedArtists(artists: List<ArtistStat>) {
    itemsIndexed(artists, key = { _, a -> a.artist }) { index, artist ->
        ArtistRow(rank = index + 1, artist = artist)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedSongs(songs: List<SongStat>) {
    itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
        SongStatRow(rank = index + 1, song = song)
    }
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(modifier = modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = ext.muted)
    }
}

@Composable
private fun ArtistRow(rank: Int, artist: ArtistStat) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                rank.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleLarge,
                color = colors.primary,
            )
            Text(
                artist.artist,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${artist.plays} plays",
                style = CaptionItalic,
                color = ext.muted,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = ext.borderGlass, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
private fun SongStatRow(rank: Int, song: SongStat) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val art = rememberSongArtwork(song.title, song.artist, song.thumbnailUrl)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                rank.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelMedium,
                color = ext.muted,
                modifier = Modifier.width(20.dp),
            )
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
                    song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatDuration(song.totalMs),
                style = CaptionItalic,
                color = ext.muted,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = ext.borderGlass, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

/** Formats milliseconds as a friendly listening duration: "12m", "3h 24m", "1d 4h". */
private fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val days = totalMin / (60 * 24)
    val hours = (totalMin % (60 * 24)) / 60
    val mins = totalMin % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}
