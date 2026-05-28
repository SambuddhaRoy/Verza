package com.verza.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioTest {

    @Test
    fun radio_buildsMixFromSeed() = runBlocking {
        val seed = InnerTube.searchSongs("daft punk get lucky").first().id
        val mix = InnerTube.radio(seed)
        println("radio seed=$seed -> ${mix.size} tracks")
        mix.take(8).forEach { println("  • ${it.title} — ${it.artist} [${it.id}] thumb=${it.thumbnailUrl != null}") }
        assertTrue("Radio mix should contain several tracks", mix.size >= 5)
    }
}
