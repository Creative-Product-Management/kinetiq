package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.data.repository.HabitSortMode
import com.example.domain.analytics.HabitAnalyticsEngine
import com.example.domain.time.FixedKinetiqClock
import com.example.ui.viewmodel.HabitCardUi
import com.example.ui.viewmodel.sortHabitCards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MomentumCardTest {

    private fun millis(date: LocalDate): Long = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun `card momentum matches detail analytics momentum`() {
        val today = LocalDate.of(2026, 10, 21)
        val created = today.minusDays(20)
        val habit = Habit(id = 7, title = "Read", modality = HabitModality.NUMERIC, targetValue = 10.0, floorValue = 2.0, unit = "pages", createdAt = millis(created))
        val records = (0..13).map { offset ->
            val date = created.plusDays(offset.toLong()).toString()
            if (offset % 3 == 2) LogEntry(habitId = 7, dateString = date, isFloorSustained = true)
            else LogEntry(habitId = 7, dateString = date, isCompleted = true)
        }
        val clock = FixedKinetiqClock(today)
        val cardMomentum = HabitAnalyticsEngine.calculateMomentumPercent(habit, records, clock = clock)
        val detailMomentum = HabitAnalyticsEngine.calculateAnalytics(habit, records, clock = clock).habitStrengthPercent
        assertEquals(detailMomentum, cardMomentum)
        assertTrue(cardMomentum in 1..99)
    }

    @Test fun `momentum sort is highest first and manual mode keeps saved order`() {
        fun card(id: Long, order: Int, momentum: Int) = HabitCardUi(
            habit = Habit(id = id, title = "H$id", orderIndex = order),
            categoryName = "General", recentDays = emptyList(), momentumPercent = momentum
        )
        val cards = listOf(card(1, 0, 20), card(2, 1, 85), card(3, 2, 85), card(4, 3, 50))
        assertEquals(listOf(1L, 2L, 3L, 4L), sortHabitCards(cards, HabitSortMode.MANUAL).map { it.habit.id })
        // Highest first; the two 85% habits keep their manual relative order.
        assertEquals(listOf(2L, 3L, 4L, 1L), sortHabitCards(cards, HabitSortMode.MOMENTUM).map { it.habit.id })
    }
}
