package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.Category
import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.data.model.RoutineStackEntity

class Converters {
    @TypeConverter
    fun fromModality(modality: HabitModality?): String {
        return modality?.name ?: HabitModality.YES_NO.name
    }

    @TypeConverter
    fun toModality(name: String?): HabitModality {
        return try {
            name?.let { HabitModality.valueOf(it) } ?: HabitModality.YES_NO
        } catch (_: Exception) {
            HabitModality.YES_NO
        }
    }
}

@Database(
    entities = [
        Habit::class,
        LogEntry::class,
        Category::class,
        RoutineStackEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OrbitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun categoryDao(): CategoryDao
}
