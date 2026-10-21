package com.example

import android.app.Application
import com.example.data.repository.HabitRepository
import com.example.di.AppContainer
import com.example.notification.HabitNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KinetiqHabitApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
        private set

    /** Back-compat accessors; new code should use [container]. */
    val repository: HabitRepository get() = container.habitRepository
    val notificationScheduler: HabitNotificationScheduler get() = container.notificationScheduler

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(applicationContext)
        container.notificationScheduler.createNotificationChannels()
        applicationScope.launch {
            container.habitRepository.seedInitialDataIfEmpty()
        }
    }

    companion object {
        lateinit var instance: KinetiqHabitApplication
            private set
    }
}
