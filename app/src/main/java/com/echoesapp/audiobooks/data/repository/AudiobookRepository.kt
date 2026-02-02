package com.echoesapp.audiobooks.data.repository

import com.echoesapp.audiobooks.domain.model.Audiobook
import com.echoesapp.audiobooks.domain.model.AudiobookProgress
import com.echoesapp.audiobooks.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface AudiobookRepository {

    // Audiobook queries
    fun getAllAudiobooks(): Flow<List<Audiobook>>
    fun getAudiobooksByCategory(category: Category): Flow<List<Audiobook>>
    fun getDownloadedAudiobooks(): Flow<List<Audiobook>>
    fun searchAudiobooks(query: String): Flow<List<Audiobook>>
    fun observeAudiobook(id: String): Flow<Audiobook?>
    suspend fun getAudiobook(id: String): Result<Audiobook>

    // Network refresh
    suspend fun refreshAudiobooks(): Result<Unit>
    suspend fun refreshAudiobook(id: String): Result<Audiobook>

    // Download management
    suspend fun setDownloadStatus(id: String, isDownloaded: Boolean): Result<Unit>

    // Playback progress
    fun getContinueListening(limit: Int = 10): Flow<List<AudiobookProgress>>
    fun getProgressForAudiobook(audiobookId: String): Flow<AudiobookProgress?>
    suspend fun savePlaybackProgress(
        audiobookId: String,
        chapterId: String,
        position: Long,
    ): Result<Unit>
    suspend fun clearProgress(audiobookId: String): Result<Unit>
}
