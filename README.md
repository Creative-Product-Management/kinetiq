# Kinetiq

Offline-first Android habit tracker built around elastic goals: every habit has a
target and a minimum "floor", scheduled rest days that shield your momentum, routine
stacks, and analytics that show strength, streaks, and friction patterns.

**Privacy model:** Kinetiq is strictly front-end only. There is no account, server,
sync, or tracking. Habits and logs live in an on-device Room database and never leave
the phone unless you export a passphrase-encrypted backup file yourself.

## Run locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (JDK 17)

1. Open Android Studio, select **Open**, and choose this directory.
2. Let Gradle sync finish.
3. Run the `app` configuration on an emulator or physical device.

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Architecture

- `data/db` — Room DAOs (habits/stacks, log entries, categories), schema v3 with explicit migrations (`app/schemas`)
- `data/repository` — `HabitRepository`, `LogRepository` (windowed queries, atomic/idempotent writes), `SettingsRepository` (DataStore)
- `data/backup` — passphrase-encrypted (AES-GCM + PBKDF2) export/import via the system file picker
- `domain/analytics` — deterministic, clock-injected habit analytics with an in-memory memo cache
- `domain/time` — `KinetiqClock` / `DateCodec` (`yyyy-MM-dd`, locale-independent)
- `notification` — repeating reminders honoring reminder/rest days, boot re-arming, Play-compliant exact-alarm fallback
- `ui` — Jetpack Compose + Navigation Compose, Material 3 theming (light/dark/system)

Room is the single source of truth. In-memory `StateFlow` windows (14 days for the
dashboard, per-habit history for detail) keep the UI instant without duplicating data.

## Release

See [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md). Release builds use R8 and
require the upload key via `KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_PASSWORD`
environment variables (never committed).
