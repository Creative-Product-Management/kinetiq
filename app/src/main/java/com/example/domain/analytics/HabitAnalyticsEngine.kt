package com.example.domain.analytics

import com.example.data.model.HabitEntity
import com.example.data.model.HabitRecordEntity
import com.example.domain.time.DateCodec
import com.example.domain.time.KineticClock
import com.example.domain.time.SystemKineticClock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class HeatmapCellState { NONE, MISSED, REST_SHIELD, FLOOR_SUSTAINED, FULL_COMPLETED }

data class HeatmapCell(
    val dateString: String,
    val dayOfWeek: Int, // ISO Mon=1..Sun=7
    val state: HeatmapCellState,
    val tooltip: String
)

data class HabitAnalyticsResult(
    val habitId: Long,
    val habitStrengthPercent: Int,
    val trajectory: String,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalTargetCompletions: Int,
    val totalFloorSustains: Int,
    val totalVolumeOrTime: Double,
    val heatmapCells: List<HeatmapCell>,
    val dayOfWeekRates: List<Float>, // Mon..Sun
    val frictionCounts: Map<String, Int>,
    val strengthHistory: List<Pair<String, Float>>
)

object HabitAnalyticsEngine {

    /**
     * First day analytics may count: the habit's creation date, or its
     * earliest log if the user backfilled days from before the habit was
     * created in the app (the home day-strip offers the past 7 days).
     * Days before this are never counted — and never penalised either,
     * since the EMA starts at 0 and missed days keep it at 0.
     */
    private fun habitStartDate(habit: HabitEntity, records: List<HabitRecordEntity>): LocalDate {
        val createdDate = DateCodec.millisToDate(habit.createdAt)
        val earliestLog = records.mapNotNull { DateCodec.parse(it.dateString) }.minOrNull()
        return if (earliestLog != null && earliestLog.isBefore(createdDate)) earliestLog else createdDate
    }

    /**
     * Daily momentum (0..100) for the last [days] calendar days ending today,
     * oldest first. Days before [habitStartDate] are null (habit excluded from
     * an overall average). From the start date onward the slot is the unrounded
     * EMA × 100. The walk still begins at max(today−90, startDate) so older
     * points in the visible window carry full memory.
     */
    fun momentumHistory(
        habit: HabitEntity,
        records: List<HabitRecordEntity>,
        clock: KineticClock = SystemKineticClock,
        restShieldEnabled: Boolean = true,
        days: Int = 30
    ): List<Float?> {
        require(days > 0) { "days must be positive" }
        val today = clock.today()
        val startDate = habitStartDate(habit, records)
        val recordMap = records.associateBy { it.dateString }
        val windowStart = today.minusDays((days - 1).toLong())
        var strength = 0.0f
        var date = maxOf(today.minusDays(90), startDate)
        // Warm the EMA through the day before the visible window.
        while (date.isBefore(windowStart) && !date.isBefore(startDate)) {
            val record = recordMap[DateCodec.format(date)]
            val isDone = record?.let { it.isCompleted || it.isFloorSustained } == true
            val isRest = restShieldEnabled && habit.isRestDay(date.dayOfWeek.value)
            if (!(isRest && !isDone)) {
                val v = when {
                    record?.isCompleted == true -> 1.0f
                    record?.isFloorSustained == true -> 0.65f
                    else -> 0.0f
                }
                strength = strength * 0.93f + (1f - 0.93f) * v
            }
            date = date.plusDays(1)
        }
        // If the habit starts after the window start, begin the walk at startDate
        // (strength still 0) once we enter the visible range.
        if (date.isBefore(startDate)) date = startDate
        val history = MutableList<Float?>(days) { null }
        while (!date.isAfter(today)) {
            if (!date.isBefore(startDate)) {
                val record = recordMap[DateCodec.format(date)]
                val isDone = record?.let { it.isCompleted || it.isFloorSustained } == true
                val isRest = restShieldEnabled && habit.isRestDay(date.dayOfWeek.value)
                if (!(isRest && !isDone)) {
                    val v = when {
                        record?.isCompleted == true -> 1.0f
                        record?.isFloorSustained == true -> 0.65f
                        else -> 0.0f
                    }
                    strength = strength * 0.93f + (1f - 0.93f) * v
                }
                val offset = ChronoUnit.DAYS.between(windowStart, date).toInt()
                if (offset in 0 until days) history[offset] = strength * 100f
            }
            date = date.plusDays(1)
        }
        return history
    }

    /**
     * Momentum only (0..100), using the exact EMA from [calculateAnalytics]:
     * target = 1.0, floor = 0.65, miss = 0.0, damping 0.93, last 90 days,
     * rest-shield days frozen. Derived from [momentumHistory] so the card
     * badge matches the right edge of the Home momentum line.
     */
    fun calculateMomentumPercent(
        habit: HabitEntity,
        records: List<HabitRecordEntity>,
        clock: KineticClock = SystemKineticClock,
        restShieldEnabled: Boolean = true
    ): Int {
        val last = momentumHistory(habit, records, clock, restShieldEnabled, days = 1)
            .lastOrNull() ?: return 0
        return last.roundToInt().coerceIn(0, 100)
    }

    fun calculateAnalytics(
        habit: HabitEntity,
        records: List<HabitRecordEntity>,
        clock: KineticClock = SystemKineticClock,
        restShieldEnabled: Boolean = true,
        weekStartsOnMonday: Boolean = true
    ): HabitAnalyticsResult {
        val today = clock.today()
        val createdDate = habitStartDate(habit, records)
        val recordMap = records.associateBy { it.dateString }

        fun isRest(date: LocalDate): Boolean = restShieldEnabled && habit.isRestDay(date.dayOfWeek.value)
        fun rec(date: LocalDate): HabitRecordEntity? = recordMap[DateCodec.format(date)]
        fun isDone(date: LocalDate): Boolean = rec(date)?.let { it.isCompleted || it.isFloorSustained } == true

        // 1) Habit strength EMA from the habit's first countable day (see habitStartDate)
        val historyDays = 90
        val windowStart = maxOf(today.minusDays(historyDays.toLong()), createdDate)
        val damping = 0.93f
        var strength = 0.0f
        val progression = mutableListOf<Pair<String, Float>>()
        var date = windowStart
        while (!date.isAfter(today)) {
            if (isRest(date) && !isDone(date)) {
                // shield: freeze decay
            } else {
                val v = when {
                    rec(date)?.isCompleted == true -> 1.0f
                    rec(date)?.isFloorSustained == true -> 0.65f
                    else -> 0.0f
                }
                strength = strength * damping + (1f - damping) * v
            }
            if (!date.isBefore(today.minusDays(30))) progression.add(DateCodec.format(date) to strength * 100f)
            date = date.plusDays(1)
        }
        val strengthPercent = (strength * 100f).roundToInt().coerceIn(0, 100)
        val lastWeek = if (progression.size >= 8) progression[progression.size - 8].second else 0f
        val trajectory = when {
            strengthPercent > lastWeek + 3f -> "Accelerating"
            strengthPercent < lastWeek - 3f -> "Rebuilding"
            strengthPercent >= 70 -> "Steady High"
            else -> "Forming Routine"
        }

        // 2) Streaks: shield days preserve but do not inflate the active-day count
        var currentStreak = 0
        var cursor = today
        if (!isDone(cursor) && !isRest(cursor)) cursor = cursor.minusDays(1)
        while (!cursor.isBefore(createdDate)) {
            when {
                isDone(cursor) -> { currentStreak++; cursor = cursor.minusDays(1) }
                isRest(cursor) -> cursor = cursor.minusDays(1) // preserved, not counted
                else -> break
            }
        }
        var bestStreak = 0
        var temp = 0
        var scan = maxOf(today.minusDays(180), createdDate)
        while (!scan.isAfter(today)) {
            when {
                isDone(scan) -> { temp++; if (temp > bestStreak) bestStreak = temp }
                isRest(scan) -> Unit // preserved, not counted
                else -> temp = 0
            }
            scan = scan.plusDays(1)
        }
        if (currentStreak > bestStreak) bestStreak = currentStreak

        var totalTarget = 0; var totalFloor = 0; var totalVolume = 0.0
        for (r in records) {
            if (r.isCompleted) totalTarget++
            if (r.isFloorSustained) totalFloor++
            totalVolume += r.numericValue
        }

        // 3) Heatmap aligned to week start: 20 full weeks ending this week
        val firstDayOfWeek = if (weekStartsOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        var heatStart = today.minusDays(139)
        while (heatStart.dayOfWeek != firstDayOfWeek) heatStart = heatStart.minusDays(1)
        if (heatStart.isBefore(createdDate)) {
            // keep alignment; pre-creation cells render as NONE below via created check
        }
        val heatmapCells = mutableListOf<HeatmapCell>()
        var h = heatStart
        // 20 weeks * 7 from aligned start, capped at today-aligned week end
        repeat(20 * 7) {
            val r = rec(h)
            val beforeCreation = h.isBefore(createdDate)
            val state = when {
                beforeCreation -> HeatmapCellState.NONE
                r?.isCompleted == true -> HeatmapCellState.FULL_COMPLETED
                r?.isFloorSustained == true -> HeatmapCellState.FLOOR_SUSTAINED
                isRest(h) -> HeatmapCellState.REST_SHIELD
                r != null && r.frictionTag != null -> HeatmapCellState.MISSED
                else -> HeatmapCellState.NONE
            }
            val tooltip = when (state) {
                HeatmapCellState.FULL_COMPLETED -> "${DateCodec.format(h)}: Completed Target"
                HeatmapCellState.FLOOR_SUSTAINED -> "${DateCodec.format(h)}: Sustained Minimum Floor"
                HeatmapCellState.REST_SHIELD -> "${DateCodec.format(h)}: Rest Day Shield"
                HeatmapCellState.MISSED -> "${DateCodec.format(h)}: Skipped (${r?.frictionTag ?: "Unspecified"})"
                HeatmapCellState.NONE -> "${DateCodec.format(h)}: No entry"
            }
            heatmapCells.add(HeatmapCell(DateCodec.format(h), h.dayOfWeek.value, state, tooltip))
            h = h.plusDays(1)
        }

        // 4) Day-of-week rates Mon..Sun over last 60 days (not before creation)
        val dayCounts = IntArray(7); val dayCompleted = IntArray(7)
        var d = maxOf(today.minusDays(60), createdDate)
        while (!d.isAfter(today)) {
            val idx = d.dayOfWeek.value - 1
            dayCounts[idx]++
            if (isDone(d)) dayCompleted[idx]++
            d = d.plusDays(1)
        }
        val dayRates = (0..6).map { i -> if (dayCounts[i] > 0) dayCompleted[i].toFloat() / dayCounts[i] else 0f }

        val frictionMap = mutableMapOf<String, Int>()
        for (r in records) r.frictionTag?.let { frictionMap[it] = (frictionMap[it] ?: 0) + 1 }

        return HabitAnalyticsResult(
            habitId = habit.id, habitStrengthPercent = strengthPercent, trajectory = trajectory,
            currentStreak = currentStreak, bestStreak = bestStreak,
            totalTargetCompletions = totalTarget, totalFloorSustains = totalFloor, totalVolumeOrTime = totalVolume,
            heatmapCells = heatmapCells, dayOfWeekRates = dayRates, frictionCounts = frictionMap,
            strengthHistory = progression
        )
    }
}
