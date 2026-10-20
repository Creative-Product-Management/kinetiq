package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.db.OrbitDatabase
import com.example.data.repository.OrbitRepository
import com.example.notification.HabitNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OrbitHabitApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: OrbitDatabase
        private set

    lateinit var repository: OrbitRepository
        private set

    lateinit var notificationScheduler: HabitNotificationScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            OrbitDatabase::class.java,
            "orbit_habit_database"
        ).fallbackToDestructiveMigration().build()

        repository = OrbitRepository(database.habitDao(), database.logEntryDao(), database.categoryDao())
        notificationScheduler = HabitNotificationScheduler(applicationContext)
        notificationScheduler.createNotificationChannels()

        applicationScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    companion object {
        lateinit var instance: OrbitHabitApplication
            private set
    }
}
