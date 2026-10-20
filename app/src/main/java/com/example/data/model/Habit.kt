package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val cueTrigger: String = "",
    val modality: HabitModality = HabitModality.YES_NO,
    val targetValue: Double = 1.0,     // Numerical target, timer minutes, or tally target
    val floorValue: Double? = null,    // Elastic minimum viable goal
    val unit: String = "",             // Measurement unit (e.g., "ml", "mins", "reps", "sprints")
    val categoryId: Long? = null,      // Foreign key reference to Category
    val category: String = "General",
    val colorHex: String = "#10B981",
    val iconName: String = "Sparkles",
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val reminderDaysMask: Int = 127,
    val restDaysMask: Int = 0,
    val routineStackId: Long? = null,
    val orderIndex: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isRestDay(dayOfWeek1To7: Int): Boolean {
        val shift = (dayOfWeek1To7 - 1).coerceIn(0, 6)
        return ((restDaysMask shr shift) and 1) == 1
    }

    fun isReminderEnabledForDay(dayOfWeek1To7: Int): Boolean {
        if (reminderHour == null || reminderMinute == null) return false
        val shift = (dayOfWeek1To7 - 1).coerceIn(0, 6)
        return ((reminderDaysMask shr shift) and 1) == 1
    }
}

typealias HabitEntity = Habit
