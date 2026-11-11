package com.verza.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import com.verza.ui.kit.Word
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.data.Card
import com.verza.data.Song
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.kit.Empty
import com.verza.ui.kit.ListRow
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.Masthead
import com.verza.ui.kit.SongMenu
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type

private val Sections = listOf("liked", "recent", "playlists", "downloaded", "on device", "your sound")

/** The library as its sections in big words, the Zune's main menu; each opens as a page. */
@Composable
fun LibraryPage(onOpen: (String) -> Unit) {
    val library = LocalContext.current.graph.library
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = open != null) { open = null }
    val p = LocalPalette.current

    when (val section = open) {
        null -> Column(Modifier.fillMaxSize()) {
            Masthead("library", "your collection")
            for (name in Sections) {
                Text(
                    name,
                    style = Type.Masthead.copy(fontSize = 36.sp, lineHeight = 42.sp),
                    color = p.ink,
                    modifier = Modifier.fillMaxWidth().clickable { open = name }.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
        "on device" -> Section(section) { OnDevice(onOpen) }
        "your sound" -> Section(section) { YourSound() }
        "playlists" -> {
            val lists by library.playlists.collectAsState(initial = null)
            val theirs by account("account-playlists", ListSerializer(Card.serializer())) { it.accountPlaylists() }
            Section(section) {
                val l = lists ?: return@Section
                if (l.isEmpty() && theirs.isEmpty()) Empty("Make a playlist from any song's menu")
                else LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalDockHeight.current)) {
                    items(l, key = { it.playlist.id }) { pl ->
                        ListRow(pl.playlist.name, if (pl.count == 1) "1 song" else "${pl.count} songs", art = pl.cover?.let { artAt(it, 226) },
                            onClick = { onOpen("pl:${pl.playlist.id}") })
                    }
                    if (theirs.isNotEmpty()) item { Text("from youtube music", style = Type.Section, color = p.ink, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)) }
                    items(theirs, key = { it.page!! }) { c -> ListRow(c.title, c.subtitle, art = c.art?.let { artAt(it, 226) }, onClick = { onOpen(c.page!!) }) }
                }
            }
        }
        "liked" -> {
            // Your likes here first, then the account's, each song once.
            val mine by library.liked.collectAsState(initial = null)
            val theirs by account("account-liked", ListSerializer(Song.serializer())) { it.accountLiked() }
            Section(section) {
                val s = (mine ?: return@Section).let { m -> (m + theirs).distinctBy { it.id } }
                if (s.isEmpty()) Empty(emptyLine(section)) else SongList(s)
            }
        }
        else -> {
            val songs by when (section) {
                "recent" -> library.recent
                else -> library.downloaded
            }.collectAsState(initial = null)
            Section(section) {
                val s = songs ?: return@Section
                if (s.isEmpty()) Empty(emptyLine(section)) else SongList(s)
            }
        }
    }
}

/**
 * What the signed-in account has under [key], from its saved copy and then fresh; empty when signed
 * out or never fetched.
 */
@Composable
private fun <T> account(key: String, serializer: KSerializer<List<T>>, fetch: suspend (com.verza.data.youtube.YouTube) -> List<T>): State<List<T>> {
    val graph = LocalContext.current.graph
    val cookie by graph.account.cookie.collectAsState()
    return produceState(emptyList(), cookie) {
        if (cookie.isNullOrBlank()) { value = emptyList(); return@produceState }
        graph.saved.load(key, serializer) { fetch(graph.youtube) }.catch { }.collect { value = it }
    }
}

private fun emptyLine(section: String) = when (section) {
    "liked" -> "Like songs to see them here"
    "recent" -> "Play something to see it here"
    else -> "Download songs to listen offline"
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Masthead(title)
        content()
    }
}

/** Songs that play as a list from the one tapped, or all of them, in order or shuffled. */
@Composable
private fun SongList(songs: List<Song>) {
    val player = LocalContext.current.graph.player
    val now by player.state.collectAsState()
    val p = LocalPalette.current
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalDockHeight.current)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Word("play", p.accent) { player.play(songs) }
                Word("shuffle") { player.shuffle(songs) }
                Spacer(Modifier.weight(1f))
                Text(if (songs.size == 1) "1 song" else "${songs.size} songs", style = Type.Small, color = p.inkMuted)
            }
        }
        itemsIndexed(songs, key = { _, s -> s.id }) { i, song ->
            ListRow(song.title, song.artist, art = song.art?.let { artAt(it, 226) }, playing = song.id == now.song?.id, onClick = { player.play(songs, i) }, trailing = { SongMenu(song) })
        }
    }
}
