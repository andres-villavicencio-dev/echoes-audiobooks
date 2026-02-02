package com.echoesapp.audiobooks.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoesapp.audiobooks.data.repository.AudiobookRepository
import com.echoesapp.audiobooks.domain.model.Audiobook
import com.echoesapp.audiobooks.domain.model.Category
import com.echoesapp.audiobooks.domain.model.LibraryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: AudiobookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryState())
    val uiState: StateFlow<LibraryState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    init {
        loadLibrary()
        observeLibrary()
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            combine(
                repository.getAudiobooksByCategory(Category.CLASSIC),
                repository.getAudiobooksByCategory(Category.AI_STORY),
                repository.getContinueListening(5),
            ) { classics, aiStories, continueListening ->
                LibraryState(
                    classics = classics,
                    aiStories = aiStories,
                    continueListening = continueListening,
                    isLoading = false,
                )
            }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Refresh from network
            repository.refreshAudiobooks()
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(error = e.message ?: "Failed to load library", isLoading = false) 
                    }
                }
        }
    }

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }

    fun getFilteredBooks(): List<Audiobook> {
        val state = _uiState.value
        return when (_selectedCategory.value) {
            Category.CLASSIC -> state.classics
            Category.AI_STORY -> state.aiStories
            Category.PODCAST -> emptyList()
            null -> state.classics + state.aiStories
        }
    }
}
