package com.verza.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Filenames for downloaded music.
 *
 * Worth pinning because every failure here is silent and looks like something else. A slash in an
 * artist name makes the download fail with an error that blames the extractor. A trailing dot is
 * dropped by FAT32 and Windows, so the file we record is not the file that exists and the track goes
 * missing on next play. And an over-eager character class eats the " - " that the whole naming scheme
 * is built around, which is exactly what the first version of this did.
 */
class DownloadNamingTest {

    @Test
    fun `strips characters filesystems reject`() {
        assertEquals("ACDC - Back in Black", DownloadNaming.stem("AC/DC", "Back in Black", "x"))
        assertEquals("AB - xyz", DownloadNaming.stem("A*B", "x?y:z", "x"))
        val messy = DownloadNaming.stem("<a>:b\"c", "d|e?f*g", "x")
        assertFalse("no illegal character survives", messy.any { it in "<>:\"/\\|?*" })
    }

    @Test
    fun `keeps the space and hyphen the format is made of`() {
        assertEquals("Sigur Ros - Hoppipolla", DownloadNaming.stem("Sigur Ros", "Hoppipolla", "x"))
        assertTrue(DownloadNaming.stem("Jay-Z", "Dirt Off Your Shoulder", "x").contains("Jay-Z"))
    }

    @Test
    fun `drops trailing dots and spaces that the filesystem would drop anyway`() {
        assertEquals("ACDC - T.N.T", DownloadNaming.stem("AC/DC", "T.N.T.", "x"))
        assertEquals("Bob - Hey", DownloadNaming.stem("Bob", "Hey.  ", "x"))
    }

    @Test
    fun `escapes reserved device names`() {
        assertEquals("_CON", DownloadNaming.stem("", "CON", "x"))
        assertEquals("_lpt1", DownloadNaming.stem("", "lpt1", "x"))
        assertEquals("CONCERT", DownloadNaming.stem("", "CONCERT", "x"))
    }

    @Test
    fun `always produces something openable`() {
        assertTrue(DownloadNaming.stem("x".repeat(200), "y".repeat(200), "id").length <= 120)
        assertEquals("dQw4w9WgXcQ", DownloadNaming.stem("", "", "dQw4w9WgXcQ"))
        assertEquals("track", DownloadNaming.stem(null, null, ""))
        assertTrue(DownloadNaming.stem(null, null, "id").isNotEmpty())
    }

    @Test
    fun `container matches what the stream actually is`() {
        // Mislabelling Opus as .m4a produces a file that looks playable and is not.
        assertEquals("m4a" to "audio/mp4", DownloadNaming.containerFor("audio/mp4"))
        assertEquals("opus" to "audio/opus", DownloadNaming.containerFor("audio/opus"))
        assertEquals("webm" to "audio/webm", DownloadNaming.containerFor("audio/webm"))
        assertEquals("mp3" to "audio/mpeg", DownloadNaming.containerFor("audio/mpeg"))
        // An unknown mime is treated as the one container YouTube reliably serves.
        assertEquals("m4a" to "audio/mp4", DownloadNaming.containerFor("application/octet-stream"))
    }

    @Test
    fun `file name joins the stem and the container`() {
        assertEquals(
            "Radiohead - Weird Fishes.m4a",
            DownloadNaming.fileName("Radiohead", "Weird Fishes", "x", "audio/mp4"),
        )
        assertEquals(
            "Radiohead - Weird Fishes.opus",
            DownloadNaming.fileName("Radiohead", "Weird Fishes", "x", "audio/opus"),
        )
    }
}
