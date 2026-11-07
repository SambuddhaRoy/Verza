package com.verza.ui.kit

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import com.verza.data.Song
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.theme.Corner
import com.verza.ui.theme.LocalPalette
import kotlinx.coroutines.launch

/** Opens a page by id; provided by the shell so any row's menu can go to an artist or album. */
val LocalOpen = androidx.compose.runtime.staticCompositionLocalOf<(String) -> Unit> { {} }

/**
 * A song's ⋯: play next, add to queue or a playlist, like, download, share, go to its artist or album;
 * then whatever [extra] items the page adds, each given a way to close the menu.
 */
@Composable
fun SongMenu(song: Song, extra: @Composable (close: () -> Unit) -> Unit = {}) {
    val context = LocalContext.current
    val graph = context.graph
    val open = LocalOpen.current
    val scope = rememberCoroutineScope()
    val liked by graph.library.likedIds.collectAsState(initial = emptySet())
    var shown by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { shown = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = LocalPalette.current.inkMuted) }
        DropdownMenu(expanded = shown, onDismissRequest = { shown = false }, shape = Corner) {
            MenuItem("Play next") { shown = false; graph.player.playNext(song) }
            MenuItem("Add to queue") { shown = false; graph.player.enqueue(listOf(song)) }
            MenuItem("Add to playlist…") { shown = false; picking = true }
            val isLiked = song.id in liked
            MenuItem(if (isLiked) "Unlike" else "Like") { shown = false; graph.like(song, !isLiked) }
            if (!song.onPhone) {
                MenuItem("Download") { shown = false; graph.downloads.save(listOf(song)) }
                MenuItem("Share") {
                    shown = false
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "${song.title} by ${song.artist}\nhttps://music.youtube.com/watch?v=${song.id}")
                    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                MenuItem("Go to artist") { shown = false; scope.launch { graph.youtube.runCatching { links(song.id, song.title) }.getOrNull()?.first?.firstOrNull()?.let { open(it.page) } } }
                MenuItem("Go to album") { shown = false; scope.launch { graph.youtube.runCatching { links(song.id, song.title) }.getOrNull()?.second?.let { open(it.page) } } }
            }
            extra { shown = false }
        }
    }
    if (picking) AddToPlaylist(song, onDone = { picking = false })
}

/** Your playlists to add [song] to, or a new one named here. */
@Composable
fun AddToPlaylist(song: Song, onDone: () -> Unit) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val lists by graph.library.playlists.collectAsState(initial = emptyList())
    var naming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (naming) "New playlist" else "Add to playlist") },
        text = {
            if (naming) OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("Name") }, shape = Corner)
            else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item { ListRow("New playlist", null, icon = Icons.Rounded.Add, onClick = { naming = true }) }
                items(lists, key = { it.playlist.id }) { pl ->
                    ListRow(pl.playlist.name, if (pl.count == 1) "1 song" else "${pl.count} songs", art = pl.cover?.let { artAt(it, 226) },
                        onClick = { scope.launch { graph.library.addToPlaylist(pl.playlist.id, song); graph.sync.flush() }; onDone() })
                }
            }
        },
        confirmButton = {
            if (naming) TextButton(enabled = name.isNotBlank(), onClick = {
                scope.launch { val id = graph.library.createPlaylist(name.trim()); graph.library.addToPlaylist(id, song); graph.sync.flush() }
                onDone()
            }) { Text("Make it") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
    )
}

@Composable
fun MenuItem(text: String, onClick: () -> Unit) = DropdownMenuItem(text = { Text(text) }, onClick = onClick)
