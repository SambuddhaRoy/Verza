package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.LibraryRepository
import com.verza.data.LocalMusicRepository
import com.verza.data.MusicRepository
import com.verza.data.PlaylistRepository
import com.verza.data.PreferencesRepository
import com.verza.data.db.PlaylistWithCover
import com.verza.data.db.SongEntity
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.MusicItem
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
    private val localMusicRepository: LocalMusicRepository,
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

    // ── On-device local music ────────────────────────────────────────────────────
    /** null = not scanned yet; emptyList = scanned, nothing found; non-empty = the device library. */
    private val _localSongs = MutableStateFlow<List<MusicItem>?>(null)
    val localSongs: StateFlow<List<MusicItem>?> = _localSongs.asStateFlow()
    private var localScanning = false

    /** Scans on-device music via MediaStore. Caller must hold the audio read permission. */
    fun loadLocalSongs(force: Boolean = false) {
        if (localScanning) return
        if (_localSongs.value != null && !force) return
        localScanning = true
        viewModelScope.launch {
            _localSongs.value = localMusicRepository.scan()
            localScanning = false
        }
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
