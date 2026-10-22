package com.example.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.KinetiqHabitApplication
import com.example.MainActivity
import com.example.R
import com.example.domain.time.DateCodec
import com.example.domain.time.SystemKinetiqClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class HabitNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit Reminder"
        val cue = intent.getStringExtra(EXTRA_HABIT_CUE) ?: ""
        val floorVal = intent.getDoubleExtra(EXTRA_FLOOR_VALUE, -1.0)
        val targetVal = intent.getDoubleExtra(EXTRA_TARGET_VALUE, 1.0)
        val unit = intent.getStringExtra(EXTRA_UNIT) ?: ""
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_HABIT_REMINDER -> {
                showHabitReminderNotification(context, notificationManager, habitId, habitTitle, cue, floorVal, targetVal, unit)
                // Re-arm the next occurrence so reminders repeat indefinitely.
                if (habitId > 0 && !isTest) {
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val app = KinetiqHabitApplication.instance
                            app.container.habitRepository.getHabitById(habitId)?.let { app.container.notificationScheduler.scheduleHabitReminder(it) }
                        } catch (_: Exception) { } finally { pending.finish() }
                    }
                }
            }
            ACTION_COMPLETE_TARGET, ACTION_SUSTAIN_FLOOR -> {
                if (isTest || habitId <= 0) { showFeedback(context, notificationManager, habitId, "Test notification", "This was a test — no habit was logged."); return }
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    var ok = false
                    try {
                        val app = KinetiqHabitApplication.instance
                        val habit = app.container.habitRepository.getHabitById(habitId)
                        if (habit != null) {
                            val today = DateCodec.todayString(SystemKinetiqClock)
                            if (action == ACTION_COMPLETE_TARGET) app.container.logRepository.setCompletedForDate(habit, today)
                            else app.container.logRepository.logFloorSustainForDate(habit, today)
                            ok = true
                        }
                    } catch (_: Exception) { ok = false }
                    finally {
                        showFeedback(
                            context, notificationManager, habitId,
                            if (ok) (if (action == ACTION_COMPLETE_TARGET) "Target Achieved!" else "Minimum Floor Sustained!") else "Could not log",
                            if (ok) "Logged $habitTitle." else "Open Routines to log $habitTitle manually."
                        )
                        pending.finish()
                    }
                }
            }
            ACTION_SNOOZE_15M -> {
                notificationManager.cancel(notificationId(habitId))
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val snoozeIntent = Intent(context, HabitNotificationReceiver::class.java).apply {
                    this.action = ACTION_HABIT_REMINDER
                    putExtra(EXTRA_HABIT_ID, habitId); putExtra(EXTRA_HABIT_TITLE, habitTitle); putExtra(EXTRA_HABIT_CUE, cue)
                    putExtra(EXTRA_FLOOR_VALUE, floorVal); putExtra(EXTRA_TARGET_VALUE, targetVal); putExtra(EXTRA_UNIT, unit)
                }
                val pi = PendingIntent.getBroadcast(context, PendingIntentCodes.code(habitId, PendingIntentCodes.SALT_SNOOZE), snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val trigger = System.currentTimeMillis() + 15 * 60 * 1000
                try {
                    val scheduler = KinetiqHabitApplication.instance.container.notificationScheduler
                    if (scheduler.canScheduleExactAlarms() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, pi)
                    }
                } catch (_: SecurityException) {
                    runCatching { alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, pi) }
                }
            }
            ACTION_EVENING_DIGEST -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = KinetiqHabitApplication.instance
                        val settings = app.container.settingsRepository.settings.first()
                        if (settings.dailyEveningDigestEnabled) {
                            val n = NotificationCompat.Builder(context, HabitNotificationScheduler.CHANNEL_NUDGES_ID)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("Evening Momentum Review")
                                .setContentText("Take a moment to log today's habits and protect your streaks.")
                                .setContentIntent(openAppPendingIntent(context, -1L))
                                .setAutoCancel(true).build()
                            notificationManager.notify(DIGEST_NOTIFICATION_ID, n)
                            app.container.notificationScheduler.scheduleEveningDigest(settings.dailyEveningDigestHour)
                        }
                    } catch (_: Exception) { } finally { pending.finish() }
                }
            }
        }
    }

    private fun notificationId(habitId: Long): Int = PendingIntentCodes.code(habitId, PendingIntentCodes.SALT_REMINDER)

    private fun openAppPendingIntent(context: Context, habitId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (habitId > 0) putExtra("selected_habit_id", habitId)
        }
        return PendingIntent.getActivity(context, PendingIntentCodes.code(habitId, PendingIntentCodes.SALT_OPEN_APP), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun showFeedback(context: Context, notificationManager: NotificationManager, habitId: Long, title: String, text: String) {
        val n = NotificationCompat.Builder(context, HabitNotificationScheduler.CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title).setContentText(text)
            .setAutoCancel(true).setTimeoutAfter(4000).build()
        runCatching { notificationManager.notify(notificationId(habitId), n) }
    }

    private fun showHabitReminderNotification(context: Context, notificationManager: NotificationManager, habitId: Long, habitTitle: String, cue: String, floorVal: Double, targetVal: Double, unit: String) {
        fun actionIntent(action: String) = Intent(context, HabitNotificationReceiver::class.java).apply {
            this.action = action; putExtra(EXTRA_HABIT_ID, habitId); putExtra(EXTRA_HABIT_TITLE, habitTitle)
            putExtra(EXTRA_HABIT_CUE, cue); putExtra(EXTRA_FLOOR_VALUE, floorVal); putExtra(EXTRA_TARGET_VALUE, targetVal); putExtra(EXTRA_UNIT, unit)
        }
        val completePi = PendingIntent.getBroadcast(context, PendingIntentCodes.code(habitId, PendingIntentCodes.SALT_COMPLETE), actionIntent(ACTION_COMPLETE_TARGET), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val floorPi = PendingIntent.getBroadcast(context, PendingIntentCodes.code(habitId, PendingIntentCodes.SALT_FLOOR), actionIntent(ACTION_SUSTAIN_FLOOR), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val snoozePi = PendingIntent.getBroadcast(context, PendingIntentCodes.code(habitId, PendingIntentCodes.SALT_SNOOZE), actionIntent(ACTION_SNOOZE_15M), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, HabitNotificationScheduler.CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Habit Reminder: $habitTitle")
            .setContentText(if (cue.isNotBlank()) "Cue: $cue" else "Time to preserve your momentum!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent(context, habitId))
            .setAutoCancel(true)
            .addAction(0, "Done (Target)", completePi)
        if (floorVal > 0) builder.addAction(0, if (unit.isNotBlank()) "Floor (${floorVal.toInt()} $unit)" else "Floor (Minimum)", floorPi)
        builder.addAction(0, "Snooze 15m", snoozePi)
        runCatching { notificationManager.notify(notificationId(habitId), builder.build()) }
        @Suppress("UNUSED_EXPRESSION") LocalDateTime.now().atZone(ZoneId.systemDefault())
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.example.notification.ACTION_HABIT_REMINDER"
        const val ACTION_COMPLETE_TARGET = "com.example.notification.ACTION_COMPLETE_TARGET"
        const val ACTION_SUSTAIN_FLOOR = "com.example.notification.ACTION_SUSTAIN_FLOOR"
        const val ACTION_SNOOZE_15M = "com.example.notification.ACTION_SNOOZE_15M"
        const val ACTION_EVENING_DIGEST = "com.example.notification.ACTION_EVENING_DIGEST"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_TITLE = "extra_habit_title"
        const val EXTRA_HABIT_CUE = "extra_habit_cue"
        const val EXTRA_HABIT_MODALITY = "extra_habit_modality"
        const val EXTRA_FLOOR_VALUE = "extra_floor_value"
        const val EXTRA_TARGET_VALUE = "extra_target_value"
        const val EXTRA_UNIT = "extra_unit"
        const val EXTRA_IS_TEST = "extra_is_test"
        const val DIGEST_NOTIFICATION_ID = 880001
    }
}
