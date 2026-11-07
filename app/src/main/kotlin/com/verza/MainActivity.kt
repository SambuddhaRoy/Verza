package com.verza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import com.verza.ui.Shell
import com.verza.ui.theme.VerzaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // After the first frame: connecting starts the player's service on the main thread, and
            // done first it held the first frame back. Then any likes still to send to the account, and a look for an update.
            LaunchedEffect(Unit) { withFrameNanos { }; graph.player.connect(); graph.sync.flush(); graph.updates.check() }
            VerzaTheme(swatches = emptyList()) { Shell() }
        }
    }
}
