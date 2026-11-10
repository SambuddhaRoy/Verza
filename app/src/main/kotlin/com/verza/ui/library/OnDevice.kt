package com.verza.ui.library

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.kit.Empty
import com.verza.ui.kit.ListRow
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.SongMenu
import com.verza.ui.kit.Word
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type

private val readAudio = com.verza.data.phone.PhoneMusic.PERMISSION

/**
 * The music on the phone: albums (a folder of one album by one artist), playlists (any other folder),
 * then every song. Scanned each time it opens, so music copied on since shows up.
 */
@Composable
fun OnDevice(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, readAudio) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    val p = LocalPalette.current

    if (!allowed) {
        Empty("Allow access to play the music on your phone. Verza only reads your files.", Icons.Rounded.PhoneAndroid)
        androidx.compose.foundation.layout.Box(Modifier.padding(start = 20.dp)) { Word("allow", p.accent) { ask.launch(readAudio) } }
        return
    }
    LaunchedEffect(Unit) { graph.phone.scan() }
    val library by graph.phone.library.collectAsState()
    val lib = library ?: return
    if (lib.songs.isEmpty()) { Empty("No music found on this phone"); return }
    val player = graph.player
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalDockHeight.current)) {
        for ((label, groups) in listOf("albums" to lib.albums, "playlists" to lib.playlists)) {
            if (groups.isEmpty()) continue
            item(key = "h$label") { Text(label, style = Type.Section, color = p.ink, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)) }
            items(groups, key = { it.id }) { c ->
                ListRow(c.title, if (c.isAlbum) c.subtitle else if (c.songs.size == 1) "1 song" else "${c.songs.size} songs",
                    art = c.songs.firstNotNullOfOrNull { it.art }?.let { artAt(it, 226) }, onClick = { onOpen(c.id) })
            }
        }
        item(key = "hsongs") { Text("songs", style = Type.Section, color = p.ink, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)) }
        itemsIndexed(lib.songs, key = { _, s -> s.id }) { i, song ->
            ListRow(song.title, song.artist, art = song.art?.let { artAt(it, 226) }, onClick = { player.play(lib.songs, i) }, trailing = { SongMenu(song) })
        }
    }
}
