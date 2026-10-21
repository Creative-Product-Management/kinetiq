package com.example.domain.analytics

import com.example.data.model.Habit
import com.example.data.repository.RecordsVersion

/** In-memory memo for analytics. Keyed by habit content + cheap records version stamp. */
class AnalyticsCache {
    private data class Key(val habitId: Long, val habitHash: Int, val count: Int, val latest: Long, val restShield: Boolean, val weekMonday: Boolean)
    private val map = LinkedHashMap<Key, HabitAnalyticsResult>(16, 0.75f, true)

    @Synchronized
    fun get(habit: Habit, version: RecordsVersion, restShieldEnabled: Boolean, weekStartsOnMonday: Boolean): HabitAnalyticsResult? =
        map[Key(habit.id, habit.hashCode(), version.count, version.latestRecordedAt, restShieldEnabled, weekStartsOnMonday)]

    @Synchronized
    fun put(habit: Habit, version: RecordsVersion, restShieldEnabled: Boolean, weekStartsOnMonday: Boolean, result: HabitAnalyticsResult) {
        map[Key(habit.id, habit.hashCode(), version.count, version.latestRecordedAt, restShieldEnabled, weekStartsOnMonday)] = result
        while (map.size > MAX) map.remove(map.keys.first())
    }

    @Synchronized fun invalidate(habitId: Long) { map.keys.removeAll { it.habitId == habitId } }
    @Synchronized fun clear() = map.clear()

    companion object { const val MAX = 32 }
}
