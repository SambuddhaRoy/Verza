package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.CuratedMix
import com.verza.data.HomeFeedBuilder
import com.verza.data.MixesRepository
import com.verza.data.PreferencesRepository
import com.verza.innertube.models.HomeSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Content(val sections: List<HomeSection>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val builder: HomeFeedBuilder,
    mixesRepository: MixesRepository,
    prefs: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Verza's on-device curated mixes (Daylist / Discover / Release radar) for the "Made for you" row. */
    val mixes: StateFlow<List<CuratedMix>> = mixesRepository.mixes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Reload on launch and whenever the account changes (sign in/out), so the feed
        // switches between anonymous and personalized automatically.
        viewModelScope.launch {
            prefs.cookieFlow.distinctUntilChanged().collect { load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            // Show what is already on disk before waiting on the network. Home used to sit on a
            // spinner until the slowest of six calls returned, over content that was ready
            // immediately — which is most of what made opening the app feel slow.
            val local = runCatching { builder.localSections() }.getOrDefault(emptyList())
            _state.value = when {
                local.isNotEmpty() -> HomeUiState.Content(local)
                _state.value is HomeUiState.Content -> _state.value
                else -> HomeUiState.Loading
            }
            builder.build()
                .onSuccess { sections ->
                    if (sections.isNotEmpty()) {
                        _state.value = HomeUiState.Content(sections)
                    } else if (local.isEmpty()) {
                        _state.value = HomeUiState.Empty
                    }
                }
                .onFailure {
                    // Offline with a library already on screen is not an error state — keep the
                    // local sections rather than replacing them with a failure message.
                    if (local.isEmpty()) {
                        _state.value = HomeUiState.Error(it.message ?: "Couldn't load home")
                    }
                }
        }
    }
}
