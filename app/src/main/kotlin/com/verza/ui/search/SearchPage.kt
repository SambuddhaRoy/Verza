package com.verza.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
        com.verza.ui.kit.Field(
            value = state.query,
            onValueChange = model::type,
            placeholder = "Songs, albums, artists",
            style = Type.Section.copy(fontSize = 22.sp, lineHeight = 28.sp),
            leading = { Icon(Icons.Rounded.Search, null, tint = p.inkMuted) },
            trailing = { if (state.query.isNotEmpty()) IconButton(onClick = { model.type("") }) { Icon(Icons.Rounded.Close, "Clear", tint = p.inkMuted) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus(); model.search() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        // The filters narrow a search, so they come with one, not before.
        if (state.query.isNotEmpty() || state.results != null) Row(
            Modifier.fillMaxWidth()
                .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(androidx.compose.ui.graphics.Brush.horizontalGradient(0.85f to Color.Black, 1f to Color.Transparent), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
                }
                .horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 48.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            for ((filter, word) in Filters) {
                Text(
                    word,
                    style = Type.Section.copy(fontSize = 24.sp),
                    color = if (filter == state.filter) p.ink else p.inkMuted,
                    modifier = Modifier.heightIn(min = 48.dp).clickable { focus.clearFocus(); model.filter(filter) }.wrapContentHeight(),
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
