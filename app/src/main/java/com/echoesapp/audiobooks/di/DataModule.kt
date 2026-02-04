package com.echoesapp.audiobooks.di

import android.content.Context
import androidx.room.Room
import com.echoesapp.audiobooks.data.local.DeviceIdManager
import com.echoesapp.audiobooks.data.local.EchoesDatabase
import com.echoesapp.audiobooks.data.local.dao.AudiobookDao
import com.echoesapp.audiobooks.data.local.dao.BookmarkDao
import com.echoesapp.audiobooks.data.local.dao.ListeningSessionDao
import com.echoesapp.audiobooks.data.local.dao.PlaybackProgressDao
import com.echoesapp.audiobooks.data.remote.EchoesApi
import com.echoesapp.audiobooks.data.repository.AudiobookRepository
import com.echoesapp.audiobooks.data.repository.AudiobookRepositoryImpl
import com.echoesapp.audiobooks.data.repository.ProgressRepository
import com.echoesapp.audiobooks.data.repository.ProgressRepositoryImpl
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    // ==================== Database ====================

    @Provides
    @Singleton
    fun provideEchoesDatabase(
        @ApplicationContext context: Context,
    ): EchoesDatabase = Room.databaseBuilder(
        context,
        EchoesDatabase::class.java,
        EchoesDatabase.DATABASE_NAME,
    )
        .addMigrations(EchoesDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()

    // ==================== DAOs ====================

    @Provides
    @Singleton
    fun provideAudiobookDao(database: EchoesDatabase): AudiobookDao =
        database.audiobookDao()

    @Provides
    @Singleton
    fun providePlaybackProgressDao(database: EchoesDatabase): PlaybackProgressDao =
        database.playbackProgressDao()

    @Provides
    @Singleton
    fun provideListeningSessionDao(database: EchoesDatabase): ListeningSessionDao =
        database.listeningSessionDao()

    @Provides
    @Singleton
    fun provideBookmarkDao(database: EchoesDatabase): BookmarkDao =
        database.bookmarkDao()

    // ==================== Device ID ====================

    @Provides
    @Singleton
    fun provideDeviceIdManager(
        @ApplicationContext context: Context,
    ): DeviceIdManager = DeviceIdManager(context)

    // ==================== Network ====================

    // Supabase anon key (public, safe to embed)
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im11dGVsbGJ3cmlva2tmcXJ6dGdsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAwMTkyMDcsImV4cCI6MjA4NTU5NTIwN30.ZwihuJnh3gKZNGjrBVVt6V69QJcWFdjWX5jn1wC8ksE"

    @Provides
    @Singleton
    fun provideOkHttpClient(
        deviceIdManager: DeviceIdManager,
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .addHeader("x-device-id", deviceIdManager.deviceId) // For RLS policies
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(EchoesApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideEchoesApi(retrofit: Retrofit): EchoesApi =
        retrofit.create(EchoesApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAudiobookRepository(
        impl: AudiobookRepositoryImpl,
    ): AudiobookRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(
        impl: ProgressRepositoryImpl,
    ): ProgressRepository
}
