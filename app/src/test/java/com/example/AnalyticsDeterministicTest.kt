package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.domain.analytics.AnalyticsCache
import com.example.domain.analytics.HabitAnalyticsEngine
import com.example.domain.time.FixedKinetiqClock
import com.example.data.repository.RecordsVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

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
            clock = FixedKinetiqClock(today)
        )
        assertTrue(result.habitStrengthPercent > 0)
        assertEquals(1, result.currentStreak)
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
        val result = HabitAnalyticsEngine.calculateAnalytics(h, records, clock = FixedKinetiqClock(today))
        assertEquals(2, result.currentStreak)
    }

    @Test fun `heatmap is aligned to Monday and has 140 cells`() {
        val today = LocalDate.of(2026, 10, 21)
        val result = HabitAnalyticsEngine.calculateAnalytics(habit(today.minusDays(200)), emptyList(), clock = FixedKinetiqClock(today))
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
        val result = HabitAnalyticsEngine.calculateAnalytics(habit(created), records, clock = FixedKinetiqClock(today))
        assertEquals(3, result.currentStreak)
        assertTrue(result.bestStreak >= 3)
    }

    @Test fun `floor sustain scores below target but above miss`() {
        val today = LocalDate.of(2026, 10, 21)
        val created = today.minusDays(9)
        fun strengthFor(record: LogEntry?) = HabitAnalyticsEngine.calculateAnalytics(
            habit(created), listOfNotNull(record), clock = FixedKinetiqClock(today)
        ).habitStrengthPercent
        val target = strengthFor(LogEntry(habitId = 1, dateString = "2026-10-21", isCompleted = true))
        val floor = strengthFor(LogEntry(habitId = 1, dateString = "2026-10-21", isFloorSustained = true))
        val miss = strengthFor(null)
        assertTrue(target > floor)
        assertTrue(floor >= miss)
    }

    @Test fun `analytics cache invalidates on version change`() {
        val cache = AnalyticsCache()
        val h = habit(LocalDate.of(2026, 10, 1))
        val result = HabitAnalyticsEngine.calculateAnalytics(h, emptyList(), clock = FixedKinetiqClock(LocalDate.of(2026, 10, 21)))
        cache.put(h, RecordsVersion(0, 0), true, true, result)
        assertTrue(cache.get(h, RecordsVersion(0, 0), true, true) != null)
        assertTrue(cache.get(h, RecordsVersion(1, 99), true, true) == null)
        cache.invalidate(1)
        assertTrue(cache.get(h, RecordsVersion(0, 0), true, true) == null)
    }
}
