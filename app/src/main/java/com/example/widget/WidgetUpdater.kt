package com.example.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {

    suspend fun setPrimaryCountdownAndRefreshAll(context: Context, countdownId: Long) = withContext(Dispatchers.IO) {
        // 1. Update SharedPreferences so both specific widget bindings and fallback match the selected countdown
        WidgetPreferences.setPrimaryCountdownForAllWidgets(context, countdownId)

        // 2. Update Glance DataStore for all active Small and Progress widgets
        val manager = GlanceAppWidgetManager(context)
        runCatching {
            val smallIds = manager.getGlanceIds(SmallCountdownWidget::class.java)
            for (id in smallIds) {
                runCatching {
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[WIDGET_COUNTDOWN_PREF_KEY] = countdownId
                    }
                    SmallCountdownWidget().update(context, id)
                }
            }
        }
        runCatching {
            val progressIds = manager.getGlanceIds(ProgressCountdownWidget::class.java)
            for (id in progressIds) {
                runCatching {
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[WIDGET_COUNTDOWN_PREF_KEY] = countdownId
                    }
                    ProgressCountdownWidget().update(context, id)
                }
            }
        }

        // 3. Trigger global update
        updateAllWidgets(context)
    }

    suspend fun updateAllWidgets(context: Context) = withContext(Dispatchers.IO) {
        val manager = GlanceAppWidgetManager(context)
        val widgets = listOf(
            SmallCountdownWidget(),
            MediumCountdownWidget(),
            ProgressCountdownWidget()
        )

        for (widget in widgets) {
            val ids = runCatching {
                manager.getGlanceIds(widget.javaClass)
            }.getOrElse { error ->
                error.printStackTrace()
                emptyList()
            }

            for (id in ids) {
                runCatching {
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[WIDGET_REFRESH_TICK_KEY] = System.currentTimeMillis()
                    }
                    widget.update(context, id)
                }.onFailure { error ->
                    error.printStackTrace()
                }
            }
        }
    }
}
