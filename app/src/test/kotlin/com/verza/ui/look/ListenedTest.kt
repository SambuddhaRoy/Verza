package com.verza.ui.look

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListenedTest {
    @Test fun nothingBeforeTheFirstPlay() = assertNull(listened(0, 0, null))

    @Test fun oneSentenceOfWhatYouPlayed() {
        assertEquals("1 play.", listened(1, 1, null))
        assertEquals("412 plays, 12 days in a row. Mostly Coldplay lately.", listened(412, 12, "Coldplay"))
        // A streak of one day isn't a streak.
        assertEquals("1,204 plays. Mostly Joji lately.", listened(1204, 1, "Joji"))
    }
}
