package com.lstn.di

import android.content.Context
import com.lstn.player.PlayerConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @Provides
    @Singleton
    fun providePlayerConnection(@ApplicationContext context: Context): PlayerConnection =
        PlayerConnection(context)
}
