package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitEntity
import com.example.data.model.HabitModality
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.FloorGold
import com.example.ui.theme.MissedRed
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitViolet
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorSheet(
    habit: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit,
    onDelete: ((HabitEntity) -> Unit)? = null
) {
    // Enable system back gesture navigation
    BackHandler { onDismiss() }

    var title by remember { mutableStateOf(habit?.title ?: "") }
    var description by remember { mutableStateOf(habit?.description ?: "") }
    var cueTrigger by remember { mutableStateOf(habit?.cueTrigger ?: "") }
    var modality by remember { mutableStateOf(habit?.modality ?: HabitModality.YES_NO) }
    var targetValue by remember {
        mutableStateOf(habit?.targetValue?.let { if (it > 0) it.toInt().toString() else "1" } ?: "1")
    }
    var floorValue by remember {
        mutableStateOf(habit?.floorValue?.let { if (it > 0) it.toInt().toString() else "" } ?: "")
    }
    var unit by remember { mutableStateOf(habit?.unit ?: modality.defaultUnit) }
    var category by remember { mutableStateOf(habit?.category ?: "Health") }
    var colorHex by remember { mutableStateOf(habit?.colorHex ?: "#10B981") }
    var iconName by remember { mutableStateOf(habit?.iconName ?: "SelfImprovement") }

    var hasReminder by remember { mutableStateOf(habit?.reminderHour != null) }
    var reminderHour by remember { mutableIntStateOf(habit?.reminderHour ?: 8) }
    var reminderMinute by remember { mutableIntStateOf(habit?.reminderMinute ?: 0) }

    var restDaysMask by remember { mutableIntStateOf(habit?.restDaysMask ?: 0) }

    val categories = listOf("Health", "Focus", "Mindfulness", "Fitness", "Routine", "Learning")
    val colors = listOf("#10B981", "#8B5CF6", "#F59E0B", "#F43F5E", "#06B6D4", "#EC4899", "#3B82F6")
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val activeColor = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { OrbitEmerald }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (habit == null) "Create New Habit" else "Edit Habit",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to home screen",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("editor_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel and discard",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBackground),
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = VoidBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Title Input
            Text("HABIT TITLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g., Morning Breathwork, 2L Water", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeColor,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("habit_title_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Habit Stacking Cue (James Clear / BJ Fogg)
            Text("HABIT STACKING ANCHOR CUE (OPTIONAL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrbitViolet)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Anchor your new habit to an existing routine: 'After I [Current Cue], I will [This Habit]'",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = cueTrigger,
                onValueChange = { cueTrigger = it },
                placeholder = { Text("e.g., After I pour my morning coffee...", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrbitViolet,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth().testTag("habit_cue_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Modality Selector
            Text("TRACKING MODALITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitModality.values().forEach { mod ->
                    val isSelected = modality == mod
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else CardSurface)
                            .border(1.dp, if (isSelected) activeColor else SurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                modality = mod
                                if (unit.isBlank() || unit == modality.defaultUnit) {
                                    unit = mod.defaultUnit
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mod.displayName,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Targets & Elastic Floor
            if (modality != HabitModality.YES_NO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Target Value
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TARGET GOAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = targetValue,
                            onValueChange = { targetValue = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeColor,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("target_value_input")
                        )
                    }

                    // Elastic Floor (Minimum Viable Habit)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FLOOR (MINIMUM)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FloorGold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = floorValue,
                            onValueChange = { floorValue = it },
                            placeholder = { Text("e.g., 20%", color = TextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FloorGold,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("floor_value_input")
                        )
                    }

                    // Unit
                    Column(modifier = Modifier.weight(0.9f)) {
                        Text("UNIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            placeholder = { Text("mins, ml", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeColor,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("unit_input")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Category Chips
            Text("CATEGORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(4).forEach { cat ->
                    val isSelected = category == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) activeColor else CardSurfaceElevated)
                            .clickable { category = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.Black else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Presets
            Text("COLOR ACCENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colors.forEach { hex ->
                    val c = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = colorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(if (isSelected) 2.5.dp else 0.dp, Color.White, CircleShape)
                            .clickable { colorHex = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scheduled Rest Day Shields
            Text("SCHEDULED REST SHIELDS (IMMUNE TO DECAY)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Designated recovery days that will not penalize your streak or momentum score.",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    val isRest = ((restDaysMask shr index) and 1) == 1
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isRest) Color(0xFF1E3A8A) else CardSurfaceElevated)
                            .border(1.dp, if (isRest) Color(0xFF3B82F6) else SurfaceBorder, CircleShape)
                            .clickable {
                                restDaysMask = restDaysMask xor (1 shl index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.take(1),
                            fontSize = 11.sp,
                            fontWeight = if (isRest) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRest) Color.White else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder Toggle & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("DAILY REMINDER NOTIFICATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(
                        text = if (hasReminder) String.format("%02d:%02d daily", reminderHour, reminderMinute) else "Disabled",
                        fontSize = 12.sp,
                        color = if (hasReminder) OrbitEmerald else TextSecondary
                    )
                }
                Switch(
                    checked = hasReminder,
                    onCheckedChange = { hasReminder = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = OrbitEmerald, checkedTrackColor = Color(0xFF0F3A2A))
                )
            }

            if (hasReminder) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { reminderHour = (reminderHour + 1) % 24 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hour: ${String.format("%02d", reminderHour)}", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = { reminderMinute = (reminderMinute + 15) % 60 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Min: ${String.format("%02d", reminderMinute)}", color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save & Delete
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val targetNum = targetValue.toDoubleOrNull() ?: 1.0
                        val floorNum = floorValue.toDoubleOrNull()
                        val newHabit = (habit ?: HabitEntity(title = title)).copy(
                            title = title,
                            description = description,
                            cueTrigger = cueTrigger,
                            modality = modality,
                            targetValue = targetNum,
                            floorValue = floorNum,
                            unit = unit,
                            category = category,
                            colorHex = colorHex,
                            iconName = iconName,
                            reminderHour = if (hasReminder) reminderHour else null,
                            reminderMinute = if (hasReminder) reminderMinute else null,
                            restDaysMask = restDaysMask
                        )
                        onSave(newHabit)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                modifier = Modifier.fillMaxWidth().testTag("save_habit_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Habit", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (habit != null && onDelete != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onDelete(habit) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MissedRed),
                    modifier = Modifier.fillMaxWidth().testTag("delete_habit_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MissedRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Habit & Logs", color = MissedRed)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier.fillMaxWidth().testTag("cancel_habit_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel & Discard", color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
