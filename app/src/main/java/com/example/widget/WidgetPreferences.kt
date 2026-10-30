package com.example.widget

import android.content.Context
import android.content.SharedPreferences

object WidgetPreferences {
    private const val PREFS_NAME = "phone_manager_widget_prefs"
    private const val KEY_PREFIX_COUNTDOWN_ID = "widget_countdown_id_"
    private const val KEY_LAST_CONFIGURED_ID = "last_configured_countdown_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setCountdownId(context: Context, appWidgetId: Int, countdownId: Long) {
        val editor = getPrefs(context).edit()
        if (appWidgetId > 0) {
            editor.putLong(KEY_PREFIX_COUNTDOWN_ID + appWidgetId, countdownId)
        }
        editor.putLong(KEY_LAST_CONFIGURED_ID, countdownId)
        editor.commit()
    }

    fun setLastConfiguredCountdownId(context: Context, countdownId: Long) {
        getPrefs(context).edit()
            .putLong(KEY_LAST_CONFIGURED_ID, countdownId)
            .commit()
    }

    fun setPrimaryCountdownForAllWidgets(context: Context, countdownId: Long) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        editor.putLong(KEY_LAST_CONFIGURED_ID, countdownId)
        for (key in prefs.all.keys) {
            if (key.startsWith(KEY_PREFIX_COUNTDOWN_ID)) {
                editor.putLong(key, countdownId)
            }
        }
        editor.commit()
    }

    fun getSpecificCountdownId(context: Context, appWidgetId: Int): Long {
        if (appWidgetId > 0) {
            return getPrefs(context).getLong(KEY_PREFIX_COUNTDOWN_ID + appWidgetId, -1L)
        }
        return -1L
    }

    fun getCountdownId(context: Context, appWidgetId: Int): Long {
        if (appWidgetId > 0) {
            val specific = getSpecificCountdownId(context, appWidgetId)
            if (specific != -1L) {
                return specific
            }
        }
        return getLastConfiguredCountdownId(context)
    }

    fun getLastConfiguredCountdownId(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_CONFIGURED_ID, -1L)
    }

    fun removeWidgetConfig(context: Context, appWidgetId: Int) {
        if (appWidgetId > 0) {
            getPrefs(context).edit()
                .remove(KEY_PREFIX_COUNTDOWN_ID + appWidgetId)
                .commit()
        }
    }

    const val KEY_WIDGET_TAP_OPENS_ITEM = "widget_tap_opens_item"
    const val KEY_THEME_MODE = "app_theme_mode" // "SYSTEM", "DARK", "LIGHT"
    const val KEY_DYNAMIC_COLOR = "app_dynamic_color"

    fun isWidgetTapOpensItemEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WIDGET_TAP_OPENS_ITEM, true)
    }

    fun setWidgetTapOpensItemEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_WIDGET_TAP_OPENS_ITEM, enabled)
            .commit()
    }

    fun getThemeMode(context: Context): String {
        return getPrefs(context).getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit()
            .putString(KEY_THEME_MODE, mode)
            .commit()
    }

    fun getDynamicColor(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DYNAMIC_COLOR, true)
    }

    fun setDynamicColor(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_DYNAMIC_COLOR, enabled)
            .commit()
    }
}
