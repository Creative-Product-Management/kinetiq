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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    categoryName: String = "General",
    momentumPercent: Int = 0
) {
    val habitColor = remember(habit.colorHex) { parseHabitColor(habit.colorHex) }
    var menuExpanded by remember { mutableStateOf(false) }

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
                onLongClick = { menuExpanded = true }
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

            Text(
                text = habit.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Kinetiq Momentum badge, left of the overflow menu
            val momentumColor = when {
                momentumPercent >= 70 -> com.example.ui.theme.KinetiqEmerald
                momentumPercent >= 40 -> com.example.ui.theme.FloorGold
                else -> com.example.ui.theme.KinetiqViolet
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(momentumColor.copy(alpha = 0.14f))
                    .border(1.dp, momentumColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .semantics { contentDescription = "Momentum $momentumPercent percent" }
                    .testTag("habit_momentum_${habit.id}")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = momentumColor, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text("$momentumPercent%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = momentumColor)
            }
            Spacer(Modifier.width(2.dp))

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(48.dp).testTag("habit_menu_${habit.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Habit options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = { menuExpanded = false; onEditClick() },
                        modifier = Modifier.testTag("habit_menu_edit_${habit.id}")
                    )
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = { menuExpanded = false; onArchiveClick() },
                        modifier = Modifier.testTag("habit_menu_archive_${habit.id}")
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                        onClick = { menuExpanded = false; onDeleteClick() },
                        modifier = Modifier.testTag("habit_menu_delete_${habit.id}")
                    )
                }
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

        // Footer: Category (left) · Cue or Target/Floor (right)
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().testTag("habit_card_footer_${habit.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = categoryName,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            val footerRight = if (habit.cueTrigger.isNotBlank()) {
                "⚡ ${habit.cueTrigger}"
            } else {
                val targetText = when (habit.modality) {
                    HabitModality.YES_NO -> "Daily Target"
                    else -> "Target ${formatHabitValue(habit.targetValue)} ${habit.unit}".trim()
                }
                val floorText = if (habit.floorValue != null && habit.floorValue > 0) {
                    " · Floor ${formatHabitValue(habit.floorValue)} ${habit.unit}".trimEnd()
                } else ""
                targetText + floorText
            }
            Text(
                text = footerRight,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatHabitValue(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
