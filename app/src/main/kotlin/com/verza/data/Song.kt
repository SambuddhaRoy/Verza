package com.verza.data

import kotlinx.serialization.Serializable

/**
 * A song, wherever it came from. [id] is a YouTube video id, or a `content://` URI for a file on the
 * phone, which plays as it is. [art] is a cover URL. [durationMs] is 0 when unknown.
 */
@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val art: String? = null,
    val durationMs: Long = 0,
) {
    val onPhone: Boolean get() = id.startsWith("content://") || id.startsWith("file://")
}

/**
 * One thing in a feed or a search: a song to play ([song] set) or a page to open ([page] set: an
 * album's or playlist's browse id, an artist's channel id).
 */
@Serializable
data class Card(
    val title: String,
    val subtitle: String = "",
    val art: String? = null,
    val song: Song? = null,
    val page: String? = null,
)

/** A titled row of cards: "quick picks", "new releases". */
@Serializable
data class Section(val title: String, val cards: List<Card>)

/** An album's or a playlist's page. */
@Serializable
data class Album(val title: String, val subtitle: String = "", val art: String? = null, val songs: List<Song> = emptyList())

/** An artist's page: name, picture, and their shelves. */
@Serializable
data class Artist(val name: String, val art: String? = null, val sections: List<Section> = emptyList())

/** A page on YouTube Music, for going to a song's artists and album. */
data class Link(val name: String, val page: String)
