package com.verza.data.youtube

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod

/** Where a song's audio is, and what it is. */
data class Stream(val url: String, val mimeType: String)

enum class Quality { LOW, MEDIUM, HIGH }

/**
 * A song's audio stream from YouTube, through NewPipeExtractor, which solves the signature and "n"
 * challenges YouTube puts in its stream URLs. Blocking; call it off the main thread.
 */
class Streams(private val http: OkHttpClient, private val quality: () -> Quality) {

    init { NewPipe.init(Fetcher(http)) }

    /**
     * The best progressive audio, or any audio, or null. [m4a] is for downloads: a file of Opus in
     * WebM is one most car stereos and other players refuse, so a download asks for AAC in MP4 first.
     * YouTube now and then fails an extraction it serves a moment later, so it gets three tries.
     */
    fun resolve(videoId: String, m4a: Boolean = false): Stream? {
        for (wait in longArrayOf(0, 500, 1200)) {
            if (wait > 0) Thread.sleep(wait)
            runCatching { once(videoId, m4a) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun once(videoId: String, m4a: Boolean): Stream? {
        val extractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
        extractor.fetchPage()
        val audio = runCatching { extractor.audioStreams }.getOrDefault(emptyList()).filter { it.content != null }
        val progressive = audio.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val pool = when {
            m4a && progressive.any { it.isMp4() } -> progressive.filter { it.isMp4() }
            progressive.isNotEmpty() -> progressive
            else -> audio
        }
        val best = when (quality()) {
            Quality.HIGH -> pool.maxByOrNull { it.averageBitrate }
            Quality.LOW -> pool.minByOrNull { it.averageBitrate }
            Quality.MEDIUM -> pool.minByOrNull { kotlin.math.abs(it.averageBitrate - 128) }
        } ?: return null
        return Stream(best.content, best.format?.mimeType ?: "audio/mp4")
    }

    private fun AudioStream.isMp4() = format?.mimeType.orEmpty().contains("mp4", ignoreCase = true)
}

/** NewPipe's network, on the app's OkHttp. NewPipe sets the headers; this only carries them. */
private class Fetcher(private val http: OkHttpClient) : Downloader() {
    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder().url(request.url())
        request.headers().forEach { (k, values) -> values.forEach { builder.addHeader(k, it) } }
        val body = request.dataToSend()?.toRequestBody(request.headers()["Content-Type"]?.firstOrNull()?.toMediaTypeOrNull())
        builder.method(request.httpMethod(), body)
        http.newCall(builder.build()).execute().use { resp ->
            return Response(resp.code, resp.message, resp.headers.toMultimap(), resp.body?.string().orEmpty(), resp.request.url.toString())
        }
    }
}
