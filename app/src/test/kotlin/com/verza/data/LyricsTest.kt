package com.verza.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {
    @Test fun `synced lines come out in time order, untimed ones left out`() {
        val lines = parseLrc("[00:12.50] second\n[00:05.1] first\nno time here\n[01:02] third")
        assertEquals(listOf(5_100L, 12_500L, 62_000L), lines.map { it.timeMs })
        assertEquals(listOf("first", "second", "third"), lines.map { it.text })
    }
}
