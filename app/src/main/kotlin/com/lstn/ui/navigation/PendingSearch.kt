package com.lstn.ui.navigation

import com.lstn.innertube.SearchFilter

/**
 * One-shot search request handed across navigation (e.g. "Go to artist" from a row menu).
 * Consumed by [com.lstn.ui.screens.SearchViewModel] on init and immediately cleared.
 */
object PendingSearch {
    var query: String? = null
    var filter: SearchFilter = SearchFilter.SONGS

    fun consume(): Pair<String, SearchFilter>? {
        val q = query ?: return null
        val f = filter
        query = null
        filter = SearchFilter.SONGS
        return q to f
    }
}
