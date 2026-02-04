package com.echoesapp.audiobooks.data.local

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a persistent device ID for anonymous user identification.
 * 
 * The device ID is:
 * - Generated once on first launch
 * - Persisted across app restarts
 * - Used for syncing progress across sessions
 * - Anonymous (no personal data)
 */
@Singleton
class DeviceIdManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get the device ID, generating one if it doesn't exist.
     */
    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: generateAndSaveDeviceId()
    
    private fun generateAndSaveDeviceId(): String {
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }
    
    /**
     * Check if this is a fresh install (no device ID yet).
     */
    fun isFreshInstall(): Boolean = !prefs.contains(KEY_DEVICE_ID)
    
    companion object {
        private const val PREFS_NAME = "echoes_device"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
