package com.verza.data.downloads

import com.verza.data.downloads.SoulseekMatch.Hit
import org.junit.Assert.assertEquals
import org.junit.Test

class SoulseekMatchTest {

    private fun hit(path: String, secs: Int = 369, slots: Boolean = true, queue: Int = 0, speed: Long = 1_000_000,
                    rate: Int = 44_100, bits: Int = 16) =
        Hit("u", path, 40_000_000, slots, speed, queue, secs, rate, bits)

    @Test
    fun queryDropsFeaturesAndBrackets() {
        assertEquals("daft punk get lucky", SoulseekMatch.query("Daft Punk, Pharrell Williams", "Get Lucky (feat. Pharrell Williams & Nile Rodgers)"))
        assertEquals("beyoncé halo", SoulseekMatch.query("Beyoncé", "Halo [Remastered 2019]"))
    }

    @Test
    fun keepsOnlyTheSameRecording() {
        val album = hit("Music\\Daft Punk\\Random Access Memories\\08 - Get Lucky (feat. Pharrell Williams).flac")
        val radio = hit("Music\\Daft Punk\\Singles\\Get Lucky (Radio Edit).flac", secs = 248)
        val sameLengthEdit = hit("Music\\Daft Punk\\Get Lucky (Edit).flac")
        val live = hit("Music\\Daft Punk\\Get Lucky - Live.flac")
        val unknownLength = hit("Music\\Daft Punk\\Get Lucky.flac", secs = 0)
        val otherSong = hit("Music\\Daft Punk\\Lose Yourself to Dance.flac")
        val picked = SoulseekMatch.rank(listOf(radio, sameLengthEdit, live, unknownLength, otherSong, album), "Get Lucky", 369_000)
        assertEquals(listOf(album), picked)
    }

    @Test
    fun aVariantInTheTitleIsWanted() {
        val live = hit("Music\\Nirvana\\Unplugged\\01 About a Girl (Live).flac", secs = 217)
        assertEquals(listOf(live), SoulseekMatch.rank(listOf(live), "About a Girl (Live)", 216_500))
    }

    @Test
    fun freeSlotThenQueueThenCdQualityThenSpeed() {
        val path = "Music\\Radiohead\\OK Computer\\02 Paranoid Android.flac"
        val busy = hit(path, slots = false, speed = 9_000_000).copy(user = "busy")
        val queued = hit(path, queue = 4).copy(user = "queued")
        val hiRes = hit(path, rate = 96_000, bits = 24, speed = 5_000_000).copy(user = "hires")
        val slow = hit(path, speed = 100_000).copy(user = "slow")
        val fast = hit(path, speed = 3_000_000).copy(user = "fast")
        val order = SoulseekMatch.rank(listOf(busy, queued, hiRes, slow, fast), "Paranoid Android", 369_000).map { it.user }
        assertEquals(listOf("fast", "slow", "hires", "queued", "busy"), order)
    }
}
