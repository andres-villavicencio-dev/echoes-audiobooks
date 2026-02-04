package com.echoesapp.audiobooks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local storage for playback progress.
 * 
 * This is the source of truth for playback state, synced to cloud periodically.
 */
@Entity(
    tableName = "playback_progress",
    foreignKeys = [
        ForeignKey(
            entity = AudiobookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("lastPlayedAt")],
)
data class PlaybackProgressEntity(
    @PrimaryKey
    val bookId: String,
    val chapterIndex: Int,              // Chapter number (0-indexed)
    val positionMs: Long,               // Position in milliseconds within the chapter
    val playbackSpeed: Float = 1.0f,    // User's preferred speed for this book
    val isCompleted: Boolean = false,   // Has the user finished this book?
    val totalListenedMs: Long = 0,      // Total time spent listening to this book
    val lastPlayedAt: Long,             // Timestamp of last playback
    val needsSync: Boolean = true,      // Flag for pending cloud sync
)
