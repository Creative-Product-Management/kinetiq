package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.HabitEntity
import com.example.data.model.HabitModality
import com.example.data.model.RoutineStackEntity
import com.example.domain.validation.HabitValidator
import com.example.ui.theme.FloorGold
import com.example.ui.theme.KinetiqEmerald
import com.example.ui.theme.KinetiqViolet
import com.example.ui.theme.MissedRed
import com.example.ui.theme.parseHabitColor

private fun formatDecimal(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorSheet(
    habit: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit,
    onDelete: ((HabitEntity) -> Unit)? = null,
    onArchive: ((HabitEntity) -> Unit)? = null,
    categories: List<Category> = emptyList(),
    stacks: List<RoutineStackEntity> = emptyList(),
    defaultFloorPercent: Int = 25
) {
    var title by rememberSaveable(habit?.id) { mutableStateOf(habit?.title ?: "") }
    var description by rememberSaveable(habit?.id) { mutableStateOf(habit?.description ?: "") }
    var cueTrigger by rememberSaveable(habit?.id) { mutableStateOf(habit?.cueTrigger ?: "") }
    var modality by rememberSaveable(habit?.id) { mutableStateOf(habit?.modality ?: HabitModality.YES_NO) }
    var targetValue by rememberSaveable(habit?.id) { mutableStateOf(habit?.targetValue?.let { formatDecimal(it) } ?: "1") }
    var floorValue by rememberSaveable(habit?.id) { mutableStateOf(habit?.floorValue?.let { formatDecimal(it) } ?: "") }
    var unit by rememberSaveable(habit?.id) { mutableStateOf(habit?.unit ?: (habit?.modality ?: HabitModality.YES_NO).defaultUnit) }
    var categoryId by rememberSaveable(habit?.id) { mutableStateOf(habit?.categoryId) }
    var routineStackId by rememberSaveable(habit?.id) { mutableStateOf(habit?.routineStackId) }
    var colorHex by rememberSaveable(habit?.id) { mutableStateOf(habit?.colorHex ?: "#10B981") }
    var iconName by rememberSaveable(habit?.id) { mutableStateOf(habit?.iconName ?: "SelfImprovement") }
    var hasReminder by rememberSaveable(habit?.id) { mutableStateOf(habit?.reminderHour != null) }
    var reminderHour by rememberSaveable(habit?.id) { mutableIntStateOf(habit?.reminderHour ?: 8) }
    var reminderMinute by rememberSaveable(habit?.id) { mutableIntStateOf(habit?.reminderMinute ?: 0) }
    var reminderDaysMask by rememberSaveable(habit?.id) { mutableIntStateOf(habit?.reminderDaysMask ?: 127) }
    var restDaysMask by rememberSaveable(habit?.id) { mutableIntStateOf(habit?.restDaysMask ?: 0) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf(HabitValidator.Result()) }

    // Progressive disclosure: optional sections start expanded only when they already hold values.
    var detailsExpanded by rememberSaveable(habit?.id) { mutableStateOf(habit?.description?.isNotBlank() == true || habit?.cueTrigger?.isNotBlank() == true) }
    var organizeExpanded by rememberSaveable(habit?.id) { mutableStateOf(habit?.categoryId != null || habit?.routineStackId != null) }
    var appearanceExpanded by rememberSaveable(habit?.id) { mutableStateOf(habit != null && (habit.colorHex != "#10B981" || habit.iconName != "SelfImprovement")) }
    var scheduleExpanded by rememberSaveable(habit?.id) { mutableStateOf((habit?.restDaysMask ?: 0) != 0 || habit?.reminderHour != null) }

    val colors = listOf("#10B981", "#8B5CF6", "#F59E0B", "#F43F5E", "#06B6D4", "#EC4899", "#3B82F6")
    val icons = listOf("SelfImprovement", "WaterDrop", "Psychology", "FitnessCenter", "Timer", "Bolt", "Sparkles")
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val activeColor = remember(colorHex) { parseHabitColor(colorHex) }

    val isDirty = remember(title, description, cueTrigger, modality, targetValue, floorValue, unit, categoryId, routineStackId, colorHex, iconName, hasReminder, reminderHour, reminderMinute, reminderDaysMask, restDaysMask) {
        habit == null || title != habit.title || description != habit.description || cueTrigger != habit.cueTrigger ||
            modality != habit.modality || targetValue != formatDecimal(habit.targetValue) ||
            floorValue != (habit.floorValue?.let { formatDecimal(it) } ?: "") || categoryId != habit.categoryId ||
            routineStackId != habit.routineStackId || restDaysMask != habit.restDaysMask
    }
    fun requestDismiss() { if (isDirty) showDiscardConfirm = true else onDismiss() }
    BackHandler { requestDismiss() }

    fun save() {
        val targetNum = targetValue.toDoubleOrNull()
        val floorNum = if (floorValue.isBlank()) null else floorValue.toDoubleOrNull()
        validation = HabitValidator.validate(title, targetNum, floorNum)
        if (modality != HabitModality.YES_NO && floorValue.isNotBlank() && floorNum == null) {
            validation = validation.copy(floorError = "Enter a valid number")
        }
        if (validation.isValid) {
            val newHabit = (habit ?: HabitEntity(title = title)).copy(
                title = title.trim(), description = description, cueTrigger = cueTrigger,
                modality = modality, targetValue = targetNum ?: 1.0, floorValue = floorNum,
                unit = unit.ifBlank { modality.defaultUnit },
                categoryId = categoryId, routineStackId = routineStackId,
                colorHex = colorHex, iconName = iconName,
                reminderHour = if (hasReminder) reminderHour else null,
                reminderMinute = if (hasReminder) reminderMinute else null,
                reminderDaysMask = reminderDaysMask, restDaysMask = restDaysMask
            )
            onSave(newHabit)
        }
    }

    // Input readability: typed text is large/semibold/bright, labels stay small and muted,
    // and fields sit on a subtle filled surface so they read as inputs, not labels.
    val inputTextStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = activeColor,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = activeColor,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        cursorColor = activeColor
    )
    val floorFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = FloorGold,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = FloorGold,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        cursorColor = FloorGold
    )
    val cueFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = KinetiqViolet,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = KinetiqViolet,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        cursorColor = KinetiqViolet
    )

    val selectedCategoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "General"
    val selectedStackName = stacks.firstOrNull { it.id == routineStackId }?.name ?: "No stack"
    val restSummary = daysOfWeek.filterIndexed { i, _ -> ((restDaysMask shr i) and 1) == 1 }
        .joinToString(" ").ifBlank { "No rest days" }
    val reminderSummary = if (hasReminder) String.format("%02d:%02d", reminderHour, reminderMinute) else "Off"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (habit == null) "New Habit" else "Edit Habit", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { requestDismiss() }, modifier = Modifier.testTag("editor_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { requestDismiss() }, modifier = Modifier.testTag("editor_close_btn")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel and discard", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Button(
                    onClick = { save() },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                    modifier = Modifier.fillMaxWidth().testTag("save_habit_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Essentials: what the habit is and how it's tracked.
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g., Morning Breathwork, 2L Water", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                singleLine = true, isError = validation.titleError != null,
                supportingText = validation.titleError?.let { { Text(it, color = MissedRed) } },
                textStyle = inputTextStyle,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("habit_title_input")
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HabitModality.entries.forEach { mod ->
                    val isSelected = modality == mod
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isSelected) activeColor else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                val previous = modality; modality = mod
                                if (unit.isBlank() || unit == previous.defaultUnit) unit = mod.defaultUnit
                                if (mod != HabitModality.YES_NO && floorValue.isBlank()) {
                                    (targetValue.toDoubleOrNull()?.let { it * defaultFloorPercent / 100.0 })?.let { floorValue = formatDecimal(it) }
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp).testTag("modality_tab_${mod.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = mod.displayName, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (modality) {
                    HabitModality.YES_NO -> "Simple daily check-in."
                    HabitModality.NUMERIC -> "Track an amount each day, e.g. 2500 ml of water."
                    HabitModality.TIMER -> "Track time in minutes with the built-in timer."
                    HabitModality.TALLY -> "Count repeats. Each tap adds 1 until the target is met."
                },
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("modality_hint")
            )

            if (modality != HabitModality.YES_NO) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = targetValue, onValueChange = { targetValue = it },
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, isError = validation.targetError != null,
                        supportingText = validation.targetError?.let { { Text(it, color = MissedRed) } },
                        textStyle = inputTextStyle,
                        colors = fieldColors,
                        modifier = Modifier.weight(1f).testTag("target_value_input")
                    )
                    OutlinedTextField(
                        value = floorValue, onValueChange = { floorValue = it },
                        label = { Text("Floor", color = FloorGold) },
                        placeholder = { Text("e.g., 0.5", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, isError = validation.floorError != null,
                        supportingText = validation.floorError?.let { { Text(it, color = MissedRed) } },
                        textStyle = inputTextStyle,
                        colors = floorFieldColors,
                        modifier = Modifier.weight(1f).testTag("floor_value_input")
                    )
                    OutlinedTextField(
                        value = unit, onValueChange = { unit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("mins, ml", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        singleLine = true,
                        textStyle = inputTextStyle,
                        colors = fieldColors,
                        modifier = Modifier.weight(0.9f).testTag("unit_input")
                    )
                }
                Text("Floor = the smallest amount that still counts on a hard day.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(18.dp))

            // Details: description + cue.
            EditorSection(
                title = "Details",
                summary = cueTrigger.ifBlank { description.ifBlank { "Description & cue (optional)" } },
                expanded = detailsExpanded,
                onToggle = { detailsExpanded = !detailsExpanded },
                testTag = "editor_section_details"
            ) {
                OutlinedTextField(
                    value = cueTrigger, onValueChange = { cueTrigger = it },
                    label = { Text("Cue (optional)", color = KinetiqViolet) },
                    placeholder = { Text("e.g., After I pour my morning coffee...", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                    supportingText = { Text("After I [cue], I will do this habit.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    textStyle = inputTextStyle,
                    colors = cueFieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("habit_cue_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Why this habit matters...", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                    textStyle = inputTextStyle,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("habit_description_input")
                )
            }

            // Organize: category + stack.
            EditorSection(
                title = "Organize",
                summary = "$selectedCategoryName · $selectedStackName",
                expanded = organizeExpanded,
                onToggle = { organizeExpanded = !organizeExpanded },
                testTag = "editor_section_organize"
            ) {
                Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                if (categories.isEmpty()) {
                    Text("General", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            val isSelected = categoryId == cat.id
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { categoryId = cat.id }.padding(horizontal = 12.dp, vertical = 6.dp).testTag("category_option_${cat.id}")
                            ) { Text(text = cat.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Routine stack (optional)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (routineStackId == null) activeColor else MaterialTheme.colorScheme.surfaceVariant).clickable { routineStackId = null }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("None", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (routineStackId == null) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    stacks.forEach { stack ->
                        val isSelected = routineStackId == stack.id
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant).clickable { routineStackId = stack.id }.padding(horizontal = 12.dp, vertical = 6.dp).testTag("stack_option_${stack.id}")) {
                            Text(stack.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Appearance: color + icon.
            EditorSection(
                title = "Appearance",
                summary = iconName,
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded },
                testTag = "editor_section_appearance",
                summaryDotColor = activeColor
            ) {
                Text("Color", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { hex ->
                        val c = parseHabitColor(hex)
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(c).border(if (isSelected) 2.5.dp else 0.dp, Color.White, CircleShape).clickable { colorHex = hex })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Icon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.forEach { name ->
                        val isSelected = iconName == name
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant).clickable { iconName = name }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Schedule: rest shields + reminder.
            EditorSection(
                title = "Schedule",
                summary = "$restSummary · Reminder $reminderSummary",
                expanded = scheduleExpanded,
                onToggle = { scheduleExpanded = !scheduleExpanded },
                testTag = "editor_section_schedule"
            ) {
                Text("Rest days (Rest Shield)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Rest days won't penalize your streak or Momentum.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                DayMaskRow(mask = restDaysMask, daysOfWeek = daysOfWeek, selectedColor = Color(0xFF1E3A8A), borderColor = Color(0xFF3B82F6)) { restDaysMask = it }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reminder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = if (hasReminder) String.format("%02d:%02d on selected days", reminderHour, reminderMinute) else "Off", fontSize = 11.sp, color = if (hasReminder) KinetiqEmerald else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = hasReminder, onCheckedChange = { hasReminder = it }, colors = SwitchDefaults.colors(checkedThumbColor = KinetiqEmerald, checkedTrackColor = Color(0xFF0F3A2A)))
                }
                if (hasReminder) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth().testTag("reminder_time_btn")) {
                        Text("Reminder time: ${String.format("%02d:%02d", reminderHour, reminderMinute)}", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Reminder days", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    DayMaskRow(mask = reminderDaysMask, daysOfWeek = daysOfWeek, selectedColor = KinetiqEmerald, borderColor = KinetiqEmerald) { reminderDaysMask = it }
                }
            }

            if (habit != null && (onArchive != null || onDelete != null)) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("MANAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                if (onArchive != null) {
                    OutlinedButton(onClick = { onArchive(habit) }, modifier = Modifier.fillMaxWidth().testTag("archive_habit_btn")) {
                        Icon(Icons.Default.Inventory2, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Archive (Restorable)")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MissedRed),
                        modifier = Modifier.fillMaxWidth().testTag("delete_habit_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MissedRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete", color = MissedRed)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(onClick = { requestDismiss() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.fillMaxWidth().testTag("cancel_habit_btn")) {
                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel Edit", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = reminderHour, initialMinute = reminderMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = { reminderHour = timeState.hour; reminderMinute = timeState.minute; showTimePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes to this habit.") },
            confirmButton = { TextButton(onClick = { showDiscardConfirm = false; onDismiss() }) { Text("Discard", color = MissedRed) } },
            dismissButton = { TextButton(onClick = { showDiscardConfirm = false }) { Text("Keep editing") } }
        )
    }
    if (showDeleteConfirm && habit != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete permanently?") },
            text = { Text("This deletes ${habit.title} and all of its logs. This cannot be undone. Consider archiving instead.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; onDelete(habit) }, colors = ButtonDefaults.buttonColors(containerColor = MissedRed)) { Text("Delete", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

/** Collapsible editor section with a one-line summary of its current values when closed. */
@Composable
private fun EditorSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    summaryDotColor: Color? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 14.dp, vertical = 13.dp)
                .testTag("${testTag}_toggle")
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(10.dp))
            if (summaryDotColor != null) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(summaryDotColor))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                summary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) { content() }
        }
    }
}

@Composable
private fun DayMaskRow(mask: Int, daysOfWeek: List<String>, selectedColor: Color, borderColor: Color, onMaskChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        daysOfWeek.forEachIndexed { index, day ->
            val isSelected = ((mask shr index) and 1) == 1
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, if (isSelected) borderColor else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onMaskChange(mask xor (1 shl index)) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = day.take(1), fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
