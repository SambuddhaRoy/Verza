package com.verza.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton hand-off for the active ExoPlayer audio session ID.
 *
 * MediaController (the IPC-friendly client API surfaced via PlayerConnection) does not expose
 * the underlying audio session ID — but our visualizer (in the :app module) needs it to attach
 * an [android.media.audiofx.Visualizer]. Since MusicService and the rest of the app live in the
 * same process, a plain singleton suffices: MusicService writes when its ExoPlayer instance
 * reports an audio session, and PlayerConnection exposes the read-only StateFlow downstream.
 *
 * A value of 0 means "no audio session" (player not yet bound to an AudioTrack).
 */
object AudioSessionRegistry {

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    /** Only callable from within :player — MusicService updates this on Player.Listener events. */
    internal fun set(id: Int) {
        _audioSessionId.value = id
    }
}
