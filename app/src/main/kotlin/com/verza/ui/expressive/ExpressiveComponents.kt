package com.verza.ui.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * The pieces every screen outside the player is built from.
 *
 * Three ideas from the spec drive all of them. Selection is expressed by *filling a container*, not
 * by an accent line or a checkmark, so the active thing is the brightest thing. Elevation is
 * expressed by *tone*, using the surface-container ladder, not by drop shadows. And shape carries
 * meaning: a selected item is rounder than an unselected one, which is the cheapest possible use of
 * shape morphing and reads instantly.
 */

/**
 * A section header. The title is set in the italic display serif, because the reference treats
 * section names as editorial moments rather than as labels.
 */
@Composable
fun ExpressiveSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    val colors = LocalExpressiveColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    style = MetaLabel,
                    color = colors.onContainerMuted,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = title,
                style = HeroTitle,
                color = colors.onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onSeeAll != null) {
            ExpressiveControl(
                onClick = onSeeAll,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "See all $title",
                container = colors.surface,
                content = colors.onSurface,
                iconSize = 18.dp,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/**
 * An artwork card for the horizontal rows on Home.
 *
 * The corner radius grows on press. It is a one-line use of shape as feedback, and it is what makes
 * a grid of covers feel like it responds to touch rather than merely registering it.
 */
@Composable
fun ExpressiveCard(
    title: String,
    subtitle: String?,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    width: androidx.compose.ui.unit.Dp = 156.dp,
    aspect: Float = 1f,
) {
    val colors = LocalExpressiveColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val radius by animateDpAsState(
        targetValue = if (pressed) 32.dp else 20.dp,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "cardCorner",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = ExpressiveMotion.spatialDefault(),
        label = "cardScale",
    )

    Column(
        modifier = modifier
            .width(width)
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(radius))
                .background(colors.surface),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = BodyStrong,
            color = colors.onContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = BodyText,
                color = colors.onContainerMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A row in a segmented list.
 *
 * Expressive lists are segmented and rounded rather than a continuous column of square rows, and the
 * selected one is filled. [position] tells the row where it sits in its group so the group reads as
 * one object: only the outer corners of the first and last rows are large.
 */
enum class SegmentPosition { SINGLE, FIRST, MIDDLE, LAST }

@Composable
fun ExpressiveListItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    selected: Boolean = false,
    position: SegmentPosition = SegmentPosition.SINGLE,
) {
    val colors = LocalExpressiveColors.current
    val big = 20.dp
    val small = 6.dp
    val shape = when (position) {
        SegmentPosition.SINGLE -> RoundedCornerShape(big)
        SegmentPosition.FIRST -> RoundedCornerShape(topStart = big, topEnd = big, bottomStart = small, bottomEnd = small)
        SegmentPosition.MIDDLE -> RoundedCornerShape(small)
        SegmentPosition.LAST -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = big, bottomEnd = big)
    }
    // Effects spring: this is a colour, so it must not overshoot.
    val bg by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.surface,
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "rowFill",
    )
    val fg = if (selected) colors.onAccent else colors.onSurface
    val fgMuted = if (selected) colors.onAccent.copy(alpha = 0.7f) else colors.onSurfaceMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            artworkUrl != null -> AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(ShapeMedium).background(colors.surfaceLow),
            )
            leadingIcon != null -> Icon(
                leadingIcon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = BodyStrong,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = BodyText,
                    color = fgMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

/** Work out each row's place in its group, so callers do not have to. */
fun segmentPositionOf(index: Int, count: Int): SegmentPosition = when {
    count <= 1 -> SegmentPosition.SINGLE
    index == 0 -> SegmentPosition.FIRST
    index == count - 1 -> SegmentPosition.LAST
    else -> SegmentPosition.MIDDLE
}

/**
 * A filter chip. Selected chips fill *and* square off slightly — the pill-to-rounded morph the spec
 * calls out, which distinguishes the active one at a glance even in monochrome.
 */
@Composable
fun ExpressiveChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = LocalExpressiveColors.current
    val radius by animateDpAsState(
        targetValue = if (selected) 14.dp else 24.dp,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "chipCorner",
    )
    val bg by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.surface,
        animationSpec = ExpressiveMotion.effectsFast(),
        label = "chipFill",
    )
    val fg = if (selected) colors.onAccent else colors.onSurface

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text = label, style = BodyText, color = fg, maxLines = 1)
    }
}

/** A horizontally scrolling group of chips — Library's tabs, Home's genres. */
@Composable
fun <T> ExpressiveChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: (T) -> ImageVector? = { null },
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option ->
            ExpressiveChip(
                label = label(option),
                selected = option == selected,
                onClick = { onSelect(option) },
                icon = icon(option),
            )
        }
    }
}

/**
 * The big-numeral card from the reference's alarm screen — a large figure, a caption, and a filled
 * container when it is the active one. Used for listening stats.
 */
@Composable
fun ExpressiveStatCard(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    shape: Shape = ShapeExtraLargeIncreased,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalExpressiveColors.current
    val bg = if (highlighted) colors.accent else colors.surface
    val fg = if (highlighted) colors.onAccent else colors.onSurface
    val captionColor = if (highlighted) colors.onAccent.copy(alpha = 0.72f) else colors.onSurfaceMuted

    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(text = value, style = NumeralLarge, color = fg, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(text = caption, style = MetaLabel, color = captionColor, maxLines = 2)
    }
}

/** Label/value metadata in two columns, the "Where / Echo Bridge" block from the reference. */
@Composable
fun ExpressiveMetaGrid(
    pairs: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExpressiveColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        pairs.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MetaLabel, color = colors.onContainerMuted)
                        Text(value, style = MetaValue, color = colors.onContainer)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
