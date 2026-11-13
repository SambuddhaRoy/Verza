package com.verza.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.verza.data.Connect
import com.verza.graph
import com.verza.ui.theme.Corner
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.launch

/**
 * Verza on your computers: each one this phone knows with what it's playing, its transport, and the
 * two ways to hand playback over ("play here", "play there"); then any heard on this Wi-Fi that
 * aren't added yet, and adding one by its address when it can't be heard.
 */
@Composable
fun Devices() {
    val context = LocalContext.current
    val connect = context.graph.connect
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current
    val paired by connect.paired.flow.collectAsState(initial = emptyList())
    val found by connect.found.collectAsState()
    val playing by connect.playing.collectAsState()
    val here by context.graph.player.state.collectAsState()
    /** The computer a code is being typed for; an empty host asks for the address too. */
    var adding by remember { mutableStateOf<Connect.Computer?>(null) }

    Column {
        if (paired.isEmpty() && found.isEmpty()) Text(
            "Verza on your computer can show here what it's playing, be controlled from here, and take over what this phone plays. " +
                "Turn on Verza Connect in its Settings, under devices, with this phone on the same Wi-Fi.",
            style = Type.Body, color = p.inkMuted, modifier = Modifier.padding(bottom = 8.dp),
        )
        for (computer in paired) {
            val inReach = computer.id in playing
            val there = playing[computer.id]
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(computer.name, style = Type.RowTitle, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            !inReach -> "Not on this network"
                            there?.track == null -> "Nothing playing"
                            else -> "${there.track.title} · ${there.track.artist}"
                        },
                        style = Type.RowSubtitle, color = if (there?.playing == true) p.accent else p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { scope.launch { connect.forget(computer) } }) { Icon(Icons.Rounded.Close, "Forget ${computer.name}", tint = p.inkMuted) }
            }
            if (inReach) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (there?.track != null) {
                    IconButton(onClick = { connect.tell(computer, "prev") }) { Icon(Icons.Rounded.SkipPrevious, "Previous on ${computer.name}", tint = p.ink) }
                    IconButton(onClick = { connect.tell(computer, "toggle") }) {
                        Icon(if (there.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (there.playing) "Pause ${computer.name}" else "Play on ${computer.name}", tint = p.ink, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { connect.tell(computer, "next") }) { Icon(Icons.Rounded.SkipNext, "Next on ${computer.name}", tint = p.ink) }
                }
                Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (there?.track != null) Word("play here", p.accent) { if (!connect.playHere(computer)) toast(context, "Nothing there this phone can play") }
                    if (here.song != null) Word("play there") {
                        toast(context, if (connect.playThere(computer)) "Playing on ${computer.name}" else "A file on this phone can't be played there")
                    }
                }
            }
        }
        for (computer in found.filter { f -> paired.none { it.id == f.id } }) {
            Row(Modifier.fillMaxWidth().clip(Corner).clickable { adding = computer }.padding(vertical = 12.dp)) {
                Column {
                    Text("Add ${computer.name}", style = Type.RowTitle, color = p.accent)
                    Text("On this Wi-Fi now", style = Type.RowSubtitle, color = p.inkMuted)
                }
            }
        }
        Row(Modifier.fillMaxWidth().clip(Corner).clickable { adding = Connect.Computer("", "", "") }.padding(vertical = 12.dp)) {
            Column {
                Text("Add a computer by its address", style = Type.RowTitle, color = if (paired.isEmpty() && found.isEmpty()) p.accent else p.ink)
                Text("For one that doesn't show up here", style = Type.RowSubtitle, color = p.inkMuted)
            }
        }
    }
    adding?.let { AddComputer(it, onDone = { adding = null }) }
}

/** The six-digit code the computer shows, and its address when it wasn't heard on the network. */
@Composable
private fun AddComputer(computer: Connect.Computer, onDone: () -> Unit) {
    val connect = LocalContext.current.graph.connect
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val byAddress = computer.host.isEmpty()
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (byAddress) "Add a computer" else "Add ${computer.name}") },
        text = {
            Column {
                Text("In Verza on the computer, open Settings, then devices, and choose Show code.", style = Type.Body)
                if (byAddress) OutlinedTextField(address, { address = it; problem = null }, singleLine = true, placeholder = { Text("Address, like 192.168.1.20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), shape = Corner, modifier = Modifier.padding(top = 12.dp))
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6); problem = null }, singleLine = true, placeholder = { Text("Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = Corner, modifier = Modifier.padding(top = 12.dp))
                problem?.let { Text(it, style = Type.RowSubtitle, color = androidx.compose.ui.graphics.Color(0xFFE5484D), modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && code.length == 6 && (!byAddress || address.isNotBlank()), onClick = {
                busy = true
                // "192.168.1.20" or "192.168.1.20:47126", as the computer's Settings shows it
                val typed = address.trim().removePrefix("http://").trimEnd('/')
                val host = if (byAddress) typed.substringBefore(':') else computer.host
                val port = if (byAddress) typed.substringAfter(':', "").toIntOrNull() ?: Connect.PORT else computer.port
                scope.launch {
                    connect.pair(host, port, code).onSuccess { onDone() }.onFailure { problem = it.message ?: "Couldn't add that computer" }
                    busy = false
                }
            }) { Text(if (busy) "Adding…" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
    )
}

/** [Devices] over Now Playing, from its menu. */
@Composable
fun DevicesDialog(onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Devices") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Devices() } },
        confirmButton = { TextButton(onClick = onDone) { Text("Done") } },
    )
}
