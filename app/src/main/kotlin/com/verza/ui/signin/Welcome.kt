package com.verza.ui.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.graph
import com.verza.ui.kit.Word
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.launch

/** A new install's first screen: what Verza is, and signing in, which is optional. Shown once. */
@Composable
fun Welcome(onSignIn: () -> Unit) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current
    val done = { scope.launch { graph.settings.onboarded.set(true) } }
    Column(
        Modifier.fillMaxSize().background(p.canvas).padding(horizontal = 28.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("verza", style = Type.Masthead.copy(fontSize = 72.sp, lineHeight = 76.sp), color = p.ink)
        Text(
            "YouTube Music without the ads, your own music and lossless sources alongside it.",
            style = Type.RowTitle, color = p.inkMuted, modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )
        Text(
            "Sign in to bring your likes, playlists and Home from your YouTube Music account. You can do it later in Settings.",
            style = Type.Body, color = p.inkMuted, modifier = Modifier.padding(bottom = 20.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Word("sign in", p.accent) { done(); onSignIn() }
            Word("not now") { done() }
        }
    }
}
