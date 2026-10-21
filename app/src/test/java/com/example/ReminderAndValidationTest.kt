package com.example

import com.example.data.backup.BackupCrypto
import com.example.data.model.Habit
import com.example.domain.validation.HabitValidator
import com.example.notification.NextReminderCalculator
import com.example.notification.PendingIntentCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDateTime

class ReminderAndValidationTest {

    private fun habit(mask: Int, rest: Int = 0) = Habit(
        title = "H", reminderHour = 8, reminderMinute = 0, reminderDaysMask = mask, restDaysMask = rest
    )

    @Test fun `weekday reminder skips weekend`() {
        // Mon-Fri bits 0..4 ; 2026-10-16 is a Friday, after 08:00 -> next is Monday 2026-10-19
        val next = NextReminderCalculator.nextFire(habit(0b0011111), LocalDateTime.of(2026, 10, 16, 9, 0))
        assertEquals(LocalDateTime.of(2026, 10, 19, 8, 0), next)
    }

    @Test fun `reminder later today is used`() {
        val next = NextReminderCalculator.nextFire(habit(127), LocalDateTime.of(2026, 10, 21, 7, 0))
        assertEquals(LocalDateTime.of(2026, 10, 21, 8, 0), next)
    }

    @Test fun `rest day is skipped`() {
        // Wed 2026-10-21 is rest (bit 2); from Tue evening next should be Thu
        val next = NextReminderCalculator.nextFire(habit(127, rest = 1 shl 2), LocalDateTime.of(2026, 10, 20, 9, 0))
        assertEquals(LocalDateTime.of(2026, 10, 22, 8, 0), next)
    }

    @Test fun `no reminder time means no fire`() {
        assertNull(NextReminderCalculator.nextFire(Habit(title = "H"), LocalDateTime.of(2026, 10, 21, 7, 0)))
    }

    @Test fun `pending intent codes are stable and distinct per action`() {
        assertEquals(PendingIntentCodes.code(5, PendingIntentCodes.SALT_COMPLETE), PendingIntentCodes.code(5, PendingIntentCodes.SALT_COMPLETE))
        assertNotEquals(PendingIntentCodes.code(5, PendingIntentCodes.SALT_COMPLETE), PendingIntentCodes.code(5, PendingIntentCodes.SALT_FLOOR))
        assertNotEquals(PendingIntentCodes.code(50001, PendingIntentCodes.SALT_REMINDER), PendingIntentCodes.code(1, PendingIntentCodes.SALT_SNOOZE))
    }

    @Test fun `validator rejects blank title non-positive target and floor above target`() {
        assertTrue(HabitValidator.validate("", 1.0, null).titleError != null)
        assertTrue(HabitValidator.validate("X", 0.0, null).targetError != null)
        assertTrue(HabitValidator.validate("X", 10.0, 20.0).floorError != null)
        assertTrue(HabitValidator.validate("X", 2.5, 0.5).isValid)
    }

    @Test fun `backup crypto round trips and rejects wrong passphrase`() {
        val plain = "kinetiq backup payload".toByteArray()
        val encrypted = BackupCrypto.encryptRaw(plain, "correct horse".toCharArray())
        assertTrue(!encrypted.contentEquals(plain))
        assertEquals("kinetiq backup payload", BackupCrypto.decryptRaw(encrypted, "correct horse".toCharArray()).decodeToString())
        try {
            BackupCrypto.decryptRaw(encrypted, "wrong passphrase".toCharArray())
            fail("Wrong passphrase must fail GCM authentication")
        } catch (_: Exception) { }
    }
}
