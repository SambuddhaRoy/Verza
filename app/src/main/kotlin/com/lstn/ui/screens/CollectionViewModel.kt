package com.lstn.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lstn.data.MusicRepository
import com.lstn.innertube.models.CollectionDetail
import com.lstn.ui.navigation.Screen
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val collectionId: String = savedStateHandle.get<String>(Screen.Collection.ARG).orEmpty()

    private val _state = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CollectionUiState.Loading
            repository.collectionDetail(collectionId)
                .onSuccess { _state.value = CollectionUiState.Content(it) }
                .onFailure { _state.value = CollectionUiState.Error(it.message ?: "Couldn't load") }
        }
    }
}
