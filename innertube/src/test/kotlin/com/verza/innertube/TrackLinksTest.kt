package com.verza.innertube

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackLinksTest {
    private fun run(text: String, id: String?, page: String?) = if (id == null) """{"text":"$text"}""" else """
        {"text":"$text","navigationEndpoint":{"browseEndpoint":{"browseId":"$id",
         "browseEndpointContextSupportedConfigs":{"browseEndpointContextMusicConfig":{"pageType":"$page"}}}}}"""

    private fun queue(vararg runs: String) = Json.parseToJsonElement("""
        {"contents":[{"playlistPanelVideoRenderer":{"videoId":"other","longBylineText":{"runs":[]}}},
         {"playlistPanelVideoRenderer":{"videoId":"v1","title":{"runs":[{"text":"Song"}]},
          "longBylineText":{"runs":[${runs.joinToString(",")}]}}}]}""")

    private fun song(title: String, vararg runs: String) = """{"musicResponsiveListItemRenderer":{"flexColumns":[
        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"$title"}]}}},
        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[${runs.joinToString(",")}]}}}]}}"""

    private val artistRun = run("Joji", "UC1", "MUSIC_PAGE_TYPE_ARTIST")
    private val albumRun = run("Nectar", "MPRE1", "MUSIC_PAGE_TYPE_ALBUM")

    @Test fun `a song has its artist and album`() {
        val (title, links) = parseTrackLinks(queue(artistRun, run(" . ", null, null), albumRun, run("2020", null, null)), "v1")!!
        assertEquals("Song", title)
        assertEquals(TrackLinks(listOf(PageLink("Joji", "UC1")), PageLink("Nectar", "MPRE1")), links)
    }

    @Test fun `every artist is kept`() {
        val (_, links) = parseTrackLinks(queue(artistRun, run(" & ", null, null), run("B", "UC2", "MUSIC_PAGE_TYPE_ARTIST")), "v1")!!
        assertEquals(listOf("Joji", "B"), links.artists.map { it.name })
        assertNull(links.album)
    }

    @Test fun `a video's uploader has a page too`() {
        val (_, links) = parseTrackLinks(queue(run("Someone", "UC9", "MUSIC_PAGE_TYPE_USER_CHANNEL")), "v1")!!
        assertEquals(listOf(PageLink("Someone", "UC9")), links.artists)
    }

    @Test fun `a song not in the response has no links`() {
        assertNull(parseTrackLinks(queue(), "absent"))
    }

    @Test fun `a video finds its song's album by title and artist`() {
        val results = Json.parseToJsonElement("""{"c":[
            ${song("Song (Remix)", artistRun, albumRun)},
            ${song("Song", run("Tribute Band", "UC7", "MUSIC_PAGE_TYPE_ARTIST"), run("Tributes", "MPRE7", "MUSIC_PAGE_TYPE_ALBUM"))},
            ${song("song", artistRun, albumRun)}]}""")
        assertEquals(PageLink("Nectar", "MPRE1"), parseSongMatch(results, "Song", listOf("Joji"))?.album)
    }

    @Test fun `no exact match, no album`() {
        val results = Json.parseToJsonElement("""{"c":[${song("Song (Remix)", artistRun, albumRun)}]}""")
        assertNull(parseSongMatch(results, "Song", listOf("Joji")))
    }
}
