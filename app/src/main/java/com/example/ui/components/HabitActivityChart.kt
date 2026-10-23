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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

/** Stack height for a date column: one slot per habit, at least [ACTIVITY_MIN_STACK_ROWS]. */
fun activityStackRows(habitCount: Int): Int = maxOf(ACTIVITY_MIN_STACK_ROWS, habitCount)

/** Soft fill for the current-day column (plot + X-axis labels). */
@Composable
private fun todayColumnFill(): Color =
    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

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
    // Same plot height for Activity and Momentum so toggling never resizes the panel.
    val plotHeight = maxOf((ACTIVITY_CELL_SIZE + ACTIVITY_CELL_SPACING) * stackRows, ACTIVITY_LABEL_MIN_HEIGHT)
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
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
                    if (mode == HomeChartMode.MOMENTUM && headline != null) {
                        Text(
                            text = "$headline%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .testTag("momentum_headline")
                        )
                    }
                    ChartModeToggle(
                        mode = mode,
                        onModeChange = { mode = it }
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
                    if (mode == HomeChartMode.ACTIVITY) {
                        VerticalAxisLabel(label = "ACTIVITY", height = plotHeight)
                        Spacer(modifier = Modifier.width(6.dp))
                        ActivityStacksBody(
                            columns = visibleActivity,
                            stackRows = stackRows,
                            plotHeight = plotHeight
                        )
                    } else {
                        // Momentum uses 0/50/100 tick labels as the Y-axis; no vertical word label.
                        MomentumLineBody(
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
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(1.dp)
            .testTag("chart_mode_toggle"),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        ChartModeChip(
            label = "Activity",
            selected = mode == HomeChartMode.ACTIVITY,
            onClick = { onModeChange(HomeChartMode.ACTIVITY) },
            testTag = "chart_mode_activity"
        )
        ChartModeChip(
            label = "Momentum",
            selected = mode == HomeChartMode.MOMENTUM,
            onClick = { onModeChange(HomeChartMode.MOMENTUM) },
            testTag = "chart_mode_momentum"
        )
    }
}

@Composable
private fun ChartModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Text(
        text = label,
        fontSize = 10.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        color = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        style = LocalTextStyle.current.copy(
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.surface)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp)
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
    stackRows: Int,
    plotHeight: androidx.compose.ui.unit.Dp
) {
    val todayFill = todayColumnFill()
    Row(horizontalArrangement = Arrangement.spacedBy(ACTIVITY_CELL_SPACING)) {
        columns.forEach { column ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(ACTIVITY_CELL_SIZE)
                    .then(
                        if (column.isToday) Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(todayFill)
                        else Modifier
                    )
                    .testTag("activity_day_${column.dateString}")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .width(ACTIVITY_CELL_SIZE)
                        .height(plotHeight)
                        .padding(vertical = 2.dp)
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
                }
                DayAxisLabels(
                    dayLetter = column.dayLetter,
                    dayNumber = column.dayNumber,
                    isToday = column.isToday,
                    shaded = false
                )
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
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val todayFill = todayColumnFill()
    val barFill = accent.copy(alpha = 0.22f)
    val chartWidth = if (points.isEmpty()) {
        0.dp
    } else {
        (ACTIVITY_CELL_SIZE + ACTIVITY_CELL_SPACING) * points.size - ACTIVITY_CELL_SPACING
    }
    Row {
        Column(
            modifier = Modifier
                .width(22.dp)
                .height(plotHeight)
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
        Column(modifier = Modifier.width(chartWidth)) {
            Canvas(
                modifier = Modifier
                    .width(chartWidth)
                    .height(plotHeight)
                    .testTag("momentum_line_canvas")
            ) {
                val h = size.height
                val cell = ACTIVITY_CELL_SIZE.toPx()
                val gap = ACTIVITY_CELL_SPACING.toPx()
                val stride = cell + gap
                fun leftFor(i: Int): Float = i * stride
                fun xFor(i: Int): Float = leftFor(i) + cell / 2f
                fun yFor(value: Float): Float = h * (1f - (value.coerceIn(0f, 100f) / 100f))

                val guide = Color(0xFF1E2633)
                drawLine(guide, Offset(0f, yFor(100f)), Offset(size.width, yFor(100f)), strokeWidth = 1f)
                drawLine(guide, Offset(0f, yFor(50f)), Offset(size.width, yFor(50f)), strokeWidth = 1f)
                drawLine(guide, Offset(0f, yFor(0f)), Offset(size.width, yFor(0f)), strokeWidth = 1f)

                // Faded bars: overall momentum for each day, under the line.
                points.forEachIndexed { i, point ->
                    val v = point.momentumPercent ?: return@forEachIndexed
                    val inset = cell * 0.15f
                    val barW = cell * 0.7f
                    val top = yFor(v)
                    val barH = (h - top).coerceAtLeast(0f)
                    // Today: soft column only up to that day's momentum (not full 0–100).
                    if (point.isToday && barH > 0f) {
                        drawRoundRect(
                            color = todayFill,
                            topLeft = Offset(leftFor(i), top),
                            size = Size(cell, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                    drawRoundRect(
                        color = barFill,
                        topLeft = Offset(leftFor(i) + inset, top),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }

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
            Row(horizontalArrangement = Arrangement.spacedBy(ACTIVITY_CELL_SPACING)) {
                points.forEach { point ->
                    DayAxisLabels(
                        dayLetter = point.dayLetter,
                        dayNumber = point.dayNumber,
                        isToday = point.isToday,
                        modifier = Modifier
                            .width(ACTIVITY_CELL_SIZE)
                            .testTag("momentum_day_${point.dateString}")
                    )
                }
            }
        }
    }
}

@Composable
private fun DayAxisLabels(
    dayLetter: String,
    dayNumber: String,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    /** When false, parent already paints the today column shade. */
    shaded: Boolean = true
) {
    val todayFill = todayColumnFill()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(
                if (isToday && shaded) Modifier
                    .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .background(todayFill)
                else Modifier
            )
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(
            text = dayLetter,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
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
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
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
