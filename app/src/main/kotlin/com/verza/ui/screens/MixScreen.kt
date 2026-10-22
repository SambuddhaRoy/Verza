package com.verza.ui.screens

import com.verza.ui.theme.isGlass
import com.verza.ui.expressive.LocalBottomChrome
import com.verza.ui.expressive.ShapeLargeIncreased
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.verza.data.CuratedMix
import com.verza.data.MixKind
import com.verza.data.MixesRepository
import com.verza.innertube.models.HomeItem
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.LocalVerzaExtendedColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MixViewModel @Inject constructor(
    mixesRepository: MixesRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val mixId: String = savedState.get<String>("mixId").orEmpty()
    val mix: StateFlow<CuratedMix?> = mixesRepository.mixes
        .map { list -> list.firstOrNull { it.id == mixId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mixesRepository.getMix(mixId))
}

@Composable
fun MixScreen(
    onBack: () -> Unit,
    onItemClick: (HomeItem) -> Unit,
    onPlayAll: (List<HomeItem>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MixViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val mix by viewModel.mix.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onBackground)
            }
        }

        val m = mix
        if (m == null) {
            Box(Modifier.fillMaxSize().padding(bottom = LocalBottomChrome.current), contentAlignment = Alignment.Center) {
                Text("Putting this mix together…", style = MaterialTheme.typography.bodyMedium, color = ext.muted)
            }
            return
        }

        val playable = m.playableSongs
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalBottomChrome.current),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                // Banner fronted by real art from inside the mix, washed with the kind tint
                // (same treatment as the Home "Made for you" cards); falls back to the gradient.
                val (top, bottom) = mixGradient(m)
                val bannerArt = remember(m.id, m.items) { mixCoverArt(m) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(ShapeLargeIncreased)
                        .background(Brush.linearGradient(listOf(top, bottom))),
                ) {
                    if (bannerArt != null) {
                        AsyncImage(
                            model = bannerArt,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    0f to top.copy(alpha = 0.25f),
                                    1f to bottom.copy(alpha = 0.92f),
                                ),
                            ),
                        )
                    }
                    Text(
                        m.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(m.subtitle, style = CaptionItalic, color = ext.muted)
                Spacer(Modifier.height(14.dp))
                if (playable.isNotEmpty()) {
                    // Glass: grey with red ink, like an album's Play.
                    val glass = isGlass()
                    Row(
                        modifier = Modifier
                            .clip(if (glass) com.verza.ui.expressive.ShapeLargeIncreased else CircleShape)
                            .background(if (glass) colors.surfaceVariant else colors.primary)
                            .clickable { onPlayAll(playable) }
                            .padding(horizontal = if (glass) 28.dp else 22.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val ink = if (glass) colors.primary else colors.onPrimary
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                        Text("Play", style = MaterialTheme.typography.titleSmall, color = ink)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            items(m.items) { item -> MixRow(item = item, onClick = { onItemClick(item) }) }
        }
    }
}

/** A song in a mix: the same card row as Library and Search. It was bare text on the page, with
 *  its own type, so a mix read as a different app. */
@Composable
private fun MixRow(item: HomeItem, onClick: () -> Unit) =
    com.verza.ui.expressive.ExpressiveListItem(
        title = item.title,
        subtitle = item.subtitle,
        onClick = onClick,
        // Never null: a song with no art still gets the placeholder square, so the titles line up.
        artworkUrl = item.thumbnailUrl ?: "",
    )

/**
 * A mix's signature gradient, shared by its Home card and its page. The three fixed mixes each have
 * one. Genre and vibe mixes vary per listener, so each picks from a set by its id, which keeps a given
 * mix the same colour every time it is generated.
 */
/**
 * The artwork that fronts a mix. Mixes built from the same favourites all opened on the same song, so
 * three cards in a row wore one album cover. Each picks among its first few covers by its id instead,
 * which varies them and keeps any one mix's cover the same between visits.
 */
internal fun mixCoverArt(mix: com.verza.data.CuratedMix): String? {
    val covers = mix.items.mapNotNull { it.thumbnailUrl }.distinct().take(6)
    return covers.getOrNull(Math.floorMod(mix.id.hashCode(), covers.size.coerceAtLeast(1)))
}

internal fun mixGradient(mix: com.verza.data.CuratedMix): Pair<Color, Color> = when (mix.kind) {
    MixKind.DAYLIST -> Color(0xFFE0894A) to Color(0xFF6E2F1A)
    MixKind.DISCOVER -> Color(0xFF6C5CE7) to Color(0xFF241F4D)
    MixKind.RELEASE_RADAR -> Color(0xFF2FA37C) to Color(0xFF123A30)
    MixKind.LISTENBRAINZ -> Color(0xFFEB743B) to Color(0xFF353070)
    MixKind.GENRE, MixKind.VIBE -> TASTE_GRADIENTS[Math.floorMod(mix.id.hashCode(), TASTE_GRADIENTS.size)]
}

private val TASTE_GRADIENTS = listOf(
    Color(0xFFE84A7F) to Color(0xFF5A1530),
    Color(0xFF3FA9F5) to Color(0xFF123A5C),
    Color(0xFFF2B33D) to Color(0xFF5E3B0B),
    Color(0xFF8BC34A) to Color(0xFF26440F),
    Color(0xFFB36AE2) to Color(0xFF3A1650),
    Color(0xFF26C6B9) to Color(0xFF0C4540),
    Color(0xFFFF7043) to Color(0xFF5C1E0C),
)
