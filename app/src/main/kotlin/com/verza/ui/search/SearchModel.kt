package com.verza.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.Graph
import com.verza.data.Card
import com.verza.data.youtube.Filter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class SearchState(
    val query: String = "",
    val filter: Filter = Filter.TOP,
    /** What the results are for; [query] may have changed since. */
    val searched: String = "",
    val results: List<Card>? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class SearchModel(private val graph: Graph) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    val history = graph.settings.searchHistory.flow
    private var running: Job? = null

    fun type(query: String) = _state.update { if (query.isEmpty()) SearchState(filter = it.filter) else it.copy(query = query) }

    fun filter(filter: Filter) {
        _state.update { it.copy(filter = filter) }
        if (_state.value.query.isNotBlank()) search()
    }

    fun search() {
        val q = _state.value.query.trim().ifEmpty { return }
        val filter = _state.value.filter
        running?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        running = viewModelScope.launch {
            remember(q)
            val result = runCatching { graph.youtube.search(q, filter) }
            _state.update {
                it.copy(
                    loading = false,
                    searched = q,
                    // Top results list a song and the single it came out on; the single reads as
                    // the same song twice, so it goes there (the albums filter keeps it).
                    results = result.getOrNull()?.let { r -> if (filter == Filter.TOP) withoutSinglesOfListedSongs(r) else r },
                    error = result.exceptionOrNull()?.let { e -> if (e is IOException) "You're offline. Downloaded songs still play." else "Search didn't work. Try again." },
                )
            }
        }
    }

    private suspend fun remember(q: String) {
        if (!graph.settings.saveSearchHistory.get()) return
        graph.settings.searchHistory.set((listOf(q) + graph.settings.searchHistory.get().filterNot { it.equals(q, true) }).take(20))
    }
}

/**
 * Drops a single from top results when its song by the same artist is listed: "Yellow" by Coldplay,
 * then "Yellow · Single · Coldplay · 2000" with the single's own cover.
 */
fun withoutSinglesOfListedSongs(cards: List<Card>): List<Card> {
    fun key(s: String) = s.trim().lowercase()
    val songs = cards.mapNotNull { c -> c.song?.let { key(it.title) to key(it.artist) } }.toSet()
    return cards.filterNot { c ->
        if (c.song != null || c.page?.startsWith("MPRE") != true) return@filterNot false
        val parts = c.subtitle.split(" • ").map { it.trim() }
        parts.firstOrNull()?.lowercase() in setOf("single", "ep") && parts.drop(1).any { (key(c.title) to key(it)) in songs }
    }
}
