package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.example.data.AlertRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AudioAlertPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var stopJob: Job? = null

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

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
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
            e.printStackTrace()
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
                ex.printStackTrace()
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
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }
}
