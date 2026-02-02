package com.echoesapp.audiobooks.data.repository

import com.echoesapp.audiobooks.data.local.dao.AudiobookDao
import com.echoesapp.audiobooks.data.local.dao.PlaybackProgressDao
import com.echoesapp.audiobooks.data.local.entity.PlaybackProgressEntity
import com.echoesapp.audiobooks.data.local.entity.toDomain
import com.echoesapp.audiobooks.data.local.entity.toEntity
import com.echoesapp.audiobooks.data.remote.EchoesApi
import com.echoesapp.audiobooks.data.remote.toDomain
import com.echoesapp.audiobooks.domain.model.Audiobook
import com.echoesapp.audiobooks.domain.model.AudiobookProgress
import com.echoesapp.audiobooks.domain.model.Category
import com.echoesapp.audiobooks.domain.model.Chapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiobookRepositoryImpl @Inject constructor(
    private val audiobookDao: AudiobookDao,
    private val playbackProgressDao: PlaybackProgressDao,
    private val api: EchoesApi,
) : AudiobookRepository {

    companion object {
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }

    override fun getAllAudiobooks(): Flow<List<Audiobook>> =
        audiobookDao.getAllAudiobooks().map { entities ->
            entities.map { entity ->
                val chapters = audiobookDao.getChaptersForAudiobook(entity.id)
                entity.toDomain(chapters)
            }
        }

    override fun getAudiobooksByCategory(category: Category): Flow<List<Audiobook>> =
        audiobookDao.getAudiobooksByCategory(category.name).map { entities ->
            entities.map { entity ->
                val chapters = audiobookDao.getChaptersForAudiobook(entity.id)
                entity.toDomain(chapters)
            }
        }

    override fun getDownloadedAudiobooks(): Flow<List<Audiobook>> =
        audiobookDao.getDownloadedAudiobooks().map { entities ->
            entities.map { entity ->
                val chapters = audiobookDao.getChaptersForAudiobook(entity.id)
                entity.toDomain(chapters)
            }
        }

    override fun searchAudiobooks(query: String): Flow<List<Audiobook>> =
        audiobookDao.searchAudiobooks(query).map { entities ->
            entities.map { entity ->
                val chapters = audiobookDao.getChaptersForAudiobook(entity.id)
                entity.toDomain(chapters)
            }
        }

    override fun observeAudiobook(id: String): Flow<Audiobook?> =
        audiobookDao.observeAudiobookById(id).flatMapLatest { entity ->
            if (entity == null) {
                flowOf(null)
            } else {
                audiobookDao.observeChaptersForAudiobook(id).map { chapters ->
                    entity.toDomain(chapters)
                }
            }
        }

    override suspend fun getAudiobook(id: String): Result<Audiobook> = runCatching {
        val cachedEntity = audiobookDao.getAudiobookById(id)
        val isCacheValid = cachedEntity != null &&
            (System.currentTimeMillis() - cachedEntity.lastUpdated) < CACHE_DURATION_MS

        if (isCacheValid && cachedEntity != null) {
            val chapters = audiobookDao.getChaptersForAudiobook(id)
            return@runCatching cachedEntity.toDomain(chapters)
        }

        // Fetch from network (Supabase returns array even for single item)
        val response = api.getAudiobook("eq.$id")
        if (!response.isSuccessful) {
            // Fall back to cache if available
            if (cachedEntity != null) {
                val chapters = audiobookDao.getChaptersForAudiobook(id)
                return@runCatching cachedEntity.toDomain(chapters)
            }
            throw ApiException("Failed to fetch audiobook: ${response.code()}")
        }

        val audiobookList = response.body() ?: throw ApiException("Empty response body")
        val audiobookDto = audiobookList.firstOrNull() ?: throw ApiException("Audiobook not found")
        
        // Fetch chapters for this audiobook
        val chaptersResponse = api.getChapters("eq.$id")
        val chapters = if (chaptersResponse.isSuccessful) {
            chaptersResponse.body()?.map { it.toDomain() } ?: emptyList()
        } else {
            emptyList()
        }
        
        val audiobook = audiobookDto.toDomain(chapters)

        // Cache the result
        cacheAudiobook(audiobook)

        audiobook
    }

    override suspend fun refreshAudiobooks(): Result<Unit> = runCatching {
        val response = api.getAudiobooks()
        if (!response.isSuccessful) {
            throw ApiException("Failed to fetch audiobooks: ${response.code()}")
        }

        val audiobookDtos = response.body() ?: emptyList()
        
        // Fetch chapters for each audiobook
        audiobookDtos.forEach { dto ->
            val chaptersResponse = api.getChapters("eq.${dto.id}")
            val chapters = if (chaptersResponse.isSuccessful) {
                chaptersResponse.body()?.map { it.toDomain() } ?: emptyList()
            } else {
                emptyList()
            }
            val audiobook = dto.toDomain(chapters)
            cacheAudiobook(audiobook)
        }
    }

    override suspend fun refreshAudiobook(id: String): Result<Audiobook> = runCatching {
        val response = api.getAudiobook("eq.$id")
        if (!response.isSuccessful) {
            throw ApiException("Failed to fetch audiobook: ${response.code()}")
        }

        val audiobookList = response.body() ?: throw ApiException("Empty response body")
        val audiobookDto = audiobookList.firstOrNull() ?: throw ApiException("Audiobook not found")
        val audiobook = audiobookDto.toDomain()

        cacheAudiobook(audiobook)
        audiobook
    }

    override suspend fun setDownloadStatus(id: String, isDownloaded: Boolean): Result<Unit> =
        runCatching {
            audiobookDao.updateDownloadStatus(id, isDownloaded)
        }

    override fun getContinueListening(limit: Int): Flow<List<AudiobookProgress>> =
        playbackProgressDao.getRecentProgress(limit).flatMapLatest { progressList ->
            if (progressList.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    progressList.map { progress ->
                        observeAudiobook(progress.bookId).map { audiobook ->
                            audiobook?.let { createAudiobookProgress(it, progress) }
                        }
                    },
                ) { results ->
                    results.filterNotNull()
                }
            }
        }

    override fun getProgressForAudiobook(audiobookId: String): Flow<AudiobookProgress?> =
        combine(
            observeAudiobook(audiobookId),
            playbackProgressDao.observeProgressForBook(audiobookId),
        ) { audiobook, progress ->
            if (audiobook != null && progress != null) {
                createAudiobookProgress(audiobook, progress)
            } else {
                null
            }
        }

    override suspend fun savePlaybackProgress(
        audiobookId: String,
        chapterId: String,
        position: Long,
    ): Result<Unit> = runCatching {
        playbackProgressDao.upsertProgress(
            PlaybackProgressEntity(
                bookId = audiobookId,
                chapterId = chapterId,
                position = position,
                lastPlayed = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun clearProgress(audiobookId: String): Result<Unit> = runCatching {
        playbackProgressDao.deleteProgressForBook(audiobookId)
    }

    private suspend fun cacheAudiobook(audiobook: Audiobook) {
        val entity = audiobook.toEntity()
        val chapterEntities = audiobook.chapters.mapIndexed { index, chapter ->
            chapter.toEntity(audiobook.id, index)
        }
        audiobookDao.insertAudiobookWithChapters(entity, chapterEntities)
    }

    private fun createAudiobookProgress(
        audiobook: Audiobook,
        progress: PlaybackProgressEntity,
    ): AudiobookProgress {
        val currentChapter = audiobook.chapters.find { it.id == progress.chapterId }
        val totalProgress = calculateTotalProgress(audiobook, currentChapter, progress.position)

        return AudiobookProgress(
            audiobook = audiobook,
            currentChapterId = progress.chapterId,
            positionInChapter = progress.position,
            percentComplete = totalProgress,
            lastPlayedAt = progress.lastPlayed,
        )
    }

    private fun calculateTotalProgress(
        audiobook: Audiobook,
        currentChapter: Chapter?,
        position: Long,
    ): Float {
        if (audiobook.duration == 0L) return 0f

        val chapterStartPosition = currentChapter?.startPosition ?: 0L
        val totalPosition = chapterStartPosition + position
        return (totalPosition.toFloat() / audiobook.duration.toFloat()).coerceIn(0f, 1f)
    }
}

class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
