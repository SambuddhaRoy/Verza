package com.verza.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.data.CuratedMix
import com.verza.data.MixKind
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.HomeSection
import com.verza.ui.expressive.CollageCell
import com.verza.ui.expressive.ExpressiveColors
import com.verza.ui.expressive.ExpressiveControl
import com.verza.ui.expressive.HeroTitle
import com.verza.ui.expressive.LocalExpressiveColors
import com.verza.ui.expressive.MetaLabel
import com.verza.ui.expressive.TitleFonts
import com.verza.ui.expressive.collageSpan
import com.verza.ui.expressive.packCollage
import com.verza.ui.expressive.pickTitleFace
import com.verza.ui.expressive.sizedArt
import com.verza.ui.theme.FontBody
import java.time.LocalDate

/**
 * Home as a collage, the Poster design's take on a feed (see LocalHomeCollage).
 *
 * Nothing is uniform on purpose. Every cover on Home, Verza's own mixes and every shelf of the feed,
 * is packed edge to edge with no gap between them at a size of its own, some huge and most middling,
 * with its name set on the art in one of the display faces on a block of the cover's colours. What
 * would have been gaps are slabs of flat colour, the way a poster wall fills its spaces.
 *
 * The arrangement is fixed for a day (sizes are a hash of the item and the date) so it does not
 * reshuffle under your thumb, and new the next morning.
 */
@Composable
internal fun HomeCollage(
    sections: List<HomeSection>,
    mixes: List<CuratedMix>,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: (HomeItem) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMix: (String) -> Unit,
) {
    val xc = LocalExpressiveColors.current
    val tiles = remember(sections, mixes) { collageTiles(sections, mixes) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // The masthead is a slab like everything below it: the page title set big on the accent.
        Row(
            modifier = Modifier.fillMaxWidth().background(xc.accent).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(LocalDate.now().dayOfWeek.name, style = MetaLabel, color = xc.onAccent)
                Text("FOR YOU", style = HeroTitle.copy(fontSize = 56.sp, lineHeight = 52.sp), color = xc.onAccent)
            }
            ExpressiveControl(
                onClick = onOpenSettings,
                icon = Icons.Outlined.Settings,
                contentDescription = "Settings",
                container = xc.container,
                content = xc.onContainer,
                iconSize = 20.dp,
                modifier = Modifier.size(44.dp),
            )
        }

        if (tiles.isEmpty()) return@Column
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Cells of about 64dp: six across a phone, more on a tablet, so a "2" is still a
            // modest cover and a full-width one is still an event.
            val columns = (maxWidth / 64.dp).toInt().coerceIn(6, 14)
            val cell: Dp = maxWidth / columns
            val seed = remember { LocalDate.now().toEpochDay() }
            val cells = remember(tiles, columns, seed) {
                packCollage(tiles.size, columns) { collageSpan(tiles[it].key, seed, columns, tiles[it].boost) }
            }
            val rows = cells.maxOf { it.row + it.height }
            Box(Modifier.fillMaxWidth().height(cell * rows)) {
                cells.forEach { c ->
                    val mod = Modifier.offset(cell * c.col, cell * c.row).size(cell * c.width, cell * c.height)
                    if (c.isFiller) {
                        Box(mod.background(slabColour(xc, c.col * 31 + c.row * 17 + c.width)))
                    } else {
                        CollageTileView(tiles[c.index], c, cell, mod, onItemClick, onItemLongPress, onOpenMix)
                    }
                }
            }
        }
        // Room for the mini player and nav bar, which sit over the foot of the page.
        Spacer(Modifier.height(16.dp))
    }
}

/** One thing on the collage: a mix or a shelf item, flattened to what a tile needs. */
private data class CollageTile(
    val key: String,
    val name: String,
    val note: String,
    val art: String?,
    val boost: Int,
    val item: HomeItem?,
    val mixId: String?,
)

/**
 * Mixes first and one size up (they are Verza's own, and the reason to open Home), then every shelf
 * dealt out in turn, one item from each, so neighbours come from different shelves rather than the
 * collage reading as the old rows with their margins cut out. Duplicates across shelves are dropped,
 * and the total is capped: past sixty covers it is scrolling for its own sake, and each one is an
 * image decode.
 */
private fun collageTiles(sections: List<HomeSection>, mixes: List<CuratedMix>): List<CollageTile> {
    val out = ArrayList<CollageTile>()
    val seen = HashSet<String>()
    mixes.forEach { m ->
        val note = when (m.kind) {
            MixKind.DAYLIST -> "Daylist"
            MixKind.DISCOVER -> "Discovery"
            MixKind.RELEASE_RADAR -> "New releases"
            MixKind.GENRE -> "Genre"
            MixKind.VIBE -> "Vibe"
        }
        if (seen.add("mix:" + m.id)) out += CollageTile("mix:" + m.id, m.title, note, mixCoverArt(m), 1, null, m.id)
    }
    val queues = sections.map { s -> ArrayDeque(s.items.map { it to s.title }) }
    while (queues.any { it.isNotEmpty() } && out.size < MAX_TILES) {
        for (q in queues) {
            val (item, shelf) = q.removeFirstOrNull() ?: continue
            val key = item.videoId ?: item.browseId ?: item.playlistId ?: (item.title + item.thumbnailUrl)
            if (!seen.add(key)) continue
            out += CollageTile(key, item.title, item.artist.ifBlank { shelf }, item.thumbnailUrl, 0, item, null)
            if (out.size >= MAX_TILES) break
        }
    }
    return out
}

private const val MAX_TILES = 60

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollageTileView(
    tile: CollageTile,
    cell: CollageCell,
    cellSize: Dp,
    modifier: Modifier,
    onItemClick: (HomeItem) -> Unit,
    onItemLongPress: (HomeItem) -> Unit,
    onOpenMix: (String) -> Unit,
) {
    val xc = LocalExpressiveColors.current
    val context = LocalContext.current
    val side = cellSize * cell.width
    val sidePx = with(LocalDensity.current) { side.roundToPx() }
    val h = tile.key.hashCode()

    // The name's face: one of the display faces, chosen per item, skipping any that cannot set it.
    val family = remember(tile.key, tile.name) {
        pickTitleFace(tile.key, TitleFonts.faces.size, avoid = null) { TitleFonts.covers(context, TitleFonts.faces[it], tile.name) }
            ?.let { FontFamily(Font(TitleFonts.faces[it])) }
    }
    val (block, ink) = labelColours(xc, h)
    // Bigger covers get bigger names, so the type scales with the art and a full-width cover
    // carries a headline. Capped well short of the side: at a sixth of a full-width cover the name's
    // block covered a third of the art, and the art is the point.
    val size = (side.value * 0.12f).coerceIn(15f, 44f)

    Box(
        modifier = modifier
            .background(slabColour(xc, h))
            .combinedClickable(
                onClick = { tile.mixId?.let(onOpenMix) ?: tile.item?.let(onItemClick) },
                onLongClick = tile.item?.let { item -> { onItemLongPress(item) } },
            ),
    ) {
        AsyncImage(
            model = sizedArt(tile.art, sidePx),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .align(if (Math.floorMod(h, 3) == 0) Alignment.TopStart else Alignment.BottomStart)
                .widthIn(max = side)
                .background(block)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                text = tile.name,
                style = TextStyle(
                    fontFamily = family ?: FontBody,
                    fontSize = size.sp,
                    lineHeight = (size * 0.98f).sp,
                ),
                color = ink,
                maxLines = if (cell.width >= 4) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (cell.width >= 3 && tile.note.isNotBlank()) {
                Text(
                    text = tile.note.uppercase(),
                    style = TextStyle(fontFamily = FontBody, fontSize = 10.sp, letterSpacing = 0.12.em),
                    color = ink.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The colour of a hole, or of a tile before its art lands. The palette's flat colours, in turn. */
private fun slabColour(xc: ExpressiveColors, h: Int): Color =
    listOf(xc.accent, xc.tertiary, xc.surfaceHighest, xc.container, xc.surface)[Math.floorMod(h, 5)]

/** A name's block and ink: always a pair the palette already measured for contrast. */
private fun labelColours(xc: ExpressiveColors, h: Int): Pair<Color, Color> = when (Math.floorMod(h / 7, 3)) {
    0 -> xc.accent to xc.onAccent
    1 -> xc.tertiary to xc.onTertiary
    else -> xc.container to xc.onContainer
}
