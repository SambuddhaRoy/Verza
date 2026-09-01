package com.verza.ui.expressive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * The persistent chrome: the navigation bar and the mini player.
 *
 * Both float as rounded blocks on the canvas rather than sitting behind a divider, so the app reads
 * as one continuous coloured surface instead of a page with a toolbar bolted to the bottom. That is
 * also what removes the last of the old glass chrome, which was the only thing still drawing a
 * near-black band across the foot of every screen.
 */

data class NavDestination(
    val route: String,
    val icon: ImageVector,
    val label: String,
)

/**
 * A floating navigation bar. The selected destination expands into a filled pill carrying its label;
 * the others stay as bare icons.
 *
 * The expansion is a spatial spring, so it overshoots and settles — selection is the single most
 * frequent interaction in the app, and it is where bounce is most worth spending.
 */
@Composable
fun ExpressiveNavBar(
    destinations: List<NavDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExpressiveColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(PillShape)
            .background(colors.surface)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        destinations.forEach { d ->
            NavPill(
                destination = d,
                selected = currentRoute == d.route,
                onClick = { onNavigate(d.route) },
            )
        }
    }
}

@Composable
private fun NavPill(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalExpressiveColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "navPress",
    )
    val bg by animateColorAsState(
        targetValue = if (selected) colors.accent else Color.Transparent,
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "navFill",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.onAccent else colors.onSurfaceMuted,
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "navTint",
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(PillShape)
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics { contentDescription = destination.label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(destination.icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        // The label expands the pill open rather than appearing at full width inside one.
        //
        // It used to be a bare `if (selected)`, with animateContentSize springing the container
        // afterwards — so the text popped in at full size and the pill's hard edge slid across it,
        // which is what read as the label sliding in from a border. Now the label's own width is the
        // animation and the pill is simply the shape around it, so there is one movement instead of
        // two fighting.
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(ExpressiveMotion.spatialDefault(), clip = false) +
                fadeIn(ExpressiveMotion.effectsDefault()),
            exit = shrinkHorizontally(ExpressiveMotion.spatialFast(), clip = false) +
                fadeOut(ExpressiveMotion.effectsFast()),
        ) {
            Text(
                text = destination.label,
                style = BodyStrong,
                color = fg,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * The mini player: a rounded card, not a strip behind a divider.
 *
 * The corner radius grows on press, which is the same shape-as-feedback idiom the cards use, and it
 * doubles as the affordance that this whole thing is one big tap target.
 */
@Composable
fun ExpressiveMiniPlayer(
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    progress: Float,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExpressiveColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val radius by animateDpAsState(
        targetValue = if (pressed) 32.dp else 22.dp,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "miniCorner",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = ExpressiveMotion.spatialDefault(),
        label = "miniScale",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .scale(scale)
            .clip(RoundedCornerShape(radius))
            .background(colors.surface)
            .clickable(interactionSource = interaction, indication = null, onClick = onExpand),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(46.dp).clip(ShapeMedium).background(colors.surfaceLow),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = BodyStrong,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = BodyText,
                    color = colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ExpressiveControl(
                onClick = onTogglePlay,
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                container = colors.accent,
                content = colors.onAccent,
                iconSize = 22.dp,
                modifier = Modifier.size(44.dp),
            )
            ExpressiveControl(
                onClick = onNext,
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next track",
                container = Color.Transparent,
                content = colors.onSurface,
                iconSize = 22.dp,
                modifier = Modifier.size(40.dp),
            )
        }
        // A hairline of progress along the bottom edge, in the accent.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(colors.surfaceLow),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(colors.accent),
            )
        }
    }
}
