package com.lstn.innertube

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.DeliveryMethod

/**
 * Resolves a playable audio stream for a videoId using NewPipeExtractor, which performs the
 * signature/n-parameter deciphering that raw InnerTube and youtubei.js could not do anonymously.
 * Blocking by design — call from a background dispatcher (see [InnerTube.resolveAudioStream]).
 */
internal object NewPipeStreamResolver {

    @Volatile private var initialized = false

    private fun ensureInit() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                NewPipe.init(NewPipeDownloader)
                initialized = true
            }
        }
    }

    fun resolve(videoId: String): StreamInfo? {
        ensureInit()
        // YouTube occasionally throttles or returns a transient extraction error; one retry
        // clears most of these.
        var lastError: Throwable? = null
        val backoffs = longArrayOf(0, 500, 1200)
        for (attempt in backoffs.indices) {
            if (backoffs[attempt] > 0) Thread.sleep(backoffs[attempt])
            try {
                resolveOnce(videoId)?.let { return it }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        lastError?.let { println("NewPipe resolve failed for $videoId: ${it.javaClass.simpleName}: ${it.message}") }
        return null
    }

    private fun resolveOnce(videoId: String): StreamInfo? {
        val extractor = ServiceList.YouTube
            .getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
        extractor.fetchPage()

        // Prefer progressive (directly streamable) audio; fall back to any audio stream.
        val pool = extractor.audioStreams.filter { it.content != null }
        val progressive = pool.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val candidates = progressive.ifEmpty { pool }
        if (candidates.isEmpty()) return null

        // Pick by the user's quality preference.
        val best = when (InnerTube.audioQuality) {
            AudioQuality.HIGH -> candidates.maxByOrNull { it.averageBitrate }
            AudioQuality.LOW -> candidates.minByOrNull { it.averageBitrate }
            AudioQuality.MEDIUM -> candidates.minByOrNull { kotlin.math.abs(it.averageBitrate - 128) }
        } ?: return null

        return StreamInfo(
            url = best.content!!,
            mimeType = best.format?.mimeType ?: "audio/mp4",
            bitrate = best.averageBitrate.takeIf { it > 0 }?.times(1000) ?: 0,
            contentLength = null,
        )
    }
}
