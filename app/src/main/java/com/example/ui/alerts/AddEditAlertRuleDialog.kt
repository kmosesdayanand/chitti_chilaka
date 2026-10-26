package com.example.ui.alerts

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AlertRule
import com.example.ui.theme.BorderSlate700
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepSlate900
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceSlate800
import com.example.ui.theme.SurfaceVariantSlate700
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AudioAlertPlayer

@Composable
fun AddEditAlertRuleDialog(
    rule: AlertRule?,
    onDismiss: () -> Unit,
    onSave: (id: Long, level: Int, direction: String, soundType: String, soundUri: String?, volume: Float, startMs: Int, endMs: Int) -> Unit
) {
    val context = LocalContext.current

    var level by remember { mutableIntStateOf(rule?.level ?: 80) }
    var direction by remember { mutableStateOf(rule?.direction ?: AlertRule.DIRECTION_CHARGING_UP) }
    var soundType by remember {
        mutableStateOf(
            rule?.soundType ?: if (direction == AlertRule.DIRECTION_CHARGING_UP) AlertRule.SOUND_VOICE_CHARGED else AlertRule.SOUND_VOICE_ALERT
        )
    }
    var soundUri by remember { mutableStateOf(rule?.soundUri) }
    var customFileName by remember {
        mutableStateOf(
            rule?.soundUri?.let { uriStr ->
                resolveFileName(context, uriStr)
            }
        )
    }
    var volume by remember { mutableFloatStateOf(rule?.volume ?: 0.8f) }
    var startMs by remember { mutableIntStateOf(rule?.startMs ?: 0) }
    var endMs by remember { mutableIntStateOf(rule?.endMs ?: 0) }
    var isTestingSound by remember { mutableStateOf(false) }

    val totalDurationMs = remember(soundType, soundUri) {
        AudioAlertPlayer.getAudioDurationMs(context, soundType, soundUri).coerceAtLeast(1000)
    }

    val effectiveEndMs = if (endMs in 1..totalDurationMs) endMs else totalDurationMs

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            soundUri = uri.toString()
            soundType = AlertRule.SOUND_CUSTOM
            customFileName = resolveFileName(context, uri.toString())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioAlertPlayer.stop()
        }
    }

    Dialog(
        onDismissRequest = {
            AudioAlertPlayer.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("dialog_add_edit_rule"),
            color = DeepSlate900
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Fixed Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = if (rule == null) "New Battery Alert" else "Edit Battery Alert",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Play an audio chime when battery hits your chosen level",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                HorizontalDivider(color = SurfaceVariantSlate700.copy(alpha = 0.5f))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Direction Selection
                    Text(
                        text = "Trigger Condition",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = direction == AlertRule.DIRECTION_CHARGING_UP,
                        onClick = {
                            direction = AlertRule.DIRECTION_CHARGING_UP
                            if (soundType == AlertRule.SOUND_VOICE_ALERT || soundType == AlertRule.SOUND_ALARM) {
                                soundType = AlertRule.SOUND_VOICE_CHARGED
                            }
                        },
                        label = { Text("⚡ When charging up to", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_direction_charging_up"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldSuccess,
                            containerColor = SurfaceSlate800,
                            labelColor = TextSecondary
                        )
                    )
                    FilterChip(
                        selected = direction == AlertRule.DIRECTION_DRAINING_DOWN,
                        onClick = {
                            direction = AlertRule.DIRECTION_DRAINING_DOWN
                            if (soundType == AlertRule.SOUND_VOICE_CHARGED || soundType == AlertRule.SOUND_ALARM) {
                                soundType = AlertRule.SOUND_VOICE_ALERT
                            }
                        },
                        label = { Text("🔋 When draining down to", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_direction_draining_down"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary,
                            containerColor = SurfaceSlate800,
                            labelColor = TextSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Battery Level Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Battery Threshold",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "$level%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Slider(
                    value = level.toFloat(),
                    onValueChange = { level = it.toInt() },
                    valueRange = 1f..100f,
                    steps = 98,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_battery_level"),
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary,
                        inactiveTrackColor = SurfaceVariantSlate700
                    )
                )

                // Quick percentage pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 20, 50, 80, 100).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (level == preset) CyanPrimary.copy(alpha = 0.2f) else SurfaceSlate800)
                                .clickable { level = preset }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$preset%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (level == preset) CyanPrimary else TextSecondary,
                                    fontWeight = if (level == preset) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sound Choice
                Text(
                    text = "Alert Sound",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = soundType == AlertRule.SOUND_VOICE_CHARGED,
                            onClick = { soundType = AlertRule.SOUND_VOICE_CHARGED },
                            label = { Text("⚡ Full Power Voice", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_sound_VOICE_CHARGED"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSuccess.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldSuccess,
                                containerColor = SurfaceSlate800,
                                labelColor = TextSecondary
                            )
                        )
                        FilterChip(
                            selected = soundType == AlertRule.SOUND_VOICE_ALERT,
                            onClick = { soundType = AlertRule.SOUND_VOICE_ALERT },
                            label = { Text("🎙️ Low Power Voice", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_sound_VOICE_ALERT"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyanPrimary,
                                containerColor = SurfaceSlate800,
                                labelColor = TextSecondary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = soundType == AlertRule.SOUND_ALARM,
                            onClick = { soundType = AlertRule.SOUND_ALARM },
                            label = { Text("⏰ Alarm", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_sound_ALARM"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyanPrimary,
                                containerColor = SurfaceSlate800,
                                labelColor = TextSecondary
                            )
                        )
                        FilterChip(
                            selected = soundType == AlertRule.SOUND_NOTIFICATION,
                            onClick = { soundType = AlertRule.SOUND_NOTIFICATION },
                            label = { Text("🔔 Notification", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_sound_NOTIFICATION"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyanPrimary,
                                containerColor = SurfaceSlate800,
                                labelColor = TextSecondary
                            )
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = soundType == AlertRule.SOUND_CUSTOM,
                            onClick = {
                                soundType = AlertRule.SOUND_CUSTOM
                                if (soundUri == null) {
                                    audioPickerLauncher.launch(arrayOf("audio/*"))
                                }
                            },
                            label = { Text("📁 Custom File...", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chip_sound_CUSTOM"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyanPrimary,
                                containerColor = SurfaceSlate800,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Custom file picker info if Custom is selected
                if (soundType == AlertRule.SOUND_CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceSlate800),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customFileName ?: "No custom file picked",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = if (customFileName != null) "Audio file stored" else "Tap browse to choose an audio file",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                )
                            }
                            OutlinedButton(
                                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                                modifier = Modifier.testTag("btn_browse_audio")
                            ) {
                                Text("Browse", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Volume Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alert Volume",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .testTag("slider_volume"),
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary,
                            inactiveTrackColor = SurfaceVariantSlate700
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Audio Segment Trimming Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Select Audio Part (Plays Once)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    if (startMs > 0 || (endMs > 0 && endMs < totalDurationMs)) {
                        TextButton(
                            onClick = {
                                startMs = 0
                                endMs = 0
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Full Audio", fontSize = 11.sp, color = CyanPrimary)
                        }
                    }
                }

                Text(
                    text = "Pick the start and end point. When triggered, the audio plays this chosen part once.",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceSlate800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Start: ${formatAudioTime(startMs)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyanPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                val lengthMs = (effectiveEndMs - startMs).coerceAtLeast(0)
                                Text(
                                    text = "Clip: ${formatAudioTime(lengthMs)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyanPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = "End: ${formatAudioTime(effectiveEndMs)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Start Position",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                        Slider(
                            value = startMs.toFloat().coerceIn(0f, (effectiveEndMs - 200).toFloat().coerceAtLeast(0f)),
                            onValueChange = {
                                startMs = it.toInt()
                            },
                            valueRange = 0f..(totalDurationMs - 200).toFloat().coerceAtLeast(0f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_audio_start"),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanPrimary,
                                activeTrackColor = CyanPrimary,
                                inactiveTrackColor = SurfaceVariantSlate700
                            )
                        )

                        Text(
                            text = "End Position",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                        Slider(
                            value = effectiveEndMs.toFloat().coerceIn((startMs + 200).toFloat(), totalDurationMs.toFloat()),
                            onValueChange = {
                                endMs = it.toInt()
                            },
                            valueRange = 200f..totalDurationMs.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_audio_end"),
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldSuccess,
                                activeTrackColor = EmeraldSuccess,
                                inactiveTrackColor = SurfaceVariantSlate700
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test Sound Button (Plays selected clip only once)
                Button(
                    onClick = {
                        if (isTestingSound) {
                            AudioAlertPlayer.stop()
                            isTestingSound = false
                        } else {
                            isTestingSound = true
                            AudioAlertPlayer.playSound(
                                context = context,
                                soundType = soundType,
                                soundUriString = soundUri,
                                volume = volume,
                                startMs = startMs,
                                endMs = effectiveEndMs,
                                onStopCallback = {
                                    isTestingSound = false
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_dialog_test_sound"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestingSound) CyanPrimary else SurfaceSlate800,
                        contentColor = if (isTestingSound) DeepSlate900 else CyanPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isTestingSound) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTestingSound) "Playing chosen part once... Tap to stop" else "Test Chosen Audio Part (Plays Once)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                }

                HorizontalDivider(color = SurfaceVariantSlate700.copy(alpha = 0.5f))

                // Fixed Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepSlate900)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            AudioAlertPlayer.stop()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("btn_cancel_alert_rule")
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            AudioAlertPlayer.stop()
                            onSave(
                                rule?.id ?: 0L,
                                level,
                                direction,
                                soundType,
                                soundUri,
                                volume,
                                startMs,
                                if (endMs >= totalDurationMs) 0 else endMs
                            )
                        },
                        modifier = Modifier.testTag("btn_save_alert_rule"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text(
                            text = if (rule == null) "Create Alert" else "Save Changes",
                            color = DeepSlate900,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun resolveFileName(context: android.content.Context, uriString: String): String {
    return try {
        val uri = Uri.parse(uriString)
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        name ?: uri.lastPathSegment ?: "Custom Audio File"
    } catch (e: Exception) {
        "Custom Audio File"
    }
}

private fun formatAudioTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return if (minutes > 0) {
        String.format("%d:%02d.%d", minutes, seconds, tenths)
    } else {
        String.format("%d.%ds", seconds, tenths)
    }
}
