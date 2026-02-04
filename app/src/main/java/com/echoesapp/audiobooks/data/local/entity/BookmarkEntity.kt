package com.echoesapp.audiobooks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * User-saved positions in audiobooks.
 * 
 * Allows users to save favorite moments with optional notes.
 */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = AudiobookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("createdAt")],
)
data class BookmarkEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val chapterIndex: Int,
    val positionMs: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val needsSync: Boolean = true,
)
