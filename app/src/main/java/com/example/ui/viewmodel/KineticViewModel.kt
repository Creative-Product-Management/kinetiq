package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.HabitEntity
import com.example.data.model.HabitModality
import com.example.data.model.HabitRecordEntity
import com.example.data.model.RoutineStackEntity
import com.example.data.repository.HabitListLayout
import com.example.data.repository.HabitRepository
import com.example.data.repository.HabitSortMode
import com.example.data.repository.KineticSettings
import com.example.data.repository.LogRepository
import com.example.data.repository.SettingsRepository
import com.example.di.AppContainer
import com.example.domain.analytics.AnalyticsCache
import com.example.domain.analytics.HabitAnalyticsEngine
import com.example.domain.analytics.HabitAnalyticsResult
import com.example.domain.time.DateCodec
import com.example.domain.time.SystemKineticClock
import com.example.notification.HabitNotificationScheduler
import com.example.ui.components.DayStripItem
import com.example.ui.components.InAppNudgeData
import com.example.ui.components.NudgeType
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** Day-scoped dismissal check: "<id>@yyyy-MM-dd". Bare legacy ids never match, so heroes reappear after upgrade. */
fun isNudgeDismissed(nudgeId: String, dismissedIds: Set<String>, todayString: String): Boolean =
    (nudgeId + "@" + todayString) in dismissedIds

fun nudgeDismissalKey(nudgeId: String, todayString: String): String = nudgeId + "@" + todayString

data class HabitCardUi(
    val habit: HabitEntity,
    val categoryName: String,
    val recentDays: List<DayStripItem>,
    val momentumPercent: Int = 0,
    /** Last 30 days (ends today) for the Home activity chart; the chart shows as many as fit on screen. */
    val activityDays: List<DayStripItem> = emptyList(),
    /**
     * Per-day momentum (0..100) for the same 30-day window as [activityDays],
     * oldest first. Null before the habit's start date. Computed from the full
     * 91-day momentum record window so early days carry EMA memory.
     */
    val momentumHistory: List<Float?> = emptyList()
)

/** Pure day-strip builder: [days] entries ending at [today], oldest first. */
fun buildDayStrip(habitRecords: List<HabitRecordEntity>, today: LocalDate, days: Int): List<DayStripItem> {
    require(days > 0) { "days must be positive" }
    val recordMap = habitRecords.associateBy { it.dateString }
    return ((days - 1) downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        DayStripItem(
            dateString = DateCodec.format(date),
            dayLetter = date.dayOfWeek.name.take(1),
            dayNumber = date.dayOfMonth.toString(),
            isToday = offset == 0,
            dayOfWeekIso = date.dayOfWeek.value,
            record = recordMap[DateCodec.format(date)]
        )
    }
}

/**
 * MANUAL keeps the saved home order. Momentum modes auto-sort by score;
 * ties keep manual relative order ([orderIndex]) in both directions, so
 * ascending is not simply the descending list reversed.
 */
fun sortHabitCards(cards: List<HabitCardUi>, mode: HabitSortMode): List<HabitCardUi> = when (mode) {
    HabitSortMode.MANUAL -> cards
    HabitSortMode.MOMENTUM_DESCENDING -> cards.sortedWith(compareByDescending<HabitCardUi> { it.momentumPercent }.thenBy { it.habit.orderIndex })
    HabitSortMode.MOMENTUM_ASCENDING -> cards.sortedWith(compareBy<HabitCardUi> { it.momentumPercent }.thenBy { it.habit.orderIndex })
}

/**
 * Hero slides for [today]: a stable slideshow that is always present while
 * the user has habits. Slide ids, order, and types never change when habits
 * are logged — logging today only updates the *text* of the first slide in
 * place, so ticking a day circle never swaps the hero to a different slide.
 *
 * Slides: 1) Today's Momentum status, 2) Momentum progress, 3) Rest Shield,
 * 4) Routine stacks, plus an optional friction insight appended last.
 */
fun buildInAppNudges(
    habits: List<HabitEntity>,
    records: List<HabitRecordEntity>,
    stacks: List<RoutineStackEntity>,
    today: LocalDate,
    momentumPercents: Map<Long, Int> = emptyMap()
): List<InAppNudgeData> {
    if (habits.isEmpty()) return emptyList()
    val todayStr = DateCodec.format(today)
    val doneToday = records.filter { it.dateString == todayStr && (it.isCompleted || it.isFloorSustained) }.map { it.habitId }.toSet()
    val stackById = stacks.associateBy { it.id }

    // 1) Today's status — same slide before and after logging today.
    val pendingToday = habits.firstOrNull { h -> h.id !in doneToday && !h.isRestDay(today.dayOfWeek.value) }
    val todaySlide = InAppNudgeData(
        id = "today-momentum",
        type = NudgeType.MOMENTUM_BOOST,
        title = "Today's Momentum",
        subtitle = if (pendingToday != null) {
            "${pendingToday.title} is not logged yet today. A floor log still counts."
        } else {
            "Everything due today is logged — today's Momentum is protected. Nice work."
        },
        actionLabel = if (pendingToday != null) "Log floor" else "View habit",
        targetHabitId = (pendingToday ?: habits.first()).id
    )

    // 2) Momentum progress — strongest habit, or how the score builds.
    val strongest = habits.maxByOrNull { momentumPercents[it.id] ?: 0 }
    val strongestPercent = strongest?.let { momentumPercents[it.id] ?: 0 } ?: 0
    val progressSlide = InAppNudgeData(
        id = "momentum-progress",
        type = NudgeType.INSIGHT,
        title = "Momentum builds day by day",
        subtitle = if (strongest != null && strongestPercent > 0) {
            "${strongest.title} is your strongest habit at $strongestPercent%. A target day adds about 7% — consistency compounds."
        } else {
            "A target day adds about 7%. Seven target days in a row reach roughly 40%, and a floor day still earns 65% of a target."
        },
        actionLabel = "View habit",
        targetHabitId = (strongest ?: habits.first()).id
    )

    // 3) Rest Shield — tomorrow's rest day if one is scheduled, else the concept.
    val restTomorrow = habits.firstOrNull { it.isRestDay(today.plusDays(1).dayOfWeek.value) }
    val restSlide = InAppNudgeData(
        id = "rest-shield",
        type = NudgeType.REST_SHIELD,
        title = if (restTomorrow != null) "Rest shield tomorrow" else "Rest days protect Momentum",
        subtitle = if (restTomorrow != null) {
            "${restTomorrow.title} has a scheduled rest day tomorrow — decay stays frozen."
        } else {
            "Schedule rest days in a habit's editor and Momentum freezes on those days — no gain, no loss, streak preserved."
        },
        actionLabel = "View habit",
        targetHabitId = (restTomorrow ?: habits.first()).id
    )

    // 4) Routine stacks — next stacked habit if one is pending, else the concept.
    val stackPending = habits.firstOrNull { it.routineStackId != null && it.id !in doneToday }
    val stackSlide = InAppNudgeData(
        id = "routine-stacks",
        type = NudgeType.STACK_PRIMED,
        title = if (stackPending != null) "${stackById[stackPending.routineStackId]?.name ?: "Your stack"} is ready" else "Stack habits into routines",
        subtitle = if (stackPending != null) {
            "${stackPending.title} is next in ${stackById[stackPending.routineStackId]?.name ?: "your stack"} and not yet logged today."
        } else {
            "Group related habits into a routine stack in the habit editor, so finishing one habit cues the next."
        },
        actionLabel = if (stackPending != null) "Open habit" else "View habit",
        targetHabitId = (stackPending ?: habits.first()).id
    )

    val slides = mutableListOf(todaySlide, progressSlide, restSlide, stackSlide)

    // Optional, appended last so the core slideshow order never shifts.
    val frictionCount = records.count { it.frictionTag != null }
    if (frictionCount > 0) {
        slides += InAppNudgeData("insight-friction", NudgeType.INSIGHT, "Friction logged $frictionCount time${if (frictionCount == 1) "" else "s"}", "Open a habit's analytics to see what most often blocks you.", "Review habits", targetHabitId = habits.first().id)
    }
    return slides
}

@OptIn(ExperimentalCoroutinesApi::class)
class KineticViewModel(
    private val habitRepository: HabitRepository,
    private val logRepository: LogRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationScheduler: HabitNotificationScheduler,
    private val analyticsCache: AnalyticsCache,
    private val container: AppContainer
) : ViewModel() {

    val settings: StateFlow<KineticSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KineticSettings())
    val themeMode: StateFlow<ThemeMode> = settings.map { it.themeMode }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)
    val weekStartsOnMonday: StateFlow<Boolean> = settings.map { it.weekStartsOnMonday }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val defaultFloorPercent: StateFlow<Int> = settings.map { it.defaultFloorPercent }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)
    val restShieldEnabled: StateFlow<Boolean> = settings.map { it.restShieldEnabled }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val dailyEveningDigestEnabled: StateFlow<Boolean> = settings.map { it.dailyEveningDigestEnabled }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val dailyEveningDigestHour: StateFlow<Int> = settings.map { it.dailyEveningDigestHour }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21)
    /** Permanent dashboard hero preference. Defaults to visible; Settings can hide it for good. */
    val showHeroSection: StateFlow<Boolean> = settings.map { it.showHeroSection }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    /** Permanent Home activity chart preference. Defaults to visible; Settings or dismiss hide it. */
    val showActivityChart: StateFlow<Boolean> = settings.map { it.showActivityChart }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val sortMode: StateFlow<HabitSortMode> = settings.map { it.sortMode }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitSortMode.MANUAL)
    val habitListLayout: StateFlow<HabitListLayout> = settings.map { it.habitListLayout }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitListLayout.CARD)

    fun setSortMode(mode: HabitSortMode) { viewModelScope.launch { settingsRepository.setSortMode(mode) } }
    fun setHabitListLayout(layout: HabitListLayout) { viewModelScope.launch { settingsRepository.setHabitListLayout(layout) } }
    fun toggleHabitListLayout() { setHabitListLayout(habitListLayout.value.toggled()) }

    /** Momentum chip tap: selects highest-first from Manual, then flips direction on each re-tap. */
    fun selectMomentumSort() { setSortMode(sortMode.value.nextMomentum()) }

    val allHabits: StateFlow<List<HabitEntity>> = habitRepository.activeHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedHabits: StateFlow<List<HabitEntity>> = habitRepository.archivedHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories: StateFlow<List<Category>> = habitRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStacks: StateFlow<List<RoutineStackEntity>> = habitRepository.allStacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Windowed (14-day) records only — the dashboard never loads full history. */
    val dashboardRecords: StateFlow<List<HabitRecordEntity>> = logRepository.dashboardRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    /** Back-compat alias used by older call sites. */
    val allRecords: StateFlow<List<HabitRecordEntity>> get() = dashboardRecords

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isStackView = MutableStateFlow(false)
    val isStackView: StateFlow<Boolean> = _isStackView.asStateFlow()

    private val _selectedHabitForDetail = MutableStateFlow<HabitEntity?>(null)
    val selectedHabitForDetail: StateFlow<HabitEntity?> = _selectedHabitForDetail.asStateFlow()

    val detailRecords: StateFlow<List<HabitRecordEntity>> = _selectedHabitForDetail
        .flatMapLatest { habit -> if (habit == null) flowOf(emptyList()) else logRepository.recordsForHabitWindow(habit.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The analytics cache is keyed by the records list itself (see
    // AnalyticsCache), so the cached result always matches the records in
    // this emission. A separate version flow must not be used here: it can
    // emit before the records flow and poison the cache with a result
    // computed from a stale record list.
    val selectedAnalytics: StateFlow<HabitAnalyticsResult?> = combine(
        _selectedHabitForDetail, detailRecords, settings
    ) { habit, records, s ->
        if (habit == null) null
        else analyticsCache.get(habit, records, s.restShieldEnabled, s.weekStartsOnMonday) ?: HabitAnalyticsEngine.calculateAnalytics(
            habit, records, restShieldEnabled = s.restShieldEnabled, weekStartsOnMonday = s.weekStartsOnMonday
        ).also { analyticsCache.put(habit, records, s.restShieldEnabled, s.weekStartsOnMonday, it) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Records in the Momentum window (last 91 days), kept separate from the 14-day day-strip window. */
    val momentumRecords: StateFlow<List<HabitRecordEntity>> = logRepository.momentumRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Precomputed card UI state; LazyColumn never does date math during composition. */
    val habitCards: StateFlow<List<HabitCardUi>> = combine(allHabits, dashboardRecords, momentumRecords, allCategories, settings) { habits, records, momentum, categories, s ->
        val catNames = categories.associate { it.id to it.name }
        val byHabit = records.groupBy { it.habitId }
        val momentumByHabit = momentum.groupBy { it.habitId }
        habits.map { habit ->
            val habitRecords = byHabit[habit.id].orEmpty()
            val momentumLogs = momentumByHabit[habit.id].orEmpty()
            val history = HabitAnalyticsEngine.momentumHistory(
                habit, momentumLogs, restShieldEnabled = s.restShieldEnabled, days = 30
            )
            HabitCardUi(
                habit, catNames[habit.categoryId] ?: "General",
                buildRecentDays(habit.id, habitRecords, s.weekStartsOnMonday, days = CARD_DAY_STRIP_DAYS),
                // Prefer the history's last point so the badge matches the Home line.
                momentumPercent = history.lastOrNull()?.roundToInt()?.coerceIn(0, 100)
                    ?: HabitAnalyticsEngine.calculateMomentumPercent(habit, momentumLogs, restShieldEnabled = s.restShieldEnabled),
                // Activity chart can show up to 30 days; source from the wider
                // momentum window (the dashboard window is only 14 days).
                activityDays = buildRecentDays(habit.id, momentumLogs, days = 30),
                momentumHistory = history
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isEditorOpen = MutableStateFlow(false)
    val isEditorOpen: StateFlow<Boolean> = _isEditorOpen.asStateFlow()
    private val _habitForEditor = MutableStateFlow<HabitEntity?>(null)
    val habitForEditor: StateFlow<HabitEntity?> = _habitForEditor.asStateFlow()
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()
    private val _isNotificationSettingsOpen = MutableStateFlow(false)
    val isNotificationSettingsOpen: StateFlow<Boolean> = _isNotificationSettingsOpen.asStateFlow()
    private val _activeTimerHabit = MutableStateFlow<HabitEntity?>(null)
    val activeTimerHabit: StateFlow<HabitEntity?> = _activeTimerHabit.asStateFlow()
    private val _quickLogTarget = MutableStateFlow<Pair<HabitEntity, DayStripItem>?>(null)
    val quickLogTarget: StateFlow<Pair<HabitEntity, DayStripItem>?> = _quickLogTarget.asStateFlow()
    private val _lastArchived = MutableStateFlow<HabitEntity?>(null)
    val lastArchived: StateFlow<HabitEntity?> = _lastArchived.asStateFlow()
    private val _pendingDeepLinkHabitId = MutableStateFlow<Long?>(null)

    /** Hero slideshow slides (from precomputed cards so Momentum is real), with dismissals filtered out. */
    val inAppNudges: StateFlow<List<InAppNudgeData>> = combine(habitCards, dashboardRecords, allStacks, settings) { cards, records, stacks, s ->
        // Dismissals are scoped to a single day ("<id>@yyyy-MM-dd") so a slide dismissed
        // yesterday reappears in a new session today. Bare ids written by older versions
        // are intentionally ignored, so previously dismissed heroes reappear after update.
        val today = DateCodec.todayString()
        buildInAppNudges(
            habits = cards.map { it.habit },
            records = records,
            stacks = stacks,
            today = SystemKineticClock.today(),
            momentumPercents = cards.associate { it.habit.id to it.momentumPercent }
        ).filterNot { nudge ->
            isNudgeDismissed(nudge.id, s.dismissedNudgeIds, today)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pending notification deep link: fire only once habits have loaded.
        viewModelScope.launch {
            combine(_pendingDeepLinkHabitId, allHabits) { id, habits -> id to habits }.collect { (id, habits) ->
                if (id != null) habits.firstOrNull { it.id == id }?.let { openHabitDetail(it); _pendingDeepLinkHabitId.value = null }
            }
        }
        // Keep evening digest alarm aligned with settings.
        viewModelScope.launch {
            settings.collect { s ->
                if (s.dailyEveningDigestEnabled) notificationScheduler.scheduleEveningDigest(s.dailyEveningDigestHour)
                else notificationScheduler.cancelEveningDigest()
            }
        }
    }

    fun onNotificationDeepLink(habitId: Long) { if (habitId > 0) _pendingDeepLinkHabitId.value = habitId }
    fun setCategory(category: String) { _selectedCategory.value = category }
    fun setCategoryId(id: Long?) { _selectedCategoryId.value = id }
    fun toggleStackView() { _isStackView.value = !_isStackView.value }
    fun openHabitDetail(habit: HabitEntity) { _selectedHabitForDetail.value = habit }
    fun selectHabitById(id: Long) { allHabits.value.firstOrNull { it.id == id }?.let { openHabitDetail(it) } }
    fun closeHabitDetail() { _selectedHabitForDetail.value = null }
    fun openNewHabitEditor() { _habitForEditor.value = null; _isEditorOpen.value = true }
    fun openEditHabitEditor(habit: HabitEntity) { _habitForEditor.value = habit; _isEditorOpen.value = true }
    fun closeEditor() { _isEditorOpen.value = false; _habitForEditor.value = null }
    fun openNotificationSettings() { _isNotificationSettingsOpen.value = true }
    fun closeNotificationSettings() { _isNotificationSettingsOpen.value = false }
    fun openTimer(habit: HabitEntity) { _activeTimerHabit.value = habit }
    fun closeTimer() { _activeTimerHabit.value = null }
    fun openQuickLog(habit: HabitEntity, item: DayStripItem) { _quickLogTarget.value = Pair(habit, item) }
    fun closeQuickLog() { _quickLogTarget.value = null }
    fun dismissNudge(id: String) {
        // Day-scoped: dismissing hides this nudge for today only, never permanently.
        viewModelScope.launch { settingsRepository.dismissNudge(nudgeDismissalKey(id, DateCodec.todayString())) }
    }

    fun setShowHeroSection(visible: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowHeroSection(visible)
            // Re-enabling the hero starts fresh so it actually reappears at once.
            if (visible) settingsRepository.clearDismissedNudges()
        }
    }

    fun setShowActivityChart(visible: Boolean) {
        viewModelScope.launch { settingsRepository.setShowActivityChart(visible) }
    }

    fun handleNudgeAction(nudge: InAppNudgeData) {
        val target = nudge.targetHabitId?.let { id -> allHabits.value.firstOrNull { it.id == id } } ?: allHabits.value.firstOrNull()
        if (target != null) openHabitDetail(target)
    }

    fun handleDayClick(habit: HabitEntity, item: DayStripItem) {
        when (habit.modality) {
            HabitModality.YES_NO -> viewModelScope.launch { logRepository.toggleHabitForDate(habit, item.dateString) }
            HabitModality.TALLY -> viewModelScope.launch { logRepository.logTallyIncrement(habit, item.dateString) }
            HabitModality.TIMER -> openQuickLog(habit, item)
            HabitModality.NUMERIC -> {
                if (item.record == null || (!item.record.isCompleted && !item.record.isFloorSustained)) {
                    viewModelScope.launch { logRepository.toggleHabitForDate(habit, item.dateString) }
                } else openQuickLog(habit, item)
            }
        }
    }

    fun logTarget(habit: HabitEntity, dateString: String) { viewModelScope.launch { logRepository.setCompletedForDate(habit, dateString); closeQuickLog() } }
    fun logFloor(habit: HabitEntity, dateString: String) { viewModelScope.launch { logRepository.logFloorSustainForDate(habit, dateString); closeQuickLog() } }
    fun logCustomAmount(habit: HabitEntity, dateString: String, amount: Double) { viewModelScope.launch { logRepository.logNumericProgress(habit, dateString, amount); closeQuickLog() } }
    fun logFriction(habitId: Long, dateString: String, tag: String, notes: String) { viewModelScope.launch { logRepository.logFrictionTag(habitId, dateString, tag, notes); closeQuickLog() } }
    fun clearLog(habitId: Long, dateString: String) { viewModelScope.launch { logRepository.clearRecord(habitId, dateString); closeQuickLog() } }

    fun finishTimerSession(habit: HabitEntity, durationSeconds: Int) {
        if (durationSeconds <= 0) { closeTimer(); return }
        viewModelScope.launch { logRepository.logDurationSeconds(habit, DateCodec.todayString(), durationSeconds); closeTimer() }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val savedId = habitRepository.saveHabit(habit)
            val updated = habit.copy(id = if (habit.id == 0L) savedId else habit.id)
            analyticsCache.invalidate(updated.id)
            if (updated.reminderHour != null && updated.reminderMinute != null && !updated.isArchived) notificationScheduler.scheduleHabitReminder(updated)
            else notificationScheduler.cancelHabitReminder(updated.id)
            closeEditor()
            if (_selectedHabitForDetail.value?.id == updated.id || _selectedHabitForDetail.value?.id == habit.id) _selectedHabitForDetail.value = updated
        }
    }

    /** Archive-first delete: habit disappears from dashboard but can be restored from Settings. */
    fun archiveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.archiveHabit(habit)
            notificationScheduler.cancelHabitReminder(habit.id)
            _lastArchived.value = habit
            closeEditor()
            if (_selectedHabitForDetail.value?.id == habit.id) closeHabitDetail()
        }
    }

    fun undoArchive() { val h = _lastArchived.value ?: return; viewModelScope.launch { habitRepository.restoreHabit(h); _lastArchived.value = null } }
    fun clearLastArchived() { _lastArchived.value = null }
    fun restoreHabit(habit: HabitEntity) { viewModelScope.launch { habitRepository.restoreHabit(habit) } }

    /** Permanent delete (from Settings archived list or editor), with FK cascade for logs. */
    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
            analyticsCache.invalidate(habit.id)
            notificationScheduler.cancelHabitReminder(habit.id)
            closeEditor()
            if (_selectedHabitForDetail.value?.id == habit.id) closeHabitDetail()
        }
    }

    fun moveHabit(habit: HabitEntity, delta: Int) {
        viewModelScope.launch { habitRepository.moveHabit(habit, delta, allHabits.value) }
    }

    fun saveHabitOrder(ordered: List<HabitEntity>) {
        viewModelScope.launch { habitRepository.saveOrder(ordered) }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            if (category.id == 0L) habitRepository.saveCategory(category)
            else habitRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { habitRepository.deleteCategory(category) }
    }

    fun reorderCategories(ordered: List<Category>) {
        viewModelScope.launch { habitRepository.reorderCategories(ordered) }
    }

    fun saveStack(stack: RoutineStackEntity) {
        viewModelScope.launch {
            if (stack.id == 0L) habitRepository.saveStack(stack)
            else habitRepository.updateStack(stack)
        }
    }

    fun deleteStack(stack: RoutineStackEntity) {
        viewModelScope.launch { habitRepository.deleteStack(stack) }
    }

    /** Habits (active + archived) currently linked to this category. */
    fun habitCountForCategory(categoryId: Long): Int =
        (allHabits.value + archivedHabits.value).count { it.categoryId == categoryId }

    /** Habits (active + archived) currently linked to this stack. */
    fun habitCountForStack(stackId: Long): Int =
        (allHabits.value + archivedHabits.value).count { it.routineStackId == stackId }

    fun sendTestNotification() { notificationScheduler.sendTestNotification("Mindful Breathwork") }
    fun canScheduleExactAlarms(): Boolean = notificationScheduler.canScheduleExactAlarms()

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }
    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { settingsRepository.setThemeMode(mode) } }
    fun setWeekStartsOnMonday(value: Boolean) { viewModelScope.launch { settingsRepository.setWeekStartsOnMonday(value) } }
    fun setDefaultFloorPercent(value: Int) { viewModelScope.launch { settingsRepository.setDefaultFloorPercent(value) } }
    fun toggleRestShield(value: Boolean) { viewModelScope.launch { settingsRepository.setRestShieldEnabled(value) } }
    fun toggleDailyEveningDigest(value: Boolean) { viewModelScope.launch { settingsRepository.setDailyEveningDigestEnabled(value) } }
    fun setDailyEveningDigestHour(hour: Int) { viewModelScope.launch { settingsRepository.setDailyEveningDigestHour(hour) } }

    fun resetAllRecords() { viewModelScope.launch { logRepository.clearAllRecords(); analyticsCache.clear() } }
    /** Opt-in sample habit from Settings; no-op if one with the sample title already exists. */
    fun loadSampleHabit() { viewModelScope.launch { habitRepository.loadSampleHabit() } }

    suspend fun exportBackup(passphrase: CharArray): ByteArray = container.backupManager.exportEncrypted(passphrase)
    suspend fun importBackup(bytes: ByteArray, passphrase: CharArray) { container.backupManager.importEncrypted(bytes, passphrase); analyticsCache.clear() }

    /** Generalised strip builder ending today; habit cards use [CARD_DAY_STRIP_DAYS]. */
    fun buildRecentDays(habitId: Long, habitRecords: List<HabitRecordEntity>, weekStartsOnMonday: Boolean = true, days: Int = CARD_DAY_STRIP_DAYS): List<DayStripItem> =
        buildDayStrip(habitRecords, SystemKineticClock.today(), days)

    fun buildRecent7Days(habitId: Long, habitRecords: List<HabitRecordEntity>, weekStartsOnMonday: Boolean = true): List<DayStripItem> =
        buildRecentDays(habitId, habitRecords, weekStartsOnMonday, days = CARD_DAY_STRIP_DAYS)

    /** Legacy helper kept for tests/call sites; prefer [habitCards]. */
    fun getRecent7Days(habitId: Long, records: List<HabitRecordEntity>): List<DayStripItem> =
        buildRecent7Days(habitId, records.filter { it.habitId == habitId })

    companion object {
        /** Days shown on each habit card's quick-log strip (ends today). */
        const val CARD_DAY_STRIP_DAYS = 9
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(KineticViewModel::class.java))
            return KineticViewModel(
                container.habitRepository, container.logRepository, container.settingsRepository,
                container.notificationScheduler, container.analyticsCache, container
            ) as T
        }
    }
}
