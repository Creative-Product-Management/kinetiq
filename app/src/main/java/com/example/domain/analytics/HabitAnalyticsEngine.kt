package com.example.domain.analytics

import com.example.data.model.HabitEntity
import com.example.data.model.HabitRecordEntity
import com.example.domain.time.DateCodec
import com.example.domain.time.KinetiqClock
import com.example.domain.time.SystemKinetiqClock
import java.time.DayOfWeek
import java.time.LocalDate
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

    fun calculateAnalytics(
        habit: HabitEntity,
        records: List<HabitRecordEntity>,
        clock: KinetiqClock = SystemKinetiqClock,
        restShieldEnabled: Boolean = true,
        weekStartsOnMonday: Boolean = true
    ): HabitAnalyticsResult {
        val today = clock.today()
        val createdDate = DateCodec.millisToDate(habit.createdAt)
        val recordMap = records.associateBy { it.dateString }

        fun isRest(date: LocalDate): Boolean = restShieldEnabled && habit.isRestDay(date.dayOfWeek.value)
        fun rec(date: LocalDate): HabitRecordEntity? = recordMap[DateCodec.format(date)]
        fun isDone(date: LocalDate): Boolean = rec(date)?.let { it.isCompleted || it.isFloorSustained } == true

        // 1) Habit strength EMA from habit creation (never before it existed)
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
