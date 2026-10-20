package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.HabitEntity
import java.util.Calendar

class HabitNotificationScheduler(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily scheduled reminders for active habits with direct logging actions"
                enableVibration(true)
            }

            val nudgesChannel = NotificationChannel(
                CHANNEL_NUDGES_ID,
                "Smart Nudges & Momentum",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Habit stacking suggestions and streak momentum updates"
            }

            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(nudgesChannel)
        }
    }

    fun scheduleHabitReminder(habit: HabitEntity) {
        val hour = habit.reminderHour ?: return
        val minute = habit.reminderMinute ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, HabitNotificationReceiver::class.java).apply {
            action = HabitNotificationReceiver.ACTION_HABIT_REMINDER
            putExtra(HabitNotificationReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(HabitNotificationReceiver.EXTRA_HABIT_TITLE, habit.title)
            putExtra(HabitNotificationReceiver.EXTRA_HABIT_CUE, habit.cueTrigger)
            putExtra(HabitNotificationReceiver.EXTRA_HABIT_MODALITY, habit.modality.name)
            putExtra(HabitNotificationReceiver.EXTRA_FLOOR_VALUE, habit.floorValue ?: -1.0)
            putExtra(HabitNotificationReceiver.EXTRA_TARGET_VALUE, habit.targetValue)
            putExtra(HabitNotificationReceiver.EXTRA_UNIT, habit.unit)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Inexact fallback
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelHabitReminder(habitId: Long) {
        val intent = Intent(context, HabitNotificationReceiver::class.java).apply {
            action = HabitNotificationReceiver.ACTION_HABIT_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun sendTestNotification(sampleHabitTitle: String = "Mindful Breathwork") {
        val intent = Intent(context, HabitNotificationReceiver::class.java).apply {
            action = HabitNotificationReceiver.ACTION_HABIT_REMINDER
            putExtra(HabitNotificationReceiver.EXTRA_HABIT_ID, 9999L)
            putExtra(HabitNotificationReceiver.EXTRA_HABIT_TITLE, sampleHabitTitle)
            putExtra(HabitNotificationReceiver.EXTRA_HABIT_CUE, "After your morning coffee")
            putExtra(HabitNotificationReceiver.EXTRA_HABIT_MODALITY, "TIMER")
            putExtra(HabitNotificationReceiver.EXTRA_FLOOR_VALUE, 1.0)
            putExtra(HabitNotificationReceiver.EXTRA_TARGET_VALUE, 5.0)
            putExtra(HabitNotificationReceiver.EXTRA_UNIT, "mins")
        }
        context.sendBroadcast(intent)
    }

    companion object {
        const val CHANNEL_REMINDERS_ID = "orbit_reminders_channel"
        const val CHANNEL_NUDGES_ID = "orbit_nudges_channel"
    }
}
