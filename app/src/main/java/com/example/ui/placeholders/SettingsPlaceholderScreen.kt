package com.example.ui.placeholders

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.VipPreferences
import com.example.data.VipSettings
import com.example.service.VipRingManager
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepSlate900
import com.example.ui.theme.DeepSlate950
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.widget.WidgetPreferences
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.launch

@Composable
fun SettingsPlaceholderScreen(
    onThemeModeChanged: (String) -> Unit = {},
    onDynamicColorChanged: (Boolean) -> Unit = {},
    onNavigateToVipSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var themeMode by remember {
        mutableStateOf(WidgetPreferences.getThemeMode(context))
    }
    var dynamicColorEnabled by remember {
        mutableStateOf(if (supportsDynamicColor) WidgetPreferences.getDynamicColor(context) else false)
    }
    var widgetTapOpensItem by remember {
        mutableStateOf(WidgetPreferences.isWidgetTapOpensItemEnabled(context))
    }

    // VIP Ring Preferences State
    val vipPrefs = remember { VipPreferences.getInstance(context) }
    val vipSettings by vipPrefs.vipSettingsFlow.collectAsState(initial = VipSettings())
    var testSecondsLeft by remember { mutableStateOf<Int?>(null) }

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
                    text = "Theme, system colors & widget interaction",
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

                Spacer(modifier = Modifier.height(10.dp))

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

                Spacer(modifier = Modifier.height(10.dp))

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

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Use System Colors (Material You Dynamic Color Switch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Use system colors",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (supportsDynamicColor) {
                                    "Colors follow your wallpaper (Android 12+)"
                                } else {
                                    "Needs Android 12 or newer"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (supportsDynamicColor) TextSecondary else TextMuted
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = dynamicColorEnabled && supportsDynamicColor,
                        enabled = supportsDynamicColor,
                        onCheckedChange = { checked ->
                            if (supportsDynamicColor) {
                                dynamicColorEnabled = checked
                                WidgetPreferences.setDynamicColor(context, checked)
                                onDynamicColorChanged(checked)
                            }
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("switch_system_colors")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. VIP RING (FULL-VOLUME) SECTION
        Text(
            text = "VIP CONTACTS RING",
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
                .testTag("card_vip_ring_settings"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate900)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Master Switch: Enable VIP ring
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Enable VIP ring",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Emergency calls from VIP contacts ring loud, even in Silent or DND",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                lineHeight = 16.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = vipSettings.vipRingEnabled,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                vipPrefs.setVipRingEnabled(checked)
                            }
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("switch_vip_ring_master")
                    )
                }

                if (vipSettings.vipRingEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Ring Volume Slider (50% to 100%)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VIP Ring volume",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                            Text(
                                text = "${(vipSettings.ringVolumePercent * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = CyanPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = vipSettings.ringVolumePercent,
                            onValueChange = { newVal ->
                                coroutineScope.launch {
                                    vipPrefs.setRingVolumePercent(newVal)
                                }
                            },
                            valueRange = 0.50f..1.0f,
                            steps = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_vip_ring_volume")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch: Override Do Not Disturb
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoNotDisturbOn,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Override Do Not Disturb",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Temporarily permits all interruptions during call",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = vipSettings.overrideDnd,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { vipPrefs.setOverrideDnd(checked) }
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("switch_vip_override_dnd")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch: Also vibrate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Also vibrate",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Vibrates along with loud sound during VIP call",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = vipSettings.alsoVibrate,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { vipPrefs.setAlsoVibrate(checked) }
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("switch_vip_also_vibrate")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch: Ring even if the phone is on silent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Ring even if phone is on silent",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Switches ringer mode to Normal for incoming VIP",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = vipSettings.ringOnSilent,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { vipPrefs.setRingOnSilent(checked) }
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("switch_vip_ring_on_silent")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Test VIP Ring button (5 seconds)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isTesting = testSecondsLeft != null
                        Button(
                            onClick = {
                                if (isTesting) {
                                    VipRingManager.stopTestRing(context) {
                                        testSecondsLeft = null
                                    }
                                } else {
                                    VipRingManager.startTestRing(
                                        context = context,
                                        onTick = { testSecondsLeft = it },
                                        onFinished = { testSecondsLeft = null }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTesting) MaterialTheme.colorScheme.error else CyanPrimary,
                                contentColor = DeepSlate950
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .testTag("button_test_vip_ring")
                        ) {
                            Icon(
                                imageVector = if (isTesting) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTesting) "Testing (${testSecondsLeft}s) - Stop" else "Test VIP ring (5s)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToVipSetup,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .testTag("button_open_vip_setup_from_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Setup Checklist", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. WIDGET INTERACTION SECTION
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
                            lineHeight = 16.5.sp
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
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("switch_widget_tap_interaction")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. APP INFORMATION CARD
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
                fontSize = 11.sp
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
