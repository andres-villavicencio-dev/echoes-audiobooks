package com.echoesapp.audiobooks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index("bookId"), Index("lastPlayed")],
)
data class PlaybackProgressEntity(
    @PrimaryKey
    val bookId: String,
    val chapterId: String,
    val position: Long, // Position in milliseconds within the chapter
    val lastPlayed: Long, // Timestamp of last playback
)
