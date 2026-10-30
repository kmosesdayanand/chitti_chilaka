package com.example.ui.countdowns

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Countdown
import com.example.ui.theme.BorderSlate700
import com.example.ui.theme.CardStyles
import com.example.ui.theme.CountdownCategories
import com.example.ui.theme.CountdownColors
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepSlate900
import com.example.ui.theme.DisplayUnits
import com.example.ui.theme.SurfaceSlate800
import com.example.ui.theme.SurfaceVariantSlate700
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TimeFormatUtils
import java.util.Calendar

private val PresetEmojis = listOf(
    "⏳", "🎂", "🚀", "🎆", "✈️", "🏕️", "🎓", "💍", "🎄", "🏖️",
    "💻", "🎯", "🏋️", "❤️", "🌟", "🏆", "🚗", "⛵", "🏖️", "🎉"
)

@Composable
fun AddEditCountdownDialog(
    countdown: Countdown?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        targetTimeMillis: Long,
        createdTimeMillis: Long,
        emoji: String,
        colorHex: String,
        notes: String,
        category: String,
        cardStyle: String,
        displayUnit: String
    ) -> Unit
) {
    val context = LocalContext.current
    val isEditing = countdown != null

    var title by remember { mutableStateOf(countdown?.title ?: "") }
    var selectedCalendar by remember {
        val cal = Calendar.getInstance()
        if (countdown != null) {
            cal.timeInMillis = countdown.targetTimeMillis
        } else {
            cal.add(Calendar.DAY_OF_YEAR, 7)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        mutableStateOf(cal)
    }

    var selectedEmoji by remember { mutableStateOf(countdown?.emoji ?: "⏳") }
    var customEmojiInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(countdown?.colorHex ?: CountdownColors[0]) }
    var selectedCategory by remember { mutableStateOf(countdown?.category ?: "General") }
    var selectedCardStyle by remember { mutableStateOf(countdown?.cardStyle ?: "modern") }
    var selectedDisplayUnit by remember { mutableStateOf(countdown?.displayUnit ?: "all") }
    var notes by remember { mutableStateOf(countdown?.notes ?: "") }
    var targetMillis by remember { mutableLongStateOf(selectedCalendar.timeInMillis) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_edit_countdown_dialog"),
        containerColor = DeepSlate900,
        title = {
            Text(
                text = if (isEditing) "Customize Countdown" else "New Custom Countdown",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .imePadding()
                    .verticalScroll(scrollState)
            ) {
                // Event Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("Event Title") },
                    placeholder = { Text("e.g., Vacation Trip, Birthday, Launch") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_countdown_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = BorderSlate700,
                        focusedLabelColor = CyanPrimary,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date & Time Pickers
                Text(
                    text = "TARGET DATE & TIME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val year = selectedCalendar.get(Calendar.YEAR)
                            val month = selectedCalendar.get(Calendar.MONTH)
                            val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)
                            DatePickerDialog(context, { _, y, m, d ->
                                val newCal = selectedCalendar.clone() as Calendar
                                newCal.set(Calendar.YEAR, y)
                                newCal.set(Calendar.MONTH, m)
                                newCal.set(Calendar.DAY_OF_MONTH, d)
                                selectedCalendar = newCal
                                targetMillis = newCal.timeInMillis
                            }, year, month, day).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_pick_date"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary,
                            containerColor = SurfaceSlate800
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Date",
                            modifier = Modifier.size(16.dp),
                            tint = CyanPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = TimeFormatUtils.formatDateShort(targetMillis),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val hour = selectedCalendar.get(Calendar.HOUR_OF_DAY)
                            val minute = selectedCalendar.get(Calendar.MINUTE)
                            TimePickerDialog(context, { _, h, m ->
                                val newCal = selectedCalendar.clone() as Calendar
                                newCal.set(Calendar.HOUR_OF_DAY, h)
                                newCal.set(Calendar.MINUTE, m)
                                selectedCalendar = newCal
                                targetMillis = newCal.timeInMillis
                            }, hour, minute, false).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_pick_time"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary,
                            containerColor = SurfaceSlate800
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            modifier = Modifier.size(16.dp),
                            tint = CyanPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = TimeFormatUtils.formatTimeOnly(targetMillis),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text(
                    text = "CATEGORY TAG",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CountdownCategories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = CyanPrimary,
                                containerColor = SurfaceSlate800,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) CyanPrimary else BorderSlate700,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Visual Style Selector
                Text(
                    text = "CARD THEME & APPEARANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CardStyles.forEach { (styleKey, styleLabel) ->
                        val isSelected = selectedCardStyle == styleKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CyanPrimary.copy(alpha = 0.2f) else SurfaceSlate800)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CyanPrimary else BorderSlate700,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCardStyle = styleKey }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = styleLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CyanPrimary else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display Units Choice
                Text(
                    text = "TIME DISPLAY FORMAT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DisplayUnits.forEach { (unitKey, unitLabel) ->
                        val isSelected = selectedDisplayUnit == unitKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDisplayUnit = unitKey },
                            label = { Text(unitLabel, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = CyanPrimary,
                                containerColor = SurfaceSlate800,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) CyanPrimary else BorderSlate700,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Accent Color (12 colors)
                Text(
                    text = "ACCENT COLOR (12 CHOICES)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CountdownColors.forEach { hex ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            CyanPrimary
                        }
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                                .testTag("color_option_$hex"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Emoji Selection
                Text(
                    text = "EMOJI / ICON",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetEmojis.forEach { emoji ->
                        val isSelected = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CyanPrimary.copy(alpha = 0.3f) else SurfaceSlate800)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CyanPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedEmoji = emoji }
                                .testTag("emoji_option_$emoji"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customEmojiInput,
                    onValueChange = {
                        customEmojiInput = it
                        if (it.isNotBlank()) {
                            selectedEmoji = it.trim().take(4)
                        }
                    },
                    label = { Text("Or custom emoji / icon") },
                    placeholder = { Text("Type any emoji e.g. 🏆 or 🌴") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_emoji"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = BorderSlate700,
                        focusedLabelColor = CyanPrimary,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notes input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 150) notes = it },
                    label = { Text("Notes / Checklist (Optional)") },
                    placeholder = { Text("e.g. Flight 204, book hotel, gift ideas") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_countdown_notes"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = BorderSlate700,
                        focusedLabelColor = CyanPrimary,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Card
                Text(
                    text = "LIVE PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                val previewColor = try {
                    Color(android.graphics.Color.parseColor(selectedColorHex))
                } catch (e: Exception) {
                    CyanPrimary
                }

                val previewTime = TimeFormatUtils.computeRemaining(
                    targetTimeMillis = targetMillis,
                    createdTimeMillis = countdown?.createdTimeMillis ?: System.currentTimeMillis(),
                    displayUnit = selectedDisplayUnit
                )

                val previewCardColor = when (selectedCardStyle) {
                    "tinted" -> previewColor.copy(alpha = 0.15f)
                    "minimal" -> DeepSlate900
                    else -> SurfaceSlate800
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selectedCardStyle == "minimal") 1.5.dp else 1.dp,
                            color = if (selectedCardStyle == "minimal") previewColor else previewColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = previewCardColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(previewColor.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = selectedEmoji, fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title.ifBlank { "Event Title" },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(previewColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = selectedCategory,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = previewColor
                                        )
                                    )
                                }
                            }
                            Text(
                                text = TimeFormatUtils.formatDate(targetMillis),
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(previewColor.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = previewTime.customDisplaySummary,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = previewColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            countdown?.id ?: 0L,
                            title,
                            targetMillis,
                            countdown?.createdTimeMillis ?: System.currentTimeMillis(),
                            selectedEmoji,
                            selectedColorHex,
                            notes,
                            selectedCategory,
                            selectedCardStyle,
                            selectedDisplayUnit
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("dialog_save_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (isEditing) "Save Customizations" else "Create Countdown", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
