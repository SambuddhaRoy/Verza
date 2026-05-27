package com.lstn.data

import com.lstn.data.db.SongDao
import com.lstn.player.DownloadLookup
import javax.inject.Inject
import javax.inject.Singleton

/** Implements [DownloadLookup] (declared in :player) against the local Room DB. */
@Singleton
class RoomDownloadLookup @Inject constructor(
    private val dao: SongDao,
) : DownloadLookup {
    override suspend fun pathFor(videoId: String): String? = dao.get(videoId)?.downloadPath
}
