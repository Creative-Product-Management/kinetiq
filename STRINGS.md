# Kinetic Routines — User-Visible Strings

**Source:** Kotlin code under `app/src/main/java/com/example/` (plus `res/values/strings.xml`).  
**Rule:** Strings are **verbatim**. Dynamic interpolations are shown with `{…}` or `$name` as in source.  
**Do not paraphrase.** Accessibility `contentDescription`s that users (or TalkBack) hear are included.

Companion docs: [SPEC.md](SPEC.md), [THEME_TOKENS.md](THEME_TOKENS.md).

---

## Global / system

| String | File | Condition |
|--------|------|-----------|
| `Routines` | `res/values/strings.xml` (`app_name`) | Launcher label / activity label |
| `Dark` / `Light` / `System Default` | `ui/theme/Theme.kt` `ThemeMode.*.displayName` | Enum only; Settings buttons use `Dark` / `Light` / `System` (not `System Default`) |
| `Yes / No` / `Numeric Goal` / `Duration Timer` / `Daily Tally` | `data/model/HabitModality.kt` `displayName` | Enum only; editor UI uses `modalityDisplayLabel` instead |
| `units` / `mins` / `times` | same | `NUMERIC` / `TIMER` / `TALLY` `defaultUnit` |
| `Check-in` | `domain/HabitEditorModality.kt` | `modalityDisplayLabel(YES_NO)` — shown on locked type chip |
| `Measurable · Amount` | same | `modalityDisplayLabel(NUMERIC)` |
| `Measurable · Count` | same | `modalityDisplayLabel(TALLY)` |
| `Measurable · Timer` | same | `modalityDisplayLabel(TIMER)` |

### Validation (`domain/validation/HabitValidator.kt`)

| String | Condition |
|--------|-----------|
| `Title is required` | Title blank |
| `Target must be greater than 0` | Target null / ≤0 / NaN / Infinite |
| `Floor cannot be negative` | Floor < 0 or NaN/Infinite |
| `Floor cannot exceed target` | Floor > target |

### Seeded library (`data/repository/LibraryDefaults.kt`)

**Categories:** `Health`, `Fitness`, `Mindfulness`, `Focus`, `Learning`, `Creative`, `Social`, `Personal`

**Stacks:**

| Name | Description |
|------|-------------|
| `Morning Routine` | `Start the day with intention` |
| `Evening Wind-down` | `Close the day calmly` |
| `Deep Work` | `Protected focus blocks` |

### Sample habit (`data/repository/HabitRepository.kt`)

| Field | String |
|-------|--------|
| Title | `Mindful Breathwork` |
| Description | `Box breathing to reset and refocus` |
| Cue | `After I pour my morning glass of water` |
| Unit | `mins` |

---

## Home / Dashboard

**File:** `ui/screens/KineticDashboardScreen.kt`

### Titles / chrome

| String | Condition |
|--------|-----------|
| `Routines` | TopAppBar title; also logo contentDescription |
| `View and sort options` | View-menu IconButton contentDescription |
| `Flat List` | View menu when currently in stack view |
| `Group by Stack` | View menu when currently flat |
| `Card Layout` | View menu when currently table |
| `Table Layout` | View menu when currently card |
| `Sort` | View menu → sort screen |
| `Notifications` | Bell IconButton contentDescription |
| `Menu` | Hamburger contentDescription |
| `Settings` | Main menu item |
| `FAQs` | Main menu item |
| `About` | Main menu item |
| `Add Habit` | FAB contentDescription |
| `All` | Category filter chip (always when list shown) |
| `Independent Habits` | Stack header when habits have no stack (rendered uppercase) |

### Empty states

| String | Condition |
|--------|-----------|
| `No habits yet` | Zero habits |
| `Tap + to create your first habit` | Same |
| `No habits found in this category` | Habits exist; filter empty |
| `Tap + to create a habit or switch filter` | Same |

### Snackbar

| String | Condition |
|--------|-----------|
| `Habit archived` | After archive |
| `Undo` | Snackbar action |

### Delete dialog

| String | Condition |
|--------|-----------|
| `Delete permanently?` | Title |
| `This deletes {habit.title} and all of its logs. This cannot be undone. Consider archiving instead.` | Body |
| `Delete` | Confirm |
| `Cancel` | Dismiss |

### Habit card / table (`ui/components/HabitCard.kt`, `HabitTable.kt`)

| String | Condition |
|--------|-----------|
| `Momentum {n} percent` | Badge contentDescription |
| `{n}%` | Badge text |
| `Habit options` | Overflow menu contentDescription |
| `Edit` | Menu |
| `Archive` | Menu |
| `Delete` | Menu |
| `{title} {date}: completed` | Day cell a11y when completed |
| `{title} {date}: floor sustained` | Floor |
| `{title} {date}: rest day` | Rest |
| `{title} {date}: not logged` | Else |
| `Completed` | Check icon contentDescription |
| `Rest Shield` | Shield icon contentDescription |
| `General` | Fallback category name (`KineticViewModel` habitCards) |

### Create type sheet (`ui/components/CreateHabitTypeSheet.kt`)

| String | Condition |
|--------|-----------|
| `New habit` | Step CHOOSE_KIND title |
| `How do you want to track it? You cannot change this later.` | Subtitle |
| `Check-in` | Kind chip |
| `Simple yes / no` | Check-in subtitle |
| `Measurable` | Kind chip |
| `Amount, count, or time` | Measurable subtitle |
| `Continue` | Continue button |
| `Check-in question` | CHECK_IN_QUESTION title |
| `Asked in check-in reminders. Your habit title is set on the next screen.` | Subtitle |
| `Question` | Field label |
| `e.g., Did you meditate today?` | Placeholder |
| `Back` | Back button |
| `How I log` | MEASURABLE_STYLE title |
| `Pick one logging style. You cannot change this later.` | Subtitle |
| `Amount` | Style chip |
| `ml, pages…` | Amount subtitle |
| `Count` | Style chip |
| `taps / reps` | Count subtitle |
| `Timer` | Style chip |
| `minutes` | Timer subtitle |
| `Track an amount each day, e.g. 2500 ml of water.` | Hint when Amount selected |
| `Count repeats. Each tap adds 1 until the target is met.` | Hint when Count selected |
| `Track time in minutes with the built-in timer.` | Hint when Timer selected |

### Quick log (`ui/components/QuickLogDialog.kt`)

| String | Condition |
|--------|-----------|
| `{habit.title}` | Dialog title |
| `Logging for {dateString}` | Subtitle |
| `Enter logged {unit\|amount}:` | Custom amount mode |
| `Save Amount` | Custom save |
| `What was the main obstacle today?` | Friction mode |
| `Low Energy` | Friction tag |
| `Time Crunch` | Friction tag |
| `Travel / Away` | Friction tag |
| `Distraction` | Friction tag |
| `Optional reflection note...` | Notes placeholder |
| `Log Obstacle & Learn` | Friction save |
| `Log Target` | Option |
| `Full target: {target} {unit}` | Log Target subtitle |
| `Sustain Floor (Minimum)` | Floor option |
| `Never zero out: {floor} {unit}` | Floor subtitle |
| `Launch Active Timer` | When TIMER |
| `Ambient focus countdown & stopwatch` | Timer subtitle |
| `Custom Amount...` | When NUMERIC or TALLY |
| `Log exact numbers or partial reps` | Custom subtitle |
| `Log Blocker / Friction` | Always |
| `Identify why it was skipped for analytics` | Friction subtitle |
| `Clear Entry` | When record exists |
| `Remove record for this date` | Clear subtitle |
| `Close` | Dismiss |

### Active timer (`ui/components/ActiveTimerDialog.kt`)

| String | Condition |
|--------|-----------|
| `{habit.title}` | Title |
| `Target: {n} mins` | Subtitle |
| `FOCUSING` | Running |
| `GOAL MET!` | secondsLeft == 0 && elapsed > 0 |
| `PAUSED` | Not running otherwise |
| `Reset Timer` | contentDescription |
| `Pause` / `Start` | Play/pause contentDescription |
| `+1m` | Add minute |
| `Complete & Log {elapsedSeconds}s` | Finish when elapsed > 0 |
| `Start the timer to log` | Finish when elapsed == 0 |
| `Dismiss` | Dismiss |

### Notification settings dialog (`ui/components/NotificationSettingsDialog.kt`)

| String | Condition |
|--------|-----------|
| `Notifications` | Title |
| `Open Settings` | When OS notifications disabled |
| `Send Test` | When enabled |
| `Test sent.` | After Send Test (in-dialog) |
| `If you don't receive notification, please turn on app notifications on your device.` | Hint |
| `Close` | Confirm |

### Activity chart (`ui/components/HabitActivityChart.kt`)

| String | Condition |
|--------|-----------|
| `Activity chart, last days` | contentDescription, Activity mode |
| `Momentum chart, overall {n} percent` | contentDescription, Momentum mode |
| `LAST {visibleCount} DAYS` | Header |
| `{n}%` | Momentum headline |
| `Hide activity chart` | Dismiss contentDescription |
| `Activity` | Mode chip |
| `Momentum` | Mode chip |
| `ACTIVITY` | Vertical axis (Activity mode) |
| `100` / `50` / `0` | Momentum Y ticks |
| `{habitTitle} {date}: logged` | Stack cell a11y |
| `{date}: no log` | Empty cell a11y |

### In-app nudges

**UI chrome** (`ui/components/InAppNudgeBanner.kt`): contentDescription `{title}. {subtitle}`; `Dismiss` close icon.  
**Note:** `actionLabel` is stored but **not rendered** as visible button text in the strip.

**Copy + selection** (`ui/viewmodel/KineticViewModel.kt` `buildInAppNudges`):

**Gate:** empty list if `habits.isEmpty()`.

#### Slide `today-momentum` (always when habits exist)

| Field | String | Condition |
|-------|--------|-----------|
| title | `Today's Momentum` | Always |
| subtitle | `{pendingToday.title} is not logged yet today. A floor log still counts.` | First habit due today not done and not rest |
| subtitle | `Everything due today is logged — today's Momentum is protected. Nice work.` | Else |
| actionLabel | `Log floor` | Pending exists |
| actionLabel | `View habit` | Else |

#### Slide `momentum-progress`

| Field | String | Condition |
|-------|--------|-----------|
| title | `Momentum builds day by day` | Always |
| subtitle | `{strongest.title} is your strongest habit at {strongestPercent}%. A target day adds about 7% — consistency compounds.` | Strongest exists and percent > 0 |
| subtitle | `A target day adds about 7%. Seven target days in a row reach roughly 40%, and a floor day still earns 65% of a target.` | Else |
| actionLabel | `View habit` | Always |

#### Slide `rest-shield`

| Field | String | Condition |
|-------|--------|-----------|
| title | `Rest shield tomorrow` | Some habit has rest tomorrow |
| title | `Rest days protect Momentum` | Else |
| subtitle | `{restTomorrow.title} has a scheduled rest day tomorrow — decay stays frozen.` | Rest tomorrow |
| subtitle | `Schedule rest days in a habit's editor and Momentum freezes on those days — no gain, no loss, streak preserved.` | Else |
| actionLabel | `View habit` | Always |

#### Slide `routine-stacks`

| Field | String | Condition |
|-------|--------|-----------|
| title | `{stackName} is ready` | Pending stacked habit; stackName = stack.name or `Your stack` |
| title | `Stack habits into routines` | Else |
| subtitle | `{stackPending.title} is next in {stackName\|your stack} and not yet logged today.` | Pending |
| subtitle | `Group related habits into a routine stack in the habit editor, so finishing one habit cues the next.` | Else |
| actionLabel | `Open habit` | Pending |
| actionLabel | `View habit` | Else |
| fallback | `Your stack` / `your stack` | Missing stack name |

#### Optional slide `insight-friction` (if any frictionTag on records)

| Field | String | Condition |
|-------|--------|-----------|
| title | `Friction logged {n} time` / `Friction logged {n} times` | frictionCount > 0 (singular/plural) |
| subtitle | `Open a habit's analytics to see what most often blocks you.` | Same |
| actionLabel | `Review habits` | Same |

---

## Habit detail

**File:** `ui/screens/HabitDetailAnalyticsScreen.kt`

| String | Condition |
|--------|-----------|
| `{habit.title}` | App bar |
| `⚡ {cueTrigger}` | App bar when cue non-blank |
| `Back` | Nav contentDescription |
| `Edit Habit` | Edit contentDescription |
| `CURRENT STREAK` | Metric label |
| `days active` | Metric caption |
| `BEST STREAK` | Metric label |
| `days record` | Metric caption |
| `TARGET COMPLETED` | Metric label |
| `times achieved` | Metric caption |
| `FLOOR SUSTAINED` | Metric label |
| `rescued streaks` | Metric caption |
| `FRICTION & OBSTACLE DIAGNOSTICS` | Section |
| `Root causes identified when habits were skipped:` | Intro |
| `{count} times` | Per friction tag |
| `RECENT ACTIVITY LOG` | Section |
| `No recorded logs yet for this habit.` | Empty logs |
| `Target completed` | Log row |
| `Sustained minimum floor` | Log row |
| `Friction: {tag}` | Log row |
| `Logged entry` | Log row fallback |
| `Delete` | Delete record contentDescription |

### Gauge (`ui/components/KineticGauge.kt`)

| String | Condition |
|--------|-----------|
| `{percent}%` | Center |
| `Momentum` | Under percent |
| `{trajectory}` uppercased | Badge |

### Trajectory labels (`domain/analytics/HabitAnalyticsEngine.kt` → gauge)

| String | Condition |
|--------|-----------|
| `Accelerating` | strengthPercent > lastWeek + 3 |
| `Rebuilding` | strengthPercent < lastWeek − 3 |
| `Steady High` | else if strengthPercent ≥ 70 |
| `Forming Routine` | else |

### Heatmap (`ui/components/HeatmapMatrix.kt`)

| String | Condition |
|--------|-----------|
| `LAST 20 WEEKS` | Header |
| `Target` / `Floor` / `Shield` | Legend |
| `M` `T` `W` `T` `F` `S` `S` | Week starts Monday |
| `S` `M` `T` `W` `T` `F` `S` | Week starts Sunday |

**Tooltips** (`HabitAnalyticsEngine`):

| String | Condition |
|--------|-----------|
| `{date}: Completed Target` | FULL_COMPLETED |
| `{date}: Sustained Minimum Floor` | FLOOR_SUSTAINED |
| `{date}: Rest Day Shield` | REST_SHIELD |
| `{date}: Skipped ({frictionTag\|Unspecified})` | MISSED |
| `{date}: No entry` | NONE |

### Weekly chart (`ui/components/WeeklyBarChart.kt`)

| String | Condition |
|--------|-----------|
| `DAY-OF-WEEK PERFORMANCE` | Header |
| `Mon` `Tue` `Wed` `Thu` `Fri` `Sat` `Sun` | Axis |
| `{percent}%` | Above bar when > 0 |

---

## Habit editor

**File:** `ui/screens/HabitEditorSheet.kt`

### Chrome

| String | Condition |
|--------|-----------|
| `New Habit` | Create title |
| `Edit Habit` | Edit title |
| `Back` | Nav contentDescription |
| `Cancel and discard` | Close contentDescription |
| `Save` | Primary button |

### Fields / hints

| String | Condition |
|--------|-----------|
| `Title` | Label |
| `e.g., Morning Breathwork, Meditate` | Title placeholder |
| `Check-in question` | YES_NO only |
| `e.g., Did you meditate today?` | Placeholder |
| `Required for reminders` | Question blank |
| `Shown in check-in reminder notifications` | Question present |
| `Simple daily check-in. Type is locked after create.` | YES_NO hint |
| `Track an amount each day, e.g. 2500 ml of water. Type is locked after create.` | NUMERIC |
| `Track time in minutes with the built-in timer. Type is locked after create.` | TIMER |
| `Count repeats. Each tap adds 1 until the target is met. Type is locked after create.` | TALLY |
| `Target` / `Floor` / `Unit` | Measurable labels |
| `e.g., 2500` / `e.g., 20` / `e.g., 3` / `e.g., 1` | Target placeholders by modality |
| `e.g., {suggestedFloor}` or `e.g., 0.5` | Floor placeholder |
| `e.g., ml` / `e.g., mins` / `e.g., times` | Unit placeholders |
| `Floor = the smallest amount that still counts on a hard day.` | Helper |
| `Enter a valid number` | Floor parse error |

### Details / organize / appearance / schedule

| String | Condition |
|--------|-----------|
| `Details` | Section |
| `Description & cue (optional)` | Summary when both empty |
| `Cue (optional)` | Label |
| `e.g., After I pour my morning coffee...` | Cue placeholder |
| `After I [cue], I will do this habit.` | Cue supporting |
| `Description (optional)` | Label |
| `Why this habit matters...` | Description placeholder |
| `Collapse Details` / `Expand Details` | Chevron a11y |
| `Organize` | Section |
| `{categoryName} · {stackName}` | Organize summary (`None` / `No stack` when unset) |
| `None` | No category chip; also no stack chip |
| `No stack` | Stack name when unset (summary only) |
| `Category` / `Routine stack` | Subheads |
| `Manage categories` / `Manage stacks` | Links |
| `Collapse Organize` / `Expand Organize` | a11y (`Collapse $title` / `Expand $title`) |
| `Appearance` | Section |
| `{iconName}` | Appearance summary (raw icon enum name) |
| `Color` / `Icon` / `Custom…` | |
| `Collapse Appearance` / `Expand Appearance` | a11y |
| `Schedule` | Section |
| `{restSummary} · Reminder {HH:mm\|Off}` | Summary (`restSummary` = selected day labels joined, or `No rest days`) |
| `No rest days` | Rest summary empty |
| `Off` | Reminder off (summary + switch subtitle) |
| `Mon` `Tue` `Wed` `Thu` `Fri` `Sat` `Sun` | Day mask source (chips show first letter) |
| `Rest days (Rest Shield)` | Label |
| `Rest days won't penalize your streak or Momentum.` | Helper |
| `Reminder` | Switch title |
| `{HH:mm} on selected days` | Reminder on (switch subtitle) |
| `Reminder time: {HH:mm}` | Time button |
| `Reminder days` | Label |
| `Collapse Schedule` / `Expand Schedule` | a11y |
| `Collapse Details` / `Expand Details` | Details section a11y |
| `MANAGE` | Edit-only section |
| `Archive (Restorable)` | Button |
| `Delete` | Button |
| `Cancel Edit` | Button |

### Dialogs

| String | Condition |
|--------|-----------|
| `Custom color` | Color dialog title |
| `Hue` / `Saturation` / `Brightness` / `Hex` | Color dialog labels |
| `Use color` / `Cancel` | Color dialog actions |
| `Reminder time` | Time picker title |
| `OK` / `Cancel` | Time picker |
| `Discard changes?` | Discard title |
| `You have unsaved changes to this habit.` | Body |
| `Discard` / `Keep editing` | Actions |
| `Delete permanently?` | Delete title |
| `This deletes {habit.title} and all of its logs. This cannot be undone. Consider archiving instead.` | Body |
| `Delete` / `Cancel` | Actions |

---

## Settings

**File:** `ui/screens/SettingsScreen.kt`

### Section titles (rendered uppercase)

`Appearance`, `Home`, `Library`, `Habits`, `Notifications`, `Data`, `Archived`

### Rows / controls

| String | Condition |
|--------|-----------|
| `Settings` | Top bar |
| `Back` | Nav a11y |
| `Dark` / `Light` / `System` | Theme buttons (+ a11y) |
| `Hero Insights` | Toggle title |
| `Top carousel on Home` | Subtitle |
| `Activity Chart` | Toggle title |
| `Activity stacks and Momentum line on Home` | Subtitle |
| `Categories` | Nav row |
| `Organize habits in the editor` | Subtitle |
| `Routine stacks` | Nav row |
| `Morning, evening, and focus groups` | Subtitle |
| `›` | Nav chevron |
| `Rest Day Shield` | Toggle |
| `Rest days don't break streaks` | Subtitle |
| `Default Floor` | Label |
| `For new habits, on low-energy days` | Subtitle |
| `20%` / `25%` / `33%` / `50%` | Floor chips |
| `Week Starts` | Row |
| `Monday` / `Sunday` | Week start value |
| `Evening Reminder` | Toggle |
| `Daily check-in nudge` | Subtitle |
| `Time` | Digest hour row |
| `{HH}:00` | Hour display |
| `May be delayed without exact alarms.` | Digest on && !exact alarms |
| `Test Notification` | Row |
| `Send` | Action |
| `Data stays on this device.` | Blurb |
| `Backup Passphrase` | Field |
| `At least 8 characters` | Supporting |
| `Export` / `Import` | Buttons |
| `Sample Habit` / `Load` | Action |
| `All Activity` / `Clear` | Danger |
| `Restore` / `Delete` | Archived rows |
| `Kinetic Routines {VERSION_NAME}` | Footer |

### Toasts / status / dialogs

| String | Condition |
|--------|-----------|
| `Test sent.` | Toast after Send when notifications enabled |
| `Backup saved.` | Export success |
| `Export failed: {message}` | Export failure (`it.message`) |
| `Backup restored.` | Import success |
| `Import failed: {message}` | Import failure (`it.message`) |
| `Could not open file` | Export when output stream null |
| `Could not read file` | Import when input stream null |
| `Sample habit loaded.` | Toast after Load |
| `Clear all activity?` | Dialog title |
| `All logs will be deleted. Habits stay.` | Dialog body |
| `Clear` / `Cancel` | Dialog |
| `Activity cleared.` | Toast after clear |
| `kinetic-routines-backup.kqbak` | Export filename (file picker) |

### Backup error messages (`data/backup/BackupManager.kt`)

Surfaced inside `Export failed: …` / `Import failed: …`:

| String | Condition |
|--------|-----------|
| `Passphrase must be at least 8 characters` | Passphrase length &lt; 8 |
| `Invalid backup file` | Corrupt / unreadable payload or decrypt failure |
| `Unsupported backup schema {schemaVersion}` | Schema mismatch on import |
| `Backup contains logs for unknown habits` | Orphan log habitIds |
| `Not a Kinetic Routines backup` | Envelope `format` ≠ `kinetiq-backup` |

---

## Categories manage

**File:** `ui/screens/CategoryManageScreen.kt`

| String | Condition |
|--------|-----------|
| `Categories` | Title |
| `Back` / `Add category` | a11y |
| `No categories yet. Add one to organize habits in the editor.` | Empty |
| `New category` / `Edit category` | Dialog titles |
| `Name` / `Color` | Fields |
| `Save` / `Cancel` | Dialog |
| `Delete {name}?` | Delete title |
| `Habits in this category become uncategorized ({n} habit)` / `…habits).` | usage > 0 |
| `This category will be removed from the library.` | usage == 0 |
| `Delete` / `Cancel` | |
| `Unused` | usage == 0 |
| `Used by {n} habit` / `Used by {n} habits` | usage > 0 |
| `Move up` / `Move down` / `Edit` / `Delete` | Row a11y |

---

## Stacks manage

**File:** `ui/screens/StackManageScreen.kt`

| String | Condition |
|--------|-----------|
| `Routine stacks` | Title |
| `Back` / `Add stack` | a11y |
| `No stacks yet. Add one to group habits into routines.` | Empty |
| `New stack` / `Edit stack` | Dialogs |
| `Name` / `Description (optional)` | Fields |
| `Save` / `Cancel` | |
| `Delete {name}?` | |
| `Habits in this stack become unstacked ({n} habit)` / `…habits).` | usage > 0 |
| `This stack will be removed from the library.` | usage == 0 |
| `Unused` / `Used by {n} habit(s)` | |
| `Edit` / `Delete` | a11y |

---

## Sort

**File:** `ui/screens/HabitSortScreen.kt`

| String | Condition |
|--------|-----------|
| `Sort` | Title |
| `Back` | a11y |
| `Sort by` | Label |
| `Manual order` | Chip |
| `Momentum Score ↓` | Descending / default momentum chip |
| `Momentum Score ↑` | Ascending chip |
| `Long-press and drag to reorder` | Manual helper |
| `Highest Momentum first — auto-sorted. Tap Momentum Score again for lowest first. Order updates as scores change.` | Descending helper |
| `Lowest Momentum first — auto-sorted. Tap Momentum Score again for highest first. Order updates as scores change.` | Ascending helper |
| `No habits to sort` | Empty |
| `#{index}` | Rank in momentum mode |
| `Drag to reorder {title}` | Handle a11y |
| `General` | Missing category |
| `{momentum}%` | Score text |

---

## FAQs

**File:** `ui/screens/FaqScreen.kt`

### Chrome

| String | Condition |
|--------|-----------|
| `FAQs` | Title |
| `Back` | a11y |
| `How Routines works, in plain language.` | Intro |
| `Collapse answer` / `Expand answer` | Chevron a11y |

### Category headers (rendered uppercase)

`Getting started`, `Logging`, `Momentum score`, `Rest Shield & streaks`, `Reminders`, `Privacy & data`

### FAQ_ITEMS (question → answer verbatim)

#### Getting started

**Q:** `How do I create my first habit?`  
**A:** `Tap the **+ button** on the Home screen.\n\n• Choose **Check-in** or **Measurable**\n• For Check-in, enter a short **check-in question** (used in reminders); for Measurable, pick Amount, Count, or Timer\n• Then set a **title** and the rest of the details (target/floor for measurable habits, cue, reminder, and so on)\n\nTap **Save** and the habit appears on your home screen.`

**Q:** `What are the habit types?`  
**A:** `Habits are either **Check-in** or **Measurable**.\n\n• **Check-in** — a simple yes/no habit. It has a display **title** plus a separate **check-in question** used in reminders\n• **Measurable · Amount** — tracks a quantity, such as pages, glasses, or kilometres\n• **Measurable · Count** — counts taps or repetitions, such as sets, sprints, or calls\n• **Measurable · Timer** — tracks minutes using the built-in timer`

**Q:** `Can I change a habit's type later?`  
**A:** `**No.** Check-in vs Measurable (and Amount / Count / Timer) is locked when you create the habit, so past logs and Momentum stay consistent.\n\nTo track differently, **delete** the habit and create a new one with the type you want. Prefer **Archive** if you only want to hide it and keep history.`

**Q:** `What is a cue?`  
**A:** `A cue is the trigger that reminds you to start — for example, "After breakfast" or "When I sit at my desk". Adding a cue makes the habit easier to remember because it attaches the new habit to something you already do. Your cue shows on the right side of each habit card.`

**Q:** `What is a routine stack?`  
**A:** `A stack is a **group of habits you do together in sequence**, such as a Morning Launchpad.\n\nGrouping habits into a stack makes a routine easier to follow than scattered individual habits.\n\nOn Home, use the **view menu beside the bell** to switch between Group by Stack and Flat List.`

#### Logging

**Q:** `How do I log a habit for today?`  
**A:** `Tap **today's circle** on the habit card.\n\n• **Yes/No** — toggles done / not done\n• **Tally** — adds one count with each tap\n• **Timer** — opens the built-in timer\n• **Number** — a tap logs your target; once logged, tapping again opens more options`

**Q:** `How do I log a floor, a custom amount, or a past day?`  
**A:** `**Long-press** any day circle on the habit card.\n\nThe quick-log menu lets you:\n\n• Log the target or the floor\n• Enter a custom amount\n• Record what got in the way (friction)\n• Clear the day\n\nThis also works for the past days shown on the card.`

**Q:** `What is the difference between Target and Floor?`  
**A:** `**Target** is the full goal for the day.\n\n**Floor** is the smallest version that still counts as showing up — for example, Target 30 reps, Floor 10 reps.\n\nOn a hard day, logging the floor keeps your streak and momentum alive. It earns slightly less momentum than a full target day.`

**Q:** `Can I edit or delete a habit?`  
**A:** `Tap the **three-dot menu** on any habit card (or long-press the card):\n\n• **Edit** — change title, target, floor, cue, schedule, and appearance (tracking type stays locked)\n• **Archive** — hides the habit but keeps its history; restore it anytime from Settings\n• **Delete** — permanent, and asks for confirmation first`

**Q:** `How do I change the order of habits on Home?`  
**A:** `Tap the view icon beside the bell on Home and choose **Sort**.\n\n• **Manual order** — long-press and drag habits into your preferred order; it is saved automatically\n• **Momentum Score ↓** — habits are auto-ordered highest Momentum first; tap it again to flip to lowest first (↑), and the order updates as scores change\n\nYou can switch between the two at any time; your manual order is kept when you switch back.`

#### Momentum score

**Q:** `What is the Kinetic Momentum score?`  
**A:** `Momentum is a **0–100% score** on each habit's detail screen. It answers: *"How strong is this habit right now?"*\n\nIt is **not** your all-time completion rate. Recent days count far more than older days, so it rises as you build consistency and falls gently when you miss days.`

**Q:** `How is Momentum calculated?`  
**A:** `Each day gets a value:\n\n• **Target hit** — 1.0\n• **Floor hit** — 0.65\n• **Missed day** — 0\n\nThe score then updates daily:\n\n**Momentum = 0.93 × yesterday's Momentum + 0.07 × today's value**\n\nOnly the **last 90 days** count, starting from the day you created the habit — earlier days never count against you. If you backfill past days on a new habit, those logged days **do** count, just as if you had logged them at the time.`

**Q:** `How fast does Momentum grow?`  
**A:** `Logging your target every day from a new habit:\n\n• **1 day** — ~7%\n• **1 week** — ~40%\n• **2 weeks** — ~64%\n• **1 month** — ~89%\n• **2 months** — ~99%\n\nA floor-only streak tops out around **65%**, because floor days are worth 0.65.`

**Q:** `What happens when I miss a day?`  
**A:** `A miss does **not** reset you to zero.\n\nYour score is multiplied by **0.93** for each missed day, so it roughly halves every 10 missed days.\n\nBecause recent days matter most, about **two solid weeks** of logging rebuilds most of a decayed score.`

**Q:** `Do rest days hurt my Momentum?`  
**A:** `**No.** On a scheduled rest day (Rest Shield), your score is **frozen** — no gain and no loss — as long as the Rest Day Shield is on in Settings.\n\nIf you log on a rest day anyway, it counts normally.`

**Q:** `What do Accelerating, Rebuilding, Steady High, and Forming Routine mean?`  
**A:** `These labels compare today's Momentum with **7 days ago**:\n\n• **Accelerating** — up more than 3 points\n• **Rebuilding** — down more than 3 points\n• **Steady High** — holding at 70% or above\n• **Forming Routine** — everything else, while the habit is still building`

**Q:** `Is Momentum based on science?`  
**A:** `**The maths is standard; the exact recipe is Kinetic's own.**\n\n• It uses an *exponentially weighted moving average*, a standard statistical technique that weights recent behaviour more heavily.\n• Floor credit follows the "shrink the habit" principle; gentle decay avoids all-or-nothing abandonment after a slip.\n• Habit-formation research (Lally et al., 2010) found automaticity took on average about **66 days**, and that missing a single occasion did not materially affect habit formation.\n\nHowever, Kinetic's exact weights (0.93, floor = 0.65, 70% threshold) are tuned for the app.\n\n**Momentum is a helpful feedback mirror — not a clinical or scientific measure of habit strength.**`

**Q:** `What does not affect Momentum?`  
**A:** `• Exceeding your target (150% counts the same as 100%)\n• Partial progress below your floor — it counts the same as a miss, so always log at least the floor\n• Your all-time streak or total completions\n• The time of day you log\n• Cues and friction tags`

#### Rest Shield & streaks

**Q:** `What is Rest Shield?`  
**A:** `Rest Shield lets you **schedule planned rest days** for a habit — for example, weekends off.\n\nOn rest days:\n\n• Your streak is **preserved**\n• Your Momentum is **frozen**\n• The day does **not** count as a miss\n\nSet rest days when creating or editing a habit, and turn the Rest Day Shield on or off in Settings.`

**Q:** `How do streaks work?`  
**A:** `Your **current streak** counts consecutive active days where you hit your target or floor.\n\n• **Rest days** preserve the streak without adding to it — they neither extend nor break it\n• **Missing an active day** ends the current streak\n• Your **best streak** is always kept`

#### Reminders

**Q:** `How do reminders work?`  
**A:** `Set a reminder time when creating or editing a habit.\n\n• Routines sends a **local notification** on scheduled days, skipping rest days\n• For **Check-in** habits, the notification leads with your **check-in question** (the title appears as supporting text)\n• From the notification, you can mark the habit done\n• Reminders are **re-armed after your phone restarts**`

**Q:** `I am not receiving notifications. What should I check?`  
**A:** `Tap the **bell** on Home.\n\n• If app notifications are off, choose **Open Settings** and turn notifications on for Routines\n• Use **Send Test** to check that notifications arrive\n\nExact reminder timing also depends on your device's battery and alarm settings.`

#### Privacy & data

**Q:** `Where is my data stored?`  
**A:** `**Everything stays on this device.**\n\nRoutines has no accounts, no servers, and no sync — and the app does not use the internet.\n\nYour habits, logs, and settings are stored locally on your phone.`

**Q:** `How do I back up or move my data?`  
**A:** `In **Settings**:\n\n• Use **Export** to save an encrypted backup file (.kqbak) protected by a passphrase you choose\n• Use **Import** on any device with Routines and enter the same passphrase to restore\n\nKeep your passphrase safe — without it, a backup cannot be opened.`

**Q:** `I archived a habit by mistake. Can I get it back?`  
**A:** `**Yes — archiving is reversible.**\n\nOpen Settings, find the **Archived** section, and restore the habit. Its history comes back with it.\n\nOnly permanent deletion from the confirmation dialog removes a habit and its logs for good.`

---

## About

**File:** `ui/screens/AboutScreen.kt`

| String | Condition |
|--------|-----------|
| `About` | Title |
| `Back` | a11y |
| `Kinetic Routines` | Brand title + logo a11y |
| `Momentum-based Habit Builder` | Tagline |
| `Version {VERSION_NAME}` | Under tagline |
| `Kinetic Routines is a calm, offline habit tracker. Build habits with a target for your best days and a floor for your hard days, protect them with scheduled rest days, and watch your Momentum grow.` | Body |
| `WHAT YOU CAN DO` | Section |
| `Track four ways: Yes/No, Number, Timer, and Tally habits.` | Bullet |
| `Set a target and a smaller floor, so hard days still count.` | Bullet |
| `Add cues and routine stacks to make habits automatic.` | Bullet |
| `Schedule Rest Shield days that protect streaks and Momentum.` | Bullet |
| `See Momentum, streaks, heatmaps, and friction patterns per habit.` | Bullet |
| `Get local reminders that skip rest days and survive reboots.` | Bullet |
| `MOMENTUM, IN ONE LINE` | Section |
| `Momentum is a 0–100% rolling score of your recent consistency: targets build it fastest, floors build it steadily, misses decay it gently, and rest days freeze it. See FAQs for the full calculation.` | Body |
| `PRIVACY` | Section |
| `Data stays on this device. No accounts, no servers, no sync — and the app does not use the internet. Encrypted backups (.kqbak) are created only when you export them, and only you hold the passphrase.` | Body |
| `Kinetic Routines {VERSION_NAME}` | Footer |
| `Developed by Creative Product Management with love.` | Credit |
| `•` | Bullet glyph |

---

## Notifications (system)

**Files:** `notification/HabitNotificationScheduler.kt`, `notification/HabitNotificationReceiver.kt`

### Channels

| String | Condition |
|--------|-----------|
| `Habit Reminders` | Channel name |
| `Daily scheduled reminders for active habits with direct logging actions` | Channel description |
| `Smart Nudges & Momentum` | Channel name |
| `Habit stacking suggestions and streak momentum updates` | Channel description |

### Habit reminder (`showHabitReminderNotification`)

| String | Condition |
|--------|-----------|
| `{checkInQuestion}` | **Title** when `modality == "YES_NO"` and question non-blank |
| `Habit Reminder: {habitTitle}` | **Title** otherwise |
| `Habit Reminder` | Fallback if `EXTRA_HABIT_TITLE` missing (used as habitTitle) |
| `{habitTitle}` | **Body** when check-in with non-blank question |
| `Cue: {cue}` | **Body** when not (check-in+question) and cue non-blank |
| `Time to preserve your momentum!` | **Body** otherwise |
| `Done (Target)` | Action |
| `Floor ({n} {unit})` | Action when floorVal > 0 and unit non-blank |
| `Floor (Minimum)` | Action when floorVal > 0 and unit blank |
| `Snooze 15m` | Action |

### Action feedback

| String | Condition |
|--------|-----------|
| `Test notification` | Title when test / invalid id |
| `This was a test — no habit was logged.` | Body |
| `Target Achieved!` | Complete success title |
| `Minimum Floor Sustained!` | Floor success title |
| `Could not log` | Failure title |
| `Logged {habitTitle}.` | Success body |
| `Open Routines to log {habitTitle} manually.` | Failure body |

### Evening digest

| String | Condition |
|--------|-----------|
| `Evening Momentum Review` | Title when digest enabled |
| `Take a moment to log today's habits and protect your streaks.` | Body |

### Test extras (scheduler)

| String | Condition |
|--------|-----------|
| `Mindful Breathwork` | Default / ViewModel test title |
| `After your morning coffee` | Test cue |
| `mins` | Test unit |

---

## Friction tags (canonical list)

**File:** `ui/components/QuickLogDialog.kt`

1. `Low Energy`  
2. `Time Crunch`  
3. `Travel / Away`  
4. `Distraction`  

---

*End of STRINGS.md — extracted verbatim from Kotlin sources.*
