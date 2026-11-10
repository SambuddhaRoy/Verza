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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsPage(onBack: () -> Unit, onSignIn: () -> Unit, onEqualizer: () -> Unit) {
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
        item {
            Row(Modifier.fillMaxWidth().clip(Corner).clickable(onClick = onEqualizer).padding(vertical = 10.dp)) {
                Column {
                    Text("Equaliser", style = Type.RowTitle, color = LocalPalette.current.ink)
                    Text("Bands, presets, bass and loudness", style = Type.RowSubtitle, color = LocalPalette.current.inkMuted)
                }
            }
        }
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

        item { SectionTitle("downloads") }
        item { DownloadKind() }
        item { Soulseek() }

        item { SectionTitle("your library") }
        item { LibraryFile() }
        item { SectionTitle("listening elsewhere") }
        item { ListenBrainz() }
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

/**
 * A ListenBrainz username adds its weekly mixes to Home. Spotify listening reaches Verza this way:
 * connect Spotify to ListenBrainz there. Checked before it's kept, and the mixes build in the background.
 */
@Composable
private fun ListenBrainz() {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val user by graph.mixPrefs.listenBrainzUser.flow.collectAsState(initial = "")
    var typed by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    val p = LocalPalette.current
    if (user.isNotBlank()) Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("ListenBrainz: $user", style = Type.RowTitle, color = p.ink)
            Text("Its weekly mixes are on Home", style = Type.RowSubtitle, color = p.inkMuted)
        }
        TextButton(onClick = { scope.launch { graph.mixPrefs.listenBrainzUser.set(""); graph.mixes.rebuildListenBrainz() } }) { Text("Disconnect") }
    } else Column(Modifier.padding(vertical = 4.dp)) {
        Text("A ListenBrainz username brings its weekly mixes to Home, Spotify listening included.", style = Type.Body, color = p.inkMuted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(typed, { typed = it; note = null }, singleLine = true, placeholder = { Text("Username") }, shape = Corner, modifier = Modifier.weight(1f))
            TextButton(enabled = typed.isNotBlank(), onClick = {
                note = "Checking…"
                scope.launch {
                    note = when (val c = graph.listenBrainz.check(typed.trim())) {
                        is com.verza.data.mixes.ListenBrainzRepository.Check.Found -> { graph.mixPrefs.listenBrainzUser.set(typed.trim()); graph.mixes.rebuildListenBrainz(); null }
                        com.verza.data.mixes.ListenBrainzRepository.Check.NoSuchUser -> "There's no ListenBrainz user by that name"
                        com.verza.data.mixes.ListenBrainzRepository.Check.Unreachable -> "Couldn't reach ListenBrainz. Try again."
                    }
                }
            }) { Text("Connect") }
        }
        note?.let { Text(it, style = Type.RowSubtitle, color = p.inkMuted) }
    }
}

/**
 * Which file a download is: YouTube's AAC, small and always there, or a FLAC from Soulseek, five to
 * ten times the size and only worth it on gear that can tell. Each says what it costs.
 */
@Composable
private fun DownloadKind() {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current
    val lossless by graph.settings.losslessDownloads.flow.collectAsState(initial = false)
    val user by graph.settings.soulseekUser.flow.collectAsState(initial = "")
    @Composable
    fun Option(value: Boolean, title: String, about: String) {
        Row(
            Modifier.fillMaxWidth().clip(Corner)
                .selectable(selected = lossless == value, role = Role.RadioButton) { scope.launch { graph.settings.losslessDownloads.set(value) } }
                .padding(vertical = 8.dp),
        ) {
            RadioButton(selected = lossless == value, onClick = null, modifier = Modifier.padding(top = 2.dp, end = 12.dp))
            Column {
                Text(title, style = Type.RowTitle, color = p.ink)
                Text(about, style = Type.RowSubtitle, color = p.inkMuted)
            }
        }
    }
    Column(Modifier.selectableGroup()) {
        Option(
            false, "Regular",
            "About 4 MB a song. YouTube's own audio, so every song downloads, in seconds, and thousands fit on " +
                "your phone. On the phone's speaker or Bluetooth headphones it sounds the same as lossless.",
        )
        Option(
            true, "Lossless (FLAC)",
            "About 30 MB a song: CD quality, exactly what's on the record. Worth it on wired headphones or a " +
                "DAC. Slower, because each song comes from someone on Soulseek and can wait in their queue, and a " +
                "song nobody shares downloads as regular instead." +
                if (user.isBlank()) " Needs a Soulseek account: sign in below." else "",
        )
    }
}

/**
 * Soulseek, a network of people sharing their music folders. Signed in, with Lossless chosen above, a download is a FLAC from
 * someone sharing the song, and YouTube's stream when nobody does. There's no sign-up: the server
 * registers a name the first time anyone signs in with it.
 */
@Composable
private fun Soulseek() {
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current
    val user by graph.settings.soulseekUser.flow.collectAsState(initial = "")
    val status by com.verza.data.downloads.Soulseek.status.collectAsState()
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    if (user.isNotBlank()) Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Soulseek: $user", style = Type.RowTitle, color = p.ink)
            Text(
                when {
                    status.online -> "Connected"
                    status.error != null -> status.error.orEmpty()
                    status.state == "connecting" -> "Connecting"
                    else -> "Connects when a lossless download starts"
                },
                style = Type.RowSubtitle, color = p.inkMuted,
            )
        }
        TextButton(onClick = {
            com.verza.data.downloads.Soulseek.logout(context)
            scope.launch { graph.settings.soulseekUser.set("") }
        }) { Text("Sign out") }
    } else Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            "Lossless songs come from Soulseek, a network of people sharing their music. " +
                "A name nobody has taken is yours the first time you sign in with it.",
            style = Type.Body, color = p.inkMuted,
        )
        OutlinedTextField(name, { name = it; note = null }, singleLine = true, enabled = !busy, placeholder = { Text("Username") },
            shape = Corner, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(password, { password = it; note = null }, singleLine = true, enabled = !busy, placeholder = { Text("Password") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = Corner, modifier = Modifier.weight(1f))
            TextButton(enabled = !busy && name.isNotBlank() && password.isNotEmpty(), onClick = {
                busy = true
                note = "Signing in…"
                val typed = name.trim()
                com.verza.data.downloads.Soulseek.login(context, typed, password)
                scope.launch {
                    val answer = kotlinx.coroutines.withTimeoutOrNull(30_000) {
                        com.verza.data.downloads.Soulseek.status.first { it.online || it.error != null }
                    }
                    note = when {
                        // Signing in is asking for lossless; Regular is one tap above to go back
                        answer?.online == true -> { graph.settings.soulseekUser.set(typed); graph.settings.losslessDownloads.set(true); null }
                        answer?.error != null -> answer.error
                        else -> "Couldn't reach Soulseek. Try again."
                    }
                    busy = false
                }
            }) { Text("Sign in") }
        }
        note?.let { Text(it, style = Type.RowSubtitle, color = p.inkMuted) }
    }
}

/**
 * Your library as a file: export it, import one (the old app's backups too), choose where downloads
 * go, and reset the listening log. The sign-in is never in the file.
 */
@Composable
private fun LibraryFile() {
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current
    var note by remember { mutableStateOf<String?>(null) }
    var resetting by remember { mutableStateOf(false) }
    val folder by graph.settings.downloadFolder.flow.collectAsState(initial = "")
    val export = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            note = runCatching {
                val text = graph.backup.export()
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }
                "Saved"
            }.getOrElse { "Couldn't save the file" }
        }
    }
    val import = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            note = runCatching {
                val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
                val r = graph.backup.import(text)
                "Added ${r.playlists} playlists and ${r.plays} listens, from ${r.songs} songs"
            }.getOrElse { "That isn't a Verza backup" }
        }
    }
    val chooseFolder = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        // Kept across restarts, or the folder is forgotten the next time the phone reboots.
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        scope.launch { graph.settings.downloadFolder.set(uri.toString()) }
    }
    @Composable fun Action(title: String, subtitle: String, onClick: () -> Unit) {
        Row(Modifier.fillMaxWidth().clip(Corner).clickable(onClick = onClick).padding(vertical = 10.dp)) {
            Column {
                Text(title, style = Type.RowTitle, color = p.ink)
                Text(subtitle, style = Type.RowSubtitle, color = p.inkMuted)
            }
        }
    }
    Column {
        Action("Export", "Playlists, likes and listening to a file") { export.launch("verza-library.json") }
        Action("Import", "Merge in a backup, from this app or the old one") { import.launch(arrayOf("application/json", "text/plain", "*/*")) }
        Action("Save music to", if (folder.isBlank()) "Music/Verza" else (android.net.Uri.parse(folder).lastPathSegment?.substringAfter(':') ?: "A folder you chose")) { chooseFolder.launch(null) }
        Action("Reset listening stats", "Delete your play history, for Your Sound and the mixes") { resetting = true }
        note?.let { Text(it, style = Type.RowSubtitle, color = p.accent) }
    }
    if (resetting) androidx.compose.material3.AlertDialog(
        onDismissRequest = { resetting = false },
        title = { Text("Reset listening stats?") },
        text = { Text("Your play history goes. Likes, playlists and downloads stay.") },
        confirmButton = { TextButton(onClick = { resetting = false; scope.launch { graph.db.stats().clearAll(); note = "Listening stats reset" } }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { resetting = false }) { Text("Cancel") } },
    )
}
