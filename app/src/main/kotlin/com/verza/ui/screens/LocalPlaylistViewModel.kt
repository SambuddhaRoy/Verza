package com.verza.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.PlaylistRepository
import com.verza.data.db.SongEntity
import com.verza.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalPlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val playlistId: Long = savedStateHandle.get<String>(Screen.LocalPlaylist.ARG)?.toLongOrNull() ?: 0L

    private val _name = MutableStateFlow<String?>(null)
    val name: StateFlow<String?> = _name.asStateFlow()

    val tracks: StateFlow<List<SongEntity>> = repository.tracksOf(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _name.value = repository.nameOf(playlistId) }
    }

    fun removeTrack(songId: String) {
        viewModelScope.launch { repository.removeTrack(playlistId, songId) }
    }

    fun rename(newName: String) {
        viewModelScope.launch {
            repository.rename(playlistId, newName)
            _name.value = newName
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch { repository.delete(playlistId) }
    }
}
