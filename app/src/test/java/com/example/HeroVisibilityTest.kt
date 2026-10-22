package com.example

import com.example.data.model.Habit
import com.example.data.model.LogEntry
import com.example.data.repository.KineticSettings
import androidx.compose.ui.unit.dp
import com.example.ui.components.HERO_AUTO_ADVANCE_DELAY_MILLIS
import com.example.ui.components.HERO_SLIDE_HEIGHT
import com.example.ui.components.NudgeType
import com.example.ui.components.nextHeroPage
import com.example.ui.viewmodel.buildInAppNudges
import com.example.ui.viewmodel.isNudgeDismissed
import com.example.ui.viewmodel.nudgeDismissalKey
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroVisibilityTest {

    @Test fun `hero is shown by default and can be permanently hidden via settings`() {
        assertTrue(KineticSettings().showHeroSection)
        assertFalse(KineticSettings(showHeroSection = false).showHeroSection)
    }

    @Test fun `hero is always a slideshow with stable slides`() {
        val today = LocalDate.of(2026, 10, 21) // Wednesday, no rest days
        val habit = Habit(id = 1, title = "Read")
        val slides = buildInAppNudges(listOf(habit), emptyList(), emptyList(), today)
        // More than one slide: swipeable, dots shown, auto-advance active.
        assertTrue(slides.size > 1)
        assertEquals(
            listOf("today-momentum", "momentum-progress", "rest-shield", "routine-stacks"),
            slides.map { it.id }
        )
        assertEquals(
            listOf(NudgeType.MOMENTUM_BOOST, NudgeType.INSIGHT, NudgeType.REST_SHIELD, NudgeType.STACK_PRIMED),
            slides.map { it.type }
        )
    }

    @Test fun `completing today keeps the same hero slides and only updates today's text`() {
        val today = LocalDate.of(2026, 10, 21)
        val habit = Habit(id = 1, title = "Read")

        val pending = buildInAppNudges(listOf(habit), emptyList(), emptyList(), today)
        val done = buildInAppNudges(
            listOf(habit),
            listOf(LogEntry(habitId = 1, dateString = "2026-10-21", isCompleted = true)),
            emptyList(),
            today
        )
        // Same slides, same order, same types/titles — no swap to another slide.
        assertEquals(pending.map { it.id }, done.map { it.id })
        assertEquals(pending.map { it.type }, done.map { it.type })
        assertEquals(pending.map { it.title }, done.map { it.title })
        assertTrue(done.isNotEmpty())
        // Only today's status text reflects the completion.
        assertTrue(pending.first().subtitle.contains("not logged yet"))
        assertTrue(done.first().subtitle.contains("Everything due today is logged"))
    }

    @Test fun `momentum progress slide uses the strongest habit's real percent`() {
        val today = LocalDate.of(2026, 10, 21)
        val habits = listOf(Habit(id = 1, title = "Read"), Habit(id = 2, title = "Run"))
        val slides = buildInAppNudges(habits, emptyList(), emptyList(), today, momentumPercents = mapOf(1L to 20, 2L to 85))
        val progress = slides.first { it.id == "momentum-progress" }
        assertTrue(progress.subtitle.contains("Run"))
        assertTrue(progress.subtitle.contains("85%"))
        assertEquals(2L, progress.targetHabitId)
    }

    @Test fun `friction insight is appended after the stable core slides`() {
        val today = LocalDate.of(2026, 10, 21)
        val habit = Habit(id = 1, title = "Read")
        val slides = buildInAppNudges(
            listOf(habit),
            listOf(LogEntry(habitId = 1, dateString = "2026-10-20", frictionTag = "Tired")),
            emptyList(),
            today
        )
        assertEquals(5, slides.size)
        assertEquals("insight-friction", slides.last().id)
        assertEquals(listOf("today-momentum", "momentum-progress", "rest-shield", "routine-stacks"), slides.dropLast(1).map { it.id })
    }

    @Test fun `no habits means no hero nudges`() {
        assertTrue(buildInAppNudges(emptyList(), emptyList(), emptyList(), LocalDate.of(2026, 10, 21)).isEmpty())
    }

    @Test fun `dismissal hides the nudge only for that day`() {
        val dismissed = setOf(nudgeDismissalKey("momentum-1", "2026-10-21"))
        assertTrue(isNudgeDismissed("momentum-1", dismissed, "2026-10-21"))
        // A new session on a new day must see the hero again.
        assertFalse(isNudgeDismissed("momentum-1", dismissed, "2026-10-22"))
    }

    @Test fun `legacy bare dismissal ids do not permanently hide the hero`() {
        val dismissed = setOf("momentum-1") // written by older versions without day scoping
        assertFalse(isNudgeDismissed("momentum-1", dismissed, "2026-10-21"))
    }

    @Test fun `dismissal keys are stable per id and day`() {
        assertEquals("rest-3@2026-10-21", nudgeDismissalKey("rest-3", "2026-10-21"))
    }

    @Test fun `auto advance waits long enough to read a slide and loops`() {
        // Comfortable reading dwell: at least 7s, never so long the hero feels frozen.
        assertTrue(HERO_AUTO_ADVANCE_DELAY_MILLIS in 7_000L..12_000L)
        assertEquals(1, nextHeroPage(0, 4))
        assertEquals(0, nextHeroPage(3, 4)) // wraps last -> first (loop)
        assertEquals(0, nextHeroPage(0, 1)) // single slide stays put
    }

    @Test fun `every hero slide uses the same fixed height`() {
        // Worst case (2-line title + 3-line subtitle + button + padding) needs ~242dp.
        assertTrue(HERO_SLIDE_HEIGHT >= 240.dp)
        assertEquals(244.dp, HERO_SLIDE_HEIGHT)
    }
}
