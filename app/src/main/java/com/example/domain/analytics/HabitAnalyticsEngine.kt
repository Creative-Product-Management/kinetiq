package com.example.domain.analytics

import com.example.data.model.HabitEntity
import com.example.data.model.HabitRecordEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class HeatmapCellState {
    NONE,
    MISSED,
    REST_SHIELD,
    FLOOR_SUSTAINED,
    FULL_COMPLETED
}

data class HeatmapCell(
    val dateString: String,
    val dayOfWeek: Int, // 1=Sun, 2=Mon...
    val state: HeatmapCellState,
    val tooltip: String
)

data class HabitAnalyticsResult(
    val habitId: Long,
    val habitStrengthPercent: Int, // 0 - 100
    val trajectory: String, // "Accelerating", "Steady", "Rebuilding", "Starting"
    val currentStreak: Int,
    val bestStreak: Int,
    val totalTargetCompletions: Int,
    val totalFloorSustains: Int,
    val totalVolumeOrTime: Double,
    val heatmapCells: List<HeatmapCell>,
    val dayOfWeekRates: List<Float>, // index 0..6 (Mon..Sun)
    val frictionCounts: Map<String, Int>,
    val strengthHistory: List<Pair<String, Float>> // (date, score)
)

object HabitAnalyticsEngine {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun calculateAnalytics(
        habit: HabitEntity,
        records: List<HabitRecordEntity>
    ): HabitAnalyticsResult {
        val recordMap = records.associateBy { it.dateString }
        val todayCal = Calendar.getInstance()
        val todayStr = sdf.format(todayCal.time)

        // 1. Calculate Habit Strength Progression using Exponential Moving Average
        val historyDays = 90
        val damping = 0.93f // ~10 day half-life
        var currentStrength = 0.0f
        val strengthProgression = mutableListOf<Pair<String, Float>>()

        for (i in historyDays downTo 0) {
            val pastCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = sdf.format(pastCal.time)
            val dayOfWeek = pastCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon..
            // Convert Calendar day (Sun=1..Sat=7) to 1..7 (Mon=1..Sun=7)
            val isoDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

            val record = recordMap[dateStr]
            val isRest = habit.isRestDay(isoDay)

            if (isRest && (record == null || (!record.isCompleted && !record.isFloorSustained))) {
                // Scheduled Rest Shield: freeze decay!
                currentStrength = currentStrength // maintain
            } else {
                val value = when {
                    record?.isCompleted == true -> 1.0f
                    record?.isFloorSustained == true -> 0.65f
                    else -> 0.0f
                }
                currentStrength = currentStrength * damping + (1f - damping) * value
            }

            if (i <= 30) {
                strengthProgression.add(Pair(dateStr, currentStrength * 100f))
            }
        }

        val strengthPercent = (currentStrength * 100f).roundToInt().coerceIn(0, 100)

        // Trajectory
        val lastWeekStrength = if (strengthProgression.size >= 8) strengthProgression[strengthProgression.size - 8].second else 0f
        val trajectory = when {
            strengthPercent > lastWeekStrength + 3f -> "Accelerating"
            strengthPercent < lastWeekStrength - 3f -> "Rebuilding"
            strengthPercent >= 70 -> "Steady High"
            else -> "Forming Routine"
        }

        // 2. Streaks calculation
        var curStreak = 0
        var bestStreak = 0
        var tempStreak = 0
        var totalTarget = 0
        var totalFloor = 0
        var totalVolume = 0.0

        // Track backwards from today for current streak
        var checkCal = Calendar.getInstance()
        var streakBroken = false

        // Check if today was logged, or if yesterday was the last log
        val todayRecord = recordMap[todayStr]
        val todayIsoDay = if (todayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else todayCal.get(Calendar.DAY_OF_WEEK) - 1
        val isTodayDoneOrShield = (todayRecord?.isCompleted == true || todayRecord?.isFloorSustained == true || habit.isRestDay(todayIsoDay))

        if (!isTodayDoneOrShield) {
            // Check yesterday
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (!streakBroken && curStreak < 365) {
            val dStr = sdf.format(checkCal.time)
            val rec = recordMap[dStr]
            val dow = checkCal.get(Calendar.DAY_OF_WEEK)
            val iso = if (dow == Calendar.SUNDAY) 7 else dow - 1

            if (rec?.isCompleted == true || rec?.isFloorSustained == true) {
                curStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else if (habit.isRestDay(iso)) {
                // Rest shield preserves streak
                curStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                streakBroken = true
            }
        }

        // Calculate all-time best streak & totals
        val sortedDates = records.map { it.dateString }.sorted()
        for (rec in records) {
            if (rec.isCompleted) totalTarget++
            if (rec.isFloorSustained) totalFloor++
            totalVolume += rec.numericValue
        }

        // Best streak scan across past 180 days
        tempStreak = 0
        val scanCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -180) }
        while (scanCal.before(todayCal) || scanCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
            val dStr = sdf.format(scanCal.time)
            val rec = recordMap[dStr]
            val dow = scanCal.get(Calendar.DAY_OF_WEEK)
            val iso = if (dow == Calendar.SUNDAY) 7 else dow - 1

            if (rec?.isCompleted == true || rec?.isFloorSustained == true || habit.isRestDay(iso)) {
                tempStreak++
                if (tempStreak > bestStreak) bestStreak = tempStreak
            } else {
                tempStreak = 0
            }
            scanCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        if (curStreak > bestStreak) bestStreak = curStreak

        // 3. Year-round Heatmap Matrix (Past 140 days / 20 weeks for mobile display)
        val heatmapCells = mutableListOf<HeatmapCell>()
        val matrixCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -139) }

        for (i in 0 until 140) {
            val dStr = sdf.format(matrixCal.time)
            val rec = recordMap[dStr]
            val dow = matrixCal.get(Calendar.DAY_OF_WEEK)
            val iso = if (dow == Calendar.SUNDAY) 7 else dow - 1
            val isRest = habit.isRestDay(iso)

            val state = when {
                rec?.isCompleted == true -> HeatmapCellState.FULL_COMPLETED
                rec?.isFloorSustained == true -> HeatmapCellState.FLOOR_SUSTAINED
                isRest -> HeatmapCellState.REST_SHIELD
                rec != null && rec.frictionTag != null -> HeatmapCellState.MISSED
                else -> HeatmapCellState.NONE
            }

            val tooltip = when (state) {
                HeatmapCellState.FULL_COMPLETED -> "$dStr: Completed Target"
                HeatmapCellState.FLOOR_SUSTAINED -> "$dStr: Sustained Minimum Floor"
                HeatmapCellState.REST_SHIELD -> "$dStr: Rest Day Shield"
                HeatmapCellState.MISSED -> "$dStr: Skipped (${rec?.frictionTag ?: "Unspecified"})"
                HeatmapCellState.NONE -> "$dStr: No entry"
            }

            heatmapCells.add(HeatmapCell(dStr, dow, state, tooltip))
            matrixCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 4. Day of Week Rates (Mon..Sun)
        val dayCounts = IntArray(7)
        val dayCompleted = IntArray(7)
        for (i in 60 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val iso = if (dow == Calendar.SUNDAY) 6 else dow - 2 // 0=Mon..6=Sun
            dayCounts[iso]++

            val dStr = sdf.format(cal.time)
            val rec = recordMap[dStr]
            if (rec?.isCompleted == true || rec?.isFloorSustained == true) {
                dayCompleted[iso]++
            }
        }
        val dayRates = (0..6).map { idx ->
            if (dayCounts[idx] > 0) (dayCompleted[idx].toFloat() / dayCounts[idx].toFloat()) else 0f
        }

        // 5. Friction counts
        val frictionMap = mutableMapOf<String, Int>()
        for (rec in records) {
            rec.frictionTag?.let { tag ->
                frictionMap[tag] = (frictionMap[tag] ?: 0) + 1
            }
        }

        return HabitAnalyticsResult(
            habitId = habit.id,
            habitStrengthPercent = strengthPercent,
            trajectory = trajectory,
            currentStreak = curStreak,
            bestStreak = bestStreak,
            totalTargetCompletions = totalTarget,
            totalFloorSustains = totalFloor,
            totalVolumeOrTime = totalVolume,
            heatmapCells = heatmapCells,
            dayOfWeekRates = dayRates,
            frictionCounts = frictionMap,
            strengthHistory = strengthProgression
        )
    }
}
