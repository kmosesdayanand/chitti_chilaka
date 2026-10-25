package com.example.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
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
import com.example.util.RingBitmapUtil
import com.example.util.TimeFormatUtils

private val COUNTDOWN_ID_KEY = ActionParameters.Key<Long>("EXTRA_COUNTDOWN_ID")

class ProgressCountdownWidget : GlanceAppWidget() {

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

        // 2. Check widget-specific SharedPreferences first
        val specificPrefsId = if (appWidgetId > 0) {
            WidgetPreferences.getSpecificCountdownId(context, appWidgetId)
        } else -1L

        // 3. Check Glance DataStore state next
        val stateCountdownId: Long? = runCatching {
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
            prefs[WIDGET_COUNTDOWN_PREF_KEY]
        }.getOrNull()

        // 4. Fallback to last configured countdown ID
        val lastConfiguredId = WidgetPreferences.getLastConfiguredCountdownId(context)

        // Determine target countdown: prioritize specific widget configuration, then DataStore, then last configured
        val targetCountdownId = when {
            specificPrefsId != -1L -> specificPrefsId
            stateCountdownId != null && stateCountdownId != -1L -> stateCountdownId
            lastConfiguredId != -1L -> lastConfiguredId
            else -> -1L
        }

        val countdown: Countdown? = if (targetCountdownId != -1L) {
            db.countdownDao().getCountdownByIdSync(targetCountdownId)
                ?: getFallbackCountdown(db)
        } else {
            getFallbackCountdown(db)
        }

        provideContent {
            ProgressWidgetContent(countdown = countdown)
        }
    }

    private suspend fun getFallbackCountdown(db: AppDatabase): Countdown? {
        val now = System.currentTimeMillis()
        val upcoming = db.countdownDao().getUpcomingCountdownsSync(now, 1)
        return upcoming.firstOrNull() ?: db.countdownDao().getAllCountdownsSync().firstOrNull()
    }
}

@androidx.compose.runtime.Composable
private fun ProgressWidgetContent(countdown: Countdown?) {
    val clickAction = if (countdown != null) {
        actionStartActivity<MainActivity>(actionParametersOf(COUNTDOWN_ID_KEY to countdown.id))
    } else {
        actionStartActivity<MainActivity>()
    }

    val backgroundColor = Color(0xFF0F172A)

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "⏳", style = TextStyle(fontSize = 32.sp))
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "No countdowns",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE2E8F0)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
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

    val progressPercent = (timeRemaining.progress * 100).toInt()
    val ringBitmap = RingBitmapUtil.createProgressRingBitmap(
        progress = timeRemaining.progress,
        colorHex = countdown.colorHex,
        sizePx = 200,
        strokeWidthPx = 18f
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(backgroundColor))
            .cornerRadius(18.dp)
            .padding(10.dp)
            .clickable(clickAction),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title
            Text(
                text = countdown.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFF1F5F9)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Ring container with emoji and percentage overlay
            Box(
                modifier = GlanceModifier.size(88.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(ringBitmap),
                    contentDescription = "Countdown Progress Ring",
                    modifier = GlanceModifier.fillMaxSize()
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = countdown.emoji,
                        style = TextStyle(fontSize = 18.sp)
                    )
                    Text(
                        text = "$progressPercent%",
                        style = TextStyle(
                            color = ColorProvider(if (timeRemaining.isPast) Color(0xFF94A3B8) else accentColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Bottom text: days left or passed
            Text(
                text = if (timeRemaining.isPast) {
                    timeRemaining.badgeText
                } else if (timeRemaining.totalDays > 0) {
                    "${timeRemaining.totalDays} days left"
                } else {
                    timeRemaining.badgeText
                },
                style = TextStyle(
                    color = ColorProvider(if (timeRemaining.isPast) Color(0xFF64748B) else Color(0xFFCBD5E1)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

class ProgressCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProgressCountdownWidget()
}
