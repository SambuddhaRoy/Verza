package com.verza.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Fires short, amplitude-scaled vibration "ticks" in time with the music. Cheap and stateless —
 * the caller (a beat detector reading [AudioVisualizer]'s bass band) decides *when* to pulse and
 * how hard, so this never touches the microphone itself; it only consumes the FFT the glow already
 * computes from playback audio.
 */
class HapticPlayer(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val hasAmplitudeControl = vibrator?.hasAmplitudeControl() == true

    /** A brief tick whose strength tracks [intensity] (0..1). No-op without a vibrator. */
    fun pulse(intensity: Float, durationMs: Long = 14L) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val amplitude = if (hasAmplitudeControl) {
            (intensity.coerceIn(0f, 1f) * 200f + 45f).toInt().coerceIn(1, 255)
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }
        runCatching { v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude)) }
    }

    fun stop() {
        runCatching { vibrator?.cancel() }
    }
}
