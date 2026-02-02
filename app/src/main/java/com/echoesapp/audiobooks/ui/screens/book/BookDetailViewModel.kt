package com.echoesapp.audiobooks.ui.screens.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoesapp.audiobooks.data.repository.AudiobookRepository
import com.echoesapp.audiobooks.domain.model.Audiobook
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val audiobook: Audiobook? = null,
    val currentChapterId: String? = null,
    val completedChapterIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AudiobookRepository,
) : ViewModel() {

    private val bookId: String = savedStateHandle.get<String>("bookId") ?: ""
    
    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        loadBookDetails()
        observeBook()
    }

    private fun observeBook() {
        viewModelScope.launch {
            repository.observeAudiobook(bookId).collect { audiobook ->
                if (audiobook != null) {
                    _uiState.update {
                        it.copy(
                            audiobook = audiobook,
                            currentChapterId = it.currentChapterId ?: audiobook.chapters.firstOrNull()?.id,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
            }
        }
    }

    fun loadBookDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            repository.getAudiobook(bookId)
                .onSuccess { audiobook ->
                    _uiState.update {
                        it.copy(
                            audiobook = audiobook,
                            currentChapterId = audiobook.chapters.firstOrNull()?.id,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false, 
                            error = e.message ?: "Book not found"
                        )
                    }
                }
        }
    }

    fun playBook() {
        val audiobook = _uiState.value.audiobook ?: return
        val chapterId = _uiState.value.currentChapterId ?: audiobook.chapters.firstOrNull()?.id
        
        // TODO: Call PlayerManager to start playback
        // playerManager.play(audiobook, chapterId)
    }

    fun playChapter(chapterId: String) {
        _uiState.update { it.copy(currentChapterId = chapterId) }
        playBook()
    }
}
