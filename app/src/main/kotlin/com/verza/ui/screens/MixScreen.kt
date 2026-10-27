package com.verza.ui.screens

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.foundation.layout.aspectRatio
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
            if (com.verza.ui.theme.isGlass()) {
                Box(Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                    GlassHeaderButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", onBack)
                }
            } else IconButton(onClick = onBack) {
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
        com.verza.ui.expressive.HeaderAndList(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalBottomChrome.current),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            header = { wide ->
                // The same cover as on Home, wide: the picture to the left of nothing, the name on
                // its band across the foot. Square in a tablet's header column, as it is on Home.
                MixCover(
                    mix = m,
                    titleStyle = MixTitleStyle.copy(fontSize = 30.sp, lineHeight = 32.sp),
                    band = if (wide) 0.26f else 0.34f,
                    maxLines = if (wide) 2 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (wide) Modifier.aspectRatio(1f) else Modifier.height(170.dp))
                        .clip(ShapeLargeIncreased),
                )
                Spacer(Modifier.height(10.dp))
                // Italic is an editorial voice; Glass keeps to the one upright face.
                Text(
                    m.subtitle,
                    style = if (com.verza.ui.theme.isGlass()) MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp) else CaptionItalic,
                    color = ext.muted,
                )
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
            },
        ) {
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

/**
 * A curated mix's cover, made the way Spotify and YouTube Music make theirs. A picture from inside
 * the mix, recoloured into the mix's own two colours (a duotone, so every mix of a kind reads as one
 * set however different its songs' covers are), and the name set large on a flat band of the mix's
 * colour below it. No label over the picture and no gradient scrim: the band is what carries the
 * name, so it reads the same on every cover.
 *
 * @param band how much of the height the name's band takes.
 */
@Composable
internal fun MixCover(
    mix: com.verza.data.CuratedMix,
    titleStyle: TextStyle,
    modifier: Modifier = Modifier,
    band: Float = 0.36f,
    maxLines: Int = 2,
) {
    val (vivid, deep) = mixGradient(mix)
    val art = remember(mix.id, mix.items) { mixCoverArt(mix) }
    // Four or more covers make a 2x2 mosaic, the way a playlist's cover is built from its songs. A
    // single picture can be a flat field of colour that the duotone leaves empty; four rarely are.
    val mosaic = remember(mix.id, mix.items) { mix.items.mapNotNull { it.thumbnailUrl }.distinct().take(4).takeIf { it.size == 4 } }
    // Shadows go to the deep colour, highlights to a lifted version of the vivid one, so the
    // picture keeps its shapes and loses its own colours.
    val duotone = remember(vivid, deep) {
        ColorFilter.colorMatrix(duotoneMatrix(deep, androidx.compose.ui.graphics.lerp(vivid, Color.White, 0.3f)))
    }
    val ink = remember(vivid) { com.verza.ui.expressive.readableOn(vivid) }
    Column(modifier.background(deep)) {
        if (mosaic != null) Column(Modifier.fillMaxWidth().weight(1f - band)) {
            for (rowCovers in mosaic.chunked(2)) Row(Modifier.fillMaxWidth().weight(1f)) {
                for (cover in rowCovers) AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = duotone,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        } else Box(Modifier.fillMaxWidth().weight(1f - band)) {
            if (art != null) AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = duotone,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(band)
                .background(vivid)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(mix.title, style = titleStyle, color = ink, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * A colour matrix mapping each pixel's luminance onto the line from [dark] to [light]. The offsets
 * column is in 0..255, as Android's ColorMatrix has it.
 */
private fun duotoneMatrix(dark: Color, light: Color): ColorMatrix {
    fun row(d: Float, l: Float) = floatArrayOf((l - d) * 0.299f, (l - d) * 0.587f, (l - d) * 0.114f, 0f, d * 255f)
    return ColorMatrix(
        row(dark.red, light.red) + row(dark.green, light.green) + row(dark.blue, light.blue) +
            floatArrayOf(0f, 0f, 0f, 1f, 0f),
    )
}

/** The mix's name on its cover: heavy and tight, a lockup rather than a caption. */
internal val MixTitleStyle = TextStyle(
    fontFamily = com.verza.ui.theme.FontDisplayCut,
    fontWeight = FontWeight.Bold,
    fontSize = 17.sp,
    lineHeight = 19.sp,
    letterSpacing = (-0.01).em,
)
