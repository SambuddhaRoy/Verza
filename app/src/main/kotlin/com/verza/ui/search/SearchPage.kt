package com.verza.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.verza.data.Card
import com.verza.data.artAt
import com.verza.data.youtube.Filter
import com.verza.graph
import com.verza.ui.kit.Empty
import com.verza.ui.kit.ListRow
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.Masthead
import com.verza.ui.kit.SongMenu
import com.verza.ui.theme.Corner
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type

private val Filters = listOf(Filter.TOP to "top results", Filter.SONGS to "songs", Filter.ALBUMS to "albums", Filter.ARTISTS to "artists", Filter.PLAYLISTS to "playlists")

/** Search: the field, the filters as words, then results, or your recent searches before any. */
@Composable
fun SearchPage(onOpen: (String) -> Unit) {
    val graph = LocalContext.current.graph
    val model: SearchModel = viewModel(factory = viewModelFactory { initializer { SearchModel(graph) } })
    val state by model.state.collectAsState()
    val history by model.history.collectAsState(initial = emptyList())
    val p = LocalPalette.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize()) {
        Masthead("search", "explore")
        OutlinedTextField(
            value = state.query,
            onValueChange = model::type,
            singleLine = true,
            placeholder = { Text("Songs, albums, artists", style = Type.Body) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (state.query.isNotEmpty()) IconButton(onClick = { model.type("") }) { Icon(Icons.Rounded.Close, "Clear") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus(); model.search() }),
            shape = Corner,
            textStyle = Type.RowTitle,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            for ((filter, word) in Filters) {
                Text(
                    word,
                    style = Type.Section.copy(fontSize = 24.sp),
                    color = if (filter == state.filter) p.ink else p.inkMuted,
                    modifier = Modifier.clickable { focus.clearFocus(); model.filter(filter) }.padding(vertical = 4.dp),
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center).padding(bottom = LocalDockHeight.current), color = p.accent)
                state.error != null -> Empty(state.error!!)
                state.results != null && state.results!!.isEmpty() -> Empty("Nothing found for \"${state.searched}\"")
                state.results != null -> Results(state.results!!, onSong = { graph.player.playWithRadio(it) }, onOpen = onOpen)
                state.query.isEmpty() && history.isNotEmpty() -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalDockHeight.current)) {
                    items(history) { q -> ListRow(title = q, subtitle = null, icon = Icons.Rounded.History, onClick = { focus.clearFocus(); model.type(q); model.search() }) }
                }
                else -> Empty("Search for any song, album or artist", Icons.Rounded.Search)
            }
        }
    }
}

@Composable
private fun Results(cards: List<Card>, onSong: (com.verza.data.Song) -> Unit, onOpen: (String) -> Unit) {
    val playing by LocalContext.current.graph.player.state.collectAsState()
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalDockHeight.current)) {
        items(cards, key = { it.song?.id ?: it.page ?: it.title }) { card ->
            val song = card.song
            ListRow(
                title = card.title,
                subtitle = card.subtitle,
                art = card.art?.let { artAt(it, 226) },
                // Artists round, records square.
                round = card.page?.startsWith("UC") == true,
                playing = song != null && song.id == playing.song?.id,
                onClick = { if (song != null) onSong(song) else card.page?.let(onOpen) },
                trailing = song?.let { s -> { SongMenu(s) } },
            )
        }
    }
}
