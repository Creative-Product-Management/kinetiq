# Kinetic Routines — Interactions & Drawing Specs

Exact gesture, dialog, and chart behavior from Compose sources.  
Companions: [SPEC.md](SPEC.md), [STRINGS.md](STRINGS.md), [THEME_TOKENS.md](THEME_TOKENS.md), [ALGORITHMS.md](ALGORITHMS.md).

**Color tokens** (`ui/theme/Color.kt`): KineticEmerald `#10B981`, KineticEmeraldGlow `#34D399`, KineticViolet `#8B5CF6`, FloorGold `#D97706`, RestShieldBlue `#2563EB`, MissedRed `#DC2626`.

---

## Shared: which dates are loggable

| Surface | Window | Loggable? |
|---------|--------|-----------|
| Habit card / table day strip | Last **9** calendar days ending **today** (`CARD_DAY_STRIP_DAYS`) | **Yes** — tap and long-press on **any** strip cell (past or today). No `isToday`-only gate. |
| Future dates | Not shown | N/A |
| Active timer **Complete** | — | Always writes **`DateCodec.todayString()`**, even if QuickLog was opened for a past day |
| HabitActivityChart | Display last N ≤ 30 days | **No** logging gestures |

Rest days remain tappable; cell shows shield chrome when empty. Logging APIs do not block rest days from these UI paths.

Day strip built by `buildDayStrip` / `KineticViewModel.buildRecentDays` — oldest → newest, ending today.

---

## Dashboard (`KineticDashboardScreen`)

### Top bar

| Control | Gesture | Outcome |
|---------|---------|---------|
| Brand (`Routines` logo + title) | — | Display only |
| View / sort menu | Tap | Dropdown: Flat List ↔ Group by Stack; Card ↔ Table Layout; **Sort** |
| Notifications (bell) | Tap | `NotificationSettingsDialog` |
| Main menu (hamburger) | Tap | **Settings**, **FAQs**, **About** |

### FAB

- Circle FAB, Add icon; contentDescription `Add Habit`
- Tap → `CreateHabitTypeSheet` → on complete `beginCreateHabit` + open editor (`habit == null`)

### Category chips

- Horizontal row: always **All**, then each category name + count badge
- Tap → filter by `categoryId` (or null for All)
- Selected: primary fill; unselected: surface + outline; chip radius 20.dp

### List modes

| Mode | Content |
|------|---------|
| Empty (no habits) | `No habits yet` / `Tap + to create your first habit` |
| Empty (filter) | `No habits found in this category` / … |
| Table layout | Sticky `HabitTableDateHeader` + `HabitTableRow`; optional stack section headers |
| Stack view (cards) | Headers `(stack name \| Independent Habits).uppercase()` + `HabitCard` |
| Flat cards | `HabitCard` list |

**Wiring (card and table):**

| Gesture | Target | Handler |
|---------|--------|---------|
| Tap day cell | Any strip day | `handleDayClick(habit, day)` |
| Long-press day cell | Any strip day | `openQuickLog(habit, day)` |
| Tap card / table label | Habit | Open detail analytics |
| Overflow Edit / Archive / Delete | Menu | Edit sheet / archive (+ snackbar Undo) / delete confirm |
| Long-press **card body** | HabitCard only | Opens same overflow menu (no drag) |

Archive snackbar: `Habit archived` + action `Undo`.  
Delete dialog: `Delete permanently?` / body with habit title / `Delete` / `Cancel`.

### Hero & activity chart

- Nudge strip if Settings **Hero Insights** on and nudges non-empty
- `HabitActivityChart` if **Activity Chart** on and cards present; chart Close → turns setting off

---

## Day-tap routing by modality (`handleDayClick`)

| Modality | Single tap on day cell |
|----------|------------------------|
| **YES_NO** | `toggleHabitForDate` (null→complete; complete→delete; floor→upgrade to complete) |
| **TALLY** | `logTallyIncrement` (+1) |
| **TIMER** | Opens **QuickLog** (does not start timer) |
| **NUMERIC** | If no record, or record is neither completed nor floor → `toggleHabitForDate` (full target); **else** → QuickLog |

**Long-press** (all modalities): always QuickLog for that `dateString`.

---

## QuickLogDialog

**Shell:** AlertDialog; title = habit title; subtitle `Logging for {dateString}`; dismiss `Close`.

### Main options (default mode)

| Option | When shown | Subtitle / notes | Action |
|--------|------------|------------------|--------|
| **Log Target** | Always | `Full target: {n} {unit}` | `setCompletedForDate` then close |
| **Sustain Floor (Minimum)** | Always | `Never zero out: {floor} {unit}` where `floor = floorValue ?: target×0.25` | `logFloorSustainForDate` then close |
| **Launch Active Timer** | **TIMER** only | `Ambient focus countdown & stopwatch` | Opens `ActiveTimerDialog` |
| **Custom Amount...** | **NUMERIC** or **TALLY** | `Log exact numbers or partial reps` | Switches to custom-amount mode |
| **Log Blocker / Friction** | Always | `Identify why it was skipped for analytics` | Friction mode |
| **Clear Entry** | Only if `item.record != null` | `Remove record for this date` | `clearRecord` then close |

### Custom-amount mode

- Label: `Enter logged {unit|amount}:`
- Number `OutlinedTextField`; **Save Amount** → `toDoubleOrNull() ?: 0.0` → `logNumericProgress` (≤0 deletes)

### Friction mode

- Prompt: `What was the main obstacle today?`
- Tags (2×2): `Low Energy`, `Time Crunch`, `Travel / Away`, `Distraction`
- Notes placeholder: `Optional reflection note...`
- **Log Obstacle & Learn** enabled only when a tag is selected → `logFrictionTag`

No other field validation beyond tag required for friction save.

---

## ActiveTimerDialog

### Controls

| Control | Behavior |
|---------|----------|
| Target | `(targetValue×60).toInt().coerceAtLeast(60)` seconds + `bonusSeconds` |
| Display | Countdown `MM:SS` of **remaining**; ring progress = elapsed / totalTarget |
| Status | `FOCUSING` if running; `GOAL MET!` if remaining 0 and elapsed > 0; else `PAUSED` |
| Reset | Pause; `accumulatedMs=0`; `bonusSeconds=0` |
| Play / Pause | Toggle; pause folds wall-clock delta into `accumulatedMs` |
| **+1m** | `bonusSeconds += 60` |
| **Complete & Log {n}s** | Enabled only if `elapsedSeconds > 0`; pause then `finishTimerSession` |
| **Dismiss** / dialog dismiss | Closes **without** logging |

Tick: `LaunchedEffect(isRunning)` updates `nowMs` every **250 ms**.  
Progress: `animateFloatAsState` (default spring/tween — no custom duration).  
Ring: 180.dp, stroke 8.dp.

### Background / process kill

| Situation | Behavior |
|-----------|----------|
| App backgrounded while dialog open & running | 250 ms loop may pause; **elapsed uses wall-clock** (`now − runningSince`), so background time still counts when UI resumes |
| Config change / recomposition | `rememberSaveable` keeps `accumulatedMs`, `runningSinceMs`, `isRunning`, `bonusSeconds` **while dialog stays composed** |
| Dismiss / Close timer | `closeTimer()` clears VM `activeTimerHabit` → dialog leaves composition → **session state discarded** |
| Process death | No SavedStateHandle / AlarmManager / foreground service for the timer. Session is **lost** unless the whole Activity+dialog is restored with the same saveable keys (not guaranteed). Habit reminder alarms are unrelated. |
| Complete | Always `logDurationSeconds(habit, **today**, durationSec)` |

---

## HabitCard

| Gesture | Target | Outcome |
|---------|--------|---------|
| Tap card | Body | Open habit detail |
| Long-press card | Body | Overflow menu (Edit / Archive / Delete) |
| Tap day cell | Strip day | `handleDayClick` |
| Long-press day cell | Strip day | QuickLog |
| Drag | — | **None** |

Chrome: card radius 14.dp; pad H12 V10; icon 28.dp; title 14.sp Bold; momentum badge (≥70 Emerald, ≥40 FloorGold, else Violet).

### DayLogCell (shared with table)

- Size **26.dp**, corner radius **6.dp**
- Fill priority: completed → habitColor; floor → FloorGold@0.2; rest → RestShieldBlue@0.15; today empty → `#222B3D`; else → `#19202D`
- Today border 1.5.dp; else 1.dp
- Glyphs: Check 12.dp (completed); 7.dp FloorGold square (floor); Shield 11.dp (rest); else tally/numeric text 9.sp if partial progress

---

## HabitTable

| Gesture | Target | Outcome |
|---------|--------|---------|
| Tap label row | Title area | Open detail (**no** long-press menu on label) |
| Overflow | ⋮ | Edit / Archive / Delete |
| Tap / long-press day | Same `DayLogCell` | Same as card |
| Drag | — | **None** |

Sticky date header: day letters/numbers; today uses **theme primary** (not per-habit color).

---

## Habit editor (`HabitEditorSheet`)

### Chrome

- Title `New Habit` / `Edit Habit`
- Back / Close → if dirty, discard dialog; else dismiss
- Bottom **Save** (validates via `HabitValidator` + check-in question when YES_NO)
- Modality chip **read-only** (locked after create)

### Field / section order (top → bottom)

1. **Title** — required (`Title is required`)
2. **Check-in question** — YES_NO only; blank → `Required for reminders`
3. Locked modality label + hint
4. **Target / Floor / Unit** — measurable only; floor blank OK (null); parse error `Enter a valid number`; validator floor/target rules
5. Collapsible **Details** — Cue, Description
6. Collapsible **Organize** — Category chips + Manage categories; Stack chips + Manage stacks
7. Collapsible **Appearance** — Color swatches + Custom…; Icon chips
8. Collapsible **Schedule** — Rest day toggles; Reminder switch; time; reminder day mask
9. **MANAGE** (edit only) — Archive (Restorable), Delete
10. **Cancel Edit**

### Dialogs

- Custom color: Hue / Saturation / Brightness / Hex → `Use color` / `Cancel`
- Reminder time: TimePicker → `OK` / `Cancel`
- Discard: `Discard changes?` / `Discard` / `Keep editing`
- Delete: same permanent-delete copy as dashboard

Defaults: reminder hour 8; `reminderDaysMask` 127; icon `SelfImprovement`; color `#10B981`.

---

## Sort screen (`HabitSortScreen`)

| Control | Behavior |
|---------|----------|
| Chip **Manual order** | Enables drag reorder |
| Chip **Momentum Score ↓ / ↑** | Auto-sort; tap Momentum chip cycles DESC ↔ ASC via `nextMomentum()` |
| Long-press + drag | **Only in Manual mode** (`detectDragGesturesAfterLongPress`); row height **64.dp**; on release `saveHabitOrder`; cancel reverts |
| Momentum mode rows | Show rank `#n` and `n%` — **not** draggable |

Empty: `No habits to sort`.

---

## Settings — sections in exact order

Chrome: section title uppercase 11.sp; card radius 16.dp.

### 1. Appearance

| Row | Control |
|-----|---------|
| Theme | Three chip buttons: **Dark** / **Light** / **System** |

### 2. Home

| Row | Control |
|-----|---------|
| **Hero Insights** — `Top carousel on Home` | Toggle |
| **Activity Chart** — `Activity stacks and Momentum line on Home` | Toggle |

### 3. Library

| Row | Control |
|-----|---------|
| **Categories** — `Organize habits in the editor` | Nav (›) |
| **Routine stacks** — `Morning, evening, and focus groups` | Nav (›) |

### 4. Habits

| Row | Control |
|-----|---------|
| **Rest Day Shield** — `Rest days don't break streaks` | Toggle |
| **Default Floor** — `For new habits, on low-energy days` | Label + chips **20% / 25% / 33% / 50%** (selected FloorGold) |
| **Week Starts** | Value button **Monday** ↔ **Sunday** |

### 5. Notifications

| Row | Control |
|-----|---------|
| **Evening Reminder** — `Daily check-in nudge` | Toggle |
| **Time** `HH:00` | Shown if digest on; tap advances hour mod 24 |
| `May be delayed without exact alarms.` | Hint if digest on && !exact alarms |
| **Test Notification** | Action **Send** (or opens OS settings if notifications disabled) |

### 6. Data

| Row | Control |
|-----|---------|
| `Data stays on this device.` | Static text |
| **Backup Passphrase** — `At least 8 characters` | Password field |
| **Export** / **Import** | Buttons (enabled when passphrase length ≥ 8) |
| **Sample Habit** | Action **Load** |
| **All Activity** | Danger action **Clear** → confirm dialog |

### 7. Archived (only if any)

| Row | Control |
|-----|---------|
| Habit title | **Restore** / **Delete** (MissedRed) |

### Footer

`Kinetic Routines {VERSION_NAME}`

---

## Drawing specs

### KineticGauge (`KineticGauge.kt`)

| Property | Value |
|----------|-------|
| Outer size | **190.dp** |
| Canvas padding | **14.dp** |
| Stroke width | **14.dp** |
| Track | Theme outline; startAngle **135°**; sweep **270°**; Round caps |
| Progress arc | Sweep gradient `[habitColor\|Emerald, KineticViolet, KineticEmeraldGlow]`; sweep = percent/100 × 270° |
| Center | `{n}%` 34.sp Bold; `Momentum` 10.sp; trajectory uppercase 11.sp (≥70 Emerald / ≥40 FloorGold / else Violet) |
| Animation | `Animatable` → `tween(900)` |

### HeatmapMatrix (`HeatmapMatrix.kt`)

| Property | Value |
|----------|-------|
| Card | Radius 16.dp; pad 16.dp |
| Title | `LAST 20 WEEKS` |
| Legend | Target (habitColor), Floor (FloorGold), Shield (RestShieldBlue); swatch **8.dp** |
| Day labels | Column of **14.dp** boxes; M–S; gap **4.dp** |
| Cells | **18.dp**; radius **3.dp**; week gap 4.dp |
| Colors | `FULL_COMPLETED` → habitColor; `FLOOR_SUSTAINED` → FloorGold; `REST_SHIELD` → RestShieldBlue; `MISSED` → MissedRed@0.5; `NONE` → `#1E2633` |
| Interaction | Tap cell → tooltip banner `#222B3D` radius 8.dp |
| Scroll | Auto-scroll to end on first load |
| Animation | None beyond scroll |

### WeeklyBarChart (`WeeklyBarChart.kt`)

| Property | Value |
|----------|-------|
| Title | `DAY-OF-WEEK PERFORMANCE` |
| Chart row height | **110.dp** |
| Bar track | **16×70.dp**, radius 4.dp, fill `#1E2633` |
| Fill height | `max(rate, 0.04)` of track; habitColor if rate > 0 else Transparent |
| Labels | Mon–Sun 10.sp; percent 9.sp above bar if > 0 |
| Animation | **None** |

### HabitActivityChart (`HabitActivityChart.kt`)

| Constant | Value |
|----------|-------|
| `ACTIVITY_CELL_SIZE` | **20.dp** |
| `ACTIVITY_CELL_SPACING` | **4.dp** |
| `ACTIVITY_MIN_STACK_ROWS` | **5** |
| `ACTIVITY_LABEL_MIN_HEIGHT` | **96.dp** |
| `ACTIVITY_MAX_DAYS` | **30** |
| Column fit | ~24.dp per column after ~20.dp label reserve |

**Shell:** Radius 16.dp; header `LAST {visibleCount} DAYS`; session toggle **Activity** | **Momentum**; Close dismisses via Settings flag. **Display-only** (no log gestures).

**Activity mode**

- Vertical `ACTIVITY` label
- Squares: habit color, radius **4.dp**; empty placeholder `#1E2633`
- Stack: earliest log at **bottom**; only completed or floor-sustained fill a square
- Today column shade: primary@0.16, radius 6.dp

**Momentum mode**

- Y ticks `100` / `50` / `0`; axis width 22.dp
- Guide lines `#1E2633`; faded bars accent@0.22; stroke **2.5.dp**; dots **3.dp**
- Null history = gap in line; headline % uses Emerald / FloorGold / Violet thresholds (≥70 / ≥40)

---

*End of INTERACTIONS.md*
