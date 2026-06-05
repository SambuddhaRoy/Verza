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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.innertube.models.HomeItem
import com.verza.ui.sleeve.Eyebrow
import com.verza.ui.sleeve.LocalSleeveMode
import com.verza.ui.sleeve.sleeveSurface
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.LocalCoverColors
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaCorner
import com.verza.ui.theme.VerzaShape

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
    onItemLongPress: ((HomeItem) -> Unit)? = null,
    style: SectionStyle = SectionStyle.STANDARD,
    onSeeAll: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = title, large = style == SectionStyle.LARGE, onSeeAll = onSeeAll)
        when (style) {
            SectionStyle.LARGE -> CardRow(items, 220.dp, onItemClick, onItemLongPress)
            SectionStyle.STANDARD -> CardRow(items, 170.dp, onItemClick, onItemLongPress)
            SectionStyle.DENSE_CARDS -> CardRow(items, 124.dp, onItemClick, onItemLongPress)
            SectionStyle.DENSE_GRID -> DenseGrid(items, onItemClick, onItemLongPress)
        }
    }
}

@Composable
private fun SectionHeader(title: String, large: Boolean, onSeeAll: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (sleeve) {
            // Reference Home sets section labels as small mono eyebrows over serif items.
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Eyebrow(text = title, color = cover.sub, modifier = Modifier.weight(1f))
                if (onSeeAll != null) {
                    TextButton(onClick = onSeeAll) {
                        Text(
                            "see all",
                            style = TextStyle(fontFamily = FontMono, fontSize = 9.5.sp, letterSpacing = 0.08.em),
                            color = cover.accent,
                        )
                    }
                }
            }
        } else {
            // Short accent rule sets the editorial mood — same idiom as Settings/EditorialSectionHeader.
            Box(
                Modifier
                    .width(28.dp)
                    .height(1.dp)
                    .clip(RoundedCornerShape(0.5.dp))
                    .background(colors.primary),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
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
                        // Italic serif "see all" reads as an editor's note rather than a button.
                        Text("see all", style = CaptionItalic, color = colors.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardRow(
    items: List<HomeItem>,
    cardWidth: Dp,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: ((HomeItem) -> Unit)? = null,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        // Stable keys let Compose skip recomposition / re-layout for unchanged cards while the
        // parent feed scrolls — meaningful for first-paint cost on the rich Home feed.
        items(
            items = items,
            key = { it.videoId ?: it.browseId ?: it.playlistId ?: it.title },
        ) { item ->
            MediaCard(
                item = item,
                width = cardWidth,
                onClick = { onItemClick(item) },
                onLongClick = onItemLongPress?.let { { it(item) } },
            )
        }
    }
}

/**
 * Non-scrolling 4-row × 2-column grid of compact track rows. Shows up to eight items at once, so
 * the user sees most of their recent / liked / queue at a glance instead of scrolling sideways.
 */
@Composable
private fun DenseGrid(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: ((HomeItem) -> Unit)? = null,
) {
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
                        onLongClick = onItemLongPress?.let { { it(item) } },
                    )
                }
                // Keep the trailing cell aligned when the row has only one item.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactTrackCell(
    item: HomeItem,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    // Dense-grid cells are always songs (Recently played / Liked / Keep listening) — fetch the
    // real album art rather than the YT thumbnail.
    val art = rememberSongArtwork(item.title, item.subtitle, item.thumbnailUrl)
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
    val cellShape = VerzaShape
    val cellSurface = if (sleeve) Modifier.sleeveSurface(cellShape)
                      else Modifier.clip(cellShape).background(colors.surface)
    val titleColor = if (sleeve) cover.ink else colors.onSurface
    val subtitleColor = if (sleeve) cover.faint else ext.muted
    Row(
        modifier = modifier
            .then(cellSurface)
            .pressableScale(onLongClick = onLongClick, onClick = onClick)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(VerzaShape)
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
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                if (sleeve) {
                    Text(
                        text = item.subtitle.uppercase(),
                        style = TextStyle(fontFamily = FontMono, fontSize = 9.sp, letterSpacing = 0.05.em),
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Default-width card, kept for callers that don't pick a style. */
@Composable
fun MediaCard(item: HomeItem, onClick: () -> Unit) = MediaCard(item = item, width = 170.dp, onClick = onClick)

@Composable
fun MediaCard(item: HomeItem, width: Dp, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val shape = VerzaShape
    // Deterministic gradient placeholder when no real artwork exists.
    val gradient = remember(item.title) { gradientFromKey(item.title) }
    // Songs: prefer the iTunes-resolved album cover over the YT video thumbnail.
    // Albums / playlists / artists: trust the YT thumbnail (already real cover art / channel image).
    val art = if (item.isSong) rememberSongArtwork(item.title, item.subtitle, item.thumbnailUrl)
              else item.thumbnailUrl

    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
    val cardSurface = if (sleeve) Modifier.sleeveSurface(shape)
                      else Modifier.clip(shape).background(colors.surface)
    val titleColor = if (sleeve) cover.ink else colors.onSurface
    val subtitleColor = if (sleeve) cover.faint else ext.muted

    Column(
        modifier = Modifier
            .width(width)
            .then(cardSurface)
            .pressableScale(onLongClick = onLongClick, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = VerzaCorner, topEnd = VerzaCorner, bottomStart = 0.dp, bottomEnd = 0.dp))
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
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                if (sleeve) {
                    Text(
                        text = item.subtitle.uppercase(),
                        style = TextStyle(fontFamily = FontMono, fontSize = 9.5.sp, letterSpacing = 0.05.em),
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
