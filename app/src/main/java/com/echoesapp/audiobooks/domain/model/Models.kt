package com.echoesapp.audiobooks.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Audiobook(
    val id: String,
    val title: String,
    val author: String,
    val narrator: String,
    val description: String,
    val coverUrl: String?,
    val duration: Long, // Total duration in milliseconds
    val chapters: List<Chapter>,
    val category: Category,
    val isDownloaded: Boolean = false,
    val releaseDate: String? = null,
)

@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val audioUrl: String,
    val duration: Long, // Duration in milliseconds
    val startPosition: Long, // Start position in the full audiobook
)

@Serializable
enum class Category {
    CLASSIC,
    AI_STORY,
    PODCAST,
}

data class PlaybackState(
    val audiobook: Audiobook? = null,
    val currentChapter: Chapter? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutes: Int? = null,
)

data class LibraryState(
    val continueListening: List<AudiobookProgress> = emptyList(),
    val classics: List<Audiobook> = emptyList(),
    val aiStories: List<Audiobook> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class AudiobookProgress(
    val audiobook: Audiobook,
    val currentChapterId: String,
    val positionInChapter: Long,
    val percentComplete: Float,
    val lastPlayedAt: Long,
)
