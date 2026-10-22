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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.Fraunces
import com.example.ui.viewmodel.KineticViewModel
import com.example.ui.viewmodel.sortHabitCards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KineticDashboardScreen(
    viewModel: KineticViewModel,
    onOpenHabitDetail: (Long) -> Unit = { viewModel.selectHabitById(it) },
    onOpenEditor: (Long?) -> Unit = { if (it == null) viewModel.openNewHabitEditor() },
    onOpenSettings: () -> Unit = { viewModel.openSettings() },
    onOpenSort: () -> Unit = {},
    onOpenFaqs: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }
    var viewMenuExpanded by remember { mutableStateOf(false) }
    var mainMenuExpanded by remember { mutableStateOf(false) }
    val habitCards by viewModel.habitCards.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val stacks by viewModel.allStacks.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val isStackView by viewModel.isStackView.collectAsStateWithLifecycle()
    val isNotificationSettingsOpen by viewModel.isNotificationSettingsOpen.collectAsStateWithLifecycle()
    val activeTimerHabit by viewModel.activeTimerHabit.collectAsStateWithLifecycle()
    val quickLogTarget by viewModel.quickLogTarget.collectAsStateWithLifecycle()
    val inAppNudges by viewModel.inAppNudges.collectAsStateWithLifecycle()
    val showHeroSection by viewModel.showHeroSection.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val lastArchived by viewModel.lastArchived.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(lastArchived) {
        if (lastArchived != null) {
            val result = snackbarHostState.showSnackbar(message = "Habit archived", actionLabel = "Undo")
            if (result == SnackbarResult.ActionPerformed) viewModel.undoArchive() else viewModel.clearLastArchived()
        }
    }

    val filteredCards = remember(habitCards, selectedCategoryId, sortMode) {
        val byCategory = if (selectedCategoryId == null) habitCards else habitCards.filter { it.habit.categoryId == selectedCategoryId }
        sortHabitCards(byCategory, sortMode)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_orbit_foreground_1792527364716),
                            contentDescription = "Routines",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Routines", fontFamily = Fraunces, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { viewMenuExpanded = true }, modifier = Modifier.testTag("toggle_stack_view_btn")) {
                            Icon(imageVector = if (isStackView) Icons.Default.ViewStream else Icons.AutoMirrored.Filled.ViewList, contentDescription = "View and sort options", tint = if (isStackView) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = viewMenuExpanded, onDismissRequest = { viewMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isStackView) "Flat List" else "Group by Stack") },
                                leadingIcon = { Icon(if (isStackView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewStream, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { viewMenuExpanded = false; viewModel.toggleStackView() },
                                modifier = Modifier.testTag("view_mode_option")
                            )
                            DropdownMenuItem(
                                text = { Text("Sort") },
                                leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { viewMenuExpanded = false; onOpenSort() },
                                modifier = Modifier.testTag("open_sort_option")
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.openNotificationSettings() }, modifier = Modifier.testTag("open_notifications_btn")) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        IconButton(onClick = { mainMenuExpanded = true }, modifier = Modifier.testTag("open_main_menu_btn")) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = mainMenuExpanded, onDismissRequest = { mainMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { mainMenuExpanded = false; onOpenSettings() },
                                modifier = Modifier.testTag("open_settings_btn")
                            )
                            DropdownMenuItem(
                                text = { Text("FAQs") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { mainMenuExpanded = false; onOpenFaqs() },
                                modifier = Modifier.testTag("open_faqs_option")
                            )
                            DropdownMenuItem(
                                text = { Text("About") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { mainMenuExpanded = false; onOpenAbout() },
                                modifier = Modifier.testTag("open_about_option")
                            )
                        }
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
            if (showHeroSection && inAppNudges.isNotEmpty()) {
                item {
                    InAppNudgeHero(
                        nudges = inAppNudges,
                        onActionClick = { nudge -> viewModel.handleNudgeAction(nudge); nudge.targetHabitId?.let(onOpenHabitDetail) },
                        onDismiss = { id -> viewModel.dismissNudge(id) }
                    )
                }
            }

            if (habitCards.isNotEmpty()) {
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
            }

            if (filteredCards.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (habitCards.isEmpty()) {
                                Text(text = "No habits yet", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Tap + to create your first habit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(text = "No habits found in this category", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Tap + to create a habit or switch filter", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                            habit = card.habit, categoryName = card.categoryName, recentDays = card.recentDays, momentumPercent = card.momentumPercent,
                            onCardClick = { viewModel.openHabitDetail(card.habit); onOpenHabitDetail(card.habit.id) },
                            onDayClick = { viewModel.handleDayClick(card.habit, it) },
                            onDayLongClick = { viewModel.openQuickLog(card.habit, it) },
                            onEditClick = { viewModel.openEditHabitEditor(card.habit); onOpenEditor(card.habit.id) },
                            onArchiveClick = { viewModel.archiveHabit(card.habit) },
                            onDeleteClick = { habitToDelete = card.habit }
                        )
                    }
                }
            } else {
                items(filteredCards, key = { it.habit.id }) { card ->
                    HabitCard(
                        habit = card.habit, categoryName = card.categoryName, recentDays = card.recentDays, momentumPercent = card.momentumPercent,
                        onCardClick = { viewModel.openHabitDetail(card.habit); onOpenHabitDetail(card.habit.id) },
                        onDayClick = { viewModel.handleDayClick(card.habit, it) },
                        onDayLongClick = { viewModel.openQuickLog(card.habit, it) },
                        onEditClick = { viewModel.openEditHabitEditor(card.habit); onOpenEditor(card.habit.id) },
                        onArchiveClick = { viewModel.archiveHabit(card.habit) },
                        onDeleteClick = { habitToDelete = card.habit }
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

    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete permanently?") },
            text = { Text("This deletes ${habit.title} and all of its logs. This cannot be undone. Consider archiving instead.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteHabit(habit); habitToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_habit_btn")
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { habitToDelete = null }) { Text("Cancel") } },
            modifier = Modifier.testTag("delete_habit_confirm_dialog")
        )
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
