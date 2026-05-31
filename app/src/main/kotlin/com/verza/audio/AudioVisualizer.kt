package com.verza.audio

import android.media.audiofx.Visualizer
import android.util.Log
import com.verza.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Tri-band intensity snapshot produced by [AudioVisualizer].
 *
 * Each band is a smoothed, normalized [0, 1] energy reading suitable for driving visual
 * parameters directly (alpha, scale, position offsets). The raw FFT magnitudes are first
 * log-compressed (audio is exponentially perceived), then split into three frequency ranges,
 * then passed through a low-pass filter so the values feel soft and fluid instead of jittery.
 *
 *  - [bass]   60–250 Hz   — kicks, basslines, low-end body
 *  - [mid]    250–2000 Hz — vocals, snares, the song's melody
 *  - [treble] 2 kHz+      — hi-hats, cymbals, air
 *  - [energy] weighted average across all three; useful when you want a single "loudness" knob
 */
data class VisualizerSignal(
    val bass: Float = 0f,
    val mid: Float = 0f,
    val treble: Float = 0f,
    val energy: Float = 0f,
)

/**
 * Wraps [android.media.audiofx.Visualizer] and exposes a [signal] [StateFlow] of smoothed
 * band energies. Must be created with a valid `audioSessionId` (from ExoPlayer); pass 0 and it
 * silently no-ops. Holds RECORD_AUDIO at the system level — caller is responsible for ensuring
 * the runtime permission is granted before constructing.
 *
 * Lifecycle:
 *  - [start] enables the capture callback. Idempotent.
 *  - [stop]  releases the native Visualizer. Must be called before re-creating with a new
 *            session id, otherwise the system handle leaks.
 *
 * Capture rate is half the system maximum (typically ~30 Hz on most devices) — at 60-fps
 * rendering we'd just see duplicate frames, and 30 Hz is plenty for "subtle" motion.
 */
class AudioVisualizer(private val audioSessionId: Int) {

    private val _signal = MutableStateFlow(VisualizerSignal())
    val signal: StateFlow<VisualizerSignal> = _signal.asStateFlow()

    private var visualizer: Visualizer? = null

    // Exponentially-smoothed band energies. ALPHA = 0.18 gives a ~5-frame time constant at 30 Hz,
    // which feels "soft" without losing the song's pulse. Lower = smoother but laggier.
    private var smoothBass = 0f
    private var smoothMid = 0f
    private var smoothTreble = 0f

    /** Starts the visualizer. Safe to call repeatedly; only the first call has effect. */
    fun start() {
        if (audioSessionId == 0) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Not starting visualizer: audioSessionId is 0")
            return
        }
        if (visualizer != null) return
        try {
            visualizer = Visualizer(audioSessionId).apply {
                // Bigger capture size = more frequency resolution. Use the maximum the system
                // allows so the band splits are clean.
                val sizeRange = Visualizer.getCaptureSizeRange()
                captureSize = sizeRange[1]
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer,
                            waveform: ByteArray,
                            samplingRate: Int,
                        ) = Unit // unused; we only care about FFT

                        override fun onFftDataCapture(
                            v: Visualizer,
                            fft: ByteArray,
                            samplingRate: Int,
                        ) {
                            processFft(fft, samplingRate)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2, // ~30 Hz on most devices
                    /* waveform = */ false,
                    /* fft     = */ true,
                )
                enabled = true
            }
            if (BuildConfig.DEBUG) Log.i(TAG, "Visualizer started for session $audioSessionId")
        } catch (t: Throwable) {
            // Most common reasons:
            //   - RECORD_AUDIO not granted (UnsupportedOperationException / RuntimeException)
            //   - Audio session id no longer valid (IllegalArgumentException)
            //   - Another process holds the Visualizer for this session
            if (BuildConfig.DEBUG) Log.e(TAG, "Visualizer init failed: ${t.javaClass.simpleName}: ${t.message}", t)
            release()
        }
    }

    /** Stops + releases the underlying Visualizer. Idempotent. */
    fun stop() = release()

    private fun release() {
        runCatching { visualizer?.enabled = false }
        runCatching { visualizer?.release() }
        visualizer = null
        // Decay the signal so any consumer animating off it lands cleanly at zero.
        _signal.value = VisualizerSignal()
        smoothBass = 0f
        smoothMid = 0f
        smoothTreble = 0f
    }

    /**
     * Converts an FFT byte array (alternating real/imaginary pairs, see the Visualizer docs)
     * into per-bin magnitudes, splits those into bass/mid/treble bands by frequency, smooths,
     * and emits a [VisualizerSignal].
     */
    private fun processFft(fft: ByteArray, samplingRate: Int) {
        // FFT byte layout from android.media.audiofx.Visualizer:
        //   fft[0]      = DC component (real)
        //   fft[1]      = Nyquist (real)
        //   fft[2k], fft[2k+1] = real, imaginary for bin k (k = 1..N/2-1)
        //
        // `samplingRate` is in millihertz (mHz), not Hz — divide by 1000.
        val sampleRateHz = samplingRate / 1000
        val nBins = fft.size / 2
        if (nBins < 4 || sampleRateHz <= 0) return

        // The Visualizer captures at sampleRate / 2 effective frequency span.
        val binWidthHz = (sampleRateHz / 2f) / nBins

        // Frequency band boundaries (in bins).
        val bassMaxBin = (250f / binWidthHz).toInt().coerceAtMost(nBins - 1)
        val midMaxBin = (2000f / binWidthHz).toInt().coerceAtMost(nBins - 1)

        var bassSum = 0f
        var bassCount = 0
        var midSum = 0f
        var midCount = 0
        var trebleSum = 0f
        var trebleCount = 0

        // Bin 0 is DC and Nyquist — skip; start at bin 1.
        for (k in 1 until nBins) {
            val real = fft[2 * k].toInt()
            val imag = fft[2 * k + 1].toInt()
            val mag = sqrt((real * real + imag * imag).toFloat())
            // Log compression — audio is perceived exponentially, so log(mag) maps better to
            // visual "intensity" than raw magnitude. Add 1 to keep ln() defined at zero.
            val logMag = ln(1f + mag)
            when {
                k <= bassMaxBin -> { bassSum += logMag; bassCount++ }
                k <= midMaxBin  -> { midSum  += logMag; midCount++  }
                else            -> { trebleSum += logMag; trebleCount++ }
            }
        }

        // Normalize each band to its bin count and to a rough [0, 1] range. The /5 divisor
        // empirically maps log magnitudes from typical music into [0, ~1.2]; we clamp to 1.
        val bass = ((bassSum / bassCount.coerceAtLeast(1)) / 5f).coerceIn(0f, 1f)
        val mid = ((midSum / midCount.coerceAtLeast(1)) / 5f).coerceIn(0f, 1f)
        val treble = ((trebleSum / trebleCount.coerceAtLeast(1)) / 5f).coerceIn(0f, 1f)

        // Low-pass filter — exponentially-weighted moving average. Asymmetric: rises take ~5
        // frames, falls take ~10 (DECAY < ALPHA). Asymmetric smoothing makes peaks pop while
        // troughs feel like a soft tail rather than abrupt cuts.
        smoothBass = smooth(smoothBass, bass)
        smoothMid = smooth(smoothMid, mid)
        smoothTreble = smooth(smoothTreble, treble)

        _signal.value = VisualizerSignal(
            bass = smoothBass,
            mid = smoothMid,
            treble = smoothTreble,
            // Weight bass slightly higher — kicks/drums dominate perceived "intensity".
            energy = (0.5f * smoothBass + 0.3f * smoothMid + 0.2f * smoothTreble).coerceIn(0f, 1f),
        )
    }

    private fun smooth(current: Float, target: Float): Float {
        val coefficient = if (target > current) ALPHA_RISE else ALPHA_FALL
        return current + (target - current) * coefficient
    }

    companion object {
        private const val TAG = "VerzaVisualizer"
        private const val ALPHA_RISE = 0.30f // ~3-frame rise-time
        private const val ALPHA_FALL = 0.10f // ~10-frame fall-time
    }
}

/**
 * Returns the raw magnitude of a complex FFT sample — re-exported for unit tests / preview use.
 * Kept top-level so it isn't tied to the [AudioVisualizer] instance.
 */
internal fun fftMagnitude(real: Byte, imag: Byte): Float =
    sqrt((real.toInt() * real.toInt() + imag.toInt() * imag.toInt()).toFloat()).let { abs(it) }
