package com.echoesapp.audiobooks.ui.screens.player

import androidx.lifecycle.ViewModel
import com.echoesapp.audiobooks.domain.model.Chapter
import com.echoesapp.audiobooks.domain.model.PlaybackState
import com.echoesapp.audiobooks.player.PlayerManager
import com.echoesapp.audiobooks.player.SleepTimerOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState

    private val _showChapterSelector = MutableStateFlow(false)
    val showChapterSelector: StateFlow<Boolean> = _showChapterSelector.asStateFlow()

    private val _showSleepTimerPicker = MutableStateFlow(false)
    val showSleepTimerPicker: StateFlow<Boolean> = _showSleepTimerPicker.asStateFlow()

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(position: Long) {
        playerManager.seekTo(position)
    }

    fun skipPrevious() {
        playerManager.previousChapter()
    }

    fun skipNext() {
        playerManager.nextChapter()
    }

    fun skipBack() {
        playerManager.skipBack()
    }

    fun skipForward() {
        playerManager.skipForward()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
    }

    fun cyclePlaybackSpeed() {
        playerManager.cyclePlaybackSpeed()
    }

    fun setSleepTimer(minutes: Int?) {
        when (minutes) {
            null, 0 -> playerManager.setSleepTimer(SleepTimerOption.OFF)
            -1 -> playerManager.setSleepTimer(SleepTimerOption.END_OF_CHAPTER)
            else -> {
                // Handle any minute value by starting timer directly
                playerManager.startSleepTimer(minutes)
            }
        }
        _showSleepTimerPicker.value = false
    }

    fun selectChapter(chapter: Chapter) {
        val audiobook = playbackState.value.audiobook ?: return
        val chapterIndex = audiobook.chapters.indexOf(chapter)
        if (chapterIndex >= 0) {
            playerManager.goToChapter(chapterIndex)
        }
        _showChapterSelector.value = false
    }

    fun toggleChapterSelector() {
        _showChapterSelector.value = !_showChapterSelector.value
    }

    fun toggleSleepTimerPicker() {
        _showSleepTimerPicker.value = !_showSleepTimerPicker.value
    }
}
