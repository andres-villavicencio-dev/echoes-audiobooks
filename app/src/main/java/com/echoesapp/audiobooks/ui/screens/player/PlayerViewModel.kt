package com.echoesapp.audiobooks.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoesapp.audiobooks.domain.model.Audiobook
import com.echoesapp.audiobooks.domain.model.Category
import com.echoesapp.audiobooks.domain.model.Chapter
import com.echoesapp.audiobooks.domain.model.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    // TODO: Inject PlayerManager when available
) : ViewModel() {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _showChapterSelector = MutableStateFlow(false)
    val showChapterSelector: StateFlow<Boolean> = _showChapterSelector.asStateFlow()

    private val _showSleepTimerPicker = MutableStateFlow(false)
    val showSleepTimerPicker: StateFlow<Boolean> = _showSleepTimerPicker.asStateFlow()

    private var progressJob: Job? = null

    init {
        // TODO: Connect to PlayerManager and observe actual playback state
        loadMockPlaybackState()
    }

    fun togglePlayPause() {
        val currentState = _playbackState.value
        _playbackState.update { it.copy(isPlaying = !it.isPlaying) }
        
        if (!currentState.isPlaying) {
            startProgressSimulation()
        } else {
            progressJob?.cancel()
        }
    }

    fun seekTo(position: Long) {
        _playbackState.update { it.copy(position = position) }
        // TODO: Call PlayerManager.seekTo(position)
    }

    fun skipPrevious() {
        val state = _playbackState.value
        val audiobook = state.audiobook ?: return
        val currentIndex = audiobook.chapters.indexOfFirst { it.id == state.currentChapter?.id }
        
        if (state.position > 3000) {
            // If more than 3 seconds in, restart current chapter
            seekTo(0)
        } else if (currentIndex > 0) {
            // Go to previous chapter
            val prevChapter = audiobook.chapters[currentIndex - 1]
            _playbackState.update {
                it.copy(
                    currentChapter = prevChapter,
                    position = 0,
                    duration = prevChapter.duration,
                )
            }
        }
    }

    fun skipNext() {
        val state = _playbackState.value
        val audiobook = state.audiobook ?: return
        val currentIndex = audiobook.chapters.indexOfFirst { it.id == state.currentChapter?.id }
        
        if (currentIndex < audiobook.chapters.size - 1) {
            val nextChapter = audiobook.chapters[currentIndex + 1]
            _playbackState.update {
                it.copy(
                    currentChapter = nextChapter,
                    position = 0,
                    duration = nextChapter.duration,
                )
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.update { it.copy(playbackSpeed = speed) }
        // TODO: Call PlayerManager.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int?) {
        _playbackState.update { it.copy(sleepTimerMinutes = minutes) }
        _showSleepTimerPicker.value = false
        // TODO: Implement actual sleep timer logic
    }

    fun selectChapter(chapter: Chapter) {
        _playbackState.update {
            it.copy(
                currentChapter = chapter,
                position = 0,
                duration = chapter.duration,
            )
        }
        _showChapterSelector.value = false
    }

    fun toggleChapterSelector() {
        _showChapterSelector.value = !_showChapterSelector.value
    }

    fun toggleSleepTimerPicker() {
        _showSleepTimerPicker.value = !_showSleepTimerPicker.value
    }

    private fun startProgressSimulation() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_playbackState.value.isPlaying) {
                delay(1000)
                val state = _playbackState.value
                val newPosition = state.position + (1000 * state.playbackSpeed).toLong()
                
                if (newPosition >= state.duration) {
                    // Auto-advance to next chapter
                    skipNext()
                } else {
                    _playbackState.update { it.copy(position = newPosition) }
                }
            }
        }
    }

    private fun loadMockPlaybackState() {
        val chapters = listOf(
            Chapter("ch1", "Chapter 1: The Beginning", "https://example.com/ch1.mp3", 1800000, 0),
            Chapter("ch2", "Chapter 2: The Journey", "https://example.com/ch2.mp3", 2100000, 1800000),
            Chapter("ch3", "Chapter 3: The Discovery", "https://example.com/ch3.mp3", 1500000, 3900000),
        )

        val audiobook = Audiobook(
            id = "1",
            title = "Pride and Prejudice",
            author = "Jane Austen",
            narrator = "Rosamund Pike",
            description = "A classic tale of love and social standing.",
            coverUrl = "https://picsum.photos/seed/pride/300/400",
            duration = 42000000,
            chapters = chapters,
            category = Category.CLASSIC,
        )

        _playbackState.value = PlaybackState(
            audiobook = audiobook,
            currentChapter = chapters[0],
            isPlaying = false,
            position = 450000, // 7:30
            duration = chapters[0].duration,
            playbackSpeed = 1.0f,
        )
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}
