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
import com.verza.data.mixes.HomeShelves
import com.verza.graph
import com.verza.ui.kit.Empty
import com.verza.ui.kit.ListRow
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.Masthead
import com.verza.ui.kit.SongMenu
import com.verza.ui.kit.SectionTitle
import com.verza.ui.page.Shelf
import com.verza.ui.theme.LocalPalette
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Home's shelves that come from the play log: [local] at once (null until read), and the "because you
 * listened to" radios, from their last saved copy and then refreshed.
 */
class HomeModel(graph: Graph) : ViewModel() {
    private val _local = MutableStateFlow<HomeShelves.Shelves?>(null)
    val local: StateFlow<HomeShelves.Shelves?> = _local.asStateFlow()
    private val _because = MutableStateFlow<List<Section>>(emptyList())
    val because: StateFlow<List<Section>> = _because.asStateFlow()

    init {
        viewModelScope.launch {
            val shelves = withContext(Dispatchers.IO) { HomeShelves.from(graph.db.stats().playsWithSongs(), System.currentTimeMillis()) }
            _local.value = shelves
            graph.saved.load("because", ListSerializer(Section.serializer())) {
                val radios = coroutineScope {
                    shelves.seeds.map { seed ->
                        async {
                            runCatching {
                                val songs = graph.youtube.radio(seed.songId).filter { it.id != seed.songId }.take(20)
                                Section("because you listened to ${seed.artist}", songs.map { Card(it.title, it.artist, it.art, song = it) })
                            }.getOrNull()
                        }
                    }.awaitAll()
                }
                // Every lookup failed: offline. Keep the saved copy rather than emptying the shelves.
                if (shelves.seeds.isNotEmpty() && radios.all { it == null }) error("offline")
                radios.filterNotNull().filter { it.cards.size >= HomeShelves.MIN_SONGS }
            }.catch { }.collect { _because.value = it }
        }
    }
}

/**
 * Home: everything on it comes from what you play. What you played last, the mixes made from your
 * listening, what you have on repeat, the radios of the artists you've played most lately, your liked
 * songs, and favourites you haven't played in a while. YouTube's own home feed isn't used: signed out,
 * and for most accounts signed in, it's charts and trending.
 */
@Composable
fun HomePage(onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val graph = LocalContext.current.graph
    val model: HomeModel = viewModel(factory = viewModelFactory { initializer { HomeModel(graph) } })
    val local by model.local.collectAsState()
    val because by model.because.collectAsState()
    val recent by graph.library.recent.collectAsState(initial = emptyList())
    val liked by graph.library.liked.collectAsState(initial = emptyList())
    val mixes by graph.mixes.mixes.collectAsState()
    val p = LocalPalette.current
    // A different handful of liked songs each day.
    val likedToday = remember(liked) { liked.shuffled(kotlin.random.Random(java.time.LocalDate.now().toEpochDay())).take(20) }

    fun songs(title: String, list: List<com.verza.data.Song>) = Section(title, list.map { Card(it.title, it.artist, it.art, song = it) })
    val shelves = buildList {
        if (recent.isNotEmpty()) add(songs("recently played", recent.take(20)))
        if (mixes.isNotEmpty()) add(Section("made for you", mixes.map { Card(it.title, it.subtitle, it.items.firstOrNull { i -> i.thumbnailUrl != null }?.thumbnailUrl, page = "mix:" + it.id) }))
        local?.onRepeat?.takeIf { it.isNotEmpty() }?.let { add(songs("on repeat", it)) }
        because.getOrNull(0)?.let(::add)
        if (liked.size >= 8) add(songs("from your liked songs", likedToday))
        because.drop(1).forEach(::add)
        local?.forgotten?.takeIf { it.isNotEmpty() }?.let { add(songs("forgotten favourites", it)) }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp + LocalDockHeight.current)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Masthead("for you", dateline(), Modifier.weight(1f))
                    IconButton(onClick = onSettings, modifier = Modifier.padding(end = 8.dp)) { Icon(Icons.Rounded.Settings, "Settings", tint = p.inkMuted) }
                }
            }
            item { UpdateLine() }
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
            local == null && recent.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = p.accent)
            shelves.isEmpty() -> Empty("Play some music and Home fills up with songs picked from what you listen to.")
        }
    }
}

/** "saturday evening". */
private fun dateline(): String {
    val now = LocalDateTime.now()
    val part = when (now.hour) { in 5..11 -> "morning"; in 12..16 -> "afternoon"; in 17..21 -> "evening"; else -> "night" }
    return now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase() + " " + part
}

/** "Verza 2.0.1 is ready · update", when a newer release is downloaded. */
@Composable
private fun UpdateLine() {
    val context = LocalContext.current
    val updates = context.graph.updates
    val ready by updates.ready.collectAsState()
    val release = ready ?: return
    androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Text("Verza ${release.version} is ready", style = com.verza.ui.theme.Type.RowTitle, color = LocalPalette.current.ink, modifier = Modifier.weight(1f))
        com.verza.ui.kit.Word("update", LocalPalette.current.accent) { install(context, release) }
    }
}

/** Installs [release], first sending the listener to allow it if Android doesn't yet. */
fun install(context: android.content.Context, release: com.verza.data.Release) {
    val updates = context.graph.updates
    if (updates.allowed()) updates.install(release)
    else context.startActivity(
        android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:${context.packageName}"))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
