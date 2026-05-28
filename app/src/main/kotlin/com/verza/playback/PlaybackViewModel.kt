package com.verza.playback

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.ArtworkRepository
import com.verza.data.DownloadManager
import com.verza.data.LibraryRepository
import com.verza.data.MusicRepository
import com.verza.data.PreferencesRepository
import com.verza.data.SavedQueue
import com.verza.data.SavedTrack
import com.verza.data.db.SongEntity
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.MusicItem
import com.verza.player.PlaybackState
import com.verza.player.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-wide playback owner. Connects to [MusicService] via [PlayerConnection], exposes the
 * live [PlaybackState], and translates [MusicItem]s into queued media. Held once at the
 * navigation root so the MiniPlayer and NowPlaying screen share a single source of truth.
 */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playerConnection: PlayerConnection,
    private val repository: MusicRepository,
    private val libraryRepository: LibraryRepository,
    private val prefs: PreferencesRepository,
    private val downloadManager: DownloadManager,
    private val artworkRepository: ArtworkRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerConnection.playbackState

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    /** IDs of liked songs, so the UI can show the current track's like state. */
    val likedIds: StateFlow<List<String>> = libraryRepository.likedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** IDs that already have a local file on disk. */
    val downloadedIds: StateFlow<List<String>> = libraryRepository.downloadedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** IDs being downloaded right now (for showing a spinner / progress hint). */
    val downloading: StateFlow<Set<String>> = downloadManager.inProgress

    /**
     * High-resolution album artwork resolved from an external metadata source (iTunes) for the
     * currently-playing track. The UI uses this in preference to the YouTube thumbnail, which is
     * often a frame from the music video rather than the real cover.
     */
    private val _currentArtworkOverride = MutableStateFlow<String?>(null)
    val currentArtworkOverride: StateFlow<String?> = _currentArtworkOverride.asStateFlow()

    init {
        playerConnection.connect(context) {
            // On (re)connect, restore the persisted queue only if the service isn't already
            // playing one (e.g. cold start after process death).
            if (playerConnection.isQueueEmpty) {
                viewModelScope.launch { restoreSavedQueue() }
            }
        }
        // Poll the playhead for a smooth progress bar; event callbacks alone are too coarse.
        viewModelScope.launch {
            var tick = 0
            while (isActive) {
                _positionMs.value = playerConnection.currentPositionMs
                // Persist position roughly every 10s while something is queued.
                if (++tick % 20 == 0) snapshotQueue()?.let { prefs.saveQueue(it) }
                delay(500)
            }
        }
        // Record play history + persist the queue whenever the track or queue changes.
        viewModelScope.launch {
            playbackState
                .map { it.currentItem?.mediaId to it.queue.size }
                .distinctUntilChanged()
                .collect {
                    currentSongEntity()?.let { libraryRepository.recordPlayed(it) }
                    snapshotQueue()?.let { prefs.saveQueue(it) }
                }
        }

        // Look up real album art for the current track via iTunes, falling back to the YT thumb.
        viewModelScope.launch {
            playbackState
                .map { it.currentItem?.mediaId }
                .distinctUntilChanged()
                .collect { id ->
                    _currentArtworkOverride.value = null
                    if (id == null) return@collect
                    val md = playbackState.value.currentItem?.mediaMetadata ?: return@collect
                    val title = md.title?.toString().orEmpty()
                    val artist = md.artist?.toString().orEmpty()
                    val better = artworkRepository.resolve(title, artist)
                    // Only apply if the track hasn't already moved on while we waited.
                    if (better != null && playbackState.value.currentItem?.mediaId == id) {
                        _currentArtworkOverride.value = better
                    }
                }
        }
    }

    private suspend fun restoreSavedQueue() {
        val saved = prefs.loadQueue() ?: return
        if (saved.tracks.isEmpty()) return
        val mediaItems = saved.tracks.map {
            PlayerConnection.buildMediaItem(it.videoId, it.title, it.artist, it.artworkUrl)
        }
        playerConnection.restoreQueue(mediaItems, saved.index, saved.positionMs)
    }

    private fun snapshotQueue(): SavedQueue? {
        val st = playbackState.value
        if (st.queue.isEmpty()) return null
        return SavedQueue(
            tracks = st.queue.map { SavedTrack(it.mediaId, it.title, it.artist, it.artworkUrl) },
            index = st.currentIndex.coerceAtLeast(0),
            positionMs = playerConnection.currentPositionMs,
        )
    }

    private fun currentSongEntity(): SongEntity? {
        val item = playbackState.value.currentItem ?: return null
        val id = item.mediaId.takeIf { it.isNotBlank() } ?: return null
        val md = item.mediaMetadata
        return SongEntity(
            id = id,
            title = md.title?.toString() ?: "Unknown",
            artist = md.artist?.toString() ?: "",
            thumbnailUrl = md.artworkUri?.toString(),
            durationMs = playbackState.value.durationMs,
            lastPlayedAt = System.currentTimeMillis(),
        )
    }

    fun toggleLikeCurrent() {
        val song = currentSongEntity() ?: return
        viewModelScope.launch { libraryRepository.toggleLike(song) }
    }

    /** Downloads the currently-playing track for offline playback. No-op if already downloaded. */
    fun downloadCurrent() {
        val entity = currentSongEntity() ?: return
        downloadManager.download(
            com.verza.innertube.models.MusicItem(
                id = entity.id,
                title = entity.title,
                artist = entity.artist,
                thumbnailUrl = entity.thumbnailUrl,
                durationMs = entity.durationMs,
            )
        )
    }

    /** Deletes the local file for the currently-playing track (and any in-flight download). */
    fun removeDownloadCurrent() {
        val id = playbackState.value.currentItem?.mediaId ?: return
        downloadManager.remove(id)
    }

    // ── Per-track actions (used by row overflow menus) ────────────────────────

    private fun MusicItem.toMediaItem() = PlayerConnection.buildMediaItem(
        videoId = id, title = title, artist = artist, albumArtUri = thumbnailUrl,
    )

    fun playNext(item: MusicItem) = playerConnection.addNext(item.toMediaItem())
    fun enqueue(item: MusicItem) = playerConnection.addToQueue(item.toMediaItem())

    fun toggleLike(item: MusicItem) {
        viewModelScope.launch {
            libraryRepository.toggleLike(
                SongEntity(
                    id = item.id,
                    title = item.title,
                    artist = item.artist,
                    thumbnailUrl = item.thumbnailUrl,
                    durationMs = item.durationMs,
                )
            )
        }
    }

    fun download(item: MusicItem) = downloadManager.download(item)

    /** Plays a home card: a song directly, or an album/playlist by expanding it into tracks. */
    fun playHomeItem(item: HomeItem) {
        if (item.isSong) {
            playSong(
                MusicItem(
                    id = item.videoId!!,
                    title = item.title,
                    artist = item.subtitle,
                    thumbnailUrl = item.thumbnailUrl,
                )
            )
        } else {
            viewModelScope.launch {
                repository.collectionTracks(item.browseId, item.playlistId)
                    .onSuccess { tracks -> if (tracks.isNotEmpty()) playSongs(tracks, 0) }
            }
        }
    }

    fun playSong(item: MusicItem) = playSongs(listOf(item), 0)

    fun playSongs(items: List<MusicItem>, startIndex: Int) {
        val mediaItems = items.map {
            PlayerConnection.buildMediaItem(
                videoId = it.id,
                title = it.title,
                artist = it.artist,
                albumArtUri = it.thumbnailUrl,
            )
        }
        playerConnection.setQueue(mediaItems, startIndex)
    }

    /** Builds a radio mix seeded by [videoId] and starts playing it. */
    fun startRadio(videoId: String) {
        if (videoId.isBlank()) return
        viewModelScope.launch {
            repository.radio(videoId).onSuccess { tracks ->
                if (tracks.isNotEmpty()) playSongs(tracks, 0)
            }
        }
    }

    /** Plays [items] with shuffle enabled, starting from a random track. */
    fun playShuffled(items: List<MusicItem>) {
        if (items.isEmpty()) return
        playerConnection.setShuffleEnabled(true)
        playSongs(items, items.indices.random())
    }

    fun playQueueItemAt(index: Int) = playerConnection.playAt(index)
    fun removeQueueItemAt(index: Int) = playerConnection.removeAt(index)

    fun togglePlay() = playerConnection.togglePlay()
    fun seekToNext() = playerConnection.seekToNext()
    fun seekToPrevious() = playerConnection.seekToPrevious()
    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)
    fun toggleShuffle() = playerConnection.setShuffleEnabled(!playbackState.value.shuffleEnabled)
    fun cycleRepeatMode() = playerConnection.cycleRepeatMode()

    override fun onCleared() {
        playerConnection.disconnect()
    }
}
