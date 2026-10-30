package com.example.ui.alerts

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlertRule
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepSlate900
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SurfaceSlate800
import com.example.ui.theme.SurfaceVariantSlate700
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertRuleSwipeableCard(
    rule: AlertRule,
    isPlaying: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onTestSound: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSwipeDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> RoseDanger.copy(alpha = 0.85f)
                    else -> SurfaceVariantSlate700
                },
                label = "DismissBackground"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Rule",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) {
        AlertRuleCard(
            rule = rule,
            isPlaying = isPlaying,
            onTap = onTap,
            onLongPress = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress()
            },
            onToggle = onToggle,
            onTestSound = onTestSound
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlertRuleCard(
    rule: AlertRule,
    isPlaying: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onTestSound: () -> Unit
) {
    val isChargingUp = rule.direction == AlertRule.DIRECTION_CHARGING_UP
    val accentColor = if (isChargingUp) EmeraldSuccess else CyanPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .testTag("rule_card_${rule.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled) SurfaceSlate800 else SurfaceSlate800.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Icon, Direction label, Threshold, Enabled switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle with icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (rule.enabled) accentColor.copy(alpha = 0.18f) else SurfaceVariantSlate700),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isChargingUp) Icons.Default.Bolt else Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = if (rule.enabled) accentColor else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Threshold & Direction Text
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${rule.level}%",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (rule.enabled) TextPrimary else TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (rule.enabled) accentColor.copy(alpha = 0.15f) else SurfaceVariantSlate700)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = rule.directionLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (rule.enabled) accentColor else TextMuted,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isChargingUp) "Alerts when charger reaches ${rule.level}%" else "Alerts when battery drops to ${rule.level}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (rule.enabled) TextSecondary else TextMuted,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Enabled Toggle Switch
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("switch_rule_${rule.id}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DeepSlate900,
                        checkedTrackColor = CyanPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceVariantSlate700
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom row: Sound tag, Volume tag, Test sound button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sound pill
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantSlate700.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (rule.soundType == AlertRule.SOUND_VOICE_CHARGED || rule.soundType == AlertRule.SOUND_VOICE_ALERT) {
                                    Icons.Default.Mic
                                } else {
                                    Icons.Default.MusicNote
                                },
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = rule.soundLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextPrimary,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Volume pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantSlate700.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${(rule.volume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    if (rule.isTrimmed) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            val startSec = rule.startMs / 1000f
                            val endSec = if (rule.endMs > 0) rule.endMs / 1000f else null
                            val trimText = if (endSec != null) "✂️ %.1fs-%.1fs".format(startSec, endSec) else "✂️ from %.1fs".format(startSec)
                            Text(
                                text = trimText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyanPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Test sound button
                OutlinedButton(
                    onClick = onTestSound,
                    modifier = Modifier.testTag("btn_test_sound_${rule.id}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isPlaying) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = if (isPlaying) CyanPrimary else TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "Stop" else "Test Sound",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
