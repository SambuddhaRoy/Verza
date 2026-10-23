package com.verza.ui.screens

import com.verza.ui.expressive.chromeGlass
import androidx.compose.foundation.layout.padding
import com.verza.ui.expressive.LocalBottomChrome
import com.verza.ui.expressive.PillShape
import com.verza.ui.theme.isGlass
import com.verza.ui.expressive.GlassSectionTitle
import com.verza.ui.components.pressableScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
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
import com.verza.ui.expressive.SectionDensity
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
                modifier = Modifier.align(Alignment.Center).padding(bottom = LocalBottomChrome.current),
            )
            is HomeUiState.Error -> RetryHint(s.message, onRetry = viewModel::load)
            is HomeUiState.Empty -> Text(
                "Nothing here yet",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted,
                modifier = Modifier.align(Alignment.Center).padding(bottom = LocalBottomChrome.current),
            )
            is HomeUiState.Content ->
                HomeContent(s.sections, mixes, onItemClick, onItemLongPress, onOpenSettings, onOpenMix)
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
    val glass = isGlass()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    com.verza.ui.expressive.onTabReselected { listState.animateScrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(xc.container),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp + LocalBottomChrome.current),
        verticalArrangement = Arrangement.spacedBy(if (isGlass()) 22.dp else 28.dp),
    ) {
        if (glass) item {
            // A large title and nothing else: no dateline, no display face.
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassMasthead("${greeting()} · ${dateline()}", "For you", Modifier.weight(1f))
                GlassHeaderButton(Icons.Outlined.Settings, "Settings", onOpenSettings)
            }
        } else item {
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

        // "Made for you": Verza's on-device curated mixes (Daylist / Discover / Release radar).
        val core = mixes.filter { it.kind != com.verza.data.MixKind.GENRE && it.kind != com.verza.data.MixKind.VIBE }
        if (core.isNotEmpty()) {
            item { MadeForYouRow(title = if (glass) "Your mixes" else "Made for you", mixes = core, onOpenMix = onOpenMix) }
        }

        // Playlists by genre and vibe, from the listener's own favourites. These took the place of a
        // row of genre chips that looked like a filter and did nothing.
        val byTaste = mixes.filter { it.kind == com.verza.data.MixKind.GENRE } +
            mixes.filter { it.kind == com.verza.data.MixKind.VIBE }
        if (byTaste.isNotEmpty()) {
            item { MadeForYouRow(title = "Your genres and vibes", mixes = byTaste, onOpenMix = onOpenMix) }
        }

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
                    density = densityFor(index, section),
                )
            }
        }
    }
}

/**
 * How a section should be laid out.
 *
 * By position rather than by content, because the point is to break up the page and the page is
 * what has a position. The first section gets the room, then the pattern alternates so no two
 * neighbours are ever the same shape. Sections too short to fill a list column keep the cards,
 * since three compact rows with one row in them looks like a bug rather than a choice.
 */
private fun densityFor(index: Int, section: HomeSection): SectionDensity {
    if (index == 0) return SectionDensity.FEATURE
    if (section.items.size < 4) return SectionDensity.STANDARD
    return if (index % 2 == 1) SectionDensity.LIST else SectionDensity.STANDARD
}
@Composable
private fun MadeForYouRow(
    title: String,
    mixes: List<com.verza.data.CuratedMix>,
    onOpenMix: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val glass = isGlass()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (glass) 10.dp else 12.dp),
    ) {
        Text(
            text = title,
            style = if (glass) GlassSectionTitle else MaterialTheme.typography.titleLarge,
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
            mixes.forEach { mix ->
                MixTile(mix = mix, glass = glass, onClick = { onOpenMix(mix.id) })
            }
        }
    }
}

/**
 * A mix on Home: its cover ([MixCover]) and nothing else. The kind of mix used to sit on the art in
 * small capitals; the cover's colour and name say it, and the label was noise at that size.
 */
@Composable
private fun MixTile(mix: com.verza.data.CuratedMix, glass: Boolean, onClick: () -> Unit) {
    val side = if (glass) 170.dp else 154.dp
    // The cover is a fixed square, so its name follows the system's text size only up to 1.2x;
    // past that, words longer than the card split mid-word.
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale.coerceAtMost(1.2f))) {
        MixCover(
            mix = mix,
            titleStyle = MixTitleStyle,
            modifier = Modifier
                .size(side)
                .pressableScale(onClick = onClick)
                .clip(VerzaShape)
                // One label, read once: the name is also printed on the cover.
                .clearAndSetSemantics {
                    contentDescription = mix.title
                    role = androidx.compose.ui.semantics.Role.Button
                },
        )
    }
}

/**
 * A round button in a page header: glass where it floats over a recorded page (see
 * LocalGlassBackdrop), otherwise the grey of a grouped surface.
 */
@Composable
internal fun GlassHeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val xc = LocalExpressiveColors.current
    val floating = com.verza.ui.theme.LocalGlassBackdrop.current != null
    Box(
        modifier = Modifier
            .size(38.dp)
            .pressableScale(onClick = onClick)
            .then(
                if (floating) Modifier.chromeGlass(CircleShape)
                else Modifier.clip(CircleShape).background(xc.surface),
            )
            .semantics { contentDescription = label; role = androidx.compose.ui.semantics.Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = xc.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BoxScope.RetryHint(message: String, onRetry: () -> Unit) {
    val ext = LocalVerzaExtendedColors.current
    Column(
        modifier = Modifier.align(Alignment.Center).padding(bottom = LocalBottomChrome.current),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
        OutlinedButton(onClick = onRetry, shape = PillShape) { Text("Retry") }
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

/**
 * A page's heading in Glass: a short line of small capitals in the accent (the time of day on
 * Home, as the Material masthead has it), over the page's name. Verza's dateline rather than a
 * bare large title, and the accent is the cover's, so the heading changes colour with the music.
 */
@Composable
internal fun GlassMasthead(eyebrow: String, title: String, modifier: Modifier = Modifier, titleLines: Int = 1) {
    val xc = LocalExpressiveColors.current
    Column(modifier) {
        Text(
            eyebrow.uppercase(),
            style = TextStyle(
                fontFamily = com.verza.ui.theme.FontBody,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.12.em,
            ),
            color = xc.accent,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(title, style = HeroTitle, color = xc.onContainer, maxLines = titleLines)
    }
}
