package com.lstn.ui.navigation

sealed class Screen(val route: String) {
    data object Home       : Screen("home")
    data object Search     : Screen("search")
    data object Library    : Screen("library")
    data object NowPlaying : Screen("now_playing")
    data object Collection : Screen("collection/{collectionId}") {
        const val ARG = "collectionId"
        // browseIds (MPRE…, VL…, RDCLAK…) are URL-safe, so no encoding needed.
        fun create(collectionId: String) = "collection/$collectionId"
    }
    data object Artist : Screen("artist/{browseId}") {
        const val ARG = "browseId"
        fun create(browseId: String) = "artist/$browseId"
    }
    data object Settings : Screen("settings")
    data object Login     : Screen("login")
    data object Lyrics    : Screen("lyrics")
    data object LocalPlaylist : Screen("local_playlist/{playlistId}") {
        const val ARG = "playlistId"
        fun create(playlistId: Long) = "local_playlist/$playlistId"
    }
}
