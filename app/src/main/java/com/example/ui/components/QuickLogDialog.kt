package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitEntity
import com.example.data.model.HabitModality
import com.example.data.model.HabitRecordEntity
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.FloorGold
import com.example.ui.theme.MissedRed
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun QuickLogDialog(
    habit: HabitEntity,
    item: DayStripItem,
    onDismiss: () -> Unit,
    onLogTarget: () -> Unit,
    onLogFloor: () -> Unit,
    onLogCustomAmount: (Double) -> Unit,
    onLogFriction: (String, String) -> Unit,
    onClear: () -> Unit,
    onOpenTimer: () -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customAmountText by remember {
        mutableStateOf(item.record?.numericValue?.let { if (it > 0) it.toInt().toString() else "" } ?: "")
    }

    var showFrictionSelector by remember { mutableStateOf(false) }
    var selectedFrictionTag by remember { mutableStateOf(item.record?.frictionTag ?: "") }
    var frictionNotesText by remember { mutableStateOf(item.record?.notes ?: "") }

    val habitColor = remember(habit.colorHex) {
        try { Color(android.graphics.Color.parseColor(habit.colorHex)) } catch (_: Exception) { OrbitEmerald }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Column {
                Text(
                    text = habit.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Logging for ${item.dateString}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showCustomInput) {
                    Text(
                        text = "Enter logged ${if (habit.unit.isNotBlank()) habit.unit else "amount"}:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = habitColor,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_amount_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val amount = customAmountText.toDoubleOrNull() ?: 0.0
                            onLogCustomAmount(amount)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = habitColor),
                        modifier = Modifier.fillMaxWidth().testTag("save_custom_amount_btn")
                    ) {
                        Text("Save Amount", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else if (showFrictionSelector) {
                    Text(
                        text = "What was the main obstacle today?",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val frictionOptions = listOf("Low Energy", "Time Crunch", "Travel / Away", "Distraction")

                    frictionOptions.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { tag ->
                                val isSelected = selectedFrictionTag == tag
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MissedRed.copy(alpha = 0.25f) else CardSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) MissedRed else SurfaceBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedFrictionTag = tag }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = frictionNotesText,
                        onValueChange = { frictionNotesText = it },
                        placeholder = { Text("Optional reflection note...", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MissedRed,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (selectedFrictionTag.isNotBlank()) {
                                onLogFriction(selectedFrictionTag, frictionNotesText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MissedRed),
                        modifier = Modifier.fillMaxWidth().testTag("save_friction_btn")
                    ) {
                        Text("Log Obstacle & Learn", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Standard Options List
                    ActionChoiceRow(
                        icon = Icons.Default.CheckCircle,
                        iconTint = habitColor,
                        title = "Log Target",
                        subtitle = "Full target: ${habit.targetValue.toInt()} ${habit.unit}",
                        onClick = onLogTarget
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val floorVal = habit.floorValue ?: (habit.targetValue * 0.25)
                    ActionChoiceRow(
                        icon = Icons.Default.Shield,
                        iconTint = FloorGold,
                        title = "Sustain Floor (Minimum)",
                        subtitle = "Never zero out: ${floorVal.toInt()} ${habit.unit}",
                        onClick = onLogFloor
                    )

                    if (habit.modality == HabitModality.TIMER) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ActionChoiceRow(
                            icon = Icons.Default.ElectricBolt,
                            iconTint = habitColor,
                            title = "Launch Active Timer",
                            subtitle = "Ambient focus countdown & stopwatch",
                            onClick = onOpenTimer
                        )
                    }

                    if (habit.modality == HabitModality.NUMERIC || habit.modality == HabitModality.TALLY) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ActionChoiceRow(
                            icon = Icons.Default.ElectricBolt,
                            iconTint = TextPrimary,
                            title = "Custom Amount...",
                            subtitle = "Log exact numbers or partial reps",
                            onClick = { showCustomInput = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ActionChoiceRow(
                        icon = Icons.Default.MoodBad,
                        iconTint = MissedRed,
                        title = "Log Blocker / Friction",
                        subtitle = "Identify why it was skipped for analytics",
                        onClick = { showFrictionSelector = true }
                    )

                    if (item.record != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ActionChoiceRow(
                            icon = Icons.Default.Delete,
                            iconTint = TextMuted,
                            title = "Clear Entry",
                            subtitle = "Remove record for this date",
                            onClick = onClear
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ActionChoiceRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardSurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
