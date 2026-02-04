package com.echoesapp.audiobooks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.echoesapp.audiobooks.data.local.entity.ListeningSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningSessionDao {

    // ==================== Session Queries ====================
    
    @Query("SELECT * FROM listening_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): ListeningSessionEntity?
    
    @Query("SELECT * FROM listening_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): ListeningSessionEntity?
    
    @Query("SELECT * FROM listening_sessions WHERE bookId = :bookId ORDER BY startedAt DESC")
    fun getSessionsForBook(bookId: String): Flow<List<ListeningSessionEntity>>
    
    @Query("SELECT * FROM listening_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<ListeningSessionEntity>>
    
    @Query("SELECT * FROM listening_sessions ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentSessionsSnapshot(limit: Int): List<ListeningSessionEntity>

    // ==================== Statistics ====================
    
    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM listening_sessions")
    suspend fun getTotalListeningTime(): Long
    
    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM listening_sessions WHERE startedAt >= :since")
    suspend fun getListeningTimeSince(since: Long): Long
    
    @Query("SELECT COUNT(DISTINCT bookId) FROM listening_sessions")
    suspend fun getUniqueBookCount(): Int
    
    @Query("SELECT COUNT(*) FROM listening_sessions")
    suspend fun getTotalSessionCount(): Int

    // ==================== Session Management ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ListeningSessionEntity)
    
    @Update
    suspend fun updateSession(session: ListeningSessionEntity)
    
    @Query("""
        UPDATE listening_sessions 
        SET endedAt = :endedAt, 
            durationMs = :durationMs, 
            toChapter = :toChapter,
            needsSync = 1
        WHERE id = :sessionId
    """)
    suspend fun endSession(
        sessionId: String,
        endedAt: Long,
        durationMs: Long,
        toChapter: Int
    )

    // ==================== Sync Support ====================
    
    @Query("SELECT * FROM listening_sessions WHERE needsSync = 1")
    suspend fun getUnsyncedSessions(): List<ListeningSessionEntity>
    
    @Query("UPDATE listening_sessions SET needsSync = 0 WHERE id = :sessionId")
    suspend fun markAsSynced(sessionId: String)
    
    @Query("UPDATE listening_sessions SET needsSync = 0 WHERE id IN (:sessionIds)")
    suspend fun markMultipleAsSynced(sessionIds: List<String>)

    // ==================== Cleanup ====================
    
    @Query("DELETE FROM listening_sessions WHERE bookId = :bookId")
    suspend fun deleteSessionsForBook(bookId: String)
    
    @Query("DELETE FROM listening_sessions WHERE startedAt < :before")
    suspend fun deleteOldSessions(before: Long)
}
