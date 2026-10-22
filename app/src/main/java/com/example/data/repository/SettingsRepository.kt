package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kinetiq_settings")

enum class HabitSortMode { MANUAL, MOMENTUM }

data class KineticSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val weekStartsOnMonday: Boolean = true,
    val defaultFloorPercent: Int = 25,
    val restShieldEnabled: Boolean = true,
    val dailyEveningDigestEnabled: Boolean = true,
    val dailyEveningDigestHour: Int = 21,
    /** Permanent user preference for the dashboard hero. When false the hero never renders. */
    val showHeroSection: Boolean = true,
    val dismissedNudgeIds: Set<String> = emptySet(),
    /** Home ordering: manual drag order, or dynamic Momentum (highest first). */
    val sortMode: HabitSortMode = HabitSortMode.MANUAL
)

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<KineticSettings> = store.data.map { p ->
        KineticSettings(
            themeMode = runCatching { ThemeMode.valueOf(p[THEME] ?: ThemeMode.DARK.name) }.getOrDefault(ThemeMode.DARK),
            weekStartsOnMonday = p[WEEK_MONDAY] ?: true,
            defaultFloorPercent = p[FLOOR_PCT] ?: 25,
            restShieldEnabled = p[REST_SHIELD] ?: true,
            dailyEveningDigestEnabled = p[DIGEST_ENABLED] ?: true,
            dailyEveningDigestHour = p[DIGEST_HOUR] ?: 21,
            showHeroSection = p[SHOW_HERO] ?: true,
            dismissedNudgeIds = p[DISMISSED_NUDGES] ?: emptySet(),
            sortMode = runCatching { HabitSortMode.valueOf(p[SORT_MODE] ?: HabitSortMode.MANUAL.name) }.getOrDefault(HabitSortMode.MANUAL)
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setWeekStartsOnMonday(v: Boolean) = store.edit { it[WEEK_MONDAY] = v }
    suspend fun setDefaultFloorPercent(v: Int) = store.edit { it[FLOOR_PCT] = v.coerceIn(1, 100) }
    suspend fun setRestShieldEnabled(v: Boolean) = store.edit { it[REST_SHIELD] = v }
    suspend fun setDailyEveningDigestEnabled(v: Boolean) = store.edit { it[DIGEST_ENABLED] = v }
    suspend fun setDailyEveningDigestHour(v: Int) = store.edit { it[DIGEST_HOUR] = v.coerceIn(0, 23) }
    suspend fun dismissNudge(id: String) = store.edit { it[DISMISSED_NUDGES] = (it[DISMISSED_NUDGES] ?: emptySet()) + id }
    suspend fun setShowHeroSection(v: Boolean) = store.edit { it[SHOW_HERO] = v }
    suspend fun setSortMode(mode: HabitSortMode) = store.edit { it[SORT_MODE] = mode.name }
    suspend fun clearDismissedNudges() = store.edit { it.remove(DISMISSED_NUDGES) }

    companion object {
        private val THEME = stringPreferencesKey("theme_mode")
        private val WEEK_MONDAY = booleanPreferencesKey("week_starts_monday")
        private val FLOOR_PCT = intPreferencesKey("default_floor_percent")
        private val REST_SHIELD = booleanPreferencesKey("rest_shield_enabled")
        private val DIGEST_ENABLED = booleanPreferencesKey("digest_enabled")
        private val DIGEST_HOUR = intPreferencesKey("digest_hour")
        private val DISMISSED_NUDGES = stringSetPreferencesKey("dismissed_nudges")
        private val SHOW_HERO = booleanPreferencesKey("show_hero_section")
        private val SORT_MODE = stringPreferencesKey("habit_sort_mode")
    }
}
