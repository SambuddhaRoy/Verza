package com.verza.ui.expressive

import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.ui.text.TextStyle
import com.verza.ui.theme.isGlass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
    if (isGlass()) {
        // "Title ›": the whole line is the link, the way a shelf heading works on Apple Music.
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(if (onSeeAll != null) Modifier.clickable(onClick = onSeeAll) else Modifier)
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                if (subtitle != null) Text(subtitle, style = BodyText.copy(fontSize = 13.sp), color = colors.onContainerMuted)
                Text(title, style = GlassSectionTitle, color = colors.onContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (onSeeAll != null) Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onContainerMuted,
                modifier = Modifier.size(24.dp),
            )
        }
        return
    }
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
    val glass = isGlass()
    val radius by animateDpAsState(
        targetValue = if (glass) 10.dp else if (pressed) 32.dp else 20.dp,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "cardCorner",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) (if (glass) 0.97f else 0.96f) else 1f,
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
        Spacer(Modifier.height(if (glass) 7.dp else 8.dp))
        Text(
            text = title,
            style = if (glass) GlassCaption else BodyStrong,
            color = colors.onContainer,
            maxLines = if (glass) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = if (glass) GlassCaption.copy(fontWeight = FontWeight.Normal) else BodyText,
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
    if (isGlass()) {
        GlassListRow(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            modifier = modifier,
            artworkUrl = artworkUrl,
            leadingIcon = leadingIcon,
            trailing = trailing,
            selected = selected,
            divider = position != SegmentPosition.LAST,
        )
        return
    }
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
            RowTitle(title, fg)
            if (!subtitle.isNullOrBlank()) RowSubtitle(subtitle, fgMuted)
        }
        trailing?.invoke()
    }
}

/**
 * A list row's title, set in a face of its own, chosen by the title so it is the same each time: a
 * list that reads as a run of names rather than as rows of a table. Bigger in Poster, where the row
 * is a slab; a step up from body size in Material, because a condensed display face at body size
 * reads smaller than the Inter it replaces.
 */
@Composable
fun RowTitle(text: String, color: Color, style: TextStyle = BodyStrong) {
    if (isGlass()) {
        // One face, at reading size: a list of names in Glass is a list, not a specimen sheet.
        Text(
            text = text,
            style = style.copy(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    val size = 19.sp
    Text(
        text = text,
        style = style.copy(fontSize = size, lineHeight = size * 1.05f).variedFace(text),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A list row's second line: small tracked capitals in the Poster design, as it was in Material. */
@Composable
fun RowSubtitle(text: String, color: Color, style: TextStyle = BodyText) {
    Text(
        text = text,
        style = if (isGlass()) style.copy(fontSize = 14.sp, lineHeight = 18.sp) else style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Card captions in Glass: one line each, the title a weight up from the subtitle. */
private val GlassCaption = TextStyle(
    fontFamily = com.verza.ui.theme.FontBody,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp,
)

/**
 * A list row in the Glass design: no tile, just the picture, the two lines and a hairline under
 * the text, the way a list is drawn on Apple Music. The playing row's title takes the accent.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GlassListRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    leadingIcon: ImageVector? = null,
    circularArt: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    selected: Boolean = false,
    divider: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = LocalExpressiveColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            artworkUrl != null -> AsyncImage(
                model = artworkUrl.ifEmpty { null },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .size(50.dp)
                    .clip(if (circularArt) PillShape else ShapeMedium)
                    .background(colors.surfaceHigh),
            )
            leadingIcon != null -> Box(Modifier.padding(vertical = 6.dp).size(50.dp), contentAlignment = Alignment.Center) {
                Icon(leadingIcon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 62.dp)
                .drawBehind {
                    if (divider) drawLine(
                        colors.line,
                        Offset(0f, size.height - 0.5f),
                        Offset(size.width, size.height - 0.5f),
                        strokeWidth = 1f,
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                RowTitle(title, if (selected) colors.accent else colors.onContainer)
                if (!subtitle.isNullOrBlank()) RowSubtitle(subtitle, colors.onContainerMuted)
            }
            trailing?.invoke()
        }
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
    val glass = isGlass()
    val radius by animateDpAsState(
        targetValue = if (glass) 50.dp else if (selected) 14.dp else 24.dp,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "chipCorner",
    )
    val bg by animateColorAsState(
        targetValue = when {
            glass && selected -> colors.onContainer
            selected -> colors.accent
            else -> colors.surface
        },
        animationSpec = ExpressiveMotion.effectsFast(),
        label = "chipFill",
    )
    val fg = when {
        glass && selected -> colors.container
        selected -> colors.onAccent
        else -> colors.onSurface
    }

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
 * An empty or failed state in the Glass design: a large, quiet symbol over one line of text, centred,
 * the way iOS says there is nothing here. Says what to do where it can; never a bare "Empty".
 */
@Composable
fun GlassEmpty(text: String, icon: ImageVector = androidx.compose.material.icons.Icons.Rounded.MusicNote) {
    val colors = LocalExpressiveColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 40.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.onContainerMuted.copy(alpha = 0.55f), modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(14.dp))
        Text(
            text,
            style = BodyText.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
            color = colors.onContainerMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
