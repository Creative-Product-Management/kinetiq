package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/**
 * v2 -> v3:
 * - Backfills [Habit.categoryId] from the legacy free-text `category` column
 *   (creating Category rows for any names not yet present), then drops the
 *   text column so categories have a single source of truth.
 * - Recreates `habits` and `log_entries` with real foreign keys
 *   (log entries cascade-delete with their habit).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) Create Category rows for legacy text values and backfill ids.
        db.execSQL(
            "INSERT OR IGNORE INTO categories (name, colorHex, iconName, orderIndex) " +
                "SELECT DISTINCT category, '#10B981', 'Folder', 0 FROM habits WHERE category IS NOT NULL AND category <> ''"
        )
        db.execSQL(
            "UPDATE habits SET categoryId = (SELECT id FROM categories WHERE categories.name = habits.category) " +
                "WHERE categoryId IS NULL AND category IS NOT NULL AND category <> ''"
        )

        // 2) Recreate habits without the text column, with FKs.
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS habits_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                cueTrigger TEXT NOT NULL,
                modality TEXT NOT NULL,
                targetValue REAL NOT NULL,
                floorValue REAL,
                unit TEXT NOT NULL,
                categoryId INTEGER,
                colorHex TEXT NOT NULL,
                iconName TEXT NOT NULL,
                reminderHour INTEGER,
                reminderMinute INTEGER,
                reminderDaysMask INTEGER NOT NULL,
                restDaysMask INTEGER NOT NULL,
                routineStackId INTEGER,
                orderIndex INTEGER NOT NULL,
                isArchived INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(routineStackId) REFERENCES routine_stacks(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )""".trimIndent()
        )
        db.execSQL(
            "INSERT INTO habits_new (id, title, description, cueTrigger, modality, targetValue, floorValue, unit, categoryId, colorHex, iconName, reminderHour, reminderMinute, reminderDaysMask, restDaysMask, routineStackId, orderIndex, isArchived, createdAt) " +
                "SELECT id, title, description, cueTrigger, modality, targetValue, floorValue, unit, categoryId, colorHex, iconName, reminderHour, reminderMinute, reminderDaysMask, restDaysMask, routineStackId, orderIndex, isArchived, createdAt FROM habits"
        )
        db.execSQL("DROP TABLE habits")
        db.execSQL("ALTER TABLE habits_new RENAME TO habits")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_categoryId ON habits (categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_routineStackId ON habits (routineStackId)")

        // 3) Recreate log_entries with cascade FK.
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS log_entries_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                habitId INTEGER NOT NULL,
                dateString TEXT NOT NULL,
                isCompleted INTEGER NOT NULL,
                isFloorSustained INTEGER NOT NULL,
                numericValue REAL NOT NULL,
                durationSeconds INTEGER NOT NULL,
                tallyCount INTEGER NOT NULL,
                frictionTag TEXT,
                notes TEXT NOT NULL,
                recordedAt INTEGER NOT NULL,
                FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )""".trimIndent()
        )
        db.execSQL(
            "INSERT INTO log_entries_new (id, habitId, dateString, isCompleted, isFloorSustained, numericValue, durationSeconds, tallyCount, frictionTag, notes, recordedAt) " +
                "SELECT id, habitId, dateString, isCompleted, isFloorSustained, numericValue, durationSeconds, tallyCount, frictionTag, notes, recordedAt FROM log_entries WHERE habitId IN (SELECT id FROM habits)"
        )
        db.execSQL("DROP TABLE log_entries")
        db.execSQL("ALTER TABLE log_entries_new RENAME TO log_entries")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_log_entries_habitId_dateString ON log_entries (habitId, dateString)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_dateString ON log_entries (dateString)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_habitId ON log_entries (habitId)")
    }
}

@Database(
    entities = [
        Habit::class,
        LogEntry::class,
        Category::class,
        RoutineStackEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KinetiqDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        val ALL_MIGRATIONS = arrayOf(MIGRATION_2_3)
    }
}
