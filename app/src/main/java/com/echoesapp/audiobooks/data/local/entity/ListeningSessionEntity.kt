package com.echoesapp.audiobooks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Records individual listening sessions for history and statistics.
 * 
 * A session starts when playback begins and ends when:
 * - User pauses/stops
 * - User switches to a different book
 * - App goes to background for extended period
 */
@Entity(
    tableName = "listening_sessions",
    foreignKeys = [
        ForeignKey(
            entity = AudiobookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("startedAt")],
)
data class ListeningSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val startedAt: Long,            // Timestamp when session started
    val endedAt: Long? = null,      // Timestamp when session ended (null if ongoing)
    val durationMs: Long = 0,       // Actual listening time (excludes pauses)
    val fromChapter: Int,           // Chapter when session started
    val toChapter: Int,             // Chapter when session ended
    val needsSync: Boolean = true,  // Flag for pending cloud sync
)
