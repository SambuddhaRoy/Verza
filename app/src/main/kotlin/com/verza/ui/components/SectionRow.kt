package com.verza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.verza.innertube.models.HomeItem
import com.verza.ui.theme.LocalVerzaExtendedColors

/**
 * The visual style of a Home section. Spotify-inspired mix so the page reads with rhythm rather
 * than as a uniform stack of carousels.
 *  - LARGE       : big featured cards (220 dp), header in headlineMedium.
 *  - STANDARD    : the original 170 dp cards.
 *  - DENSE_CARDS : compact 124 dp cards — useful for "Browse" tail content.
 *  - DENSE_GRID  : a 4×2 non-scrolling grid of small track rows — fits eight items per screen.
 */
enum class SectionStyle { LARGE, STANDARD, DENSE_CARDS, DENSE_GRID }

@Composable
fun SectionRow(
    title: String,
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier,
    style: SectionStyle = SectionStyle.STANDARD,
    onSeeAll: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = title, large = style == SectionStyle.LARGE, onSeeAll = onSeeAll)
        when (style) {
            SectionStyle.LARGE -> CardRow(items = items, cardWidth = 220.dp, onItemClick = onItemClick)
            SectionStyle.STANDARD -> CardRow(items = items, cardWidth = 170.dp, onItemClick = onItemClick)
            SectionStyle.DENSE_CARDS -> CardRow(items = items, cardWidth = 124.dp, onItemClick = onItemClick)
            SectionStyle.DENSE_GRID -> DenseGrid(items = items, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun SectionHeader(title: String, large: Boolean, onSeeAll: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See all", style = MaterialTheme.typography.labelLarge, color = colors.primary)
            }
        }
    }
}

@Composable
private fun CardRow(items: List<HomeItem>, cardWidth: Dp, onItemClick: (HomeItem) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        // Stable keys let Compose skip recomposition / re-layout for unchanged cards while the
        // parent feed scrolls — meaningful for first-paint cost on the rich Home feed.
        items(
            items = items,
            key = { it.videoId ?: it.browseId ?: it.playlistId ?: it.title },
        ) { item -> MediaCard(item = item, width = cardWidth, onClick = { onItemClick(item) }) }
    }
}

/**
 * Non-scrolling 4-row × 2-column grid of compact track rows. Shows up to eight items at once, so
 * the user sees most of their recent / liked / queue at a glance instead of scrolling sideways.
 */
@Composable
private fun DenseGrid(items: List<HomeItem>, onItemClick: (HomeItem) -> Unit) {
    val display = items.take(8)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        display.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { item ->
                    CompactTrackCell(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onClick = { onItemClick(item) },
                    )
                }
                // Keep the trailing cell aligned when the row has only one item.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactTrackCell(item: HomeItem, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    // Dense-grid cells are always songs (Recently played / Liked / Keep listening) — fetch the
    // real album art rather than the YT thumbnail.
    val art = rememberSongArtwork(item.title, item.subtitle, item.thumbnailUrl)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant),
        ) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Default-width card, kept for callers that don't pick a style. */
@Composable
fun MediaCard(item: HomeItem, onClick: () -> Unit) = MediaCard(item = item, width = 170.dp, onClick = onClick)

@Composable
fun MediaCard(item: HomeItem, width: Dp, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val shape = RoundedCornerShape(16.dp)
    // Deterministic gradient placeholder when no real artwork exists.
    val gradient = remember(item.title) { gradientFromKey(item.title) }
    // Songs: prefer the iTunes-resolved album cover over the YT video thumbnail.
    // Albums / playlists / artists: trust the YT thumbnail (already real cover art / channel image).
    val art = if (item.isSong) rememberSongArtwork(item.title, item.subtitle, item.thumbnailUrl)
              else item.thumbnailUrl

    Column(
        modifier = Modifier
            .width(width)
            .clip(shape)
            .background(colors.surface)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                .background(gradient),
        ) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val placeholderPalettes = listOf(
    Color(0xFFF97373) to Color(0xFFEB5C5C),
    Color(0xFF6DD5ED) to Color(0xFF2193B0),
    Color(0xFFF093FB) to Color(0xFFF5576C),
    Color(0xFF43E97B) to Color(0xFF38F9D7),
    Color(0xFFFA709A) to Color(0xFFFEE140),
    Color(0xFF4FACFE) to Color(0xFF00F2FE),
    Color(0xFF667EEA) to Color(0xFF764BA2),
    Color(0xFFFFECD2) to Color(0xFFFCB69F),
)

private fun gradientFromKey(key: String): Brush {
    val pair = placeholderPalettes[(key.hashCode().ushr(1)) % placeholderPalettes.size]
    return Brush.linearGradient(listOf(pair.first, pair.second))
}
