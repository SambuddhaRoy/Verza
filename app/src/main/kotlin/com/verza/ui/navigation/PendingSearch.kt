package com.verza.ui.navigation

import com.verza.innertube.SearchFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A search staged from somewhere else in the app — "Go to artist" on a row menu, say.
 *
 * A flow rather than a plain var read once in SearchViewModel's init. Search is a bottom-bar tab, so
 * navigating to it with launchSingleTop reuses the existing back-stack entry and therefore the
 * existing ViewModel: init did not run again, the request was never consumed, and the tap did
 * nothing — then the stale query hijacked the next Search screen that happened to be created.
 */
object PendingSearch {
    private val _pending = MutableStateFlow<Pair<String, SearchFilter>?>(null)
    val pending: StateFlow<Pair<String, SearchFilter>?> = _pending.asStateFlow()

    fun request(query: String, filter: SearchFilter) {
        _pending.value = query to filter
    }

    fun consume() {
        _pending.value = null
    }
}
