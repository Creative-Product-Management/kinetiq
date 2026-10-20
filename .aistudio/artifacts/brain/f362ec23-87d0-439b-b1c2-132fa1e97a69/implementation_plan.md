# OrbitHabit: Behavioral Habit Engine & Analytical Companion

OrbitHabit is an innovative, front-end only Android habit application built on behavioral science principles (BJ Fogg's *Tiny Habits* and James Clear's *Atomic Habits*) combined with Google product design elegance. While inspired by the clean, offline speed and open simplicity of Loop Habit Tracker (uhabits), OrbitHabit fundamentally differentiates itself with Elastic Habits (Floor vs Target), Habit Stacking Chains, Resilient Momentum with Rest Shields, 1-tap Friction Insights, and a robust dual **Push & In-App Notification System**.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Key Out-of-the-Box Behavioral & Notification Systems:**
> 1. **Dual Notification Architecture (Local Push & In-App Nudges)**:
>    - **System Push Notifications**: Local scheduled notifications using Android `AlarmManager` and `NotificationManager` with action buttons ("Complete Target", "Log Floor / Minimum", and "Snooze 15m") directly in the notification shade, respecting Android 13+ `POST_NOTIFICATIONS` runtime permissions.
>    - **In-App Smart Nudges**: Contextual banners and prompt cards on the dashboard (e.g., stack completion nudges, rest shield confirmations, and contextual time-of-day reminders like "Start your 3-min timer now").
> 2. **Elastic Habits (Floor vs. Target / "Never Zero")**: Adaptable "Minimum Viable Goal" (e.g., 2 minutes or 1 page) alongside "Target Goal" (20 minutes / 15 pages). On overwhelmed days, hitting the Floor preserves behavioral identity and momentum without guilt or broken streaks.
> 3. **Habit Stacking Chains (Cue-Routine Flows)**: Group atomic habits into contiguous stacks (e.g. *Morning Launchpad*: Hydrate ➔ 2-min Breathwork ➔ Read 1 Page). Completing one step triggers tactile momentum into the next.
> 4. **Resilient Momentum Engine (Anti-Streak Anxiety)**: Replaces brittle streak counters that reset to zero upon a single bad day with a rolling **Habit Orbit & Strength Metric** and built-in **Rest Shields** (scheduled rest days & sickness freeze).
> 5. **1-Tap Friction & Context Insights**: When a habit isn't completed or completed with difficulty, a lightning-fast 1-tap tag ("Low energy", "Time crunch", "Wrong environment") uncovers *why* you struggle.
> 6. **Multi-Modal Tracking & 100% Offline Room Persistence**: Boolean checks, Numeric Targets with custom units, Countdown & Stopwatch Timers, and Daily Tallies stored entirely on-device with zero accounts.

---

## 1. Overview & Core Concept

- **What It Does**: OrbitHabit acts as an adaptive habit momentum engine. Users track multiple habit types, link related habits into sequential stacks, receive proactive yet non-intrusive local notifications with actionable buttons, and analyze their habit health without relying on cloud services.
- **Target Audience / Persona**: High-agency individuals, students, professionals, and mindful builders who want a fast, privacy-respecting tracker with modern behavioral psychology and intelligent reminders.
- **Key Value**: Delivers the high-velocity, distraction-free privacy of Loop Habit Tracker, while eliminating the streak-loss despair through Elastic Goals and providing timely notification cues to maintain daily execution.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **The Orbit Grid (Home Dashboard)**:
   - **Horizontal Fast-Check Strip**: 7-day quick-action grid with fluid completion toggles.
   - **In-App Smart Nudge Banner**: Top contextual pill highlighting active routine stacks, upcoming reminder alerts, or congratulating momentum milestones.
   - **Tactile Gesture Interaction**:
     - *Single Tap*: Log target completion (or advance tally by 1).
     - *Short Hold / Swipe*: Choose "Floor / Minimum Viable Habit" or enter custom numerical/timer value.
     - *Card Tap*: Expands inline quick-notes and 1-tap friction reflection if skipped.
   - **Stack Mode Toggle**: Switch between viewing by Categories and viewing as sequential **Routine Stacks**.

2. **Notifications & Reminders System**:
   - **Habit Reminder Configuration**: Each habit can have custom reminder times (e.g., 07:30 AM, 01:00 PM) and active days.
   - **Actionable Push Notifications**: When a notification arrives, the user can act immediately from their lock screen or status bar:
     - Button 1: **"Done (Target)"** -> Instantly marks habit complete without opening app.
     - Button 2: **"Sustain (Floor)"** -> Logs the minimum viable habit.
     - Button 3: **"Snooze 15m"** -> Re-schedules the alarm for 15 minutes later.
   - **Notification Permission & Testing**: Friendly in-app card for requesting Android 13+ `POST_NOTIFICATIONS` permission with a "Send Test Reminder" button to confirm immediate device support.

3. **Habit & Routine Creation Flow**:
   - Modality selection (Boolean, Numeric, Timer, Tally).
   - Elastic goal definitions (Target vs Floor).
   - Scheduling & Rest Shield configuration.
   - Notification reminder time selection.
   - Habit stacking cue ("After [Trigger Habit], I will [New Habit]").

4. **Behavioral Analytics & Habit Deep-Dive**:
   - Orbit Health & Strength gauge (0-100%).
   - Streaks & Elastic ratio breakdown (Target vs Floor vs Rest Shield).
   - Annual multi-shade heatmap contribution calendar.
   - Friction diagnostics & completion by day-of-week.

5. **Focus Timer & Quick Actions**:
   - Ambient timer with audio/haptic feedback for duration habits.

### Visual Identity & Theme
- **Aesthetic Direction**: *Google Material Expressive Dark & Vibrant*. Deep void surface (`#0D1117`) with elevated tonal containers (`#161B22`, `#21262D`).
- **Accent Neon Pastels**:
  - Cyan Emerald (`#10B981`) for Physical Vitality
  - Electric Violet (`#8B5CF6`) for Deep Focus & Learning
  - Solar Amber (`#F59E0B`) for Mindfulness & Well-being
  - Neon Coral (`#F43F5E`) for Creative & Personal Pursuits
- **Micro-Interactions**:
  - Haptic response on completion.
  - Spring-scale card animations.
  - Notification pill transitions.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Actionable Local Push Notifications (AlarmManager + BroadcastReceiver)**
  - *Chosen Approach*: Standard Android `AlarmManager` scheduling with a dedicated `HabitNotificationReceiver` that builds rich `NotificationCompat.Builder` notifications with `PendingIntent` action buttons for direct target and floor logging.
  - *Why*: Allows zero-friction logging without needing to switch out of what the user is currently doing on their phone.
  - *Alternatives Considered*: Cloud push notifications (Firebase Cloud Messaging) require network and backend servers; local alarms are 100% offline, instant, and private.
- **Decision 2: In-App Smart Nudges**
  - *Chosen Approach*: Reactive banner at the top of the dashboard derived from time-of-day, incomplete stacked habits, and active rest shields.
  - *Why*: Complements push notifications with immediate in-context awareness while browsing the app.
- **Decision 3: Elastic Goals (Floor vs Target)**
  - *Chosen Approach*: Optional `floorValue` per habit allowing users to sustain streaks on low-energy days.
  - *Why*: Prevents abandonment due to rigid binary expectations.
- **Decision 4: On-Device Room SQLite Persistence**
  - *Chosen Approach*: Room database with DAO reactive `Flow`s.
  - *Why*: Sub-millisecond queries, complete privacy, zero dependencies on external services.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
├────────────────────────────────────────────────────────────────────────┤
│  TopAppBar (In-App Nudge Banner, Category Chips, Stack Mode Toggle)   │
│  ├─ OrbitDashboardScreen (7-day quick-action grid, elastic chips)      │
│  ├─ NotificationSettingsDialog (Reminder times, Test push, Permission) │
│  ├─ HabitStackFlowScreen (Step-by-step guided routine launcher)        │
│  ├─ HabitDetailAnalyticsScreen (Gauge, Heatmap, Elastic bars, Charts)  │
│  ├─ HabitEditorSheet (Elastic targets, Modality picker, Reminder time) │
│  └─ ActiveTimerSheet (Ambient countdown & stopwatch for timer habits)  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Observes StateFlow / Dispatches Intents
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   OrbitHabitViewModel & AnalyticsEngine                │
├────────────────────────────────────────────────────────────────────────┤
│  - Active filter & Routine Stack state                                 │
│  - Elastic log handler (Full target vs Floor sustain)                  │
│  - In-App Nudge evaluator (contextual time & stack cues)               │
│  - Real-time Momentum scoring with Rest Shield support                 │
│  - Reminder scheduler caller via HabitNotificationManager              │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │ Calls                           │ Repository Pattern
                   ▼                                 ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────┐
│       Notification Subsystem         │  │     OrbitHabitRepository     │
├──────────────────────────────────────┤  ├──────────────────────────────┤
│ HabitNotificationScheduler           │  │ HabitDao (Habits, Records,   │
│ ├─ AlarmManager (Exact / Inexact)    │  │  Stacks, Friction Logs)      │
│ ├─ HabitNotificationReceiver (Broadcast)                               │
│ │   ├─ Action: COMPLETE_TARGET       │  └──────────────┬───────────────┘
│ │   ├─ Action: SUSTAIN_FLOOR         │                 │ Reactive Flow
│ │   └─ Action: SNOOZE_15M            │                 ▼
│ └─ NotificationManagerCompat         │  ┌──────────────────────────────┐
│     (Channels: Reminders, Nudges)    │  │ OrbitHabitDatabase (Room)    │
└──────────────────────────────────────┘  └──────────────────────────────┘
```

### Data Model & Persistence
- `HabitEntity`:
  - `id: Long` (PK)
  - `title: String`, `cueTrigger: String`, `description: String`
  - `modality: HabitModality` (YES_NO, NUMERIC, TIMER, TALLY)
  - `targetValue: Double`, `floorValue: Double?`, `unit: String`
  - `category: String`, `colorHex: String`, `iconName: String`
  - `reminderHour: Int?`, `reminderMinute: Int?`, `reminderDaysMask: Int`
  - `isRestDayScheduled(dayOfWeek): Boolean`
  - `routineStackId: Long?`, `createdAt: Long`
- `HabitRecordEntity`:
  - `id: Long` (PK), `habitId: Long` (FK), `date: String` (YYYY-MM-DD)
  - `isCompleted: Boolean`, `isFloorSustained: Boolean`
  - `recordedValue: Double`, `durationSeconds: Int`
  - `frictionTag: String?`, `notes: String?`
- `RoutineStackEntity`:
  - `id: Long` (PK), `title: String`, `cueIcon: String`

### Android Permissions & Notification Setup
- Declare `android.permission.POST_NOTIFICATIONS` in `AndroidManifest.xml`.
- Runtime permission request via Compose `rememberLauncherForActivityResult(RequestPermission())`.
- Notification channel initialization (`orbit_habit_reminders`) with high importance and vibration.
- Receiver declared in `AndroidManifest.xml` for handling notification actions and background database logging.

### Verification Plan
- Build and compile verification via `compile_applet`.
- Verify Room database initialization, DAO operations, and sample seed habit pre-population.
- Test push notification trigger, in-app nudge banner reactivity, and direct notification action logging.
