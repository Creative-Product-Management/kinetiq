package com.example.domain.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Injectable clock so analytics, reminders and repositories are deterministic in tests. */
interface KineticClock {
    fun today(): LocalDate
    fun now(): LocalDateTime
    fun nowMillis(): Long
}

object SystemKineticClock : KineticClock {
    override fun today(): LocalDate = LocalDate.now()
    override fun now(): LocalDateTime = LocalDateTime.now()
    override fun nowMillis(): Long = System.currentTimeMillis()
}

class FixedKineticClock(private val fixedToday: LocalDate, private val fixedMillis: Long = 0L) : KineticClock {
    override fun today(): LocalDate = fixedToday
    override fun now(): LocalDateTime = fixedToday.atTime(9, 0)
    override fun nowMillis(): Long = fixedMillis
}

/** Single place for the yyyy-MM-dd storage format. Never use Locale.getDefault() for keys. */
object DateCodec {
    fun format(date: LocalDate): String = date.toString() // ISO-8601, locale-independent
    fun parse(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
    fun todayString(clock: KineticClock = SystemKineticClock): String = format(clock.today())
    fun millisToDate(millis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}
