package com.verza.data.phone

import com.verza.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneGroupingTest {

    private fun file(
        folder: String, name: String, title: String, artist: String?, album: String?,
        track: Int = 0, albumArtist: String? = null, year: Int? = null,
    ) = PhoneFile(
        item = Song(id = "content://$folder$name", title = title, artist = artist ?: "Unknown artist", album = album),
        folder = folder, fileName = name, album = album, albumArtist = albumArtist, track = track, year = year,
        tagged = artist != null,
    )

    @Test fun `a folder of one album by one artist is that album, in track order`() {
        val f = "Music/Coldplay/Parachutes (2000)/"
        val (albums, playlists) = PhoneGrouping.group(listOf(
            file(f, "02 Shiver.flac", "Shiver", "Coldplay", "Parachutes", track = 2, year = 2000),
            file(f, "01 Don't Panic.flac", "Don't Panic", "Coldplay", "Parachutes", track = 1, year = 2000),
            file(f, "05 Yellow.flac", "Yellow", "Coldplay", "Parachutes", track = 5, year = 2000),
        ))
        assertEquals(1, albums.size)
        assertTrue(playlists.isEmpty())
        val a = albums.single()
        assertEquals("Parachutes", a.title)
        assertEquals("Coldplay · 2000", a.subtitle)
        assertEquals(listOf("Don't Panic", "Shiver", "Yellow"), a.songs.map { it.title })
    }

    @Test fun `featured artists don't split an album, and its discs are one album`() {
        val (albums, _) = PhoneGrouping.group(listOf(
            file("Music/RAM/CD1/", "a.flac", "Give Life Back to Music", "Daft Punk", "Random Access Memories", track = 1001),
            file("Music/RAM/CD1/", "b.flac", "Get Lucky", "Daft Punk feat. Pharrell Williams", "Random Access Memories", track = 1008),
            file("Music/RAM/CD2/", "c.flac", "Contact", "Daft Punk", "Random Access Memories", track = 2001),
        ))
        assertEquals(1, albums.size)
        assertEquals("Daft Punk", albums.single().artist)
        assertEquals(listOf("Give Life Back to Music", "Get Lucky", "Contact"), albums.single().songs.map { it.title })
    }

    @Test fun `a compilation keeps together by its album artist`() {
        val f = "Music/Now 100/"
        val (albums, playlists) = PhoneGrouping.group(listOf(
            file(f, "1.flac", "One", "Artist A", "Now 100", albumArtist = "Various Artists", track = 1),
            file(f, "2.flac", "Two", "Artist B", "Now 100", albumArtist = "Various Artists", track = 2),
        ))
        assertEquals(1, albums.size)
        assertTrue(playlists.isEmpty())
    }

    @Test fun `a folder of different artists is a playlist by the folder's name`() {
        val f = "Music/Verza/Road trip/"
        val (albums, playlists) = PhoneGrouping.group(listOf(
            // Verza's own downloads carry no album, so MediaStore names it after the folder.
            file(f, "Oasis - Wonderwall.m4a", "Wonderwall", "Oasis", "Road trip"),
            file(f, "Keane - Somewhere Only We Know.m4a", "Somewhere Only We Know", "Keane", "Road trip"),
        ))
        assertTrue(albums.isEmpty())
        assertEquals("Road trip", playlists.single().title)
        assertEquals("Playlist", playlists.single().subtitle)
        assertEquals(listOf("Somewhere Only We Know", "Wonderwall"), playlists.single().songs.map { it.title })
    }

    @Test fun `untagged files are never an album, and loose songs are no playlist`() {
        val (albums, playlists) = PhoneGrouping.group(listOf(
            // MediaStore gives an untagged file its folder's name as an album, and no artist.
            file("Music/Voice notes/", "a.mp3", "a", null, "Voice notes"),
            file("Music/Voice notes/", "b.mp3", "b", null, "Voice notes"),
            file("Music/", "x.mp3", "x", "Someone", "One"),
            file("Download/", "y.mp3", "y", "Else", "Two"),
            file("Music/Verza/", "z.m4a", "z", "Third", "Verza"),
        ))
        assertTrue(albums.isEmpty())
        assertEquals(listOf("Voice notes"), playlists.map { it.title })
    }

    @Test fun `ids are the same from one scan to the next, and different for each`() {
        val files = listOf(
            file("Music/A/", "1.flac", "x", "A", "Album A"),
            file("Music/B/", "1.flac", "y", "B", "Album B"),
        )
        val first = PhoneGrouping.group(files).first.map { it.id }
        assertEquals(first, PhoneGrouping.group(files).first.map { it.id })
        assertEquals(2, first.toSet().size)
        assertTrue(first.all { PhoneGrouping.isLocalId(it) })
    }
}
