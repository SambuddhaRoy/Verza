package com.verza.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SameSongTest {
    private fun s(title: String, artist: String, ms: Long = 0) = Song("x", title, artist, durationMs = ms)

    @Test fun `what one copy adds and another leaves out doesn't matter`() {
        assertTrue(SameSong.same(s("Sparks", "Coldplay", 227_000), s("Sparks", "Coldplay", 228_500)))
        assertTrue(SameSong.same(s("Princess of China (feat. Rihanna)", "Coldplay & Rihanna"), s("Princess of China", "Coldplay")))
        assertTrue(SameSong.same(s("Here Comes the Sun - Remastered 2019", "The Beatles"), s("Here Comes The Sun", "The Beatles")))
        assertTrue(SameSong.same(s("Don't Stop Me Now", "Queen"), s("Dont Stop Me Now", "Queen")))
    }

    @Test fun `another take, song or artist isn't the same`() {
        assertFalse(SameSong.same(s("Yellow", "Coldplay", 266_000), s("Yellow", "Coldplay", 301_000)))  // live
        assertFalse(SameSong.same(s("Yellow", "Coldplay"), s("Yellow (Live)", "Coldplay")))
        assertFalse(SameSong.same(s("Yellow", "Coldplay"), s("Yellow", "Katy Perry")))
    }

    @Test fun `the first artist named is who it's by`() {
        assertEquals("Daft Punk", SameSong.mainArtist("Daft Punk feat. Pharrell Williams"))
        assertEquals("Coldplay", SameSong.mainArtist("Coldplay, BTS"))
        assertEquals("Simon and Garfunkel", SameSong.mainArtist("Simon and Garfunkel"))
        assertEquals("Lil Nas X", SameSong.mainArtist("Lil Nas X"))
        assertEquals("AC/DC", SameSong.mainArtist("AC/DC"))
    }
}
