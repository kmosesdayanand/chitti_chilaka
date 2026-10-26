package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.BatteryAlertService
import com.example.ui.alerts.AlertsScreen
import com.example.ui.alerts.AlertsViewModel
import com.example.ui.countdowns.CountdownsScreen
import com.example.ui.countdowns.CountdownsViewModel
import com.example.ui.navigation.Screen
import com.example.ui.placeholders.ContactsPlaceholderScreen
import com.example.ui.placeholders.SettingsPlaceholderScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepSlate900
import com.example.ui.theme.DeepSlate950
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.widget.WidgetUpdater
import com.example.worker.CountdownWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val countdownsViewModel: CountdownsViewModel by viewModels()
    private val alertsViewModel: AlertsViewModel by viewModels()
    private var pendingCountdownId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle opening from widget
        handleWidgetIntent(intent)

        try {
            CountdownWorker.schedulePeriodicUpdates(applicationContext)
            BatteryAlertService.syncWithDatabase(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdater.updateAllWidgets(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            var themeMode by remember {
                mutableStateOf(com.example.widget.WidgetPreferences.getThemeMode(this@MainActivity))
            }
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            MyApplicationTheme(darkTheme = isDark) {
                var showSplash by remember { mutableStateOf(true) }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Countdowns) }
                val targetId by pendingCountdownId

                // When an intent with a target countdown arrives, switch to Countdowns tab and highlight
                LaunchedEffect(targetId) {
                    if (targetId != null && targetId != -1L) {
                        showSplash = false
                        currentScreen = Screen.Countdowns
                        countdownsViewModel.setHighlightedId(targetId)
                    }
                }

                if (showSplash) {
                    com.example.ui.splash.SentrySplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    // If user is on a placeholder screen, pressing back returns to Countdowns
                    if (currentScreen != Screen.Countdowns) {
                        BackHandler {
                            currentScreen = Screen.Countdowns
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = DeepSlate950,
                        bottomBar = {
                            SentryBottomNav(
                                currentScreen = currentScreen,
                                onTabSelected = { selected ->
                                    currentScreen = selected
                                }
                            )
                        }
                    ) { innerPadding ->
                        when (currentScreen) {
                            Screen.Countdowns -> {
                                CountdownsScreen(
                                    viewModel = countdownsViewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            Screen.Alerts -> {
                                AlertsScreen(
                                    viewModel = alertsViewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            Screen.Contacts -> {
                                ContactsPlaceholderScreen()
                            }
                            Screen.Settings -> {
                                SettingsPlaceholderScreen(
                                    onThemeModeChanged = { newMode ->
                                        themeMode = newMode
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        val countdownId = intent?.getLongExtra("EXTRA_COUNTDOWN_ID", -1L) ?: -1L
        val openItemOnTap = com.example.widget.WidgetPreferences.isWidgetTapOpensItemEnabled(this)
        if (countdownId != -1L && openItemOnTap) {
            pendingCountdownId.value = countdownId
            countdownsViewModel.setHighlightedId(countdownId)
        } else {
            pendingCountdownId.value = null
            countdownsViewModel.setHighlightedId(null)
        }
    }
}

@Composable
fun SentryBottomNav(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = DeepSlate900,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        Screen.items.forEach { screen ->
            val isSelected = currentScreen.route == screen.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title,
                        tint = if (isSelected) CyanPrimary else TextMuted
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isSelected) CyanPrimary else TextMuted
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CyanPrimary.copy(alpha = 0.15f),
                    selectedIconColor = CyanPrimary,
                    selectedTextColor = CyanPrimary,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_tab_${screen.route}")
            )
        }
    }
}
