package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {

    @Query("SELECT * FROM log_entries WHERE habitId = :habitId ORDER BY dateString ASC")
    fun getLogsForHabit(habitId: Long): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries WHERE habitId = :habitId ORDER BY dateString ASC")
    suspend fun getLogsListForHabit(habitId: Long): List<LogEntry>

    @Query("SELECT * FROM log_entries WHERE habitId = :habitId AND dateString = :dateString LIMIT 1")
    suspend fun getLog(habitId: Long, dateString: String): LogEntry?

    @Query("SELECT * FROM log_entries WHERE dateString IN (:dates)")
    fun getLogsForDates(dates: List<String>): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries WHERE dateString = :dateString")
    fun getLogsForDate(dateString: String): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: LogEntry): Long

    @Query("DELETE FROM log_entries WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteLog(habitId: Long, dateString: String)

    @Query("DELETE FROM log_entries WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: Long)

    // Specialized query methods supporting numerical targets, timers, and multi-check tallies
    @Query("SELECT COALESCE(SUM(numericValue), 0.0) FROM log_entries WHERE habitId = :habitId")
    fun getTotalNumericProgress(habitId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM log_entries WHERE habitId = :habitId")
    fun getTotalDurationSeconds(habitId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(tallyCount), 0) FROM log_entries WHERE habitId = :habitId")
    fun getTotalTallyCount(habitId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM log_entries WHERE habitId = :habitId AND (isCompleted = 1 OR isFloorSustained = 1)")
    fun getTotalCompletedCount(habitId: Long): Flow<Int>
}
