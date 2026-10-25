# Notification Port — Expo (`expo-notifications`)

Maps each Kinetic Routines Android notification behavior to what **Expo** can and cannot do on **iOS** and **Android**, including **app killed**, then proposes the closest design. Risks are marked **[RISK]**.

**Android source of truth:** `HabitNotificationScheduler`, `HabitNotificationReceiver`, `BootReceiver`, `NextReminderCalculator`  
**Expo references:** [expo-notifications](https://docs.expo.dev/versions/latest/sdk/notifications/), [What you need to know](https://docs.expo.dev/push-notifications/what-you-need-to-know/) (SDK docs as of writing).

Kinetic is offline-first with **local** alarms only (no FCM/APNs server). Port should stay on **local scheduled notifications**, not Expo Push Service, unless you later add a server.

---

## Capability matrix (quick)

| Behavior | Android native (today) | Expo Android | Expo iOS |
|----------|------------------------|--------------|----------|
| Schedule habit reminder at wall-clock time | AlarmManager exact / inexact | Yes — `DateTrigger` / `DailyTrigger` / `WeeklyTrigger` + `delivery` | Yes — calendar / daily / weekly / date (no exact-alarm API) |
| Skip rest days / reminder day mask | App computes next fire 0–8 days | App must compute (same) | Same |
| Notification actions Done / Floor / Snooze | BroadcastReceiver → Room, app can stay dead | **Yes** via categories + `registerTaskAsync` when bg/killed | Actions yes; **killed → typically cold-starts app**; no headless Room write without launch |
| Body tap → open habit detail | Activity PI + `selected_habit_id` | Yes — response listener / last response | Yes — register listener early + `getLastNotificationResponse` |
| Evening digest | Alarm + nudges channel | Yes — `DailyTrigger` + channel | Yes — `DailyTrigger` |
| Re-arm after fire | Receiver schedules next | App must re-schedule in response/received handler or use repeating triggers | Same |
| Boot re-arm | `BootReceiver` | Library adds `RECEIVE_BOOT_COMPLETED` and restores schedules | N/A (iOS persists UNNotificationRequests) |
| Timezone change re-arm | `TIMEZONE_CHANGED` → BootReceiver | **Not equivalent** — **[RISK]** | OS may adjust calendar triggers; verify |
| Exact alarm when denied | Inexact `setAndAllowWhileIdle` fallback | `getExactAlarmPermissionsAsync` / inexact `bestEffort` | Always “granted” in Expo API; OS timing soft |
| Log from action with process dead | `goAsync` + Room | Android JS task can write SQLite/AsyncStorage | iOS: write after launch in response handler |

---

## 1. Habit reminder (schedule + present)

### Android today
- One-shot `AlarmManager` to next eligible `LocalDateTime` (`NextReminderCalculator`, offsets 0–8).
- Skips days without reminder bit; skips rest days.
- Exact if `canScheduleExactAlarms()`, else inexact allow-while-idle.
- On fire: show notification from **Intent extras** (no DB read for copy); then re-arm next from Room.
- Channels: `orbit_reminders_channel` (HIGH).

### Expo — can
| | Android | iOS |
|--|---------|-----|
| Present local notification at a future date | Yes (`DateTriggerInput`) | Yes |
| Daily at hour:minute | Yes (`DailyTriggerInput`) | Yes |
| Weekly on a weekday | Yes (`WeeklyTriggerInput`) | Yes |
| iOS-only rich calendar components | — | `CalendarTriggerInput` (iOS only) |
| Channels / importance | Yes (`setNotificationChannelAsync`) | N/A (use interruption level) |
| Exact-ish delivery flag | `delivery: 'bestEffort' \| 'alarmClock'` | No; use `interruptionLevel: 'timeSensitive'` for prominence |
| Request exact-alarm permission | `requestExactAlarmPermissionsAsync` (12+) | N/A |

### Expo — cannot / limited
- **No** first-class “skip these ISO weekdays (rest mask)” in the trigger — app must precompute or cancel/reschedule.
- **[RISK]** Repeating `DailyTrigger` / `WeeklyTrigger` **will fire on rest days** unless you cancel and reschedule or suppress in a received handler (received while killed may not run JS on iOS for local alerts).
- **[RISK]** Android 14+ denies `SCHEDULE_EXACT_ALARM` by default; without it, times drift under Doze (same class of issue as native fallback).
- **[RISK]** `alarmClock` shows a status-bar alarm icon until fire — UX cost for many habits.

### Closest design
1. Keep **`NextReminderCalculator`** (port pure function).
2. Per habit, `scheduleNotificationAsync` with **`DateTrigger`** = next fire (one-shot), `channelId: reminders`, content + `data: { habitId, kind: 'habit_reminder' }`, `categoryIdentifier: 'habit_reminder'`.
3. On Android set `delivery: 'alarmClock'` for reminders if exact permission granted; else `'bestEffort'`. Mirror Settings copy: *May be delayed without exact alarms.*
4. After present or after action handling, **cancel id + schedule next** from SQLite (mirror native re-arm).
5. Alternative: one `WeeklyTrigger` per enabled weekday × habit — fewer re-arms, but **[RISK]** rest-day fires unless you dynamically cancel that weekday’s request when rest mask changes.

**Killed:** OS delivers the local notification without the app process. Presentation works on both platforms.

---

## 2. Actions: Done (Target) / Floor / Snooze 15m

### Android today
- Notification actions → `PendingIntent.getBroadcast` → `HabitNotificationReceiver`.
- Done → `setCompletedForDate(today)`; Floor → `logFloorSustainForDate(today)` (effective floor in repo); Snooze → cancel UI + alarm +15m.
- Works with **app process dead** (`goAsync`).
- Floor button omitted if stored `floorValue` null/`≤0` (does **not** use 0.25 fallback for the button).

### Expo — can
| | Android | iOS |
|--|---------|-----|
| Define actions via categories | `setNotificationCategoryAsync` | Same |
| Action options | `opensAppToForeground`, destructive, auth | Same |
| Handle action in foreground/background | `addNotificationResponseReceivedListener` | Same |
| Handle action when **terminated** | **`registerTaskAsync`** runs for action taps (Android) | App **launches**; use top-level listener + `getLastNotificationResponse` / `useLastNotificationResponse` |
| Schedule snooze as +15 min local notif | `DateTrigger` now+15m | Same |

### Expo — cannot / limited
- **[RISK] iOS:** There is **no** BroadcastReceiver-equivalent that writes SQLite while remaining fully killed. Tapping Done/Floor **brings the app up** (even with `opensAppToForeground: false`, cold start / brief wake is the reliable path). User may see splash; log must run in JS as early as possible.
- **[RISK] Android killed actions:** Background task can lag **~3–5s** before running ([expo#36800](https://github.com/expo/expo/issues/36800)) — users may tap repeatedly.
- **[RISK]** Task must be `TaskManager.defineTask` + `Notifications.registerTaskAsync` in an **early-imported** module (`index.ts`); easy to misconfigure → silent no-ops when killed.
- **[RISK]** `registerTaskAsync` docs emphasize headless **push** when terminated for *receive*; action-tap support on Android is newer — pin Expo SDK version and regression-test Done/Floor from swipe-killed state.
- iOS cannot schedule a true AlarmManager snooze independent of notification permission / Focus modes the same way.

### Closest design
1. Category `habit_reminder` actions: `DONE`, `FLOOR`, `SNOOZE` (identifiers without `-`/`:` per Expo warning).
2. `FLOOR` only if `floorValue != null && floorValue > 0` (match Android button), **or** intentionally fix port to show Floor using `target*0.25` (product decision — **[RISK]** parity vs UX).
3. **Android:** In background task, on `actionIdentifier`:
   - DONE/FLOOR → write SQLite for **today**, dismiss notification, optional feedback local notif.
   - SNOOZE → dismiss, `scheduleNotificationAsync` +15m with same content/category/data.
4. **iOS:** Prefer `opensAppToForeground: true` for DONE/FLOOR so logging is reliable; handle in response listener **before** first paint if possible; show brief “Logged” toast. Snooze can use `opensAppToForeground: false` and schedule from response handler (still may launch).
5. Shared: after DONE/FLOOR, re-schedule next habit reminder (calculator).

**Killed summary**

| Action | Android Expo | iOS Expo |
|--------|--------------|----------|
| Done / Floor | Possible via JS task → SQLite without UI | App starts → log in response handler |
| Snooze | Schedule local notif from task | Schedule from response handler (app may open) |

---

## 3. Evening digest

### Android today
- Settings: enabled (default true), hour (default 21).
- `scheduleAt` → `ACTION_EVENING_DIGEST`; channel **nudges** (DEFAULT).
- On fire: if still enabled, show “Evening Momentum Review”; re-arm next day; tap opens app **without** habit id.

### Expo — can
- `DailyTriggerInput { hour, minute: 0, channelId: 'nudges' }` on both platforms.
- Or one-shot `DateTrigger` + re-arm (parity with habit path).
- Separate Android channel for lower importance.

### Expo — cannot / limited
- **[RISK]** `DailyTrigger` does not know DataStore “disabled” until fire — must `cancelScheduledNotificationAsync` when user toggles off (native cancels alarm).
- Minute is supported in Expo daily trigger (native digest is always `:00` — keep minute `0`).

### Closest design
- Identifier `digest_evening`; cancel/reschedule on settings change and on boot/app start reconciliation.
- Tap → navigate home only (`data.kind: 'digest'`).
- `delivery: 'bestEffort'` (less critical than habit reminders).

**Killed:** OS shows digest; no JS required until tap.

---

## 4. Tap routing (notification body)

### Android today
- ContentIntent → `MainActivity` with `selected_habit_id` if `habitId > 0`.
- VM deep link → open detail when habits loaded.

### Expo — can
- Put `habitId` in notification `data`.
- On response with `DEFAULT_ACTION_IDENTIFIER`, read `data.habitId` and navigate.
- Cold start: **`getLastNotificationResponse` / `useLastNotificationResponse`** (required on iOS).

### Expo — cannot / limited
- **[RISK]** If navigation runs before root navigator mounts, deep link is dropped — queue like Android `pendingDeepLinkId`.
- Digest / test with invalid ids must not open a missing habit.

### Closest design
- Mirror Android: `data: { habitId, kind }`; clear last response after handling (`clearLastNotificationResponse`).

**Killed:** Body tap always launches/resumes app — routing works if last-response is handled.

---

## 5. Boot / timezone / package-replace re-arm

### Android today
- `BootReceiver`: `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`.
- Re-reads Room + DataStore → schedule all habit reminders + digest.
- In-flight **snoozes are lost** across reboot.

### Expo — can
| | Android | iOS |
|--|---------|-----|
| Persist scheduled notifications across reboot | Yes — library uses `RECEIVE_BOOT_COMPLETED` | Yes — system keeps pending requests |
| Recompute from DB on app launch | Yes (recommended) | Yes |

### Expo — cannot / limited
- **[RISK]** Expo does **not** document a public `TIMEZONE_CHANGED` hook equivalent to Kinetic’s `BootReceiver`. After TZ change, wall-clock `DateTrigger` instants may be wrong until app runs.
- **[RISK]** Snooze one-shots still lost on reboot (same as native).
- **[RISK]** Android force-stop from Settings stops notifications until manual open (platform limit).

### Closest design
1. Rely on Expo’s boot restore for already-scheduled requests.
2. On every cold start / `AppState` active: **reconcile** — cancel all Kinetic-managed ids, reschedule from SQLite + settings via `NextReminderCalculator` (covers TZ drift when user opens app).
3. Optional: listen to `expo-localization` / app foreground after significant time jump; still **[RISK]** if user never opens app after TZ change until next wrong fire.
4. Document: snooze does not survive reboot (parity).

---

## 6. Exact alarm fallback

### Android today
- Exact → `setExactAndAllowWhileIdle`; else `setAndAllowWhileIdle`; catch → `set`.
- Settings hint when digest on && !exact.
- No in-app deep link to exact-alarm settings (only notification settings for POST).

### Expo — can
- `getExactAlarmPermissionsAsync` / `requestExactAlarmPermissionsAsync` (opens system UI).
- Without permission: approximate delivery; `alarmClock` falls back to `bestEffort`.
- iOS API reports granted always (no-op).

### Expo — cannot / limited
- **[RISK]** Cannot replicate three-tier AlarmManager catch cascade exactly; trust Expo’s `delivery` mapping.
- **[RISK]** Google Play policy: `USE_EXACT_ALARM` only for alarm/timer/calendar-like apps — habit reminders may need user-granted `SCHEDULE_EXACT_ALARM`, not the privileged permission.
- iOS Focus / Scheduled Summary can delay or group notifications — no app API to force exact minute.

### Closest design
- On Android 12+: prompt for exact alarms when user enables any habit reminder or digest; keep Settings banner.
- Reminders: `delivery: 'alarmClock'` if granted, else `'bestEffort'`.
- Digest: always `'bestEffort'`.
- iOS: set `interruptionLevel: 'timeSensitive'` on habit reminders (requires appropriate entitlement/usage; still not exact).

---

## 7. Channels, test notification, extras

| Topic | Expo approach | Risk |
|-------|---------------|------|
| Reminder + feedback channel HIGH | `setNotificationChannelAsync('orbit_reminders_channel' or new id, { importance: MAX/HIGH })` | Renaming channel id loses user settings |
| Nudges DEFAULT | Second channel for digest | — |
| Test notification | `scheduleNotificationAsync({ trigger: null })` immediate with test `data` | Don’t attach real habitId; Done should no-op or show “test” (fix Android 9999 → could not log) |
| Check-in title/body | Same string rules in scheduler when building content | Keep shared TS/Kotlin string helper |
| POST_NOTIFICATIONS | `requestPermissionsAsync` (Android 13+) | Native app currently only deep-links to settings — Expo should request properly |

---

## Recommended architecture (Expo app)

```
┌─────────────────────────────────────────────────────────┐
│ Early index.ts                                          │
│  TaskManager.defineTask(NOTIFICATION_TASK)              │
│  Notifications.registerTaskAsync(...)                   │
│  addNotificationResponseReceivedListener(...)           │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│ NotificationScheduler (TS port of NextReminderCalculator│
│  + schedule/cancel/reconcile)                           │
│  - DateTrigger one-shots per habit                      │
│  - DailyTrigger digest                                  │
│  - category habit_reminder                              │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│ Handlers                                                │
│  DONE/FLOOR → SQLite today + feedback + reschedule      │
│  SNOOZE → +15m DateTrigger                              │
│  DEFAULT tap → navigation queue (habit detail / home)   │
│  App launch → reconcile all schedules from DB           │
└─────────────────────────────────────────────────────────┘
```

**Do not rely on Expo Push / FCM for core habit reminders** (offline, privacy, killed-device delivery of actions is local-category based).

---

## Risk register (priority)

| ID | Risk | Severity | Mitigation |
|----|------|----------|------------|
| R1 | iOS Done/Floor cannot silently log while fully killed | High | Accept brief app open; log in cold-start response path |
| R2 | Android killed action task delay 3–5s | Medium | Immediate dismiss via system; debounce duplicate logs; feedback notif |
| R3 | Rest days fire if using repeating weekly/daily triggers | High | Prefer one-shot DateTrigger + calculator; never blind Daily per habit |
| R4 | Timezone change without app open | Medium | Reconcile on next launch; document gap vs native BootReceiver |
| R5 | Exact alarms denied on Android 14+ | Medium | Prompt + Settings banner; bestEffort fallback |
| R6 | `registerTaskAsync` mis-registration | High | E2E: kill app → tap Done → assert SQLite row |
| R7 | Floor button vs 0.25 effective floor mismatch | Low | Product choice; document in STRINGS/ALGORITHMS |
| R8 | Force-stop Android stops all notifs | Low | Document (platform) |
| R9 | iOS Focus / Summary delays | Medium | timeSensitive where allowed; no hard guarantee |
| R10 | Snooze lost on reboot | Low | Parity with native; optional persist snooze deadline in DB + reconcile |

---

## Testing checklist (port)

1. Schedule habit → kill app → notification appears at time (both OS).
2. Kill → **Done** → row completed for today (Android without UI; iOS after launch).
3. Kill → **Floor** → floor row; already-completed stays target.
4. Kill → **Snooze** → notification returns ~15m later.
5. Deny exact alarms → still eventually fires; banner shown.
6. Toggle rest day → next scheduled fire skips that weekday.
7. Reboot device → habits + digest still scheduled (Android).
8. Change timezone → open app → times correct after reconcile.
9. Digest tap → home, not detail.
10. Habit body tap → detail for that id.

---

*End of NOTIFICATION_PORT.md*
