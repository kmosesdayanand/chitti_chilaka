package com.example.widget

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.Countdown

object WidgetDataLoader {

    suspend fun loadCountdown(context: Context, appWidgetId: Int, stateCountdownId: Long?): Countdown? {
        val db = AppDatabase.getInstance(context)
        val dao = db.countdownDao()

        val specificPrefsId = if (appWidgetId > 0) {
            WidgetPreferences.getSpecificCountdownId(context, appWidgetId)
        } else {
            -1L
        }

        val targetId = when {
            stateCountdownId != null && stateCountdownId != -1L -> stateCountdownId
            specificPrefsId != -1L -> specificPrefsId
            else -> -1L
        }

        if (targetId != -1L) {
            val item = dao.getCountdownByIdSync(targetId)
            if (item != null) {
                return item
            }
        }

        // Fallback: next upcoming countdown, then the first countdown from all
        val now = System.currentTimeMillis()
        val upcoming = dao.getUpcomingCountdownsSync(now, 1)
        return upcoming.firstOrNull() ?: dao.getAllCountdownsSync().firstOrNull()
    }

    suspend fun loadUpcoming(context: Context, limit: Int): List<Countdown> {
        val db = AppDatabase.getInstance(context)
        val dao = db.countdownDao()
        val now = System.currentTimeMillis()

        var items = dao.getUpcomingCountdownsSync(now, limit)
        if (items.size < limit) {
            val all = dao.getAllCountdownsSync()
            val existingIds = items.map { it.id }.toSet()
            items = items + all.filter { it.id !in existingIds }.take(limit - items.size)
        }
        return items
    }
}
