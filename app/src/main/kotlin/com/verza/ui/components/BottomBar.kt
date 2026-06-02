package com.verza.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.verza.ui.navigation.Screen
import com.verza.ui.sleeve.LocalSleeveMode
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.LocalCoverColors
import com.verza.ui.theme.LocalVerzaExtendedColors

private data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
)

private val navItems = listOf(
    NavItem(Screen.Home,       Icons.Outlined.Home,         "Home"),
    NavItem(Screen.Search,     Icons.Outlined.Search,       "Search"),
    NavItem(Screen.Library,    Icons.Outlined.LibraryMusic, "Library"),
    NavItem(Screen.NowPlaying, Icons.Outlined.MusicNote,    "Now Playing"),
)

@Composable
fun VerzaBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current

    // Sleeve dresses the nav as a translucent dark bar (so the reactive glow shows through),
    // with the cover accent marking the active tab.
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
    val activeColor = if (sleeve) cover.accent else colors.primary
    val inactiveColor = if (sleeve) cover.faint else ext.muted
    val labelActiveColor = if (sleeve) cover.ink else colors.onSurface
    // Sleeve dresses the nav as translucent "glass" — the same low-opacity white wash the cards
    // and mini-player use (sleeveSurface) — so the live reactive glow shows through it.
    val barBackground = if (sleeve) {
        Modifier.background(Color.White.copy(alpha = 0.06f))
    } else {
        Modifier.background(colors.surface)
    }
    val dividerColor = if (sleeve) Color.White.copy(alpha = 0.12f) else colors.outlineVariant

    Column(modifier = modifier.fillMaxWidth().then(barBackground)) {
        HorizontalDivider(thickness = 1.dp, color = dividerColor)
        Row(
            // windowInsetsPadding *before* height — so the 72 dp content sits ABOVE the system
            // gesture inset instead of being eaten by it. Otherwise the label clips on devices
            // with a tall navigation bar.
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(72.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navItems.forEach { item ->
                val active = currentRoute == item.screen.route
                val tint by animateColorAsState(
                    targetValue = if (active) activeColor else inactiveColor,
                    animationSpec = tween(180),
                    label = "navTint",
                )
                val labelColor by animateColorAsState(
                    targetValue = if (active) labelActiveColor else inactiveColor,
                    animationSpec = tween(180),
                    label = "navLabel",
                )
                // Active-state icon spring: the selected icon scales up to 1.10× via a
                // medium-bouncy spring, so tapping a tab gives kinetic confirmation on top
                // of the colour change. Inactive icons stay at 1.0× (no return spring).
                val iconScale by animateFloatAsState(
                    targetValue = if (active) 1.10f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "navIconScale",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = { onNavigate(item.screen) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Active indicator: 3 dp accent bar at the top of the item.
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (active) activeColor else Color.Transparent),
                    )
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                    )
                    Spacer(Modifier.height(if (sleeve) 4.dp else 2.dp))
                    Text(
                        text = if (sleeve) item.label.uppercase() else item.label,
                        color = labelColor,
                        style = if (sleeve)
                            TextStyle(fontFamily = FontMono, fontSize = 8.5.sp, letterSpacing = 0.1.em)
                        else
                            MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
