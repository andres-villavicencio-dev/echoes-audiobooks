package com.echoesapp.audiobooks.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoPlayNext: Boolean = true,
    val skipSilence: Boolean = false,
    val defaultSleepTimerMinutes: Int? = null,
    val downloadOverWifiOnly: Boolean = true,
    val autoDownloadNewEpisodes: Boolean = false,
    val storageUsedMb: Long = 0L,
    val appVersion: String = "1.0.0",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // TODO: Inject DataStore<Preferences> or SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // TODO: Load from DataStore
            // For now, using defaults
            _uiState.update {
                it.copy(
                    isDarkMode = true,
                    defaultPlaybackSpeed = 1.0f,
                    autoPlayNext = true,
                    storageUsedMb = 256L,
                    appVersion = "1.0.0",
                )
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
        // TODO: Save to DataStore
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(defaultPlaybackSpeed = speed) }
        // TODO: Save to DataStore
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _uiState.update { it.copy(autoPlayNext = enabled) }
        // TODO: Save to DataStore
    }

    fun setSkipSilence(enabled: Boolean) {
        _uiState.update { it.copy(skipSilence = enabled) }
        // TODO: Save to DataStore
    }

    fun setDefaultSleepTimer(minutes: Int?) {
        _uiState.update { it.copy(defaultSleepTimerMinutes = minutes) }
        // TODO: Save to DataStore
    }

    fun setDownloadOverWifiOnly(enabled: Boolean) {
        _uiState.update { it.copy(downloadOverWifiOnly = enabled) }
        // TODO: Save to DataStore
    }

    fun setAutoDownloadNewEpisodes(enabled: Boolean) {
        _uiState.update { it.copy(autoDownloadNewEpisodes = enabled) }
        // TODO: Save to DataStore
    }

    fun clearDownloads() {
        viewModelScope.launch {
            // TODO: Clear downloaded files
            _uiState.update { it.copy(storageUsedMb = 0L) }
        }
    }
}
