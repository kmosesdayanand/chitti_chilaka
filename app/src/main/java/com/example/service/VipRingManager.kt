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
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.VipPreferences
import com.example.util.PhoneNumberMatcher
import com.example.util.VipPermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Core engine managing the VIP Contacts Full-Volume Ring lifecycle.
 *
 * Handles:
 * - Matching incoming numbers against enabled VIP contacts
 * - Capturing prior audio state (ringer mode, stream volume, DND interruption filter)
 * - Boosting ringer volume to target level and overriding Do Not Disturb
 * - Fallback ringtone playback when system ring is silent
 * - Restoring prior state upon call answer (OFFHOOK), hangup (IDLE), app launch, or reboot
 */
object VipRingManager {

    private const val TAG = "VipRingManager"

    @Volatile
    private var customRingtone: Ringtone? = null

    @Volatile
    private var isTestingRing: Boolean = false

    private var testJob: Job? = null

    /**
     * Called when an incoming call is detected from CallScreeningService or PhoneStateReceiver.
     * Guaranteed never to throw or block the system incoming call sequence.
     */
    fun onIncomingCall(context: Context, incomingNumber: String?) {
        if (incomingNumber.isNullOrBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val vipPrefs = VipPreferences.getInstance(context)
                val settings = vipPrefs.getVipSettings()
                if (!settings.vipRingEnabled) {
                    Log.d(TAG, "VIP Ring is disabled in settings. Skipping.")
                    return@launch
                }

                val db = AppDatabase.getInstance(context)
                val enabledContacts = db.vipContactDao().getEnabledContactsSync()
                val matchedContact = enabledContacts.firstOrNull { contact ->
                    PhoneNumberMatcher.matches(incomingNumber, contact.phoneNumber)
                }

                if (matchedContact == null) {
                    Log.d(TAG, "Incoming number $incomingNumber does not match any enabled VIP contact.")
                    return@launch
                }

                Log.i(TAG, "VIP incoming call matched: ${matchedContact.contactName} ($incomingNumber)")
                boostVolumeForVipCall(context, matchedContact.contactName)
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating incoming call", e)
            }
        }
    }

    /**
     * Applies full-volume VIP boost, saving prior state only if not already active.
     */
    fun boostVolumeForVipCall(context: Context, contactName: String) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val vipPrefs = VipPreferences.getInstance(context)

            // Read current preferences and saved state synchronously
            val settings = vipPrefs.getVipSettingsSync()
            val priorState = vipPrefs.getSavedAudioStateSync()

            // Edge Case: Two VIP calls in a row must not overwrite saved "previous state"
            // with already-boosted values. Save only if no VIP ring is currently active.
            if (!priorState.isActive) {
                val currentRingerMode = audioManager.ringerMode
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                val currentFilter = if (notificationManager != null && VipPermissionChecker.isNotificationPolicyAccessGranted(context)) {
                    notificationManager.currentInterruptionFilter
                } else {
                    -1
                }

                Log.d(TAG, "Saving initial audio state: mode=$currentRingerMode, vol=$currentVolume, dnd=$currentFilter")
                CoroutineScope(Dispatchers.IO).launch {
                    vipPrefs.saveAudioState(
                        ringerMode = currentRingerMode,
                        streamVolume = currentVolume,
                        interruptionFilter = currentFilter
                    )
                }
            } else {
                Log.d(TAG, "VIP ring already active; retaining original saved state.")
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

            // 5. If the system ring is silent, play default ringtone via RingtoneManager
            if (settings.ringOnSilent && audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                playFallbackRingtone(context)
            }

            // 6. Start short-lived foreground-safe service to monitor call state until OFFHOOK/IDLE
            VipRingService.start(context, contactName)

        } catch (e: Exception) {
            Log.e(TAG, "Error applying VIP ring volume boost", e)
        }
    }

    /**
     * Restores the audio state saved before the VIP call started.
     * Wrapped in try/catch to guarantee safe recovery.
     */
    fun restorePreviousState(context: Context) {
        try {
            Log.d(TAG, "Restoring previous audio state...")
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val vipPrefs = VipPreferences.getInstance(context)

            // Stop fallback ringtone if playing
            stopFallbackRingtone()

            val state = vipPrefs.getSavedAudioStateSync()
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

                // Clear persisted state flags
                CoroutineScope(Dispatchers.IO).launch {
                    vipPrefs.clearSavedAudioState()
                }
            }

            // Stop the monitoring service
            VipRingService.stop(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error restoring previous audio state", e)
        }
    }

    /**
     * Recovers state if the phone restarted or app process was recreated during an active ring.
     */
    fun restoreIfLeftActive(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val vipPrefs = VipPreferences.getInstance(context)
                val state = vipPrefs.getSavedAudioState()
                if (state.isActive) {
                    Log.i(TAG, "Detected leftover active VIP ring state from prior session. Restoring now...")
                    restorePreviousState(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking leftover active state", e)
            }
        }
    }

    /**
     * Plays the system default ringtone through RingtoneManager on STREAM_RING.
     */
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
     * Executes a 5-second test ring at the configured VIP volume,
     * ticking remaining seconds and then restoring previous audio state.
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
                // Apply volume boost with test tag
                boostVolumeForVipCall(context, "VIP Ring Test")
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
