package com.verza.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream

class LinksTest {
    private fun song(id: String) = Song(id, "t$id", "a")

    @Test fun sessionRoundTrips() {
        val songs = listOf(song("aaaaaaaaaaa"), song("bbbbbbbbbbb"), song("ccccccccccc"))
        val s = Sessions.read(Sessions.link(songs, 1, 42_000)!!)!!
        assertEquals(listOf("aaaaaaaaaaa", "bbbbbbbbbbb", "ccccccccccc"), s.tracks.map { it.id })
        assertEquals(1, s.index)
        assertEquals(42_000, s.positionMs)
    }

    @Test fun phoneSongsStayBehindAndTheIndexFollows() {
        val songs = listOf(song("content://media/1"), song("aaaaaaaaaaa"), song("content://media/2"), song("bbbbbbbbbbb"))
        val s = Sessions.read(Sessions.link(songs, 3, 5_000)!!)!!
        assertEquals(listOf("aaaaaaaaaaa", "bbbbbbbbbbb"), s.tracks.map { it.id })
        assertEquals(1, s.index)
        // Playing a phone song: the share starts from the one before it, at its start.
        val t = Sessions.read(Sessions.link(songs, 2, 5_000)!!)!!
        assertEquals(0, t.index)
        assertEquals(0, t.positionMs)
        assertNull(Sessions.link(listOf(song("content://media/1")), 0, 0))
    }

    private fun craft(json: String): String {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(json.toByteArray()) }
        return Sessions.PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    @Test fun craftedLinksAreRefused() {
        // Only YouTube ids survive; nothing that could name a file.
        val s = Sessions.read(craft("""{"index":9,"tracks":[{"id":"file:///data/x"},{"id":"aaaaaaaaaaa"},{"id":"../../etc"}]}"""))!!
        assertEquals(listOf("aaaaaaaaaaa"), s.tracks.map { it.id })
        assertEquals(0, s.index)
        assertNull(Sessions.read(craft("""{"tracks":[{"id":"content://x"}]}""")))
        // A bomb: small compressed, huge inflated.
        assertNull(Sessions.read(craft("""{"tracks":[{"id":"aaaaaaaaaaa","title":"${"x".repeat(400_000)}"}]}""")))
        assertNull(Sessions.read("verza://session/not-base64!!"))
    }

    @Test fun findsTheVideoInSharedText() {
        assertEquals("dQw4w9WgXcQ", videoIdIn("Listen: https://youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("dQw4w9WgXcQ", videoIdIn("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RD"))
        assertEquals("dQw4w9WgXcQ", videoIdIn("https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", videoIdIn("https://youtube.com/shorts/dQw4w9WgXcQ"))
        assertNull(videoIdIn("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertNull(videoIdIn("no link here"))
        assertNull(videoIdIn("https://youtu.be/short"))
    }
}
