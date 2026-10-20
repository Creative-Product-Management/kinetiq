package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.HabitEntity
import com.example.data.model.HabitModality
import com.example.data.model.HabitRecordEntity
import com.example.domain.analytics.HabitAnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kinetiq", appName)
  }

  @Test
  fun `test habit analytics calculation with floor sustain`() {
    val habit = HabitEntity(
      id = 1L,
      title = "Daily Meditation",
      modality = HabitModality.TIMER,
      targetValue = 10.0,
      floorValue = 2.0,
      unit = "mins"
    )

    val records = listOf(
      HabitRecordEntity(
        habitId = 1L,
        dateString = "2026-10-18",
        isCompleted = true
      ),
      HabitRecordEntity(
        habitId = 1L,
        dateString = "2026-10-19",
        isFloorSustained = true
      )
    )

    val analytics = HabitAnalyticsEngine.calculateAnalytics(habit, records)
    assertTrue("Habit strength should be greater than zero", analytics.habitStrengthPercent > 0)
    assertEquals(1, analytics.totalTargetCompletions)
    assertEquals(1, analytics.totalFloorSustains)
  }
}
