package com.verza.innertube

import android.util.Log
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.DeliveryMethod

/**
 * Resolves a playable audio stream for a videoId using NewPipeExtractor, which performs the
 * signature/n-parameter deciphering that raw InnerTube and youtubei.js could not do anonymously.
 * Blocking by design — call from a background dispatcher (see [InnerTube.resolveAudioStream]).
 *
 * Last resolution attempt's summary is captured in [lastDiagnostic] so the calling layer can
 * surface it in a Toast / log — useful for diagnosing release-build silent failures.
 */
internal object NewPipeStreamResolver {

    @Volatile private var initialized = false

    /** Populated by [resolve]; the calling MusicService reads this to surface a Toast on failure. */
    @Volatile var lastDiagnostic: String = ""
        private set

    private fun ensureInit() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                NewPipe.init(NewPipeDownloader)
                initialized = true
            }
        }
    }

    /**
     * [preferM4a] is for downloads, not playback. Streaming can take whatever container is best,
     * because ExoPlayer opens all of them; a file on disk cannot, since a .webm of Opus is a file
     * most other players and every car stereo will refuse. So a download asks for AAC-in-MP4 and
     * only falls back to the usual pick when YouTube offers nothing else.
     */
    fun resolve(videoId: String, preferM4a: Boolean = false): StreamInfo? {
        ensureInit()
        // YouTube occasionally throttles or returns a transient extraction error; one retry
        // clears most of these.
        var lastError: Throwable? = null
        val backoffs = longArrayOf(0, 500, 1200)
        for (attempt in backoffs.indices) {
            if (backoffs[attempt] > 0) Thread.sleep(backoffs[attempt])
            try {
                resolveOnce(videoId, preferM4a)?.let { return it }
            } catch (t: Throwable) {
                lastError = t
                if (BuildConfig.DEBUG) Log.e("VerzaPlayback", "resolveOnce threw on attempt $attempt for $videoId", t)
            }
        }
        lastError?.let {
            lastDiagnostic = "${it.javaClass.simpleName}: ${it.message ?: "(no msg)"}"
            if (BuildConfig.DEBUG) Log.e("VerzaPlayback", "NewPipe resolve failed: $lastDiagnostic")
        }
        return null
    }

    private fun resolveOnce(videoId: String, preferM4a: Boolean = false): StreamInfo? {
        val extractor = ServiceList.YouTube
            .getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
        extractor.fetchPage()

        val audioStreams = runCatching { extractor.audioStreams }.getOrElse { emptyList() }
        val videoStreams = runCatching { extractor.videoStreams }.getOrElse { emptyList() }
        val dashUrl = runCatching { extractor.dashMpdUrl }.getOrNull()?.takeIf { it.isNotBlank() }
        val hlsUrl = runCatching { extractor.hlsUrl }.getOrNull()?.takeIf { it.isNotBlank() }

        // Per-stream visibility so we can tell content-null-but-stream-exists apart from
        // truly-empty extraction.
        val audioNonNull = audioStreams.count { it.content != null }
        val audioProgressive = audioStreams.count { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val videoNonNull = videoStreams.count { it.content != null }
        lastDiagnostic = "audio=${audioStreams.size}(nn=$audioNonNull,prog=$audioProgressive) " +
            "video=${videoStreams.size}(nn=$videoNonNull) " +
            "dash=${if (dashUrl != null) "Y" else "n"} hls=${if (hlsUrl != null) "Y" else "n"}"
        if (BuildConfig.DEBUG) {
            Log.i("VerzaPlayback", "resolveOnce($videoId): $lastDiagnostic")
            audioStreams.forEachIndexed { i, s ->
                Log.i(
                    "VerzaPlayback",
                    "  audio[$i] fmt=${s.format} br=${s.averageBitrate} delivery=${s.deliveryMethod} nullContent=${s.content == null}",
                )
            }
        }

        // ── Strategy 1: progressive HTTP audio — works with vanilla DefaultMediaSourceFactory.
        val progressive = audioStreams.filter {
            it.content != null && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
        }
        if (preferM4a) {
            val mp4 = progressive.filter { (it.format?.mimeType ?: "").contains("mp4", true) }
            if (mp4.isNotEmpty()) return mp4.pickByQualityToInfo("Strategy 0: progressive m4a")
        }
        if (progressive.isNotEmpty()) return progressive.pickByQualityToInfo("Strategy 1: progressive")

        // ── Strategy 2: any audio stream with content. DASH manifest URLs work because the
        // player module already pulls in media3-exoplayer-dash; ExoPlayer's DefaultMediaSourceFactory
        // auto-routes them to DashMediaSource based on the URL's mime / extension.
        val anyAudioContent = audioStreams.filter { it.content != null }
        if (anyAudioContent.isNotEmpty()) return anyAudioContent.pickByQualityToInfo("Strategy 2: any-audio")

        // ── Strategy 3: video stream with embedded audio (older mp4 + m4a combos). Last resort.
        val videoWithAudio = videoStreams.filter { it.content != null }
        if (videoWithAudio.isNotEmpty()) {
            val best = videoWithAudio.maxByOrNull { it.bitrate.takeIf { b -> b > 0 } ?: 0 }
            if (best != null) {
                if (BuildConfig.DEBUG) Log.w("VerzaPlayback", "Falling back to video stream with audio for $videoId")
                return StreamInfo(
                    url = best.content!!,
                    mimeType = best.format?.mimeType ?: "video/mp4",
                    bitrate = best.bitrate.takeIf { it > 0 }?.times(1000) ?: 0,
                    contentLength = null,
                )
            }
        }

        // ── Strategy 4: DASH manifest URL at the page level (some videos only expose this).
        if (dashUrl != null) {
            if (BuildConfig.DEBUG) Log.w("VerzaPlayback", "Falling back to page-level DASH manifest for $videoId")
            return StreamInfo(
                url = dashUrl,
                mimeType = "application/dash+xml",
                bitrate = 0,
                contentLength = null,
            )
        }

        lastDiagnostic = "No playable URL after 4 fallbacks. $lastDiagnostic"
        return null
    }

    private fun List<org.schabi.newpipe.extractor.stream.AudioStream>.pickByQualityToInfo(
        strategy: String,
    ): StreamInfo? {
        val best = when (InnerTube.audioQuality) {
            AudioQuality.HIGH -> maxByOrNull { it.averageBitrate }
            AudioQuality.LOW -> minByOrNull { it.averageBitrate }
            AudioQuality.MEDIUM -> minByOrNull { kotlin.math.abs(it.averageBitrate - 128) }
        } ?: return null
        if (BuildConfig.DEBUG) Log.i("VerzaPlayback", "$strategy → ${best.format} ${best.averageBitrate}kbps")
        return StreamInfo(
            url = best.content!!,
            mimeType = best.format?.mimeType ?: "audio/mp4",
            bitrate = best.averageBitrate.takeIf { it > 0 }?.times(1000) ?: 0,
            contentLength = null,
        )
    }
}
