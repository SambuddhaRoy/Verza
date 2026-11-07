package com.verza.ui.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.verza.data.Card
import com.verza.data.Section
import com.verza.data.Song
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.kit.Cover
import com.verza.ui.kit.Empty
import com.verza.ui.kit.ListRow
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.SectionTitle
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
                id.startsWith("pl:") -> graph.library.playlist(id.removePrefix("pl:").toLong()).map { PageContent.OfAlbum(it) }
                id.startsWith("UC") -> graph.saved.load(id, Artist.serializer()) { graph.youtube.artist(id) }.map { PageContent.OfArtist(it) }
                else -> graph.saved.load(id, Album.serializer()) { graph.youtube.album(id) }.map { PageContent.OfAlbum(it) }
            }
            pages.catch { e -> emit(PageContent.Failed(if (e is IOException) "You're offline, and this page hasn't been opened before" else "Couldn't open this")) }
                .collect { _content.value = it }
        }
    }
}

/** An album's, a playlist's or an artist's page, with the way back over its top corner. */
@Composable
fun PageScreen(id: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val graph = LocalContext.current.graph
    val model: PageModel = viewModel(key = id, factory = viewModelFactory { initializer { PageModel(graph, id) } })
    val content by model.content.collectAsState()
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize()) {
        when (val c = content) {
            null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = p.accent)
            is PageContent.Failed -> Empty(c.message)
            is PageContent.OfAlbum -> AlbumPage(c.album)
            is PageContent.OfArtist -> ArtistPage(c.artist, onOpen)
        }
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.ink) }
    }
}

/** The cover, the name, then play and shuffle as words, then the songs in order. */
@Composable
private fun AlbumPage(album: Album) {
    val p = LocalPalette.current
    val player = LocalContext.current.graph.player
    val now by player.state.collectAsState()
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 16.dp + LocalDockHeight.current)) {
        item {
            Column(Modifier.widthIn(max = 420.dp)) {
                Cover(album.art?.let { artAt(it, 1200) }, 0.dp, Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(14.dp))
                Text(album.title, style = Type.Title, color = p.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (album.subtitle.isNotBlank()) Text(album.subtitle, style = Type.RowSubtitle, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 6.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Word("play", p.accent, enabled = album.songs.isNotEmpty()) { player.play(album.songs) }
                    Word("shuffle", enabled = album.songs.isNotEmpty()) { player.shuffle(album.songs) }
                    Spacer(Modifier.weight(1f))
                    Text(songsLabel(album.songs), style = Type.Small, color = p.inkMuted)
                }
            }
        }
        itemsIndexed(album.songs, key = { i, s -> "$i${s.id}" }) { i, song ->
            TrackRow(i + 1, song, playing = song.id == now.song?.id) { player.play(album.songs, i) }
        }
    }
}

@Composable
private fun TrackRow(number: Int, song: Song, playing: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clip(com.verza.ui.theme.Corner).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$number", style = Type.RowSubtitle, color = p.inkMuted, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = Type.RowTitle, color = if (playing) p.accent else p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = Type.RowSubtitle, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The artist's picture and name, then their shelves: songs as rows, the rest as covers across. */
@Composable
private fun ArtistPage(artist: Artist, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val player = LocalContext.current.graph.player
    LazyColumn(contentPadding = PaddingValues(top = 56.dp, bottom = 16.dp + LocalDockHeight.current)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Cover(artist.art?.let { artAt(it, 1200) }, 0.dp, Modifier.fillMaxWidth().aspectRatio(1f))
                Text(artist.name, style = Type.Masthead, color = p.ink, modifier = Modifier.padding(top = 12.dp))
            }
        }
        for (section in artist.sections) {
            item(key = "t" + section.title) { SectionTitle(section.title.lowercase(), Modifier.padding(horizontal = 20.dp)) }
            if (section.cards.all { it.song != null }) {
                val songs = section.cards.mapNotNull { it.song }
                items(section.cards.take(5), key = { section.title + it.song!!.id }) { card ->
                    ListRow(card.title, card.subtitle, art = card.art?.let { artAt(it, 226) }, modifier = Modifier.padding(horizontal = 20.dp), onClick = { player.play(songs, songs.indexOf(card.song)) })
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
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(section.cards, key = { it.song?.id ?: it.page ?: it.title }) { card ->
            Column(Modifier.width(150.dp).clip(com.verza.ui.theme.Corner).clickable { card.song?.let(player::playWithRadio) ?: card.page?.let(onOpen) }) {
                Cover(card.art?.let { artAt(it, 544) }, 150.dp, round = card.page?.startsWith("UC") == true)
                Text(card.title, style = Type.RowTitle.copy(fontSize = Type.RowSubtitle.fontSize), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                Text(card.subtitle, style = Type.RowSubtitle, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun songsLabel(songs: List<Song>): String {
    val minutes = songs.sumOf { it.durationMs } / 60_000
    val count = if (songs.size == 1) "1 song" else "${songs.size} songs"
    return if (minutes > 0) "$count · $minutes min" else count
}
