package com.verza.ui.expressive

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
import androidx.compose.material.icons.rounded.Search
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
 * The Glass design's bottom chrome: a capsule of tabs with the search button beside it, and the
 * mini player floating above them, all three glass over the page.
 *
 * Scrolling down folds the three into one row, the way the tab bar gets out of the way on an
 * iPhone: the tabs shrink to a circle showing the current one, the mini player drops into the space
 * between, and scrolling back up (or tapping the circle) opens them out again. The chrome keeps its
 * full height while folded, so the pages' bottom padding (LocalBottomChrome) never changes and
 * nothing reflows; the space it frees is see-through and passes touches to the page.
 */
@Composable
fun GlassBottomChrome(
    tabs: List<NavDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    searchRoute: String,
    minimized: Boolean,
    onExpand: () -> Unit,
    mini: MiniPlayerState?,
    modifier: Modifier = Modifier,
) {
    val folded by animateFloatAsState(
        targetValue = if (minimized && mini != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f),
        label = "chromeFold",
    )
    val selected = tabs.indexOfFirst { it.route == currentRoute }
    val haptics = rememberHaptics()
    // Concentric with the screen: the capsule's ends sit as far in from the edge as the display's
    // own corner is rounder than the capsule, so the two curves run parallel the way they do on an
    // iPhone. Phones that do not report a corner get 16dp.
    val view = LocalView.current
    val side = with(LocalDensity.current) {
        val corner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.display?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
        } else 0
        if (corner > 0) (corner.toDp() - TAB_HEIGHT / 2).coerceIn(10.dp, 20.dp) else 16.dp
    }
    // Larger text, up to a point: the capsule's height is fixed, and a label at twice the size
    // would spill out of it. Past 1.3x the tabs' icons carry it.
    val d = LocalDensity.current

    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale.coerceAtMost(1.3f))) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(start = side, end = side, top = 6.dp, bottom = 8.dp)) {
        val tabWidth = maxWidth - TAB_HEIGHT - GAP
        Layout(
            content = {
                GlassTabBar(
                    tabs = tabs,
                    selected = selected,
                    onSelect = { haptics.tick(); onNavigate(tabs[it].route) },
                    folded = folded,
                    onExpand = onExpand,
                    expandedWidth = tabWidth,
                )
                GlassCircleButton(
                    icon = Icons.Rounded.Search,
                    contentDescription = "Search",
                    selected = currentRoute == searchRoute,
                    onClick = { onNavigate(searchRoute) },
                )
                if (mini != null) GlassMiniPlayer(mini, folded)
            },
        ) { measurables, constraints ->
            val w = constraints.maxWidth
            val gap = GAP.roundToPx()
            val tabH = TAB_HEIGHT.roundToPx()
            val miniH = MINI_HEIGHT.roundToPx()
            val foldH = FOLDED_HEIGHT.roundToPx()
            val row2 = if (mini != null) miniH + gap else 0
            val total = row2 + tabH
            fun lerp(a: Int, b: Int) = (a + (b - a) * folded).toInt()
            fun Constraints.exact(width: Int, height: Int) = Constraints.fixed(width.coerceAtLeast(0), height.coerceAtLeast(0))

            val rowH = lerp(tabH, foldH)
            val rowY = lerp(row2, total - foldH)
            val tab = measurables[0].measure(constraints.exact(lerp(w - tabH - gap, foldH), rowH))
            val search = measurables[1].measure(constraints.exact(rowH, rowH))
            val player = measurables.getOrNull(2)?.measure(
                constraints.exact(lerp(w, w - foldH * 2 - gap * 2), lerp(miniH, foldH)),
            )
            layout(w, total) {
                // placeRelative: mirrored in a right-to-left language, search on the left.
                tab.placeRelative(0, rowY)
                search.placeRelative(w - rowH, rowY)
                player?.placeRelative(lerp(0, foldH + gap), lerp(0, total - foldH))
            }
        }
    }
    }
}

/** What the mini player shows, gathered so the chrome can take it as one optional thing. */
class MiniPlayerState(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val onExpand: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
)

private val TAB_HEIGHT = 62.dp
private val MINI_HEIGHT = 58.dp
private val FOLDED_HEIGHT = 54.dp
private val GAP = 10.dp

private val TabLabel = TextStyle(fontFamily = FontBody, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 12.sp)

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

@Composable
private fun GlassTabBar(
    tabs: List<NavDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    folded: Float,
    onExpand: () -> Unit,
    expandedWidth: Dp,
) {
    val colors = LocalExpressiveColors.current
    val dark = colors.container.luminance() < 0.5f
    // The highlight slides between tabs on a spring, and swells under a finger: the one piece of
    // the chrome that behaves like liquid rather than like a button.
    val at by animateFloatAsState(
        targetValue = selected.coerceAtLeast(0).toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "tabHighlight",
    )
    var pressed by remember { mutableIntStateOf(-1) }
    val swell by animateFloatAsState(
        targetValue = if (pressed >= 0) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "tabSwell",
    )
    val highlight by animateFloatAsState(
        targetValue = if (selected >= 0) 1f else 0f,
        animationSpec = spring(stiffness = 700f),
        label = "tabHighlightAlpha",
    )
    val tabsAlpha = (1f - folded * 2f).coerceIn(0f, 1f)
    val iconAlpha = (folded * 2f - 1f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .chromeGlass(CircleShape)
            .then(
                if (folded > 0.5f) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpand,
                ).semantics { contentDescription = "Show tabs" } else Modifier,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (tabsAlpha > 0f) Row(
            modifier = Modifier
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .requiredWidth(expandedWidth)
                .fillMaxHeight()
                .graphicsLayer { alpha = tabsAlpha }
                .padding(4.dp)
                .drawBehind {
                    if (tabs.isEmpty() || highlight <= 0f) return@drawBehind
                    val itemW = size.width / tabs.size
                    val grow = 1f + swell * 0.06f
                    val w = itemW * grow
                    val h = size.height * (1f + swell * 0.04f)
                    // The row lays its tabs out right to left in an RTL language; so does this.
                    val slot = if (layoutDirection == LayoutDirection.Rtl) tabs.size - 1 - at else at
                    drawRoundRect(
                        color = if (dark) Color.White.copy(alpha = 0.13f * highlight) else Color.Black.copy(alpha = 0.07f * highlight),
                        topLeft = Offset(slot * itemW - (w - itemW) / 2f, (size.height - h) / 2f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(h / 2f),
                    )
                },
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
                        .clickable(interactionSource = interaction, indication = null, enabled = folded < 0.5f) { onSelect(i) }
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            this.selected = on
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val tint = if (on) colors.accent else colors.onContainer
                    Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(tab.label, style = TabLabel, color = tint, maxLines = 1)
                }
            }
        }
        if (iconAlpha > 0f) {
            val current = tabs.getOrNull(selected) ?: tabs.firstOrNull()
            if (current != null) Icon(
                current.icon,
                contentDescription = null,
                tint = if (selected >= 0) colors.accent else colors.onContainer,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .graphicsLayer { alpha = iconAlpha },
            )
        }
    }
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

/**
 * The mini player as a glass capsule: cover, title over artist, play and next. Folded into the
 * tab row it keeps all of that, only narrower.
 */
@Composable
fun GlassMiniPlayer(state: MiniPlayerState, folded: Float = 0f, modifier: Modifier = Modifier) {
    val colors = LocalExpressiveColors.current
    val interaction = remember { MutableInteractionSource() }
    val down by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (down) 1.025f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 650f),
        label = "miniSwell",
    )
    Row(
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .chromeGlass(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = state.onExpand)
            .semantics(mergeDescendants = true) { contentDescription = "Now playing: ${state.title}, ${state.artist}" }
            .padding(start = 9.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val art = 40.dp - 4.dp * folded
        AsyncImage(
            model = state.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(art)
                .clip(RoundedCornerShape(art / 2))
                .background(colors.surfaceHigh),
        )
        Spacer(Modifier.width(10.dp))
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
        val haptics = rememberHaptics()
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
