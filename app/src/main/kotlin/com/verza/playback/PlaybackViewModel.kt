package com.verza.playback

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.audio.AudioEffectsController
import com.verza.audio.EqConfig
import com.verza.data.ArtworkRepository
import com.verza.data.DownloadManager
import com.verza.data.LibraryRepository
import com.verza.data.LyricsRepository
import com.verza.data.MusicRepository
import com.verza.data.PreferencesRepository
import com.verza.data.SavedQueue
import com.verza.data.SavedTrack
import com.verza.data.SessionInbox
import com.verza.data.SessionShareRepository
import com.verza.data.SharedSession
import com.verza.data.SharedTrack
import com.verza.data.StatsRepository
import com.verza.data.db.SongEntity
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.MusicItem
import com.verza.player.PlaybackState
import com.verza.player.PlayerConnection
import com.verza.player.PlayerSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A "Focus" (deep-work / flow) listening block. While one is active the queue is kept topped up
 * so music never stops, and — for a timed block — playback gently fades and pauses when the time
 * is up. [endAt] is null for an open-ended session.
 */
data class FocusSession(val startedAt: Long, val endAt: Long?) {
    val openEnded: Boolean get() = endAt == null
}

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
    private val statsRepository: StatsRepository,
    private val audioEffects: AudioEffectsController,
    private val sessionShare: SessionShareRepository,
    private val lyricsRepository: LyricsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerConnection.playbackState

    /** Active audio session id; piped to the visualizer engine when glow reactivity is on. */
    val audioSessionId: StateFlow<Int> = playerConnection.audioSessionId

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

    /** Epoch-millis at which the sleep timer will pause playback, or null when no timer is set. */
    private val _sleepTimerEndAt = MutableStateFlow<Long?>(null)
    val sleepTimerEndAt: StateFlow<Long?> = _sleepTimerEndAt.asStateFlow()
    private var sleepJob: Job? = null

    /** "Gentle start" — when on, resuming playback eases the volume up over a couple of seconds. */
    private var gentleStart = false
    private var rampJob: Job? = null

    /** Active Focus/Flow session (null when none). The UI reads this for the live block indicator. */
    private val _focusSession = MutableStateFlow<FocusSession?>(null)
    val focusSession: StateFlow<FocusSession?> = _focusSession.asStateFlow()

    /**
     * One-shot: minutes focused when a session just ended (timed completion or manual stop), or null.
     * The UI shows a brief "you focused for N min" summary, then calls [consumeFocusComplete].
     */
    private val _focusComplete = MutableStateFlow<Int?>(null)
    val focusComplete: StateFlow<Int?> = _focusComplete.asStateFlow()
    private var focusJob: Job? = null

    /** A validated shared session awaiting the user's go-ahead (from an incoming verza:// link). */
    private val _pendingSharedSession = MutableStateFlow<SharedSession?>(null)
    val pendingSharedSession: StateFlow<SharedSession?> = _pendingSharedSession.asStateFlow()

    // ── Listen-time accumulation (powers "Your Sound" stats) ───────────────────
    // We tally the real time the user spends listening to each track (only while actually
    // playing) using elapsedRealtime deltas from the polling loop, then flush a PlayEvent
    // when the track changes or the VM clears. Approximating with track duration would have
    // over-counted skips; this measures actual engaged listening.
    private var accumTrackId: String? = null
    private var accumMs = 0L
    private var lastTickElapsed = 0L

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
                accumulateListen(android.os.SystemClock.elapsedRealtime())
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

        // Mirror the skip-silence preference into the player module (applied by MusicService).
        viewModelScope.launch {
            prefs.skipSilenceFlow.collect { PlayerSettings.setSkipSilence(it) }
        }

        // Track the gentle-start ("sunrise") preference.
        viewModelScope.launch {
            prefs.gentleStartFlow.collect { gentleStart = it }
        }

        // ── Sound suite: bind the equaliser/bass/loudness effects to the live audio session and
        // keep them in sync with the saved preferences. The session id changes when ExoPlayer
        // (re)creates its audio sink; rebinding re-attaches the effects to the new session.
        viewModelScope.launch {
            audioSessionId.collect { audioEffects.bind(it) }
        }
        viewModelScope.launch {
            combine(
                prefs.eqEnabledFlow,
                prefs.eqBandsFlow,
                prefs.bassStrengthFlow,
                prefs.loudnessEnabledFlow,
            ) { enabled, bands, bass, loudness ->
                EqConfig(
                    eqEnabled = enabled,
                    bandLevelsMb = bands,
                    bassStrength = bass,
                    loudnessEnabled = loudness,
                )
            }.collect { audioEffects.apply(it) }
        }

        // A shared listening session arrived via a verza:// deep link. We never auto-play it — the
        // intent filter is exported, so a link could come from anywhere. Decode + validate, then
        // surface it for explicit confirmation (see [pendingSharedSession]).
        viewModelScope.launch {
            SessionInbox.pending.collect { link ->
                if (link != null) {
                    _pendingSharedSession.value = sessionShare.decodeLink(link)
                    SessionInbox.consume()
                }
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
                    // Local tracks already carry their own album art — don't override with an
                    // iTunes guess (which would be wrong and a needless network call).
                    if (id.startsWith("content://") || id.startsWith("file://")) return@collect
                    val md = playbackState.value.currentItem?.mediaMetadata ?: return@collect
                    val title = md.title?.toString().orEmpty()
                    val artist = md.artist?.toString().orEmpty()
                    // Warm the lyrics cache in the background so opening the Lyrics screen is instant.
                    viewModelScope.launch { lyricsRepository.prefetch(title, artist, playbackState.value.durationMs) }
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
        // "Resume on open" — pick up where the user left off instead of restoring paused.
        if (prefs.resumeOnOpenFlow.first()) playerConnection.play()
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

    /** Appends an entire album/playlist to the end of the queue (plays after current content). */
    fun enqueueAll(items: List<MusicItem>) {
        if (items.isEmpty()) return
        playerConnection.addToQueue(items.map { it.toMediaItem() })
    }

    /** Adds a home card to the queue: a song directly, or an album/playlist expanded into tracks. */
    fun enqueueHomeItem(item: HomeItem) {
        if (item.isSong && item.videoId != null) {
            enqueue(MusicItem(id = item.videoId!!, title = item.title, artist = item.subtitle, thumbnailUrl = item.thumbnailUrl))
        } else {
            viewModelScope.launch {
                repository.collectionTracks(item.browseId, item.playlistId)
                    .onSuccess { tracks -> enqueueAll(tracks) }
            }
        }
    }

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

    // ── Shareable listening sessions ──────────────────────────────────────────────

    /**
     * Builds a `verza://session/...` link capturing the current queue + position so a friend can
     * pick up the same set. Returns null when nothing streamable is playing (e.g. local-only files).
     */
    fun buildSessionShareLink(): String? {
        val st = playbackState.value
        if (st.queue.isEmpty()) return null
        val tracks = st.queue.map { SharedTrack(id = it.mediaId, title = it.title, artist = it.artist, art = it.artworkUrl) }
        val session = SharedSession(
            index = st.currentIndex.coerceAtLeast(0),
            positionMs = playerConnection.currentPositionMs,
            tracks = tracks,
        )
        return sessionShare.encodeLink(session)
    }

    /** User confirmed the incoming shared session — load + start it, then clear the prompt. */
    fun acceptSharedSession() {
        _pendingSharedSession.value?.let { loadSharedSession(it) }
        _pendingSharedSession.value = null
    }

    /** User declined the incoming shared session. */
    fun dismissSharedSession() { _pendingSharedSession.value = null }

    /** Loads a decoded shared session into the player and starts it at the shared track + position. */
    private fun loadSharedSession(session: SharedSession) {
        if (session.tracks.isEmpty()) return
        val mediaItems = session.tracks.map {
            PlayerConnection.buildMediaItem(
                videoId = it.id,
                title = it.title,
                artist = it.artist,
                albumArtUri = it.art,
            )
        }
        val startIndex = session.index.coerceIn(0, mediaItems.lastIndex)
        playerConnection.setQueue(mediaItems, startIndex)
        if (session.positionMs > 0) playerConnection.seekTo(session.positionMs)
    }

    /** Builds a radio mix seeded by [videoId] and starts playing it. */
    fun startRadio(videoId: String) {
        if (videoId.isBlank()) return
        viewModelScope.launch {
            repository.radio(videoId).onSuccess { tracks ->
                if (tracks.isEmpty()) return@onSuccess
                // If the radio is seeded from the track that's *already playing*, keep it playing
                // (don't reload it from the start) and just swap the radio continuation in after it.
                val current = playbackState.value.currentItem?.mediaId
                if (current != null && current == videoId) {
                    val continuation = tracks.filter { it.id != videoId }.map { it.toMediaItem() }
                    playerConnection.replaceUpcoming(continuation)
                } else {
                    playSongs(tracks, 0)
                }
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

    fun togglePlay() {
        val wasPlaying = playbackState.value.isPlaying
        playerConnection.togglePlay()
        if (wasPlaying) {
            // Pausing — abandon any in-progress gentle ramp and restore full volume.
            rampJob?.cancel()
            playerConnection.setVolume(1f)
        } else if (gentleStart) {
            // Resuming with "gentle start" — ease the volume up from silence.
            rampJob?.cancel()
            rampJob = viewModelScope.launch {
                val steps = 24
                for (i in 0..steps) {
                    playerConnection.setVolume(i / steps.toFloat())
                    delay(2_500L / steps)
                }
            }
        }
    }
    fun seekToNext() = playerConnection.seekToNext()
    fun seekToPrevious() = playerConnection.seekToPrevious()
    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)
    fun toggleShuffle() = playerConnection.setShuffleEnabled(!playbackState.value.shuffleEnabled)
    fun cycleRepeatMode() = playerConnection.cycleRepeatMode()

    // ── Listen accumulation ────────────────────────────────────────────────────

    /** Called every polling tick. Adds engaged-listen time to the current track, flushing on change. */
    private fun accumulateListen(nowElapsed: Long) {
        val st = playbackState.value
        val id = st.currentItem?.mediaId
        if (id != accumTrackId) {
            flushListen()
            accumTrackId = id
            accumMs = 0L
            lastTickElapsed = if (st.isPlaying) nowElapsed else 0L
            return
        }
        if (st.isPlaying) {
            if (lastTickElapsed != 0L) accumMs += (nowElapsed - lastTickElapsed)
            lastTickElapsed = nowElapsed
        } else {
            // Paused — stop counting and reset the delta anchor so the paused gap isn't tallied.
            lastTickElapsed = 0L
        }
    }

    /** Writes the accumulated listen for the current track to the stats log (if substantial). */
    private fun flushListen() {
        val id = accumTrackId
        val ms = accumMs
        accumMs = 0L
        lastTickElapsed = 0L
        if (id != null && ms >= 5_000) {
            viewModelScope.launch { statsRepository.record(id, ms) }
        }
    }

    // ── Sleep timer ─────────────────────────────────────────────────────────────

    /**
     * Arms a sleep timer that pauses playback after [durationMs], fading the volume down over the
     * final [fadeMs] ms. A short fade (the default) feels like a clean stop; a long fade is a gentle
     * "wind-down" where the music dissolves over minutes. Pass null (or <= 0) to cancel.
     */
    fun setSleepTimer(durationMs: Long?, fadeMs: Long = 4_000L) {
        sleepJob?.cancel()
        playerConnection.setVolume(1f) // undo any in-progress fade from a prior timer
        if (durationMs == null || durationMs <= 0) {
            _sleepTimerEndAt.value = null
            return
        }
        _sleepTimerEndAt.value = System.currentTimeMillis() + durationMs
        sleepJob = viewModelScope.launch {
            val fade = fadeMs.coerceIn(1_000L, durationMs)
            delay((durationMs - fade).coerceAtLeast(0L))
            // Soft volume ramp so the music dissolves rather than cutting out — finer steps for the
            // long wind-down so it's imperceptibly smooth.
            val steps = (fade / 250L).toInt().coerceIn(16, 320)
            for (i in steps downTo 0) {
                playerConnection.setVolume(i / steps.toFloat())
                delay(fade / steps)
            }
            playerConnection.pause()
            playerConnection.setVolume(1f)
            _sleepTimerEndAt.value = null
        }
    }

    /**
     * "Wind down": like a sleep timer, but the music gently dissolves over the final stretch
     * (~the last third, capped at 5 minutes) rather than a short fade — for drifting off.
     */
    fun setSleepTimerWindDown(durationMs: Long) {
        if (durationMs <= 0) return
        val fade = (durationMs / 3).coerceIn(60_000L, 5 * 60_000L)
        setSleepTimer(durationMs, fadeMs = fade)
    }

    /** Convenience: pause when the current track ends (computed from its remaining time). */
    fun setSleepTimerEndOfTrack() {
        val remaining = (playerConnection.currentDurationMs - playerConnection.currentPositionMs)
            .coerceAtLeast(0L)
        if (remaining > 0L) setSleepTimer(remaining)
    }

    /** Smoothly fades the output volume to silence over [fadeMs], pauses, then restores full volume. */
    private suspend fun fadeAndPause(fadeMs: Long) {
        val steps = 24
        for (i in steps downTo 0) {
            playerConnection.setVolume(i / steps.toFloat())
            delay(fadeMs / steps)
        }
        playerConnection.pause()
        playerConnection.setVolume(1f)
    }

    // ── Focus / Flow sessions ────────────────────────────────────────────────────

    /**
     * Begins a Focus block. [durationMs] null starts an open-ended session (ends only when the user
     * stops it). While active, the queue is kept topped up with a radio continuation so a moment of
     * silence never breaks concentration; a timed block gently fades out and pauses when time's up.
     */
    fun startFocusSession(durationMs: Long?) {
        focusJob?.cancel()
        val now = System.currentTimeMillis()
        _focusSession.value = FocusSession(startedAt = now, endAt = durationMs?.let { now + it })
        // Make sure something is actually playing when the block begins.
        if (!playbackState.value.isPlaying && !playerConnection.isQueueEmpty) playerConnection.play()

        var lastSeed: String? = null
        focusJob = viewModelScope.launch {
            while (isActive) {
                val session = _focusSession.value ?: break
                val st = playbackState.value

                // Endless flow: when only a couple of tracks remain, extend the queue with a radio
                // mix seeded from the current track. Skips local files (no radio) and de-dupes.
                val upcoming = st.queue.size - 1 - st.currentIndex
                val seed = st.currentItem?.mediaId
                if (upcoming in 0..2 && seed != null && !seed.startsWith("content://") && seed != lastSeed) {
                    lastSeed = seed
                    repository.radio(seed).onSuccess { tracks ->
                        val existing = st.queue.map { it.mediaId }.toHashSet()
                        val fresh = tracks.filter { it.id !in existing }
                        if (fresh.isNotEmpty()) enqueueAll(fresh)
                    }
                }

                // Timed block complete → fade, pause, surface a summary.
                val endAt = session.endAt
                if (endAt != null && System.currentTimeMillis() >= endAt) {
                    val minutes = ((System.currentTimeMillis() - session.startedAt) / 60_000L).toInt()
                    fadeAndPause(2_500L)
                    _focusSession.value = null
                    _focusComplete.value = minutes.coerceAtLeast(1)
                    break
                }
                delay(4_000L)
            }
        }
    }

    /** Ends the current Focus block early (music keeps playing); surfaces a summary if it was meaningful. */
    fun endFocusSession() {
        focusJob?.cancel()
        val session = _focusSession.value
        _focusSession.value = null
        if (session != null) {
            val minutes = ((System.currentTimeMillis() - session.startedAt) / 60_000L).toInt()
            if (minutes >= 1) _focusComplete.value = minutes
        }
    }

    /** Clears the one-shot Focus-complete summary after the UI has shown it. */
    fun consumeFocusComplete() { _focusComplete.value = null }

    override fun onCleared() {
        flushListen()
        sleepJob?.cancel()
        focusJob?.cancel()
        audioEffects.bind(0)
        playerConnection.disconnect()
    }
}
