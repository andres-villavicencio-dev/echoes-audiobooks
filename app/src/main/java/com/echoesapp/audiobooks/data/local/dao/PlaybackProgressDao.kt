package com.echoesapp.audiobooks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echoesapp.audiobooks.data.local.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {

    @Query("SELECT * FROM playback_progress WHERE bookId = :bookId")
    suspend fun getProgressForBook(bookId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE bookId = :bookId")
    fun observeProgressForBook(bookId: String): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress ORDER BY lastPlayed DESC")
    fun getAllProgress(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentProgress(limit: Int): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress ORDER BY lastPlayed DESC LIMIT :limit")
    suspend fun getRecentProgressSnapshot(limit: Int): List<PlaybackProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: PlaybackProgressEntity)

    @Query(
        """
        UPDATE playback_progress 
        SET chapterId = :chapterId, position = :position, lastPlayed = :lastPlayed 
        WHERE bookId = :bookId
        """,
    )
    suspend fun updateProgress(
        bookId: String,
        chapterId: String,
        position: Long,
        lastPlayed: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM playback_progress WHERE bookId = :bookId")
    suspend fun deleteProgressForBook(bookId: String)

    @Query("DELETE FROM playback_progress")
    suspend fun deleteAllProgress()

    @Query("SELECT EXISTS(SELECT 1 FROM playback_progress WHERE bookId = :bookId)")
    suspend fun hasProgressForBook(bookId: String): Boolean
}
