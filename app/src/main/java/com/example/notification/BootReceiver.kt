package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.KineticRoutinesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Re-arms all reminders after reboot, timezone change, or app update (alarms do not survive these). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = KineticRoutinesApplication.instance
                app.container.habitRepository.activeHabits.first()
                    .filter { it.reminderHour != null && it.reminderMinute != null }
                    .forEach { app.container.notificationScheduler.scheduleHabitReminder(it) }
                val settings = app.container.settingsRepository.settings.first()
                if (settings.dailyEveningDigestEnabled) app.container.notificationScheduler.scheduleEveningDigest(settings.dailyEveningDigestHour)
                else app.container.notificationScheduler.cancelEveningDigest()
            } catch (_: Exception) { } finally { pending.finish() }
        }
    }
}
