package com.verza.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFilterTest {

    @Test
    fun searchFilters_returnSongsAlbumsPlaylists() = runBlocking {
        val q = "daft punk"

        val songs = InnerTube.searchItems(q, SearchFilter.SONGS)
        println("SONGS -> ${songs.size}; first=${songs.firstOrNull()?.title} song=${songs.firstOrNull()?.isSong}")
        assertTrue("No song results", songs.any { it.isSong })

        val albums = InnerTube.searchItems(q, SearchFilter.ALBUMS)
        println("ALBUMS -> ${albums.size}; first=${albums.firstOrNull()?.title} browseId=${albums.firstOrNull()?.browseId}")
        assertTrue("No album results", albums.isNotEmpty())

        // Open the first album's detail page (header + tracks).
        val album = albums.firstOrNull { it.browseId != null }
        if (album != null) {
            val detail = InnerTube.collectionDetail(album.browseId!!)
            println("detail \"${detail.title}\" / ${detail.subtitle} -> ${detail.tracks.size} tracks, thumb=${detail.thumbnailUrl != null}")
            assertTrue("Collection detail had no title", detail.title.isNotBlank())
            assertTrue("Collection detail had no tracks", detail.tracks.isNotEmpty())
        }
    }
}
