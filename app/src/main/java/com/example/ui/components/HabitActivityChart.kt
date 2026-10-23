package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FloorGold
import com.example.ui.theme.KineticEmerald
import com.example.ui.theme.KineticViolet
import com.example.ui.theme.parseHabitColor
import com.example.ui.viewmodel.HabitCardUi
import kotlin.math.roundToInt

val ACTIVITY_CELL_SIZE = 20.dp
val ACTIVITY_CELL_SPACING = 4.dp

/** Minimum stack height per date so empty days still show a full placeholder column. */
const val ACTIVITY_MIN_STACK_ROWS = 5

/** Minimum height the vertical ACTIVITY / MOMENTUM label needs. */
val ACTIVITY_LABEL_MIN_HEIGHT = 96.dp

/** Fixed plot height for the Momentum line graph (does not grow with habit count). */
val MOMENTUM_PLOT_HEIGHT = ACTIVITY_LABEL_MIN_HEIGHT

/** Stack height for a date column: one slot per habit, at least [ACTIVITY_MIN_STACK_ROWS]. */
fun activityStackRows(habitCount: Int): Int = maxOf(ACTIVITY_MIN_STACK_ROWS, habitCount)

enum class HomeChartMode { ACTIVITY, MOMENTUM }

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
 * One date point on the overall Momentum line. [momentumPercent] is null when
 * no visible habit had started yet (gap in the line, not a zero).
 */
data class MomentumDayPoint(
    val dateString: String,
    val dayLetter: String,
    val dayNumber: String,
    val isToday: Boolean,
    val momentumPercent: Float?
)

/**
 * Builds the stacked columns from the visible cards: for each of the
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

/**
 * Overall Momentum series for the visible cards: unweighted mean of each
 * habit's [HabitCardUi.momentumHistory] on that date. Habits that have not
 * started (null) are excluded. A day with no started habits is null.
 * Dates match [buildActivityColumns].
 */
fun buildOverallMomentumPoints(cards: List<HabitCardUi>): List<MomentumDayPoint> {
    val dates = cards.firstOrNull()?.activityDays.orEmpty()
    if (dates.isEmpty()) return emptyList()
    return dates.indices.map { index ->
        val dateItem = dates[index]
        val scores = cards.mapNotNull { card -> card.momentumHistory.getOrNull(index) }
        val mean = if (scores.isEmpty()) null else scores.average().toFloat()
        MomentumDayPoint(
            dateString = dateItem.dateString,
            dayLetter = dateItem.dayLetter,
            dayNumber = dateItem.dayNumber,
            isToday = dateItem.isToday,
            momentumPercent = mean
        )
    }
}

/** Rounded headline for the latest non-null overall Momentum point, or null. */
fun overallMomentumHeadline(points: List<MomentumDayPoint>): Int? =
    points.lastOrNull { it.momentumPercent != null }?.momentumPercent?.roundToInt()?.coerceIn(0, 100)

fun momentumAccentColor(percent: Int): Color = when {
    percent >= 70 -> KineticEmerald
    percent >= 40 -> FloorGold
    else -> KineticViolet
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
 * Home chart panel: Activity stacks or overall Momentum line graph.
 * Dates run along the bottom (weekday letter + date number; as many recent
 * days as fit, up to 30, today at the right). Display-only; logging happens
 * on the habit cards below. [onDismiss] permanently hides the chart
 * (same as turning it off in Settings). Mode is session-only.
 */
@Composable
fun HabitActivityChart(
    rows: List<HabitCardUi>,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    if (rows.isEmpty()) return
    val activityColumns = buildActivityColumns(rows)
    val momentumPoints = buildOverallMomentumPoints(rows)
    if (activityColumns.isEmpty()) return
    var mode by remember { mutableStateOf(HomeChartMode.ACTIVITY) }
    val stackRows = activityStackRows(rows.size)
    val stackHeight = maxOf((ACTIVITY_CELL_SIZE + ACTIVITY_CELL_SPACING) * stackRows, ACTIVITY_LABEL_MIN_HEIGHT)
    val plotHeight = if (mode == HomeChartMode.ACTIVITY) stackHeight else MOMENTUM_PLOT_HEIGHT
    val headline = overallMomentumHeadline(momentumPoints)
    val accent = headline?.let { momentumAccentColor(it) } ?: KineticViolet

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, top = 8.dp, end = 8.dp, bottom = 14.dp)
            .testTag("activity_chart")
            .semantics {
                contentDescription = when (mode) {
                    HomeChartMode.ACTIVITY -> "Activity chart, last days"
                    HomeChartMode.MOMENTUM -> "Momentum chart, overall ${headline ?: 0} percent"
                }
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val visibleCount = activityColumnCount(maxWidth.value.toInt())
            val visibleActivity = activityColumns.takeLast(visibleCount)
            val visibleMomentum = momentumPoints.takeLast(visibleCount)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAST ${visibleCount} DAYS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
                    if (mode == HomeChartMode.MOMENTUM && headline != null) {
                        Text(
                            text = "$headline%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("momentum_headline")
                        )
                    }
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

                Spacer(modifier = Modifier.height(8.dp))
                ChartModeToggle(
                    mode = mode,
                    onModeChange = { mode = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    VerticalAxisLabel(
                        label = if (mode == HomeChartMode.ACTIVITY) "ACTIVITY" else "MOMENTUM",
                        height = plotHeight
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    when (mode) {
                        HomeChartMode.ACTIVITY -> ActivityStacksBody(
                            columns = visibleActivity,
                            stackRows = stackRows
                        )
                        HomeChartMode.MOMENTUM -> MomentumLineBody(
                            points = visibleMomentum,
                            accent = accent,
                            plotHeight = plotHeight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartModeToggle(
    mode: HomeChartMode,
    onModeChange: (HomeChartMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(3.dp)
            .testTag("chart_mode_toggle"),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        ChartModeChip(
            label = "Activity",
            selected = mode == HomeChartMode.ACTIVITY,
            onClick = { onModeChange(HomeChartMode.ACTIVITY) },
            testTag = "chart_mode_activity",
            modifier = Modifier.weight(1f)
        )
        ChartModeChip(
            label = "Momentum",
            selected = mode == HomeChartMode.MOMENTUM,
            onClick = { onModeChange(HomeChartMode.MOMENTUM) },
            testTag = "chart_mode_momentum",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ChartModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag)
    )
}

@Composable
private fun VerticalAxisLabel(label: String, height: androidx.compose.ui.unit.Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.width(14.dp).height(height)
    ) {
        label.forEach { letter ->
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
}

@Composable
private fun ActivityStacksBody(
    columns: List<ActivityDayColumn>,
    stackRows: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ACTIVITY_CELL_SPACING)) {
        columns.forEach { column ->
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
                DayAxisLabels(dayLetter = column.dayLetter, dayNumber = column.dayNumber, isToday = column.isToday)
            }
        }
    }
}

@Composable
private fun MomentumLineBody(
    points: List<MomentumDayPoint>,
    accent: Color,
    plotHeight: androidx.compose.ui.unit.Dp
) {
    if (points.isEmpty()) return
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val todayBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    Column {
        Row(
            modifier = Modifier.height(plotHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-axis tick labels (100 / 50 / 0)
            Column(
                modifier = Modifier
                    .width(22.dp)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("100", "50", "0").forEach { tick ->
                    Text(
                        text = tick,
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        color = axisColor,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
            val chartWidth = (ACTIVITY_CELL_SIZE + ACTIVITY_CELL_SPACING) * points.size - ACTIVITY_CELL_SPACING
            Box(
                modifier = Modifier
                    .width(chartWidth)
                    .fillMaxHeight()
                    .testTag("momentum_line_canvas")
            ) {
                // Today highlight behind the line.
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(ACTIVITY_CELL_SPACING)
                ) {
                    points.forEach { point ->
                        Box(
                            modifier = Modifier
                                .width(ACTIVITY_CELL_SIZE)
                                .fillMaxHeight()
                                .then(
                                    if (point.isToday) Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(todayBg)
                                    else Modifier
                                )
                        )
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val n = points.size
                    val colW = if (n > 0) w / n else w
                    fun xFor(i: Int): Float = colW * i + colW / 2f
                    fun yFor(value: Float): Float = h * (1f - (value.coerceIn(0f, 100f) / 100f))

                    val guide = Color(0xFF1E2633)
                    drawLine(guide, Offset(0f, yFor(100f)), Offset(w, yFor(100f)), strokeWidth = 1f)
                    drawLine(guide, Offset(0f, yFor(50f)), Offset(w, yFor(50f)), strokeWidth = 1f)
                    drawLine(guide, Offset(0f, yFor(0f)), Offset(w, yFor(0f)), strokeWidth = 1f)

                    // Break the path across null gaps (no started habits that day).
                    var segment = Path()
                    var hasPoint = false
                    points.forEachIndexed { i, point ->
                        val v = point.momentumPercent
                        if (v == null) {
                            if (hasPoint) {
                                drawPath(
                                    path = segment,
                                    color = accent,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                segment = Path()
                                hasPoint = false
                            }
                        } else {
                            val x = xFor(i)
                            val y = yFor(v)
                            if (!hasPoint) {
                                segment.moveTo(x, y)
                                hasPoint = true
                            } else {
                                segment.lineTo(x, y)
                            }
                        }
                    }
                    if (hasPoint) {
                        drawPath(
                            path = segment,
                            color = accent,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    points.forEachIndexed { i, point ->
                        val v = point.momentumPercent ?: return@forEachIndexed
                        drawCircle(
                            color = accent,
                            radius = 3.dp.toPx(),
                            center = Offset(xFor(i), yFor(v))
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ACTIVITY_CELL_SPACING)) {
            // Align with canvas: left pad for Y tick labels already outside this row.
            Spacer(modifier = Modifier.width(22.dp))
            points.forEach { point ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(ACTIVITY_CELL_SIZE)
                        .testTag("momentum_day_${point.dateString}")
                ) {
                    DayAxisLabels(dayLetter = point.dayLetter, dayNumber = point.dayNumber, isToday = point.isToday)
                }
            }
        }
    }
}

@Composable
private fun DayAxisLabels(dayLetter: String, dayNumber: String, isToday: Boolean) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = dayLetter,
        fontSize = 9.sp,
        lineHeight = 9.sp,
        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        color = if (isToday) {
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
        text = dayNumber,
        fontSize = 8.sp,
        lineHeight = 8.sp,
        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isToday) {
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
