package com.example.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.OrbitHabitApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit Reminder"
        val cue = intent.getStringExtra(EXTRA_HABIT_CUE) ?: ""
        val floorVal = intent.getDoubleExtra(EXTRA_FLOOR_VALUE, -1.0)
        val targetVal = intent.getDoubleExtra(EXTRA_TARGET_VALUE, 1.0)
        val unit = intent.getStringExtra(EXTRA_UNIT) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_HABIT_REMINDER -> {
                showHabitReminderNotification(
                    context,
                    notificationManager,
                    habitId,
                    habitTitle,
                    cue,
                    floorVal,
                    targetVal,
                    unit
                )
            }
            ACTION_COMPLETE_TARGET -> {
                val pendingResult = goAsync()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = OrbitHabitApplication.instance.repository
                        val habit = repo.getHabitById(habitId)
                        if (habit != null) {
                            repo.toggleHabitForDate(habit, todayStr)
                        }
                    } catch (_: Exception) {}

                    // Update notification with success message
                    val feedback = NotificationCompat.Builder(context, HabitNotificationScheduler.CHANNEL_REMINDERS_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Target Achieved! 🚀")
                        .setContentText("Logged $habitTitle. Momentum is orbiting strong!")
                        .setAutoCancel(true)
                        .setTimeoutAfter(3000)
                        .build()
                    notificationManager.notify(habitId.toInt(), feedback)
                    pendingResult.finish()
                }
            }
            ACTION_SUSTAIN_FLOOR -> {
                val pendingResult = goAsync()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = OrbitHabitApplication.instance.repository
                        val habit = repo.getHabitById(habitId)
                        if (habit != null) {
                            repo.logFloorSustainForDate(habit, todayStr)
                        }
                    } catch (_: Exception) {}

                    val feedback = NotificationCompat.Builder(context, HabitNotificationScheduler.CHANNEL_REMINDERS_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Minimum Floor Sustained! 🛡️")
                        .setContentText("Logged $habitTitle. You never zero out!")
                        .setAutoCancel(true)
                        .setTimeoutAfter(3000)
                        .build()
                    notificationManager.notify(habitId.toInt(), feedback)
                    pendingResult.finish()
                }
            }
            ACTION_SNOOZE_15M -> {
                notificationManager.cancel(habitId.toInt())
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val snoozeTime = System.currentTimeMillis() + (15 * 60 * 1000)

                val snoozeIntent = Intent(context, HabitNotificationReceiver::class.java).apply {
                    this.action = ACTION_HABIT_REMINDER
                    putExtra(EXTRA_HABIT_ID, habitId)
                    putExtra(EXTRA_HABIT_TITLE, habitTitle)
                    putExtra(EXTRA_HABIT_CUE, cue)
                    putExtra(EXTRA_FLOOR_VALUE, floorVal)
                    putExtra(EXTRA_TARGET_VALUE, targetVal)
                    putExtra(EXTRA_UNIT, unit)
                }

                val pi = PendingIntent.getBroadcast(
                    context,
                    (habitId + 50000).toInt(),
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pi)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pi)
                }
            }
        }
    }

    private fun showHabitReminderNotification(
        context: Context,
        notificationManager: NotificationManager,
        habitId: Long,
        habitTitle: String,
        cue: String,
        floorVal: Double,
        targetVal: Double,
        unit: String
    ) {
        // App launch intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("selected_habit_id", habitId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Complete Target
        val completeIntent = Intent(context, HabitNotificationReceiver::class.java).apply {
            action = ACTION_COMPLETE_TARGET
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_TITLE, habitTitle)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Sustain Floor
        val floorIntent = Intent(context, HabitNotificationReceiver::class.java).apply {
            action = ACTION_SUSTAIN_FLOOR
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_TITLE, habitTitle)
        }
        val floorPendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 2).toInt(),
            floorIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, HabitNotificationReceiver::class.java).apply {
            action = ACTION_SNOOZE_15M
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_TITLE, habitTitle)
            putExtra(EXTRA_HABIT_CUE, cue)
            putExtra(EXTRA_FLOOR_VALUE, floorVal)
            putExtra(EXTRA_TARGET_VALUE, targetVal)
            putExtra(EXTRA_UNIT, unit)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId * 10 + 3).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentBody = if (cue.isNotBlank()) "Cue: $cue" else "Time to preserve your momentum!"

        val builder = NotificationCompat.Builder(context, HabitNotificationScheduler.CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Habit Reminder: $habitTitle")
            .setContentText(contentBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Done (Target)", completePendingIntent)

        if (floorVal > 0) {
            val floorLabel = if (unit.isNotBlank()) "Floor (${floorVal.toInt()} $unit)" else "Floor (Minimum)"
            builder.addAction(0, floorLabel, floorPendingIntent)
        }

        builder.addAction(0, "Snooze 15m", snoozePendingIntent)

        notificationManager.notify(habitId.toInt(), builder.build())
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.example.notification.ACTION_HABIT_REMINDER"
        const val ACTION_COMPLETE_TARGET = "com.example.notification.ACTION_COMPLETE_TARGET"
        const val ACTION_SUSTAIN_FLOOR = "com.example.notification.ACTION_SUSTAIN_FLOOR"
        const val ACTION_SNOOZE_15M = "com.example.notification.ACTION_SNOOZE_15M"

        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_TITLE = "extra_habit_title"
        const val EXTRA_HABIT_CUE = "extra_habit_cue"
        const val EXTRA_HABIT_MODALITY = "extra_habit_modality"
        const val EXTRA_FLOOR_VALUE = "extra_floor_value"
        const val EXTRA_TARGET_VALUE = "extra_target_value"
        const val EXTRA_UNIT = "extra_unit"
    }
}
