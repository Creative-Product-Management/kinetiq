package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.KineticDatabase
import com.example.data.model.Category
import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.repository.HabitRepository
import com.example.data.repository.LibraryDefaults
import com.example.data.repository.LogRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.EditorKind
import com.example.domain.MeasurableLogStyle
import com.example.domain.resolveEditorModality
import com.example.domain.toEditorKind
import com.example.domain.time.FixedKineticClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryAndEditorModalityTest {

    private fun newDb(context: Context): KineticDatabase =
        Room.inMemoryDatabaseBuilder(context, KineticDatabase::class.java).allowMainThreadQueries().build()

    @Test
    fun `editor modality maps check-in and measurable styles`() {
        assertEquals(HabitModality.YES_NO, resolveEditorModality(EditorKind.CHECK_IN, MeasurableLogStyle.AMOUNT))
        assertEquals(HabitModality.NUMERIC, resolveEditorModality(EditorKind.MEASURABLE, MeasurableLogStyle.AMOUNT))
        assertEquals(HabitModality.TALLY, resolveEditorModality(EditorKind.MEASURABLE, MeasurableLogStyle.COUNT))
        assertEquals(HabitModality.TIMER, resolveEditorModality(EditorKind.MEASURABLE, MeasurableLogStyle.TIMER))

        assertEquals(EditorKind.CHECK_IN, HabitModality.YES_NO.toEditorKind())
        assertEquals(EditorKind.MEASURABLE, HabitModality.NUMERIC.toEditorKind())
        assertEquals(EditorKind.MEASURABLE, HabitModality.TALLY.toEditorKind())
        assertEquals(EditorKind.MEASURABLE, HabitModality.TIMER.toEditorKind())

        assertEquals(MeasurableLogStyle.AMOUNT, MeasurableLogStyle.fromModality(HabitModality.NUMERIC))
        assertEquals(MeasurableLogStyle.COUNT, MeasurableLogStyle.fromModality(HabitModality.TALLY))
        assertEquals(MeasurableLogStyle.TIMER, MeasurableLogStyle.fromModality(HabitModality.TIMER))
        assertNull(MeasurableLogStyle.fromModality(HabitModality.YES_NO))
    }

    @Test
    fun `seedDefaultLibraryIfEmpty inserts defaults once tables empty`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())

        assertTrue(habitRepo.snapshotCategories().isEmpty())
        assertTrue(habitRepo.snapshotStacks().isEmpty())

        habitRepo.seedDefaultLibraryIfEmpty()
        assertEquals(LibraryDefaults.categories.size, habitRepo.snapshotCategories().size)
        assertEquals(LibraryDefaults.stacks.size, habitRepo.snapshotStacks().size)
        assertEquals("Health", habitRepo.snapshotCategories().minByOrNull { it.orderIndex }?.name)

        // Second call with non-empty tables does not duplicate.
        habitRepo.seedDefaultLibraryIfEmpty()
        assertEquals(LibraryDefaults.categories.size, habitRepo.snapshotCategories().size)
        assertEquals(LibraryDefaults.stacks.size, habitRepo.snapshotStacks().size)
        db.close()
    }

    @Test
    fun `seed flag prevents re-insert after deliberate wipe`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())
        val settings = SettingsRepository(context)

        // Reset shared DataStore so prior tests cannot pollute the gate.
        settings.setLibraryDefaultsSeeded(false)
        habitRepo.seedDefaultLibraryIfEmpty()
        settings.setLibraryDefaultsSeeded(true)
        assertTrue(settings.isLibraryDefaultsSeeded())
        assertFalse(habitRepo.snapshotCategories().isEmpty())

        // User deletes everything.
        habitRepo.snapshotCategories().forEach { habitRepo.deleteCategory(it) }
        habitRepo.snapshotStacks().forEach { habitRepo.deleteStack(it) }
        assertTrue(habitRepo.snapshotCategories().isEmpty())
        assertTrue(habitRepo.snapshotStacks().isEmpty())

        // App start gate: flag stays set, so we do not call seed again.
        if (!settings.isLibraryDefaultsSeeded()) {
            habitRepo.seedDefaultLibraryIfEmpty()
        }
        assertTrue(habitRepo.snapshotCategories().isEmpty())
        assertTrue(habitRepo.snapshotStacks().isEmpty())
        db.close()
    }

    @Test
    fun `deleting category clears habit categoryId`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())

        val catId = habitRepo.saveCategory(Category(name = "Focus", colorHex = "#3D4F6F", orderIndex = 0))
        val habitId = habitRepo.saveHabit(
            Habit(title = "Deep work", modality = HabitModality.YES_NO, targetValue = 1.0, categoryId = catId)
        )
        assertEquals(catId, habitRepo.getHabitById(habitId)?.categoryId)

        val category = habitRepo.snapshotCategories().first { it.id == catId }
        habitRepo.deleteCategory(category)

        assertNull(habitRepo.getHabitById(habitId)?.categoryId)
        assertTrue(habitRepo.snapshotCategories().isEmpty())
        db.close()
    }

    @Test
    fun `updating habit color does not clear logs`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())
        val logRepo = LogRepository(db, db.logEntryDao(), FixedKineticClock(LocalDate.of(2026, 10, 21)))

        val habitId = habitRepo.saveHabit(
            Habit(title = "Water", modality = HabitModality.YES_NO, targetValue = 1.0, colorHex = "#10B981")
        )
        val habit = habitRepo.getHabitById(habitId)!!
        logRepo.toggleHabitForDate(habit, "2026-10-21")
        logRepo.toggleHabitForDate(habit, "2026-10-20")
        assertEquals(2, habitRepo.snapshotLogs().count { it.habitId == habitId })

        habitRepo.saveHabit(habit.copy(colorHex = "#F43F5E", title = "Water intake"))
        val updated = habitRepo.getHabitById(habitId)!!
        assertEquals("#F43F5E", updated.colorHex)
        assertEquals("Water intake", updated.title)
        assertEquals(2, habitRepo.snapshotLogs().count { it.habitId == habitId })
        assertNotNull(logRepo.getRecord(habitId, "2026-10-21"))
        db.close()
    }
}
