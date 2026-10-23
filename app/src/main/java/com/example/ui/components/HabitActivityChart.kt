package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.parseHabitColor
import com.example.ui.viewmodel.HabitCardUi

val ACTIVITY_CELL_SIZE = 20.dp
val ACTIVITY_CELL_SPACING = 4.dp

/** Minimum stack height per date so empty days still show a full placeholder column. */
const val ACTIVITY_MIN_STACK_ROWS = 5

/** Minimum height the vertical ACTIVITY label (8 tight-metric letters) needs. */
val ACTIVITY_LABEL_MIN_HEIGHT = 96.dp

/** Stack height for a date column: one slot per habit, at least [ACTIVITY_MIN_STACK_ROWS]. */
fun activityStackRows(habitCount: Int): Int = maxOf(ACTIVITY_MIN_STACK_ROWS, habitCount)

/** One filled square: a habit logged on a date, in the habit's own colour. */
data class ActivitySquare(
    val habitId: Long,
    val habitTitle: String,
    val colorHex: String,
    val recordedAt: Long,
    val isFloor: Boolean
)

/**
 * One date column of the stacked activity chart. [squares] are in stacking
 * order, earliest-logged first (bottom of the stack).
 */
data class ActivityDayColumn(
    val dateString: String,
    val dayLetter: String,
    val dayNumber: String,
    val isToday: Boolean,
    val squares: List<ActivitySquare>
)

/**
 * Builds the stacked columns from the visible cards: for each of the 14
 * activity dates, every habit logged that day (target or floor) contributes
 * one square, ordered by when it was logged (`recordedAt`, earliest first;
 * ties keep card order). Habits with only partial/below-floor progress and
 * friction-only records do not fill a square.
 */
fun buildActivityColumns(cards: List<HabitCardUi>): List<ActivityDayColumn> {
    val dates = cards.firstOrNull()?.activityDays.orEmpty()
    if (dates.isEmpty()) return emptyList()
    return dates.indices.map { index ->
        val dateItem = dates[index]
        val squares = cards.mapNotNull { card ->
            val record = card.activityDays.getOrNull(index)?.record ?: return@mapNotNull null
            if (!record.isCompleted && !record.isFloorSustained) return@mapNotNull null
            ActivitySquare(
                habitId = card.habit.id,
                habitTitle = card.habit.title,
                colorHex = card.habit.colorHex,
                recordedAt = record.recordedAt,
                isFloor = record.isFloorSustained && !record.isCompleted
            )
        }.sortedBy { it.recordedAt }
        ActivityDayColumn(
            dateString = dateItem.dateString,
            dayLetter = dateItem.dayLetter,
            dayNumber = dateItem.dayNumber,
            isToday = dateItem.isToday,
            squares = squares
        )
    }
}

/** Maximum days the chart will show, however wide the screen is. */
const val ACTIVITY_MAX_DAYS = 30

/**
 * How many date columns fit in [chartInnerWidthDp] of chart content width,
 * reserving room for the vertical ACTIVITY label. Capped at
 * [ACTIVITY_MAX_DAYS]; the most recent days are shown.
 */
fun activityColumnCount(chartInnerWidthDp: Int): Int {
    val labelReserved = 20 // 14dp label + 6dp gap
    val perColumn = 24 // 20dp cell + 4dp spacing
    return ((chartInnerWidthDp - labelReserved + ACTIVITY_CELL_SPACING.value.toInt()) / perColumn)
        .coerceIn(1, ACTIVITY_MAX_DAYS)
}

/**
 * Stacked activity overview for Home: dates run along the bottom (X-axis
 * shows weekday letter + date number under each column; as many recent
 * days as fit the screen, up to 30, today at the right), and each date's
 * logged habits fill squares stacked bottom-up in the order they were
 * logged. A square's habit is identified by its colour. Display-only;
 * logging happens on the habit cards below.
 * [onDismiss] permanently hides the chart (same as turning it off in Settings).
 */
@Composable
fun HabitActivityChart(
    rows: List<HabitCardUi>,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    if (rows.isEmpty()) return
    val columns = buildActivityColumns(rows)
    if (columns.isEmpty()) return
    val stackRows = activityStackRows(rows.size)
    // Chart height is at least the minimum the ACTIVITY label needs to display.
    val stackHeight = maxOf((ACTIVITY_CELL_SIZE + ACTIVITY_CELL_SPACING) * stackRows, ACTIVITY_LABEL_MIN_HEIGHT)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, top = 8.dp, end = 8.dp, bottom = 14.dp)
            .testTag("activity_chart")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val visible = columns.takeLast(activityColumnCount(maxWidth.value.toInt()))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAST ${visible.size} DAYS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
                    if (onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp).testTag("activity_chart_dismiss")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hide activity chart",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row {
                    // Vertical Y-axis label, letters stacked like the sketch.
                    // Exactly the stack height (never longer than the chart):
                    // the stack is at least ACTIVITY_MIN_STACK_ROWS tall, which
                    // is the minimum the 8 tight-metric letters need to fit.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.width(14.dp).height(stackHeight)
                    ) {
                        "ACTIVITY".forEach { letter ->
                            Text(
                                text = letter.toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = LocalTextStyle.current.copy(
                                    fontSize = 9.sp,
                                    lineHeight = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(ACTIVITY_CELL_SPACING)) {
                        visible.forEach { column ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(ACTIVITY_CELL_SIZE)
                                    .then(
                                        if (column.isToday) Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        else Modifier
                                    )
                                    .padding(vertical = 2.dp)
                                    .testTag("activity_day_${column.dateString}")
                            ) {
                                // Stack, top slot first: highest level is the top of the stack.
                                for (level in (stackRows - 1) downTo 0) {
                                    val square = column.squares.getOrNull(level)
                                    if (square != null) {
                                        Box(
                                            modifier = Modifier
                                                .padding(bottom = ACTIVITY_CELL_SPACING)
                                                .size(ACTIVITY_CELL_SIZE)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(parseHabitColor(square.colorHex))
                                                .semantics {
                                                    contentDescription = "${square.habitTitle} ${column.dateString}: logged"
                                                }
                                                .testTag("activity_cell_${square.habitId}_${column.dateString}")
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .padding(bottom = ACTIVITY_CELL_SPACING)
                                                .size(ACTIVITY_CELL_SIZE)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF1E2633))
                                                .semantics {
                                                    contentDescription = "${column.dateString}: no log"
                                                }
                                                .testTag("activity_placeholder_${column.dateString}_$level")
                                        )
                                    }
                                }

                                // X-axis: weekday letter + date number under each column.
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = column.dayLetter,
                                    fontSize = 9.sp,
                                    lineHeight = 9.sp,
                                    fontWeight = if (column.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (column.isToday) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    style = LocalTextStyle.current.copy(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                                Text(
                                    text = column.dayNumber,
                                    fontSize = 8.sp,
                                    lineHeight = 8.sp,
                                    fontWeight = if (column.isToday) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (column.isToday) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    style = LocalTextStyle.current.copy(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
