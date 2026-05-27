package com.lstn.data

import kotlinx.serialization.Serializable

/** A serializable snapshot of the playback queue, persisted so it survives app restarts. */
@Serializable
data class SavedQueue(
    val tracks: List<SavedTrack> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0L,
)

@Serializable
data class SavedTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
)
