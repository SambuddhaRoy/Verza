package com.verza.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.LocalGrouping
import com.verza.data.LocalMusicRepository
import com.verza.data.MusicRepository
import com.verza.innertube.models.CollectionDetail
import com.verza.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    data class Content(val detail: CollectionDetail) : CollectionUiState
    data class Error(val message: String) : CollectionUiState
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val local: LocalMusicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val collectionId: String = savedStateHandle.get<String>(Screen.Collection.ARG).orEmpty()

    /** An album or playlist made from the phone's own files. */
    val isLocal: Boolean = LocalGrouping.isLocalId(collectionId)

    private val _state = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CollectionUiState.Loading
            // An album or playlist from the phone's own music.
            if (isLocal) {
                val detail = local.collection(collectionId)
                _state.value = if (detail == null) CollectionUiState.Error("Couldn't find that on this phone")
                else CollectionUiState.Content(detail)
                return@launch
            }
            repository.collectionDetail(collectionId)
                .onSuccess { _state.value = CollectionUiState.Content(it) }
                .onFailure { _state.value = CollectionUiState.Error(it.message ?: "Couldn't load") }
        }
    }
}
