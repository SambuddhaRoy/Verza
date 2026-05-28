package com.verza.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.verza.ui.navigation.Screen
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

    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
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
                    targetValue = if (active) colors.primary else ext.muted,
                    animationSpec = tween(180),
                    label = "navTint",
                )
                val labelColor by animateColorAsState(
                    targetValue = if (active) colors.onSurface else ext.muted,
                    animationSpec = tween(180),
                    label = "navLabel",
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
                            .background(if (active) colors.primary else androidx.compose.ui.graphics.Color.Transparent),
                    )
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = labelColor,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
