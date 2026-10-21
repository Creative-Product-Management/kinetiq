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
