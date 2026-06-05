package com.verza.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.verza.innertube.models.MusicItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the user's on-device music via [MediaStore]. Each track is surfaced as a [MusicItem] whose
 * `id` is the file's `content://` URI — that URI is self-describing, so the rest of the app (queue,
 * playlists, likes) treats local tracks exactly like remote ones, and the playback service plays
 * the content URI directly instead of resolving it through NewPipe (see `MusicService`).
 *
 * Read-only. Requires READ_MEDIA_AUDIO (API 33+) / READ_EXTERNAL_STORAGE (≤ API 32); the caller is
 * responsible for holding the runtime permission before invoking [scan].
 */
@Singleton
class LocalMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Album-art base URI; appending an albumId yields a loadable cover (Coil reads content URIs). */
    private val albumArtBase: Uri = Uri.parse("content://media/external/audio/albumart")

    suspend fun scan(): List<MusicItem> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        // Only real music files (excludes ringtones, notifications, podcasts flagged otherwise),
        // and skip zero-length entries the media scanner sometimes leaves behind.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val items = ArrayList<MusicItem>()
        runCatching {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val albumId = cursor.getLong(albumIdCol)
                    val artUri = ContentUris.withAppendedId(albumArtBase, albumId)
                    val artist = cursor.getString(artistCol)
                        ?.takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING }
                        ?: "Unknown artist"
                    items += MusicItem(
                        id = contentUri.toString(),
                        title = cursor.getString(titleCol)?.takeUnless { it.isBlank() } ?: "Unknown",
                        artist = artist,
                        album = cursor.getString(albumCol),
                        thumbnailUrl = artUri.toString(),
                        durationMs = cursor.getLong(durationCol),
                    )
                }
            }
        }
        items
    }
}
