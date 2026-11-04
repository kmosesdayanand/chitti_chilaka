package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.example.data.AlertRule
import com.example.widget.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AudioAlertPlayer {

    private const val TAG = "AudioAlertPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private var stopJob: Job? = null
    private var focusRequest: AudioFocusRequest? = null
    private var previousAlarmVolume: Int? = null
    private var applicationContext: Context? = null

    @Synchronized
    fun playSound(
        context: Context,
        soundType: String,
        soundUriString: String?,
        volume: Float,
        startMs: Int = 0,
        endMs: Int = 0,
        maxDurationMs: Long = 30000L,
        onStopCallback: (() -> Unit)? = null
    ) {
        stop()
        val appContext = context.applicationContext
        applicationContext = appContext

        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // Part 11: Request audio focus with USAGE_ALARM and GAIN_TRANSIENT_MAY_DUCK
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .build()
                focusRequest = req
                audioManager?.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to request audio focus", e)
        }

        // Part 12: Temporarily set alarm stream to max volume if full volume option is enabled
        try {
            if (WidgetPreferences.isPlayAlertsFullVolume(appContext)) {
                audioManager?.let { am ->
                    val currentAlarmVol = am.getStreamVolume(AudioManager.STREAM_ALARM)
                    val maxAlarmVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    previousAlarmVolume = currentAlarmVol
                    am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)
                }
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to boost alarm stream volume", e)
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                setOnErrorListener { _, what, extra ->
                    AppLog.e(TAG, "MediaPlayer error what=$what, extra=$extra")
                    stop()
                    onStopCallback?.invoke()
                    true
                }

                if (soundType == AlertRule.SOUND_VOICE_CHARGED) {
                    val afd = context.resources.openRawResourceFd(com.example.R.raw.battery_charged_alert)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                } else if (soundType == AlertRule.SOUND_VOICE_ALERT) {
                    val afd = context.resources.openRawResourceFd(com.example.R.raw.battery_low_alert)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                } else {
                    val uri = resolveUri(context, soundType, soundUriString)
                    setDataSource(context, uri)
                }
                isLooping = false // PLAY ONLY ONCE
                val clampedVol = volume.coerceIn(0f, 1f)
                setVolume(clampedVol, clampedVol)
                prepare()

                if (startMs > 0 && startMs < duration) {
                    seekTo(startMs)
                }

                setOnCompletionListener {
                    stop()
                    onStopCallback?.invoke()
                }

                start()
            }
            mediaPlayer = player

            // If an end timestamp is set, schedule stop; otherwise stop on playback completion or max fallback timeout
            val durationLimit = if (endMs > startMs) {
                (endMs - startMs).toLong()
            } else {
                maxDurationMs
            }

            stopJob = CoroutineScope(Dispatchers.Main).launch {
                delay(durationLimit)
                stop()
                onStopCallback?.invoke()
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to play audio alert, falling back to default alarm", e)
            // Fallback to default alarm sound if custom audio fails
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val fallbackPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setOnErrorListener { _, what, extra ->
                        AppLog.e(TAG, "Fallback MediaPlayer error what=$what, extra=$extra")
                        stop()
                        onStopCallback?.invoke()
                        true
                    }
                    setDataSource(context, fallbackUri)
                    isLooping = false // PLAY ONLY ONCE
                    val clampedVol = volume.coerceIn(0f, 1f)
                    setVolume(clampedVol, clampedVol)
                    prepare()
                    if (startMs > 0 && startMs < duration) {
                        seekTo(startMs)
                    }
                    setOnCompletionListener {
                        stop()
                        onStopCallback?.invoke()
                    }
                    start()
                }
                mediaPlayer = fallbackPlayer
                val durationLimit = if (endMs > startMs) (endMs - startMs).toLong() else maxDurationMs
                stopJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(durationLimit)
                    stop()
                    onStopCallback?.invoke()
                }
            } catch (ex: Exception) {
                AppLog.e(TAG, "Fallback audio alert also failed", ex)
                stop()
                onStopCallback?.invoke()
            }
        }
    }

    @Synchronized
    fun playSound(
        context: Context,
        soundType: String,
        soundUriString: String?,
        volume: Float,
        durationMs: Long,
        onStopCallback: (() -> Unit)? = null
    ) {
        playSound(
            context = context,
            soundType = soundType,
            soundUriString = soundUriString,
            volume = volume,
            startMs = 0,
            endMs = 0,
            maxDurationMs = durationMs,
            onStopCallback = onStopCallback
        )
    }

    fun getAudioDurationMs(context: Context, soundType: String, soundUriString: String?): Int {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            when (soundType) {
                AlertRule.SOUND_VOICE_CHARGED -> {
                    val afd = context.resources.openRawResourceFd(com.example.R.raw.battery_charged_alert)
                    retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                AlertRule.SOUND_VOICE_ALERT -> {
                    val afd = context.resources.openRawResourceFd(com.example.R.raw.battery_low_alert)
                    retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                AlertRule.SOUND_CUSTOM -> {
                    if (!soundUriString.isNullOrBlank()) {
                        retriever.setDataSource(context, Uri.parse(soundUriString))
                    } else {
                        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        retriever.setDataSource(context, defaultUri)
                    }
                }
                else -> {
                    val uri = RingtoneManager.getDefaultUri(
                        if (soundType == AlertRule.SOUND_NOTIFICATION) RingtoneManager.TYPE_NOTIFICATION else RingtoneManager.TYPE_ALARM
                    )
                    retriever.setDataSource(context, uri)
                }
            }
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toIntOrNull() ?: 5000
        } catch (e: Exception) {
            5000
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun resolveUri(context: Context, soundType: String, soundUriString: String?): Uri {
        return when (soundType) {
            AlertRule.SOUND_VOICE_CHARGED -> {
                Uri.parse("android.resource://${context.packageName}/${com.example.R.raw.battery_charged_alert}")
            }
            AlertRule.SOUND_VOICE_ALERT -> {
                Uri.parse("android.resource://${context.packageName}/${com.example.R.raw.battery_low_alert}")
            }
            AlertRule.SOUND_NOTIFICATION -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            AlertRule.SOUND_CUSTOM -> {
                if (!soundUriString.isNullOrBlank()) {
                    try {
                        val parsed = Uri.parse(soundUriString)
                        context.contentResolver.openInputStream(parsed)?.close()
                        parsed
                    } catch (e: Exception) {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    }
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
            }
            else -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        }
    }

    @Synchronized
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    @Synchronized
    fun stop() {
        stopJob?.cancel()
        stopJob = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        // Restore original alarm stream volume if it was boosted
        try {
            previousAlarmVolume?.let { origVol ->
                val audioManager = applicationContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, origVol, 0)
                previousAlarmVolume = null
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Error restoring alarm stream volume", e)
        }

        // Abandon audio focus
        try {
            val audioManager = applicationContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
            focusRequest = null
        } catch (e: Exception) {
            AppLog.e(TAG, "Error abandoning audio focus", e)
        }
    }
}
