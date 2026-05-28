package com.verza.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Test

class BrowseProbeTest {

    @Test
    fun probe_extraBrowseFeeds() = runBlocking {
        val feeds = listOf(
            "FEmusic_history",
            "FEmusic_library_corpus_artists",
            "FEmusic_listening_review",
            "FEmusic_mixed_for_you",
        )
        for (id in feeds) {
            val sections = runCatching { parseHomeSections(InnerTube.browse(id)) }.getOrElse { emptyList() }
            println("── $id -> ${sections.size} sections")
            sections.take(8).forEach { s ->
                val first = s.items.firstOrNull()
                val kind = when { first?.isSong == true -> "song"; first?.browseId != null -> "collection"; else -> "?" }
                println("    § ${s.title} (${s.items.size} items, first=$kind \"${first?.title}\")")
            }
        }
    }
}
