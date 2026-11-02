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

    /** Lossless sources, asked in this order before YouTube. See [AddonClient]. */
    private val _addons = MutableStateFlow<List<AddonSource>>(emptyList())
    val addons: StateFlow<List<AddonSource>> = _addons.asStateFlow()

    fun setAddons(sources: List<AddonSource>) {
        _addons.value = sources
    }

    /** Whether sources are used on a metered connection. A FLAC is several times the data. */
    private val _addonsOnMobileData = MutableStateFlow(false)
    val addonsOnMobileData: StateFlow<Boolean> = _addonsOnMobileData.asStateFlow()

    fun setAddonsOnMobileData(allowed: Boolean) {
        _addonsOnMobileData.value = allowed
    }

    /**
     * When the service last took over a source's failure by replaying the song from YouTube. The
     * controller sees the same error and would otherwise skip the song as well.
     */
    @Volatile private var addonRetryAt = 0L

    fun noteAddonRetry() {
        addonRetryAt = System.currentTimeMillis()
    }

    /** Whether the error just seen is one the service is already handling. Answers once. */
    fun takeAddonRetry(): Boolean {
        val handled = System.currentTimeMillis() - addonRetryAt < 3_000L
        addonRetryAt = 0L
        return handled
    }
}
