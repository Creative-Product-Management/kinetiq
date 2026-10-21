package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "log_entries",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
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
    val numericValue: Double = 0.0,
    val durationSeconds: Int = 0,
    val tallyCount: Int = 0,
    val frictionTag: String? = null,
    val notes: String = "",
    val recordedAt: Long = System.currentTimeMillis()
)

typealias HabitRecordEntity = LogEntry
