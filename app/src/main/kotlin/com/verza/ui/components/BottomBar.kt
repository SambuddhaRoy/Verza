package com.verza.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.verza.ui.navigation.Screen
import com.verza.ui.sleeve.LocalSleeveMode
import com.verza.ui.theme.FontBody
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.LocalCoverColors
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.verso.drawThread
import com.verza.ui.verso.threadPhase
import kotlin.math.sin

private data class NavItem(val screen: Screen, val label: String)

// Verso navigation is wordmarks on a thread — no icons. The thread is the horizon the app
// rests on; each destination is a node along it.
private val navItems = listOf(
    NavItem(Screen.Home,       "home"),
    NavItem(Screen.Search,     "search"),
    NavItem(Screen.Library,    "library"),
    NavItem(Screen.NowPlaying, "playing"),
)

private const val TWO_PI = (2.0 * Math.PI).toFloat()

/**
 * The Verso "thread horizon" navigation. A continuous undulating thread spans the bottom of the
 * app; the four destinations are nodes resting on it, marked by lowercase wordmarks. A carrier
 * dot glides along the thread to whichever node is active, and the thread's swell deepens while
 * music plays — the bar itself is quietly alive.
 */
@Composable
fun VerzaBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current

    val accent = if (sleeve) cover.accent else colors.primary
    val inactive = if (sleeve) cover.faint else ext.muted
    val activeLabel = if (sleeve) cover.ink else colors.onBackground
    val threadColor = inactive.copy(alpha = 0.45f)

    val selectedIndex = navItems.indexOfFirst { it.screen.route == currentRoute }

    // The carrier dot's slot position — springs along the thread when the tab changes.
    val carrier by animateFloatAsState(
        targetValue = (if (selectedIndex >= 0) selectedIndex else 0).toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "navCarrier",
    )
    // The horizon swells with playback and settles when idle.
    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying) 4.5f else 2.2f,
        animationSpec = tween(900),
        label = "navAmplitude",
    )
    val phase by threadPhase(alive = true)

    // No opaque bar: the glow shows through a soft scrim that deepens toward the bottom edge.
    val scrim = Brush.verticalGradient(
        0f to Color.Transparent,
        0.55f to colors.background.copy(alpha = if (sleeve) 0.30f else 0.82f),
        1f to colors.background.copy(alpha = if (sleeve) 0.42f else 0.94f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(scrim)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(62.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val n = navItems.size
            val cy = size.height * 0.34f
            val ampPx = amplitude.dp.toPx()
            val wavelength = size.width / 2.6f
            fun waveY(x: Float) = cy + ampPx * sin(TWO_PI * x / wavelength + phase)
            drawThread(
                color = threadColor,
                amplitudePx = ampPx,
                wavelengthPx = wavelength,
                strokePx = 1.6.dp.toPx(),
                phase = phase,
                yCenter = cy,
            )
            // Resting node at every destination; the active one is overdrawn by the carrier.
            for (i in 0 until n) {
                val x = size.width / n * (i + 0.5f)
                drawCircle(inactive, radius = 2.4.dp.toPx(), center = Offset(x, waveY(x)))
            }
            // The carrier dot, riding the wave between nodes.
            val cx = size.width / n * (carrier + 0.5f)
            drawCircle(accent.copy(alpha = 0.30f), radius = 8.5.dp.toPx(), center = Offset(cx, waveY(cx)))
            drawCircle(accent, radius = 4.4.dp.toPx(), center = Offset(cx, waveY(cx)))
        }
        Row(Modifier.fillMaxSize()) {
            navItems.forEachIndexed { index, item ->
                val active = index == selectedIndex
                val labelColor by animateColorAsState(
                    targetValue = if (active) activeLabel else inactive,
                    animationSpec = tween(220),
                    label = "navLabel",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // No ripple: the carrier dot gliding over IS the touch feedback.
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onNavigate(item.screen) },
                        ),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        text = if (sleeve) item.label.uppercase() else item.label,
                        color = labelColor,
                        style = if (sleeve)
                            TextStyle(fontFamily = FontMono, fontSize = 8.5.sp, letterSpacing = 0.1.em)
                        else
                            TextStyle(
                                fontFamily = FontBody,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 12.sp,
                                letterSpacing = 0.02.em,
                            ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
        }
    }
}
