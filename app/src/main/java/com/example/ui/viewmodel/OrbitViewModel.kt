package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.OrbitHabitApplication
import com.example.data.model.HabitEntity
import com.example.data.model.HabitModality
import com.example.data.model.HabitRecordEntity
import com.example.data.model.RoutineStackEntity
import com.example.data.repository.OrbitRepository
import com.example.ui.components.DayStripItem
import com.example.ui.components.InAppNudgeData
import com.example.ui.components.NudgeType
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrbitViewModel(
    private val repository: OrbitRepository = OrbitHabitApplication.instance.repository
) : ViewModel() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val allHabits: StateFlow<List<HabitEntity>> = repository.activeHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecords: StateFlow<List<HabitRecordEntity>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStacks: StateFlow<List<RoutineStackEntity>> = repository.allStacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isStackView = MutableStateFlow(false)
    val isStackView: StateFlow<Boolean> = _isStackView.asStateFlow()

    private val _selectedHabitForDetail = MutableStateFlow<HabitEntity?>(null)
    val selectedHabitForDetail: StateFlow<HabitEntity?> = _selectedHabitForDetail.asStateFlow()

    private val _isEditorOpen = MutableStateFlow(false)
    val isEditorOpen: StateFlow<Boolean> = _isEditorOpen.asStateFlow()

    private val _habitForEditor = MutableStateFlow<HabitEntity?>(null)
    val habitForEditor: StateFlow<HabitEntity?> = _habitForEditor.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _weekStartsOnMonday = MutableStateFlow(true)
    val weekStartsOnMonday: StateFlow<Boolean> = _weekStartsOnMonday.asStateFlow()

    private val _defaultFloorPercent = MutableStateFlow(25)
    val defaultFloorPercent: StateFlow<Int> = _defaultFloorPercent.asStateFlow()

    private val _restShieldEnabled = MutableStateFlow(true)
    val restShieldEnabled: StateFlow<Boolean> = _restShieldEnabled.asStateFlow()

    private val _dailyEveningDigestEnabled = MutableStateFlow(true)
    val dailyEveningDigestEnabled: StateFlow<Boolean> = _dailyEveningDigestEnabled.asStateFlow()

    private val _dailyEveningDigestHour = MutableStateFlow(21)
    val dailyEveningDigestHour: StateFlow<Int> = _dailyEveningDigestHour.asStateFlow()

    private val _isNotificationSettingsOpen = MutableStateFlow(false)
    val isNotificationSettingsOpen: StateFlow<Boolean> = _isNotificationSettingsOpen.asStateFlow()

    private val _activeTimerHabit = MutableStateFlow<HabitEntity?>(null)
    val activeTimerHabit: StateFlow<HabitEntity?> = _activeTimerHabit.asStateFlow()

    private val _quickLogTarget = MutableStateFlow<Pair<HabitEntity, DayStripItem>?>(null)
    val quickLogTarget: StateFlow<Pair<HabitEntity, DayStripItem>?> = _quickLogTarget.asStateFlow()

    private val _inAppNudge = MutableStateFlow<InAppNudgeData?>(
        InAppNudgeData(
            type = NudgeType.STACK_PRIMED,
            title = "Morning Launchpad Ready",
            subtitle = "You have 2 habits linked in sequence. Completing Breathwork primes Hydration!",
            actionLabel = "Start Stack"
        )
    )
    val inAppNudge: StateFlow<InAppNudgeData?> = _inAppNudge.asStateFlow()

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleStackView() {
        _isStackView.value = !_isStackView.value
    }

    fun openHabitDetail(habit: HabitEntity) {
        _selectedHabitForDetail.value = habit
    }

    fun closeHabitDetail() {
        _selectedHabitForDetail.value = null
    }

    fun openNewHabitEditor() {
        _habitForEditor.value = null
        _isEditorOpen.value = true
    }

    fun openEditHabitEditor(habit: HabitEntity) {
        _habitForEditor.value = habit
        _isEditorOpen.value = true
    }

    fun closeEditor() {
        _isEditorOpen.value = false
        _habitForEditor.value = null
    }

    fun openNotificationSettings() {
        _isNotificationSettingsOpen.value = true
    }

    fun closeNotificationSettings() {
        _isNotificationSettingsOpen.value = false
    }

    fun openTimer(habit: HabitEntity) {
        _activeTimerHabit.value = habit
    }

    fun closeTimer() {
        _activeTimerHabit.value = null
    }

    fun openQuickLog(habit: HabitEntity, item: DayStripItem) {
        _quickLogTarget.value = Pair(habit, item)
    }

    fun closeQuickLog() {
        _quickLogTarget.value = null
    }

    fun dismissNudge() {
        _inAppNudge.value = null
    }

    fun handleDayClick(habit: HabitEntity, item: DayStripItem) {
        when (habit.modality) {
            HabitModality.YES_NO -> {
                viewModelScope.launch {
                    repository.toggleHabitForDate(habit, item.dateString)
                }
            }
            HabitModality.TALLY -> {
                viewModelScope.launch {
                    repository.logTallyIncrement(habit, item.dateString)
                }
            }
            HabitModality.TIMER -> {
                // Open quick log or timer
                openQuickLog(habit, item)
            }
            HabitModality.NUMERIC -> {
                // If not done, fast toggle target, or if already logged, open dialog
                if (item.record == null || (!item.record.isCompleted && !item.record.isFloorSustained)) {
                    viewModelScope.launch {
                        repository.toggleHabitForDate(habit, item.dateString)
                    }
                } else {
                    openQuickLog(habit, item)
                }
            }
        }
    }

    fun logTarget(habit: HabitEntity, dateString: String) {
        viewModelScope.launch {
            repository.toggleHabitForDate(habit, dateString)
            closeQuickLog()
        }
    }

    fun logFloor(habit: HabitEntity, dateString: String) {
        viewModelScope.launch {
            repository.logFloorSustainForDate(habit, dateString)
            closeQuickLog()
        }
    }

    fun logCustomAmount(habit: HabitEntity, dateString: String, amount: Double) {
        viewModelScope.launch {
            repository.logNumericProgress(habit, dateString, amount)
            closeQuickLog()
        }
    }

    fun logFriction(habitId: Long, dateString: String, tag: String, notes: String) {
        viewModelScope.launch {
            repository.logFrictionTag(habitId, dateString, tag, notes)
            closeQuickLog()
        }
    }

    fun clearLog(habitId: Long, dateString: String) {
        viewModelScope.launch {
            repository.clearRecord(habitId, dateString)
            closeQuickLog()
        }
    }

    fun finishTimerSession(habit: HabitEntity, durationSeconds: Int) {
        val todayStr = sdf.format(Calendar.getInstance().time)
        viewModelScope.launch {
            repository.logDurationSeconds(habit, todayStr, durationSeconds)
            closeTimer()
        }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val savedId = repository.saveHabit(habit)
            val updated = habit.copy(id = if (habit.id == 0L) savedId else habit.id)
            if (updated.reminderHour != null && updated.reminderMinute != null) {
                OrbitHabitApplication.instance.notificationScheduler.scheduleHabitReminder(updated)
            } else {
                OrbitHabitApplication.instance.notificationScheduler.cancelHabitReminder(updated.id)
            }
            closeEditor()
            if (_selectedHabitForDetail.value?.id == habit.id) {
                _selectedHabitForDetail.value = updated
            }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            OrbitHabitApplication.instance.notificationScheduler.cancelHabitReminder(habit.id)
            closeEditor()
            if (_selectedHabitForDetail.value?.id == habit.id) {
                closeHabitDetail()
            }
        }
    }

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }
    fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode }
    fun setWeekStartsOnMonday(value: Boolean) { _weekStartsOnMonday.value = value }
    fun setDefaultFloorPercent(value: Int) { _defaultFloorPercent.value = value }
    fun toggleRestShield(value: Boolean) { _restShieldEnabled.value = value }
    fun toggleDailyEveningDigest(value: Boolean) { _dailyEveningDigestEnabled.value = value }
    fun setDailyEveningDigestHour(hour: Int) { _dailyEveningDigestHour.value = hour }

    fun resetAllRecords() {
        viewModelScope.launch {
            repository.clearAllRecords()
        }
    }

    fun reseedDemoData() {
        viewModelScope.launch {
            repository.reseedData()
        }
    }

    fun exportDataJson(): String {
        val hList = allHabits.value
        val rList = allRecords.value
        val sb = StringBuilder()
        sb.append("{\n  \"app\": \"Kinetiq\",\n  \"exportedAt\": \"${System.currentTimeMillis()}\",\n")
        sb.append("  \"habitsCount\": ${hList.size},\n  \"recordsCount\": ${rList.size},\n")
        sb.append("  \"habits\": [\n")
        hList.forEachIndexed { i, h ->
            sb.append("    {\"id\": ${h.id}, \"title\": \"${h.title}\", \"modality\": \"${h.modality}\", \"target\": ${h.targetValue}, \"floor\": ${h.floorValue}}\n")
            if (i < hList.size - 1) sb.append(",")
        }
        sb.append("  ]\n}")
        return sb.toString()
    }

    // Generate recent 7 days strip for a habit
    fun getRecent7Days(habitId: Long, records: List<HabitRecordEntity>): List<DayStripItem> {
        val recordMap = records.filter { it.habitId == habitId }.associateBy { it.dateString }
        val list = mutableListOf<DayStripItem>()
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        val todayCal = Calendar.getInstance()

        val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")

        for (i in 0..6) {
            val dateStr = sdf.format(cal.time)
            val isToday = cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) &&
                    cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
            val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon..
            val dayLetter = dayLetters[dow - 1]
            val dayNumber = cal.get(Calendar.DAY_OF_MONTH).toString()
            val iso = if (dow == Calendar.SUNDAY) 7 else dow - 1

            list.add(
                DayStripItem(
                    dateString = dateStr,
                    dayLetter = dayLetter,
                    dayNumber = dayNumber,
                    isToday = isToday,
                    dayOfWeekIso = iso,
                    record = recordMap[dateStr]
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }
}
