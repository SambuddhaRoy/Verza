package com.verza.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.verza.BuildConfig
import com.verza.data.Setting
import com.verza.data.sources.SourceProblem
import com.verza.data.youtube.Quality
import com.verza.graph
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.SectionTitle
import com.verza.ui.theme.Corner
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsPage(onBack: () -> Unit, onSignIn: () -> Unit) {
    val graph = LocalContext.current.graph
    val s = graph.settings
    val scope = rememberCoroutineScope()
    val quality by s.quality.flow.collectAsState(initial = Quality.HIGH.name)
    val fade by s.fadeSeconds.flow.collectAsState(initial = 0)
    val sources by graph.sourceList.collectAsState()
    var adding by remember { mutableStateOf(false) }
    val cookie by graph.account.cookie.collectAsState()

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalDockHeight.current)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = LocalPalette.current.ink) }
            }
            Text("settings", style = Type.Masthead, color = LocalPalette.current.ink, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        }
        item { SectionTitle("account") }
        item {
            // Signed in, your likes and playlists go to YouTube Music and Home is your own.
            if (cookie.isNullOrBlank()) Row(Modifier.fillMaxWidth().clip(Corner).clickable(onClick = onSignIn).padding(vertical = 12.dp)) {
                Column {
                    Text("Sign in to YouTube Music", style = Type.RowTitle, color = LocalPalette.current.accent)
                    Text("Your likes, playlists and Home from your account", style = Type.RowSubtitle, color = LocalPalette.current.inkMuted)
                }
            } else Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Signed in to YouTube Music", style = Type.RowTitle, color = LocalPalette.current.ink, modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { graph.account.set(null) } }) { Text("Sign out") }
            }
        }
        item { SectionTitle("playback") }
        item {
            Choice("Audio quality", "How much of YouTube's stream to fetch", listOf("HIGH" to "High", "MEDIUM" to "Medium", "LOW" to "Low"), quality) {
                scope.launch { s.quality.set(it) }
            }
        }
        item {
            Choice("Fade between songs", "Fade out at the end of a song and in at the start of the next", listOf(0 to "Off", 3 to "3s", 6 to "6s", 12 to "12s"), fade) {
                scope.launch { s.fadeSeconds.set(it) }
            }
        }
        item { Toggle("Skip silence", "Skip silent stretches inside a song", s.skipSilence) }
        item { Toggle("Resume on open", "Open where you left off", s.resumeOnOpen) }

        item { SectionTitle("lossless sources") }
        item {
            Text(
                "A server that serves songs as FLAC. When it has the one you play, you hear it lossless instead of YouTube's stream.",
                style = Type.Body, color = LocalPalette.current.inkMuted, modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        for ((i, source) in sources.withIndex()) item(key = "source$i") {
            // Only the host: the rest of a link can be a password.
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(source.name, style = Type.RowTitle, color = LocalPalette.current.ink)
                    Text(runCatching { java.net.URI(source.url).host }.getOrNull().orEmpty(), style = Type.RowSubtitle, color = LocalPalette.current.inkMuted)
                }
                IconButton(onClick = { scope.launch { s.sources.set(sources.filterIndexed { j, _ -> j != i }) } }) {
                    Icon(Icons.Rounded.Close, "Remove ${source.name}", tint = LocalPalette.current.inkMuted)
                }
            }
        }
        item { Row(Modifier.fillMaxWidth().clip(Corner).clickable { adding = true }.padding(vertical = 12.dp)) { Text("Add a source", style = Type.RowTitle, color = LocalPalette.current.accent) } }
        if (sources.isNotEmpty()) item { Toggle("Use on mobile data", "A lossless song is roughly 30 MB, several times YouTube's", s.sourcesOnMobileData) }

        item { SectionTitle("search") }
        item { Toggle("Save search history", "Remember recent searches", s.saveSearchHistory) }
        item {
            Row(Modifier.fillMaxWidth().clip(Corner).clickable { scope.launch { s.searchHistory.set(emptyList()) } }.padding(vertical = 12.dp)) {
                Text("Clear search history", style = Type.RowTitle, color = LocalPalette.current.ink)
            }
        }
        item { SectionTitle("about") }
        item {
            val context = LocalContext.current
            val ready by graph.updates.ready.collectAsState()
            var checked by remember { mutableStateOf<String?>(null) }
            Row(Modifier.fillMaxWidth().clip(Corner).clickable {
                val r = ready
                if (r != null) com.verza.ui.home.install(context, r)
                else { checked = "Looking…"; scope.launch { checked = graph.updates.check()?.let { "Verza ${it.version} is downloading" } ?: "Verza is up to date" } }
            }.padding(vertical = 12.dp)) {
                Column {
                    Text(if (ready != null) "Update to ${ready!!.version}" else "Check for updates", style = Type.RowTitle, color = LocalPalette.current.accent)
                    Text(checked ?: "Verza ${BuildConfig.VERSION_NAME}", style = Type.RowSubtitle, color = LocalPalette.current.inkMuted)
                }
            }
        }
    }
    if (adding) AddSource(onDone = { adding = false })
}

/** Paste a link; it's checked against the server before it's kept. */
@Composable
private fun AddSource(onDone: () -> Unit) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Add a source") },
        text = {
            Column {
                OutlinedTextField(link, { link = it; problem = null }, singleLine = true, placeholder = { Text("https://…/manifest.json") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), shape = Corner)
                problem?.let { Text(it, style = Type.RowSubtitle, color = androidx.compose.ui.graphics.Color(0xFFE5484D), modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = link.isNotBlank() && !checking, onClick = {
                checking = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { graph.sources.inspect(link) } }
                    checking = false
                    result.onSuccess { source ->
                        val list = graph.settings.sources.get()
                        if (list.none { it.url == source.url }) graph.settings.sources.set(list + source)
                        onDone()
                    }.onFailure { problem = (it as? SourceProblem)?.message ?: "Couldn't add that source." }
                }
            }) { Text(if (checking) "Checking…" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
    )
}

/** A setting with a switch; the whole row toggles it, and says so to TalkBack. */
@Composable
private fun Toggle(title: String, subtitle: String, setting: Setting<Boolean>) {
    val scope = rememberCoroutineScope()
    val on by setting.flow.collectAsState(initial = false)
    Row(
        Modifier.fillMaxWidth().clip(Corner).clickable { scope.launch { setting.set(!on) } }
            .semantics(mergeDescendants = true) { role = Role.Switch; stateDescription = if (on) "On" else "Off" }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = Type.RowTitle, color = LocalPalette.current.ink)
            Text(subtitle, style = Type.RowSubtitle, color = LocalPalette.current.inkMuted)
        }
        Switch(checked = on, onCheckedChange = null)
    }
}

/** A setting with a few values; a tap moves to the next. */
@Composable
private fun <T> Choice(title: String, subtitle: String, options: List<Pair<T, String>>, value: T, onPick: (T) -> Unit) {
    val i = options.indexOfFirst { it.first == value }.coerceAtLeast(0)
    Row(
        Modifier.fillMaxWidth().clip(Corner).clickable { onPick(options[(i + 1) % options.size].first) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = Type.RowTitle, color = LocalPalette.current.ink)
            Text(subtitle, style = Type.RowSubtitle, color = LocalPalette.current.inkMuted)
        }
        Text(options[i].second, style = Type.RowTitle, color = LocalPalette.current.accent)
    }
}
