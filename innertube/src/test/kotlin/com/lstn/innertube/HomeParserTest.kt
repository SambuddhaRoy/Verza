package com.lstn.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeParserTest {

    @Test
    fun home_returnsSectionsAndExpandableCollections() = runBlocking {
        val sections = InnerTube.homeSections()
        println("home -> ${sections.size} sections")
        sections.take(6).forEach { s ->
            val first = s.items.firstOrNull()
            val kind = when { first?.isSong == true -> "song"; first?.browseId != null -> "collection"; else -> "?" }
            println("  § ${s.title}  (${s.items.size} items, first=$kind \"${first?.title}\")")
        }
        assertTrue("Home feed returned no sections", sections.isNotEmpty())
        assertTrue("No section had items", sections.any { it.items.isNotEmpty() })

        // Expand the first collection (album/playlist) into tracks to prove playback wiring.
        val collection = sections.flatMap { it.items }.firstOrNull { !it.isSong && it.browseId != null }
        if (collection != null) {
            val tracks = InnerTube.collectionTracks(browseId = collection.browseId, playlistId = collection.playlistId)
            println("expanded \"${collection.title}\" -> ${tracks.size} tracks; first = ${tracks.firstOrNull()?.title}")
            assertTrue("Collection \"${collection.title}\" expanded to no tracks", tracks.isNotEmpty())
        } else {
            println("(no collection card found to expand)")
        }
    }
}
