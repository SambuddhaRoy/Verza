package com.verza.ui.expressive

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import android.view.View
import android.view.RoundedCorner
import android.view.HapticFeedbackConstants
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.ui.theme.FontBody
import com.verza.ui.theme.liquidGlass

/**
 * The Glass design's dock: one pane of glass holding the player and the tabs together, divided by
 * a hairline that is also the song's progress. Not a tab bar with a player floated over it: the
 * thing you are listening to and the places you can go are one object at the foot of the screen.
 *
 * The selected tab sits in a soft pool of the accent's light, which slides to the next tab on a
 * spring. Scrolling a page down folds the tabs away and leaves the player, with the current tab's
 * icon at its end to bring them back; scrolling up brings them back too. The dock keeps its full
 * height while folded, so the pages' bottom padding (LocalBottomChrome) never changes and nothing
 * reflows; the space it frees is see-through and passes touches to the page.
 */
@Composable
fun GlassDock(
    tabs: List<NavDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    minimized: Boolean,
    onExpand: () -> Unit,
    mini: MiniPlayerState?,
    modifier: Modifier = Modifier,
) {
    val folded by animateFloatAsState(
        targetValue = if (minimized && mini != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f),
        label = "dockFold",
    )
    val selected = tabs.indexOfFirst { it.route == currentRoute }
    val haptics = rememberHaptics()
    val colors = LocalExpressiveColors.current
    // Concentric with the screen: the dock sits as far in from the edge as the display's own corner
    // is rounder than the dock's, so the two curves run parallel. 12dp where no corner is reported.
    val view = LocalView.current
    val side = with(LocalDensity.current) {
        val corner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.display?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
        } else 0
        if (corner > 0) (corner.toDp() - DOCK_RADIUS).coerceIn(8.dp, 18.dp) else 12.dp
    }
    // Larger text, up to a point: the dock's rows are a fixed height. Past 1.3x the icons carry it.
    val d = LocalDensity.current
    val full = (if (mini != null) MINI_ROW else 0.dp) + TABS_ROW

    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale.coerceAtMost(1.3f))) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = side, end = side, top = 6.dp, bottom = 8.dp)
                .height(full),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(Modifier.fillMaxWidth().chromeGlass(DockShape)) {
                if (mini != null) {
                    MiniRow(
                        state = mini,
                        tabIcon = tabs.getOrNull(selected)?.icon ?: tabs.firstOrNull()?.icon,
                        folded = folded,
                        onExpand = onExpand,
                        modifier = Modifier.fillMaxWidth().height(MINI_ROW),
                    )
                    // The divider is the progress: the dock's one line, doing two jobs.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(2.dp)
                            .drawBehind {
                                val h = size.height
                                drawRoundRect(colors.line, size = size, cornerRadius = CornerRadius(h / 2f))
                                drawRoundRect(
                                    colors.accent,
                                    size = Size(size.width * mini.progress.coerceIn(0f, 1f), h),
                                    cornerRadius = CornerRadius(h / 2f),
                                )
                            },
                    )
                }
                val tabsHeight = TABS_ROW * (1f - folded)
                if (tabsHeight > 0.5.dp) DockTabs(
                    tabs = tabs,
                    selected = selected,
                    onSelect = { haptics.tick(); onNavigate(tabs[it].route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tabsHeight)
                        .clipToBounds()
                        .graphicsLayer { alpha = (1f - folded * 1.6f).coerceIn(0f, 1f) },
                )
            }
        }
    }
}

/** What the mini player shows, gathered so the dock can take it as one optional thing. */
class MiniPlayerState(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val progress: Float,
    val onExpand: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
)

private val DOCK_RADIUS = 26.dp
private val DockShape = SquircleShape(DOCK_RADIUS)
private val MINI_ROW = 62.dp
private val TABS_ROW = 58.dp

private val TabLabel = TextStyle(fontFamily = FontBody, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 12.sp)

/**
 * The tabs, in the dock's lower row. The selected one is lit from behind by a soft pool of the
 * accent, as if the colour were shining up through the glass, and the pool slides between tabs on
 * a spring and swells under a finger.
 */
@Composable
private fun DockTabs(
    tabs: List<NavDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    val colors = LocalExpressiveColors.current
    val at by animateFloatAsState(
        targetValue = selected.coerceAtLeast(0).toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "tabLight",
    )
    var pressed by remember { mutableIntStateOf(-1) }
    val swell by animateFloatAsState(
        targetValue = if (pressed >= 0) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "tabSwell",
    )
    val lit by animateFloatAsState(
        targetValue = if (selected >= 0) 1f else 0f,
        animationSpec = spring(stiffness = 700f),
        label = "tabLit",
    )
    Row(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .drawBehind {
                if (tabs.isEmpty() || lit <= 0f) return@drawBehind
                val itemW = size.width / tabs.size
                // The row lays its tabs out right to left in an RTL language; so does this.
                val slot = if (layoutDirection == LayoutDirection.Rtl) tabs.size - 1 - at else at
                val center = Offset(slot * itemW + itemW / 2f, size.height * 0.42f)
                val r = 30.dp.toPx() * (1f + swell * 0.12f)
                drawCircle(
                    Brush.radialGradient(
                        0f to colors.accent.copy(alpha = 0.38f * lit),
                        0.55f to colors.accent.copy(alpha = 0.13f * lit),
                        1f to Color.Transparent,
                        center = center,
                        radius = r,
                    ),
                    radius = r,
                    center = center,
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { i, tab ->
            val interaction = remember { MutableInteractionSource() }
            val down by interaction.collectIsPressedAsState()
            LaunchedEffect(down) { if (down) pressed = i else if (pressed == i) pressed = -1 }
            val on = i == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource = interaction, indication = null) { onSelect(i) }
                    .semantics(mergeDescendants = true) {
                        role = Role.Tab
                        this.selected = on
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val tint = if (on) colors.accent else colors.onContainerMuted
                Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(3.dp))
                Text(tab.label, style = TabLabel, color = if (on) colors.onContainer else colors.onContainerMuted, maxLines = 1)
            }
        }
    }
}

/**
 * Glass for the chrome, tuned per mode: smoky in dark, milky in light. Light mode also gets a soft
 * shadow, since a white pane on a white page has no edge otherwise.
 */
@Composable
fun Modifier.chromeGlass(shape: Shape): Modifier {
    val c = LocalExpressiveColors.current
    val dark = c.container.luminance() < 0.5f
    return this
        .shadow(
            elevation = if (dark) 0.dp else 18.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.10f),
            spotColor = Color.Black.copy(alpha = 0.16f),
        )
        .liquidGlass(
            shape = shape,
            tint = if (dark) Color.Black.copy(alpha = 0.46f) else Color.White.copy(alpha = 0.62f),
            fallback = if (dark) Color(0xF21C1C1E) else Color(0xF5F9F9FB),
            base = c.container,
            rim = if (dark) Color.White.copy(alpha = 0.75f) else Color.White,
            edge = if (dark) Color.Transparent else Color.Black.copy(alpha = 0.07f),
        )
}

/** A round glass button: search beside the tabs, and the buttons over Now Playing's backdrop. */
@Composable
fun GlassCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color = if (selected) LocalExpressiveColors.current.accent else LocalExpressiveColors.current.onContainer,
    glass: @Composable Modifier.(Shape) -> Modifier = { chromeGlass(it) },
    iconSize: Dp = 24.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val down by interaction.collectIsPressedAsState()
    // Glass swells when pressed rather than sinking: it is lit from inside, not pushed in.
    val scale by animateFloatAsState(
        targetValue = if (down) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "glassButton",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .glass(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** The mini player on its own pane, for when the tabs are a rail down the side. */
@Composable
fun GlassMiniPlayer(state: MiniPlayerState, modifier: Modifier = Modifier) {
    Box(modifier.chromeGlass(DockShape)) {
        MiniRow(state, tabIcon = null, folded = 0f, onExpand = {}, modifier = Modifier.fillMaxSize())
    }
}

/**
 * The player's row: cover, title over artist, play and next. Folded, next gives way to the current
 * tab's icon, the way back to the tabs.
 */
@Composable
private fun MiniRow(
    state: MiniPlayerState,
    tabIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    folded: Float,
    onExpand: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalExpressiveColors.current
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null, onClick = state.onExpand)
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).semantics(mergeDescendants = true) {
                contentDescription = "Now playing: ${state.title}, ${state.artist}"
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = state.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(SquircleShape(11.dp))
                    .background(colors.surfaceHigh),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.title,
                    style = TextStyle(fontFamily = FontBody, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
                    color = colors.onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.artist.isNotBlank()) Text(
                    state.artist,
                    style = TextStyle(fontFamily = FontBody, fontSize = 13.sp, lineHeight = 16.sp),
                    color = colors.onContainerMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        PlainIconButton(
            icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (state.isPlaying) "Pause" else "Play",
            onClick = { haptics.click(); state.onTogglePlay() },
            tint = colors.onContainer,
        )
        if (folded < 0.5f) PlainIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next track",
            onClick = state.onNext,
            tint = colors.onContainer,
            modifier = Modifier.graphicsLayer { alpha = 1f - folded * 2f },
        ) else if (tabIcon != null) PlainIconButton(
            icon = tabIcon,
            contentDescription = "Show tabs",
            onClick = onExpand,
            tint = colors.accent,
            iconSize = 22.dp,
            modifier = Modifier.graphicsLayer { alpha = folded * 2f - 1f },
        )
    }
}

/**
 * A bare glyph that shows a soft disc while pressed: how buttons look on glass, where a filled
 * button would cover the very thing the glass is for.
 */
@Composable
fun PlainIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 26.dp,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val down by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (down) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "plainPress",
    )
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                if (press > 0f) drawCircle(tint.copy(alpha = 0.14f * press), radius = this.size.minDimension / 2f * (0.7f + 0.3f * press))
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(iconSize).scale(1f - 0.12f * press),
        )
    }
}

/**
 * Touch you can feel, used the way iOS uses it: sparingly, and only where something physical
 * happened. A tick for a change of selection or a toggle, a firmer click for play and pause, and a
 * distinct bump when a drag crosses the point where letting go will act (close, skip). Everything
 * else stays silent, because haptics on every tap stop meaning anything. The system's own haptics
 * setting is honoured by performHapticFeedback.
 */
class Haptics(private val view: View) {
    fun tick() { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    fun click() { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) }
    fun threshold() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
            else HapticFeedbackConstants.CONTEXT_CLICK,
        )
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
