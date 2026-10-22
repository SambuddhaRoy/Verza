package com.verza.ui.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The control vocabulary: a filled shape, a glyph or a word, and a spring on press.
 *
 * Not M3 Buttons. The reference's play control is a ~90dp labelled pill and its shuffle is a
 * scalloped circle — sizes and shapes the stock components fight rather than express, so skinning
 * them back out would be more code than drawing them.
 *
 * Every one takes a contentDescription. The screens these replace had icon-only rows with none,
 * which made the player opaque to TalkBack; legibility is not only a contrast problem.
 */
@Composable
private fun pressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        // Spatial: this moves something, so it is allowed to overshoot on release.
        animationSpec = ExpressiveMotion.spatialDefault(),
        label = "pressScale",
    )
    return scale
}

/** A filled control of any shape. The primitive the rest are built from. */
@Composable
fun ExpressiveControl(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    iconSize: Dp = 28.dp,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(if (enabled) container else container.copy(alpha = 0.4f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
    }
}

/**
 * The play control: a wide pill with a word in it, not an icon.
 *
 * Spelling it out is the reference's choice and it is a good one — it is the only control on the
 * screen you look for rather than glance at, and a word is faster to find than a glyph among a row
 * of other glyphs.
 */
@Composable
fun PlayPill(
    playing: Boolean,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    val label = if (playing) "PAUSE" else "PLAY"
    Box(
        modifier = modifier
            .scale(scale)
            .clip(PillShape)
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = if (playing) "Pause" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = PillLabel, color = content)
    }
}

/**
 * The row of secondary actions. One row, always visible, so what used to hide behind an overflow
 * menu is simply on screen.
 */
@Composable
fun ExpressiveToolbar(
    items: List<ToolbarItem>,
    colors: ExpressiveColors,
    modifier: Modifier = Modifier,
    /** Spread the items across the full width rather than packing them at the start. */
    spread: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(colors.surface)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = if (spread) Arrangement.SpaceEvenly else Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            ExpressiveControl(
                onClick = item.onClick,
                icon = item.icon,
                contentDescription = item.label,
                container = if (item.active) colors.accent else Color.Transparent,
                content = if (item.active) colors.onAccent else colors.onSurface,
                shape = CircleShape,
                iconSize = 21.dp,
                modifier = Modifier.size(46.dp),
            )
        }
    }
}

data class ToolbarItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val active: Boolean = false,
)

/** A labelled row for a sheet — icon, name, optional value on the right. */
@Composable
fun ExpressiveSheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    colors: ExpressiveColors,
    modifier: Modifier = Modifier,
    value: String? = null,
    active: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(ShapeMedium)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) colors.accent else colors.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = BodyText,
            color = if (active) colors.accent else colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (value != null) Text(text = value, style = Timecode, color = colors.onSurface)
    }
}
