package com.verza.innertube

import com.verza.innertube.models.ArtistDetail
import com.verza.innertube.models.CollectionDetail
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.HomeSection
import com.verza.innertube.models.MusicItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ── Home / browse feed parsing ─────────────────────────────────────────────────
//
// FEmusic_home is a sectionList of shelves. Each shelf is a musicCarouselShelfRenderer
// (horizontal cards) or musicShelfRenderer (vertical list). Anonymous home is mostly
// album/playlist cards (browseIds); we keep both songs and collections and expand the
// latter into tracks when the user taps them.

internal fun parseHomeSections(response: JsonElement): List<HomeSection> {
    val shelves = response.findAll("musicCarouselShelfRenderer") +
        response.findAll("musicShelfRenderer")

    return shelves.mapNotNull { shelf ->
        val title = shelf.sectionTitle() ?: return@mapNotNull null
        val contents = shelf["contents"]?.array ?: return@mapNotNull null
        val items = contents.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            (obj["musicTwoRowItemRenderer"] as? JsonObject)?.let { parseTwoRowItem(it) }
                ?: (obj["musicResponsiveListItemRenderer"] as? JsonObject)?.let { parseResponsiveListItem(it)?.toHomeItem() }
        }.distinctBy { it.videoId ?: it.browseId ?: it.title }

        if (items.isEmpty()) null else HomeSection(title = title, items = items)
    }.distinctBy { it.title }
}

private fun JsonObject.sectionTitle(): String? {
    this["title"]?.findFirst("runs")?.array?.firstOrNull()?.child("text").string?.let { return it }
    this["header"]?.findFirst("runs")?.array?.firstOrNull()?.child("text").string?.let { return it }
    return null
}

/** A `musicTwoRowItemRenderer` card — a song (watchEndpoint) or a collection (browseEndpoint). */
private fun parseTwoRowItem(renderer: JsonObject): HomeItem? {
    val title = renderer["title"]?.findFirst("runs")?.array
        ?.firstOrNull()?.child("text").string ?: return null
    val subtitle = renderer["subtitle"]?.findFirst("runs")?.array
        ?.mapNotNull { it.child("text").string }
        ?.filter { it.isNotBlank() && it.trim() != "•" }
        ?.joinToString(" ") .orEmpty()
    val thumbnailUrl = renderer.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string

    val videoId = renderer.findFirst("watchEndpoint")?.child("videoId").string
    val browseId = renderer.findFirst("browseEndpoint")?.child("browseId").string
    val playlistId = renderer.findFirst("watchPlaylistEndpoint")?.child("playlistId").string
        ?: renderer.findFirst("watchEndpoint")?.child("playlistId").string

    if (videoId == null && browseId == null && playlistId == null) return null
    return HomeItem(
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId,
        browseId = browseId,
        playlistId = playlistId,
    )
}

/** Parses the user's library playlists page (FEmusic_liked_playlists) into playlist cards. */
internal fun parseLibraryPlaylists(response: JsonElement): List<HomeItem> =
    response.findAll("musicTwoRowItemRenderer")
        .mapNotNull { parseTwoRowItem(it) }
        .filter { it.browseId != null || it.playlistId != null }
        .distinctBy { it.browseId ?: it.playlistId ?: it.title }

private fun MusicItem.toHomeItem() = HomeItem(
    title = title,
    subtitle = artist,
    thumbnailUrl = thumbnailUrl,
    videoId = id,
)

/** Parses the track list from an album/playlist browse page into playable songs. */
internal fun parseCollectionTracks(response: JsonElement): List<MusicItem> =
    fillTrackDefaults(response, parseSearchSongs(response))

private fun collectionHeader(response: JsonElement): JsonObject? =
    (response.findFirst("musicResponsiveHeaderRenderer")
        ?: response.findFirst("musicDetailHeaderRenderer")
        ?: response.findFirst("musicEditablePlaylistDetailHeaderRenderer")) as? JsonObject

/**
 * Album track rows usually omit per-track artwork (they share the album cover) and, for
 * single-artist albums, the artist (shown only in the header). Backfill both from the header so
 * Now Playing and the queue don't show a blank cover / "Unknown artist".
 */
private fun fillTrackDefaults(response: JsonElement, tracks: List<MusicItem>): List<MusicItem> {
    val header = collectionHeader(response)
    val cover = header?.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string
        ?: response.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string
    val artist = header?.get("straplineTextOne")?.findFirst("runs")?.array
        ?.firstOrNull { it.child("text").string?.isNotBlank() == true }?.child("text").string

    return tracks.map { t ->
        t.copy(
            thumbnailUrl = t.thumbnailUrl ?: cover,
            artist = if (t.artist.isBlank() || t.artist.equals("Unknown artist", ignoreCase = true))
                (artist ?: t.artist) else t.artist,
        )
    }
}

/** Parses an artist browse page: header (name + image) plus its shelves, reusing the home parser. */
internal fun parseArtistPage(response: JsonElement): ArtistDetail {
    val header = (response.findFirst("musicImmersiveHeaderRenderer")
        ?: response.findFirst("musicResponsiveHeaderRenderer")
        ?: response.findFirst("musicVisualHeaderRenderer")) as? JsonObject

    val name = header?.get("title")?.findFirst("runs")?.array
        ?.firstOrNull()?.child("text").string ?: "Artist"
    val thumbnailUrl = header?.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string
        ?: response.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string

    return ArtistDetail(
        name = name,
        thumbnailUrl = thumbnailUrl,
        sections = parseHomeSections(response),
    )
}

/** Parses an album/playlist browse page into header metadata plus its tracks. */
internal fun parseCollectionDetail(response: JsonElement): CollectionDetail {
    val header = collectionHeader(response)

    val title = header?.get("title")?.findFirst("runs")?.array
        ?.firstOrNull()?.child("text").string ?: "Collection"
    val subtitle = (header?.get("subtitle") ?: header?.get("straplineTextOne"))
        ?.findFirst("runs")?.array
        ?.mapNotNull { it.child("text").string }
        ?.filter { it.isNotBlank() && it.trim() != "•" }
        ?.joinToString(" ") .orEmpty()
    val thumbnailUrl = header?.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string
        ?: response.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string

    return CollectionDetail(
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        tracks = fillTrackDefaults(response, parseSearchSongs(response)),
    )
}
