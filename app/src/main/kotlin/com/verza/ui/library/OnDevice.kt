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
fun OnDevice(onOpen: (String) -> Unit, onOpenGroup: (String, String, List<com.verza.data.Song>) -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, readAudio) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    val p = LocalPalette.current

    if (!allowed) {
        // The word under the line, not after the page: placed after it, it was pushed off the screen.
        Empty("Allow access to play the music on your phone. Verza only reads your files.", Icons.Rounded.PhoneAndroid, action = "allow") { ask.launch(readAudio) }
        return
    }
    LaunchedEffect(Unit) { graph.phone.scan() }
    val library by graph.phone.library.collectAsState()
    val lib = library ?: return
    if (lib.songs.isEmpty()) { Empty("No music found on this phone"); return }
    // Songs, albums or artists, as on the desktop; Verza's own downloads are under Downloaded.
    Collection("local", lib.songs.filter { it.onPhone }, facts = { lib.facts[it.id] ?: com.verza.data.phone.Facts() }, onOpenGroup = onOpenGroup,
        folders = lib.playlists.map { c -> c.title to { onOpen(c.id) } })
}
