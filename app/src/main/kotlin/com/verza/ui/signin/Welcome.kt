package com.verza.ui.signin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.data.phone.PhoneMusic
import com.verza.graph
import com.verza.ui.kit.Word
import com.verza.ui.kit.takesTouches
import com.verza.ui.settings.Lossless
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.launch

/**
 * The first screens of a new install, each one skippable: what Verza is and signing in; what a
 * download is, Regular or Lossless, with the Soulseek sign-in Lossless needs; then reading the music
 * on the phone. [account] false skips the first, for an update from before the other two were asked.
 */
@Composable
fun Welcome(account: Boolean, onSignIn: () -> Unit) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current
    var step by rememberSaveable { mutableIntStateOf(if (account) 0 else 1) }
    val finish = { scope.launch { graph.settings.onboarded.set(true); graph.settings.musicSetUp.set(true) } }
    val allow = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) graph.scope.launch { runCatching { graph.phone.scan() } }
        finish()
    }

    Column(
        Modifier.fillMaxSize().takesTouches().background(p.canvas).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp).padding(top = 72.dp, bottom = 28.dp),
    ) {
        @Composable fun Heading(text: String, lead: String) {
            Text(text, style = Type.Masthead.copy(fontSize = 64.sp, lineHeight = 68.sp), color = p.ink)
            Text(lead, style = Type.RowTitle, color = p.inkMuted, modifier = Modifier.padding(top = 12.dp, bottom = 20.dp))
        }
        @Composable fun Words(content: @Composable () -> Unit) =
            Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) { content() }

        when (step) {
            0 -> {
                Heading("verza", "YouTube Music without the ads, your own music and lossless sources alongside it.")
                Text(
                    "Sign in to bring your likes, playlists and Home from your YouTube Music account. You can do it later in Settings.",
                    style = Type.Body, color = p.inkMuted,
                )
                Words {
                    // Back from signing in, this screen opens at the next step
                    Word("sign in", p.accent) { scope.launch { graph.settings.onboarded.set(true) }; onSignIn() }
                    Word("not now") { scope.launch { graph.settings.onboarded.set(true) }; step = 1 }
                }
            }
            1 -> {
                Heading("downloads", "Any song, album or playlist can be saved to play with no connection. Settings can change this later.")
                Lossless()
                Words {
                    Word("next", p.accent) { if (graph.phone.allowed()) finish() else step = 2 }
                }
            }
            else -> {
                Heading("your music", "Play the music already on this phone, with your downloads, alongside everything else.")
                Text(
                    "Your own files show up in Library under on device, and start a radio or join a playlist like any song. " +
                        "When a song you have comes up anywhere, in a radio, a playlist or a mix, it plays from the file: no data, " +
                        "and a lossless file plays lossless. Verza never changes your own files.",
                    style = Type.Body, color = p.inkMuted,
                )
                Words {
                    Word("allow", p.accent) { allow.launch(PhoneMusic.PERMISSION) }
                    Word("not now") { finish() }
                }
            }
        }
    }
}
