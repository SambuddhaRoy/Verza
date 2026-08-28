package com.verza.ui.expressive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        ExpressiveSectionHeader(title = title, subtitle = subtitle, onSeeAll = onSeeAll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items) { index, item ->
                // A 3-step rhythm: two standard cards, then a wider one. Enough to break the grid
                // without making the row feel arbitrary.
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
        Spacer(Modifier.height(22.dp))
    }
}

/**
 * A vertical segmented list of tracks — Library's tab contents, and any "see all".
 *
 * Rows are grouped into one rounded block rather than stacked as separate cards, so a list reads as
 * a single object. [nowPlayingId] fills its row, which is the spec's "selection is a filled
 * container" rule applied to the one row that matters most.
 */
@Composable
fun ExpressiveTrackList(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier,
    nowPlayingId: String? = null,
    trailing: @Composable ((HomeItem) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items.forEachIndexed { index, item ->
            ExpressiveListItem(
                title = item.title,
                subtitle = item.subtitle,
                artworkUrl = item.thumbnailUrl,
                onClick = { onItemClick(item) },
                selected = nowPlayingId != null && item.videoId == nowPlayingId,
                position = segmentPositionOf(index, items.size),
                trailing = trailing?.let { t -> { t(item) } },
            )
        }
    }
}
