package com.example.ui.placeholders

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPlaceholderScreen(
    onThemeModeChanged: (String) -> Unit = {},
    onDynamicColorChanged: (Boolean) -> Unit = {}
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

    // Live preview controls state
    var previewSwitchChecked by remember { mutableStateOf(true) }
    var previewSliderValue by remember { mutableFloatStateOf(0.65f) }

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

        // 2. THEME COLORS LIVE PALETTE CARD
        Text(
            text = "THEME COLORS PALETTE",
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
                .testTag("card_theme_colors_palette"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate900)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Active Color Tokens",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = if (dynamicColorEnabled && supportsDynamicColor) {
                        "Generated in real-time from device wallpaper via Material You"
                    } else {
                        "Sentry custom brand theme palette"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Color Swatches Grid
                val currentScheme = MaterialTheme.colorScheme
                val colorTokens = listOf(
                    "Primary" to currentScheme.primary,
                    "On primary" to currentScheme.onPrimary,
                    "Secondary" to currentScheme.secondary,
                    "Tertiary" to currentScheme.tertiary,
                    "Background" to currentScheme.background,
                    "Surface" to currentScheme.surface,
                    "Surface container" to currentScheme.surfaceContainer,
                    "On surface" to currentScheme.onSurface,
                    "On surface variant" to currentScheme.onSurfaceVariant,
                    "Outline" to currentScheme.outline,
                    "Error" to currentScheme.error
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorTokens.forEach { (name, color) ->
                        ColorSwatchItem(name = name, color = color)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Row with Controls
                Text(
                    text = "Controls Live Preview",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("preview_primary_button")
                    ) {
                        Text("Primary")
                    }

                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("preview_outlined_button")
                    ) {
                        Text("Outlined")
                    }

                    Switch(
                        checked = previewSwitchChecked,
                        onCheckedChange = { previewSwitchChecked = it },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("preview_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Slider Preview
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Slider Preview (${(previewSliderValue * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Slider(
                        value = previewSliderValue,
                        onValueChange = { previewSliderValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preview_slider")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Typography Sample
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Sample Title • Typography Hierarchy",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Material 3 dynamic typography adapts legibly to wallpaper colors, AMOLED deep blacks, and display zoom levels.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
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
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("switch_widget_tap_interaction")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. PERMISSIONS & SYSTEM INTEGRITY
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

        // 5. HOME SCREEN WIDGETS GUIDE
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
                    description = "Shows a big 'days left' badge, countdown title, and emoji. Configurable to any countdown with GlanceTheme system styling."
                )
                Spacer(modifier = Modifier.height(10.dp))
                WidgetGuideItem(
                    title = "2. Sentry Upcoming List (4x2)",
                    description = "Displays the next 3 upcoming events with direct taps to open each countdown."
                )
                Spacer(modifier = Modifier.height(10.dp))
                WidgetGuideItem(
                    title = "3. Sentry Progress Ring (2x2)",
                    description = "Features a circular elapsed time ring matching system palette with % completed."
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. APP INFORMATION CARD
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
private fun ColorSwatchItem(name: String, color: Color) {
    val hexCode = String.format("#%06X", (color.toArgb() and 0x00FFFFFF))
    val isLight = color.luminance() > 0.45f
    val textColor = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 140.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 12.sp
                )
            )
            Text(
                text = hexCode,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )
            )
        }
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
