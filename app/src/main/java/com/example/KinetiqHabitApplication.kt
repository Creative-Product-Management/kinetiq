package com.example

import android.app.Application
import com.example.data.repository.HabitRepository
import com.example.di.AppContainer
import com.example.notification.HabitNotificationScheduler

class KinetiqHabitApplication : Application() {

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
        // No auto-seeding: a fresh install starts empty, and deleting every
        // habit is a legitimate state that must survive relaunch. The only
        // sample data is the single opt-in habit in Settings.
    }

    companion object {
        lateinit var instance: KinetiqHabitApplication
            private set
    }
}
