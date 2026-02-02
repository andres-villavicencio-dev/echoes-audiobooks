package com.echoesapp.audiobooks.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.echoesapp.audiobooks.domain.model.Audiobook
import com.echoesapp.audiobooks.domain.model.Category

@Entity(tableName = "audiobooks")
data class AudiobookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val narrator: String,
    val description: String,
    val coverUrl: String?,
    val duration: Long,
    val category: String,
    val isDownloaded: Boolean,
    val releaseDate: String?,
    val lastUpdated: Long = System.currentTimeMillis(),
)

fun AudiobookEntity.toDomain(chapters: List<ChapterEntity>): Audiobook = Audiobook(
    id = id,
    title = title,
    author = author,
    narrator = narrator,
    description = description,
    coverUrl = coverUrl,
    duration = duration,
    chapters = chapters.map { it.toDomain() },
    category = Category.valueOf(category),
    isDownloaded = isDownloaded,
    releaseDate = releaseDate,
)

fun Audiobook.toEntity(): AudiobookEntity = AudiobookEntity(
    id = id,
    title = title,
    author = author,
    narrator = narrator,
    description = description,
    coverUrl = coverUrl,
    duration = duration,
    category = category.name,
    isDownloaded = isDownloaded,
    releaseDate = releaseDate,
)
