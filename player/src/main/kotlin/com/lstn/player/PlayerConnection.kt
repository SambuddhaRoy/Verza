package com.lstn.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.guava.await

// ── Playback state snapshots ──────────────────────────────────────────────────

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentItem: MediaItem? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<QueueItem> = emptyList(),
    val currentIndex: Int = -1,
)

/** A lightweight, UI-friendly snapshot of one item in the playback queue. */
data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
)

// ── Connection wrapper ────────────────────────────────────────────────────────

class PlayerConnection(context: Context) {

    private val sessionToken = SessionToken(
        context,
        ComponentName(context, MusicService::class.java),
    )

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) = syncState()
        override fun onIsPlayingChanged(isPlaying: Boolean) = syncState()
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) = syncState()
        override fun onShuffleModeEnabledChanged(enabled: Boolean) = syncState()
        override fun onRepeatModeChanged(repeatMode: Int) = syncState()
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) = syncState()
    }

    fun connect(context: Context, onConnected: () -> Unit = {}) {
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = future.get().also { ctrl ->
                ctrl.addListener(playerListener)
                syncState()
            }
            onConnected()
        }, MoreExecutors.directExecutor())
    }

    /** True when the underlying player currently has no items queued. */
    val isQueueEmpty: Boolean
        get() = (controller?.mediaItemCount ?: 0) == 0

    /** Restores a persisted queue without auto-playing (resumes paused at [positionMs]). */
    fun restoreQueue(items: List<MediaItem>, startIndex: Int, positionMs: Long) {
        val ctrl = controller ?: return
        if (items.isEmpty()) return
        ctrl.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), positionMs)
        ctrl.prepare()
        // Intentionally not play() — restore in a paused state.
    }

    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    // ── Playback controls ─────────────────────────────────────────────────────

    fun play() = controller?.play()
    fun pause() = controller?.pause()
    fun togglePlay() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)
    fun seekToNext() = controller?.seekToNext()
    fun seekToPrevious() = controller?.seekToPrevious()
    fun setShuffleEnabled(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }
    fun cycleRepeatMode() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF  -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL  -> Player.REPEAT_MODE_ONE
                else                    -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setQueue(items: List<MediaItem>, startIndex: Int = 0) {
        controller?.apply {
            setMediaItems(items, startIndex, 0L)
            prepare()
            play()
        }
    }

    fun addToQueue(item: MediaItem) = controller?.addMediaItem(item)

    /** Inserts [item] directly after the current track, so it plays next. */
    fun addNext(item: MediaItem) {
        controller?.let {
            val idx = (it.currentMediaItemIndex + 1).coerceAtMost(it.mediaItemCount)
            it.addMediaItem(idx, item)
        }
    }

    /** Jumps to and plays the queue entry at [index]. */
    fun playAt(index: Int) {
        controller?.let {
            if (index in 0 until it.mediaItemCount) {
                it.seekTo(index, 0L)
                it.play()
            }
        }
    }

    /** Removes the queue entry at [index]. */
    fun removeAt(index: Int) {
        controller?.let {
            if (index in 0 until it.mediaItemCount) it.removeMediaItem(index)
        }
    }

    /** Live playhead position, polled by the UI for a smooth progress bar. */
    val currentPositionMs: Long
        get() = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L

    /** Live duration of the current item (may be C.TIME_UNSET early on). */
    val currentDurationMs: Long
        get() = controller?.duration?.coerceAtLeast(0L) ?: 0L

    // ── State helpers ─────────────────────────────────────────────────────────

    private fun syncState() {
        val ctrl = controller ?: return
        val queue = (0 until ctrl.mediaItemCount).map { i ->
            val mi = ctrl.getMediaItemAt(i)
            QueueItem(
                mediaId = mi.mediaId,
                title = mi.mediaMetadata.title?.toString() ?: "",
                artist = mi.mediaMetadata.artist?.toString() ?: "",
                artworkUrl = mi.mediaMetadata.artworkUri?.toString(),
            )
        }
        _playbackState.value = PlaybackState(
            isPlaying      = ctrl.isPlaying,
            currentItem    = ctrl.currentMediaItem,
            durationMs     = ctrl.duration.coerceAtLeast(0L),
            positionMs     = ctrl.currentPosition.coerceAtLeast(0L),
            shuffleEnabled = ctrl.shuffleModeEnabled,
            repeatMode     = ctrl.repeatMode,
            queue          = queue,
            currentIndex   = ctrl.currentMediaItemIndex,
        )
    }

    // ── MediaItem factory ─────────────────────────────────────────────────────

    companion object {
        fun buildMediaItem(
            videoId: String,
            title: String,
            artist: String,
            albumArtUri: String? = null,
        ): MediaItem = MediaItem.Builder()
            .setMediaId(videoId)
            .setUri("innertube://$videoId") // resolved by a DataSource in MusicService
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .apply { albumArtUri?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                    .build()
            )
            .build()
    }
}
