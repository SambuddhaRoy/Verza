package com.lstn.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val explicit: Boolean = false,
)

@Serializable
data class MusicAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
    val tracks: List<MusicItem> = emptyList(),
)

@Serializable
data class MusicArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val subscribers: String? = null,
)

@Serializable
data class MusicPlaylist(
    val id: String,
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val trackCount: Int = 0,
    val tracks: List<MusicItem> = emptyList(),
)

// A card on the home/explore feed — either a playable song or a browsable collection
// (album/playlist) that we expand into tracks on tap.
@Serializable
data class HomeItem(
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val videoId: String? = null,    // present → directly playable song
    val browseId: String? = null,   // present → album/playlist page to expand
    val playlistId: String? = null,
) {
    val isSong: Boolean get() = videoId != null
}

// A titled row of items on the home/explore feed (e.g. "Quick picks", "Listen again").
@Serializable
data class HomeSection(
    val title: String,
    val items: List<HomeItem> = emptyList(),
)

// An artist page: header metadata plus shelves (top songs, albums, singles, …).
@Serializable
data class ArtistDetail(
    val name: String,
    val thumbnailUrl: String? = null,
    val sections: List<HomeSection> = emptyList(),
)

// An expanded album/playlist page: header metadata plus its tracks.
@Serializable
data class CollectionDetail(
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val tracks: List<MusicItem> = emptyList(),
)

// Search / browse result container
@Serializable
data class SearchResult(
    val tracks: List<MusicItem> = emptyList(),
    val albums: List<MusicAlbum> = emptyList(),
    val artists: List<MusicArtist> = emptyList(),
    val playlists: List<MusicPlaylist> = emptyList(),
    val continuationToken: String? = null,
)
