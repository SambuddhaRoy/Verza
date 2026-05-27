package com.lstn.data

import com.lstn.innertube.InnerTube
import com.lstn.innertube.SearchFilter
import com.lstn.innertube.models.ArtistDetail
import com.lstn.innertube.models.CollectionDetail
import com.lstn.innertube.models.HomeItem
import com.lstn.innertube.models.HomeSection
import com.lstn.innertube.models.MusicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single seam between the UI and the InnerTube client. ViewModels depend on this
 * (never on raw JSON), which keeps parsing concerns out of the presentation layer and
 * gives us one place to add caching/Room later.
 */
@Singleton
class MusicRepository @Inject constructor() {

    suspend fun searchSongs(query: String): Result<List<MusicItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.searchSongs(query) } }

    suspend fun searchItems(query: String, filter: SearchFilter): Result<List<HomeItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.searchItems(query, filter) } }

    suspend fun searchSuggestions(query: String): Result<List<String>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.searchSuggestions(query) } }

    suspend fun radio(videoId: String): Result<List<MusicItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.radio(videoId) } }

    /** The signed-in user's "Liked Music" playlist (VLLM). Empty when signed out. */
    suspend fun accountLikedSongs(): Result<List<MusicItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.collectionTracks(browseId = "VLLM") } }

    suspend fun homeSections(): Result<List<HomeSection>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.homeFeed() } }

    suspend fun collectionTracks(browseId: String?, playlistId: String?): Result<List<MusicItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.collectionTracks(browseId, playlistId) } }

    suspend fun collectionDetail(collectionId: String): Result<CollectionDetail> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.collectionDetail(collectionId) } }

    suspend fun artistPage(browseId: String): Result<ArtistDetail> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.artistPage(browseId) } }

    suspend fun libraryPlaylists(): Result<List<HomeItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.libraryPlaylists() } }

    suspend fun subscribedArtists(): Result<List<HomeItem>> =
        runCatching { withContext(Dispatchers.IO) { InnerTube.subscribedArtists() } }
}
