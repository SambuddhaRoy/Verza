package com.verza.innertube

/**
 * YouTube Music search filter scopes. The opaque [params] strings are the standard filter
 * tokens the web client sends; each constrains results to one result type. [TOP] sends no
 * filter param at all, so YouTube returns its mixed, relevance-ranked top-results page
 * (songs, artists, albums and playlists together). [label] is the tab caption shown in the UI.
 */
enum class SearchFilter(val params: String, val label: String) {
    TOP("", "Top results"),
    SONGS("EgWKAQIIAWoKEAoQAxAEEAkQBQ%3D%3D", "Songs"),
    ALBUMS("EgWKAQIYAWoKEAoQAxAEEAkQBQ%3D%3D", "Albums"),
    ARTISTS("EgWKAQIgAWoKEAoQAxAEEAkQBQ%3D%3D", "Artists"),
    PLAYLISTS("EgWKAQIoAWoKEAoQAxAEEAkQBQ%3D%3D", "Playlists"),
}
