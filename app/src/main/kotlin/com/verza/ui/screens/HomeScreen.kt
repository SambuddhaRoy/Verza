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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import coil3.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.HomeSection
import androidx.compose.foundation.background
import com.verza.ui.expressive.ExpressiveMotion
import com.verza.ui.expressive.ExpressiveControl
import com.verza.ui.expressive.ExpressiveSectionRow
import com.verza.ui.expressive.HeroTitle
import com.verza.ui.expressive.LocalExpressiveColors
import com.verza.ui.expressive.MetaLabel
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
    onOpenMix: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mixes by viewModel.mixes.collectAsStateWithLifecycle()

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
            is HomeUiState.Content -> HomeContent(s.sections, mixes, onItemClick, onItemLongPress, onOpenSettings, onOpenMix)
        }
    }
}

@Composable
private fun HomeContent(
    sections: List<HomeSection>,
    mixes: List<com.verza.data.CuratedMix>,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: (HomeItem) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMix: (String) -> Unit,
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

    val xc = LocalExpressiveColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(xc.container),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            // Masthead: a wide-tracked mono dateline over an italic display serif, which is how the
            // reference treats a page title — as an editorial moment rather than a label.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${greeting()} · ${dateline()}".uppercase(),
                        style = MetaLabel,
                        color = xc.onContainerMuted,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "For You",
                        style = HeroTitle,
                        color = xc.onContainer,
                    )
                }
                ExpressiveControl(
                    onClick = onOpenSettings,
                    icon = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    container = xc.surface,
                    content = xc.onSurface,
                    iconSize = 20.dp,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        // "Made for you" — Verza's on-device curated mixes (Daylist / Discover / Release radar).
        if (mixes.isNotEmpty()) {
            item { MadeForYouRow(mixes = mixes, onOpenMix = onOpenMix) }
        }

        // Decorative genre chip row — visual filter affordance, not wired yet.
        item { GenreChipRow() }

        itemsIndexed(items = sections, key = { _, s -> s.title }) { index, section ->
            // Per-section stagger: each row's `visible` flips on 40 ms after the previous,
            // driven by a single counter at HomeContent scope. Holding the counter at this
            // scope (not per-item) means scrolling away and back doesn't re-trigger — items
            // recycled by LazyColumn read the already-advanced counter and just appear.
            val visible = index < visibleCount
            // Opacity is an effects animation, so it stays critically damped — a row that flickers
            // past full opacity and back looks like a rendering fault.
            val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = ExpressiveMotion.effectsDefault(),
                label = "homeRowAlpha",
            )
            // Position is spatial, so it overshoots slightly and settles. This is the bounce.
            val translationY by animateFloatAsState(
                targetValue = if (visible) 0f else translateYPx,
                animationSpec = ExpressiveMotion.spatialDefault(),
                label = "homeRowY",
            )
            val revealScale by animateFloatAsState(
                targetValue = if (visible) 1f else 0.92f,
                animationSpec = ExpressiveMotion.spatialDefault(),
                label = "homeRowScale",
            )
            Box(
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha
                    this.translationY = translationY
                    this.scaleX = revealScale
                    this.scaleY = revealScale
                },
            ) {
                ExpressiveSectionRow(
                    title = section.title,
                    items = section.items,
                    onItemClick = onItemClick,
                    onItemLongPress = onItemLongPress,
                )
            }
        }
    }
}

@Composable
private fun MadeForYouRow(
    mixes: List<com.verza.data.CuratedMix>,
    onOpenMix: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Made for you",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            mixes.forEach { mix -> MixCard(mix = mix, onClick = { onOpenMix(mix.id) }) }
        }
    }
}

/**
 * The cover for a curated mix: real album art from a song *inside* the mix, washed with the
 * mix kind's signature tint so Daylist/Discovery/Release radar keep their colour identities
 * (and the eyebrow + title stay legible over any artwork). Falls back to the plain gradient
 * while the mix is still being generated.
 */
@Composable
private fun MixCard(mix: com.verza.data.CuratedMix, onClick: () -> Unit) {
    val (top, bottom) = mixGradient(mix.kind)
    val eyebrow = when (mix.kind) {
        com.verza.data.MixKind.DAYLIST -> "DAYLIST"
        com.verza.data.MixKind.DISCOVER -> "DISCOVERY"
        com.verza.data.MixKind.RELEASE_RADAR -> "NEW RELEASES"
    }
    // The first item with art fronts the mix — stable for the life of the generated mix.
    val coverArt = remember(mix.items) { mix.items.firstNotNullOfOrNull { it.thumbnailUrl } }
    Box(
        modifier = Modifier
            .size(154.dp)
            .clip(VerzaShape)
            .background(Brush.linearGradient(listOf(top, bottom)))
            .clickable(onClick = onClick),
    ) {
        if (coverArt != null) {
            AsyncImage(
                model = coverArt,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Kind-tinted scrim: light at the top, deep at the foot where the title sits.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to top.copy(alpha = 0.30f),
                        0.45f to bottom.copy(alpha = 0.35f),
                        1f to bottom.copy(alpha = 0.92f),
                    ),
                ),
            )
        }
        Box(Modifier.fillMaxSize().padding(14.dp)) {
            Text(
                text = eyebrow,
                style = TextStyle(fontFamily = FontMono, fontSize = 10.sp, letterSpacing = 0.12.em),
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.TopStart),
            )
            Text(
                text = mix.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 2,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

/** Distinct vivid gradient per mix kind — playlist-cover identity, independent of the app theme. */
private fun mixGradient(kind: com.verza.data.MixKind): Pair<Color, Color> = when (kind) {
    com.verza.data.MixKind.DAYLIST -> Color(0xFFE0894A) to Color(0xFF6E2F1A)
    com.verza.data.MixKind.DISCOVER -> Color(0xFF6C5CE7) to Color(0xFF241F4D)
    com.verza.data.MixKind.RELEASE_RADAR -> Color(0xFF2FA37C) to Color(0xFF123A30)
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
