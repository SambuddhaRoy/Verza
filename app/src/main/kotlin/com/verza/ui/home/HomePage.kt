package com.verza.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.verza.Graph
import com.verza.data.Card
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.kit.Empty
import com.verza.ui.kit.ListRow
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.Masthead
import com.verza.ui.kit.SongMenu
import com.verza.ui.kit.SectionTitle
import com.verza.ui.page.Shelf
import com.verza.ui.theme.LocalPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/** The feed: null while there's nothing to show yet, empty when the network failed with no saved copy. */
class HomeModel(graph: Graph) : ViewModel() {
    private val _feed = MutableStateFlow<List<Section>?>(null)
    val feed: StateFlow<List<Section>?> = _feed.asStateFlow()

    init {
        viewModelScope.launch {
            graph.saved.load("home", ListSerializer(Section.serializer())) { graph.youtube.home() }
                .catch { emit(emptyList()) }
                .collect { _feed.value = it }
        }
    }
}

/**
 * Home: what you played last, then YouTube Music's shelves. Drawn from the last saved feed at once and
 * refreshed behind it; the old Home waited on three network calls every launch (6s to its first shelf).
 */
@Composable
fun HomePage(onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val graph = LocalContext.current.graph
    val model: HomeModel = viewModel(factory = viewModelFactory { initializer { HomeModel(graph) } })
    val feed by model.feed.collectAsState()
    val recent by graph.library.recent.collectAsState(initial = emptyList())
    val p = LocalPalette.current

    val shelves = buildList {
        if (recent.isNotEmpty()) add(Section("recently played", recent.take(20).map { Card(it.title, it.artist, it.art, song = it) }))
        addAll(feed.orEmpty().map { it.copy(title = it.title.lowercase()) })
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp + LocalDockHeight.current)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Masthead("for you", dateline(), Modifier.weight(1f))
                    IconButton(onClick = onSettings, modifier = Modifier.padding(end = 8.dp)) { Icon(Icons.Rounded.Settings, "Settings", tint = p.inkMuted) }
                }
            }
            for (section in shelves) {
                item(key = "t" + section.title) { SectionTitle(section.title, Modifier.padding(horizontal = 20.dp)) }
                // Songs read as a list, four of them; albums, playlists and mixes as covers across.
                if (section.cards.all { it.song != null } && section.title != "recently played") {
                    val songs = section.cards.mapNotNull { it.song }
                    items(section.cards.take(4), key = { section.title + it.song!!.id }) { card ->
                        ListRow(card.title, card.subtitle, art = card.art?.let { artAt(it, 226) }, modifier = Modifier.padding(horizontal = 20.dp),
                            onClick = { graph.player.play(songs, songs.indexOf(card.song)) }, trailing = { SongMenu(card.song!!) })
                    }
                } else item(key = "r" + section.title) { Shelf(section, onOpen) }
            }
        }
        when {
            feed == null && recent.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = p.accent)
            feed?.isEmpty() == true && recent.isEmpty() -> Empty("You're offline. Downloaded songs still play.")
        }
    }
}

/** "saturday evening". */
private fun dateline(): String {
    val now = LocalDateTime.now()
    val part = when (now.hour) { in 5..11 -> "morning"; in 12..16 -> "afternoon"; in 17..21 -> "evening"; else -> "night" }
    return now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase() + " " + part
}
