package com.verza.ui.page

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.ui.kit.GlassButton
import com.verza.ui.kit.Glassed
import com.verza.ui.kit.Hairline
import com.verza.ui.theme.artCorner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.TextButton
import com.verza.ui.kit.MenuItem
import com.verza.ui.theme.Corner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.verza.Graph
import com.verza.data.Album
import com.verza.data.Artist
import com.verza.data.Section
import com.verza.data.Song
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.kit.Cover
import com.verza.ui.kit.Empty
import com.verza.ui.kit.ListRow
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.SectionTitle
import com.verza.ui.kit.SongMenu
import com.verza.ui.kit.Word
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

/** An album or playlist, an artist, or a failure; null while loading. */
sealed interface PageContent {
    data class OfAlbum(val album: Album) : PageContent
    data class OfArtist(val artist: Artist) : PageContent
    data class Failed(val message: String) : PageContent
}

class PageModel(graph: Graph, id: String) : ViewModel() {
    private val _content = MutableStateFlow<PageContent?>(null)
    val content: StateFlow<PageContent?> = _content.asStateFlow()

    init {
        viewModelScope.launch {
            // "local:" is an album or folder on the phone, "pl:" a playlist made in Verza, a channel id an artist, anything else an album or a
            // YouTube playlist.
            val pages: Flow<PageContent> = when {
                id.startsWith("local:") -> kotlinx.coroutines.flow.flow { graph.phone.collection(id)?.let { emit(PageContent.OfAlbum(it)) } ?: emit(PageContent.Failed("Couldn't find that on this phone")) }
                id.startsWith("mix:") -> kotlinx.coroutines.flow.flow {
                    graph.mixes.getMix(id.removePrefix("mix:"))?.let { m ->
                        emit(PageContent.OfAlbum(Album(m.title, m.subtitle, m.items.firstOrNull()?.thumbnailUrl, m.items.mapNotNull { it.toCard().song })))
                    } ?: emit(PageContent.Failed("This mix has been remade; open it again from Home"))
                }
                id.startsWith("pl:") -> graph.library.playlist(id.removePrefix("pl:").toLong()).map { PageContent.OfAlbum(it) }
                id.startsWith("UC") -> graph.saved.load(id, Artist.serializer()) { graph.youtube.artist(id) }.map { PageContent.OfArtist(it) }
                else -> graph.saved.load(id, Album.serializer()) { graph.youtube.album(id) }.map { PageContent.OfAlbum(it) }
            }
            pages.catch { e -> emit(PageContent.Failed(if (e is IOException) "You're offline, and this page hasn't been opened before" else "Couldn't open this")) }
                .collect { _content.value = it }
        }
    }
}

/** An album's, a playlist's or an artist's page, with the way back and its actions in glass over its top. */
@Composable
fun PageScreen(id: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val graph = LocalContext.current.graph
    val model: PageModel = viewModel(key = id, factory = viewModelFactory { initializer { PageModel(graph, id) } })
    val content by model.content.collectAsState()
    val p = LocalPalette.current
    val downloaded by graph.library.downloadedIds.collectAsState()
    Glassed(
        ground = p.canvas,
        modifier = Modifier.fillMaxSize(),
        content = {
            when (val c = content) {
                null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = p.accent)
                is PageContent.Failed -> Empty(c.message)
                is PageContent.OfAlbum -> AlbumPage(c.album, playlist = id.removePrefix("pl:").takeIf { id.startsWith("pl:") }?.toLongOrNull())
                is PageContent.OfArtist -> ArtistPage(c.artist, onOpen)
            }
        },
        glass = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
                Spacer(Modifier.weight(1f))
                (content as? PageContent.OfAlbum)?.album?.let { album ->
                    if (album.songs.any { !it.onPhone && it.id !in downloaded }) GlassButton(Icons.Rounded.Download, "Download") { graph.downloads.save(album.songs) }
                    if (id.startsWith("pl:")) PlaylistMenu(id.removePrefix("pl:").toLong(), album.title, onDeleted = onBack)
                }
            }
        },
    )
}

/** A playlist made in Verza: renamed or deleted from the ⋯ over its top corner. */
@Composable
private fun PlaylistMenu(id: Long, name: String, onDeleted: () -> Unit) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf<String?>(null) }   // "rename" or "delete"
    Box {
        GlassButton(Icons.Rounded.MoreHoriz, "More") { shown = true }
        DropdownMenu(expanded = shown, onDismissRequest = { shown = false }, shape = Corner) {
            MenuItem("Rename") { shown = false; asking = "rename" }
            MenuItem("Delete playlist") { shown = false; asking = "delete" }
        }
    }
    val done = { asking = null }
    when (asking) {
        "rename" -> {
            var text by remember { mutableStateOf(name) }
            AlertDialog(
                onDismissRequest = done,
                title = { Text("Rename playlist") },
                text = { com.verza.ui.kit.Field(text, { text = it }, placeholder = "Name") },
                confirmButton = { TextButton(enabled = text.isNotBlank() && text.trim() != name, onClick = { scope.launch { graph.library.renamePlaylist(id, text.trim()); graph.sync.flush() }; done() }) { Text("Rename") } },
                dismissButton = { TextButton(onClick = done) { Text("Cancel") } },
            )
        }
        "delete" -> AlertDialog(
            onDismissRequest = done,
            title = { Text("Delete \"$name\"?") },
            text = { Text("The songs stay in your library; only the playlist goes, here and on YouTube Music.") },
            confirmButton = { TextButton(onClick = { done(); onDeleted(); graph.scope.launch { graph.library.deletePlaylist(id); graph.sync.flush() } }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = done) { Text("Cancel") } },
        )
    }
}

/** The cover, centred, the name and who made it, Play and Shuffle side by side, then the songs in order. */
@Composable
private fun AlbumPage(album: Album, playlist: Long?) {
    val p = LocalPalette.current
    val graph = LocalContext.current.graph
    val player = graph.player
    val now by player.state.collectAsState()
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 60.dp, bottom = 16.dp + LocalDockHeight.current)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // The cover lights the page round it: a blur of itself behind it, where Apple puts a grey
                // shadow, as the desktop's Now Playing glows. Android 12 blurs; before it, the shadow.
                val glows = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth(0.72f).widthIn(max = 320.dp).aspectRatio(1f)) {
                    if (glows) Box(
                        Modifier.align(Alignment.Center).requiredSize(maxWidth + 120.dp)
                            .graphicsLayer { translationY = 20.dp.toPx(); alpha = if (p.dark) 0.8f else 0.65f }
                            .blur(44.dp, BlurredEdgeTreatment.Unbounded),
                    ) { AsyncImage(album.art?.let { artAt(it, 226) }, null, contentScale = ContentScale.Crop, modifier = Modifier.padding(60.dp).fillMaxSize()) }
                    val shape = artCorner(300.dp)
                    Cover(
                        album.art?.let { artAt(it, 1200) }, 0.dp,
                        Modifier.matchParentSize().then(if (glows) Modifier else Modifier.shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.35f))),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(album.title, style = Type.Section, color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                // "Coldplay • 2002": who in the accent, as Apple Music has it, and the rest quietly under.
                val parts = album.subtitle.split(Regex("\\s*[•·]\\s*")).filter { it.isNotBlank() }
                val who = parts.firstOrNull { it !in Kinds && it.toIntOrNull() == null }.orEmpty()
                val rest = parts.filter { it != who }.joinToString(" · ")
                if (who.isNotBlank()) Text(who, style = Type.RowTitle.copy(fontSize = 19.sp, lineHeight = 24.sp), color = p.accent, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text(listOf(rest, songsLabel(album.songs)).filter { it.isNotBlank() }.joinToString(" · "), style = Type.Small, color = p.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Word("Play", p.accent, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow, enabled = album.songs.isNotEmpty()) { player.play(album.songs) }
                    Word("Shuffle", p.accent, Modifier.weight(1f), icon = Icons.Rounded.Shuffle, enabled = album.songs.isNotEmpty()) { player.shuffle(album.songs) }
                }
            }
        }
        item { Hairline() }
        itemsIndexed(album.songs, key = { i, s -> "$i${s.id}" }) { i, song ->
            TrackRow(i + 1, song, playing = song.id == now.song?.id, playlist = playlist) { player.play(album.songs, i) }
        }
    }
}

@Composable
private fun TrackRow(number: Int, song: Song, playing: Boolean, playlist: Long?, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clip(Corner).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text("$number", style = Type.RowSubtitle, color = if (playing) p.accent else p.inkMuted, modifier = Modifier.width(34.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                    Text(song.title, style = Type.RowTitle.copy(fontSize = 16.sp, lineHeight = 21.sp), color = if (playing) p.accent else p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = Type.RowSubtitle.copy(fontSize = 14.sp, lineHeight = 19.sp), color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val graph = LocalContext.current.graph
                SongMenu(song) { close ->
                    if (playlist != null) MenuItem("Remove from this playlist") {
                        close(); graph.scope.launch { graph.library.removeFromPlaylist(playlist, song.id); graph.sync.flush() }
                    }
                }
            }
            Hairline()
        }
    }
}

/** The artist's picture, the width of the page with their name over its foot, then their shelves. */
@Composable
private fun ArtistPage(artist: Artist, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val player = LocalContext.current.graph.player
    val top = artist.sections.firstOrNull { s -> s.cards.isNotEmpty() && s.cards.all { it.song != null } }?.cards?.mapNotNull { it.song }.orEmpty()
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp + LocalDockHeight.current)) {
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(p.surfaceHigh)) {
                AsyncImage(artist.art?.let { artAt(it, 1200) }, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f))))
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 20.dp, end = 16.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(artist.name, style = Type.Masthead, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (top.isNotEmpty()) Box(
                        Modifier.size(52.dp).clip(CircleShape).background(p.accent).clickable { player.play(top) }
                            .semantics { role = Role.Button; contentDescription = "Play ${artist.name}" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.PlayArrow, null, tint = p.onAccent, modifier = Modifier.size(30.dp)) }
                }
            }
        }
        for (section in artist.sections) {
            item(key = "t" + section.title) { SectionTitle(section.title.lowercase(), Modifier.padding(horizontal = 20.dp)) }
            if (section.cards.all { it.song != null }) {
                val songs = section.cards.mapNotNull { it.song }
                items(section.cards.take(5), key = { section.title + it.song!!.id }) { card ->
                    ListRow(card.title, card.subtitle, art = card.art?.let { artAt(it, 226) }, modifier = Modifier.padding(horizontal = 20.dp), onClick = { player.play(songs, songs.indexOf(card.song)) }, trailing = { SongMenu(card.song!!) })
                }
            } else item(key = "r" + section.title) { Shelf(section, onOpen) }
        }
    }
}

/** A row of covers across the page, each opening its page or playing its song. */
@Composable
fun Shelf(section: Section, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val player = LocalContext.current.graph.player
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(section.cards, key = { it.song?.id ?: it.page ?: it.title }) { card ->
            val round = card.page?.startsWith("UC") == true
            Column(Modifier.width(156.dp).clip(Corner).clickable { card.song?.let(player::playWithRadio) ?: card.page?.let(onOpen) }, horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start) {
                Cover(card.art?.let { artAt(it, 544) }, 156.dp, round = round)
                val small = Type.RowSubtitle.copy(fontSize = 14.sp, lineHeight = 18.sp)
                Text(if (card.song != null) com.verza.data.shownTitle(card.title, card.subtitle.substringBefore(" • ")) else card.title, style = small, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
                Text(card.subtitle.replace(" • ", " · "), style = small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** What a byline may say a record is, before who made it: "Album • Coldplay • 2012". */
private val Kinds = setOf("Album", "Single", "EP", "Playlist", "Mix", "Audiobook")

private fun songsLabel(songs: List<Song>): String {
    val minutes = songs.sumOf { it.durationMs } / 60_000
    val count = if (songs.size == 1) "1 song" else "${songs.size} songs"
    return if (minutes > 0) "$count · $minutes min" else count
}
