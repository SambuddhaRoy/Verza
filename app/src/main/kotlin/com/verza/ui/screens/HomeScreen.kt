package com.verza.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.HomeSection
import com.verza.ui.components.SectionRow
import com.verza.ui.components.SectionStyle
import com.verza.ui.sleeve.Eyebrow
import com.verza.ui.sleeve.LocalSleeveMode
import com.verza.ui.sleeve.sleeveButton
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.LocalCoverColors
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaShape
import java.util.Calendar

@Composable
fun HomeScreen(
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: (HomeItem) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
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
            is HomeUiState.Content -> HomeContent(s.sections, onItemClick, onItemLongPress, onOpenSettings)
        }
    }
}

@Composable
private fun HomeContent(
    sections: List<HomeSection>,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: (HomeItem) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current

    // Stagger counter — advances one tick per ~40 ms when the sections list arrives.
    // Each section row checks `index < visibleCount` to decide whether it's faded in.
    var visibleCount by remember(sections) { mutableIntStateOf(0) }
    LaunchedEffect(sections) {
        for (i in sections.indices) {
            delay(40)
            visibleCount = i + 1
        }
    }
    // 24 dp downward offset for the fade-up. Converting to px once here avoids LocalDensity
    // lookups inside every item lambda.
    val translateYPx = with(LocalDensity.current) { 24.dp.toPx() }

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
                    if (sleeve) {
                        // Editorial masthead — a wide-tracked mono dateline above a serif title.
                        Eyebrow(text = "${greeting()} · ${dateline()}", color = cover.sub)
                        Spacer(Modifier.height(8.dp))
                    } else {
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
                    }
                    Text(
                        text = "For You",
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onBackground,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = if (sleeve) cover.sub else ext.muted)
                }
            }
        }

        // Decorative genre chip row — visual filter affordance, not wired yet.
        item { GenreChipRow() }

        itemsIndexed(items = sections, key = { _, s -> s.title }) { index, section ->
            // Per-section stagger: each row's `visible` flips on 40 ms after the previous,
            // driven by a single counter at HomeContent scope. Holding the counter at this
            // scope (not per-item) means scrolling away and back doesn't re-trigger — items
            // recycled by LazyColumn read the already-advanced counter and just appear.
            val visible = index < visibleCount
            val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(durationMillis = 280),
                label = "homeRowAlpha",
            )
            val translationY by animateFloatAsState(
                targetValue = if (visible) 0f else translateYPx,
                animationSpec = tween(durationMillis = 320),
                label = "homeRowY",
            )
            Box(
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha
                    this.translationY = translationY
                },
            ) {
                SectionRow(
                    title = section.title,
                    items = section.items,
                    onItemClick = onItemClick,
                    onItemLongPress = onItemLongPress,
                    style = styleFor(section.title),
                )
            }
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
    title.equals("More like your week", ignoreCase = true) -> SectionStyle.LARGE
    title.startsWith("Similar to", ignoreCase = true) -> SectionStyle.LARGE
    title.equals("Browse charts and trending", ignoreCase = true) -> SectionStyle.DENSE_CARDS
    else -> SectionStyle.STANDARD
}

@Composable
private fun GenreChipRow() {
    val colors = MaterialTheme.colorScheme
    val sleeve = LocalSleeveMode.current
    val cover = LocalCoverColors.current
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
            if (sleeve) {
                // Mono filter pills: the active one is ink-filled, the rest use the translucent
                // glass wash (slightly lighter than the background, no outline).
                val base = if (selected) Modifier.clip(VerzaShape).background(cover.ink.copy(alpha = 0.92f))
                           else Modifier.sleeveButton(VerzaShape)
                val fg = if (selected) cover.bg else cover.sub
                Box(
                    modifier = base
                        .clickable(onClick = { active = g })
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        g,
                        style = TextStyle(fontFamily = FontMono, fontSize = 12.5.sp, letterSpacing = 0.02.em),
                        color = fg,
                    )
                }
            } else {
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
}

@Composable
private fun BoxScope.RetryHint(message: String, onRetry: () -> Unit) {
    val ext = LocalVerzaExtendedColors.current
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

/** Weekday name for the Sleeve masthead dateline (e.g. "Saturday"). */
private fun dateline(): String {
    val cal = Calendar.getInstance()
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Sunday"
    }
}
