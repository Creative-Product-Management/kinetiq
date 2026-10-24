package com.example

import com.example.ui.screens.FAQ_ITEMS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaqContentTest {

    @Test fun `faq covers getting started momentum rest reminders and privacy`() {
        val categories = FAQ_ITEMS.map { it.category }.toSet()
        assertTrue(categories.containsAll(setOf("Getting started", "Momentum score", "Rest Shield & streaks", "Reminders", "Privacy & data")))
    }

    @Test fun `create faq describes type sheet and locked types`() {
        val create = FAQ_ITEMS.first { it.question == "How do I create my first habit?" }.answer
        assertTrue(create.contains("Check-in"))
        assertTrue(create.contains("Measurable"))
        assertTrue(create.contains("check-in question", ignoreCase = true))
        assertTrue(create.contains("title", ignoreCase = true))
        val locked = FAQ_ITEMS.first { it.question == "Can I change a habit's type later?" }
        assertTrue(locked.answer.contains("locked", ignoreCase = true))
        assertTrue(locked.answer.contains("delete", ignoreCase = true))
        val types = FAQ_ITEMS.first { it.question == "What are the habit types?" }.answer
        assertTrue(types.contains("Check-in"))
        assertTrue(types.contains("check-in question", ignoreCase = true))
        assertTrue(types.contains("Amount"))
        assertTrue(types.contains("Count"))
        assertTrue(types.contains("Timer"))
    }

    @Test fun `faq questions are unique and answers are non-blank`() {
        assertEquals(FAQ_ITEMS.size, FAQ_ITEMS.map { it.question }.distinct().size)
        assertTrue(FAQ_ITEMS.all { it.question.isNotBlank() && it.answer.isNotBlank() })
    }

    @Test fun `long answers use formatting markup instead of blobs`() {
        val momentumAnswers = FAQ_ITEMS.filter { it.category == "Momentum score" }.map { it.answer }
        assertTrue("expected paragraph breaks in momentum answers", momentumAnswers.any { it.contains("\n\n") })
        assertTrue("expected bullet lists in momentum answers", momentumAnswers.any { it.contains("\n• ") })
        assertTrue("expected bold lead-ins in momentum answers", momentumAnswers.any { it.contains("**") })
        FAQ_ITEMS.forEach { faq ->
            val boldMarkers = faq.answer.split("**").size - 1
            assertTrue("unbalanced ** in: ${faq.question}", boldMarkers % 2 == 0)
        }
    }

    @Test fun `momentum faq states the formula and the honesty disclaimer`() {
        val momentum = FAQ_ITEMS.filter { it.category == "Momentum score" }.joinToString("\n") { "${it.question} ${it.answer}" }
        assertTrue(momentum.contains("0.93"))
        assertTrue(momentum.contains("0.65"))
        assertTrue(momentum.contains("90 days"))
        assertTrue(momentum.contains("not a clinical"))
        assertTrue(momentum.contains("Lally"))
    }
}
