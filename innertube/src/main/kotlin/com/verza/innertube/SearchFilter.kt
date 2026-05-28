package com.verza.innertube

/**
 * YouTube Music search filter scopes. The opaque [params] strings are the standard filter
 * tokens the web client sends; each constrains results to one result type.
 */
enum class SearchFilter(val params: String) {
    SONGS("EgWKAQIIAWoKEAoQAxAEEAkQBQ%3D%3D"),
    ALBUMS("EgWKAQIYAWoKEAoQAxAEEAkQBQ%3D%3D"),
    ARTISTS("EgWKAQIgAWoKEAoQAxAEEAkQBQ%3D%3D"),
    PLAYLISTS("EgWKAQIoAWoKEAoQAxAEEAkQBQ%3D%3D"),
}
