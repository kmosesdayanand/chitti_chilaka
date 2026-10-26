package com.example.widget

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import com.example.data.Countdown
import com.example.util.TimeFormatUtils

private val COUNTDOWN_ID_KEY = ActionParameters.Key<Long>("EXTRA_COUNTDOWN_ID")

class MediumCountdownWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = WidgetDataLoader.loadUpcoming(context, 3)

        provideContent {
            val prefs = currentState<Preferences>()
            val tick = prefs[WIDGET_REFRESH_TICK_KEY]
            val items by produceState<List<Countdown>>(initial, tick) {
                value = WidgetDataLoader.loadUpcoming(context, 3)
            }
            MediumWidgetContent(context = context, items = items)
        }
    }
}

@androidx.compose.runtime.Composable
private fun MediumWidgetContent(context: Context, items: List<Countdown>) {
    val backgroundColor = Color(0xFF0F172A)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(backgroundColor))
            .cornerRadius(18.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱️",
                    style = TextStyle(fontSize = 13.sp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "UPCOMING COUNTDOWNS",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF38BDF8)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active countdowns. Tap to add one!",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8)),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                items.forEachIndexed { index, countdown ->
                    MediumCountdownRow(context = context, countdown = countdown)
                    if (index < items.size - 1) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MediumCountdownRow(context: Context, countdown: Countdown) {
    val opensItem = WidgetPreferences.isWidgetTapOpensItemEnabled(context)
    val rowClickAction = if (opensItem) {
        actionStartActivity<MainActivity>(
            actionParametersOf(COUNTDOWN_ID_KEY to countdown.id)
        )
    } else {
        actionStartActivity<MainActivity>()
    }

    val timeRemaining = TimeFormatUtils.computeRemaining(
        targetTimeMillis = countdown.targetTimeMillis,
        createdTimeMillis = countdown.createdTimeMillis
    )

    val accentColor = try {
        Color(android.graphics.Color.parseColor(countdown.colorHex))
    } catch (e: Exception) {
        Color(0xFF38BDF8)
    }

    val cardColor = Color(0xFF1E293B)

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(cardColor))
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(rowClickAction)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji circle
            Box(
                modifier = GlanceModifier
                    .size(24.dp)
                    .background(ColorProvider(accentColor.copy(alpha = 0.2f)))
                    .cornerRadius(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdown.emoji,
                    style = TextStyle(fontSize = 12.sp)
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Title and date
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = countdown.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF8FAFC)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = TimeFormatUtils.formatDateShort(countdown.targetTimeMillis),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF94A3B8)),
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Remaining Badge
            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(if (timeRemaining.isPast) Color(0xFF334155) else accentColor.copy(alpha = 0.25f)))
                    .cornerRadius(6.dp)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (timeRemaining.isPast) {
                        timeRemaining.badgeText
                    } else if (timeRemaining.totalDays > 0) {
                        "${timeRemaining.totalDays}d left"
                    } else {
                        "${timeRemaining.hours}h ${timeRemaining.minutes}m"
                    },
                    style = TextStyle(
                        color = ColorProvider(if (timeRemaining.isPast) Color(0xFF94A3B8) else accentColor),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class MediumCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumCountdownWidget()
}
