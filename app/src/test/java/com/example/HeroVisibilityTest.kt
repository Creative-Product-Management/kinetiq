package com.example

import com.example.data.repository.KineticSettings
import androidx.compose.ui.unit.dp
import com.example.ui.components.HERO_AUTO_ADVANCE_DELAY_MILLIS
import com.example.ui.components.HERO_SLIDE_HEIGHT
import com.example.ui.components.nextHeroPage
import com.example.ui.viewmodel.isNudgeDismissed
import com.example.ui.viewmodel.nudgeDismissalKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroVisibilityTest {

    @Test fun `hero is shown by default and can be permanently hidden via settings`() {
        assertTrue(KineticSettings().showHeroSection)
        assertFalse(KineticSettings(showHeroSection = false).showHeroSection)
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
