package com.echoesapp.audiobooks.data.repository

import com.echoesapp.audiobooks.data.local.entity.BookmarkEntity
import com.echoesapp.audiobooks.data.local.entity.ListeningSessionEntity
import com.echoesapp.audiobooks.data.local.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing playback progress, listening sessions, and bookmarks.
 * 
 * Handles both local storage and cloud synchronization.
 */
interface ProgressRepository {

    // ==================== Playback Progress ====================
    
    /**
     * Get progress for a specific audiobook.
     */
    suspend fun getProgress(bookId: String): PlaybackProgressEntity?
    
    /**
     * Observe progress for a specific audiobook.
     */
    fun observeProgress(bookId: String): Flow<PlaybackProgressEntity?>
    
    /**
     * Get recently played books (not completed).
     */
    fun getContinueListening(limit: Int = 10): Flow<List<PlaybackProgressEntity>>
    
    /**
     * Save playback position (called frequently while playing).
     */
    suspend fun savePosition(
        bookId: String,
        chapterIndex: Int,
        positionMs: Long,
    )
    
    /**
     * Update playback speed preference for a book.
     */
    suspend fun updatePlaybackSpeed(bookId: String, speed: Float)
    
    /**
     * Mark a book as completed.
     */
    suspend fun markAsCompleted(bookId: String)
    
    /**
     * Clear progress for a book (start over).
     */
    suspend fun clearProgress(bookId: String)

    // ==================== Listening Sessions ====================
    
    /**
     * Start a new listening session.
     */
    suspend fun startSession(bookId: String, chapterIndex: Int): String
    
    /**
     * End the current listening session.
     */
    suspend fun endSession(sessionId: String, durationMs: Long, toChapter: Int)
    
    /**
     * Get the active session (if any).
     */
    suspend fun getActiveSession(): ListeningSessionEntity?
    
    /**
     * Get listening history for a book.
     */
    fun getSessionsForBook(bookId: String): Flow<List<ListeningSessionEntity>>

    // ==================== Bookmarks ====================
    
    /**
     * Add a bookmark at the current position.
     */
    suspend fun addBookmark(
        bookId: String,
        chapterIndex: Int,
        positionMs: Long,
        note: String? = null,
    ): String
    
    /**
     * Get bookmarks for a book.
     */
    fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>>
    
    /**
     * Delete a bookmark.
     */
    suspend fun deleteBookmark(bookmarkId: String)

    // ==================== Statistics ====================
    
    /**
     * Get total listening time.
     */
    suspend fun getTotalListeningTime(): Long
    
    /**
     * Get listening time for today.
     */
    suspend fun getTodayListeningTime(): Long
    
    /**
     * Get listening time for this week.
     */
    suspend fun getWeekListeningTime(): Long

    // ==================== Sync ====================
    
    /**
     * Sync all pending changes to the cloud.
     */
    suspend fun syncToCloud(): Result<Unit>
    
    /**
     * Fetch progress from cloud and merge with local.
     */
    suspend fun syncFromCloud(): Result<Unit>
    
    /**
     * Perform a full bidirectional sync.
     */
    suspend fun fullSync(): Result<Unit>
}
