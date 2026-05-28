package com.verza.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.MusicRepository
import com.verza.innertube.models.ArtistDetail
import com.verza.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ArtistUiState {
    data object Loading : ArtistUiState
    data class Content(val detail: ArtistDetail) : ArtistUiState
    data class Error(val message: String) : ArtistUiState
}

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: MusicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val browseId: String = savedStateHandle.get<String>(Screen.Artist.ARG).orEmpty()

    private val _state = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val state: StateFlow<ArtistUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = ArtistUiState.Loading
            repository.artistPage(browseId)
                .onSuccess { _state.value = ArtistUiState.Content(it) }
                .onFailure { _state.value = ArtistUiState.Error(it.message ?: "Couldn't load") }
        }
    }
}
