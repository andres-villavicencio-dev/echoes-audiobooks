package com.echoesapp.audiobooks.data.remote

import com.echoesapp.audiobooks.domain.model.Audiobook
import com.echoesapp.audiobooks.domain.model.Category
import com.echoesapp.audiobooks.domain.model.Chapter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase API models for Echoes.
 * Field names use snake_case to match PostgreSQL column names.
 */

@Serializable
data class AudiobookDto(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("author")
    val author: String,
    @SerialName("narrator")
    val narrator: String = "AI Narrator",
    @SerialName("description")
    val description: String? = null,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    @SerialName("duration_ms")
    val durationMs: Long = 0,
    @SerialName("category")
    val category: String,
    @SerialName("is_featured")
    val isFeatured: Boolean = false,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class ChapterDto(
    @SerialName("id")
    val id: String,
    @SerialName("audiobook_id")
    val audiobookId: String,
    @SerialName("title")
    val title: String,
    @SerialName("audio_url")
    val audioUrl: String,
    @SerialName("duration_ms")
    val durationMs: Long = 0,
    @SerialName("chapter_index")
    val chapterIndex: Int,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class ApiError(
    @SerialName("message")
    val message: String,
    @SerialName("code")
    val code: String? = null,
    @SerialName("hint")
    val hint: String? = null,
)

// Domain mappers
fun ChapterDto.toDomain(): Chapter = Chapter(
    id = id,
    title = title,
    audioUrl = audioUrl,
    duration = durationMs,
    startPosition = 0L, // Will be calculated from chapter index
)

fun AudiobookDto.toDomain(chapters: List<Chapter> = emptyList()): Audiobook = Audiobook(
    id = id,
    title = title,
    author = author,
    narrator = narrator,
    description = description ?: "",
    coverUrl = coverUrl,
    duration = durationMs,
    chapters = chapters,
    category = runCatching { Category.valueOf(category.uppercase()) }.getOrDefault(Category.CLASSIC),
    isDownloaded = false,
    releaseDate = releaseDate,
)

fun List<AudiobookDto>.toDomain(): List<Audiobook> = map { it.toDomain() }
