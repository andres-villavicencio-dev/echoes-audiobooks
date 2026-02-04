package com.echoesapp.audiobooks.di

import android.content.Context
import com.echoesapp.audiobooks.data.repository.ProgressRepository
import com.echoesapp.audiobooks.player.PlayerManager
import com.echoesapp.audiobooks.player.SleepTimer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing player-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideSleepTimer(): SleepTimer {
        return SleepTimer()
    }

    @Provides
    @Singleton
    fun providePlayerManager(
        @ApplicationContext context: Context,
        sleepTimer: SleepTimer,
        progressRepository: ProgressRepository,
    ): PlayerManager {
        return PlayerManager(context, sleepTimer, progressRepository)
    }
}
