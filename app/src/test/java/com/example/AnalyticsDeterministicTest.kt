package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.domain.analytics.AnalyticsCache
import com.example.domain.analytics.HabitAnalyticsEngine
import com.example.domain.time.FixedKineticClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class AnalyticsDeterministicTest {

    private fun millis(date: LocalDate): Long = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun habit(created: LocalDate, restMask: Int = 0) = Habit(
        id = 1, title = "Test", modality = HabitModality.NUMERIC, targetValue = 10.0, floorValue = 2.0,
        unit = "reps", restDaysMask = restMask, createdAt = millis(created)
    )

    @Test fun `new habit is not penalized for days before creation`() {
        val today = LocalDate.of(2026, 10, 21)
        val h = habit(today)
        val result = HabitAnalyticsEngine.calculateAnalytics(
            h, listOf(LogEntry(habitId = 1, dateString = "2026-10-21", isCompleted = true)),
            clock = FixedKineticClock(today)
        )
        assertTrue(result.habitStrengthPercent > 0)
        assertEquals(1, result.currentStreak)
    }

    @Test fun `backfilled logs before creation count toward momentum and streak`() {
        val today = LocalDate.of(2026, 10, 21)
        // Habit created today, but the user backfills the past 6 days plus
        // today from the home day-strip (7 consecutive target days).
        val h = habit(today)
        val records = (0..6).map { offset ->
            LogEntry(habitId = 1, dateString = today.minusDays((6 - offset).toLong()).toString(), isCompleted = true)
        }
        val clock = FixedKineticClock(today)
        val result = HabitAnalyticsEngine.calculateAnalytics(h, records, clock = clock)
        // 7 target days of EMA: 1 - 0.93^7 ≈ 40%.
        assertEquals(40, result.habitStrengthPercent)
        assertEquals(40, HabitAnalyticsEngine.calculateMomentumPercent(h, records, clock = clock))
        assertEquals(7, result.currentStreak)
        assertEquals(7, result.bestStreak)
    }

    @Test fun `days before creation with no logs still do not penalize a new habit`() {
        val today = LocalDate.of(2026, 10, 21)
        val h = habit(today)
        val result = HabitAnalyticsEngine.calculateAnalytics(
            h, listOf(LogEntry(habitId = 1, dateString = "2026-10-21", isCompleted = true)),
            clock = FixedKineticClock(today)
        )
        assertEquals(7, result.habitStrengthPercent)
    }

    @Test fun `rest shield preserves but does not inflate streak`() {
        val today = LocalDate.of(2026, 10, 21) // Wed
        val created = LocalDate.of(2026, 10, 18)
        // Tue (bit 1) is a rest day
        val h = habit(created, restMask = 1 shl 1)
        val records = listOf(
            LogEntry(habitId = 1, dateString = "2026-10-18", isCompleted = true),
            LogEntry(habitId = 1, dateString = "2026-10-19", isCompleted = true)
            // 2026-10-20 Tue: rest, no record. 2026-10-21: not yet logged.
        )
        val result = HabitAnalyticsEngine.calculateAnalytics(h, records, clock = FixedKineticClock(today))
        assertEquals(2, result.currentStreak)
    }

    @Test fun `heatmap is aligned to Monday and has 140 cells`() {
        val today = LocalDate.of(2026, 10, 21)
        val result = HabitAnalyticsEngine.calculateAnalytics(habit(today.minusDays(200)), emptyList(), clock = FixedKineticClock(today))
        assertEquals(140, result.heatmapCells.size)
        assertEquals(1, result.heatmapCells.first().dayOfWeek) // Monday
        assertEquals(7, result.heatmapCells[6].dayOfWeek) // Sunday ends first week
    }

    @Test fun `best streak survives New Year boundary`() {
        val today = LocalDate.of(2027, 1, 2)
        val created = LocalDate.of(2026, 12, 30)
        val records = listOf("2026-12-30", "2026-12-31", "2027-01-01").map {
            LogEntry(habitId = 1, dateString = it, isCompleted = true)
        }
        val result = HabitAnalyticsEngine.calculateAnalytics(habit(created), records, clock = FixedKineticClock(today))
        assertEquals(3, result.currentStreak)
        assertTrue(result.bestStreak >= 3)
    }

    @Test fun `floor sustain scores below target but above miss`() {
        val today = LocalDate.of(2026, 10, 21)
        val created = today.minusDays(9)
        fun strengthFor(record: LogEntry?) = HabitAnalyticsEngine.calculateAnalytics(
            habit(created), listOfNotNull(record), clock = FixedKineticClock(today)
        ).habitStrengthPercent
        val target = strengthFor(LogEntry(habitId = 1, dateString = "2026-10-21", isCompleted = true))
        val floor = strengthFor(LogEntry(habitId = 1, dateString = "2026-10-21", isFloorSustained = true))
        val miss = strengthFor(null)
        assertTrue(target > floor)
        assertTrue(floor >= miss)
    }

    @Test fun `momentum history is null before start and today matches percent`() {
        val today = LocalDate.of(2026, 10, 21)
        val h = habit(today)
        val records = listOf(LogEntry(habitId = 1, dateString = "2026-10-21", isCompleted = true))
        val clock = FixedKineticClock(today)
        val history = HabitAnalyticsEngine.momentumHistory(h, records, clock = clock, days = 7)
        assertEquals(7, history.size)
        assertTrue(history.dropLast(1).all { it == null })
        assertEquals(7, history.last()!!.roundToInt())
        assertEquals(7, HabitAnalyticsEngine.calculateMomentumPercent(h, records, clock = clock))
    }

    @Test fun `momentum history seven target days rounds to forty`() {
        val today = LocalDate.of(2026, 10, 21)
        val h = habit(today.minusDays(6))
        val records = (0..6).map { offset ->
            LogEntry(habitId = 1, dateString = today.minusDays((6 - offset).toLong()).toString(), isCompleted = true)
        }
        val clock = FixedKineticClock(today)
        val history = HabitAnalyticsEngine.momentumHistory(h, records, clock = clock, days = 7)
        assertEquals(40, history.last()!!.roundToInt())
        assertEquals(40, HabitAnalyticsEngine.calculateMomentumPercent(h, records, clock = clock))
    }

    @Test fun `rest shield day freezes momentum history`() {
        // Oct 21 2026 = Wed → Oct 19 Mon, Oct 20 Tue. Rest on Tue (bit 1).
        val created = LocalDate.of(2026, 10, 19)
        val h = habit(created, restMask = 1 shl 1)
        val records = listOf(LogEntry(habitId = 1, dateString = "2026-10-19", isCompleted = true))
        val throughRest = HabitAnalyticsEngine.momentumHistory(
            h, records, clock = FixedKineticClock(LocalDate.of(2026, 10, 20)), days = 2
        )
        // Mon ≈ 7%; Tue rest with no log freezes that score.
        assertEquals(throughRest[0], throughRest[1])
        assertTrue(throughRest[0] != null && throughRest[0]!! > 0f)
    }

    @Test fun `analytics cache is keyed by the records it was computed from`() {
        val cache = AnalyticsCache()
        val h = habit(LocalDate.of(2026, 10, 1))
        val clock = FixedKineticClock(LocalDate.of(2026, 10, 21))
        val emptyResult = HabitAnalyticsEngine.calculateAnalytics(h, emptyList(), clock = clock)
        cache.put(h, emptyList(), true, true, emptyResult)
        assertTrue(cache.get(h, emptyList(), true, true) != null)
        // A different records list must miss, even if it would share the same
        // count/latest stamp timing as a stale emission (the old cache race).
        val oneRecord = listOf(LogEntry(habitId = 1, dateString = "2026-10-21", isCompleted = true))
        assertTrue(cache.get(h, oneRecord, true, true) == null)
        // Recomputing from the new records and caching must then hit and
        // return the new (non-zero) result, not the poisoned empty one.
        val freshResult = HabitAnalyticsEngine.calculateAnalytics(h, oneRecord, clock = clock)
        assertTrue(freshResult.habitStrengthPercent > 0)
        cache.put(h, oneRecord, true, true, freshResult)
        assertEquals(freshResult, cache.get(h, oneRecord, true, true))
        cache.invalidate(1)
        assertTrue(cache.get(h, oneRecord, true, true) == null)
    }
}
