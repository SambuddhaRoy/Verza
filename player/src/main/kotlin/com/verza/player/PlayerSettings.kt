package com.verza.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide bridge for playback options that live in the :app module's DataStore but must be
 * applied to the ExoPlayer instance inside MusicService (:player). Since :player can't depend on
 * :app, the app side pushes values in via [setSkipSilence] and MusicService observes the flow —
 * same pattern as [AudioSessionRegistry].
 */
object PlayerSettings {

    private val _skipSilence = MutableStateFlow(false)
    val skipSilence: StateFlow<Boolean> = _skipSilence.asStateFlow()

    fun setSkipSilence(enabled: Boolean) {
        _skipSilence.value = enabled
    }

    /** Seconds of fade either side of a track change. 0 turns it off. */
    private val _fadeSeconds = MutableStateFlow(0)
    val fadeSeconds: StateFlow<Int> = _fadeSeconds.asStateFlow()

    fun setFadeSeconds(seconds: Int) {
        _fadeSeconds.value = seconds.coerceIn(0, 12)
    }

    /**
     * Set while something else owns the volume, so the track fade leaves it alone.
     *
     * The sleep timer ramps the volume down over its final stretch from the app side. Both writing
     * to the same property would mean whichever ran last won, and the timer's fade would keep being
     * undone every 200ms.
     */
    private val _volumeHeldElsewhere = MutableStateFlow(false)
    val volumeHeldElsewhere: StateFlow<Boolean> = _volumeHeldElsewhere.asStateFlow()

    fun setVolumeHeldElsewhere(held: Boolean) {
        _volumeHeldElsewhere.value = held
    }
}
