package com.verza.playback

import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The music's spectrum, for Now Playing's visualiser: the player's own output, copied as it goes by
 * (a TeeAudioProcessor in the player's audio sink), so it needs no microphone permission, which
 * Android's Visualizer does. Mono, 1024 samples a look, in [BANDS] bands spaced on a log scale, each
 * 0 to 1. Worked out only while something is [watching].
 */
object Spectrum : TeeAudioProcessor.AudioBufferSink {
    const val BANDS = 48
    private const val N = 1024

    /** The latest bands, 0 to 1, low to high. Read on the UI thread; a torn read is one frame's flicker. */
    val bands = FloatArray(BANDS)
    @Volatile var bass = 0f; private set
    /** How many things on screen want the spectrum; it is only worked out while one does. */
    private val watchers = java.util.concurrent.atomic.AtomicInteger(0)
    val watching: Boolean get() = watchers.get() > 0
    fun watch() { watchers.incrementAndGet() }
    fun unwatch() { watchers.updateAndGet { maxOf(0, it - 1) } }

    private var rate = 44100
    private var channels = 2
    private var encoding = C.ENCODING_PCM_16BIT
    private val window = FloatArray(N)
    private var filled = 0
    private val re = FloatArray(N)
    private val im = FloatArray(N)
    private val db = FloatArray(BANDS)
    private var top = -60f
    private var logged = false
    private val hann = FloatArray(N) { (0.5 - 0.5 * cos(2 * PI * it / (N - 1))).toFloat() }

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        rate = sampleRateHz; channels = max(1, channelCount); this.encoding = encoding; filled = 0
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (!watching) return
        val b = buffer.duplicate().order(ByteOrder.nativeOrder())
        val bytes = when (encoding) { C.ENCODING_PCM_16BIT -> 2; C.ENCODING_PCM_FLOAT -> 4; else -> return }
        val frame = bytes * channels
        while (b.remaining() >= frame) {
            var sum = 0f
            for (c in 0 until channels) sum += if (bytes == 2) b.short / 32768f else b.float
            window[filled++] = sum / channels
            if (filled == N) { analyse(); filled = 0 }
        }
    }

    private fun analyse() {
        for (i in 0 until N) { re[i] = window[i] * hann[i]; im[i] = 0f }
        fft(re, im)
        // Bands from ~60Hz to ~16kHz, each the loudest bin in its stretch, in decibels mapped to 0..1.
        val binHz = rate.toFloat() / N
        val lo = max(1, (60f / binHz).toInt())
        val hi = min(N / 2 - 1, (16000f / binHz).toInt())
        for (k in 0 until BANDS) {
            val a = (lo * (hi.toFloat() / lo).pow(k / BANDS.toFloat())).toInt()
            val z = max(a + 1, (lo * (hi.toFloat() / lo).pow((k + 1) / BANDS.toFloat())).toInt())
            var peak = 0f
            for (i in a until min(z, hi + 1)) peak = max(peak, hypot(re[i], im[i]))
            db[k] = 20f * ln(max(peak / (N / 4f), 1e-7f)) / ln(10f)
        }
        // Scaled to the loudest band lately, which falls back slowly: a quiet recording moves the bars
        // as much as a loud one, and a fixed scale left most music in the bottom third.
        val loudest = db.max()
        top = if (loudest > top) loudest else top - 0.05f
        for (k in 0 until BANDS) bands[k] = ((db[k] - (top - 42f)) / 42f).coerceIn(0f, 1f)
        if (!logged) { logged = true; android.util.Log.d("Verza", "spectrum: top ${top.toInt()} dB, bands ${bands.take(6).joinToString { "%.2f".format(it) }}") }
        bass = (bands[0] + bands[1] + bands[2] + bands[3]) / 4f
    }

    /** In-place radix-2 FFT. */
    private fun fft(x: FloatArray, y: FloatArray) {
        val n = x.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { var t = x[i]; x[i] = x[j]; x[j] = t; t = y[i]; y[i] = y[j]; y[j] = t }
        }
        var len = 2
        while (len <= n) {
            val ang = -2 * PI / len
            for (i in 0 until n step len) {
                for (k in 0 until len / 2) {
                    val c = cos(ang * k).toFloat(); val s = sin(ang * k).toFloat()
                    val ur = x[i + k]; val ui = y[i + k]
                    val vr = x[i + k + len / 2] * c - y[i + k + len / 2] * s
                    val vi = x[i + k + len / 2] * s + y[i + k + len / 2] * c
                    x[i + k] = ur + vr; y[i + k] = ui + vi
                    x[i + k + len / 2] = ur - vr; y[i + k + len / 2] = ui - vi
                }
            }
            len = len shl 1
        }
    }
}
