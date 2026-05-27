package com.lstn.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedTest {

    @Test
    fun homeFeed_isRich() = runBlocking {
        val sections = InnerTube.homeFeed()
        println("homeFeed -> ${sections.size} sections")
        sections.forEach { println("  § ${it.title} (${it.items.size})") }
        assertTrue("Expected several sections", sections.size >= 4)
    }
}
