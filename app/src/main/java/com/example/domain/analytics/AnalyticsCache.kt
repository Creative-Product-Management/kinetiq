package com.example.domain.analytics

import com.example.data.model.Habit
import com.example.data.model.HabitRecordEntity

/**
 * In-memory memo for analytics.
 *
 * The key is derived from the *actual records list* the result was computed
 * from (its structural hash), never from a separately-observed version stamp.
 * Previously the key came from an independent count/latest-recordedAt flow:
 * when that flow emitted before the records flow, a result computed from a
 * stale (e.g. empty) record list was cached under the new version's key, and
 * every later lookup hit that poisoned entry — the detail gauge could stay at
 * 0% even though the habit had been logged. Keying on the records themselves
 * makes a cache hit proof that the records are identical to the ones used.
 */
class AnalyticsCache {
    private data class Key(val habitId: Long, val habitHash: Int, val recordsHash: Int, val restShield: Boolean, val weekMonday: Boolean)
    private val map = LinkedHashMap<Key, HabitAnalyticsResult>(16, 0.75f, true)

    @Synchronized
    fun get(habit: Habit, records: List<HabitRecordEntity>, restShieldEnabled: Boolean, weekStartsOnMonday: Boolean): HabitAnalyticsResult? =
        map[Key(habit.id, habit.hashCode(), records.hashCode(), restShieldEnabled, weekStartsOnMonday)]

    @Synchronized
    fun put(habit: Habit, records: List<HabitRecordEntity>, restShieldEnabled: Boolean, weekStartsOnMonday: Boolean, result: HabitAnalyticsResult) {
        map[Key(habit.id, habit.hashCode(), records.hashCode(), restShieldEnabled, weekStartsOnMonday)] = result
        while (map.size > MAX) map.remove(map.keys.first())
    }

    @Synchronized fun invalidate(habitId: Long) { map.keys.removeAll { it.habitId == habitId } }
    @Synchronized fun clear() = map.clear()

    companion object { const val MAX = 32 }
}
