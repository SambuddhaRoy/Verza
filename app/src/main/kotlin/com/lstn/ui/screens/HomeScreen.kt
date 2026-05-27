package com.lstn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lstn.innertube.models.HomeItem
import com.lstn.innertube.models.HomeSection
import com.lstn.ui.components.SectionRow
import com.lstn.ui.components.SectionStyle
import com.lstn.ui.theme.LocalLstnExtendedColors
import java.util.Calendar

@Composable
fun HomeScreen(
    onItemClick: (HomeItem) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is HomeUiState.Loading -> CircularProgressIndicator(
                color = colors.primary,
                modifier = Modifier.align(Alignment.Center),
            )
            is HomeUiState.Error -> RetryHint(s.message, onRetry = viewModel::load)
            is HomeUiState.Empty -> Text(
                "Nothing here yet",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted,
                modifier = Modifier.align(Alignment.Center),
            )
            is HomeUiState.Content -> HomeContent(s.sections, onItemClick, onOpenSettings)
        }
    }
}

@Composable
private fun HomeContent(
    sections: List<HomeSection>,
    onItemClick: (HomeItem) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Accent "flourish" bar.
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.primary),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = greeting(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "For You",
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onBackground,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = ext.muted)
                }
            }
        }

        // Decorative genre chip row — visual filter affordance, not wired yet.
        item { GenreChipRow() }

        items(items = sections, key = { it.title }) { section ->
            SectionRow(
                title = section.title,
                items = section.items,
                onItemClick = onItemClick,
                style = styleFor(section.title),
            )
        }
    }
}

/**
 * Spotify-style mixed rhythm — three section sizes spread across the page so it doesn't read as a
 * uniform stack of identical carousels. Falls through to STANDARD for anything unrecognised.
 */
private fun styleFor(title: String): SectionStyle = when {
    title.equals("Recently played", ignoreCase = true) -> SectionStyle.DENSE_GRID
    title.equals("Keep listening", ignoreCase = true) -> SectionStyle.DENSE_GRID
    title.equals("From your liked songs", ignoreCase = true) -> SectionStyle.DENSE_GRID
    title.equals("Your daily discover", ignoreCase = true) -> SectionStyle.LARGE
    title.startsWith("Similar to", ignoreCase = true) -> SectionStyle.LARGE
    title.equals("Browse charts and trending", ignoreCase = true) -> SectionStyle.DENSE_CARDS
    else -> SectionStyle.STANDARD
}

@Composable
private fun GenreChipRow() {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    val genres = listOf("All", "Electronic", "Indie", "Jazz", "Lo-fi", "Ambient", "Classical")
    var active by remember { mutableStateOf(genres.first()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        genres.forEach { g ->
            val selected = g == active
            val bg = if (selected) colors.primary else colors.primaryContainer.copy(alpha = 0.5f)
            val fg = if (selected) colors.onPrimary else colors.primary
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(onClick = { active = g })
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(g, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

@Composable
private fun BoxScope.RetryHint(message: String, onRetry: () -> Unit) {
    val ext = LocalLstnExtendedColors.current
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(100)) { Text("Retry") }
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night"
    }
}
