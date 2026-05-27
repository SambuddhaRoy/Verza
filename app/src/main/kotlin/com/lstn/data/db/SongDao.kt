package com.lstn.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 100")
    fun recentlyPlayed(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE liked = 1 ORDER BY likedAt DESC")
    fun liked(): Flow<List<SongEntity>>

    @Query("SELECT id FROM songs WHERE liked = 1")
    fun likedIds(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE downloadPath IS NOT NULL ORDER BY lastPlayedAt DESC")
    fun downloaded(): Flow<List<SongEntity>>

    @Query("SELECT id FROM songs WHERE downloadPath IS NOT NULL")
    fun downloadedIds(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun get(id: String): SongEntity?

    @Upsert
    suspend fun upsert(song: SongEntity)
}
