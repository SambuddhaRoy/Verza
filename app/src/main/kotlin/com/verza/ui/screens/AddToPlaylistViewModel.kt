package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.PlaylistRepository
import com.verza.data.db.PlaylistWithCover
import com.verza.innertube.models.MusicItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the "Add to playlist" bottom sheet — lists local playlists and inserts on tap. */
@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistWithCover>> = repository.all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun add(playlistId: Long, item: MusicItem) {
        viewModelScope.launch { repository.addTrack(playlistId, item) }
    }

    fun create(name: String, withTrack: MusicItem? = null) {
        viewModelScope.launch {
            val id = repository.create(name)
            if (withTrack != null) repository.addTrack(id, withTrack)
        }
    }
}
