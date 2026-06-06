package com.verza.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import com.verza.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One equaliser band: its centre frequency (Hz) for labelling. */
data class EqBand(val centerFreqHz: Int)

/**
 * Device equaliser layout — band count, the level range each band accepts (millibels), and the
 * centre frequency of each band. Read once from the platform; constant for the device.
 */
data class EqMetadata(
    val bandCount: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int,
    val bands: List<EqBand>,
) {
    companion object {
        /** Conventional 5-band, ±15 dB layout used when the platform query fails. */
        val Default = EqMetadata(
            bandCount = 5,
            minLevelMb = -1500,
            maxLevelMb = 1500,
            bands = listOf(60, 230, 910, 3600, 14000).map { EqBand(it) },
        )
    }
}

/** The user's chosen sound settings, applied to whatever audio session is currently live. */
data class EqConfig(
    val eqEnabled: Boolean = false,
    val bandLevelsMb: List<Int> = emptyList(),
    val bassStrength: Int = 0,        // 0..1000 (0 = off)
    val loudnessEnabled: Boolean = false,
)

/**
 * Owns the platform audio effects (Equalizer, BassBoost, LoudnessEnhancer) bound to ExoPlayer's
 * audio session. Lives in :app — the session id is published by the player and read here, so no
 * effect plumbing crosses into the :player module.
 *
 * Singleton so the playback owner (which drives [bind] + [apply] from the session id and the saved
 * preferences) and the Equalizer screen (which reads [metadata]) share one instance.
 *
 * Notes on scope vs. crossfade: true track-overlap crossfade needs a dual-decoder rewrite of the
 * single-ExoPlayer pipeline and would collide with the sleep-timer / wind-down volume fades, so it
 * is deliberately out of scope here. Gapless playback is already automatic in ExoPlayer for
 * compatible streams. This suite covers the tonal half of "sound quality": EQ, bass, and loudness.
 */
@Singleton
class AudioEffectsController @Inject constructor() {

    private var sessionId = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private var lastConfig = EqConfig()

    private val _metadata = MutableStateFlow(queryMetadata())
    /** Device equaliser layout, for the UI to render the right bands. */
    val metadata: StateFlow<EqMetadata> = _metadata.asStateFlow()

    /** (Re)binds the effects to [newSessionId]. Pass 0 to release. Safe to call repeatedly. */
    @Synchronized
    fun bind(newSessionId: Int) {
        if (newSessionId == sessionId && (equalizer != null || newSessionId == 0)) return
        releaseEffects()
        sessionId = newSessionId
        if (newSessionId == 0) return
        try {
            equalizer = Equalizer(EFFECT_PRIORITY, newSessionId)
            bassBoost = BassBoost(EFFECT_PRIORITY, newSessionId)
            loudness = LoudnessEnhancer(newSessionId)
            equalizer?.let { _metadata.value = readMetadata(it) }
            applyInternal(lastConfig)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.e(TAG, "AudioFx init failed: ${t.javaClass.simpleName}: ${t.message}", t)
            releaseEffects()
        }
    }

    /** Applies [config] to the live effects (and remembers it for the next [bind]). */
    @Synchronized
    fun apply(config: EqConfig) {
        lastConfig = config
        applyInternal(config)
    }

    private fun applyInternal(config: EqConfig) {
        val eq = equalizer
        runCatching {
            if (eq != null) {
                eq.enabled = config.eqEnabled
                if (config.eqEnabled && config.bandLevelsMb.isNotEmpty()) {
                    val range = eq.bandLevelRange
                    val min = range[0].toInt()
                    val max = range[1].toInt()
                    val n = eq.numberOfBands.toInt()
                    for (i in 0 until minOf(n, config.bandLevelsMb.size)) {
                        val lvl = config.bandLevelsMb[i].coerceIn(min, max).toShort()
                        eq.setBandLevel(i.toShort(), lvl)
                    }
                }
            }
        }
        runCatching {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    val on = config.bassStrength > 0
                    bb.enabled = on
                    if (on) bb.setStrength(config.bassStrength.coerceIn(0, 1000).toShort())
                }
            }
        }
        runCatching {
            loudness?.let { le ->
                le.enabled = config.loudnessEnabled
                if (config.loudnessEnabled) le.setTargetGain(LOUDNESS_TARGET_MB)
            }
        }
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        bassBoost = null
        loudness = null
    }

    /**
     * Reads the device band layout from a throwaway equaliser on the global output mix (session 0).
     * It is never enabled, so it leaves audio untouched — we only read the constant band metadata.
     */
    private fun queryMetadata(): EqMetadata = try {
        val probe = Equalizer(EFFECT_PRIORITY, 0)
        val md = readMetadata(probe)
        probe.release()
        md
    } catch (t: Throwable) {
        EqMetadata.Default
    }

    private fun readMetadata(eq: Equalizer): EqMetadata {
        val n = eq.numberOfBands.toInt()
        val range = eq.bandLevelRange
        val bands = (0 until n).map { EqBand(centerFreqHz = eq.getCenterFreq(it.toShort()) / 1000) }
        return EqMetadata(
            bandCount = n,
            minLevelMb = range[0].toInt(),
            maxLevelMb = range[1].toInt(),
            bands = bands,
        )
    }

    companion object {
        private const val TAG = "VerzaAudioFx"
        private const val EFFECT_PRIORITY = 0
        private const val LOUDNESS_TARGET_MB = 700 // gentle, headroom-safe lift for quiet tracks
    }
}
