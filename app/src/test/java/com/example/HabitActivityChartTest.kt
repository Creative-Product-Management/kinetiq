package com.example

import com.example.data.model.Habit
import com.example.data.model.LogEntry
import com.example.ui.components.activityColumnCount
import com.example.ui.components.activityStackRows
import com.example.ui.components.buildActivityColumns
import com.example.ui.viewmodel.HabitCardUi
import com.example.ui.viewmodel.buildDayStrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitActivityChartTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 21)

    private fun card(id: Long, colorHex: String, records: List<LogEntry>): HabitCardUi {
        val habit = Habit(id = id, title = "H$id", colorHex = colorHex)
        return HabitCardUi(
            habit = habit,
            categoryName = "General",
            recentDays = buildDayStrip(records.filter { it.habitId == id }, today, days = 7),
            activityDays = buildDayStrip(records.filter { it.habitId == id }, today, days = 14)
        )
    }

    private fun log(habitId: Long, date: String, recordedAt: Long, completed: Boolean = true, floor: Boolean = false) =
        LogEntry(habitId = habitId, dateString = date, isCompleted = completed, isFloorSustained = floor, recordedAt = recordedAt)

    @Test fun `columns cover 14 days ending today`() {
        val columns = buildActivityColumns(listOf(card(1, "#111111", emptyList())))
        assertEquals(14, columns.size)
        assertEquals("2026-10-08", columns.first().dateString)
        assertEquals("2026-10-21", columns.last().dateString)
        assertTrue(columns.last().isToday)
    }

    @Test fun `squares stack in logged order earliest first with habit colours`() {
        // Habit 2 logged at t=100, habit 1 later at t=300, on the same day.
        val records = listOf(
            log(1, "2026-10-21", recordedAt = 300),
            log(2, "2026-10-21", recordedAt = 100)
        )
        val cards = listOf(card(1, "#AAAAAA", records), card(2, "#BBBBBB", records))
        val column = buildActivityColumns(cards).last()
        assertEquals(listOf(2L, 1L), column.squares.map { it.habitId })
        assertEquals(listOf("#BBBBBB", "#AAAAAA"), column.squares.map { it.colorHex })
    }

    @Test fun `floor logs fill a square and partial or friction-only records do not`() {
        val records = listOf(
            log(1, "2026-10-21", recordedAt = 100, completed = false, floor = true),
            LogEntry(habitId = 2, dateString = "2026-10-21", numericValue = 1.0, recordedAt = 200),
            LogEntry(habitId = 3, dateString = "2026-10-21", frictionTag = "Tired", recordedAt = 300)
        )
        val cards = listOf(card(1, "#111111", records), card(2, "#222222", records), card(3, "#333333", records))
        val column = buildActivityColumns(cards).last()
        assertEquals(listOf(1L), column.squares.map { it.habitId })
        assertTrue(column.squares.single().isFloor)
    }

    @Test fun `empty days have no squares and stack height is habit count with minimum five`() {
        val columns = buildActivityColumns(listOf(card(1, "#111111", emptyList()), card(2, "#222222", emptyList())))
        assertTrue(columns.all { it.squares.isEmpty() })
        assertEquals(5, activityStackRows(0))
        assertEquals(5, activityStackRows(2))
        assertEquals(5, activityStackRows(5))
        assertEquals(8, activityStackRows(8))
    }

    @Test fun `column count fills the available width and caps at 30 days`() {
        // Phone-width chart content (~300dp incl. label) fits about 11 days.
        assertEquals(11, activityColumnCount(300))
        // Wider screens show more, never more than 30.
        assertEquals(20, activityColumnCount(500))
        assertEquals(30, activityColumnCount(760))
        assertEquals(30, activityColumnCount(2000))
        // Degenerate widths still show at least today.
        assertEquals(1, activityColumnCount(10))
    }
}
