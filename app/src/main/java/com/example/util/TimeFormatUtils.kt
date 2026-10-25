package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TimeRemaining(
    val isPast: Boolean,
    val totalDays: Long,
    val totalHours: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val badgeText: String, // e.g. "14d 5h 20m" or "32 days ago"
    val daysOnlyText: String, // e.g. "14" or "32"
    val subtitleText: String, // e.g. "5h 20m left" or "passed"
    val progress: Float, // 0f to 1f
    val customDisplaySummary: String // Custom string according to displayUnit
)

object TimeFormatUtils {

    fun computeRemaining(
        targetTimeMillis: Long,
        createdTimeMillis: Long,
        displayUnit: String = "all",
        now: Long = System.currentTimeMillis()
    ): TimeRemaining {
        val isPast = targetTimeMillis <= now
        val diff = if (isPast) now - targetTimeMillis else targetTimeMillis - now

        val totalDays = TimeUnit.MILLISECONDS.toDays(diff)
        val totalHours = TimeUnit.MILLISECONDS.toHours(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

        val totalSpan = (targetTimeMillis - createdTimeMillis).coerceAtLeast(1000L)
        val elapsed = (now - createdTimeMillis).coerceAtLeast(0L)
        val progress = if (isPast) {
            1.0f
        } else {
            (elapsed.toFloat() / totalSpan.toFloat()).coerceIn(0f, 1f)
        }

        if (isPast) {
            val badge = if (totalDays <= 0) {
                if (hours <= 0) "${minutes}m ago" else "${hours}h ago"
            } else if (totalDays == 1L) {
                "1 day ago"
            } else {
                "$totalDays days ago"
            }

            val customText = when (displayUnit) {
                "days" -> if (totalDays == 1L) "1 day ago" else "$totalDays days ago"
                "hours" -> "$totalHours hrs ago"
                "weeks" -> {
                    val w = totalDays / 7
                    val d = totalDays % 7
                    if (w > 0) "${w}w ${d}d ago" else "$totalDays days ago"
                }
                else -> badge
            }

            return TimeRemaining(
                isPast = true,
                totalDays = totalDays,
                totalHours = totalHours,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                badgeText = badge,
                daysOnlyText = totalDays.toString(),
                subtitleText = "Event passed",
                progress = 1.0f,
                customDisplaySummary = customText
            )
        } else {
            val badge = when {
                totalDays > 0 -> "${totalDays}d ${hours}h ${minutes}m"
                hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s left"
            }
            val subtitle = when {
                totalDays > 0 -> "${hours}h ${minutes}m remaining"
                hours > 0 -> "${minutes}m remaining"
                else -> "Moments away"
            }

            val customText = when (displayUnit) {
                "days" -> if (totalDays == 1L) "1 day left" else "$totalDays days left"
                "hours" -> "$totalHours hours left"
                "weeks" -> {
                    val w = totalDays / 7
                    val d = totalDays % 7
                    if (w > 0) "${w}w ${d}d left" else "$totalDays days left"
                }
                else -> badge
            }

            return TimeRemaining(
                isPast = false,
                totalDays = totalDays,
                totalHours = totalHours,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                badgeText = badge,
                daysOnlyText = totalDays.toString(),
                subtitleText = subtitle,
                progress = progress,
                customDisplaySummary = customText
            )
        }
    }

    fun formatDate(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }

    fun formatDateShort(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }

    fun formatTimeOnly(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }
}
