package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HabitEntity
import com.example.data.repository.HabitSortMode
import com.example.ui.viewmodel.KineticViewModel
import com.example.ui.viewmodel.sortHabitCards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitSortScreen(
    viewModel: KineticViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val habitCards by viewModel.habitCards.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val isMomentumMode = sortMode.isMomentum
    val isMomentumAscending = sortMode == HabitSortMode.MOMENTUM_ASCENDING
    val momentumById = remember(habitCards) { habitCards.associate { it.habit.id to it.momentumPercent } }

    var ordered by remember { mutableStateOf<List<HabitEntity>>(emptyList()) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val displayedHabits = if (isMomentumMode) sortHabitCards(habitCards, sortMode).map { it.habit } else ordered

    // Sync from DB when not actively dragging.
    LaunchedEffect(habits) {
        if (draggingIndex == -1) ordered = habits
    }

    val rowHeightDp = 64.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeightDp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sort", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("sort_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("sort_screen")
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "Sort by",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SortModeChip(
                    label = "Manual order",
                    selected = !isMomentumMode,
                    onClick = { viewModel.setSortMode(HabitSortMode.MANUAL) },
                    testTag = "sort_mode_manual",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                SortModeChip(
                    // ↓ = highest first, ↑ = lowest first; re-tapping flips (see helper text).
                    label = if (isMomentumAscending) "Momentum Score ↑" else "Momentum Score ↓",
                    selected = isMomentumMode,
                    onClick = { viewModel.selectMomentumSort() },
                    testTag = "sort_mode_momentum",
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = when {
                    isMomentumAscending -> "Lowest Momentum first — auto-sorted. Tap Momentum Score again for highest first. Order updates as scores change."
                    isMomentumMode -> "Highest Momentum first — auto-sorted. Tap Momentum Score again for lowest first. Order updates as scores change."
                    else -> "Long-press and drag to reorder"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            if (displayedHabits.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No habits to sort", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(displayedHabits, key = { _, habit -> habit.id }) { index, habit ->
                        val isDragging = index == draggingIndex
                        val categoryName = categories.firstOrNull { it.id == habit.categoryId }?.name ?: "General"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeightDp)
                                .padding(vertical = 4.dp)
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .pointerInput(habit.id, ordered.size, isMomentumMode) {
                                    if (isMomentumMode) return@pointerInput
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val steps = (dragOffsetY / rowHeightPx).toInt()
                                            if (steps != 0) {
                                                val from = draggingIndex
                                                val to = (from + steps).coerceIn(0, ordered.lastIndex)
                                                if (to != from) {
                                                    ordered = ordered.toMutableList().apply {
                                                        add(to, removeAt(from))
                                                    }
                                                    draggingIndex = to
                                                    dragOffsetY -= steps * rowHeightPx
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            viewModel.saveHabitOrder(ordered)
                                            draggingIndex = -1
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingIndex = -1
                                            dragOffsetY = 0f
                                            ordered = habits
                                        }
                                    )
                                }
                                .padding(horizontal = 12.dp)
                                .testTag("sort_row_${habit.id}")
                        ) {
                            if (isMomentumMode) {
                                // Auto-sorted: show rank instead of a drag affordance.
                                Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        "#${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder ${habit.title}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    habit.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    categoryName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isMomentumMode) {
                                Text(
                                    "${momentumById[habit.id] ?: 0}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.testTag("sort_momentum_${habit.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortModeChip(label: String, selected: Boolean, onClick: () -> Unit, testTag: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag(testTag)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
