package com.verza.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.verza.innertube.models.MusicItem

/** A song the user has played and/or liked, cached locally for the Library. */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val liked: Boolean = false,
    val likedAt: Long? = null,
    val lastPlayedAt: Long? = null,
    /** Local file path when the track has been downloaded for offline playback. */
    val downloadPath: String? = null,
) {
    fun toMusicItem() = MusicItem(
        id = id,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
    )
}
