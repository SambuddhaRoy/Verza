package com.lstn.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lstn.data.LibraryRepository
import com.lstn.data.MusicRepository
import com.lstn.data.PlaylistRepository
import com.lstn.data.PreferencesRepository
import com.lstn.data.db.PlaylistWithCover
import com.lstn.data.db.SongEntity
import com.lstn.innertube.models.HomeItem
import com.lstn.innertube.models.MusicItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    libraryRepository: LibraryRepository,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    prefs: PreferencesRepository,
) : ViewModel() {

    val recentlyPlayed: StateFlow<List<SongEntity>> = libraryRepository.recentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val liked: StateFlow<List<SongEntity>> = libraryRepository.liked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloaded: StateFlow<List<SongEntity>> = libraryRepository.downloaded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** User-created local playlists, surfaced alongside the YT Music ones. */
    val localPlaylists: StateFlow<List<PlaylistWithCover>> = playlistRepository.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch { playlistRepository.create(name) }
    }

    val isSignedIn: StateFlow<Boolean> = prefs.cookieFlow
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _playlists = MutableStateFlow<List<HomeItem>>(emptyList())
    val playlists: StateFlow<List<HomeItem>> = _playlists.asStateFlow()

    // The account's server-side "Liked Music"; preferred over local likes when signed in.
    private val _accountLiked = MutableStateFlow<List<MusicItem>>(emptyList())
    val accountLiked: StateFlow<List<MusicItem>> = _accountLiked.asStateFlow()

    private val _artists = MutableStateFlow<List<HomeItem>>(emptyList())
    val artists: StateFlow<List<HomeItem>> = _artists.asStateFlow()

    init {
        // Load (or clear) the account's library as sign-in state changes.
        viewModelScope.launch {
            prefs.cookieFlow.distinctUntilChanged().collect { cookie ->
                if (cookie.isNullOrBlank()) {
                    _playlists.value = emptyList()
                    _accountLiked.value = emptyList()
                    _artists.value = emptyList()
                } else {
                    musicRepository.libraryPlaylists().onSuccess { _playlists.value = it }
                    musicRepository.accountLikedSongs().onSuccess { _accountLiked.value = it }
                    musicRepository.subscribedArtists().onSuccess { _artists.value = it }
                }
            }
        }
    }
}
