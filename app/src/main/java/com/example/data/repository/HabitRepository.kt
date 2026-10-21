package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.CategoryDao
import com.example.data.db.HabitDao
import com.example.data.db.KinetiqDatabase
import com.example.data.db.LogEntryDao
import com.example.data.model.Category
import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.data.model.RoutineStackEntity
import com.example.domain.time.DateCodec
import com.example.domain.time.SystemKinetiqClock
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class HabitRepository(
    private val database: KinetiqDatabase,
    private val habitDao: HabitDao,
    private val logEntryDao: LogEntryDao,
    private val categoryDao: CategoryDao
) {
    val activeHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()
    val archivedHabits: Flow<List<Habit>> = habitDao.getArchivedHabits()
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allStacks: Flow<List<RoutineStackEntity>> = habitDao.getAllRoutineStacks()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getHabitById(habitId: Long): Habit? = habitDao.getHabitById(habitId)
    fun getHabitByIdFlow(habitId: Long): Flow<Habit?> = habitDao.getHabitByIdFlow(habitId)

    suspend fun saveHabit(habit: Habit): Long = habitDao.insertHabit(habit)
    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    /** FK cascade removes log entries atomically with the habit. */
    suspend fun deleteHabit(habit: Habit) {
        database.withTransaction { habitDao.deleteHabit(habit) }
    }

    suspend fun archiveHabit(habit: Habit) = habitDao.updateHabit(habit.copy(isArchived = true))
    suspend fun restoreHabit(habit: Habit) = habitDao.updateHabit(habit.copy(isArchived = false))

    suspend fun moveHabit(habit: Habit, delta: Int, siblings: List<Habit>) {
        val index = siblings.indexOfFirst { it.id == habit.id }
        val target = index + delta
        if (index < 0 || target !in siblings.indices) return
        val other = siblings[target]
        database.withTransaction {
            habitDao.updateHabit(habit.copy(orderIndex = other.orderIndex))
            habitDao.updateHabit(other.copy(orderIndex = habit.orderIndex))
        }
    }

    suspend fun saveCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun saveStack(stack: RoutineStackEntity): Long = habitDao.insertStack(stack)

    /** Replace all domain data atomically (used by backup import). */
    suspend fun replaceAllData(
        categories: List<Category>,
        stacks: List<RoutineStackEntity>,
        habits: List<Habit>,
        logs: List<LogEntry>
    ) {
        database.withTransaction {
            logEntryDao.clearAllLogs()
            // Delete habits via DAO so FK cascade also clears any stragglers.
            habitDao.getAllHabitsList().forEach { habitDao.deleteHabit(it) }
            habitDao.clearAllStacks()
            categoryDao.clearAllCategories()
            categories.forEach { categoryDao.insertCategory(it) }
            stacks.forEach { habitDao.insertStack(it) }
            habits.forEach { habitDao.insertHabit(it) }
            logs.forEach { logEntryDao.insertOrUpdate(it) }
        }
    }

    suspend fun snapshotHabits(): List<Habit> = habitDao.getAllHabitsList()
    suspend fun snapshotStacks(): List<RoutineStackEntity> = habitDao.getAllStacksList()
    suspend fun snapshotCategories(): List<Category> = categoryDao.getAllCategoriesList()
    suspend fun snapshotLogs(): List<LogEntry> = logEntryDao.getAllLogsList()

    suspend fun reseedData() {
        database.withTransaction {
            // Remove only previously seeded demo habits (by title), preserving user data.
            val demoTitles = DEMO_TITLES
            habitDao.getAllHabitsList().filter { it.title in demoTitles }.forEach { habitDao.deleteHabit(it) }
            seedLocked(forceDemoHabits = true)
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        if (habitDao.getHabitsCount() > 0) return
        database.withTransaction { seedLocked(forceDemoHabits = false) }
    }

    private suspend fun seedLocked(forceDemoHabits: Boolean) {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategory(Category(name = "Health", colorHex = "#10B981", iconName = "Favorite", orderIndex = 0))
            categoryDao.insertCategory(Category(name = "Focus", colorHex = "#8B5CF6", iconName = "Psychology", orderIndex = 1))
            categoryDao.insertCategory(Category(name = "Mindfulness", colorHex = "#F59E0B", iconName = "SelfImprovement", orderIndex = 2))
            categoryDao.insertCategory(Category(name = "Fitness", colorHex = "#EF4444", iconName = "FitnessCenter", orderIndex = 3))
        }
        val categoriesByName = categoryDao.getAllCategoriesList().associateBy { it.name }
        val existingTitles = habitDao.getAllHabitsList().map { it.title }.toSet()
        if (!forceDemoHabits && existingTitles.isNotEmpty()) return
        if (DEMO_TITLES.all { it in existingTitles } && !forceDemoHabits) return
        if (DEMO_TITLES.all { it in existingTitles }) return

        val morningStackId = habitDao.insertStack(RoutineStackEntity(name = "Morning Launchpad", description = "Build unstoppable early momentum with mindful energy", iconName = "WbSunny", colorHex = "#F59E0B"))
        val focusStackId = habitDao.insertStack(RoutineStackEntity(name = "Deep Focus Ritual", description = "Protect high-leverage cognitive output", iconName = "Psychology", colorHex = "#8B5CF6"))

        val h1Id = habitDao.insertHabit(Habit(title = "Mindful Breathwork", description = "Box breathing to reset autonomic nervous system", cueTrigger = "After I pour my morning glass of water", modality = HabitModality.TIMER, targetValue = 5.0, floorValue = 1.0, unit = "mins", categoryId = categoriesByName["Mindfulness"]?.id, colorHex = "#F59E0B", iconName = "SelfImprovement", reminderHour = 7, reminderMinute = 30, routineStackId = morningStackId, orderIndex = 0))
        val h2Id = habitDao.insertHabit(Habit(title = "Daily Hydration", description = "Drink pure water throughout the day for cellular energy", cueTrigger = "When sitting down at my work desk", modality = HabitModality.NUMERIC, targetValue = 2500.0, floorValue = 1000.0, unit = "ml", categoryId = categoriesByName["Health"]?.id, colorHex = "#10B981", iconName = "WaterDrop", reminderHour = 10, reminderMinute = 0, routineStackId = morningStackId, orderIndex = 1))
        val h3Id = habitDao.insertHabit(Habit(title = "Deep Work Sprint", description = "Uninterrupted focus sessions without notifications", cueTrigger = "After reviewing daily high-priority objectives", modality = HabitModality.TALLY, targetValue = 3.0, floorValue = 1.0, unit = "sprints", categoryId = categoriesByName["Focus"]?.id, colorHex = "#8B5CF6", iconName = "Psychology", reminderHour = 14, reminderMinute = 0, routineStackId = focusStackId, orderIndex = 2))
        val h4Id = habitDao.insertHabit(Habit(title = "Pushups & Core", description = "Strength and posture maintenance", cueTrigger = "Before taking evening shower", modality = HabitModality.NUMERIC, targetValue = 30.0, floorValue = 10.0, unit = "reps", categoryId = categoriesByName["Fitness"]?.id, colorHex = "#EF4444", iconName = "FitnessCenter", reminderHour = 19, reminderMinute = 30, restDaysMask = 64, orderIndex = 3))

        val today = SystemKinetiqClock.today()
        for (i in 21 downTo 1) {
            val date = today.minusDays(i.toLong())
            val dateStr = DateCodec.format(date)
            val dow = date.dayOfWeek.value // Mon=1..Sun=7
            if (i % 6 != 0) {
                val isFloor = (i == 4 || i == 11)
                logEntryDao.insertOrUpdate(LogEntry(habitId = h1Id, dateString = dateStr, isCompleted = !isFloor, isFloorSustained = isFloor, durationSeconds = if (isFloor) 120 else 300, numericValue = if (isFloor) 2.0 else 5.0))
            } else if (i == 6) {
                logEntryDao.insertOrUpdate(LogEntry(habitId = h1Id, dateString = dateStr, frictionTag = "Low Energy", notes = "Woke up with headache, rested instead"))
            }
            if (i % 5 != 0) {
                val isFloor = (i == 3 || i == 8 || i == 15)
                val ml = if (isFloor) 1200.0 else (2500.0 + (i * 50 % 400))
                logEntryDao.insertOrUpdate(LogEntry(habitId = h2Id, dateString = dateStr, isCompleted = !isFloor, isFloorSustained = isFloor, numericValue = ml))
            }
            if (dow <= 5) {
                val sprints = if (i % 4 == 0) 1 else 3
                logEntryDao.insertOrUpdate(LogEntry(habitId = h3Id, dateString = dateStr, isCompleted = sprints >= 3, isFloorSustained = sprints == 1, tallyCount = sprints, numericValue = sprints.toDouble()))
            }
            if (dow != 7 && i % 4 != 2) {
                val reps = if (i % 7 == 3) 15.0 else 30.0
                logEntryDao.insertOrUpdate(LogEntry(habitId = h4Id, dateString = dateStr, isCompleted = reps >= 30.0, isFloorSustained = reps < 30.0, numericValue = reps))
            }
        }
        @Suppress("UNUSED_EXPRESSION") LocalDate.now()
    }

    companion object {
        val DEMO_TITLES = setOf("Mindful Breathwork", "Daily Hydration", "Deep Work Sprint", "Pushups & Core")
    }
}
