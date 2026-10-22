package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.KineticDatabase
import com.example.data.db.LogEntryDao
import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.domain.time.DateCodec
import com.example.domain.time.KineticClock
import com.example.domain.time.SystemKineticClock
import kotlinx.coroutines.flow.Flow

class LogRepository(
    private val database: KineticDatabase,
    private val logEntryDao: LogEntryDao,
    private val clock: KineticClock = SystemKineticClock
) {
    /** Windowed dashboard cache: only the last [DASHBOARD_WINDOW_DAYS] days are ever loaded for the list. */
    fun dashboardRecords(): Flow<List<LogEntry>> {
        val today = clock.today()
        return logEntryDao.getLogsBetween(
            DateCodec.format(today.minusDays((DASHBOARD_WINDOW_DAYS - 1).toLong())),
            DateCodec.format(today)
        )
    }

    /** Last 91 days (today-90..today) for all habits — exactly the Momentum EMA window. */
    fun momentumRecords(): Flow<List<LogEntry>> {
        val today = clock.today()
        return logEntryDao.getLogsBetween(DateCodec.format(today.minusDays(90)), DateCodec.format(today))
    }

    fun recordsForHabit(habitId: Long): Flow<List<LogEntry>> = logEntryDao.getLogsForHabit(habitId)

    fun recordsForHabitWindow(habitId: Long, days: Int = DETAIL_WINDOW_DAYS): Flow<List<LogEntry>> =
        logEntryDao.getLogsForHabitSince(habitId, DateCodec.format(clock.today().minusDays(days.toLong())))

    suspend fun getRecord(habitId: Long, dateString: String): LogEntry? = logEntryDao.getLog(habitId, dateString)

    private fun floorOf(habit: Habit): Double = habit.floorValue ?: (habit.targetValue * DEFAULT_FLOOR_RATIO)

    suspend fun toggleHabitForDate(habit: Habit, dateString: String) {
        database.withTransaction {
            val existing = logEntryDao.getLog(habit.id, dateString)
            if (existing == null) {
                logEntryDao.insertOrUpdate(completedRecord(habit, dateString, null))
            } else if (existing.isCompleted) {
                logEntryDao.deleteLog(habit.id, dateString)
            } else {
                logEntryDao.insertOrUpdate(completedRecord(habit, dateString, existing))
            }
        }
    }

    /** Idempotent completion used by notification actions: never un-logs. */
    suspend fun setCompletedForDate(habit: Habit, dateString: String) {
        database.withTransaction {
            val existing = logEntryDao.getLog(habit.id, dateString)
            if (existing?.isCompleted == true) return@withTransaction
            logEntryDao.insertOrUpdate(completedRecord(habit, dateString, existing))
        }
    }

    private fun completedRecord(habit: Habit, dateString: String, existing: LogEntry?): LogEntry {
        val base = existing ?: LogEntry(habitId = habit.id, dateString = dateString)
        return base.copy(
            isCompleted = true, isFloorSustained = false,
            numericValue = habit.targetValue,
            durationSeconds = if (habit.modality == HabitModality.TIMER) (habit.targetValue * 60).toInt() else base.durationSeconds,
            tallyCount = if (habit.modality == HabitModality.TALLY) habit.targetValue.toInt() else maxOf(base.tallyCount, 1),
            recordedAt = System.currentTimeMillis()
        )
    }

    suspend fun logFloorSustainForDate(habit: Habit, dateString: String) {
        database.withTransaction {
            val existing = logEntryDao.getLog(habit.id, dateString)
            if (existing?.isCompleted == true) return@withTransaction
            val floorVal = floorOf(habit)
            val base = existing ?: LogEntry(habitId = habit.id, dateString = dateString)
            logEntryDao.insertOrUpdate(base.copy(
                isCompleted = false, isFloorSustained = true, numericValue = floorVal,
                durationSeconds = if (habit.modality == HabitModality.TIMER) (floorVal * 60).toInt() else base.durationSeconds,
                tallyCount = if (habit.modality == HabitModality.TALLY) floorVal.toInt().coerceAtLeast(1) else base.tallyCount,
                recordedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun logNumericProgress(habit: Habit, dateString: String, amount: Double) {
        database.withTransaction {
            if (amount <= 0.0) { logEntryDao.deleteLog(habit.id, dateString); return@withTransaction }
            val existing = logEntryDao.getLog(habit.id, dateString)
            val base = existing ?: LogEntry(habitId = habit.id, dateString = dateString)
            val isMet = amount >= habit.targetValue
            logEntryDao.insertOrUpdate(base.copy(
                numericValue = amount,
                durationSeconds = if (habit.modality == HabitModality.TIMER) (amount * 60).toInt() else base.durationSeconds,
                tallyCount = if (habit.modality == HabitModality.TALLY) amount.toInt() else base.tallyCount,
                isCompleted = isMet, isFloorSustained = !isMet && amount >= floorOf(habit),
                recordedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun logTallyIncrement(habit: Habit, dateString: String) {
        database.withTransaction {
            val existing = logEntryDao.getLog(habit.id, dateString)
            val next = (existing?.tallyCount ?: 0) + 1
            val base = existing ?: LogEntry(habitId = habit.id, dateString = dateString)
            val isMet = next >= habit.targetValue
            logEntryDao.insertOrUpdate(base.copy(
                tallyCount = next, numericValue = next.toDouble(),
                isCompleted = isMet, isFloorSustained = !isMet && next >= floorOf(habit),
                recordedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun logDurationSeconds(habit: Habit, dateString: String, durationSec: Int) {
        if (durationSec <= 0) return
        database.withTransaction {
            val existing = logEntryDao.getLog(habit.id, dateString)
            val totalSec = (existing?.durationSeconds ?: 0) + durationSec
            val base = existing ?: LogEntry(habitId = habit.id, dateString = dateString)
            val isMet = totalSec >= (habit.targetValue * 60).toInt()
            logEntryDao.insertOrUpdate(base.copy(
                durationSeconds = totalSec, numericValue = totalSec / 60.0,
                isCompleted = isMet, isFloorSustained = !isMet && totalSec >= (floorOf(habit) * 60).toInt(),
                recordedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun logFrictionTag(habitId: Long, dateString: String, frictionTag: String, notes: String = "") {
        database.withTransaction {
            val existing = logEntryDao.getLog(habitId, dateString)
            val base = existing ?: LogEntry(habitId = habitId, dateString = dateString)
            logEntryDao.insertOrUpdate(base.copy(frictionTag = frictionTag, notes = notes, recordedAt = System.currentTimeMillis()))
        }
    }

    suspend fun clearRecord(habitId: Long, dateString: String) = logEntryDao.deleteLog(habitId, dateString)
    suspend fun clearAllRecords() = logEntryDao.clearAllLogs()

    companion object {
        const val DEFAULT_FLOOR_RATIO = 0.25
        const val DASHBOARD_WINDOW_DAYS = 14
        const val DETAIL_WINDOW_DAYS = 400
    }
}
