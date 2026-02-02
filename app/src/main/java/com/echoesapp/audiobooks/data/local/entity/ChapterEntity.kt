package com.echoesapp.audiobooks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.echoesapp.audiobooks.domain.model.Chapter

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = AudiobookEntity::class,
            parentColumns = ["id"],
            childColumns = ["audiobookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("audiobookId")],
)
data class ChapterEntity(
    @PrimaryKey
    val id: String,
    val audiobookId: String,
    val title: String,
    val audioUrl: String,
    val duration: Long,
    val startPosition: Long,
    val chapterIndex: Int,
)

fun ChapterEntity.toDomain(): Chapter = Chapter(
    id = id,
    title = title,
    audioUrl = audioUrl,
    duration = duration,
    startPosition = startPosition,
)

fun Chapter.toEntity(audiobookId: String, index: Int): ChapterEntity = ChapterEntity(
    id = id,
    audiobookId = audiobookId,
    title = title,
    audioUrl = audioUrl,
    duration = duration,
    startPosition = startPosition,
    chapterIndex = index,
)
