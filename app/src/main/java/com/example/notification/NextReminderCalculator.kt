package com.example.notification

import com.example.data.model.Habit
import java.time.LocalDateTime

/** Pure, deterministic next-fire calculation honoring reminder days and rest days. */
object NextReminderCalculator {
    fun nextFire(habit: Habit, from: LocalDateTime): LocalDateTime? {
        val hour = habit.reminderHour ?: return null
        val minute = habit.reminderMinute ?: return null
        for (offset in 0..8) {
            val date = from.toLocalDate().plusDays(offset.toLong())
            val iso = date.dayOfWeek.value // Mon=1..Sun=7
            if (!habit.isReminderEnabledForDay(iso)) continue
            if (habit.isRestDay(iso)) continue
            val candidate = date.atTime(hour, minute)
            if (candidate.isAfter(from)) return candidate
        }
        return null
    }

    fun nextDigestFire(hour: Int, from: LocalDateTime): LocalDateTime {
        val today = from.toLocalDate().atTime(hour.coerceIn(0, 23), 0)
        return if (today.isAfter(from)) today else today.plusDays(1)
    }
}

/** Stable, collision-resistant PendingIntent request codes (no arithmetic like id*10+n). */
object PendingIntentCodes {
    const val SALT_REMINDER = 11
    const val SALT_OPEN_APP = 23
    const val SALT_COMPLETE = 37
    const val SALT_FLOOR = 41
    const val SALT_SNOOZE = 53
    const val SALT_DIGEST = 67

    fun code(habitId: Long, salt: Int): Int {
        var h = habitId xor (habitId ushr 32)
        h = h * 31 + salt
        return (h and 0x3fffffff).toInt()
    }
}
