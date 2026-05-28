package com.verza.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.MusicRepository
import com.verza.data.PreferencesRepository
import com.verza.innertube.SearchFilter
import com.verza.innertube.models.HomeItem
import com.verza.ui.navigation.PendingSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data object Empty : SearchUiState
    data class Results(val items: List<HomeItem>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val prefs: PreferencesRepository,
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    var filter by mutableStateOf(SearchFilter.SONGS)
        private set

    var showSuggestions by mutableStateOf(false)
        private set

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<String>> = prefs.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // If something elsewhere staged a pending search (e.g. "Go to artist" from a row menu),
        // consume it now and kick off the search automatically.
        PendingSearch.consume()?.let { (q, f) ->
            query = q
            filter = f
            search()
        }
    }

    private val queryFlow = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val suggestions: StateFlow<List<String>> = queryFlow
        .debounce(250)
        .distinctUntilChanged()
        .mapLatest { q ->
            if (q.trim().length < 2) emptyList()
            else repository.searchSuggestions(q.trim()).getOrDefault(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        query = value
        queryFlow.value = value
        showSuggestions = value.isNotBlank()
    }

    fun onFilterChange(value: SearchFilter) {
        if (value == filter) return
        filter = value
        if (query.isNotBlank()) search()
    }

    fun applyHistory(query: String) {
        this.query = query
        search()
    }

    fun clearHistory() {
        viewModelScope.launch { prefs.clearSearchHistory() }
    }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        showSuggestions = false
        viewModelScope.launch {
            prefs.addSearchQuery(q)
            _uiState.value = SearchUiState.Loading
            repository.searchItems(q, filter)
                .onSuccess { items ->
                    _uiState.value =
                        if (items.isEmpty()) SearchUiState.Empty else SearchUiState.Results(items)
                }
                .onFailure { _uiState.value = SearchUiState.Error(it.message ?: "Search failed") }
        }
    }
}
