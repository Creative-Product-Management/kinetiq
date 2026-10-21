package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.HabitEntity
import java.time.LocalDateTime
import java.time.ZoneId

class HabitNotificationScheduler(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val remindersChannel = NotificationChannel(CHANNEL_REMINDERS_ID, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Daily scheduled reminders for active habits with direct logging actions"
                enableVibration(true)
            }
            val nudgesChannel = NotificationChannel(CHANNEL_NUDGES_ID, "Smart Nudges & Momentum", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Habit stacking suggestions and streak momentum updates"
            }
            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(nudgesChannel)
        }
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun scheduleHabitReminder(habit: HabitEntity) {
        val next = NextReminderCalculator.nextFire(habit, LocalDateTime.now()) ?: run { cancelHabitReminder(habit.id); return }
        scheduleAt(habitIntent(habit), PendingIntentCodes.code(habit.id, PendingIntentCodes.SALT_REMINDER), next)
    }

    fun scheduleEveningDigest(hour: Int) {
        val next = NextReminderCalculator.nextDigestFire(hour, LocalDateTime.now())
        val intent = Intent(context, HabitNotificationReceiver::class.java).apply {
            action = HabitNotificationReceiver.ACTION_EVENING_DIGEST
        }
        scheduleAt(intent, PendingIntentCodes.code(0L, PendingIntentCodes.SALT_DIGEST), next)
    }

    fun cancelEveningDigest() {
        val intent = Intent(context, HabitNotificationReceiver::class.java).apply { action = HabitNotificationReceiver.ACTION_EVENING_DIGEST }
        PendingIntent.getBroadcast(context, PendingIntentCodes.code(0L, PendingIntentCodes.SALT_DIGEST), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?.let { alarmManager.cancel(it) }
    }

    private fun habitIntent(habit: HabitEntity): Intent = Intent(context, HabitNotificationReceiver::class.java).apply {
        action = HabitNotificationReceiver.ACTION_HABIT_REMINDER
        putExtra(HabitNotificationReceiver.EXTRA_HABIT_ID, habit.id)
        putExtra(HabitNotificationReceiver.EXTRA_HABIT_TITLE, habit.title)
        putExtra(HabitNotificationReceiver.EXTRA_HABIT_CUE, habit.cueTrigger)
        putExtra(HabitNotificationReceiver.EXTRA_HABIT_MODALITY, habit.modality.name)
        putExtra(HabitNotificationReceiver.EXTRA_FLOOR_VALUE, habit.floorValue ?: -1.0)
        putExtra(HabitNotificationReceiver.EXTRA_TARGET_VALUE, habit.targetValue)
        putExtra(HabitNotificationReceiver.EXTRA_UNIT, habit.unit)
    }

    internal fun scheduleAt(intent: Intent, requestCode: Int, whenLocal: LocalDateTime) {
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val trigger = whenLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            if (canScheduleExactAlarms()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                else alarmManager.setExact(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                // Inexact fallback: still delivers (batched by Doze) without exact-alarm permission.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                else alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (_: SecurityException) {
            runCatching { alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, pi) }
        }
    }

    fun cancelHabitReminder(habitId: Long) {
        val intent = Intent(context, HabitNotificationReceiver::class.java).apply { action = HabitNotificationReceiver.ACTION_HABIT_REMINDER }
        val pi = PendingIntent.getBroadcast(context, PendingIntentCodes.code(habitId, PendingIntentCodes.SALT_REMINDER), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) alarmManager.cancel(pi)
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
            putExtra(HabitNotificationReceiver.EXTRA_IS_TEST, true)
        }
        context.sendBroadcast(intent)
    }

    companion object {
        const val CHANNEL_REMINDERS_ID = "orbit_reminders_channel"
        const val CHANNEL_NUDGES_ID = "orbit_nudges_channel"
    }
}
