package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.CategoryDao
import com.example.data.db.HabitDao
import com.example.data.db.KineticDatabase
import com.example.data.db.LogEntryDao
import com.example.data.model.Category
import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.data.model.RoutineStackEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(
    private val database: KineticDatabase,
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

    /**
     * Inserts a new habit, or updates an existing one in place.
     * Must not use INSERT REPLACE for edits: REPLACE deletes the row first and
     * cascades to [log_entries] via ON DELETE CASCADE.
     */
    suspend fun saveHabit(habit: Habit): Long {
        return if (habit.id == 0L) {
            habitDao.insertHabit(habit)
        } else {
            // Modality is immutable after create — preserve existing type if a caller tries to change it.
            val existing = habitDao.getHabitById(habit.id)
            val toSave = if (existing != null && existing.modality != habit.modality) {
                habit.copy(modality = existing.modality)
            } else {
                habit
            }
            habitDao.updateHabit(toSave)
            habit.id
        }
    }

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

    /** Persists a full manual ordering (from the Sort screen) in one transaction. */
    suspend fun saveOrder(ordered: List<Habit>) {
        database.withTransaction {
            ordered.forEachIndexed { index, habit ->
                if (habit.orderIndex != index) habitDao.updateHabit(habit.copy(orderIndex = index))
            }
        }
    }

    suspend fun saveCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
    suspend fun countHabitsWithCategory(categoryId: Long): Int = habitDao.countHabitsWithCategory(categoryId)

    suspend fun saveStack(stack: RoutineStackEntity): Long = habitDao.insertStack(stack)
    suspend fun updateStack(stack: RoutineStackEntity) = habitDao.updateStack(stack)
    suspend fun deleteStack(stack: RoutineStackEntity) = habitDao.deleteStack(stack)
    suspend fun countHabitsWithStack(stackId: Long): Int = habitDao.countHabitsWithStack(stackId)

    /** Persists category order from a manage-screen reorder. */
    suspend fun reorderCategories(ordered: List<Category>) {
        database.withTransaction {
            ordered.forEachIndexed { index, cat ->
                if (cat.orderIndex != index) categoryDao.updateCategory(cat.copy(orderIndex = index))
            }
        }
    }

    /**
     * Inserts built-in categories/stacks only when those tables are empty.
     * Caller should gate with the DataStore seed flag so a deliberate wipe is not undone.
     */
    suspend fun seedDefaultLibraryIfEmpty() = database.withTransaction {
        if (categoryDao.getCategoryCount() == 0) {
            LibraryDefaults.categories.forEach { categoryDao.insertCategory(it) }
        }
        if (habitDao.getStackCount() == 0) {
            LibraryDefaults.stacks.forEach { habitDao.insertStack(it) }
        }
    }

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

    /**
     * Opt-in sample habit (Settings -> Sample Habit -> Load). Inserts exactly
     * one habit with no logs, stacks, categories, or reminder, and is
     * idempotent: if a habit with the sample title already exists nothing is
     * added. Returns true when a new sample habit was inserted.
     */
    suspend fun loadSampleHabit(): Boolean = database.withTransaction {
        val existing = habitDao.getAllHabitsList()
        if (existing.any { it.title.equals(SAMPLE_HABIT_TITLE, ignoreCase = true) }) {
            false
        } else {
            val nextOrder = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
            habitDao.insertHabit(
                Habit(
                    title = SAMPLE_HABIT_TITLE,
                    description = "Box breathing to reset and refocus",
                    cueTrigger = "After I pour my morning glass of water",
                    modality = HabitModality.TIMER,
                    targetValue = 5.0,
                    floorValue = 1.0,
                    unit = "mins",
                    colorHex = "#F59E0B",
                    iconName = "SelfImprovement",
                    orderIndex = nextOrder
                )
            )
            true
        }
    }

    companion object {
        const val SAMPLE_HABIT_TITLE = "Mindful Breathwork"
    }
}
