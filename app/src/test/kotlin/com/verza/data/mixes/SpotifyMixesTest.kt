package com.verza.data.mixes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyMixesTest {
    @Test fun findsThePlaylistInAnyLink() {
        assertEquals("37i9dQZEVXcTWG0H2txEJ9", SpotifyMixes.idOf("https://open.spotify.com/playlist/37i9dQZEVXcTWG0H2txEJ9?si=abc123"))
        assertEquals("37i9dQZF1E39vTG3GurFPW", SpotifyMixes.idOf("https://open.spotify.com/intl-de/playlist/37i9dQZF1E39vTG3GurFPW"))
        assertEquals("37i9dQZF1E39vTG3GurFPW", SpotifyMixes.idOf("spotify:playlist:37i9dQZF1E39vTG3GurFPW"))
        assertNull(SpotifyMixes.idOf("https://open.spotify.com/album/1DFixLWuPkv3KT3TnV35m3"))
        assertNull(SpotifyMixes.idOf("not a link"))
    }

    // The shape of a real Discover Weekly embed page (November 2026), cut down to two songs.
    private val page = """<html><script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"state":{"data":{"entity":{
        "type":"playlist","name":"Discover Weekly","subtitle":"Spotify","trackList":[
        {"uri":"spotify:track:1","title":"Voices","subtitle":"Disclosure,\u00a0Sasha Keable","duration":249094},
        {"uri":"spotify:track:2","title":"Ghostlike Dreams","subtitle":"nthng","duration":479035},
        {"uri":"spotify:track:3","title":"","subtitle":"Nobody"}
    ]}}}}}}</script></html>"""

    @Test fun readsTheNameAndSongsFirstArtistOnly() {
        val p = SpotifyMixes.parse(page)!!
        assertEquals("Discover Weekly", p.name)
        assertEquals(listOf("Voices" to "Disclosure", "Ghostlike Dreams" to "nthng"), p.pairs)
    }

    @Test fun aPageWithoutItsDataIsNothing() {
        assertNull(SpotifyMixes.parse("<html>Page not found</html>"))
    }
}
