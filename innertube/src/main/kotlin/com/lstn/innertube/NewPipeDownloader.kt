package com.lstn.innertube

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal [Downloader] backing NewPipeExtractor, using HttpURLConnection. Pure JVM (no Android
 * APIs), so it runs identically in unit tests and on-device. NewPipe sets its own per-request
 * headers (client identity, user-agent); we just relay them.
 */
internal object NewPipeDownloader : Downloader() {

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val conn = (URL(request.url()).openConnection() as HttpURLConnection).apply {
            requestMethod = request.httpMethod()
            connectTimeout = 20_000
            readTimeout = 20_000
            instanceFollowRedirects = true
        }
        request.headers().forEach { (key, values) ->
            values.forEach { conn.addRequestProperty(key, it) }
        }
        request.dataToSend()?.let { body ->
            conn.doOutput = true
            conn.outputStream.use { it.write(body) }
        }

        val code = conn.responseCode
        val stream = if (code in 200..399) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return Response(code, conn.responseMessage, conn.headerFields, body, conn.url.toString())
    }
}
