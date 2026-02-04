package com.echoesapp.audiobooks.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.echoesapp.audiobooks.data.repository.ProgressRepository
import com.echoesapp.audiobooks.domain.model.Audiobook
import com.echoesapp.audiobooks.domain.model.Chapter
import com.echoesapp.audiobooks.domain.model.PlaybackState
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for audio playback using Media3.
 * Provides a clean API for controlling audiobook playback with StateFlow-based state updates.
 * 
 * Integrates with ProgressRepository for:
 * - Automatic progress saving (every 10 seconds while playing)
 * - Listening session tracking
 * - Per-book playback speed memory
 * - Cloud sync support
 */
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sleepTimer: SleepTimer,
    private val progressRepository: ProgressRepository,
) {
    companion object {
        private const val TAG = "PlayerManager"
        private const val SAVE_INTERVAL_MS = 10_000L  // Save progress every 10 seconds
        private const val SYNC_INTERVAL_MS = 30_000L  // Sync to cloud every 30 seconds
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentAudiobook: Audiobook? = null
    private var currentChapterIndex: Int = 0
    
    // Pending play action (for when controller isn't ready yet)
    private var pendingPlayAction: (() -> Unit)? = null
    
    // Session tracking
    private var currentSessionId: String? = null
    private var sessionStartTime: Long = 0L
    private var lastSaveTime: Long = 0L
    private var lastSyncTime: Long = 0L

    private var isInitialized = false

    init {
        observeSleepTimer()
        startPositionUpdates()
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            isInitialized = true
            initializeController()
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                controller = controllerFuture?.get()
                controller?.addListener(playerListener)
                Log.d(TAG, "MediaController ready")
                
                // Execute any pending play action
                pendingPlayAction?.invoke()
                pendingPlayAction = null
            },
            MoreExecutors.directExecutor()
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            
            if (isPlaying) {
                onPlaybackStarted()
            } else {
                onPlaybackPaused()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> handleChapterEnded()
                Player.STATE_READY -> updateDuration()
                else -> {}
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _playbackState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                // Auto-transition to next chapter
                val newIndex = controller?.currentMediaItemIndex ?: return
                if (newIndex != currentChapterIndex) {
                    currentChapterIndex = newIndex
                    updateCurrentChapter()
                    saveProgressNow() // Save when chapter changes
                }
            }
        }
    }

    /**
     * Called when playback starts or resumes.
     */
    private fun onPlaybackStarted() {
        val audiobook = currentAudiobook ?: return
        
        scope.launch(Dispatchers.IO) {
            // Start a new listening session if we don't have one
            if (currentSessionId == null) {
                currentSessionId = progressRepository.startSession(audiobook.id, currentChapterIndex)
                sessionStartTime = System.currentTimeMillis()
                Log.d(TAG, "Started session: $currentSessionId")
            }
        }
    }

    /**
     * Called when playback pauses.
     */
    private fun onPlaybackPaused() {
        // Save progress immediately on pause
        saveProgressNow()
        
        // End the current session
        endCurrentSession()
    }

    /**
     * End the current listening session.
     */
    private fun endCurrentSession() {
        val sessionId = currentSessionId ?: return
        val duration = System.currentTimeMillis() - sessionStartTime
        
        scope.launch(Dispatchers.IO) {
            progressRepository.endSession(
                sessionId = sessionId,
                durationMs = duration,
                toChapter = currentChapterIndex
            )
            Log.d(TAG, "Ended session: $sessionId, duration: ${duration / 1000}s")
        }
        
        currentSessionId = null
        sessionStartTime = 0L
    }

    /**
     * Save progress immediately.
     */
    private fun saveProgressNow() {
        val audiobook = currentAudiobook ?: return
        val position = controller?.currentPosition ?: return
        
        scope.launch(Dispatchers.IO) {
            progressRepository.savePosition(
                bookId = audiobook.id,
                chapterIndex = currentChapterIndex,
                positionMs = position
            )
            lastSaveTime = System.currentTimeMillis()
            Log.d(TAG, "Saved progress: chapter=$currentChapterIndex, position=$position")
        }
    }

    /**
     * Sync progress to cloud.
     */
    private fun syncToCloud() {
        scope.launch(Dispatchers.IO) {
            progressRepository.syncToCloud()
            lastSyncTime = System.currentTimeMillis()
            Log.d(TAG, "Synced to cloud")
        }
    }

    /**
     * Start playing an audiobook from a specific chapter.
     */
    fun play(audiobook: Audiobook, startChapterIndex: Int = 0) {
        ensureInitialized()
        currentAudiobook = audiobook
        currentChapterIndex = startChapterIndex.coerceIn(0, audiobook.chapters.lastIndex)

        // Load saved playback speed for this book
        scope.launch(Dispatchers.IO) {
            val savedProgress = progressRepository.getProgress(audiobook.id)
            val savedSpeed = savedProgress?.playbackSpeed ?: 1.0f
            
            launch(Dispatchers.Main) {
                controller?.setPlaybackSpeed(savedSpeed)
                _playbackState.update { it.copy(playbackSpeed = savedSpeed) }
            }
        }

        val mediaItems = audiobook.chapters.mapIndexed { index, chapter ->
            createMediaItem(audiobook, chapter, index)
        }

        // Helper to start playback
        val startPlayback: () -> Unit = {
            controller?.apply {
                setMediaItems(mediaItems, currentChapterIndex, 0)
                prepare()
                play()
            }
            Log.d(TAG, "Started playback: ${audiobook.title}, chapter $currentChapterIndex")
        }

        // If controller is ready, play now; otherwise queue for later
        if (controller != null) {
            startPlayback()
        } else {
            Log.d(TAG, "Controller not ready, queueing play action")
            pendingPlayAction = startPlayback
        }

        _playbackState.update {
            it.copy(
                audiobook = audiobook,
                currentChapter = audiobook.chapters.getOrNull(currentChapterIndex),
                isPlaying = true,
                position = 0L,
                duration = audiobook.chapters.getOrNull(currentChapterIndex)?.duration ?: 0L,
            )
        }
    }

    /**
     * Resume playback from a saved progress position.
     */
    fun playFromProgress(
        audiobook: Audiobook,
        chapterId: String,
        positionInChapter: Long,
    ) {
        ensureInitialized()
        currentAudiobook = audiobook
        currentChapterIndex = audiobook.chapters.indexOfFirst { it.id == chapterId }
            .takeIf { it >= 0 } ?: 0

        // Load saved playback speed for this book
        scope.launch(Dispatchers.IO) {
            val savedProgress = progressRepository.getProgress(audiobook.id)
            val savedSpeed = savedProgress?.playbackSpeed ?: 1.0f
            
            launch(Dispatchers.Main) {
                controller?.setPlaybackSpeed(savedSpeed)
                _playbackState.update { it.copy(playbackSpeed = savedSpeed) }
            }
        }

        val mediaItems = audiobook.chapters.mapIndexed { index, chapter ->
            createMediaItem(audiobook, chapter, index)
        }

        val startPlayback: () -> Unit = {
            controller?.apply {
                setMediaItems(mediaItems, currentChapterIndex, positionInChapter)
                prepare()
                play()
            }
            Log.d(TAG, "Started playback from progress: ${audiobook.title}")
        }

        if (controller != null) {
            startPlayback()
        } else {
            pendingPlayAction = startPlayback
        }

        _playbackState.update {
            it.copy(
                audiobook = audiobook,
                currentChapter = audiobook.chapters.getOrNull(currentChapterIndex),
                isPlaying = true,
                position = positionInChapter,
                duration = audiobook.chapters.getOrNull(currentChapterIndex)?.duration ?: 0L,
            )
        }
    }

    /**
     * Resume playback from saved progress using chapter index.
     */
    fun playFromProgress(
        audiobook: Audiobook,
        chapterIndex: Int,
        positionInChapter: Long,
        playbackSpeed: Float = 1.0f,
    ) {
        ensureInitialized()
        currentAudiobook = audiobook
        currentChapterIndex = chapterIndex.coerceIn(0, audiobook.chapters.lastIndex)

        val mediaItems = audiobook.chapters.mapIndexed { index, chapter ->
            createMediaItem(audiobook, chapter, index)
        }

        val startPlayback: () -> Unit = {
            controller?.apply {
                setMediaItems(mediaItems, currentChapterIndex, positionInChapter)
                setPlaybackSpeed(playbackSpeed)
                prepare()
                play()
            }
            Log.d(TAG, "Started playback from progress (index): ${audiobook.title}")
        }

        if (controller != null) {
            startPlayback()
        } else {
            pendingPlayAction = startPlayback
        }

        _playbackState.update {
            it.copy(
                audiobook = audiobook,
                currentChapter = audiobook.chapters.getOrNull(currentChapterIndex),
                isPlaying = true,
                position = positionInChapter,
                duration = audiobook.chapters.getOrNull(currentChapterIndex)?.duration ?: 0L,
                playbackSpeed = playbackSpeed,
            )
        }
    }

    private fun createMediaItem(audiobook: Audiobook, chapter: Chapter, index: Int): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(chapter.title)
            .setArtist(audiobook.author)
            .setAlbumTitle(audiobook.title)
            .setTrackNumber(index + 1)
            .setArtworkUri(audiobook.coverUrl?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(chapter.id)
            .setUri(chapter.audioUrl)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.parse(chapter.audioUrl))
                    .build()
            )
            .build()
    }

    fun pause() {
        controller?.pause()
    }

    fun resume() {
        controller?.play()
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) pause() else resume()
        }
    }

    /**
     * Seek to a specific position in the current chapter.
     */
    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0))
        _playbackState.update { it.copy(position = positionMs.coerceAtLeast(0)) }
    }

    /**
     * Skip forward by 30 seconds.
     */
    fun skipForward() {
        controller?.let {
            val newPosition = (it.currentPosition + PlaybackService.SKIP_FORWARD_MS)
                .coerceAtMost(it.duration)
            seekTo(newPosition)
        }
    }

    /**
     * Skip back by 15 seconds.
     */
    fun skipBack() {
        controller?.let {
            val newPosition = (it.currentPosition - PlaybackService.SKIP_BACK_MS)
                .coerceAtLeast(0)
            seekTo(newPosition)
        }
    }

    /**
     * Go to the next chapter.
     */
    fun nextChapter() {
        val audiobook = currentAudiobook ?: return
        if (currentChapterIndex < audiobook.chapters.lastIndex) {
            controller?.seekToNextMediaItem()
            currentChapterIndex++
            updateCurrentChapter()
            saveProgressNow()
        }
    }

    /**
     * Go to the previous chapter.
     */
    fun previousChapter() {
        controller?.let {
            // If we're more than 3 seconds into the chapter, restart it
            if (it.currentPosition > 3000) {
                seekTo(0)
            } else if (currentChapterIndex > 0) {
                it.seekToPreviousMediaItem()
                currentChapterIndex--
                updateCurrentChapter()
                saveProgressNow()
            }
        }
    }

    /**
     * Jump to a specific chapter.
     */
    fun goToChapter(chapterIndex: Int) {
        val audiobook = currentAudiobook ?: return
        if (chapterIndex in 0..audiobook.chapters.lastIndex) {
            controller?.seekTo(chapterIndex, 0)
            currentChapterIndex = chapterIndex
            updateCurrentChapter()
            saveProgressNow()
        }
    }

    /**
     * Set playback speed (0.5x to 2.0x).
     * Also saves the speed preference for this book.
     */
    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        controller?.setPlaybackSpeed(clampedSpeed)
        _playbackState.update { it.copy(playbackSpeed = clampedSpeed) }
        
        // Save speed preference for this book
        val audiobook = currentAudiobook ?: return
        scope.launch(Dispatchers.IO) {
            progressRepository.updatePlaybackSpeed(audiobook.id, clampedSpeed)
        }
    }

    /**
     * Cycle through common playback speeds.
     */
    fun cyclePlaybackSpeed() {
        val currentSpeed = _playbackState.value.playbackSpeed
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        val currentIndex = speeds.indexOfFirst { it >= currentSpeed }
        val nextIndex = if (currentIndex == -1 || currentIndex == speeds.lastIndex) 0 else currentIndex + 1
        setPlaybackSpeed(speeds[nextIndex])
    }

    /**
     * Add a bookmark at the current position.
     */
    fun addBookmark(note: String? = null) {
        val audiobook = currentAudiobook ?: return
        val position = controller?.currentPosition ?: return
        
        scope.launch(Dispatchers.IO) {
            val bookmarkId = progressRepository.addBookmark(
                bookId = audiobook.id,
                chapterIndex = currentChapterIndex,
                positionMs = position,
                note = note
            )
            Log.d(TAG, "Added bookmark: $bookmarkId at chapter=$currentChapterIndex, position=$position")
        }
    }

    /**
     * Set sleep timer using predefined option.
     */
    fun setSleepTimer(option: SleepTimerOption) {
        when (option) {
            SleepTimerOption.OFF -> {
                sleepTimer.cancel()
                _playbackState.update { it.copy(sleepTimerMinutes = null) }
            }
            SleepTimerOption.END_OF_CHAPTER -> {
                sleepTimer.setEndOfChapter {
                    pause()
                    _playbackState.update { it.copy(sleepTimerMinutes = null) }
                }
                _playbackState.update { it.copy(sleepTimerMinutes = -1) } // -1 indicates end of chapter
            }
            else -> startSleepTimer(option.minutes)
        }
    }

    /**
     * Start sleep timer with custom minutes.
     */
    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            sleepTimer.cancel()
            _playbackState.update { it.copy(sleepTimerMinutes = null) }
            return
        }
        sleepTimer.start(minutes) {
            pause()
            _playbackState.update { it.copy(sleepTimerMinutes = null) }
        }
        _playbackState.update { it.copy(sleepTimerMinutes = minutes) }
    }

    /**
     * Get current playback position in milliseconds.
     */
    fun getCurrentPosition(): Long = controller?.currentPosition ?: 0L

    /**
     * Get current chapter ID.
     */
    fun getCurrentChapterId(): String? = currentAudiobook?.chapters?.getOrNull(currentChapterIndex)?.id

    /**
     * Get current chapter index.
     */
    fun getCurrentChapterIndex(): Int = currentChapterIndex

    /**
     * Stop playback and release resources.
     */
    fun stop() {
        // Save final progress before stopping
        saveProgressNow()
        endCurrentSession()
        syncToCloud()
        
        controller?.stop()
        controller?.clearMediaItems()
        sleepTimer.cancel()
        currentAudiobook = null
        currentChapterIndex = 0
        _playbackState.value = PlaybackState()
    }

    private fun handleChapterEnded() {
        val audiobook = currentAudiobook ?: return

        // Check if sleep timer is set to end of chapter
        if (sleepTimer.isEndOfChapterMode()) {
            pause()
            sleepTimer.cancel()
            _playbackState.update { it.copy(sleepTimerMinutes = null) }
            return
        }

        // Auto-advance to next chapter if available
        if (currentChapterIndex >= audiobook.chapters.lastIndex) {
            // Audiobook finished
            _playbackState.update { it.copy(isPlaying = false) }
            
            // Mark as completed
            scope.launch(Dispatchers.IO) {
                progressRepository.markAsCompleted(audiobook.id)
                Log.d(TAG, "Audiobook completed: ${audiobook.title}")
            }
        }
    }

    private fun updateCurrentChapter() {
        val chapter = currentAudiobook?.chapters?.getOrNull(currentChapterIndex)
        _playbackState.update {
            it.copy(
                currentChapter = chapter,
                position = 0L,
                duration = chapter?.duration ?: 0L,
            )
        }
    }

    private fun updateDuration() {
        controller?.let { ctrl ->
            _playbackState.update { it.copy(duration = ctrl.duration.coerceAtLeast(0)) }
        }
    }

    private fun observeSleepTimer() {
        scope.launch {
            sleepTimer.remainingTime.collect { remaining ->
                if (remaining != null && remaining > 0) {
                    _playbackState.update { it.copy(sleepTimerMinutes = (remaining / 60000).toInt()) }
                }
            }
        }
    }

    private fun startPositionUpdates() {
        scope.launch {
            while (isActive) {
                controller?.let { ctrl ->
                    if (ctrl.isPlaying) {
                        _playbackState.update { it.copy(position = ctrl.currentPosition) }
                        
                        // Periodic progress save
                        val now = System.currentTimeMillis()
                        if (now - lastSaveTime >= SAVE_INTERVAL_MS) {
                            saveProgressNow()
                        }
                        
                        // Periodic cloud sync
                        if (now - lastSyncTime >= SYNC_INTERVAL_MS) {
                            syncToCloud()
                        }
                    }
                }
                delay(500) // Update every 500ms
            }
        }
    }

    fun release() {
        // Final save before release
        saveProgressNow()
        endCurrentSession()
        syncToCloud()
        
        sleepTimer.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        scope.cancel()
    }
}

enum class SleepTimerOption(val minutes: Int, val label: String) {
    OFF(0, "Off"),
    MINUTES_15(15, "15 minutes"),
    MINUTES_30(30, "30 minutes"),
    MINUTES_45(45, "45 minutes"),
    MINUTES_60(60, "60 minutes"),
    END_OF_CHAPTER(-1, "End of chapter"),
}
