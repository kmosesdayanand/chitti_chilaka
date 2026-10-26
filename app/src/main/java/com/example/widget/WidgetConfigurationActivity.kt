package com.example.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.data.AppDatabase
import com.example.data.Countdown
import com.example.ui.theme.MyApplicationTheme
import com.example.util.TimeFormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set the result to CANCELED. If the user backs out, the widget will not be added.
        setResult(Activity.RESULT_CANCELED)

        // Extract appWidgetId robustly from intent or extras
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            val extraObj = intent?.extras?.get(AppWidgetManager.EXTRA_APPWIDGET_ID)
            appWidgetId = when (extraObj) {
                is Int -> extraObj
                is Long -> extraObj.toInt()
                is String -> extraObj.toIntOrNull() ?: AppWidgetManager.INVALID_APPWIDGET_ID
                else -> AppWidgetManager.INVALID_APPWIDGET_ID
            }
        }

        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val themeMode = WidgetPreferences.getThemeMode(this@WidgetConfigurationActivity)
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            MyApplicationTheme(darkTheme = isDark) {
                val coroutineScope = rememberCoroutineScope()
                var countdowns by remember { mutableStateOf<List<Countdown>?>(null) }
                var tapOpensItem by remember {
                    mutableStateOf(WidgetPreferences.isWidgetTapOpensItemEnabled(this@WidgetConfigurationActivity))
                }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.getInstance(applicationContext)
                        val list = db.countdownDao().getAllCountdownsSync()
                        withContext(Dispatchers.Main) {
                            countdowns = list
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "Select Countdown",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Choose an event for this widget",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { finish() },
                                    modifier = Modifier.testTag("config_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val list = countdowns
                        if (list == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (list.isEmpty()) {
                            Text(
                                text = "No countdowns available.\nOpen Sentry to create one.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Open event on widget tap",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (tapOpensItem) {
                                                        "Tapping widget focuses this countdown in Sentry"
                                                    } else {
                                                        "Tapping widget opens the main Sentry overview"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Switch(
                                                checked = tapOpensItem,
                                                onCheckedChange = { checked ->
                                                    tapOpensItem = checked
                                                    WidgetPreferences.setWidgetTapOpensItemEnabled(
                                                        this@WidgetConfigurationActivity,
                                                        checked
                                                    )
                                                    coroutineScope.launch {
                                                        WidgetUpdater.updateAllWidgets(this@WidgetConfigurationActivity)
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }
                                }

                                items(list, key = { it.id }) { item ->
                                    CountdownSelectionItem(
                                        countdown = item,
                                        onClick = {
                                            coroutineScope.launch {
                                                // 1. Synchronously persist to SharedPreferences (widget-specific + last configured)
                                                WidgetPreferences.setCountdownId(
                                                    applicationContext,
                                                    appWidgetId,
                                                    item.id
                                                )

                                                // 2. Persist to Glance DataStore state and immediately trigger widget update
                                                withContext(Dispatchers.IO) {
                                                    if (appWidgetId > 0) {
                                                        val glanceManager = GlanceAppWidgetManager(applicationContext)
                                                        val glanceId = runCatching { glanceManager.getGlanceIdBy(appWidgetId) }.getOrNull()
                                                            ?: androidx.glance.appwidget.AppWidgetId(appWidgetId)

                                                        runCatching {
                                                            updateAppWidgetState(applicationContext, glanceId) { prefs ->
                                                                prefs[WIDGET_COUNTDOWN_PREF_KEY] = item.id
                                                            }
                                                        }

                                                        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                                                        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                                                        val providerClass = info?.provider?.className ?: ""

                                                        when {
                                                            providerClass.contains("Progress") -> {
                                                                runCatching { ProgressCountdownWidget().update(applicationContext, glanceId) }
                                                            }
                                                            providerClass.contains("Medium") -> {
                                                                runCatching { MediumCountdownWidget().update(applicationContext, glanceId) }
                                                            }
                                                            providerClass.contains("Small") -> {
                                                                runCatching { SmallCountdownWidget().update(applicationContext, glanceId) }
                                                            }
                                                        }
                                                    }

                                                    // Also update all widgets to make sure UI is immediately fresh
                                                    runCatching {
                                                        WidgetUpdater.updateAllWidgets(applicationContext)
                                                    }
                                                }

                                                // 3. Set OK result with AppWidgetId so Android host displays the widget
                                                val resultValue = Intent().apply {
                                                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                                    }
                                                }
                                                setResult(Activity.RESULT_OK, resultValue)
                                                finish()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownSelectionItem(
    countdown: Countdown,
    onClick: () -> Unit
) {
    val accentColor = remember(countdown.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(countdown.colorHex))
        } catch (e: Exception) {
            Color(0xFF38BDF8)
        }
    }

    val timeRemaining = remember(countdown.targetTimeMillis, countdown.createdTimeMillis, countdown.displayUnit) {
        TimeFormatUtils.computeRemaining(
            targetTimeMillis = countdown.targetTimeMillis,
            createdTimeMillis = countdown.createdTimeMillis,
            displayUnit = countdown.displayUnit
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("select_countdown_${countdown.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = countdown.emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = countdown.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFF1F5F9),
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = countdown.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = accentColor
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = TimeFormatUtils.formatDate(countdown.targetTimeMillis),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (timeRemaining.isPast) Color(0xFF334155) else accentColor.copy(alpha = 0.25f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = timeRemaining.customDisplaySummary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (timeRemaining.isPast) Color(0xFF94A3B8) else accentColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
