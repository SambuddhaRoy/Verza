package com.lstn.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPageTest {

    @Test
    fun artistSearch_andPage() = runBlocking {
        val artists = InnerTube.searchItems("daft punk", SearchFilter.ARTISTS)
        println("ARTISTS -> ${artists.size}; first=${artists.firstOrNull()?.title} browseId=${artists.firstOrNull()?.browseId}")
        assertTrue("No artist results", artists.isNotEmpty())

        val artist = artists.first { it.browseId != null }
        assertTrue("Artist browseId should be a channel id (UC…)", artist.browseId!!.startsWith("UC"))

        val page = InnerTube.artistPage(artist.browseId!!)
        println("artist page \"${page.name}\" thumb=${page.thumbnailUrl != null} -> ${page.sections.size} sections")
        page.sections.take(6).forEach { s ->
            val first = s.items.firstOrNull()
            val kind = when { first?.isSong == true -> "song"; first?.browseId != null -> "collection"; else -> "?" }
            println("  § ${s.title} (${s.items.size} items, first=$kind \"${first?.title}\")")
        }
        assertTrue("Artist page has no name", page.name.isNotBlank())
        assertTrue("Artist page has no sections", page.sections.isNotEmpty())
    }
}
