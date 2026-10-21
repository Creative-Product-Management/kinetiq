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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitEntity
import com.example.data.model.HabitModality
import com.example.data.model.HabitRecordEntity
import com.example.ui.theme.FloorGold
import com.example.ui.theme.parseHabitColor
import com.example.ui.theme.KinetiqEmerald
import com.example.ui.theme.RestShieldBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayStripItem(
    val dateString: String,
    val dayLetter: String,
    val dayNumber: String,
    val isToday: Boolean,
    val dayOfWeekIso: Int, // 1=Mon..7=Sun
    val record: HabitRecordEntity?
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitCard(
    habit: HabitEntity,
    recentDays: List<DayStripItem>,
    onCardClick: () -> Unit,
    onDayClick: (DayStripItem) -> Unit,
    onDayLongClick: (DayStripItem) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    categoryName: String = "General",
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val habitColor = remember(habit.colorHex) { parseHabitColor(habit.colorHex) }

    val iconVector: ImageVector = when (habit.iconName) {
        "SelfImprovement" -> Icons.Default.SelfImprovement
        "WaterDrop" -> Icons.Default.WaterDrop
        "Psychology" -> Icons.Default.Psychology
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "Timer" -> Icons.Default.Timer
        "Bolt" -> Icons.Default.Bolt
        else -> Icons.Default.Star
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onMenuClick
            )
            .padding(16.dp)
            .testTag("habit_card_${habit.id}")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(habitColor.copy(alpha = 0.15f))
                    .border(1.dp, habitColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = habit.title,
                    tint = habitColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF21262D))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = categoryName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cue Trigger or Modality info
                if (habit.cueTrigger.isNotBlank()) {
                    Text(
                        text = "⚡ Cue: ${habit.cueTrigger}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    val targetText = when (habit.modality) {
                        HabitModality.YES_NO -> "Daily Target"
                        HabitModality.NUMERIC -> "Target: ${habit.targetValue.toInt()} ${habit.unit}"
                        HabitModality.TIMER -> "Target: ${habit.targetValue.toInt()} ${habit.unit}"
                        HabitModality.TALLY -> "Target: ${habit.targetValue.toInt()} ${habit.unit}"
                    }
                    val floorText = if (habit.floorValue != null && habit.floorValue > 0) {
                        " • Floor: ${habit.floorValue.toInt()} ${habit.unit}"
                    } else ""
                    Text(
                        text = targetText + floorText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onMoveUp != null) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Move habit up", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            if (onMoveDown != null) {
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Move habit down", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(48.dp).testTag("habit_menu_${habit.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Edit habit options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7-Day Quick-Action Completion Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            recentDays.forEach { item ->
                val isRestDay = habit.isRestDay(item.dayOfWeekIso)
                val isCompleted = item.record?.isCompleted == true
                val isFloor = item.record?.isFloorSustained == true

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.dayLetter,
                        fontSize = 10.sp,
                        fontWeight = if (item.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (item.isToday) habitColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.dayNumber,
                        fontSize = 9.sp,
                        color = if (item.isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // 44dp visual target inside a labelled, screen-reader friendly control
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .semantics {
                                contentDescription = "${habit.title} ${item.dateString}: " + when {
                                    isCompleted -> "completed"
                                    isFloor -> "floor sustained"
                                    isRestDay -> "rest day"
                                    else -> "not logged"
                                }
                            }
                            .clip(CircleShape)
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
                                shape = CircleShape
                            )
                            .combinedClickable(
                                onClick = { onDayClick(item) },
                                onLongClick = { onDayLongClick(item) }
                            )
                            .testTag("day_btn_${habit.id}_${item.dateString}"),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isCompleted -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            isFloor -> {
                                // Double circle for Floor minimum
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(FloorGold)
                                )
                            }
                            isRestDay -> {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Rest Shield",
                                    tint = RestShieldBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            item.record != null && (item.record.numericValue > 0 || item.record.tallyCount > 0) -> {
                                Text(
                                    text = if (habit.modality == HabitModality.TALLY) "${item.record.tallyCount}" else "${item.record.numericValue.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = habitColor
                                )
                            }
                            else -> {
                                // Empty state
                            }
                        }
                    }
                }
            }
        }
    }
}
