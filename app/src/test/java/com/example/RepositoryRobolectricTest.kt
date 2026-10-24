package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.KineticDatabase
import com.example.data.db.MIGRATION_2_3
import com.example.data.db.MIGRATION_3_4
import com.example.data.model.Category
import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.data.repository.HabitRepository
import com.example.data.repository.LogRepository
import com.example.domain.analytics.HabitAnalyticsEngine
import com.example.domain.time.DateCodec
import com.example.domain.time.FixedKineticClock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryRobolectricTest {

    private fun newDb(context: Context): KineticDatabase =
        Room.inMemoryDatabaseBuilder(context, KineticDatabase::class.java).allowMainThreadQueries().build()

    @Test fun `tally increments are atomic under concurrency and completion is idempotent`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())
        val logRepo = LogRepository(db, db.logEntryDao(), FixedKineticClock(LocalDate.of(2026, 10, 21)))
        val today = "2026-10-21"
        val id = habitRepo.saveHabit(Habit(title = "Sprints", modality = HabitModality.TALLY, targetValue = 3.0, floorValue = 1.0, unit = "sprints"))
        val habit = habitRepo.getHabitById(id)!!

        (1..10).map { async { logRepo.logTallyIncrement(habit, today) } }.awaitAll()
        var record = logRepo.getRecord(id, today)
        assertEquals(10, record?.tallyCount)
        assertEquals(true, record?.isCompleted)

        logRepo.setCompletedForDate(habit, today)
        logRepo.setCompletedForDate(habit, today)
        record = logRepo.getRecord(id, today)
        assertEquals(10, record?.tallyCount) // idempotent set does not reset or duplicate

        // FK cascade: deleting habit removes logs
        habitRepo.deleteHabit(habit)
        assertEquals(null, logRepo.getRecord(id, today))
        db.close()
    }

    @Test fun `yes-no day tap counts as target completion for momentum`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())
        val today = LocalDate.of(2026, 10, 21)
        val clock = FixedKineticClock(today)
        val logRepo = LogRepository(db, db.logEntryDao(), clock)
        val createdMillis = today.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val id = habitRepo.saveHabit(Habit(title = "Meditate", modality = HabitModality.YES_NO, targetValue = 1.0, createdAt = createdMillis))
        val habit = habitRepo.getHabitById(id)!!

        // Tapping today's day circle (the only Yes/No log action) logs target.
        logRepo.toggleHabitForDate(habit, "2026-10-21")
        val record = logRepo.getRecord(id, "2026-10-21")
        assertEquals(true, record?.isCompleted)

        val records = db.logEntryDao().getLogsListForHabit(id)
        val result = HabitAnalyticsEngine.calculateAnalytics(habit, records, clock = clock)
        assertEquals(7, result.habitStrengthPercent) // one target day: 0.07 EMA
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.totalTargetCompletions)

        // Tapping again toggles off and momentum returns to zero.
        logRepo.toggleHabitForDate(habit, "2026-10-21")
        assertEquals(null, logRepo.getRecord(id, "2026-10-21"))
        val cleared = HabitAnalyticsEngine.calculateAnalytics(habit, db.logEntryDao().getLogsListForHabit(id), clock = clock)
        assertEquals(0, cleared.habitStrengthPercent)
        db.close()
    }

    @Test fun `backfilled day taps from before creation raise momentum`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())
        val today = LocalDate.of(2026, 10, 21)
        val clock = FixedKineticClock(today)
        val logRepo = LogRepository(db, db.logEntryDao(), clock)
        // Habit created today (default createdAt would be now; pin it to today).
        val createdMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val id = habitRepo.saveHabit(Habit(title = "Read", modality = HabitModality.YES_NO, targetValue = 1.0, createdAt = createdMillis))
        val habit = habitRepo.getHabitById(id)!!

        // User taps every circle in the 7-day strip (6 past days + today).
        for (offset in 6 downTo 0) {
            logRepo.toggleHabitForDate(habit, today.minusDays(offset.toLong()).toString())
        }
        val records = db.logEntryDao().getLogsListForHabit(id)
        assertEquals(7, records.size)
        // Previously these pre-creation logs were ignored and this stayed 0.
        assertEquals(40, HabitAnalyticsEngine.calculateMomentumPercent(habit, records, clock = clock))
        assertEquals(40, HabitAnalyticsEngine.calculateAnalytics(habit, records, clock = clock).habitStrengthPercent)
        db.close()
    }

    @Test fun `sample habit loads once only and deleting all habits stays empty`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())

        // Fresh database starts empty: nothing is auto-seeded.
        assertTrue(habitRepo.snapshotHabits().isEmpty())

        assertTrue(habitRepo.loadSampleHabit())
        val habits = habitRepo.snapshotHabits()
        assertEquals(1, habits.size)
        assertEquals(HabitRepository.SAMPLE_HABIT_TITLE, habits.first().title)
        assertEquals(HabitModality.TIMER, habits.first().modality)
        assertEquals(null, habits.first().reminderHour)
        assertTrue(habitRepo.snapshotLogs().isEmpty())
        assertTrue(habitRepo.snapshotStacks().isEmpty())
        assertTrue(habitRepo.snapshotCategories().isEmpty())

        // Loading again does not duplicate the sample habit.
        assertFalse(habitRepo.loadSampleHabit())
        assertEquals(1, habitRepo.snapshotHabits().size)

        // Deleting it sticks (no resurrection), but Load can opt back in.
        habitRepo.deleteHabit(habits.first())
        assertTrue(habitRepo.snapshotHabits().isEmpty())
        assertTrue(habitRepo.loadSampleHabit())
        habitRepo.deleteHabit(habitRepo.snapshotHabits().first())
        assertTrue(habitRepo.snapshotHabits().isEmpty())
        db.close()
    }

    @Test fun `saveOrder persists manual ordering`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())
        val a = habitRepo.saveHabit(Habit(title = "Alpha", orderIndex = 0))
        val b = habitRepo.saveHabit(Habit(title = "Beta", orderIndex = 1))
        val c = habitRepo.saveHabit(Habit(title = "Gamma", orderIndex = 2))
        val habits = habitRepo.activeHabits.first()
        assertEquals(listOf(a, b, c), habits.map { it.id })
        habitRepo.saveOrder(listOf(habits[2], habits[0], habits[1]))
        val reordered = habitRepo.activeHabits.first()
        assertEquals(listOf(c, a, b), reordered.map { it.id })
        assertEquals(listOf(0, 1, 2), reordered.map { it.orderIndex })
        db.close()
    }

    @Test fun `dashboard window only loads recent records`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = newDb(context)
        val habitRepo = HabitRepository(db, db.habitDao(), db.logEntryDao(), db.categoryDao())
        val logRepo = LogRepository(db, db.logEntryDao(), FixedKineticClock(LocalDate.of(2026, 10, 21)))
        val id = habitRepo.saveHabit(Habit(title = "Water", modality = HabitModality.NUMERIC, targetValue = 2500.0, unit = "ml"))
        db.logEntryDao().insertOrUpdate(LogEntry(habitId = id, dateString = "2026-09-01", isCompleted = true, numericValue = 2500.0))
        db.logEntryDao().insertOrUpdate(LogEntry(habitId = id, dateString = "2026-10-21", isCompleted = true, numericValue = 2500.0))
        val window = logRepo.dashboardRecords().first()
        assertEquals(1, window.size)
        assertEquals("2026-10-21", window.first().dateString)
        db.close()
    }

    @Test fun `encrypted backup exports and imports into a fresh database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db1 = newDb(context)
        val repo1 = HabitRepository(db1, db1.habitDao(), db1.logEntryDao(), db1.categoryDao())
        val catId = repo1.saveCategory(Category(name = "Health", colorHex = "#10B981", iconName = "Favorite", orderIndex = 0))
        val habitId = repo1.saveHabit(Habit(title = "Backup Habit", modality = HabitModality.NUMERIC, targetValue = 2.5, floorValue = 0.5, unit = "L", categoryId = catId))
        db1.logEntryDao().insertOrUpdate(LogEntry(habitId = habitId, dateString = "2026-10-20", isCompleted = true, numericValue = 2.5))
        val bytes = com.example.data.backup.BackupManager(repo1).exportEncrypted("test-passphrase".toCharArray())
        assertTrue(!bytes.decodeToString().contains("Backup Habit"))

        val db2 = newDb(context)
        val repo2 = HabitRepository(db2, db2.habitDao(), db2.logEntryDao(), db2.categoryDao())
        com.example.data.backup.BackupManager(repo2).importEncrypted(bytes, "test-passphrase".toCharArray())
        val restored = repo2.snapshotHabits()
        assertEquals(1, restored.size)
        assertEquals(2.5, restored.first().targetValue, 0.001)
        assertEquals(1, repo2.snapshotLogs().size)
        db1.close(); db2.close()
    }

    @Test fun `migration 2 to 3 preserves habits logs and backfills category`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, colorHex TEXT NOT NULL, iconName TEXT NOT NULL, orderIndex INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS routine_stacks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, iconName TEXT NOT NULL, colorHex TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, cueTrigger TEXT NOT NULL, modality TEXT NOT NULL, targetValue REAL NOT NULL, floorValue REAL, unit TEXT NOT NULL, categoryId INTEGER, category TEXT NOT NULL, colorHex TEXT NOT NULL, iconName TEXT NOT NULL, reminderHour INTEGER, reminderMinute INTEGER, reminderDaysMask INTEGER NOT NULL, restDaysMask INTEGER NOT NULL, routineStackId INTEGER, orderIndex INTEGER NOT NULL, isArchived INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS log_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, dateString TEXT NOT NULL, isCompleted INTEGER NOT NULL, isFloorSustained INTEGER NOT NULL, numericValue REAL NOT NULL, durationSeconds INTEGER NOT NULL, tallyCount INTEGER NOT NULL, frictionTag TEXT, notes TEXT NOT NULL, recordedAt INTEGER NOT NULL)")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_log_entries_habitId_dateString ON log_entries (habitId, dateString)")
                        db.execSQL("INSERT INTO habits (id, title, description, cueTrigger, modality, targetValue, floorValue, unit, categoryId, category, colorHex, iconName, reminderHour, reminderMinute, reminderDaysMask, restDaysMask, routineStackId, orderIndex, isArchived, createdAt) VALUES (7, 'Legacy Habit', '', '', 'NUMERIC', 2.5, 0.5, 'L', NULL, 'Health', '#10B981', 'Sparkles', NULL, NULL, 127, 0, NULL, 0, 0, 1700000000000)")
                        db.execSQL("INSERT INTO log_entries (id, habitId, dateString, isCompleted, isFloorSustained, numericValue, durationSeconds, tallyCount, frictionTag, notes, recordedAt) VALUES (3, 7, '2026-10-20', 1, 0, 2.5, 0, 0, NULL, '', 1700000000000)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                }).build()
        )
        val writable = helper.writableDatabase
        writable.close(); helper.close()

        val db = Room.databaseBuilder(context, KineticDatabase::class.java, name)
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4).allowMainThreadQueries().build()
        runBlocking {
            val habit = db.habitDao().getHabitById(7)
            assertNotNull(habit)
            assertEquals("Legacy Habit", habit!!.title)
            assertEquals(2.5, habit.targetValue, 0.001)
            assertEquals("", habit.checkInQuestion)
            assertNotNull("categoryId must be backfilled from legacy text", habit.categoryId)
            val logs = db.logEntryDao().getLogsListForHabit(7)
            assertEquals(1, logs.size)
            assertTrue(logs.first().isCompleted)
        }
        db.close()
        context.deleteDatabase(name)
        @Suppress("UNUSED_EXPRESSION") DateCodec::class
        @Suppress("UNUSED_EXPRESSION") Category::class
    }
}
