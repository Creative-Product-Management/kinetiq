package com.example

import android.app.Application
import com.example.data.repository.HabitRepository
import com.example.di.AppContainer
import com.example.notification.HabitNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KineticRoutinesApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Back-compat accessors; new code should use [container]. */
    val repository: HabitRepository get() = container.habitRepository
    val notificationScheduler: HabitNotificationScheduler get() = container.notificationScheduler

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(applicationContext)
        container.notificationScheduler.createNotificationChannels()
        // No auto-seeding of habits: a fresh install starts with no habits, and
        // deleting every habit is a legitimate state. Categories/stacks get a
        // one-time default library so the editor has something to pick from.
        appScope.launch {
            val settings = container.settingsRepository
            if (!settings.isLibraryDefaultsSeeded()) {
                container.habitRepository.seedDefaultLibraryIfEmpty()
                settings.setLibraryDefaultsSeeded(true)
            }
        }
    }

    companion object {
        lateinit var instance: KineticRoutinesApplication
            private set
    }
}
