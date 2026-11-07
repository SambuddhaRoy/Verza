package com.verza.data.youtube

import com.verza.data.Album
import com.verza.data.Artist
import com.verza.data.Card
import com.verza.data.Link
import com.verza.data.Section
import com.verza.data.Song
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// Turning YouTube Music's responses into the app's models. Pure functions of the JSON, so they're
// tested against saved responses (ParseTest).

/** The label YouTube Music puts first on a song or video row in mixed results. */
private val TYPE_LABELS = setOf("Song", "Video", "Episode")

/** A song row (`musicResponsiveListItemRenderer`): search results, album tracks, shelf rows. */
internal fun parseSongRow(row: JsonObject): Song? {
    val id = row.findFirst("watchEndpoint").child("videoId").string
        ?: row.findFirst("playlistItemData").child("videoId").string ?: return null
    val columns = row["flexColumns"] as? JsonArray ?: return null
    val title = columns.getOrNull(0).firstRunText() ?: return null
    // "Artist • Album • 3:45", in separate runs, with a "Song" label first in mixed results.
    val parts = columns.getOrNull(1).runTexts().let { if (it.firstOrNull() in TYPE_LABELS) it.drop(1) else it }
    return Song(
        id = id,
        title = title,
        artist = parts.firstOrNull { !it.isDuration() } ?: "Unknown artist",
        art = row.thumbnail(),
        durationMs = parts.lastOrNull { it.isDuration() }.durationMs(),
    )
}

private fun JsonElement?.firstRunText(): String? = this?.findFirst("runs")?.array?.firstOrNull()?.child("text").string

/** Every song row in a response, once each. */
internal fun parseSongs(response: JsonElement): List<Song> =
    response.findAll("musicResponsiveListItemRenderer").mapNotNull(::parseSongRow).distinctBy { it.id }

/** A row in mixed search results: a song, or an album, playlist or artist to open. */
internal fun parseResultRow(row: JsonObject): Card? {
    val columns = row["flexColumns"] as? JsonArray ?: return null
    val title = columns.getOrNull(0).firstRunText() ?: return null
    val song = parseSongRow(row)
    val parts = columns.getOrNull(1).runTexts()
    // Song rows drop their type label, so the artist comes first: playing a result used to save
    // "Song Radiohead" as the artist and split one band into many in the history.
    val subtitle = (if (song != null && parts.firstOrNull() in TYPE_LABELS) parts.drop(1) else parts).joinToString(" • ")
    val page = row["navigationEndpoint"]?.findFirst("browseEndpoint").child("browseId").string
        ?: row.findFirst("browseEndpoint").child("browseId").string
    if (song == null && page == null) return null
    return Card(title = title, subtitle = subtitle, art = row.thumbnail(), song = song, page = if (song == null) page else null)
}

internal fun parseResults(response: JsonElement): List<Card> =
    response.findAll("musicResponsiveListItemRenderer").mapNotNull(::parseResultRow)
        .distinctBy { it.song?.id ?: it.page ?: it.title }

/** A card on a shelf (`musicTwoRowItemRenderer`): a song, or a page. */
private fun parseTwoRow(item: JsonObject): Card? {
    val title = item.firstRun("title") ?: return null
    val runs = item["subtitle"].runTexts()
    val subtitle = runs.joinToString(" • ")
    val art = item.thumbnail()
    val videoId = item.findFirst("watchEndpoint").child("videoId").string
    val page = item.findFirst("browseEndpoint").child("browseId").string
        ?: item.findFirst("watchPlaylistEndpoint").child("playlistId").string?.let { "VL$it" }
    return when {
        videoId != null -> Card(title, subtitle, art, song = Song(videoId, title, runs.firstOrNull { it !in TYPE_LABELS } ?: "Unknown artist", art = art))
        page != null -> Card(title, subtitle, art, page = page)
        else -> null
    }
}

/** The shelves of a feed or an artist's page. */
internal fun parseSections(response: JsonElement): List<Section> =
    (response.findAll("musicCarouselShelfRenderer") + response.findAll("musicShelfRenderer")).mapNotNull { shelf ->
        val title = shelf.firstRun("title") ?: shelf.firstRun("header") ?: return@mapNotNull null
        val cards = (shelf["contents"] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            (o["musicTwoRowItemRenderer"] as? JsonObject)?.let(::parseTwoRow)
                ?: (o["musicResponsiveListItemRenderer"] as? JsonObject)?.let(::parseSongRow)
                    ?.let { Card(it.title, it.artist, it.art, song = it) }
        }.distinctBy { it.song?.id ?: it.page ?: it.title }
        if (cards.isEmpty()) null else Section(title, cards)
    }.distinctBy { it.title }

/** The playlists or artists on a library page: every card that opens a page. */
internal fun parsePages(response: JsonElement): List<Card> =
    response.findAll("musicTwoRowItemRenderer").mapNotNull(::parseTwoRow).filter { it.page != null }.distinctBy { it.page }

private fun header(response: JsonElement): JsonObject? =
    (response.findFirst("musicResponsiveHeaderRenderer") ?: response.findFirst("musicDetailHeaderRenderer")
        ?: response.findFirst("musicEditablePlaylistDetailHeaderRenderer")) as? JsonObject

/**
 * An album's or a playlist's page. Album rows usually leave out the cover (they share the album's)
 * and, on a one-artist album, the artist, which only the header names; both are filled from it.
 */
internal fun parseAlbum(response: JsonElement): Album {
    val head = header(response)
    val art = head.thumbnail() ?: response.thumbnail()
    val artist = head.child("straplineTextOne").runTexts().firstOrNull()
    val songs = parseSongs(response).map { s ->
        s.copy(art = s.art ?: art, artist = if (s.artist == "Unknown artist") artist ?: s.artist else s.artist)
    }
    val title = head.firstRun("title") ?: "Album"
    val subtitle = (head.child("subtitle") ?: head.child("straplineTextOne")).runTexts().joinToString(" ")
    return Album(title, subtitle, art, songs.map { it.copy(album = it.album ?: title) })
}

internal fun parseArtist(response: JsonElement): Artist {
    val head = (response.findFirst("musicImmersiveHeaderRenderer") ?: response.findFirst("musicResponsiveHeaderRenderer")
        ?: response.findFirst("musicVisualHeaderRenderer")) as? JsonObject
    return Artist(head.firstRun("title") ?: "Artist", head.thumbnail() ?: response.thumbnail(), parseSections(response))
}

/** A song's radio, or the queue `next` answers with (`playlistPanelVideoRenderer` rows). */
internal fun parseQueue(response: JsonElement): List<Song> =
    response.findAll("playlistPanelVideoRenderer").mapNotNull { r ->
        val id = r.findFirst("watchEndpoint").child("videoId").string ?: r["videoId"].string ?: return@mapNotNull null
        val title = r.firstRun("title") ?: return@mapNotNull null
        val artist = (r["longBylineText"] ?: r["shortBylineText"]).runTexts().firstOrNull() ?: "Unknown artist"
        Song(id, title, artist, art = r.thumbnail(), durationMs = r.firstRun("lengthText").durationMs())
    }.distinctBy { it.id }

internal fun parseSuggestions(response: JsonElement): List<String> =
    response.findAll("searchSuggestionRenderer").map { it["suggestion"]?.findFirst("runs")?.array
        ?.mapNotNull { r -> r.child("text").string }?.joinToString("").orEmpty() }
        .filter { it.isNotBlank() }.distinct()

/** The artist and album pages among a song's byline runs: "Joji • Nectar • 2020". */
private fun bylineLinks(runs: List<JsonElement>): Pair<List<Link>, Link?> {
    val artists = ArrayList<Link>()
    var album: Link? = null
    for (run in runs) {
        val browse = run.child("navigationEndpoint").child("browseEndpoint")
        val id = browse.child("browseId").string ?: continue
        val name = run.child("text").string ?: continue
        when (browse?.findFirst("pageType").string) {
            // A video's uploader is a plain channel; it still has a page, and the artist page opens it.
            "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> artists += Link(name, id)
            "MUSIC_PAGE_TYPE_ALBUM" -> if (album == null) album = Link(name, id)
        }
    }
    return artists to album
}

/** A song's artists and album, from its line in `next`'s queue. */
internal fun parseLinks(response: JsonElement, videoId: String): Pair<List<Link>, Link?>? {
    val row = response.findAll("playlistPanelVideoRenderer").firstOrNull { it["videoId"].string == videoId } ?: return null
    return bylineLinks(row["longBylineText"]?.findFirst("runs")?.array.orEmpty())
}

/**
 * The album of the song a music video is of, from a song search. Only the same title by one of the
 * same artists counts: anything looser hands a cover or a tribute its album.
 */
internal fun parseAlbumOf(response: JsonElement, title: String, artists: List<String>): Link? {
    val wanted = artists.map { it.trim().lowercase() }
    return response.findAll("musicResponsiveListItemRenderer").firstNotNullOfOrNull { row ->
        val columns = row["flexColumns"] as? JsonArray ?: return@firstNotNullOfOrNull null
        val (byline, album) = bylineLinks(columns.getOrNull(1)?.findFirst("runs")?.array.orEmpty())
        album.takeIf { columns.getOrNull(0).firstRunText().equals(title.trim(), true) && byline.any { it.name.lowercase() in wanted } }
    }
}
