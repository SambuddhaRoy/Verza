package com.verza.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import com.verza.ui.kit.ChromeButton
import com.verza.ui.kit.Glassed
import com.verza.ui.kit.Hairline
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import com.verza.ui.kit.Word
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
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

/** The Library's sections: the key kept while one is open, its name, its symbol. Your Sound is a tab of its own. */
private val Sections = listOf(
    Triple("liked", "Liked songs", Icons.Rounded.Favorite),
    Triple("recent", "Recently played", Icons.Rounded.History),
    Triple("playlists", "Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    Triple("downloaded", "Downloaded", Icons.Rounded.ArrowCircleDown),
    Triple("on device", "On this phone", Icons.Rounded.Smartphone),
)

private fun nameOf(key: String) = Sections.firstOrNull { it.first == key }?.second ?: key

/** The library as a list of its sections, as Apple Music's is; each opens as a page. */
@Composable
fun LibraryPage(onOpen: (String) -> Unit) {
    val library = LocalContext.current.graph.library
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = open != null) { open = null }
    val p = LocalPalette.current
    // An album or artist opened from a collection: its songs, in its order.
    var group by remember { mutableStateOf<Pair<String, List<Song>>?>(null) }
    BackHandler(enabled = group != null) { group = null }
    group?.let { (title, songs) -> Section(title, onBack = { group = null }) { SongList(songs) }; return }
    val graph = LocalContext.current.graph

    when (val section = open) {
        null -> Column(Modifier.fillMaxSize()) {
            Masthead("Library")
            for ((key, name, icon) in Sections) {
                Row(
                    Modifier.fillMaxWidth().clickable { open = key }.padding(start = 20.dp)
                        .semantics(mergeDescendants = true) { role = androidx.compose.ui.semantics.Role.Button },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = p.ink, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.heightIn(min = 54.dp).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = Type.RowTitle.copy(fontSize = 20.sp, lineHeight = 25.sp), color = p.ink, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ChevronRight, null, tint = p.inkMuted)
                        }
                        Hairline()
                    }
                }
            }
        }
        "on device" -> Section(nameOf(section), onBack = { open = null }) { OnDevice(onOpen) { t, _, ss -> group = t to ss } }
        "downloaded" -> {
            val songs by library.downloaded.collectAsState(initial = null)
            val phone by graph.phone.library.collectAsState()
            // Downloads sort by their files' tags, which the phone's scan reads, where it may.
            LaunchedEffect(Unit) { if (graph.phone.allowed() && graph.phone.library.value == null) runCatching { graph.phone.scan() } }
            Section(nameOf(section), onBack = { open = null }) {
                val s = songs ?: return@Section
                if (s.isEmpty()) Empty(emptyLine(section))
                else Collection("downloaded", s, facts = { phone?.facts?.get(it.id) ?: com.verza.data.phone.Facts(album = it.album) }, onOpenGroup = { t, _, ss -> group = t to ss })
            }
        }
        "playlists" -> {
            val lists by library.playlists.collectAsState(initial = null)
            val theirs by account("account-playlists", ListSerializer(Card.serializer())) { it.accountPlaylists() }
            Section(nameOf(section), onBack = { open = null }) {
                val l = lists ?: return@Section
                // A playlist can be started here, not only from a song's menu.
                var naming by remember { mutableStateOf(false) }
                Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { Word("New playlist", p.accent, icon = Icons.Rounded.Add) { naming = true } }
                if (naming) NewPlaylist(onDone = { naming = false }, onMade = { onOpen("pl:$it") })
                if (l.isEmpty() && theirs.isEmpty()) Empty("Make a playlist here or from any song's menu")
                else LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalDockHeight.current)) {
                    items(l, key = { it.playlist.id }) { pl ->
                        ListRow(pl.playlist.name, if (pl.count == 1) "1 song" else "${pl.count} songs", art = pl.cover?.let { artAt(it, 226) },
                            onClick = { onOpen("pl:${pl.playlist.id}") })
                    }
                    if (theirs.isNotEmpty()) item { Text("From YouTube Music", style = Type.Section, color = p.ink, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)) }
                    items(theirs, key = { it.page!! }) { c -> ListRow(c.title, c.subtitle, art = c.art?.let { artAt(it, 226) }, onClick = { onOpen(c.page!!) }) }
                }
            }
        }
        "liked" -> {
            // Your likes here first, then the account's, each song once.
            val mine by library.liked.collectAsState(initial = null)
            val theirs by account("account-liked", ListSerializer(Song.serializer())) { it.accountLiked() }
            Section(nameOf(section), onBack = { open = null }) {
                val s = (mine ?: return@Section).let { m -> (m + theirs).distinctBy { it.id } }
                if (s.isEmpty()) Empty(emptyLine(section)) else SongList(s)
            }
        }
        else -> {
            val songs by when (section) {
                "recent" -> library.recent
                else -> library.downloaded
            }.collectAsState(initial = null)
            Section(nameOf(section), onBack = { open = null }) {
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
private fun Section(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    // The way back to the Library, in glass over the page's top, as an album page has.
    Glassed(
        ground = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        content = {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(52.dp))
                Masthead(title)
                content()
            }
        },
        glass = { ChromeButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), onClick = onBack) },
    )
}

/** Names a new, empty playlist and opens it. */
@Composable
private fun NewPlaylist(onDone: () -> Unit, onMade: (Long) -> Unit) {
    val graph = LocalContext.current.graph
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDone,
        title = { Text("New playlist") },
        text = { com.verza.ui.kit.Field(name, { name = it }, placeholder = "Name") },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = name.isNotBlank(), onClick = {
                scope.launch { val id = graph.library.createPlaylist(name.trim()); graph.sync.flush(); onMade(id) }
                onDone()
            }) { Text("Make it") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDone) { Text("Cancel") } },
    )
}

/** Songs that play as a list from the one tapped, or all of them, in order or shuffled. */
@Composable
private fun SongList(songs: List<Song>) {
    val player = LocalContext.current.graph.player
    val now by player.state.collectAsState()
    val p = LocalPalette.current
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalDockHeight.current)) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Word("Play", p.accent, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow) { player.play(songs) }
                    Word("Shuffle", p.ink, Modifier.weight(1f), icon = Icons.Rounded.Shuffle) { player.shuffle(songs) }
                }
                Text(if (songs.size == 1) "1 song" else "${songs.size} songs", style = Type.Small, color = p.inkMuted, modifier = Modifier.padding(top = 10.dp))
            }
        }
        itemsIndexed(songs, key = { _, s -> s.id }) { i, song ->
            ListRow(song.title, song.artist, art = song.art?.let { artAt(it, 226) }, playing = song.id == now.song?.id, onClick = { player.play(songs, i) }, trailing = { SongMenu(song) })
        }
    }
}
