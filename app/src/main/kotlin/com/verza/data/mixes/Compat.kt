package com.verza.data.mixes

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.verza.data.Card
import com.verza.data.Setting
import com.verza.data.Song
import com.verza.data.youtube.Filter
import com.verza.data.youtube.YouTube
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// The mixes code came over from the old app as it was, tested and with its own cache. These are the
// shapes it was written against, and the one adapter between it and the new YouTube client. Cached
// mixes are stored in these shapes, so an update reads the old app's mixes straight away.

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

/** A song (videoId) or a page to open (browseId), as a mix holds them. */
@Serializable
data class HomeItem(
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val videoId: String? = null,
    val browseId: String? = null,
    val playlistId: String? = null,
) {
    val isSong: Boolean get() = videoId != null
    val artist: String get() = subtitle.substringBefore(" • ").trim()

    fun toCard(): Card = Card(
        title, subtitle, thumbnailUrl,
        song = videoId?.let { Song(it, title, artist, art = thumbnailUrl) },
        page = if (videoId == null) browseId ?: playlistId?.let { "VL$it" } else null,
    )
}

@Serializable
data class HomeSection(val title: String, val items: List<HomeItem> = emptyList())

@Serializable
data class ArtistDetail(val name: String, val thumbnailUrl: String? = null, val sections: List<HomeSection> = emptyList())

enum class SearchFilter(val filter: Filter) { SONGS(Filter.SONGS), ARTISTS(Filter.ARTISTS), ALBUMS(Filter.ALBUMS) }

private fun Song.old() = MusicItem(id, title, artist, album, art, durationMs)
private fun Card.old() = HomeItem(title, subtitle, art, song?.id, page)

/** The YouTube calls the mixes make, in their old shapes. */
class MusicRepository(private val youtube: YouTube) {
    suspend fun searchSongs(query: String): Result<List<MusicItem>> = runCatching { youtube.searchSongs(query).map { it.old() } }
    suspend fun radio(videoId: String): Result<List<MusicItem>> = runCatching { youtube.radio(videoId).map { it.old() } }
    suspend fun searchItems(query: String, filter: SearchFilter): Result<List<HomeItem>> = runCatching { youtube.search(query, filter.filter).map { it.old() } }
    suspend fun subscribedArtists(): Result<List<HomeItem>> = runCatching { youtube.accountArtists().map { it.old() } }
    suspend fun artistPage(browseId: String): Result<ArtistDetail> = runCatching {
        youtube.artist(browseId).let { a -> ArtistDetail(a.name, a.art, a.sections.map { s -> HomeSection(s.title, s.cards.map { it.old() }) }) }
    }
}

/** The one setting the mixes read: the ListenBrainz user. */
class PreferencesRepository(context: Context) {
    val listenBrainzUser = Setting(context, stringPreferencesKey("listenbrainz_user"), "")
    val listenBrainzUserFlow: Flow<String> = listenBrainzUser.flow
}
