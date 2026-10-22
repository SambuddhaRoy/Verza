package com.verza.ui.screens

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.CompositionLocalProvider
import com.verza.ui.theme.LocalGlassBackdrop
import com.verza.ui.theme.rememberGlassBackdrop
import com.verza.ui.theme.glassSource
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.text.style.TextAlign
import com.verza.ui.expressive.ShapeLarge
import com.verza.ui.theme.isGlass
import androidx.compose.foundation.layout.padding
import com.verza.ui.expressive.LocalBottomChrome
import com.verza.ui.expressive.ShapeLargeIncreased
import com.verza.tracksLabel
import com.verza.ui.expressive.RowTitle
import com.verza.ui.expressive.RowSubtitle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.Download
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
    onAddToQueue: (List<MusicItem>) -> Unit,
    onDownloadAll: (List<MusicItem>, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Glass: the buttons floating over the page are glass over a recording of it.
    val glass = isGlass()
    val backdrop = rememberGlassBackdrop()
    Box(modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(if (glass) Modifier.glassSource(backdrop) else Modifier)) {
        when (val s = state) {
            is CollectionUiState.Loading -> CircularProgressIndicator(
                color = colors.primary,
                modifier = Modifier.align(Alignment.Center).padding(bottom = LocalBottomChrome.current),
            )
            is CollectionUiState.Error -> Column(
                modifier = Modifier.align(Alignment.Center).padding(bottom = LocalBottomChrome.current),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(s.message, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
                OutlinedButton(onClick = viewModel::load, shape = CircleShape) { Text("Retry") }
            }
            is CollectionUiState.Content ->
                CollectionContent(s.detail, onPlayTracks, onShuffle, onAddToQueue, onDownloadAll)
        }
        }

        // In Glass the album's other actions sit opposite the back button, so the header can keep
        // to the cover, the names and two buttons.
        if (glass) (state as? CollectionUiState.Content)?.detail?.let { detail ->
            CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassHeaderButton(Icons.Filled.AddToQueue, "Add to queue") { if (detail.tracks.isNotEmpty()) onAddToQueue(detail.tracks) }
                    GlassHeaderButton(Icons.Filled.Download, "Download") { if (detail.tracks.isNotEmpty()) onDownloadAll(detail.tracks, detail.title) }
                }
            }
        }
        // Floating circular back affordance.
        if (glass) CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            Box(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                GlassHeaderButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", onBack)
            }
        } else IconButton(
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
    onAddToQueue: (List<MusicItem>) -> Unit,
    onDownloadAll: (List<MusicItem>, String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
    val tracks = detail.tracks
    val glass = isGlass()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Sleeve's full-bleed header runs to the top edge; the standard header clears the
        // floating back button.
        contentPadding = PaddingValues(top = if (sleeve) 0.dp else 64.dp, bottom = 28.dp + LocalBottomChrome.current),
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
                    SleeveOutlineAction(
                        cover = cover,
                        onClick = { if (tracks.isNotEmpty()) onAddToQueue(tracks) },
                        icon = Icons.Filled.AddToQueue,
                        contentDescription = "Add to queue",
                    )
                    SleeveOutlineAction(
                        cover = cover,
                        onClick = { if (tracks.isNotEmpty()) onDownloadAll(tracks, detail.title) },
                        icon = Icons.Filled.Download,
                        contentDescription = "Download",
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
        } else if (glass) {
            item { GlassCollectionHeader(detail, onPlayTracks, onShuffle) }
            itemsIndexed(tracks) { index, track ->
                TrackRow(index = index + 1, track = track) { onPlayTracks(tracks, index) }
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
                                .shadow(elevation = 12.dp, shape = ShapeLargeIncreased, clip = false)
                                .clip(ShapeLargeIncreased)
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
                                text = tracksLabel(tracks.size),
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
                        OutlinedButton(
                            onClick = { onAddToQueue(tracks) },
                            enabled = tracks.isNotEmpty(),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Icon(Icons.Filled.AddToQueue, contentDescription = "Add to queue", modifier = Modifier.size(18.dp))
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
    val base = tracksLabel(count)
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
                append(tracksLabel(trackCount))
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

/**
 * The Glass album header: the cover centred and large, the name under it, the artist in the
 * accent, and Play and Shuffle as two equal grey buttons, as an album opens on Apple Music.
 */
@Composable
private fun GlassCollectionHeader(
    detail: CollectionDetail,
    onPlayTracks: (List<MusicItem>, Int) -> Unit,
    onShuffle: (List<MusicItem>) -> Unit,
) {
    val xc = com.verza.ui.expressive.LocalExpressiveColors.current
    val tracks = detail.tracks
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .widthIn(max = 300.dp)
                .aspectRatio(1f)
                .shadow(elevation = 18.dp, shape = ShapeLarge, clip = false)
                .clip(ShapeLarge)
                .background(xc.surfaceHigh),
        ) {
            if (detail.thumbnailUrl != null) AsyncImage(
                model = detail.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            detail.title,
            style = com.verza.ui.expressive.HeroDisplay,
            color = xc.onContainer,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (detail.subtitle.isNotBlank()) Text(
            detail.subtitle,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal, fontSize = 18.sp),
            color = xc.accent,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(trackSummary(tracks.size, tracks), style = MaterialTheme.typography.bodySmall, color = xc.onContainerMuted)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.widthIn(max = 420.dp)) {
            GlassWideButton(Icons.Filled.PlayArrow, "Play", Modifier.weight(1f), enabled = tracks.isNotEmpty()) { onPlayTracks(tracks, 0) }
            GlassWideButton(Icons.Filled.Shuffle, "Shuffle", Modifier.weight(1f), enabled = tracks.isNotEmpty()) { onShuffle(tracks) }
        }
    }
}

@Composable
private fun GlassWideButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val xc = com.verza.ui.expressive.LocalExpressiveColors.current
    Row(
        modifier = modifier
            .height(48.dp)
            .pressableScale(enabled = enabled, onClick = onClick)
            .clip(ShapeLargeIncreased)
            .background(xc.surface),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = xc.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), color = xc.accent)
    }
}

@Composable
private fun TrackRow(index: Int, track: MusicItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val glass = isGlass()

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
                text = if (glass) index.toString() else index.toString().padStart(2, '0'),
                style = if (glass) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.labelMedium,
                color = ext.muted,
                textAlign = if (glass) TextAlign.Center else null,
                modifier = Modifier.width(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                RowTitle(track.title, colors.onBackground, MaterialTheme.typography.titleMedium)
                if (track.artist.isNotBlank()) {
                    RowSubtitle(track.artist, ext.muted, MaterialTheme.typography.bodySmall)
                }
            }
            TrackActionsMenu(item = track)
        }
        // Hairline between tracks — reads as a magazine track listing rather than a tile stack.
        HorizontalDivider(
            thickness = 0.5.dp,
            color = ext.borderGlass,
            // In Glass the rule starts under the title, the number hanging in the margin.
            modifier = Modifier.padding(start = if (glass) 58.dp else 20.dp, end = 20.dp),
        )
    }
}
