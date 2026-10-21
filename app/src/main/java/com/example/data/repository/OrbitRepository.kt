package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.db.HabitDao
import com.example.data.db.LogEntryDao
import com.example.data.model.Category
import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.data.model.RoutineStackEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrbitRepository(
    val habitDao: HabitDao,
    val logEntryDao: LogEntryDao? = null,
    val categoryDao: CategoryDao? = null
) {

    val activeHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()
    val allRecords: Flow<List<LogEntry>> = habitDao.getAllRecords()
    val allStacks: Flow<List<RoutineStackEntity>> = habitDao.getAllRoutineStacks()
    val allCategories: Flow<List<Category>>? = categoryDao?.getAllCategories()

    fun getRecordsForDates(dates: List<String>): Flow<List<LogEntry>> {
        return habitDao.getRecordsForDates(dates)
    }

    fun getRecordsForHabit(habitId: Long): Flow<List<LogEntry>> {
        return habitDao.getRecordsForHabit(habitId)
    }

    suspend fun getHabitById(habitId: Long): Habit? {
        return habitDao.getHabitById(habitId)
    }

    suspend fun saveHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteAllRecordsForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    suspend fun toggleHabitForDate(habit: Habit, dateString: String) {
        val existing = habitDao.getRecord(habit.id, dateString)
        if (existing == null) {
            val newRecord = LogEntry(
                habitId = habit.id,
                dateString = dateString,
                isCompleted = true,
                isFloorSustained = false,
                numericValue = habit.targetValue,
                durationSeconds = if (habit.modality == HabitModality.TIMER) (habit.targetValue * 60).toInt() else 0,
                tallyCount = if (habit.modality == HabitModality.TALLY) habit.targetValue.toInt() else 1
            )
            habitDao.insertOrUpdateRecord(newRecord)
        } else if (existing.isCompleted) {
            // Uncheck
            habitDao.deleteRecord(habit.id, dateString)
        } else {
            // Was floor sustained or partial, promote to full complete
            habitDao.insertOrUpdateRecord(
                existing.copy(
                    isCompleted = true,
                    isFloorSustained = false,
                    numericValue = habit.targetValue,
                    durationSeconds = if (habit.modality == HabitModality.TIMER) (habit.targetValue * 60).toInt() else existing.durationSeconds,
                    tallyCount = if (habit.modality == HabitModality.TALLY) habit.targetValue.toInt() else 1
                )
            )
        }
    }

    suspend fun logFloorSustainForDate(habit: Habit, dateString: String) {
        val existing = habitDao.getRecord(habit.id, dateString)
        val floorVal = habit.floorValue ?: (habit.targetValue * 0.3)
        val record = (existing ?: LogEntry(habitId = habit.id, dateString = dateString)).copy(
            isCompleted = false,
            isFloorSustained = true,
            numericValue = floorVal,
            durationSeconds = if (habit.modality == HabitModality.TIMER) (floorVal * 60).toInt() else 0,
            tallyCount = if (habit.modality == HabitModality.TALLY) floorVal.toInt().coerceAtLeast(1) else 1
        )
        habitDao.insertOrUpdateRecord(record)
    }

    suspend fun logNumericProgress(habit: Habit, dateString: String, amount: Double) {
        val existing = habitDao.getRecord(habit.id, dateString)
        val isMet = amount >= habit.targetValue
        val isFloor = !isMet && (habit.floorValue != null && amount >= habit.floorValue)
        val base = existing ?: LogEntry(habitId = habit.id, dateString = dateString)
        val record = base.copy(
            numericValue = amount,
            // Keep the modality-specific field in sync so a custom amount logged
            // for a tally or timer habit shows up on the habit card and analytics.
            durationSeconds = if (habit.modality == HabitModality.TIMER) (amount * 60).toInt() else base.durationSeconds,
            tallyCount = if (habit.modality == HabitModality.TALLY) amount.toInt() else base.tallyCount,
            isCompleted = isMet,
            isFloorSustained = isFloor
        )
        habitDao.insertOrUpdateRecord(record)
    }

    suspend fun logTallyIncrement(habit: Habit, dateString: String) {
        val existing = habitDao.getRecord(habit.id, dateString)
        val currentCount = existing?.tallyCount ?: 0
        val nextCount = currentCount + 1
        val isMet = nextCount >= habit.targetValue
        val isFloor = !isMet && (habit.floorValue != null && nextCount >= habit.floorValue)
        val record = (existing ?: LogEntry(habitId = habit.id, dateString = dateString)).copy(
            tallyCount = nextCount,
            numericValue = nextCount.toDouble(),
            isCompleted = isMet,
            isFloorSustained = isFloor
        )
        habitDao.insertOrUpdateRecord(record)
    }

    suspend fun logDurationSeconds(habit: Habit, dateString: String, durationSec: Int) {
        val existing = habitDao.getRecord(habit.id, dateString)
        val totalSec = (existing?.durationSeconds ?: 0) + durationSec
        val targetSec = (habit.targetValue * 60).toInt()
        val floorSec = ((habit.floorValue ?: (habit.targetValue * 0.3)) * 60).toInt()
        val isMet = totalSec >= targetSec
        val isFloor = !isMet && totalSec >= floorSec
        val record = (existing ?: LogEntry(habitId = habit.id, dateString = dateString)).copy(
            durationSeconds = totalSec,
            numericValue = (totalSec / 60.0),
            isCompleted = isMet,
            isFloorSustained = isFloor
        )
        habitDao.insertOrUpdateRecord(record)
    }

    suspend fun logFrictionTag(habitId: Long, dateString: String, frictionTag: String, notes: String = "") {
        val existing = habitDao.getRecord(habitId, dateString)
        val record = (existing ?: LogEntry(habitId = habitId, dateString = dateString)).copy(
            frictionTag = frictionTag,
            notes = notes
        )
        habitDao.insertOrUpdateRecord(record)
    }

    suspend fun clearRecord(habitId: Long, dateString: String) {
        habitDao.deleteRecord(habitId, dateString)
    }

    suspend fun clearAllRecords() {
        habitDao.clearAllRecords()
    }

    suspend fun reseedData() {
        seedInitialDataIfEmpty(force = true)
    }

    suspend fun seedInitialDataIfEmpty(force: Boolean = false) {
        if (!force && habitDao.getHabitsCount() > 0) return

        // 1. Seed Categories if categoryDao is present
        categoryDao?.let { catDao ->
            if (catDao.getCategoryCount() == 0) {
                catDao.insertCategory(Category(name = "Health", colorHex = "#10B981", iconName = "Favorite", orderIndex = 0))
                catDao.insertCategory(Category(name = "Focus", colorHex = "#8B5CF6", iconName = "Psychology", orderIndex = 1))
                catDao.insertCategory(Category(name = "Mindfulness", colorHex = "#F59E0B", iconName = "SelfImprovement", orderIndex = 2))
                catDao.insertCategory(Category(name = "Fitness", colorHex = "#EF4444", iconName = "FitnessCenter", orderIndex = 3))
            }
        }

        // 2. Seed Routine Stacks
        val morningStackId = habitDao.insertStack(
            RoutineStackEntity(
                name = "Morning Launchpad",
                description = "Build unstoppable early momentum with mindful energy",
                iconName = "WbSunny",
                colorHex = "#F59E0B"
            )
        )
        val focusStackId = habitDao.insertStack(
            RoutineStackEntity(
                name = "Deep Focus Ritual",
                description = "Protect high-leverage cognitive output",
                iconName = "Psychology",
                colorHex = "#8B5CF6"
            )
        )

        // 3. Seed Habits
        val h1Id = habitDao.insertHabit(
            Habit(
                title = "Mindful Breathwork",
                description = "Box breathing to reset autonomic nervous system",
                cueTrigger = "After I pour my morning glass of water",
                modality = HabitModality.TIMER,
                targetValue = 5.0, // 5 mins
                floorValue = 1.0,  // 1 min floor
                unit = "mins",
                category = "Mindfulness",
                colorHex = "#F59E0B",
                iconName = "SelfImprovement",
                reminderHour = 7,
                reminderMinute = 30,
                routineStackId = morningStackId,
                orderIndex = 0
            )
        )

        val h2Id = habitDao.insertHabit(
            Habit(
                title = "Daily Hydration",
                description = "Drink pure water throughout the day for cellular energy",
                cueTrigger = "When sitting down at my work desk",
                modality = HabitModality.NUMERIC,
                targetValue = 2500.0,
                floorValue = 1000.0,
                unit = "ml",
                category = "Health",
                colorHex = "#10B981",
                iconName = "WaterDrop",
                reminderHour = 10,
                reminderMinute = 0,
                routineStackId = morningStackId,
                orderIndex = 1
            )
        )

        val h3Id = habitDao.insertHabit(
            Habit(
                title = "Deep Work Sprint",
                description = "Uninterrupted focus sessions without notifications",
                cueTrigger = "After reviewing daily high-priority objectives",
                modality = HabitModality.TALLY,
                targetValue = 3.0,
                floorValue = 1.0,
                unit = "sprints",
                category = "Focus",
                colorHex = "#8B5CF6",
                iconName = "Psychology",
                reminderHour = 14,
                reminderMinute = 0,
                routineStackId = focusStackId,
                orderIndex = 2
            )
        )

        val h4Id = habitDao.insertHabit(
            Habit(
                title = "Pushups & Core",
                description = "Strength and posture maintenance",
                cueTrigger = "Before taking evening shower",
                modality = HabitModality.NUMERIC,
                targetValue = 30.0,
                floorValue = 10.0,
                unit = "reps",
                category = "Fitness",
                colorHex = "#EF4444",
                iconName = "FitnessCenter",
                reminderHour = 19,
                reminderMinute = 30,
                restDaysMask = 64, // Sunday rest day shield
                orderIndex = 3
            )
        )

        // 4. Seed Realistic Historical Activity for past 21 days
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 21 downTo 1) {
            val pastCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = sdf.format(pastCal.time)
            val dayOfWeek = pastCal.get(Calendar.DAY_OF_WEEK)

            // Habit 1 (Mindful Breathwork)
            if (i % 6 != 0) {
                val isFloor = (i == 4 || i == 11)
                habitDao.insertOrUpdateRecord(
                    LogEntry(
                        habitId = h1Id,
                        dateString = dateStr,
                        isCompleted = !isFloor,
                        isFloorSustained = isFloor,
                        durationSeconds = if (isFloor) 120 else 300,
                        numericValue = if (isFloor) 2.0 else 5.0
                    )
                )
            } else if (i == 6) {
                habitDao.insertOrUpdateRecord(
                    LogEntry(
                        habitId = h1Id,
                        dateString = dateStr,
                        frictionTag = "Low Energy",
                        notes = "Woke up with headache, rested instead"
                    )
                )
            }

            // Habit 2 (Hydration)
            if (i % 5 != 0) {
                val isFloor = (i == 3 || i == 8 || i == 15)
                val ml = if (isFloor) 1200.0 else (2500.0 + (i * 50 % 400))
                habitDao.insertOrUpdateRecord(
                    LogEntry(
                        habitId = h2Id,
                        dateString = dateStr,
                        isCompleted = !isFloor,
                        isFloorSustained = isFloor,
                        numericValue = ml
                    )
                )
            }

            // Habit 3 (Deep Work Sprints)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                val sprints = if (i % 4 == 0) 1 else 3
                val isFloor = sprints == 1
                habitDao.insertOrUpdateRecord(
                    LogEntry(
                        habitId = h3Id,
                        dateString = dateStr,
                        isCompleted = !isFloor,
                        isFloorSustained = isFloor,
                        tallyCount = sprints,
                        numericValue = sprints.toDouble()
                    )
                )
            }

            // Habit 4 (Pushups)
            if (dayOfWeek != Calendar.SUNDAY && i % 4 != 2) {
                val reps = if (i % 7 == 3) 15.0 else 30.0
                val isFloor = reps < 30.0
                habitDao.insertOrUpdateRecord(
                    LogEntry(
                        habitId = h4Id,
                        dateString = dateStr,
                        isCompleted = !isFloor,
                        isFloorSustained = isFloor,
                        numericValue = reps
                    )
                )
            }
        }
    }
}
