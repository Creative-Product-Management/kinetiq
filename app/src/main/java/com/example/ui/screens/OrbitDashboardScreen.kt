package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.HabitEntity
import com.example.ui.components.ActiveTimerDialog
import com.example.ui.components.HabitCard
import com.example.ui.components.InAppNudgeHero
import com.example.ui.components.NotificationSettingsDialog
import com.example.ui.components.QuickLogDialog
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.Fraunces
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitViolet
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBackground
import com.example.ui.viewmodel.OrbitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitDashboardScreen(
    viewModel: OrbitViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val records by viewModel.allRecords.collectAsStateWithLifecycle()
    val stacks by viewModel.allStacks.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isStackView by viewModel.isStackView.collectAsStateWithLifecycle()
    val selectedHabitForDetail by viewModel.selectedHabitForDetail.collectAsStateWithLifecycle()
    val isEditorOpen by viewModel.isEditorOpen.collectAsStateWithLifecycle()
    val habitForEditor by viewModel.habitForEditor.collectAsStateWithLifecycle()
    val isNotificationSettingsOpen by viewModel.isNotificationSettingsOpen.collectAsStateWithLifecycle()
    val activeTimerHabit by viewModel.activeTimerHabit.collectAsStateWithLifecycle()
    val quickLogTarget by viewModel.quickLogTarget.collectAsStateWithLifecycle()
    val inAppNudges by viewModel.inAppNudges.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()

    // Filter habits by category
    val filteredHabits = remember(habits, selectedCategory) {
        if (selectedCategory == "All") habits
        else habits.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val categories = listOf("All", "Health", "Focus", "Mindfulness", "Fitness")

    // Show Settings screen if open
    if (isSettingsOpen) {
        SettingsScreen(
            viewModel = viewModel,
            onBack = { viewModel.closeSettings() }
        )
        return
    }

    // Show Editor screen if open (takes precedence when editing from Dashboard OR from Detail screen!)
    if (isEditorOpen) {
        HabitEditorSheet(
            habit = habitForEditor,
            onDismiss = { viewModel.closeEditor() },
            onSave = { updatedHabit -> viewModel.saveHabit(updatedHabit) },
            onDelete = { habitToDelete -> viewModel.deleteHabit(habitToDelete) }
        )
        return
    }

    // Show Detail screen if selected
    if (selectedHabitForDetail != null) {
        val habit = selectedHabitForDetail!!
        val habitRecords = records.filter { it.habitId == habit.id }
        HabitDetailAnalyticsScreen(
            habit = habit,
            records = habitRecords,
            onBack = { viewModel.closeHabitDetail() },
            onEditHabit = { viewModel.openEditHabitEditor(habit) },
            onDeleteRecord = { dateStr -> viewModel.clearLog(habit.id, dateStr) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_orbit_foreground_1792527364716),
                            contentDescription = "Kinetiq",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Kinetiq",
                            fontFamily = Fraunces,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleStackView() },
                        modifier = Modifier.testTag("toggle_stack_view_btn")
                    ) {
                        Icon(
                            imageVector = if (isStackView) Icons.Default.ViewStream else Icons.Default.ViewList,
                            contentDescription = "Toggle Stack View",
                            tint = if (isStackView) OrbitViolet else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.openNotificationSettings() },
                        modifier = Modifier.testTag("open_notifications_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.testTag("open_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBackground),
                windowInsets = WindowInsets.statusBars
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewHabitEditor() },
                containerColor = OrbitEmerald,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("create_habit_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = VoidBackground,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // In-App Smart Nudge hero carousel
            if (inAppNudges.isNotEmpty()) {
                item {
                    InAppNudgeHero(
                        nudges = inAppNudges,
                        onActionClick = {
                            // Find first stack habit
                            val firstStackHabit = habits.firstOrNull { it.routineStackId != null }
                            if (firstStackHabit != null) {
                                viewModel.openHabitDetail(firstStackHabit)
                            }
                        },
                        onDismiss = { id -> viewModel.dismissNudge(id) }
                    )
                }
            }

            // Category Horizontal Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val count = if (cat == "All") habits.size else habits.count { it.category.equals(cat, ignoreCase = true) }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) OrbitEmerald else CardSurface)
                                .border(1.dp, if (isSelected) OrbitEmerald else SurfaceBorder, RoundedCornerShape(20.dp))
                                .clickable { viewModel.setCategory(cat) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .testTag("category_chip_$cat")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.Black.copy(alpha = 0.2f) else CardSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Habits Section
            if (filteredHabits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No habits found in this category",
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Tap + to create your first habit or switch filter",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (isStackView) {
                // Grouped by Routine Stacks
                val stackGrouped = filteredHabits.groupBy { it.routineStackId }

                stackGrouped.forEach { (stackId, stackHabits) ->
                    val stack = stacks.firstOrNull { it.id == stackId }

                    item(key = "stack_header_${stackId ?: 0}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = OrbitViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (stack?.name ?: "Independent Habits").uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = OrbitViolet
                            )
                        }
                    }

                    items(stackHabits, key = { it.id }) { habit ->
                        val recentDays = viewModel.getRecent7Days(habit.id, records)
                        HabitCard(
                            habit = habit,
                            recentDays = recentDays,
                            onCardClick = { viewModel.openHabitDetail(habit) },
                            onDayClick = { dayItem -> viewModel.handleDayClick(habit, dayItem) },
                            onDayLongClick = { dayItem -> viewModel.openQuickLog(habit, dayItem) },
                            onMenuClick = { viewModel.openEditHabitEditor(habit) }
                        )
                    }
                }
            } else {
                // Standard Flat List sorted by orderIndex
                items(filteredHabits, key = { it.id }) { habit ->
                    val recentDays = viewModel.getRecent7Days(habit.id, records)
                    HabitCard(
                        habit = habit,
                        recentDays = recentDays,
                        onCardClick = { viewModel.openHabitDetail(habit) },
                        onDayClick = { dayItem -> viewModel.handleDayClick(habit, dayItem) },
                        onDayLongClick = { dayItem -> viewModel.openQuickLog(habit, dayItem) },
                        onMenuClick = { viewModel.openEditHabitEditor(habit) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    // Quick Log Dialog Overlay
    quickLogTarget?.let { (habit, dayItem) ->
        QuickLogDialog(
            habit = habit,
            item = dayItem,
            onDismiss = { viewModel.closeQuickLog() },
            onLogTarget = { viewModel.logTarget(habit, dayItem.dateString) },
            onLogFloor = { viewModel.logFloor(habit, dayItem.dateString) },
            onLogCustomAmount = { amount -> viewModel.logCustomAmount(habit, dayItem.dateString, amount) },
            onLogFriction = { tag, notes -> viewModel.logFriction(habit.id, dayItem.dateString, tag, notes) },
            onClear = { viewModel.clearLog(habit.id, dayItem.dateString) },
            onOpenTimer = {
                viewModel.closeQuickLog()
                viewModel.openTimer(habit)
            }
        )
    }

    // Active Focus Timer Dialog Overlay
    activeTimerHabit?.let { habit ->
        ActiveTimerDialog(
            habit = habit,
            onDismiss = { viewModel.closeTimer() },
            onFinishTimer = { durationSec -> viewModel.finishTimerSession(habit, durationSec) }
        )
    }

    // Notification Settings Dialog Overlay
    if (isNotificationSettingsOpen) {
        NotificationSettingsDialog(
            onDismiss = { viewModel.closeNotificationSettings() }
        )
    }
}
