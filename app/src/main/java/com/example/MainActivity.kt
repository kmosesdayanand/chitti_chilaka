package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.service.BatteryAlertService
import com.example.service.VipRingManager
import com.example.ui.alerts.AlertsScreen
import com.example.ui.alerts.AlertsViewModel
import com.example.ui.contacts.ContactsScreen
import com.example.ui.contacts.ContactsViewModel
import com.example.ui.contacts.VipSetupScreen
import com.example.ui.countdowns.CountdownsScreen
import com.example.ui.countdowns.CountdownsViewModel
import com.example.ui.navigation.Screen
import com.example.ui.placeholders.SettingsPlaceholderScreen
import com.example.ui.splash.SentrySplashScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepSlate900
import com.example.ui.theme.DeepSlate950
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.widget.WidgetPreferences
import com.example.widget.WidgetUpdater
import com.example.worker.CountdownWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val countdownsViewModel: CountdownsViewModel by viewModels()
    private val alertsViewModel: AlertsViewModel by viewModels()
    private val contactsViewModel: ContactsViewModel by viewModels()
    private var pendingCountdownId = mutableStateOf<Long?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle opening from widget
        handleWidgetIntent(intent)

        try {
            CountdownWorker.schedulePeriodicUpdates(applicationContext)
            BatteryAlertService.syncWithDatabase(applicationContext)
            VipRingManager.restoreIfLeftActive(applicationContext)
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
            val windowSizeClass = calculateWindowSizeClass(this)
            val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
            val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            val systemDark = isSystemInDarkTheme()
            var themeMode by remember {
                mutableStateOf(WidgetPreferences.getThemeMode(this@MainActivity))
            }
            var dynamicColor by remember {
                mutableStateOf(WidgetPreferences.getDynamicColor(this@MainActivity))
            }

            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            // Sync status bar & navigation bar contrast with current theme
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window
                    if (window != null) {
                        val controller = WindowCompat.getInsetsController(window, view)
                        controller.isAppearanceLightStatusBars = !isDark
                        controller.isAppearanceLightNavigationBars = !isDark
                    }
                }
            }

            MyApplicationTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor
            ) {
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
                    SentrySplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    var showVipSetupScreen by remember { mutableStateOf(false) }

                    if (showVipSetupScreen) {
                        BackHandler {
                            showVipSetupScreen = false
                        }
                        VipSetupScreen(
                            viewModel = contactsViewModel,
                            onNavigateBack = { showVipSetupScreen = false }
                        )
                    } else {
                        // If user is on a secondary screen, pressing back returns to Countdowns
                        if (currentScreen != Screen.Countdowns) {
                            BackHandler {
                                currentScreen = Screen.Countdowns
                            }
                        }

                        if (isCompact) {
                            // Compact width (Phones in portrait, e.g. OnePlus Nord 6 in portrait): Bottom NavigationBar
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = DeepSlate950,
                                contentWindowInsets = WindowInsets.safeDrawing,
                                bottomBar = {
                                    SentryBottomNav(
                                        currentScreen = currentScreen,
                                        onTabSelected = { selected ->
                                            currentScreen = selected
                                        }
                                    )
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    MainScreenRouter(
                                        currentScreen = currentScreen,
                                        countdownsViewModel = countdownsViewModel,
                                        alertsViewModel = alertsViewModel,
                                        contactsViewModel = contactsViewModel,
                                        onNavigateToSetup = { showVipSetupScreen = true },
                                        onThemeModeChanged = { newMode -> themeMode = newMode },
                                        onDynamicColorChanged = { newDynamic -> dynamicColor = newDynamic }
                                    )
                                }
                            }
                        } else {
                            // Medium / Expanded width (Landscape phones, foldables, tablets): Side NavigationRail
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = DeepSlate950,
                                contentWindowInsets = WindowInsets.safeDrawing
                            ) { innerPadding ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    SentryNavigationRail(
                                        currentScreen = currentScreen,
                                        onTabSelected = { selected ->
                                            currentScreen = selected
                                        }
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .navigationBarsPadding(),
                                        contentAlignment = if (isExpanded) Alignment.TopCenter else Alignment.TopStart
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(if (isExpanded) Modifier.widthIn(max = 840.dp) else Modifier)
                                        ) {
                                            MainScreenRouter(
                                                currentScreen = currentScreen,
                                                countdownsViewModel = countdownsViewModel,
                                                alertsViewModel = alertsViewModel,
                                                contactsViewModel = contactsViewModel,
                                                onNavigateToSetup = { showVipSetupScreen = true },
                                                onThemeModeChanged = { newMode -> themeMode = newMode },
                                                onDynamicColorChanged = { newDynamic -> dynamicColor = newDynamic }
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        val countdownId = intent?.getLongExtra("EXTRA_COUNTDOWN_ID", -1L) ?: -1L
        val openItemOnTap = WidgetPreferences.isWidgetTapOpensItemEnabled(this)
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
private fun MainScreenRouter(
    currentScreen: Screen,
    countdownsViewModel: CountdownsViewModel,
    alertsViewModel: AlertsViewModel,
    contactsViewModel: ContactsViewModel,
    onNavigateToSetup: () -> Unit,
    onThemeModeChanged: (String) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit
) {
    when (currentScreen) {
        Screen.Countdowns -> {
            CountdownsScreen(
                viewModel = countdownsViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
        Screen.Alerts -> {
            AlertsScreen(
                viewModel = alertsViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
        Screen.Contacts -> {
            ContactsScreen(
                viewModel = contactsViewModel,
                onNavigateToSetup = onNavigateToSetup,
                modifier = Modifier.fillMaxSize()
            )
        }
        Screen.Settings -> {
            SettingsPlaceholderScreen(
                onThemeModeChanged = onThemeModeChanged,
                onDynamicColorChanged = onDynamicColorChanged,
                onNavigateToVipSetup = onNavigateToSetup
            )
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
                            fontSize = 10.sp,
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
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("nav_tab_${screen.route}")
            )
        }
    }
}

@Composable
fun SentryNavigationRail(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = DeepSlate900,
        contentColor = CyanPrimary,
        modifier = modifier
            .fillMaxHeight()
            .testTag("side_nav_rail")
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Screen.items.forEach { screen ->
            val isSelected = currentScreen.route == screen.route
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = CyanPrimary.copy(alpha = 0.15f),
                    selectedIconColor = CyanPrimary,
                    selectedTextColor = CyanPrimary,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("rail_tab_${screen.route}")
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
