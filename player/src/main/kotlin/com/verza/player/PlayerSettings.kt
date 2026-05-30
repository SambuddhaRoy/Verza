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
}
