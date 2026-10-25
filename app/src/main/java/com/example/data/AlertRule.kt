package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_rules")
data class AlertRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val level: Int, // 1 to 100
    val direction: String, // "CHARGING_UP" or "DRAINING_DOWN"
    val soundType: String, // "ALARM", "NOTIFICATION", "CUSTOM"
    val soundUri: String? = null,
    val volume: Float = 0.8f, // 0.0 to 1.0
    val enabled: Boolean = true,
    val lastTriggered: Boolean = false,
    val startMs: Int = 0, // audio segment start in milliseconds
    val endMs: Int = 0 // audio segment end in milliseconds (0 means until end of file)
) {
    val isTrimmed: Boolean
        get() = startMs > 0 || endMs > 0
    companion object {
        const val DIRECTION_CHARGING_UP = "CHARGING_UP"
        const val DIRECTION_DRAINING_DOWN = "DRAINING_DOWN"

        const val SOUND_ALARM = "ALARM"
        const val SOUND_NOTIFICATION = "NOTIFICATION"
        const val SOUND_VOICE_ALERT = "VOICE_ALERT"
        const val SOUND_VOICE_CHARGED = "VOICE_CHARGED"
        const val SOUND_CUSTOM = "CUSTOM"
    }

    val directionLabel: String
        get() = if (direction == DIRECTION_CHARGING_UP) "When charging up to" else "When draining down to"

    val soundLabel: String
        get() = when (soundType) {
            SOUND_VOICE_CHARGED -> "Full Power (Voice)"
            SOUND_VOICE_ALERT -> "Low Power (Voice)"
            SOUND_NOTIFICATION -> "Notification"
            SOUND_CUSTOM -> "Custom Audio"
            else -> "Default Alarm"
        }
}
