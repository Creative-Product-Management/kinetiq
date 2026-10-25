package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.data.model.RoutineStackEntity
import com.example.data.repository.HabitSortMode
import com.example.data.repository.LogRepository
import com.example.domain.analytics.HabitAnalyticsEngine
import com.example.domain.analytics.HeatmapCellState
import com.example.domain.time.FixedKineticClock
import com.example.notification.NextReminderCalculator
import com.example.ui.components.InAppNudgeData
import com.example.ui.viewmodel.HabitCardUi
import com.example.ui.viewmodel.buildInAppNudges
import com.example.ui.viewmodel.sortHabitCards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Dumps deterministic golden fixtures under the repo golden/ directory
 * (one JSON file per scenario).
 * Input: habits / logs / clock / settings.
 * Expected: analytics, reminders, logging outcomes, sort order, nudges, floor.
 *
 * Re-run this test to refresh fixtures after intentional algorithm changes.
 */
class GoldenFixturesDumpTest {

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun habit(
        id: Long = 1,
        title: String = "Test",
        modality: HabitModality = HabitModality.NUMERIC,
        target: Double = 10.0,
        floor: Double? = 2.0,
        unit: String = "reps",
        created: LocalDate,
        restMask: Int = 0,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        reminderDaysMask: Int = 127,
        routineStackId: Long? = null,
        orderIndex: Int = 0
    ) = Habit(
        id = id,
        title = title,
        modality = modality,
        targetValue = target,
        floorValue = floor,
        unit = unit,
        restDaysMask = restMask,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        reminderDaysMask = reminderDaysMask,
        routineStackId = routineStackId,
        orderIndex = orderIndex,
        createdAt = millis(created)
    )

    private fun targetLog(habitId: Long, date: LocalDate) =
        LogEntry(habitId = habitId, dateString = date.toString(), isCompleted = true, numericValue = 10.0)

    private fun floorLog(habitId: Long, date: LocalDate) =
        LogEntry(habitId = habitId, dateString = date.toString(), isFloorSustained = true, numericValue = 2.0)

    private fun frictionLog(habitId: Long, date: LocalDate, tag: String = "Low Energy") =
        LogEntry(habitId = habitId, dateString = date.toString(), frictionTag = tag)

    private fun effectiveFloor(h: Habit): Double =
        h.floorValue ?: (h.targetValue * LogRepository.DEFAULT_FLOOR_RATIO)

    private fun editorFloorPlaceholder(target: Double, defaultFloorPercent: Int): Double =
        target * defaultFloorPercent / 100.0

    /** Mirrors LogRepository flag/value outcomes without Room. */
    private fun loggingOutcome(
        action: String,
        habit: Habit,
        existing: LogEntry? = null,
        amount: Double = 0.0,
        durationSec: Int = 0
    ): Map<String, Any?> {
        val floor = effectiveFloor(habit)
        val out = linkedMapOf<String, Any?>("effectiveFloor" to floor)
        when (action) {
            "completedRecord" -> {
                out["isCompleted"] = true
                out["isFloorSustained"] = false
                out["numericValue"] = habit.targetValue
                out["durationSeconds"] =
                    if (habit.modality == HabitModality.TIMER) (habit.targetValue * 60).toInt()
                    else existing?.durationSeconds ?: 0
                out["tallyCount"] =
                    if (habit.modality == HabitModality.TALLY) habit.targetValue.toInt()
                    else maxOf(existing?.tallyCount ?: 0, 1)
                out["deleted"] = false
            }
            "logFloorSustain" -> {
                if (existing?.isCompleted == true) {
                    out["noOp"] = true
                } else {
                    out["isCompleted"] = false
                    out["isFloorSustained"] = true
                    out["numericValue"] = floor
                    out["durationSeconds"] =
                        if (habit.modality == HabitModality.TIMER) (floor * 60).toInt()
                        else existing?.durationSeconds ?: 0
                    out["tallyCount"] =
                        if (habit.modality == HabitModality.TALLY) floor.toInt().coerceAtLeast(1)
                        else existing?.tallyCount ?: 0
                    out["deleted"] = false
                }
            }
            "logNumericProgress" -> {
                if (amount <= 0.0) {
                    out["deleted"] = true
                } else {
                    val isMet = amount >= habit.targetValue
                    out["isCompleted"] = isMet
                    out["isFloorSustained"] = !isMet && amount >= floor
                    out["numericValue"] = amount
                    out["deleted"] = false
                }
            }
            "logTallyIncrement" -> {
                val next = (existing?.tallyCount ?: 0) + 1
                val isMet = next >= habit.targetValue
                out["tallyCount"] = next
                out["numericValue"] = next.toDouble()
                out["isCompleted"] = isMet
                out["isFloorSustained"] = !isMet && next >= floor
                out["deleted"] = false
            }
            "logDurationSeconds" -> {
                if (durationSec <= 0) {
                    out["noOp"] = true
                } else {
                    val totalSec = (existing?.durationSeconds ?: 0) + durationSec
                    val isMet = totalSec >= (habit.targetValue * 60).toInt()
                    out["durationSeconds"] = totalSec
                    out["numericValue"] = totalSec / 60.0
                    out["isCompleted"] = isMet
                    out["isFloorSustained"] = !isMet && totalSec >= (floor * 60).toInt()
                    out["deleted"] = false
                }
            }
            else -> error("unknown action $action")
        }
        return out
    }

    private fun analyticsExpected(
        h: Habit,
        records: List<LogEntry>,
        today: LocalDate,
        restShieldEnabled: Boolean = true,
        weekStartsOnMonday: Boolean = true
    ): Map<String, Any?> {
        val clock = FixedKineticClock(today)
        val result = HabitAnalyticsEngine.calculateAnalytics(
            h, records, clock = clock,
            restShieldEnabled = restShieldEnabled,
            weekStartsOnMonday = weekStartsOnMonday
        )
        val percent = HabitAnalyticsEngine.calculateMomentumPercent(
            h, records, clock = clock, restShieldEnabled = restShieldEnabled
        )
        val history7 = HabitAnalyticsEngine.momentumHistory(
            h, records, clock = clock, restShieldEnabled = restShieldEnabled, days = 7
        )
        val stateCounts = linkedMapOf<String, Int>()
        HeatmapCellState.entries.forEach { stateCounts[it.name] = 0 }
        result.heatmapCells.forEach { stateCounts[it.state.name] = stateCounts.getValue(it.state.name) + 1 }
        val recentNonNone = result.heatmapCells
            .filter { it.state != HeatmapCellState.NONE }
            .takeLast(21)
            .map { cell ->
                linkedMapOf(
                    "date" to cell.dateString,
                    "dayOfWeek" to cell.dayOfWeek,
                    "state" to cell.state.name,
                    "tooltip" to cell.tooltip
                )
            }
        val todayCell = result.heatmapCells.find { it.dateString == today.toString() }
        return linkedMapOf(
            "momentumPercent" to percent,
            "habitStrengthPercent" to result.habitStrengthPercent,
            "trajectory" to result.trajectory,
            "currentStreak" to result.currentStreak,
            "bestStreak" to result.bestStreak,
            "totalTargetCompletions" to result.totalTargetCompletions,
            "totalFloorSustains" to result.totalFloorSustains,
            "dayOfWeekRates" to result.dayOfWeekRates.map { it.toDouble() },
            "frictionCounts" to result.frictionCounts.toSortedMap(),
            "heatmapSize" to result.heatmapCells.size,
            "heatmapFirstDayOfWeek" to result.heatmapCells.first().dayOfWeek,
            "heatmapStateCounts" to stateCounts,
            "todayHeatmapState" to todayCell?.state?.name,
            "todayHeatmapTooltip" to todayCell?.tooltip,
            "recentNonNoneHeatmapCells" to recentNonNone,
            "momentumHistoryLast7" to history7.map { it?.toDouble() },
            "strengthHistoryLast3" to result.strengthHistory.takeLast(3).map { (d, s) ->
                linkedMapOf("date" to d, "strengthTimes100" to s.toDouble())
            }
        )
    }

    private fun nudgesExpected(
        habits: List<Habit>,
        records: List<LogEntry>,
        stacks: List<RoutineStackEntity>,
        today: LocalDate,
        momentum: Map<Long, Int>
    ): List<Map<String, Any?>> {
        return buildInAppNudges(habits, records, stacks, today, momentum).map { n: InAppNudgeData ->
            linkedMapOf(
                "id" to n.id,
                "type" to n.type.name,
                "title" to n.title,
                "subtitle" to n.subtitle,
                "actionLabel" to n.actionLabel,
                "targetHabitId" to n.targetHabitId
            )
        }
    }

    private fun goldenDir(): File {
        val cwd = File(System.getProperty("user.dir"))
        val root = when {
            cwd.resolve("ALGORITHMS.md").exists() -> cwd
            cwd.name == "app" && cwd.parentFile.resolve("ALGORITHMS.md").exists() -> cwd.parentFile
            cwd.parentFile?.resolve("ALGORITHMS.md")?.exists() == true -> cwd.parentFile
            else -> cwd
        }
        return File(root, "golden").also { it.mkdirs() }
    }

    private data class Scenario(
        val id: String,
        val description: String,
        val build: () -> Pair<Map<String, Any?>, Map<String, Any?>>
    )

    private fun habitJson(h: Habit) = linkedMapOf(
        "id" to h.id,
        "title" to h.title,
        "modality" to h.modality.name,
        "targetValue" to h.targetValue,
        "floorValue" to h.floorValue,
        "unit" to h.unit,
        "restDaysMask" to h.restDaysMask,
        "reminderHour" to h.reminderHour,
        "reminderMinute" to h.reminderMinute,
        "reminderDaysMask" to h.reminderDaysMask,
        "routineStackId" to h.routineStackId,
        "orderIndex" to h.orderIndex,
        "createdAt" to h.createdAt,
        "createdAtLocalDate" to java.time.Instant.ofEpochMilli(h.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDate().toString()
    )

    private fun logJson(e: LogEntry) = linkedMapOf(
        "habitId" to e.habitId,
        "dateString" to e.dateString,
        "isCompleted" to e.isCompleted,
        "isFloorSustained" to e.isFloorSustained,
        "numericValue" to e.numericValue,
        "durationSeconds" to e.durationSeconds,
        "tallyCount" to e.tallyCount,
        "frictionTag" to e.frictionTag
    )

    private fun scenarios(): List<Scenario> {
        val today = LocalDate.of(2026, 10, 21) // Wednesday
        val list = mutableListOf<Scenario>()

        fun add(id: String, description: String, build: () -> Pair<Map<String, Any?>, Map<String, Any?>>) {
            list += Scenario(id, description, build)
        }

        add("01-new-habit-first-target", "Created today, one target -> 7% Accelerating, streak 1") {
            val h = habit(created = today)
            val logs = listOf(targetLog(1, today))
            linkedMapOf(
                "clockToday" to today.toString(),
                "settings" to linkedMapOf("restShieldEnabled" to true, "weekStartsOnMonday" to true),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf("analytics" to analyticsExpected(h, logs, today))
        }

        add("02-seven-target-days-backfill", "7 consecutive targets including backfills -> ~40%") {
            val h = habit(created = today)
            val logs = (0..6).map { targetLog(1, today.minusDays((6 - it).toLong())) }
            linkedMapOf(
                "clockToday" to today.toString(),
                "settings" to linkedMapOf("restShieldEnabled" to true, "weekStartsOnMonday" to true),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf("analytics" to analyticsExpected(h, logs, today))
        }

        add("03-empty-logs-zero-momentum", "Habit exists, no logs -> 0%") {
            val h = habit(created = today.minusDays(10))
            linkedMapOf(
                "clockToday" to today.toString(),
                "settings" to linkedMapOf("restShieldEnabled" to true, "weekStartsOnMonday" to true),
                "habits" to listOf(habitJson(h)),
                "logs" to emptyList<Any>()
            ) to linkedMapOf("analytics" to analyticsExpected(h, emptyList(), today))
        }

        add("04-rest-shield-preserves-streak", "Rest day bridges streak without inflating") {
            val created = LocalDate.of(2026, 10, 18)
            val h = habit(created = created, restMask = 1 shl 1)
            val logs = listOf(targetLog(1, created), targetLog(1, created.plusDays(1)))
            linkedMapOf(
                "clockToday" to today.toString(),
                "settings" to linkedMapOf("restShieldEnabled" to true, "weekStartsOnMonday" to true),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf("analytics" to analyticsExpected(h, logs, today))
        }

        add("05-rest-shield-disabled-counts-as-miss", "Same rest mask but shield off -> streak broken") {
            val created = LocalDate.of(2026, 10, 18)
            val h = habit(created = created, restMask = 1 shl 1)
            val logs = listOf(targetLog(1, created), targetLog(1, created.plusDays(1)))
            linkedMapOf(
                "clockToday" to today.toString(),
                "settings" to linkedMapOf("restShieldEnabled" to false, "weekStartsOnMonday" to true),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf("analytics" to analyticsExpected(h, logs, today, restShieldEnabled = false))
        }

        add("06-log-on-rest-day-unfreezes-ema", "Target on rest day updates EMA (no freeze)") {
            val created = today.minusDays(5)
            val h = habit(created = created, restMask = 1 shl (today.dayOfWeek.value - 1))
            val logs = listOf(targetLog(1, today))
            val withLog = analyticsExpected(h, logs, today)
            val without = analyticsExpected(h, emptyList(), today)
            linkedMapOf(
                "clockToday" to today.toString(),
                "settings" to linkedMapOf("restShieldEnabled" to true, "weekStartsOnMonday" to true),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf(
                "analyticsWithLogOnRest" to withLog,
                "analyticsRestOnlyNoLog" to without,
                "momentumIncreased" to ((withLog["momentumPercent"] as Int) > (without["momentumPercent"] as Int))
            )
        }

        add("07-floor-vs-target-vs-miss", "Floor credit between target and miss") {
            val created = today.minusDays(9)
            val h = habit(created = created)
            val t = analyticsExpected(h, listOf(targetLog(1, today)), today)
            val f = analyticsExpected(h, listOf(floorLog(1, today)), today)
            val m = analyticsExpected(h, emptyList(), today)
            linkedMapOf(
                "clockToday" to today.toString(),
                "habits" to listOf(habitJson(h))
            ) to linkedMapOf(
                "target" to t,
                "floor" to f,
                "miss" to m,
                "ordering" to linkedMapOf(
                    "targetGtFloor" to ((t["momentumPercent"] as Int) > (f["momentumPercent"] as Int)),
                    "floorGteMiss" to ((f["momentumPercent"] as Int) >= (m["momentumPercent"] as Int))
                )
            )
        }

        add("08-new-year-boundary-streak", "Streak across Dec 31 / Jan 1") {
            val t = LocalDate.of(2027, 1, 2)
            val created = LocalDate.of(2026, 12, 30)
            val h = habit(created = created)
            val logs = listOf("2026-12-30", "2026-12-31", "2027-01-01").map {
                LogEntry(habitId = 1, dateString = it, isCompleted = true)
            }
            linkedMapOf(
                "clockToday" to t.toString(),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf("analytics" to analyticsExpected(h, logs, t))
        }

        add("09-miss-breaks-current-streak", "Gap day ends current streak; best may remain") {
            val created = today.minusDays(5)
            val h = habit(created = created)
            val logs = listOf(
                targetLog(1, today.minusDays(4)),
                targetLog(1, today.minusDays(3)),
                targetLog(1, today)
            )
            linkedMapOf(
                "clockToday" to today.toString(),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf("analytics" to analyticsExpected(h, logs, today))
        }

        add("10-grace-today-unlogged", "Unlogged today uses yesterday for streak cursor") {
            val created = today.minusDays(3)
            val h = habit(created = created)
            val logs = listOf(targetLog(1, today.minusDays(2)), targetLog(1, today.minusDays(1)))
            linkedMapOf(
                "clockToday" to today.toString(),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf("analytics" to analyticsExpected(h, logs, today))
        }

        add("11-heatmap-monday-alignment", "140 cells, week starts Monday") {
            val h = habit(created = today.minusDays(200))
            linkedMapOf(
                "clockToday" to today.toString(),
                "settings" to linkedMapOf("weekStartsOnMonday" to true),
                "habits" to listOf(habitJson(h))
            ) to linkedMapOf("analytics" to analyticsExpected(h, emptyList(), today, weekStartsOnMonday = true))
        }

        add("12-heatmap-sunday-week-start", "Week starts Sunday") {
            val h = habit(created = today.minusDays(200))
            linkedMapOf(
                "clockToday" to today.toString(),
                "settings" to linkedMapOf("weekStartsOnMonday" to false),
                "habits" to listOf(habitJson(h))
            ) to linkedMapOf("analytics" to analyticsExpected(h, emptyList(), today, weekStartsOnMonday = false))
        }

        add("13-heatmap-friction-is-missed", "Friction tag -> MISSED; bare miss -> NONE") {
            val created = today.minusDays(10)
            val h = habit(created = created)
            val withFriction = listOf(frictionLog(1, today.minusDays(1)))
            linkedMapOf(
                "clockToday" to today.toString(),
                "habits" to listOf(habitJson(h)),
                "logsWithFriction" to withFriction.map { logJson(it) }
            ) to linkedMapOf(
                "withFriction" to analyticsExpected(h, withFriction, today),
                "bareMiss" to analyticsExpected(h, emptyList(), today)
            )
        }

        add("14-dow-rates-sixty-day-window", "DOW rates over 60 days with Mon-only targets") {
            val created = today.minusDays(60)
            val h = habit(created = created)
            val logs = (0..60).mapNotNull { offset ->
                val d = created.plusDays(offset.toLong())
                if (d.dayOfWeek.value == 1 && !d.isAfter(today)) targetLog(1, d) else null
            }
            linkedMapOf(
                "clockToday" to today.toString(),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf("analytics" to analyticsExpected(h, logs, today))
        }

        add("15-trajectory-steady-high", "Long target streak -> Steady High or Accelerating") {
            val created = today.minusDays(40)
            val h = habit(created = created)
            val logs = (0..39).map { targetLog(1, created.plusDays(it.toLong())) }
            linkedMapOf(
                "clockToday" to today.toString(),
                "habits" to listOf(habitJson(h)),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf("analytics" to analyticsExpected(h, logs, today))
        }

        add("16-effective-floor-null-uses-025", "Null floorValue -> target * 0.25 at runtime") {
            val h = habit(created = today, floor = null, target = 100.0)
            linkedMapOf(
                "habit" to habitJson(h),
                "settingsDefaultFloorPercent" to 33
            ) to linkedMapOf(
                "runtimeEffectiveFloor" to effectiveFloor(h),
                "editorPlaceholderAt33" to editorFloorPlaceholder(100.0, 33),
                "settingsIgnoredAtRuntime" to (effectiveFloor(h) != editorFloorPlaceholder(100.0, 33))
            )
        }

        add("17-effective-floor-explicit", "Explicit floorValue wins over 0.25") {
            val h = habit(created = today, floor = 7.0, target = 100.0)
            linkedMapOf("habit" to habitJson(h)) to
                linkedMapOf("runtimeEffectiveFloor" to effectiveFloor(h))
        }

        add("18-log-numeric-below-floor-miss", "Amount below floor -> neither completed nor floor") {
            val h = habit(created = today, modality = HabitModality.NUMERIC, target = 10.0, floor = 3.0)
            linkedMapOf(
                "habit" to habitJson(h),
                "action" to "logNumericProgress",
                "amount" to 2.0
            ) to linkedMapOf("outcome" to loggingOutcome("logNumericProgress", h, amount = 2.0))
        }

        add("19-log-numeric-floor-band", "Amount in [floor, target) -> floor sustained") {
            val h = habit(created = today, modality = HabitModality.NUMERIC, target = 10.0, floor = 3.0)
            linkedMapOf(
                "habit" to habitJson(h),
                "action" to "logNumericProgress",
                "amount" to 5.0
            ) to linkedMapOf("outcome" to loggingOutcome("logNumericProgress", h, amount = 5.0))
        }

        add("20-log-numeric-target-and-delete", "Amount >= target -> completed; <=0 -> deleted") {
            val h = habit(created = today, modality = HabitModality.NUMERIC, target = 10.0, floor = 3.0)
            linkedMapOf(
                "habit" to habitJson(h),
                "action" to "logNumericProgress"
            ) to linkedMapOf(
                "atTarget" to loggingOutcome("logNumericProgress", h, amount = 10.0),
                "aboveTarget" to loggingOutcome("logNumericProgress", h, amount = 15.0),
                "zeroDeletes" to loggingOutcome("logNumericProgress", h, amount = 0.0)
            )
        }

        add("21-log-tally-increment-to-floor-and-target", "Tally steps through floor then target") {
            val h = habit(created = today, modality = HabitModality.TALLY, target = 3.0, floor = 2.0, unit = "times")
            val after1 = loggingOutcome("logTallyIncrement", h, existing = null)
            val after2 = loggingOutcome(
                "logTallyIncrement", h,
                existing = LogEntry(habitId = 1, dateString = today.toString(), tallyCount = 1, numericValue = 1.0)
            )
            val after3 = loggingOutcome(
                "logTallyIncrement", h,
                existing = LogEntry(
                    habitId = 1, dateString = today.toString(), tallyCount = 2, numericValue = 2.0, isFloorSustained = true
                )
            )
            linkedMapOf("habit" to habitJson(h)) to
                linkedMapOf("after1" to after1, "after2" to after2, "after3" to after3)
        }

        add("22-log-timer-duration-floor-and-target", "Timer seconds vs floor/target minutes") {
            val h = habit(created = today, modality = HabitModality.TIMER, target = 20.0, floor = 5.0, unit = "mins")
            linkedMapOf("habit" to habitJson(h)) to linkedMapOf(
                "belowFloor" to loggingOutcome("logDurationSeconds", h, durationSec = 60),
                "atFloor" to loggingOutcome("logDurationSeconds", h, durationSec = 5 * 60),
                "atTarget" to loggingOutcome("logDurationSeconds", h, durationSec = 20 * 60),
                "zeroNoOp" to loggingOutcome("logDurationSeconds", h, durationSec = 0),
                "accumulate" to loggingOutcome(
                    "logDurationSeconds", h,
                    existing = LogEntry(habitId = 1, dateString = today.toString(), durationSeconds = 4 * 60),
                    durationSec = 60
                )
            )
        }

        add("23-log-yesno-completed-record", "YES_NO completedRecord shape") {
            val h = habit(created = today, modality = HabitModality.YES_NO, target = 1.0, floor = null, unit = "")
            linkedMapOf("habit" to habitJson(h)) to
                linkedMapOf("outcome" to loggingOutcome("completedRecord", h))
        }

        add("24-log-floor-no-op-if-completed", "Floor sustain ignored when already completed") {
            val h = habit(created = today)
            val existing = LogEntry(habitId = 1, dateString = today.toString(), isCompleted = true, numericValue = 10.0)
            linkedMapOf("habit" to habitJson(h), "existingCompleted" to true) to
                linkedMapOf("outcome" to loggingOutcome("logFloorSustain", h, existing = existing))
        }

        add("25-reminder-later-today", "Before reminder hour -> fires today") {
            val h = habit(created = today, reminderHour = 8, reminderMinute = 0, reminderDaysMask = 127)
            val from = today.atTime(7, 0)
            linkedMapOf("habit" to habitJson(h), "from" to from.toString()) to
                linkedMapOf("nextFire" to NextReminderCalculator.nextFire(h, from)?.toString())
        }

        add("26-reminder-skips-weekend", "Fri after hour -> next Mon") {
            val fri = LocalDate.of(2026, 10, 23)
            val h = habit(
                created = fri.minusDays(30),
                reminderHour = 8, reminderMinute = 0,
                reminderDaysMask = 0b0011111
            )
            val from = fri.atTime(9, 0)
            linkedMapOf("habit" to habitJson(h), "from" to from.toString()) to
                linkedMapOf("nextFire" to NextReminderCalculator.nextFire(h, from)?.toString())
        }

        add("27-reminder-skips-rest-day", "Rest Wed -> Thu") {
            val wed = LocalDate.of(2026, 10, 21)
            val h = habit(
                created = wed.minusDays(10),
                reminderHour = 8, reminderMinute = 0,
                restMask = 1 shl 2
            )
            val from = wed.atTime(7, 0)
            linkedMapOf("habit" to habitJson(h), "from" to from.toString()) to
                linkedMapOf("nextFire" to NextReminderCalculator.nextFire(h, from)?.toString())
        }

        add("28-reminder-null-and-digest", "No reminder -> null; digest hour roll") {
            val h = habit(created = today)
            val from = today.atTime(21, 0)
            linkedMapOf("from" to from.toString(), "digestHour" to 20) to linkedMapOf(
                "habitNextFire" to NextReminderCalculator.nextFire(h, from)?.toString(),
                "digestFire" to NextReminderCalculator.nextDigestFire(20, from).toString(),
                "digestFireMorning" to NextReminderCalculator.nextDigestFire(8, from).toString()
            )
        }

        add("29-reminder-same-minute-strictly-after", "Exact reminder time is not after -> next day") {
            val h = habit(created = today, reminderHour = 8, reminderMinute = 30)
            val from = today.atTime(8, 30)
            linkedMapOf("habit" to habitJson(h), "from" to from.toString()) to
                linkedMapOf("nextFire" to NextReminderCalculator.nextFire(h, from)?.toString())
        }

        add("30-sort-manual-desc-asc-ties", "Sort modes and tie-break by orderIndex") {
            fun card(id: Long, order: Int, momentum: Int) = HabitCardUi(
                habit = Habit(id = id, title = "H$id", orderIndex = order, createdAt = millis(today)),
                categoryName = "General", recentDays = emptyList(), momentumPercent = momentum
            )
            val cards = listOf(card(1, 0, 20), card(2, 1, 85), card(3, 2, 85), card(4, 3, 50))
            linkedMapOf("momentums" to listOf(20, 85, 85, 50)) to linkedMapOf(
                "manual" to sortHabitCards(cards, HabitSortMode.MANUAL).map { it.habit.id },
                "desc" to sortHabitCards(cards, HabitSortMode.MOMENTUM_DESCENDING).map { it.habit.id },
                "asc" to sortHabitCards(cards, HabitSortMode.MOMENTUM_ASCENDING).map { it.habit.id },
                "nextFromManual" to HabitSortMode.MANUAL.nextMomentum().name,
                "legacyStored" to HabitSortMode.fromStored("MOMENTUM").name
            )
        }

        add("31-nudges-pending-and-complete", "Hero slides before/after logging today") {
            val h = habit(id = 1, title = "Meditate", created = today.minusDays(5))
            val empty = emptyList<LogEntry>()
            val done = listOf(targetLog(1, today))
            val mom = mapOf(1L to 40)
            linkedMapOf(
                "clockToday" to today.toString(),
                "habits" to listOf(habitJson(h))
            ) to linkedMapOf(
                "pending" to nudgesExpected(listOf(h), empty, emptyList(), today, mom),
                "complete" to nudgesExpected(listOf(h), done, emptyList(), today, mom)
            )
        }

        add("32-nudges-rest-stack-friction", "Rest tomorrow, stack pending, friction insight") {
            val tomorrowIso = today.plusDays(1).dayOfWeek.value
            val stacked = habit(
                id = 2, title = "Journal", created = today.minusDays(3),
                routineStackId = 9, restMask = 1 shl (tomorrowIso - 1)
            )
            val other = habit(id = 1, title = "Walk", created = today.minusDays(3))
            val stacks = listOf(RoutineStackEntity(id = 9, name = "Morning Routine"))
            val logs = listOf(frictionLog(1, today.minusDays(2), "Time Crunch"))
            val mom = mapOf(1L to 10, 2L to 55)
            linkedMapOf(
                "clockToday" to today.toString(),
                "habits" to listOf(habitJson(other), habitJson(stacked)),
                "stacks" to listOf(linkedMapOf("id" to 9, "name" to "Morning Routine")),
                "logs" to logs.map { logJson(it) }
            ) to linkedMapOf(
                "nudges" to nudgesExpected(listOf(other, stacked), logs, stacks, today, mom)
            )
        }

        add("33-nudges-empty-habits", "No habits -> empty nudges") {
            linkedMapOf("clockToday" to today.toString(), "habits" to emptyList<Any>()) to
                linkedMapOf("nudges" to nudgesExpected(emptyList(), emptyList(), emptyList(), today, emptyMap()))
        }

        add("34-timezone-createdAt-same-local-day", "createdAt millis at local start-of-day matches clock day") {
            val h = habit(created = today)
            val logs = listOf(targetLog(1, today))
            val lateMillis = today.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val lateHabit = h.copy(createdAt = lateMillis)
            linkedMapOf(
                "clockToday" to today.toString(),
                "zone" to ZoneId.systemDefault().id,
                "habits" to listOf(habitJson(h), habitJson(lateHabit))
            ) to linkedMapOf(
                "midnightCreate" to analyticsExpected(h, logs, today),
                "lateCreateSameDay" to analyticsExpected(lateHabit, logs, today)
            )
        }

        add("35-mixed-modality-analytics", "YES_NO / NUMERIC / TALLY / TIMER logs all count as done") {
            val created = today.minusDays(3)
            val habits = listOf(
                habit(id = 1, title = "Check", modality = HabitModality.YES_NO, target = 1.0, floor = null, created = created),
                habit(id = 2, title = "Water", modality = HabitModality.NUMERIC, target = 2000.0, floor = 500.0, unit = "ml", created = created),
                habit(id = 3, title = "Pushups", modality = HabitModality.TALLY, target = 20.0, floor = 5.0, unit = "times", created = created),
                habit(id = 4, title = "Focus", modality = HabitModality.TIMER, target = 25.0, floor = 10.0, unit = "mins", created = created)
            )
            val results = linkedMapOf<String, Any?>()
            habits.forEach { h ->
                val log = when (h.modality) {
                    HabitModality.YES_NO -> LogEntry(habitId = h.id, dateString = today.toString(), isCompleted = true, numericValue = 1.0)
                    HabitModality.NUMERIC -> LogEntry(habitId = h.id, dateString = today.toString(), isCompleted = true, numericValue = 2000.0)
                    HabitModality.TALLY -> LogEntry(habitId = h.id, dateString = today.toString(), isCompleted = true, tallyCount = 20, numericValue = 20.0)
                    HabitModality.TIMER -> LogEntry(habitId = h.id, dateString = today.toString(), isCompleted = true, durationSeconds = 25 * 60, numericValue = 25.0)
                }
                results[h.modality.name] = analyticsExpected(h, listOf(log), today)
            }
            linkedMapOf(
                "clockToday" to today.toString(),
                "habits" to habits.map { habitJson(it) }
            ) to linkedMapOf("byModality" to results)
        }

        return list
    }

    private fun jsonEscape(s: String): String = buildString {
        s.forEach { c ->
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
            }
        }
    }

    private fun toJson(value: Any?, indent: Int = 0): String {
        val pad = "  ".repeat(indent)
        val padIn = "  ".repeat(indent + 1)
        return when (value) {
            null -> "null"
            is Boolean -> value.toString()
            is Number -> {
                val d = value.toDouble()
                if (d.isNaN() || d.isInfinite()) "null"
                else if (value is Float || value is Double) {
                    if (d == d.toLong().toDouble() && d in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble())
                        d.toLong().toString()
                    else d.toString()
                } else value.toString()
            }
            is String -> "\"${jsonEscape(value)}\""
            is Map<*, *> -> {
                if (value.isEmpty()) "{}"
                else value.entries.joinToString(",\n", "{\n", "\n$pad}") { (k, v) ->
                    "$padIn\"${jsonEscape(k.toString())}\": ${toJson(v, indent + 1)}"
                }
            }
            is List<*> -> {
                if (value.isEmpty()) "[]"
                else value.joinToString(",\n", "[\n", "\n$pad]") { item ->
                    "$padIn${toJson(item, indent + 1)}"
                }
            }
            else -> "\"${jsonEscape(value.toString())}\""
        }
    }

    @Test
    fun `dumps at least thirty golden fixtures to golden dir`() {
        val dir = goldenDir()
        val all = scenarios()
        assertTrue("need at least 30 scenarios, got ${all.size}", all.size >= 30)

        dir.listFiles()?.filter { it.extension == "json" }?.forEach { it.delete() }

        all.forEach { scenario ->
            val (input, expected) = scenario.build()
            val doc = linkedMapOf(
                "id" to scenario.id,
                "description" to scenario.description,
                "input" to input,
                "expected" to expected
            )
            val file = File(dir, "${scenario.id}.json")
            file.writeText(toJson(doc) + "\n")
            assertTrue(file.exists() && file.length() > 20)
        }

        val written = dir.listFiles()?.count { it.extension == "json" } ?: 0
        assertTrue("expected at least 30 golden files in ${dir.absolutePath}, got $written", written >= 30)
        assertEquals(all.size, written)
    }
}
