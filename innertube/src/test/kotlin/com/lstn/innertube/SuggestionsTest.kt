package com.lstn.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionsTest {

    @Test
    fun searchSuggestions_returnsCompletions() = runBlocking {
        val suggestions = InnerTube.searchSuggestions("daft p")
        println("suggestions for 'daft p' -> ${suggestions.size}")
        suggestions.take(10).forEach { println("  • $it") }
        assertTrue("No suggestions returned", suggestions.isNotEmpty())
    }
}
