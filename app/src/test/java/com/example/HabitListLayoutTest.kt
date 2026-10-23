package com.example

import com.example.data.repository.HabitListLayout
import com.example.ui.viewmodel.KineticViewModel
import com.example.ui.viewmodel.buildDayStrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitListLayoutTest {

    @Test fun `fromStored defaults unknown and null to card`() {
        assertEquals(HabitListLayout.CARD, HabitListLayout.fromStored(null))
        assertEquals(HabitListLayout.CARD, HabitListLayout.fromStored(""))
        assertEquals(HabitListLayout.CARD, HabitListLayout.fromStored("CARD"))
        assertEquals(HabitListLayout.CARD, HabitListLayout.fromStored("bogus"))
    }

    @Test fun `fromStored recognizes table`() {
        assertEquals(HabitListLayout.TABLE, HabitListLayout.fromStored("TABLE"))
    }

    @Test fun `toggled flips between card and table`() {
        assertEquals(HabitListLayout.TABLE, HabitListLayout.CARD.toggled())
        assertEquals(HabitListLayout.CARD, HabitListLayout.TABLE.toggled())
    }

    @Test fun `card day strip is nine days ending today`() {
        val today = LocalDate.of(2026, 10, 23)
        val strip = buildDayStrip(emptyList(), today, days = KineticViewModel.CARD_DAY_STRIP_DAYS)
        assertEquals(9, strip.size)
        assertEquals("2026-10-15", strip.first().dateString)
        assertEquals("2026-10-23", strip.last().dateString)
        assertTrue(strip.last().isToday)
    }
}
