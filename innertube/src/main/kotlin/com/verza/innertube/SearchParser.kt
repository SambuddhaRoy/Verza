package com.verza.innertube

import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.MusicItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ── Search response parsing ────────────────────────────────────────────────────
//
// A WEB_REMIX song-filtered search nests playable rows as `musicResponsiveListItemRenderer`
// objects scattered through sectionList/musicShelf wrappers. We collect them recursively
// and keep only the rows that resolve to a playable videoId.

internal fun parseSearchSongs(response: JsonElement): List<MusicItem> =
    response.findAll("musicResponsiveListItemRenderer")
        .mapNotNull { parseResponsiveListItem(it) }
        .distinctBy { it.id }

/**
 * Parses any search result type into [HomeItem]s: songs carry a videoId (playable directly),
 * albums/playlists carry a browseId (expanded into tracks on tap). Filter scope is decided by
 * the request params, so one parser serves every tab.
 */
internal fun parseSearchItems(response: JsonElement): List<HomeItem> =
    response.findAll("musicResponsiveListItemRenderer")
        .mapNotNull { parseSearchListItem(it) }
        .distinctBy { it.videoId ?: it.browseId ?: it.title }

/**
 * Parses a watch-next / radio response (playlistPanelVideoRenderer rows) into songs.
 * Used to build a song "radio" — an auto-generated endless-style mix from a seed track.
 */
internal fun parseWatchPlaylist(response: JsonElement): List<MusicItem> =
    response.findAll("playlistPanelVideoRenderer").mapNotNull { r ->
        val videoId = r.findFirst("watchEndpoint")?.child("videoId").string
            ?: r["videoId"].string ?: return@mapNotNull null
        val title = r["title"]?.findFirst("runs")?.array
            ?.firstOrNull()?.child("text").string ?: return@mapNotNull null
        val artist = (r["longBylineText"] ?: r["shortBylineText"])?.findFirst("runs")?.array
            ?.mapNotNull { it.child("text").string }
            ?.firstOrNull { it.isNotBlank() && it.trim() != "•" } ?: "Unknown artist"
        val thumbnailUrl = r.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string
        val duration = r["lengthText"]?.findFirst("runs")?.array
            ?.firstOrNull()?.child("text").string.durationLabelToMillis()
        MusicItem(id = videoId, title = title, artist = artist, thumbnailUrl = thumbnailUrl, durationMs = duration)
    }.distinctBy { it.id }

/** Parses the get_search_suggestions response into plain suggestion strings. */
internal fun parseSearchSuggestions(response: JsonElement): List<String> =
    response.findAll("searchSuggestionRenderer").mapNotNull { r ->
        r["suggestion"]?.findFirst("runs")?.array
            ?.mapNotNull { it.child("text").string }
            ?.joinToString("")
    }.filter { it.isNotBlank() }.distinct()

private fun parseSearchListItem(renderer: JsonObject): HomeItem? {
    val flexColumns = renderer["flexColumns"] as? JsonArray ?: return null
    val title = flexColumns.getOrNull(0)?.findFirst("runs")?.array
        ?.firstOrNull()?.child("text").string ?: return null
    val subtitle = flexColumns.getOrNull(1)?.findFirst("runs")?.array
        ?.mapNotNull { it.child("text").string }
        ?.filter { it.isNotBlank() && it.trim() != "•" }
        ?.joinToString(" ") .orEmpty()
    val thumbnailUrl = renderer.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string

    val videoId = renderer.findFirst("watchEndpoint")?.child("videoId").string
    // For non-song results, prefer the item's own navigation target over any nested
    // (e.g. artist) browse links found deeper in the subtitle runs.
    val browseId = renderer["navigationEndpoint"]?.findFirst("browseEndpoint")?.child("browseId").string
        ?: renderer.findFirst("browseEndpoint")?.child("browseId").string
    val playlistId = renderer.findFirst("watchPlaylistEndpoint")?.child("playlistId").string

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

private val durationRegex = Regex("""^\d{1,2}(:\d{2}){1,2}$""")

/** Parses a `musicResponsiveListItemRenderer` (search rows, shelf rows) into a playable song. */
internal fun parseResponsiveListItem(renderer: JsonObject): MusicItem? {
    // videoId: prefer an explicit watch endpoint, fall back to playlist item data.
    val videoId = renderer.findFirst("watchEndpoint")?.child("videoId").string
        ?: renderer.findFirst("playlistItemData")?.child("videoId").string
        ?: return null

    val flexColumns = renderer["flexColumns"] as? JsonArray ?: return null

    // Title lives in the first flex column's first run.
    val title = flexColumns.getOrNull(0)
        ?.findFirst("runs")?.array
        ?.firstOrNull()?.child("text").string
        ?: return null

    // The second column carries "Artist • Album • Duration" as separate runs.
    val subtitleRuns = flexColumns.getOrNull(1)?.findFirst("runs")?.array
    val subtitleTexts = subtitleRuns
        ?.mapNotNull { it.child("text").string }
        ?.filter { it.isNotBlank() && it.trim() != "•" }
        .orEmpty()

    val durationText = subtitleTexts.lastOrNull { it.matches(durationRegex) }
    val artist = subtitleTexts.firstOrNull { !it.matches(durationRegex) } ?: "Unknown artist"

    // Largest available thumbnail.
    val thumbnailUrl = renderer.findFirst("thumbnails")?.array
        ?.lastOrNull()?.child("url").string

    return MusicItem(
        id = videoId,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationText.durationLabelToMillis(),
    )
}
