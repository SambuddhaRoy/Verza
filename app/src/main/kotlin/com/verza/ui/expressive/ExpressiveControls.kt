package com.verza.ui.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable

/**
 * The control vocabulary: a filled shape, a glyph, and a spring on press.
 *
 * These are deliberately not M3 Buttons. The reference's play control is a 96dp-tall pill and its
 * shuffle is a scalloped circle — sizes and shapes the stock components fight rather than express,
 * and skinning them back out is more code than drawing them.
 *
 * Every one takes a contentDescription. The old player had icon-only controls with none, which made
 * the whole screen opaque to TalkBack; readability is not only about contrast.
 */
@Composable
private fun pressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = ExpressiveMotion.bouncy(),
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

/** The play/pause control: a wide pill, the largest target on the screen because it is the one you hit most. */
@Composable
fun PlayPill(
    playing: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    ExpressiveControl(
        onClick = onClick,
        icon = icon,
        contentDescription = if (playing) "Pause" else "Play",
        container = container,
        content = content,
        shape = PillShape,
        iconSize = 40.dp,
        modifier = modifier.height(92.dp),
    )
}

/**
 * The bottom pill of secondary actions. One row, always visible, so the things that used to hide
 * behind an overflow menu are simply on screen.
 */
@Composable
fun ExpressiveToolbar(
    items: List<ToolbarItem>,
    colors: ExpressiveColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(colors.elevated)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            ExpressiveControl(
                onClick = item.onClick,
                icon = item.icon,
                contentDescription = item.label,
                container = if (item.active) colors.accent.copy(alpha = 0.22f) else Color.Transparent,
                content = if (item.active) colors.accent else colors.inkMuted,
                shape = CircleShape,
                iconSize = 21.dp,
                modifier = Modifier.size(44.dp),
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

/** A labelled row for the "more" sheet — icon, name, and an optional value on the right. */
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
            .clip(ExpressiveCornerSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) colors.accent else colors.inkMuted,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            color = if (active) colors.accent else colors.ink,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(text = value, color = colors.inkFaint, fontSize = 14.sp)
        }
    }
}

/** Spacer helper so the transport row's gaps stay in one place. */
@Composable
fun ControlGap(width: Dp = 12.dp) {
    Box(modifier = Modifier.width(width))
}
