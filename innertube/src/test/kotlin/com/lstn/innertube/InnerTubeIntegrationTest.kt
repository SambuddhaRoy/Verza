package com.lstn.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live integration tests against the real YouTube pipeline (no device required — pure JVM):
 *
 *   search (our InnerTube) → resolve stream (NewPipeExtractor) → fetch audio bytes
 *
 * Kept intentionally small (one resolution per run) so we don't trigger YouTube throttling,
 * which is what hammering several back-to-back extractions does.
 */
class InnerTubeIntegrationTest {

    private val query = "daft punk get lucky"

    @Test
    fun search_returnsPlayableSongs() = runBlocking {
        val songs = InnerTube.searchSongs(query)
        println("search \"$query\" -> ${songs.size} songs; first = ${songs.firstOrNull()?.title} [${songs.firstOrNull()?.id}]")
        assertTrue("Search returned no songs", songs.isNotEmpty())
        assertTrue("First song has no videoId", songs.first().id.isNotBlank())
    }

    @Test
    fun endToEnd_resolveAndServeBytes() = runBlocking {
        val songs = InnerTube.searchSongs(query)
        assertTrue("No search results to resolve", songs.isNotEmpty())
        val song = songs.first()

        val stream = InnerTube.resolveAudioStream(song.id)
        println("resolved ${song.title} [${song.id}] -> mime=${stream?.mimeType} bitrate=${stream?.bitrate}")
        assertNotNull("NewPipe could not resolve an audio stream", stream)
        assertTrue("Stream URL is not http(s)", stream!!.url.startsWith("http"))

        val status = rangedGetStatus(stream.url)
        println("ranged GET -> HTTP $status ${if (status == 200 || status == 206) "PLAYABLE ✅" else "BLOCKED ❌"}")
        assertTrue("Stream URL returned HTTP $status (need 200/206)", status == 200 || status == 206)
    }

    /** Fetches the first 2 KB of [url] with a Range header and returns the HTTP status code. */
    private fun rangedGetStatus(url: String): Int {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Range", "bytes=0-2047")
            setRequestProperty("User-Agent", "Mozilla/5.0")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return try {
            conn.connect()
            val code = conn.responseCode
            runCatching { (if (code in 200..299) conn.inputStream else conn.errorStream)?.close() }
            code
        } finally {
            conn.disconnect()
        }
    }
}
