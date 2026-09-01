package com.verza.ui.expressive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verza.innertube.models.HomeItem

/**
 * A titled row of cards — the unit Home is built from.
 *
 * Two things make it expressive rather than a carousel with a bold label. The heading is set in the
 * italic display serif, so a section name reads as editorial rather than as chrome. And the cards
 * alternate size in a repeating rhythm instead of marching along identically, which is the "variety
 * of shapes" tactic doing real work: an even row of squares is exactly what the style is a reaction
 * against.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveSectionRow(
    title: String,
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onItemLongPress: ((HomeItem) -> Unit)? = null,
    onSeeAll: (() -> Unit)? = null,
    density: SectionDensity = SectionDensity.STANDARD,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        ExpressiveSectionHeader(title = title, subtitle = subtitle, onSeeAll = onSeeAll)
        when (density) {
            SectionDensity.FEATURE -> FeatureRow(items, onItemClick, onItemLongPress)
            SectionDensity.LIST -> ListRow(items, onItemClick, onItemLongPress)
            SectionDensity.STANDARD -> StandardRow(items, onItemClick, onItemLongPress)
        }
        Spacer(Modifier.height(22.dp))
    }
}

/**
 * How much room a section takes.
 *
 * Every section used to be the same carousel of the same cards, so a page of six of them read as one
 * long undifferentiated scroll and nothing looked more important than anything else. Three shapes is
 * enough to give the page a beat without turning it into a catalogue of layouts.
 */
enum class SectionDensity { FEATURE, STANDARD, LIST }

/** Big squares, few of them. For the section at the top and whatever else deserves the room. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeatureRow(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: ((HomeItem) -> Unit)?,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items) { item ->
            ExpressiveCard(
                title = item.title,
                subtitle = item.subtitle,
                artworkUrl = item.thumbnailUrl,
                onClick = { onItemClick(item) },
                width = 208.dp,
                aspect = 1f,
                modifier = if (onItemLongPress == null) Modifier else Modifier.combinedClickable(
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongPress(item) },
                ),
            )
        }
    }
}

/** The original rhythm: two standard cards then a wider one. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StandardRow(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: ((HomeItem) -> Unit)?,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(items) { index, item ->
            val wide = index % 3 == 2
            ExpressiveCard(
                title = item.title,
                subtitle = item.subtitle,
                artworkUrl = item.thumbnailUrl,
                onClick = { onItemClick(item) },
                width = if (wide) 200.dp else 152.dp,
                aspect = if (wide) 1.32f else 1f,
                modifier = if (onItemLongPress == null) Modifier else Modifier.combinedClickable(
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongPress(item) },
                ),
            )
        }
    }
}

/**
 * Three compact rows per column, scrolling sideways.
 *
 * Wide and short rather than tall and square, so a list section reads as a different kind of thing
 * from the card sections around it and fits three times as many tracks in the same height. Columns
 * are a fixed width so they snap to a rhythm instead of ending mid-title.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListRow(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: ((HomeItem) -> Unit)?,
) {
    val columns = items.chunked(3)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(columns) { column ->
            Column(
                modifier = Modifier.width(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (item in column) {
                    CompactItem(item, onItemClick, onItemLongPress)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactItem(
    item: HomeItem,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: ((HomeItem) -> Unit)?,
) {
    val colors = LocalExpressiveColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLargeIncreased)
            .background(colors.surface)
            .combinedClickable(
                onClick = { onItemClick(item) },
                onLongClick = onItemLongPress?.let { { it(item) } },
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(ShapeMedium)
                .background(colors.surfaceHigh),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = BodyStrong,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.subtitle?.let {
                Text(
                    it,
                    style = BodyText,
                    color = colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
