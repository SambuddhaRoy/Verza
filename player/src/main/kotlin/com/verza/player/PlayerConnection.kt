package com.verza.player

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

    /** Audio session id of the currently-playing ExoPlayer; 0 when no session is active. */
    val audioSessionId: StateFlow<Int> = AudioSessionRegistry.audioSessionId

    /**
     * Consecutive failures, so a queue of unplayable tracks stops instead of racing to the end.
     * Reset by the first item that actually starts.
     */
    private var consecutiveErrors = 0

    private val playerListener = object : Player.Listener {
        /**
         * A track that will not resolve skips to the next one.
         *
         * Nothing handled this before: ExoPlayer went to STATE_IDLE and the session simply stopped,
         * with a full queue still loaded and a play button that did nothing, because recovering
         * needs prepare() and no one was calling it. One region-blocked or expired track ended the
         * listening session.
         */
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val ctrl = controller ?: return
            consecutiveErrors++
            if (consecutiveErrors > MAX_CONSECUTIVE_ERRORS || !ctrl.hasNextMediaItem()) {
                consecutiveErrors = 0
                syncState()
                return
            }
            ctrl.seekToNextMediaItem()
            ctrl.prepare()
            ctrl.play()
        }

        override fun onPlaybackStateChanged(playbackState: Int) = syncState()
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) consecutiveErrors = 0
            syncState()
        }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) = syncState()
        override fun onShuffleModeEnabledChanged(enabled: Boolean) = syncState()
        override fun onRepeatModeChanged(repeatMode: Int) = syncState()
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) = syncState()
    }

    fun connect(context: Context, onConnected: () -> Unit = {}) {
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            // buildAsync completes exceptionally when the session service cannot be bound or
            // started — resuming into a background-start restriction, or the service having been
            // killed. This runs on a direct executor, so an uncaught get() took the process down
            // rather than leaving the app merely disconnected.
            val ctrl = runCatching { future.get() }.getOrNull()
            if (ctrl == null) {
                _playbackState.value = PlaybackState()
                return@addListener
            }
            controller = ctrl.also {
                it.addListener(playerListener)
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

    /**
     * Playback speed, pitch-corrected.
     *
     * Deliberately not persisted. A music player that quietly resumes at 1.5x three days later is
     * indistinguishable from a broken one, so this lasts as long as the session does.
     */
    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed.coerceIn(0.25f, 3f))
    }

    /** The speed the player is running at, 1.0 when untouched. */
    val speed: Float get() = controller?.playbackParameters?.speed ?: 1f

    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    // ── Playback controls ─────────────────────────────────────────────────────

    fun play() = controller?.play()
    fun pause() = controller?.pause()
    /** Sets output volume 0f..1f — used by the sleep-timer fade-out. */
    fun setVolume(volume: Float) { controller?.volume = volume.coerceIn(0f, 1f) }
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

    /** Appends a batch (e.g. a whole album/playlist) to the end of the queue. */
    fun addToQueue(items: List<MediaItem>) {
        if (items.isNotEmpty()) controller?.addMediaItems(items)
    }

    /**
     * Keeps the currently-playing item exactly as it is (no reload), drops the rest of the queue,
     * and appends [upcoming] after it. Used to start a radio from the current song without
     * restarting it. Falls back to a fresh queue when nothing is playing.
     */
    fun replaceUpcoming(upcoming: List<MediaItem>) {
        val ctrl = controller ?: return
        val cur = ctrl.currentMediaItemIndex
        if (cur < 0 || ctrl.mediaItemCount == 0) {
            if (upcoming.isNotEmpty()) setQueue(upcoming)
            return
        }
        // Trim everything after the current item, then everything before it, so the current item
        // ends up at index 0 still playing; then append the new continuation.
        if (ctrl.mediaItemCount > cur + 1) ctrl.removeMediaItems(cur + 1, ctrl.mediaItemCount)
        if (cur > 0) ctrl.removeMediaItems(0, cur)
        if (upcoming.isNotEmpty()) ctrl.addMediaItems(upcoming)
    }

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
        /** Stop after this many failures in a row rather than racing to the end of a dead queue. */
        private const val MAX_CONSECUTIVE_ERRORS = 3

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
                    // Tagging the item as playable music (not an untyped media item) lets the
                    // system + OEM surfaces — lock screen, OnePlus hole-punch popout, always-on
                    // display — treat it as a proper "now playing" card with the cover art.
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .apply { albumArtUri?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                    .build()
            )
            .build()
    }
}
