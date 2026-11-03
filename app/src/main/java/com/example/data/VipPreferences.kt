package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.vipPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "vip_ring_preferences")

data class VipSettings(
    val vipRingEnabled: Boolean = true,
    val ringVolumePercent: Float = 1.0f, // 0.50f to 1.0f
    val overrideDnd: Boolean = true,
    val alsoVibrate: Boolean = true,
    val ringOnSilent: Boolean = true
)

data class SavedAudioState(
    val isActive: Boolean = false,
    val ringerMode: Int = -1,
    val streamVolume: Int = -1,
    val interruptionFilter: Int = -1,
    val savedTimestampMillis: Long = 0L
)

class VipPreferences(private val context: Context) {

    companion object {
        val KEY_VIP_RING_ENABLED = booleanPreferencesKey("vip_ring_enabled")
        val KEY_RING_VOLUME_PERCENT = floatPreferencesKey("vip_ring_volume_percent")
        val KEY_OVERRIDE_DND = booleanPreferencesKey("vip_override_dnd")
        val KEY_ALSO_VIBRATE = booleanPreferencesKey("vip_also_vibrate")
        val KEY_RING_ON_SILENT = booleanPreferencesKey("vip_ring_on_silent")

        // State tracking for call restoration across crashes/reboots
        val KEY_SAVED_STATE_ACTIVE = booleanPreferencesKey("vip_saved_state_active")
        val KEY_SAVED_RINGER_MODE = intPreferencesKey("vip_saved_ringer_mode")
        val KEY_SAVED_STREAM_VOLUME = intPreferencesKey("vip_saved_stream_volume")
        val KEY_SAVED_INTERRUPTION_FILTER = intPreferencesKey("vip_saved_interruption_filter")
        val KEY_SAVED_TIMESTAMP = longPreferencesKey("vip_saved_timestamp")

        @Volatile
        private var INSTANCE: VipPreferences? = null

        fun getInstance(context: Context): VipPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VipPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var cachedSettings: VipSettings = VipSettings()

    @Volatile
    private var cachedAudioState: SavedAudioState = SavedAudioState()

    init {
        prefScope.launch {
            vipSettingsFlow.collect { settings ->
                cachedSettings = settings
            }
        }
        prefScope.launch {
            savedAudioStateFlow.collect { audioState ->
                cachedAudioState = audioState
            }
        }
    }

    val vipSettingsFlow: Flow<VipSettings> = context.vipPreferencesDataStore.data.map { prefs ->
        VipSettings(
            vipRingEnabled = prefs[KEY_VIP_RING_ENABLED] ?: true,
            ringVolumePercent = prefs[KEY_RING_VOLUME_PERCENT] ?: 1.0f,
            overrideDnd = prefs[KEY_OVERRIDE_DND] ?: true,
            alsoVibrate = prefs[KEY_ALSO_VIBRATE] ?: true,
            ringOnSilent = prefs[KEY_RING_ON_SILENT] ?: true
        )
    }

    val savedAudioStateFlow: Flow<SavedAudioState> = context.vipPreferencesDataStore.data.map { prefs ->
        SavedAudioState(
            isActive = prefs[KEY_SAVED_STATE_ACTIVE] ?: false,
            ringerMode = prefs[KEY_SAVED_RINGER_MODE] ?: -1,
            streamVolume = prefs[KEY_SAVED_STREAM_VOLUME] ?: -1,
            interruptionFilter = prefs[KEY_SAVED_INTERRUPTION_FILTER] ?: -1,
            savedTimestampMillis = prefs[KEY_SAVED_TIMESTAMP] ?: 0L
        )
    }

    suspend fun getVipSettings(): VipSettings {
        return vipSettingsFlow.first()
    }

    // In-memory instant access: removes runBlocking completely from call paths
    fun getVipSettingsSync(): VipSettings = cachedSettings
    fun getCachedSettings(): VipSettings = cachedSettings

    suspend fun setVipRingEnabled(enabled: Boolean) {
        context.vipPreferencesDataStore.edit { prefs ->
            prefs[KEY_VIP_RING_ENABLED] = enabled
        }
        cachedSettings = cachedSettings.copy(vipRingEnabled = enabled)
    }

    suspend fun setRingVolumePercent(volume: Float) {
        val clamped = volume.coerceIn(0.5f, 1.0f)
        context.vipPreferencesDataStore.edit { prefs ->
            prefs[KEY_RING_VOLUME_PERCENT] = clamped
        }
        cachedSettings = cachedSettings.copy(ringVolumePercent = clamped)
    }

    suspend fun setOverrideDnd(enabled: Boolean) {
        context.vipPreferencesDataStore.edit { prefs ->
            prefs[KEY_OVERRIDE_DND] = enabled
        }
        cachedSettings = cachedSettings.copy(overrideDnd = enabled)
    }

    suspend fun setAlsoVibrate(enabled: Boolean) {
        context.vipPreferencesDataStore.edit { prefs ->
            prefs[KEY_ALSO_VIBRATE] = enabled
        }
        cachedSettings = cachedSettings.copy(alsoVibrate = enabled)
    }

    suspend fun setRingOnSilent(enabled: Boolean) {
        context.vipPreferencesDataStore.edit { prefs ->
            prefs[KEY_RING_ON_SILENT] = enabled
        }
        cachedSettings = cachedSettings.copy(ringOnSilent = enabled)
    }

    /**
     * Persist previous audio state when a VIP call triggers volume boost.
     * Only called when no VIP ring state is currently active.
     */
    suspend fun saveAudioState(
        ringerMode: Int,
        streamVolume: Int,
        interruptionFilter: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        context.vipPreferencesDataStore.edit { prefs ->
            prefs[KEY_SAVED_STATE_ACTIVE] = true
            prefs[KEY_SAVED_RINGER_MODE] = ringerMode
            prefs[KEY_SAVED_STREAM_VOLUME] = streamVolume
            prefs[KEY_SAVED_INTERRUPTION_FILTER] = interruptionFilter
            prefs[KEY_SAVED_TIMESTAMP] = timestamp
        }
        cachedAudioState = SavedAudioState(
            isActive = true,
            ringerMode = ringerMode,
            streamVolume = streamVolume,
            interruptionFilter = interruptionFilter,
            savedTimestampMillis = timestamp
        )
    }

    suspend fun getSavedAudioState(): SavedAudioState {
        return savedAudioStateFlow.first()
    }

    // In-memory instant access: removes runBlocking completely from call paths
    fun getSavedAudioStateSync(): SavedAudioState = cachedAudioState
    fun getCachedAudioState(): SavedAudioState = cachedAudioState

    /**
     * Clear saved audio state when ringer mode and volume have been restored.
     */
    suspend fun clearSavedAudioState() {
        context.vipPreferencesDataStore.edit { prefs ->
            prefs[KEY_SAVED_STATE_ACTIVE] = false
            prefs[KEY_SAVED_RINGER_MODE] = -1
            prefs[KEY_SAVED_STREAM_VOLUME] = -1
            prefs[KEY_SAVED_INTERRUPTION_FILTER] = -1
            prefs[KEY_SAVED_TIMESTAMP] = 0L
        }
        cachedAudioState = SavedAudioState()
    }
}
