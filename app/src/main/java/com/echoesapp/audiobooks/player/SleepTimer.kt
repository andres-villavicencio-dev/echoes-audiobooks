package com.echoesapp.audiobooks.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sleep timer for audiobook playback.
 * Supports countdown timers (15, 30, 45, 60 minutes) and end-of-chapter mode.
 */
@Singleton
class SleepTimer @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private var onTimerComplete: (() -> Unit)? = null

    private val _remainingTime = MutableStateFlow<Long?>(null)
    val remainingTime: StateFlow<Long?> = _remainingTime.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var endOfChapterMode = false

    /**
     * Start a countdown timer for the specified number of minutes.
     * @param minutes Duration in minutes
     * @param onComplete Callback when timer completes
     */
    fun start(minutes: Int, onComplete: () -> Unit) {
        cancel()

        if (minutes <= 0) return

        endOfChapterMode = false
        onTimerComplete = onComplete
        val durationMs = minutes * 60 * 1000L

        _remainingTime.value = durationMs
        _isActive.value = true

        timerJob = scope.launch {
            var remaining = durationMs
            while (isActive && remaining > 0) {
                delay(1000)
                remaining -= 1000
                _remainingTime.value = remaining.coerceAtLeast(0)
            }

            if (remaining <= 0) {
                onTimerComplete?.invoke()
                reset()
            }
        }
    }

    /**
     * Set timer to trigger at end of current chapter.
     * The PlayerManager will call the callback when the chapter ends.
     */
    fun setEndOfChapter(onComplete: () -> Unit) {
        cancel()
        endOfChapterMode = true
        onTimerComplete = onComplete
        _isActive.value = true
        _remainingTime.value = null // No countdown for end-of-chapter
    }

    /**
     * Check if timer is in end-of-chapter mode.
     */
    fun isEndOfChapterMode(): Boolean = endOfChapterMode && _isActive.value

    /**
     * Add time to the current timer.
     * @param minutes Additional minutes to add
     */
    fun addTime(minutes: Int) {
        if (!_isActive.value || endOfChapterMode) return

        val currentRemaining = _remainingTime.value ?: return
        val additionalMs = minutes * 60 * 1000L
        _remainingTime.value = currentRemaining + additionalMs
    }

    /**
     * Cancel the current timer.
     */
    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        reset()
    }

    private fun reset() {
        _remainingTime.value = null
        _isActive.value = false
        onTimerComplete = null
        endOfChapterMode = false
    }

    /**
     * Get formatted remaining time string (e.g., "15:30").
     */
    fun getFormattedRemainingTime(): String? {
        if (endOfChapterMode) return "End of chapter"

        val remaining = _remainingTime.value ?: return null
        val minutes = (remaining / 60000).toInt()
        val seconds = ((remaining % 60000) / 1000).toInt()
        return "%d:%02d".format(minutes, seconds)
    }

    /**
     * Get remaining time in minutes (rounded up).
     */
    fun getRemainingMinutes(): Int? {
        if (endOfChapterMode) return null
        val remaining = _remainingTime.value ?: return null
        return ((remaining + 59999) / 60000).toInt() // Round up
    }
}
