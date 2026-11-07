package com.verza.data.sources

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class AddonSourcesTest {

    private fun row(title: String, artist: String = "Coldplay", seconds: Double? = 279.0) =
        SourceTrack(id = "1", title = title, artist = artist, rawDuration = seconds?.let(::JsonPrimitive))

    private fun same(title: String, row: SourceTrack, artist: String = "Coldplay", ms: Long = 279_000L) =
        Match.matches(title, artist, ms, row)

    @Test fun `the same recording matches, whatever the title's packaging`() {
        assertTrue(same("Paradise", row("Paradise")))
        assertTrue(same("Paradise (Official Video)", row("Paradise")))
        assertTrue(same("Paradise", row("Paradise (Remastered 2011)")))
        assertTrue(same("Paradise (feat. Someone)", row("Paradise")))
        assertTrue(same("Coldplay - Paradise", row("Paradise")))
        assertTrue(same("Beyoncé", row("Beyonce")))
        assertTrue(same("Tum Hi Ho (From \"Aashiqui 2\")", row("Tum Hi Ho", "Arijit Singh"), "Arijit Singh"))
    }

    @Test fun `a different recording does not`() {
        assertFalse(same("Paradise", row("Paradise (Live)")))
        assertFalse(same("Paradise", row("Paradise - Live at Wembley")))
        assertFalse(same("Paradise", row("Paradise (Tiesto Remix)")))
        assertFalse(same("Paradise", row("Paradise (Acoustic)")))
        assertFalse(same("All Too Well", row("All Too Well (Taylor's Version)", "Taylor Swift"), "Taylor Swift"))
        assertFalse(same("Paradise", row("Paradise City")))
        // A song that is really called this is not a live take of it.
        assertTrue(same("Live Forever", row("Live Forever")))
        assertFalse(same("Live Forever", row("Live Forever (Live)")))
    }

    @Test fun `the artist and the length have to agree too`() {
        assertFalse(same("Paradise", row("Paradise", "Sade")))
        assertTrue(same("Paradise", row("Paradise", "Coldplay, Rihanna")))
        assertTrue(same("Paradise", row("Paradise"), "Coldplay - Topic"))
        assertTrue(same("Dog Days", row("Dog Days", "Florence and the Machine"), "Florence + The Machine"))
        assertFalse(same("Paradise", row("Paradise", seconds = 300.0)))
        assertTrue(same("Paradise", row("Paradise", seconds = 281.0)))
        // Neither side has to know the length.
        assertTrue(same("Paradise", row("Paradise", seconds = null)))
        assertTrue(same("Paradise", row("Paradise"), ms = 0L))
    }

    @Test fun `pick prefers the lossless row`() {
        val lossy = row("Paradise").copy(id = "mp3", format = "mp3")
        val flac = row("Paradise", seconds = 281.0).copy(id = "flac", format = "flac")
        assertEquals("flac", Match.pick("Paradise", "Coldplay", 279_000L, listOf(lossy, flac))?.id)
    }

    @Test fun `the badge reads the real format and ignores lossy ones`() {
        assertEquals("FLAC · 24-bit / 96 kHz", losslessLabel(MimeTypes.AUDIO_FLAC, C.ENCODING_PCM_24BIT, 96_000))
        assertEquals("FLAC · 16-bit / 44.1 kHz", losslessLabel(MimeTypes.AUDIO_FLAC, C.ENCODING_PCM_16BIT, 44_100))
        assertEquals("ALAC · 48 kHz", losslessLabel(MimeTypes.AUDIO_ALAC, Format_NO_VALUE, 48_000))
        assertNull(losslessLabel(MimeTypes.AUDIO_AAC, C.ENCODING_PCM_16BIT, 44_100))
        // A WAV on the phone is raw PCM to the player; its container says what it was.
        assertEquals("WAV · 16-bit / 44.1 kHz", losslessLabel(MimeTypes.AUDIO_RAW, C.ENCODING_PCM_16BIT, 44_100, MimeTypes.AUDIO_WAV))
        assertEquals("PCM · 24-bit / 48 kHz", losslessLabel(MimeTypes.AUDIO_RAW, C.ENCODING_PCM_24BIT, 48_000))
    }

    // ── against a real server ───────────────────────────────────────────────

    private lateinit var server: ServerSocket
    private lateinit var base: String
    private val routes = ConcurrentHashMap<String, Pair<Int, String>>()
    private val requests = CopyOnWriteArrayList<String>()
    @Volatile private var sources = emptyList<Source>()
    private val client = Sources(OkHttpClient()) { sources }

    @Before fun start() {
        server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                socket.use {
                    val reader = it.getInputStream().bufferedReader()
                    val target = reader.readLine().orEmpty().split(' ').getOrElse(1) { "/" }
                    while (reader.readLine().orEmpty().isNotEmpty()) { /* headers */ }
                    requests += target
                    val path = target.substringBefore('?')
                    val (code, body) = routes[URLDecoder.decode(path, "UTF-8")] ?: (404 to "{}")
                    val type = if (path.endsWith(".flac")) "audio/flac" else "application/json"
                    // Files are bytes: ISO-8859-1 maps each char of the route's string to one byte.
                    val bytes = if (path.endsWith(".flac")) body.toByteArray(Charsets.ISO_8859_1) else body.toByteArray()
                    it.getOutputStream().apply {
                        write("HTTP/1.1 $code X\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(bytes)
                        flush()
                    }
                }
            }
        }
        base = "http://127.0.0.1:${server.localPort}/token"
        sources = listOf(Source("Test", base, mapOf("quality" to "LOSSLESS")))
    }

    @After fun stop() {
        server.close()
        sources = emptyList()
    }

    private val paradise = """{"tracks":[{"id":"a/b","title":"Paradise","artist":"Coldplay","duration":279,"format":"flac"}]}"""

    @Test fun `search then stream gives the file, with the settings and an encoded id`() {
        routes["/token/search"] = 200 to paradise
        routes["/token/stream/a/b"] = 200 to """{"url":"/token/song.flac"}"""
        routes["/token/song.flac"] = 200 to "fLaC"
        assertEquals("$base/song.flac", client.find("Paradise", "Coldplay", 279_000L))
        assertTrue(requests.first(), requests.first().contains("quality=LOSSLESS") && requests.first().contains("q=Paradise"))
    }

    @Test fun `a row of a different length never reaches the stream call`() {
        routes["/token/search"] = 200 to """{"tracks":[{"id":"a","title":"Paradise","artist":"Coldplay","duration":90,"format":"flac"}]}"""
        assertNull(client.find("Paradise", "Coldplay", 279_000L))
        assertFalse(requests.any { it.contains("/stream") })
    }

    @Test fun `one row with an odd length does not lose the answer`() {
        routes["/token/search"] = 200 to """{"tracks":[{"id":"x","title":"Paradise","artist":"Coldplay","duration":"3:04","format":null,"streamURL":"$base/song.flac"}]}"""
        routes["/token/song.flac"] = 200 to "fLaC"
        assertEquals("$base/song.flac", client.find("Paradise", "Coldplay", 279_000L))
    }

    @Test fun `a row's own url skips the stream call`() {
        routes["/token/search"] = 200 to """{"tracks":[{"id":"1","title":"Paradise","artist":"Coldplay","streamURL":"$base/song.flac"}]}"""
        routes["/token/song.flac"] = 200 to "fLaC"
        assertEquals("$base/song.flac", client.find("Paradise", "Coldplay", 279_000L))
        assertFalse(requests.any { it.contains("/stream") })
    }

    @Test fun `everything short of playable audio is a miss`() {
        routes["/token/search"] = 200 to paradise
        routes["/token/song.flac"] = 200 to "fLaC"
        val refused = listOf(
            """{"url":"$base/song.flac","encrypted":"widevine"}""",
            """{"url":"$base/song.flac","manifest":"dash"}""",
            """{"url":"$base/song.flac","format":"hls"}""",
            """{"url":"file:///data/data/com.verza/files/secrets"}""",
            """{"url":"$base/missing.flac"}""",
            """{"error":"No playable audio"}""",
        )
        refused.forEach { answer ->
            routes["/token/stream/a/b"] = 200 to answer
            assertNull(answer, client.find("Paradise", "Coldplay", 279_000L))
        }
    }

    @Test fun `a preview, or a lossy file named FLAC, is a miss`() {
        routes["/token/search"] = 200 to paradise
        listOf("preview.flac" to flacHead(44_100, 30.0), "lossy.flac" to "ID3\u0004 an mp3 by another name").forEach { (name, file) ->
            routes["/token/stream/a/b"] = 200 to """{"url":"$base/$name"}"""
            routes["/token/$name"] = 200 to file
            assertNull(name, client.find("Paradise", "Coldplay", 279_000L))
        }
        // A few seconds off is a remaster, not a preview.
        routes["/token/stream/a/b"] = 200 to """{"url":"$base/whole.flac"}"""
        routes["/token/whole.flac"] = 200 to flacHead(44_100, 285.0)
        assertEquals("$base/whole.flac", client.find("Paradise", "Coldplay", 279_000L))
    }

    @Test fun `a FLAC file's length comes from its first bytes`() {
        assertEquals(278.0, flacSeconds(flacHead(44_100, 278.0).toByteArray(Charsets.ISO_8859_1))!!, 0.01)
        assertEquals(30.0, flacSeconds(flacHead(96_000, 30.0).toByteArray(Charsets.ISO_8859_1))!!, 0.01)
        assertEquals(-1.0, flacSeconds("fLaC".toByteArray())!!, 0.0)
        assertNull(flacSeconds("ID3 not this".toByteArray()))
    }

    /** The start of a FLAC file of this rate and length, 16-bit stereo, as a route's body. */
    private fun flacHead(rate: Int, seconds: Double): String {
        val b = ByteArray(42)
        "fLaC".forEachIndexed { i, c -> b[i] = c.code.toByte() }
        b[7] = 34
        val samples = (rate * seconds).toLong()
        b[18] = (rate shr 12).toByte()
        b[19] = (rate shr 4).toByte()
        b[20] = (((rate and 0xf) shl 4) or (1 shl 1)).toByte()
        b[21] = ((15 shl 4) or ((samples shr 32).toInt() and 0xf)).toByte()
        b[22] = (samples shr 24).toByte()
        b[23] = (samples shr 16).toByte()
        b[24] = (samples shr 8).toByte()
        b[25] = samples.toByte()
        return String(b, Charsets.ISO_8859_1)
    }

    @Test fun `a source cannot name a file on this phone`() {
        routes["/token/search"] = 200 to paradise
        routes["/token/stream/a/b"] = 200 to """{"url":"content://com.verza.provider/secrets"}"""
        assertNull(client.find("Paradise", "Coldplay", 279_000L))
    }

    @Test fun `not having it is a miss, and a broken server is left alone for a while`() {
        assertNull(client.find("Paradise", "Coldplay", 279_000L))     // 404: asked again next time
        assertNull(client.find("Paradise", "Coldplay", 279_000L))
        assertEquals(2, requests.size)

        routes["/token/search"] = 500 to "{}"
        assertNull(client.find("Paradise", "Coldplay", 279_000L))
        assertNull(client.find("Paradise", "Coldplay", 279_000L))
        assertEquals(3, requests.size)
    }

    @Test fun `inspect takes the manifest link, the root, or a server with no manifest`() {
        routes["/token/manifest.json"] = 200 to """{"id":"x","name":"My FLAC","resources":["search","stream"],
            "settings":[{"key":"region","default":"eu"},{"key":"quality","default":"high","options":[{"value":"high"},{"value":"flac-max"}]}]}"""
        val added = client.inspect("$base/manifest.json")
        assertEquals("My FLAC", added.name)
        assertEquals(base, added.url)
        assertEquals(mapOf("region" to "eu", "quality" to "flac-max"), added.params)
        assertEquals(base, client.inspect(base).url)

        routes.remove("/token/manifest.json")
        routes["/token/search"] = 200 to """{"tracks":[]}"""
        assertEquals("127.0.0.1", client.inspect(base).name)
    }

    @Test fun `inspect says what is wrong`() {
        fun problem(input: String) = runCatching { client.inspect(input) }.exceptionOrNull() as? SourceProblem
        assertTrue(problem("http://example.com/manifest.json")!!.message!!.contains("https"))
        assertTrue(problem("not a link at all")!!.message!!.contains("link"))
        assertTrue(problem(base)!!.message!!.contains("isn't a source"))
        routes["/token/manifest.json"] = 200 to """{"id":"x","resources":["stream"]}"""
        assertTrue(problem(base)!!.message!!.contains("searched"))
    }

    private companion object {
        const val Format_NO_VALUE = -1
    }
}
