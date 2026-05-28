package com.verza.di

import android.content.Context
import androidx.room.Room
import com.verza.data.RoomDownloadLookup
import com.verza.data.db.VerzaDatabase
import com.verza.data.db.MIGRATION_1_2
import com.verza.data.db.MIGRATION_2_3
import com.verza.data.db.PlaylistDao
import com.verza.data.db.SongDao
import com.verza.player.DownloadLookup
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadLookupModule {
    @Binds
    abstract fun bindDownloadLookup(impl: RoomDownloadLookup): DownloadLookup
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VerzaDatabase =
        Room.databaseBuilder(context, VerzaDatabase::class.java, "verza.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // Last-resort: if a future schema change ships without a migration we drop tables
            // rather than crash; users only lose their local likes/history.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSongDao(db: VerzaDatabase): SongDao = db.songDao()

    @Provides
    fun providePlaylistDao(db: VerzaDatabase): PlaylistDao = db.playlistDao()
}
