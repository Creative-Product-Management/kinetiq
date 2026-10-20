package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "log_entries",
    indices = [
        Index(value = ["habitId", "dateString"], unique = true),
        Index(value = ["dateString"]),
        Index(value = ["habitId"])
    ]
)
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val dateString: String, // Format: YYYY-MM-DD
    val isCompleted: Boolean = false,
    val isFloorSustained: Boolean = false,
    val numericValue: Double = 0.0, // For NUMERIC targets (e.g. 2500 ml, 30 reps)
    val durationSeconds: Int = 0,   // For TIMER habits (e.g. 300 seconds of meditation)
    val tallyCount: Int = 0,        // For multi-check TALLIES (e.g. 1, 2, 3 sprints)
    val frictionTag: String? = null,
    val notes: String = "",
    val recordedAt: Long = System.currentTimeMillis()
)

typealias HabitRecordEntity = LogEntry
