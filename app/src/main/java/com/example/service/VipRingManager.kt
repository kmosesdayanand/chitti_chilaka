package com.example.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.VipContact
import com.example.data.VipPreferences
import com.example.data.VipSettings
import com.example.util.PhoneNumberMatcher
import com.example.util.VipPermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

/**
 * Core engine managing the VIP Contacts Full-Volume Ring lifecycle.
 *
 * Rules:
 * - PART 1: Boost only when needed (Silent, Vibrate, DND active, or Normal with volume < 40%).
 *   Normal mode with volume >= 40% is left completely untouched.
 *   Test VIP ring button always runs the boost regardless of state.
 * - PART 2: Cold start fix: restoreIfLeftActive only restores leftover state when call state
 *   is IDLE and saved state is older than 2 minutes (or upon device reboot recovery).
 * - PART 3: Save and restore race fix:
 *   - Original state saved first and awaited before boosting (never in background task).
 *   - Boost and restore synchronized under a single Mutex.
 *   - Existing active saved state is never overwritten.
 *   - In-memory cache for settings and audio state eliminates runBlocking on DataStore.
 *   - In-memory cache for enabled VIP contacts eliminates database latency during calls.
 */
object VipRingManager {

    private const val TAG = "VipRingManager"

    // Named constant for ring volume boost threshold ratio (40%)
    const val BOOST_VOLUME_THRESHOLD_RATIO = 0.40f

    private val audioMutex = Mutex()
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var cachedEnabledContacts: List<VipContact> = emptyList()

    @Volatile
    private var isInitialized: Boolean = false

    @Volatile
    private var customRingtone: Ringtone? = null

    @Volatile
    private var isTestingRing: Boolean = false

    private var testJob: Job? = null

    /**
     * Initializes background observation and in-memory caches.
     * Called during Application.onCreate.
     */
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        // Prime preferences instance and in-memory cache
        VipPreferences.getInstance(appContext)

        // Cache enabled VIP contacts continuously in memory
        managerScope.launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                db.vipContactDao().getEnabledContactsFlow().collect { contacts ->
                    cachedEnabledContacts = contacts
                    Log.d(TAG, "Cached enabled VIP contacts updated: count=${contacts.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting enabled VIP contacts flow", e)
            }
        }
    }

    /**
     * Evaluates if current audio state qualifies for VIP volume boost:
     * - If DND is active and overrideDnd is ON -> boost (calls are silenced)
     * - If ringer mode is Silent or Vibrate -> boost
     * - If ringer mode is Normal and ring volume is below 40% -> boost
     * - If ringer mode is Normal and ring volume is 40% or higher -> DO NOT boost
     */
    private fun shouldBoost(
        context: Context,
        audioManager: AudioManager,
        notificationManager: NotificationManager?,
        settings: VipSettings
    ): Boolean {
        // 1. Check Do Not Disturb
        if (settings.overrideDnd && notificationManager != null && VipPermissionChecker.isNotificationPolicyAccessGranted(context)) {
            val currentFilter = notificationManager.currentInterruptionFilter
            val isDndActive = currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                    currentFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN &&
                    currentFilter != -1
            if (isDndActive) {
                Log.d(TAG, "DND is active (filter=$currentFilter) and overrideDnd=true -> qualifies for VIP boost")
                return true
            }
        }

        // 2. Check Ringer Mode
        val ringerMode = audioManager.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            Log.d(TAG, "Ringer mode is ${if (ringerMode == AudioManager.RINGER_MODE_SILENT) "SILENT" else "VIBRATE"} -> qualifies for VIP boost")
            return true
        }

        // 3. Normal mode volume threshold check (40%)
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val volumeRatio = if (maxVol > 0) currentVol.toFloat() / maxVol.toFloat() else 0f

            if (volumeRatio < BOOST_VOLUME_THRESHOLD_RATIO) {
                Log.d(TAG, "Ringer mode is NORMAL and volume ratio ($currentVol/$maxVol = ${(volumeRatio * 100).toInt()}%) < ${(BOOST_VOLUME_THRESHOLD_RATIO * 100).toInt()}% -> qualifies for VIP boost")
                return true
            }

            Log.d(TAG, "Ringer mode is NORMAL and volume ratio ($currentVol/$maxVol = ${(volumeRatio * 100).toInt()}%) >= ${(BOOST_VOLUME_THRESHOLD_RATIO * 100).toInt()}% -> no boost needed")
            return false
        }

        return false
    }

    /**
     * Called when an incoming call is detected from CallScreeningService or PhoneStateReceiver.
     * Guaranteed never to throw, block the system call sequence, or block on the database.
     */
    fun onIncomingCall(context: Context, incomingNumber: String?) {
        if (incomingNumber.isNullOrBlank()) return

        managerScope.launch {
            try {
                val vipPrefs = VipPreferences.getInstance(context)
                val settings = vipPrefs.getCachedSettings()
                if (!settings.vipRingEnabled) {
                    Log.d(TAG, "VIP Ring is disabled in settings. Skipping.")
                    return@launch
                }

                // Check in-memory cache first so matching never waits on SQLite
                var enabledContacts = cachedEnabledContacts
                if (enabledContacts.isEmpty()) {
                    // Fallback on cold start if flow collection has not emitted yet
                    val db = AppDatabase.getInstance(context)
                    enabledContacts = db.vipContactDao().getEnabledContactsSync()
                    cachedEnabledContacts = enabledContacts
                }

                val matchedContact = enabledContacts.firstOrNull { contact ->
                    PhoneNumberMatcher.matches(incomingNumber, contact.phoneNumber)
                }

                if (matchedContact == null) {
                    Log.d(TAG, "Incoming number $incomingNumber does not match any enabled VIP contact.")
                    return@launch
                }

                Log.i(TAG, "VIP incoming call matched: ${matchedContact.contactName} ($incomingNumber)")
                boostVolumeForVipCallInternal(context, matchedContact.contactName, isTest = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating incoming call", e)
            }
        }
    }

    /**
     * Public entry point to trigger volume boost (asynchronous).
     */
    fun boostVolumeForVipCall(context: Context, contactName: String, isTest: Boolean = false) {
        managerScope.launch {
            boostVolumeForVipCallInternal(context, contactName, isTest)
        }
    }

    /**
     * Applies full-volume VIP boost behind audioMutex.
     * Guaranteed:
     * - Audio state saved and awaited before boosting (no background task race)
     * - Never overwrites an active saved state
     * - Only boosts if qualifying (or if isTest is true)
     */
    private suspend fun boostVolumeForVipCallInternal(
        context: Context,
        contactName: String,
        isTest: Boolean
    ) = audioMutex.withLock {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@withLock
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val vipPrefs = VipPreferences.getInstance(context)
            val settings = vipPrefs.getCachedSettings()

            // PART 1: If not a test, verify if the audio state actually qualifies for a boost
            if (!isTest && !shouldBoost(context, audioManager, notificationManager, settings)) {
                Log.i(TAG, "Audio state does not qualify for VIP boost. Keeping existing volume and settings untouched.")
                return@withLock
            }

            // PART 3: Check if an active saved state already exists; never overwrite it
            val priorState = vipPrefs.getCachedAudioState()
            if (!priorState.isActive) {
                val currentRingerMode = audioManager.ringerMode
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                val currentFilter = if (notificationManager != null && VipPermissionChecker.isNotificationPolicyAccessGranted(context)) {
                    notificationManager.currentInterruptionFilter
                } else {
                    -1
                }

                Log.d(TAG, "Saving initial audio state before boost: mode=$currentRingerMode, vol=$currentVolume, dnd=$currentFilter")
                // Await save completion directly - never in a detached background task
                vipPrefs.saveAudioState(
                    ringerMode = currentRingerMode,
                    streamVolume = currentVolume,
                    interruptionFilter = currentFilter,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                Log.d(TAG, "VIP ring already active; retaining original saved state without overwriting.")
            }

            // 1. Override Do Not Disturb if enabled and permission granted
            if (settings.overrideDnd && notificationManager != null && VipPermissionChecker.isNotificationPolicyAccessGranted(context)) {
                try {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    Log.d(TAG, "DND interruption filter set to INTERRUPTION_FILTER_ALL")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set DND interruption filter", e)
                }
            }

            // 2. Set Ringer Mode to NORMAL
            try {
                if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set ringer mode to NORMAL", e)
            }

            // 3. Boost STREAM_RING volume to target percentage (50%-100%)
            try {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                val targetVol = (maxVol * settings.ringVolumePercent).roundToInt().coerceIn(1, maxVol)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, targetVol, 0)
                Log.d(TAG, "STREAM_RING volume boosted to $targetVol / $maxVol (${(settings.ringVolumePercent * 100).toInt()}%)")
            } catch (e: Exception) {
                Log.w(TAG, "Could not set stream volume", e)
            }

            // 4. Trigger vibration if configured
            if (settings.alsoVibrate) {
                triggerVibration(context)
            }

            // 5. If system ring is silent, play fallback ringtone via RingtoneManager
            if (settings.ringOnSilent && audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                playFallbackRingtone(context)
            }

            // 6. Start short-lived foreground-safe service to monitor call state until OFFHOOK/IDLE (skip during test)
            if (!isTest) {
                VipRingService.start(context, contactName)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error applying VIP ring volume boost", e)
        }
    }

    /**
     * Restores previous audio state behind audioMutex.
     */
    fun restorePreviousState(context: Context) {
        managerScope.launch {
            restorePreviousStateInternal(context)
        }
    }

    private suspend fun restorePreviousStateInternal(context: Context) = audioMutex.withLock {
        try {
            Log.d(TAG, "Restoring previous audio state under mutex...")
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val vipPrefs = VipPreferences.getInstance(context)

            // Stop fallback ringtone if playing
            stopFallbackRingtone()

            val state = vipPrefs.getSavedAudioState()
            if (state.isActive) {
                // Restore STREAM_RING volume first
                if (audioManager != null && state.streamVolume != -1) {
                    try {
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                        val safeVol = state.streamVolume.coerceIn(0, maxVol)
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, safeVol, 0)
                        Log.d(TAG, "Restored STREAM_RING volume to $safeVol")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to restore stream volume", e)
                    }
                }

                // Restore ringer mode (Silent / Vibrate / Normal)
                if (audioManager != null && state.ringerMode != -1) {
                    try {
                        audioManager.ringerMode = state.ringerMode
                        Log.d(TAG, "Restored ringer mode to ${state.ringerMode}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to restore ringer mode", e)
                    }
                }

                // Restore Do Not Disturb interruption filter
                if (notificationManager != null && state.interruptionFilter != -1 &&
                    VipPermissionChecker.isNotificationPolicyAccessGranted(context)
                ) {
                    try {
                        notificationManager.setInterruptionFilter(state.interruptionFilter)
                        Log.d(TAG, "Restored DND interruption filter to ${state.interruptionFilter}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to restore DND interruption filter", e)
                    }
                }

                // Clear persisted state flags and wait for completion
                vipPrefs.clearSavedAudioState()
            }

            // Stop the monitoring service
            VipRingService.stop(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error restoring previous audio state", e)
        }
    }

    /**
     * PART 2: Cold start fix.
     * Only restores leftover state when:
     * - The phone call state is IDLE (never while ringing or offhook), and
     * - The saved state is older than 2 minutes (or during device boot recovery).
     */
    fun restoreIfLeftActive(context: Context, isBootRecovery: Boolean = false) {
        managerScope.launch {
            try {
                val vipPrefs = VipPreferences.getInstance(context)
                val state = vipPrefs.getSavedAudioState()
                if (!state.isActive) return@launch

                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val callState = try {
                    telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE
                } catch (_: Exception) {
                    TelephonyManager.CALL_STATE_IDLE
                }

                // Never restore if a call is actively ringing or answered
                if (callState != TelephonyManager.CALL_STATE_IDLE) {
                    Log.d(TAG, "Call state is not IDLE ($callState). Skipping leftover restore.")
                    return@launch
                }

                val ageMs = System.currentTimeMillis() - state.savedTimestampMillis
                val isOlderThanTwoMinutes = state.savedTimestampMillis > 0L && ageMs > (2 * 60 * 1000L)

                if (isBootRecovery || isOlderThanTwoMinutes) {
                    Log.i(TAG, "Detected leftover active VIP ring state (boot=$isBootRecovery, age=${ageMs / 1000}s). Restoring...")
                    restorePreviousStateInternal(context)
                } else {
                    Log.d(TAG, "Active state was saved recently (${ageMs / 1000}s <= 120s). Skipping restore to avoid undoing an active VIP call.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking leftover active state", e)
            }
        }
    }

    private fun playFallbackRingtone(context: Context) {
        try {
            stopFallbackRingtone()
            val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return

            val ringtone = RingtoneManager.getRingtone(context, ringUri) ?: return
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_RING)
                .build()

            ringtone.play()
            customRingtone = ringtone
            Log.d(TAG, "Fallback ringtone playing on ring stream.")
        } catch (e: Exception) {
            Log.w(TAG, "Could not play fallback ringtone", e)
        }
    }

    private fun stopFallbackRingtone() {
        try {
            customRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping custom ringtone", e)
        } finally {
            customRingtone = null
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 400, 300, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 300, 400), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 400, 300, 400), -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not trigger vibration", e)
        }
    }

    /**
     * Executes a 5-second test ring at the configured VIP volume.
     * Always runs the boost regardless of current audio state.
     */
    fun startTestRing(
        context: Context,
        onTick: (secondsLeft: Int) -> Unit,
        onFinished: () -> Unit
    ) {
        if (isTestingRing) {
            stopTestRing(context, onFinished)
            return
        }

        isTestingRing = true
        testJob?.cancel()

        testJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Always force boost for the test ring (isTest = true)
                boostVolumeForVipCallInternal(context, "VIP Ring Test", isTest = true)
                playFallbackRingtone(context)

                for (seconds in 5 downTo 1) {
                    onTick(seconds)
                    delay(1000)
                }
            } finally {
                stopTestRing(context, onFinished)
            }
        }
    }

    fun stopTestRing(context: Context, onFinished: (() -> Unit)? = null) {
        testJob?.cancel()
        testJob = null
        isTestingRing = false
        restorePreviousState(context)
        onFinished?.invoke()
    }

    fun isTesting(): Boolean = isTestingRing
}
