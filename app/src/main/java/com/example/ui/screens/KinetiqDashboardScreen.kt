package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ViewList
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.ActiveTimerDialog
import com.example.ui.components.HabitCard
import com.example.ui.components.InAppNudgeHero
import com.example.ui.components.NotificationSettingsDialog
import com.example.ui.components.QuickLogDialog
import com.example.ui.theme.Fraunces
import com.example.ui.viewmodel.KinetiqViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KinetiqDashboardScreen(
    viewModel: KinetiqViewModel,
    onOpenHabitDetail: (Long) -> Unit = { viewModel.selectHabitById(it) },
    onOpenEditor: (Long?) -> Unit = { if (it == null) viewModel.openNewHabitEditor() },
    onOpenSettings: () -> Unit = { viewModel.openSettings() },
    modifier: Modifier = Modifier
) {
    val habitCards by viewModel.habitCards.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val stacks by viewModel.allStacks.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val isStackView by viewModel.isStackView.collectAsStateWithLifecycle()
    val isNotificationSettingsOpen by viewModel.isNotificationSettingsOpen.collectAsStateWithLifecycle()
    val activeTimerHabit by viewModel.activeTimerHabit.collectAsStateWithLifecycle()
    val quickLogTarget by viewModel.quickLogTarget.collectAsStateWithLifecycle()
    val inAppNudges by viewModel.inAppNudges.collectAsStateWithLifecycle()
    val lastArchived by viewModel.lastArchived.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(lastArchived) {
        if (lastArchived != null) {
            val result = snackbarHostState.showSnackbar(message = "Habit archived", actionLabel = "Undo")
            if (result == SnackbarResult.ActionPerformed) viewModel.undoArchive() else viewModel.clearLastArchived()
        }
    }

    val filteredCards = remember(habitCards, selectedCategoryId) {
        if (selectedCategoryId == null) habitCards else habitCards.filter { it.habit.categoryId == selectedCategoryId }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_orbit_foreground_1792527364716),
                            contentDescription = "Kinetiq",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Kinetiq", fontFamily = Fraunces, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleStackView() }, modifier = Modifier.testTag("toggle_stack_view_btn")) {
                        Icon(imageVector = if (isStackView) Icons.Default.ViewStream else Icons.AutoMirrored.Filled.ViewList, contentDescription = "Toggle Stack View", tint = if (isStackView) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.openNotificationSettings() }, modifier = Modifier.testTag("open_notifications_btn")) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("open_settings_btn")) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets.statusBars
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenEditor(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.navigationBarsPadding().testTag("create_habit_fab")
            ) { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit", modifier = Modifier.size(28.dp)) }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (inAppNudges.isNotEmpty()) {
                item {
                    InAppNudgeHero(
                        nudges = inAppNudges,
                        onActionClick = { /* resolved below per-nudge via hero's single action; use first nudge target */ inAppNudges.firstOrNull()?.let { viewModel.handleNudgeAction(it); it.targetHabitId?.let(onOpenHabitDetail) } },
                        onDismiss = { id -> viewModel.dismissNudge(id) }
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip(label = "All", count = habitCards.size, isSelected = selectedCategoryId == null) { viewModel.setCategoryId(null) }
                    categories.forEach { cat ->
                        CategoryChip(
                            label = cat.name,
                            count = habitCards.count { it.habit.categoryId == cat.id },
                            isSelected = selectedCategoryId == cat.id
                        ) { viewModel.setCategoryId(cat.id) }
                    }
                }
            }

            if (filteredCards.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "No habits found in this category", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Tap + to create your first habit or switch filter", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (isStackView) {
                val grouped = filteredCards.groupBy { it.habit.routineStackId }
                grouped.forEach { (stackId, stackCards) ->
                    val stack = stacks.firstOrNull { it.id == stackId }
                    item(key = "stack_header_${stackId ?: 0}") {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = (stack?.name ?: "Independent Habits").uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    items(stackCards, key = { it.habit.id }) { card ->
                        HabitCard(
                            habit = card.habit, categoryName = card.categoryName, recentDays = card.recentDays,
                            onCardClick = { viewModel.openHabitDetail(card.habit); onOpenHabitDetail(card.habit.id) },
                            onDayClick = { viewModel.handleDayClick(card.habit, it) },
                            onDayLongClick = { viewModel.openQuickLog(card.habit, it) },
                            onMenuClick = { viewModel.openEditHabitEditor(card.habit); onOpenEditor(card.habit.id) },
                            onMoveUp = { viewModel.moveHabit(card.habit, -1) },
                            onMoveDown = { viewModel.moveHabit(card.habit, 1) }
                        )
                    }
                }
            } else {
                items(filteredCards, key = { it.habit.id }) { card ->
                    HabitCard(
                        habit = card.habit, categoryName = card.categoryName, recentDays = card.recentDays,
                        onCardClick = { viewModel.openHabitDetail(card.habit); onOpenHabitDetail(card.habit.id) },
                        onDayClick = { viewModel.handleDayClick(card.habit, it) },
                        onDayLongClick = { viewModel.openQuickLog(card.habit, it) },
                        onMenuClick = { viewModel.openEditHabitEditor(card.habit); onOpenEditor(card.habit.id) },
                        onMoveUp = { viewModel.moveHabit(card.habit, -1) },
                        onMoveDown = { viewModel.moveHabit(card.habit, 1) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    quickLogTarget?.let { (habit, dayItem) ->
        QuickLogDialog(
            habit = habit, item = dayItem,
            onDismiss = { viewModel.closeQuickLog() },
            onLogTarget = { viewModel.logTarget(habit, dayItem.dateString) },
            onLogFloor = { viewModel.logFloor(habit, dayItem.dateString) },
            onLogCustomAmount = { amount -> viewModel.logCustomAmount(habit, dayItem.dateString, amount) },
            onLogFriction = { tag, notes -> viewModel.logFriction(habit.id, dayItem.dateString, tag, notes) },
            onClear = { viewModel.clearLog(habit.id, dayItem.dateString) },
            onOpenTimer = { viewModel.closeQuickLog(); viewModel.openTimer(habit) }
        )
    }

    activeTimerHabit?.let { habit ->
        ActiveTimerDialog(habit = habit, onDismiss = { viewModel.closeTimer() }, onFinishTimer = { durationSec -> viewModel.finishTimerSession(habit, durationSec) })
    }

    if (isNotificationSettingsOpen) {
        NotificationSettingsDialog(onDismiss = { viewModel.closeNotificationSettings() }, onSendTestNotification = { viewModel.sendTestNotification() })
    }
}

@Composable
private fun CategoryChip(label: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) primary else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isSelected) primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("category_chip_$label")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(if (isSelected) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(text = "$count", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
