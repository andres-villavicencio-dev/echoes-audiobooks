package com.echoesapp.audiobooks.data.repository

import android.util.Log
import com.echoesapp.audiobooks.data.local.DeviceIdManager
import com.echoesapp.audiobooks.data.local.dao.BookmarkDao
import com.echoesapp.audiobooks.data.local.dao.ListeningSessionDao
import com.echoesapp.audiobooks.data.local.dao.PlaybackProgressDao
import com.echoesapp.audiobooks.data.local.entity.BookmarkEntity
import com.echoesapp.audiobooks.data.local.entity.ListeningSessionEntity
import com.echoesapp.audiobooks.data.local.entity.PlaybackProgressEntity
import com.echoesapp.audiobooks.data.remote.EchoesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: PlaybackProgressDao,
    private val sessionDao: ListeningSessionDao,
    private val bookmarkDao: BookmarkDao,
    private val deviceIdManager: DeviceIdManager,
    private val api: EchoesApi,
) : ProgressRepository {

    companion object {
        private const val TAG = "ProgressRepository"
    }

    // ==================== Playback Progress ====================

    override suspend fun getProgress(bookId: String): PlaybackProgressEntity? {
        return progressDao.getProgressForBook(bookId)
    }

    override fun observeProgress(bookId: String): Flow<PlaybackProgressEntity?> {
        return progressDao.observeProgressForBook(bookId)
    }

    override fun getContinueListening(limit: Int): Flow<List<PlaybackProgressEntity>> {
        return progressDao.getRecentProgress(limit)
    }

    override suspend fun savePosition(
        bookId: String,
        chapterIndex: Int,
        positionMs: Long,
    ) {
        withContext(Dispatchers.IO) {
            val existing = progressDao.getProgressForBook(bookId)
            if (existing != null) {
                progressDao.updatePosition(
                    bookId = bookId,
                    chapterIndex = chapterIndex,
                    positionMs = positionMs,
                )
            } else {
                progressDao.upsertProgress(
                    PlaybackProgressEntity(
                        bookId = bookId,
                        chapterIndex = chapterIndex,
                        positionMs = positionMs,
                        lastPlayedAt = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    override suspend fun updatePlaybackSpeed(bookId: String, speed: Float) {
        withContext(Dispatchers.IO) {
            val existing = progressDao.getProgressForBook(bookId)
            if (existing != null) {
                progressDao.updatePlaybackSpeed(bookId, speed)
            }
        }
    }

    override suspend fun markAsCompleted(bookId: String) {
        withContext(Dispatchers.IO) {
            progressDao.updateCompleted(bookId, true)
        }
    }

    override suspend fun clearProgress(bookId: String) {
        withContext(Dispatchers.IO) {
            progressDao.deleteProgressForBook(bookId)
            sessionDao.deleteSessionsForBook(bookId)
            bookmarkDao.deleteBookmarksForBook(bookId)
        }
    }

    // ==================== Listening Sessions ====================

    override suspend fun startSession(bookId: String, chapterIndex: Int): String {
        return withContext(Dispatchers.IO) {
            // End any existing active session
            val activeSession = sessionDao.getActiveSession()
            if (activeSession != null) {
                sessionDao.endSession(
                    sessionId = activeSession.id,
                    endedAt = System.currentTimeMillis(),
                    durationMs = activeSession.durationMs,
                    toChapter = activeSession.toChapter,
                )
            }

            // Start new session
            val session = ListeningSessionEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                startedAt = System.currentTimeMillis(),
                fromChapter = chapterIndex,
                toChapter = chapterIndex,
            )
            sessionDao.insertSession(session)
            session.id
        }
    }

    override suspend fun endSession(sessionId: String, durationMs: Long, toChapter: Int) {
        withContext(Dispatchers.IO) {
            sessionDao.endSession(
                sessionId = sessionId,
                endedAt = System.currentTimeMillis(),
                durationMs = durationMs,
                toChapter = toChapter,
            )

            // Also update total listened time on progress
            val session = sessionDao.getSession(sessionId)
            if (session != null) {
                progressDao.addListeningTime(session.bookId, durationMs)
            }
        }
    }

    override suspend fun getActiveSession(): ListeningSessionEntity? {
        return withContext(Dispatchers.IO) {
            sessionDao.getActiveSession()
        }
    }

    override fun getSessionsForBook(bookId: String): Flow<List<ListeningSessionEntity>> {
        return sessionDao.getSessionsForBook(bookId)
    }

    // ==================== Bookmarks ====================

    override suspend fun addBookmark(
        bookId: String,
        chapterIndex: Int,
        positionMs: Long,
        note: String?,
    ): String {
        return withContext(Dispatchers.IO) {
            val bookmark = BookmarkEntity(
                bookId = bookId,
                chapterIndex = chapterIndex,
                positionMs = positionMs,
                note = note,
            )
            bookmarkDao.insertBookmark(bookmark)
            bookmark.id
        }
    }

    override fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksForBook(bookId)
    }

    override suspend fun deleteBookmark(bookmarkId: String) {
        withContext(Dispatchers.IO) {
            bookmarkDao.deleteBookmark(bookmarkId)
        }
    }

    // ==================== Statistics ====================

    override suspend fun getTotalListeningTime(): Long {
        return withContext(Dispatchers.IO) {
            sessionDao.getTotalListeningTime()
        }
    }

    override suspend fun getTodayListeningTime(): Long {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            sessionDao.getListeningTimeSince(calendar.timeInMillis)
        }
    }

    override suspend fun getWeekListeningTime(): Long {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            sessionDao.getListeningTimeSince(calendar.timeInMillis)
        }
    }

    // ==================== Sync ====================

    override suspend fun syncToCloud(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = deviceIdManager.deviceId

                // Sync progress
                val unsyncedProgress = progressDao.getUnsyncedProgress()
                if (unsyncedProgress.isNotEmpty()) {
                    // TODO: Batch upload to Supabase
                    Log.d(TAG, "Syncing ${unsyncedProgress.size} progress records")
                    // For now, mark as synced
                    progressDao.markMultipleAsSynced(unsyncedProgress.map { it.bookId })
                }

                // Sync sessions
                val unsyncedSessions = sessionDao.getUnsyncedSessions()
                if (unsyncedSessions.isNotEmpty()) {
                    // TODO: Batch upload to Supabase
                    Log.d(TAG, "Syncing ${unsyncedSessions.size} sessions")
                    sessionDao.markMultipleAsSynced(unsyncedSessions.map { it.id })
                }

                // Sync bookmarks
                val unsyncedBookmarks = bookmarkDao.getUnsyncedBookmarks()
                if (unsyncedBookmarks.isNotEmpty()) {
                    // TODO: Batch upload to Supabase
                    Log.d(TAG, "Syncing ${unsyncedBookmarks.size} bookmarks")
                    bookmarkDao.markMultipleAsSynced(unsyncedBookmarks.map { it.id })
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sync to cloud failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun syncFromCloud(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = deviceIdManager.deviceId
                
                // TODO: Fetch progress from Supabase
                // Merge strategy: take max(local.positionMs, cloud.positionMs)
                
                Log.d(TAG, "Sync from cloud completed")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sync from cloud failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun fullSync(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            syncFromCloud()
            syncToCloud()
        }
    }
}
