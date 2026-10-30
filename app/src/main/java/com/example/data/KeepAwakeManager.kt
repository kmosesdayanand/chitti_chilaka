package com.example.data

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log
import com.example.service.KeepAwakeService
import com.example.service.KeepAwakeTileService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class KeepAwakeDuration(
    val minutes: Int,
    val durationMs: Long,
    val label: String,
    val subtitle: String
) {
    OFF(0, 0L, "Off", "Off"),
    FIVE_MINUTES(5, 5 * 60 * 1000L, "5 minutes", "5 min"),
    TEN_MINUTES(10, 10 * 60 * 1000L, "10 minutes", "10 min"),
    THIRTY_MINUTES(30, 30 * 60 * 1000L, "30 minutes", "30 min"),
    ONE_HOUR(60, 60 * 60 * 1000L, "1 hour", "1 hour"),
    INFINITE(-1, 24 * 60 * 60 * 1000L, "Infinite", "Infinite");

    fun next(): KeepAwakeDuration = when (this) {
        OFF -> FIVE_MINUTES
        FIVE_MINUTES -> TEN_MINUTES
        TEN_MINUTES -> THIRTY_MINUTES
        THIRTY_MINUTES -> ONE_HOUR
        ONE_HOUR -> INFINITE
        INFINITE -> OFF
    }
}

data class KeepAwakeState(
    val isRunning: Boolean = false,
    val duration: KeepAwakeDuration = KeepAwakeDuration.OFF,
    val endTimeMillis: Long = 0L
)

object KeepAwakeManager {
    private const val TAG = "KeepAwakeManager"

    private val _stateFlow = MutableStateFlow(KeepAwakeState())
    val stateFlow: StateFlow<KeepAwakeState> = _stateFlow.asStateFlow()

    val isRunning: Boolean
        get() = _stateFlow.value.isRunning

    val currentDuration: KeepAwakeDuration
        get() = _stateFlow.value.duration

    val endTimeMillis: Long
        get() = _stateFlow.value.endTimeMillis

    fun start(context: Context, duration: KeepAwakeDuration) {
        if (duration == KeepAwakeDuration.OFF) {
            stop(context)
            return
        }
        val end = if (duration == KeepAwakeDuration.INFINITE) {
            System.currentTimeMillis() + duration.durationMs
        } else {
            System.currentTimeMillis() + duration.durationMs
        }
        _stateFlow.value = KeepAwakeState(
            isRunning = true,
            duration = duration,
            endTimeMillis = end
        )
        KeepAwakeService.start(context, duration)
        requestTileUpdate(context)
    }

    fun stop(context: Context) {
        _stateFlow.value = KeepAwakeState(
            isRunning = false,
            duration = KeepAwakeDuration.OFF,
            endTimeMillis = 0L
        )
        KeepAwakeService.stop(context)
        requestTileUpdate(context)
    }

    fun onServiceStarted(duration: KeepAwakeDuration, endTime: Long) {
        _stateFlow.value = KeepAwakeState(
            isRunning = true,
            duration = duration,
            endTimeMillis = endTime
        )
    }

    fun onServiceStopped(context: Context) {
        _stateFlow.value = KeepAwakeState(
            isRunning = false,
            duration = KeepAwakeDuration.OFF,
            endTimeMillis = 0L
        )
        requestTileUpdate(context)
    }

    fun getRemainingSubtitle(): String {
        val state = _stateFlow.value
        if (!state.isRunning || state.duration == KeepAwakeDuration.OFF) return "Off"
        if (state.duration == KeepAwakeDuration.INFINITE) return "Infinite"

        val remainingMs = (state.endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val minutes = (remainingMs + 59_999L) / 60_000L
        return when {
            minutes > 60 -> "${minutes / 60}h ${minutes % 60}m"
            minutes > 1 -> "${minutes} min"
            remainingMs > 0 -> "${(remainingMs / 1000L).coerceAtLeast(1)}s left"
            else -> "Off"
        }
    }

    fun getRemainingFormatted(): String {
        val state = _stateFlow.value
        if (!state.isRunning || state.duration == KeepAwakeDuration.OFF) return "Off"
        if (state.duration == KeepAwakeDuration.INFINITE) return "Screen will stay awake indefinitely"

        val remainingMs = (state.endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val totalSecs = remainingMs / 1000L
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d remaining", hours, mins, secs)
            mins > 0 -> String.format("%d:%02d remaining", mins, secs)
            else -> String.format("%d seconds remaining", secs)
        }
    }

    fun requestTileUpdate(context: Context) {
        try {
            val component = ComponentName(context, KeepAwakeTileService::class.java)
            TileService.requestListeningState(context, component)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request tile listening state", e)
        }
    }
}
