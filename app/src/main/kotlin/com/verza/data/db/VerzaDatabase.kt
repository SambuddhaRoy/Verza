package com.verza.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SongEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class, PlayEventEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class VerzaDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playEventDao(): PlayEventDao
}

/** v1 → v2: adds `downloadPath` so songs can be cached for offline playback. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE songs ADD COLUMN downloadPath TEXT")
    }
}

/** v2 → v3: adds local user playlists. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS playlists (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS playlist_tracks (
                playlistId INTEGER NOT NULL,
                songId TEXT NOT NULL,
                position INTEGER NOT NULL,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY(playlistId, songId),
                FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId ON playlist_tracks(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_songId ON playlist_tracks(songId)")
    }
}

/** v3 → v4: adds the play-event log backing the "Your Sound" listening-stats page. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS play_events (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                songId TEXT NOT NULL,
                playedAt INTEGER NOT NULL,
                listenedMs INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_play_events_songId ON play_events(songId)")
    }
}
