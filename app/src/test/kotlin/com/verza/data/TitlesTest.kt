package com.verza.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** The desktop's cases for nice(), so the two apps show a title the same way. */
class TitlesTest {
    @Test fun videoTitlesReadAsSongs() {
        val cases = listOf(
            Triple("'Ordinary Love' (From Mandela OST) Lyric Video", "U2", "Ordinary Love (From Mandela OST)"),
            Triple("coldplay - o (lyrics)", "deep blue sounds", "coldplay - o"),
            Triple("Manchester Orchestra - Fade Into You (Mazzy Star Cover)", "Manchester Orchestra", "Fade Into You (Mazzy Star Cover)"),
            Triple("Yellow (Official Video)", "Coldplay", "Yellow"),
            Triple("Highway to Hell (Official Video)", "AC/DC", "Highway to Hell"),
            Triple("Video", "India.Arie", "Video"),
            Triple("Viva La Vida (Live from UNSTAGED)", "Coldplay", "Viva La Vida (Live from UNSTAGED)"),
            Triple("Song [Official Music Video] | Vevo", "X", "Song"),
            Triple("Get Lucky (feat. Pharrell Williams)", "Daft Punk", "Get Lucky (feat. Pharrell Williams)"),
            Triple("$$$", "XXXTENTACION", "$$$"),
        )
        for ((title, artist, want) in cases) assertEquals(title, want, shownTitle(title, artist))
    }
}
