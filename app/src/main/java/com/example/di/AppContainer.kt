package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.backup.BackupManager
import com.example.data.db.KineticDatabase
import com.example.data.repository.HabitRepository
import com.example.data.repository.LogRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.analytics.AnalyticsCache
import com.example.notification.HabitNotificationScheduler

/** Manual DI container. Single construction site for DB, repositories and schedulers. */
class AppContainer(context: Context) {
    val database: KineticDatabase = Room.databaseBuilder(
        context.applicationContext,
        KineticDatabase::class.java,
        "orbit_habit_database" // Brand rename: intentionally kept legacy file name so existing installs keep their data
    ).addMigrations(*KineticDatabase.ALL_MIGRATIONS).build()

    val habitRepository: HabitRepository = HabitRepository(database, database.habitDao(), database.logEntryDao(), database.categoryDao())
    val logRepository: LogRepository = LogRepository(database, database.logEntryDao())
    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)
    val analyticsCache: AnalyticsCache = AnalyticsCache()
    val notificationScheduler: HabitNotificationScheduler = HabitNotificationScheduler(context.applicationContext)
    val backupManager: BackupManager = BackupManager(habitRepository)
}
