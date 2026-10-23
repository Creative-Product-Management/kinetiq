package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitEntity
import com.example.data.model.HabitModality
import com.example.data.model.HabitRecordEntity
import com.example.ui.theme.FloorGold
import com.example.ui.theme.habitIconVector
import com.example.ui.theme.parseHabitColor
import com.example.ui.theme.RestShieldBlue

data class DayStripItem(
    val dateString: String,
    val dayLetter: String,
    val dayNumber: String,
    val isToday: Boolean,
    val dayOfWeekIso: Int, // 1=Mon..7=Sun
    val record: HabitRecordEntity?
)

val DAY_CELL_SIZE = 26.dp
private val DAY_CELL_RADIUS = 6.dp
val DAY_CELL_SHAPE = RoundedCornerShape(DAY_CELL_RADIUS)

@Composable
fun HabitMomentumBadge(habitId: Long, momentumPercent: Int, modifier: Modifier = Modifier) {
    val momentumColor = when {
        momentumPercent >= 70 -> com.example.ui.theme.KineticEmerald
        momentumPercent >= 40 -> com.example.ui.theme.FloorGold
        else -> com.example.ui.theme.KineticViolet
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(momentumColor.copy(alpha = 0.14f))
            .border(1.dp, momentumColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .semantics { contentDescription = "Momentum $momentumPercent percent" }
            .testTag("habit_momentum_$habitId")
    ) {
        Icon(Icons.Default.Bolt, contentDescription = null, tint = momentumColor, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(2.dp))
        Text("$momentumPercent%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = momentumColor)
    }
}

@Composable
fun HabitOverflowMenu(
    habitId: Long,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box {
        IconButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.size(32.dp).testTag("habit_menu_$habitId")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Habit options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { onExpandedChange(false); onEditClick() },
                modifier = Modifier.testTag("habit_menu_edit_$habitId")
            )
            DropdownMenuItem(
                text = { Text("Archive") },
                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { onExpandedChange(false); onArchiveClick() },
                modifier = Modifier.testTag("habit_menu_archive_$habitId")
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                onClick = { onExpandedChange(false); onDeleteClick() },
                modifier = Modifier.testTag("habit_menu_delete_$habitId")
            )
        }
    }
}

/** Rounded day log cell shared by Card and Table layouts. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayLogCell(
    habit: HabitEntity,
    item: DayStripItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColor = remember(habit.colorHex) { parseHabitColor(habit.colorHex) }
    val isRestDay = habit.isRestDay(item.dayOfWeekIso)
    val isCompleted = item.record?.isCompleted == true
    val isFloor = item.record?.isFloorSustained == true

    Box(
        modifier = modifier
            .size(DAY_CELL_SIZE)
            .semantics {
                contentDescription = "${habit.title} ${item.dateString}: " + when {
                    isCompleted -> "completed"
                    isFloor -> "floor sustained"
                    isRestDay -> "rest day"
                    else -> "not logged"
                }
            }
            .clip(DAY_CELL_SHAPE)
            .background(
                when {
                    isCompleted -> habitColor
                    isFloor -> FloorGold.copy(alpha = 0.2f)
                    isRestDay -> RestShieldBlue.copy(alpha = 0.15f)
                    item.isToday -> Color(0xFF222B3D)
                    else -> Color(0xFF19202D)
                }
            )
            .border(
                width = if (item.isToday) 1.5.dp else 1.dp,
                color = when {
                    isCompleted -> habitColor
                    isFloor -> FloorGold
                    isRestDay -> RestShieldBlue.copy(alpha = 0.6f)
                    item.isToday -> MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
                shape = DAY_CELL_SHAPE
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("day_btn_${habit.id}_${item.dateString}"),
        contentAlignment = Alignment.Center
    ) {
        when {
            isCompleted -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
            isFloor -> {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(FloorGold)
                )
            }
            isRestDay -> {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Rest Shield",
                    tint = RestShieldBlue,
                    modifier = Modifier.size(11.dp)
                )
            }
            item.record != null && (item.record.numericValue > 0 || item.record.tallyCount > 0) -> {
                Text(
                    text = if (habit.modality == HabitModality.TALLY) "${item.record.tallyCount}" else "${item.record.numericValue.toInt()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = habitColor,
                    style = LocalTextStyle.current.copy(
                        lineHeight = 9.sp,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitCard(
    habit: HabitEntity,
    recentDays: List<DayStripItem>,
    onCardClick: () -> Unit,
    onDayClick: (DayStripItem) -> Unit,
    onDayLongClick: (DayStripItem) -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    momentumPercent: Int = 0
) {
    val habitColor = remember(habit.colorHex) { parseHabitColor(habit.colorHex) }
    var menuExpanded by remember { mutableStateOf(false) }
    val iconVector = habitIconVector(habit.iconName)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = { menuExpanded = true }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("habit_card_${habit.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(habitColor.copy(alpha = 0.15f))
                    .border(1.dp, habitColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = habit.title,
                    tint = habitColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = habit.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            HabitMomentumBadge(habitId = habit.id, momentumPercent = momentumPercent)
            HabitOverflowMenu(
                habitId = habit.id,
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                onEditClick = onEditClick,
                onArchiveClick = onArchiveClick,
                onDeleteClick = onDeleteClick
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            recentDays.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.dayLetter,
                        fontSize = 9.sp,
                        lineHeight = 9.sp,
                        fontWeight = if (item.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (item.isToday) habitColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = item.dayNumber,
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        color = if (item.isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DayLogCell(
                        habit = habit,
                        item = item,
                        onClick = { onDayClick(item) },
                        onLongClick = { onDayLongClick(item) }
                    )
                }
            }
        }
    }
}
