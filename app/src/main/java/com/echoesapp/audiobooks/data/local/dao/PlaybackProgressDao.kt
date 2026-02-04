package com.echoesapp.audiobooks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.echoesapp.audiobooks.data.local.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {

    // ==================== Progress Queries ====================
    
    @Query("SELECT * FROM playback_progress WHERE bookId = :bookId")
    suspend fun getProgressForBook(bookId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE bookId = :bookId")
    fun observeProgressForBook(bookId: String): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress ORDER BY lastPlayedAt DESC")
    fun getAllProgress(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE isCompleted = 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentProgress(limit: Int): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE isCompleted = 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun getRecentProgressSnapshot(limit: Int): List<PlaybackProgressEntity>
    
    @Query("SELECT * FROM playback_progress WHERE isCompleted = 1 ORDER BY lastPlayedAt DESC")
    fun getCompletedBooks(): Flow<List<PlaybackProgressEntity>>

    // ==================== Progress Updates ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: PlaybackProgressEntity)

    @Query("""
        UPDATE playback_progress 
        SET chapterIndex = :chapterIndex, 
            positionMs = :positionMs, 
            lastPlayedAt = :lastPlayedAt,
            needsSync = 1
        WHERE bookId = :bookId
    """)
    suspend fun updatePosition(
        bookId: String,
        chapterIndex: Int,
        positionMs: Long,
        lastPlayedAt: Long = System.currentTimeMillis(),
    )
    
    @Query("""
        UPDATE playback_progress 
        SET playbackSpeed = :speed, needsSync = 1
        WHERE bookId = :bookId
    """)
    suspend fun updatePlaybackSpeed(bookId: String, speed: Float)
    
    @Query("""
        UPDATE playback_progress 
        SET isCompleted = :completed, needsSync = 1
        WHERE bookId = :bookId
    """)
    suspend fun updateCompleted(bookId: String, completed: Boolean)
    
    @Query("""
        UPDATE playback_progress 
        SET totalListenedMs = totalListenedMs + :additionalMs, needsSync = 1
        WHERE bookId = :bookId
    """)
    suspend fun addListeningTime(bookId: String, additionalMs: Long)

    // ==================== Sync Support ====================
    
    @Query("SELECT * FROM playback_progress WHERE needsSync = 1")
    suspend fun getUnsyncedProgress(): List<PlaybackProgressEntity>
    
    @Query("UPDATE playback_progress SET needsSync = 0 WHERE bookId = :bookId")
    suspend fun markAsSynced(bookId: String)
    
    @Query("UPDATE playback_progress SET needsSync = 0 WHERE bookId IN (:bookIds)")
    suspend fun markMultipleAsSynced(bookIds: List<String>)

    // ==================== Cleanup ====================
    
    @Query("DELETE FROM playback_progress WHERE bookId = :bookId")
    suspend fun deleteProgressForBook(bookId: String)

    @Query("DELETE FROM playback_progress")
    suspend fun deleteAllProgress()

    @Query("SELECT EXISTS(SELECT 1 FROM playback_progress WHERE bookId = :bookId)")
    suspend fun hasProgressForBook(bookId: String): Boolean
}
