package com.verza.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Scale-on-press feedback for any tappable surface. Use [pressableScale] as a drop-in
 * replacement for `.clickable { … }` on cards, song rows, theme tiles, etc. — the
 * resulting composition presses to 0.97× with a soft spring and rebounds on release.
 *
 * The scale uses `graphicsLayer`, which does not affect layout, so touch targets stay
 * the un-scaled size. The ripple is the system default (`LocalIndication`) so the visual
 * feedback matches the user's launcher / theme.
 *
 * Choosing 0.97× rather than the more common 0.95× because Verza's editorial mood reads
 * better with restrained kinetics — a sharper press would feel arcade-y.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressableScale(
    pressedScale: Float = 0.97f,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressableScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        // combinedClickable adds the long-press gesture (with the system long-press haptic) while
        // still feeding press interactions to [interactionSource] so the scale animation works.
        .combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick,
        )
}
