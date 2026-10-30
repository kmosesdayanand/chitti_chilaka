package com.example.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.GlanceTheme
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.Countdown
import com.example.util.RingBitmapUtil
import com.example.util.TimeFormatUtils

private val COUNTDOWN_ID_KEY = ActionParameters.Key<Long>("EXTRA_COUNTDOWN_ID")

class ProgressCountdownWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(160.dp, 160.dp),
            DpSize(240.dp, 240.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId: Int = when (id) {
            is AppWidgetId -> id.appWidgetId
            else -> {
                runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrNull()
                    ?: Regex("""appWidgetId=(\d+)""").find(id.toString())?.groupValues?.get(1)?.toIntOrNull()
                    ?: -1
            }
        }

        val initialPrefs = runCatching {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        }.getOrNull()
        val initialCountdownId = initialPrefs?.get(WIDGET_COUNTDOWN_PREF_KEY)
        val initial = WidgetDataLoader.loadCountdown(context, appWidgetId, initialCountdownId)

        provideContent {
            val prefs = currentState<Preferences>()
            val stateId = prefs[WIDGET_COUNTDOWN_PREF_KEY]
            val tick = prefs[WIDGET_REFRESH_TICK_KEY]
            val countdown by produceState<Countdown?>(initial, stateId, tick) {
                value = WidgetDataLoader.loadCountdown(context, appWidgetId, stateId)
            }
            GlanceTheme {
                ProgressWidgetContent(context = context, countdown = countdown)
            }
        }
    }
}

@Composable
private fun ProgressWidgetContent(context: Context, countdown: Countdown?) {
    val opensItem = WidgetPreferences.isWidgetTapOpensItemEnabled(context)
    val clickAction = if (countdown != null && opensItem) {
        actionStartActivity<MainActivity>(actionParametersOf(COUNTDOWN_ID_KEY to countdown.id))
    } else {
        actionStartActivity<MainActivity>()
    }

    if (countdown == null) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(18.dp)
                .padding(14.dp)
                .clickable(clickAction),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "⏳", style = TextStyle(fontSize = 30.sp))
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "No countdowns",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Tap to setup",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
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
        Color(0xFF00E5D0)
    }

    val ringColorInt = try {
        android.graphics.Color.parseColor(countdown.colorHex)
    } catch (e: Exception) {
        if (Build.VERSION.SDK_INT >= 31) {
            context.getColor(android.R.color.system_accent1_500)
        } else {
            0xFF00E5D0.toInt()
        }
    }

    val trackColorInt = if (Build.VERSION.SDK_INT >= 31) {
        try {
            context.getColor(android.R.color.system_neutral2_700)
        } catch (e: Exception) {
            android.graphics.Color.argb(45, android.graphics.Color.red(ringColorInt), android.graphics.Color.green(ringColorInt), android.graphics.Color.blue(ringColorInt))
        }
    } else {
        android.graphics.Color.argb(45, android.graphics.Color.red(ringColorInt), android.graphics.Color.green(ringColorInt), android.graphics.Color.blue(ringColorInt))
    }

    val progressPercent = (timeRemaining.progress * 100).toInt()
    val ringBitmap = RingBitmapUtil.createProgressRingBitmap(
        progress = timeRemaining.progress,
        ringColor = ringColorInt,
        trackColor = trackColorInt,
        sizePx = 220,
        strokeWidthPx = 18f
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
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
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Ring container with emoji and percentage overlay
            Box(
                modifier = GlanceModifier.size(86.dp),
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
                            color = if (timeRemaining.isPast) GlanceTheme.colors.onSurfaceVariant else ColorProvider(accentColor),
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
                    color = if (timeRemaining.isPast) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetPreferences.removeWidgetConfig(context, it) }
    }
}
