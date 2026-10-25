package com.example.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Countdown
import com.example.util.TimeFormatUtils

val WIDGET_COUNTDOWN_PREF_KEY = longPreferencesKey("widget_countdown_id")
private val COUNTDOWN_ID_KEY = ActionParameters.Key<Long>("EXTRA_COUNTDOWN_ID")

class SmallCountdownWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)

        // 1. Resolve appWidgetId
        val appWidgetId: Int = when (id) {
            is AppWidgetId -> id.appWidgetId
            else -> {
                runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrNull()
                    ?: Regex("""appWidgetId=(\d+)""").find(id.toString())?.groupValues?.get(1)?.toIntOrNull()
                    ?: -1
            }
        }

        // 1. The value in the Glance state (WIDGET_COUNTDOWN_PREF_KEY) for this widget, if it is not null and not -1.
        val stateCountdownId: Long? = runCatching {
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
            prefs[WIDGET_COUNTDOWN_PREF_KEY]
        }.getOrNull()

        // 2. The value from WidgetPreferences.getSpecificCountdownId(context, appWidgetId), if it is not -1.
        val specificPrefsId = if (appWidgetId > 0) {
            WidgetPreferences.getSpecificCountdownId(context, appWidgetId)
        } else -1L

        // Choose the countdown in this order only:
        // 1. Glance state (WIDGET_COUNTDOWN_PREF_KEY) if not null and not -1
        // 2. Specific countdown id from SharedPreferences if not -1
        // 3. Otherwise fallback countdown
        val targetCountdownId = when {
            stateCountdownId != null && stateCountdownId != -1L -> stateCountdownId
            specificPrefsId != -1L -> specificPrefsId
            else -> -1L
        }

        val countdown: Countdown? = if (targetCountdownId != -1L) {
            db.countdownDao().getCountdownByIdSync(targetCountdownId)
                ?: getFallbackCountdown(db)
        } else {
            getFallbackCountdown(db)
        }

        provideContent {
            SmallWidgetContent(countdown = countdown)
        }
    }

    private suspend fun getFallbackCountdown(db: AppDatabase): Countdown? {
        val now = System.currentTimeMillis()
        val upcoming = db.countdownDao().getUpcomingCountdownsSync(now, 1)
        return upcoming.firstOrNull() ?: db.countdownDao().getAllCountdownsSync().firstOrNull()
    }
}

@androidx.compose.runtime.Composable
private fun SmallWidgetContent(countdown: Countdown?) {
    val clickAction = if (countdown != null) {
        actionStartActivity<MainActivity>(actionParametersOf(COUNTDOWN_ID_KEY to countdown.id))
    } else {
        actionStartActivity<MainActivity>()
    }

    val backgroundColor = Color(0xFF0F172A)
    val cardBackground = Color(0xFF1E293B)

    if (countdown == null) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(backgroundColor))
                .cornerRadius(18.dp)
                .padding(14.dp)
                .clickable(clickAction),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏳",
                    style = TextStyle(fontSize = 32.sp)
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "No countdowns",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE2E8F0)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Tap to add one",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF94A3B8)),
                        fontSize = 11.sp
                    )
                )
            }
        }
        return
    }

    val timeRemaining = TimeFormatUtils.computeRemaining(
        targetTimeMillis = countdown.targetTimeMillis,
        createdTimeMillis = countdown.createdTimeMillis,
        displayUnit = countdown.displayUnit
    )

    val accentColor = try {
        Color(android.graphics.Color.parseColor(countdown.colorHex))
    } catch (e: Exception) {
        Color(0xFF38BDF8)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(backgroundColor))
            .cornerRadius(18.dp)
            .clickable(clickAction)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Header: Emoji and title
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(ColorProvider(accentColor.copy(alpha = 0.25f)))
                        .cornerRadius(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdown.emoji,
                        style = TextStyle(fontSize = 15.sp)
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = countdown.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF1F5F9)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Main Display: Big Days Number
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(ColorProvider(cardBackground))
                    .cornerRadius(12.dp)
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeRemaining.daysOnlyText,
                        style = TextStyle(
                            color = ColorProvider(if (timeRemaining.isPast) Color(0xFF94A3B8) else accentColor),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = if (timeRemaining.isPast) {
                            if (timeRemaining.totalDays == 1L) "DAY AGO" else "DAYS AGO"
                        } else {
                            if (timeRemaining.totalDays == 1L) "DAY LEFT" else "DAYS LEFT"
                        },
                        style = TextStyle(
                            color = ColorProvider(if (timeRemaining.isPast) Color(0xFF64748B) else Color(0xFFCBD5E1)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Subtitle info
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (timeRemaining.isPast) {
                        TimeFormatUtils.formatDateShort(countdown.targetTimeMillis)
                    } else {
                        timeRemaining.subtitleText
                    },
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF94A3B8)),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

class SmallCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallCountdownWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetPreferences.removeWidgetConfig(context, it) }
    }
}
