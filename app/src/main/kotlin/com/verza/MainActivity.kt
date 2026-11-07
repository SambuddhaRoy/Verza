package com.verza

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.verza.data.SharedSession
import com.verza.data.Sessions
import com.verza.data.toSong
import com.verza.data.videoIdIn
import com.verza.ui.Shell
import com.verza.ui.theme.VerzaTheme
import com.verza.ui.theme.coverSwatches

class MainActivity : ComponentActivity() {
    /** A "listen along" link waiting for a yes. */
    private var incoming by mutableStateOf<SharedSession?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after a rotation the launching intent is still here, and taking it
        // again would restart a shared song mid-listen.
        if (savedInstanceState == null) take(intent)
        setContent {
            // After the first frame: connecting starts the player's service on the main thread, and
            // done first it held the first frame back. Then any likes still to send to the account, and a look for an update.
            LaunchedEffect(Unit) { withFrameNanos { }; graph.player.connect(); graph.sync.flush(); graph.updates.check() }
            val playing by graph.player.state.collectAsState()
            VerzaTheme(swatches = coverSwatches(playing.song?.art)) {
                Shell()
                incoming?.let { ListenAlong(it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    /** A link opened or text shared into the app. */
    private fun take(intent: Intent?) {
        val data = intent?.data
        if (intent?.action == Intent.ACTION_VIEW && data?.scheme == "verza") {
            incoming = Sessions.read(data.toString())
            if (incoming == null) Toast.makeText(this, "That listening session link doesn't work", Toast.LENGTH_SHORT).show()
            return
        }
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        } ?: return
        val id = videoIdIn(text)
        if (id != null) graph.player.playId(id)
        else Toast.makeText(this, "No YouTube link in that to play", Toast.LENGTH_SHORT).show()
    }

    // Asked, never started on its own: anyone can make one of these links.
    @androidx.compose.runtime.Composable
    private fun ListenAlong(session: SharedSession) {
        val n = session.tracks.size
        val first = session.tracks[session.index].title
        AlertDialog(
            onDismissRequest = { incoming = null },
            title = { Text("Listen along?") },
            text = { Text("Someone shared $n ${if (n == 1) "song" else "songs"}${if (first.isNotBlank()) ", playing \"$first\"" else ""}. This replaces your queue.") },
            confirmButton = {
                TextButton(onClick = {
                    graph.player.play(session.tracks.map { it.toSong() }, session.index, session.positionMs)
                    incoming = null
                }) { Text("Listen") }
            },
            dismissButton = { TextButton(onClick = { incoming = null }) { Text("Not now") } },
        )
    }
}
