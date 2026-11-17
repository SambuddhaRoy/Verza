package com.verza.ui.nowplaying

import com.verza.data.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsGapsTest {
    @Test fun aBlankLineIsANoteOnlyBeforeAFiveSecondGap() {
        val lines = listOf(
            LyricLine(0, ""),            // 12s of intro: kept
            LyricLine(12_000, "Look at the stars"),
            LyricLine(15_000, ""),       // 1s: dropped
            LyricLine(16_000, "Look how they shine for you"),
            LyricLine(20_000, ""),       // 8s instrumental: kept
            LyricLine(28_000, "And everything you do"),
            LyricLine(31_000, ""),       // last line, nothing after: dropped
        )
        assertEquals(listOf(0L, 12_000L, 16_000L, 20_000L, 28_000L), musicalGaps(lines).map { it.timeMs })
    }
}
