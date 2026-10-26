package com.example.ui.placeholders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepSlate900
import com.example.ui.theme.DeepSlate950
import com.example.ui.theme.SurfaceSlate800
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.widget.WidgetPreferences
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.launch

@Composable
fun SettingsPlaceholderScreen(
    onThemeModeChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var themeMode by remember {
        mutableStateOf(WidgetPreferences.getThemeMode(context))
    }
    var widgetTapOpensItem by remember {
        mutableStateOf(WidgetPreferences.isWidgetTapOpensItemEnabled(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlate950)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("screen_settings")
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(CyanPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = CyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Sentry Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Theme & widget interaction preferences",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1. APPEARANCE & THEME SECTION
        Text(
            text = "APPEARANCE & THEME",
            style = MaterialTheme.typography.labelMedium.copy(
                color = CyanPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_theme_settings"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate900)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ThemeOptionRow(
                    title = "Follow System (Recommended)",
                    subtitle = "Automatically adapts to Android dark / light system setting",
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = themeMode == "SYSTEM",
                    onClick = {
                        themeMode = "SYSTEM"
                        WidgetPreferences.setThemeMode(context, "SYSTEM")
                        onThemeModeChanged("SYSTEM")
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ThemeOptionRow(
                    title = "Dark Theme",
                    subtitle = "Sentry Midnight Navy & Glowing Luminous Teal",
                    icon = Icons.Default.DarkMode,
                    isSelected = themeMode == "DARK",
                    onClick = {
                        themeMode = "DARK"
                        WidgetPreferences.setThemeMode(context, "DARK")
                        onThemeModeChanged("DARK")
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ThemeOptionRow(
                    title = "Light Theme",
                    subtitle = "Clean Crisp Slate & Ocean Teal accents",
                    icon = Icons.Default.LightMode,
                    isSelected = themeMode == "LIGHT",
                    onClick = {
                        themeMode = "LIGHT"
                        WidgetPreferences.setThemeMode(context, "LIGHT")
                        onThemeModeChanged("LIGHT")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. WIDGET INTERACTION SECTION
        Text(
            text = "WIDGET INTERACTION",
            style = MaterialTheme.typography.labelMedium.copy(
                color = CyanPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_widget_settings"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate900)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Open Event on Widget Tap",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (widgetTapOpensItem) {
                                "Tapping a home screen widget focuses and highlights that specific countdown in Sentry."
                            } else {
                                "Tapping a home screen widget opens the main Sentry overview without focusing on the item."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = widgetTapOpensItem,
                    onCheckedChange = { checked ->
                        widgetTapOpensItem = checked
                        WidgetPreferences.setWidgetTapOpensItemEnabled(context, checked)
                        coroutineScope.launch {
                            WidgetUpdater.updateAllWidgets(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyanPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceSlate800
                    ),
                    modifier = Modifier.testTag("switch_widget_tap_interaction")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. PERMISSIONS & SYSTEM INTEGRITY
        Text(
            text = "SYSTEM & PERMISSIONS",
            style = MaterialTheme.typography.labelMedium.copy(
                color = CyanPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate900)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PermissionItem(
                    icon = Icons.Default.RestartAlt,
                    title = "Receive Boot Completed",
                    description = "Used to re-schedule hourly widget timers after phone reboots so widgets stay accurate without internet or background draining."
                )
                Spacer(modifier = Modifier.height(12.dp))
                PermissionItem(
                    icon = Icons.Default.Storage,
                    title = "Local Room Storage",
                    description = "All countdowns and alert rules are saved strictly locally on your phone. No internet access or external accounts required."
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. HOME SCREEN WIDGETS GUIDE
        Text(
            text = "AVAILABLE WIDGETS",
            style = MaterialTheme.typography.labelMedium.copy(
                color = CyanPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate900)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                WidgetGuideItem(
                    title = "1. Sentry Countdown (2x2)",
                    description = "Shows a big 'days left' badge, countdown title, and emoji. Configurable to any countdown."
                )
                Spacer(modifier = Modifier.height(10.dp))
                WidgetGuideItem(
                    title = "2. Sentry Upcoming List (4x2)",
                    description = "Displays the next 3 upcoming events with direct taps to open each countdown."
                )
                Spacer(modifier = Modifier.height(10.dp))
                WidgetGuideItem(
                    title = "3. Sentry Progress Ring (2x2)",
                    description = "Features a circular elapsed time ring from creation date to target date with % completed."
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. APP INFORMATION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate900)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sentry_logo),
                        contentDescription = "Sentry",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Sentry",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Version 1.0 • Smart Device Monitor",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyanPrimary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) CyanPrimary else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) CyanPrimary else TextPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint = if (isSelected) CyanPrimary else TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CyanPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun WidgetGuideItem(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            lineHeight = 18.sp
        )
    }
}
