# Kinetic Routines Release Checklist (Google Play, public)

## Build and signing
- [ ] Upload key generated and stored outside git; `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` set in the release environment only
- [ ] Play App Signing enabled in Play Console
- [ ] `versionCode` incremented, `versionName` updated semantically
- [ ] `./gradlew lintRelease testDebugUnitTest bundleRelease` green locally (release signing present)
- [ ] Release AAB installed and smoke-tested on one physical low-end device

## Fresh-install smoke test
- [ ] First launch shows an empty dashboard (no seeded habits); Settings -> Sample Habit -> Load adds exactly one sample habit with no logs, and deleting all habits survives relaunch
- [ ] Create habit in each modality (Yes/No, Numeric with decimal target, Timer, Tally) and log target/floor/custom/friction
- [ ] Timer only logs elapsed time; completing with 0s is impossible
- [ ] Reminder fires, repeats next day, Done/Floor actions are idempotent, snooze works
- [ ] Reboot device (or emulator) and confirm reminders are re-armed
- [ ] Encrypted backup exported, app data cleared, backup imported with identical habits/logs; wrong passphrase rejected
- [ ] Light, dark, and system themes readable on dashboard, detail, editor, settings, dialogs
- [ ] TalkBack can create, log, archive, and restore a habit

## Play Console declarations
- [ ] Play listing title set to "Kinetic Routines", short name / launcher label verified as "Routines", tagline "Momentum-based Habit Builder" in listing/About
- [ ] Data Safety form: no data collected, no data shared (offline-only, no network permission)
- [ ] Privacy policy URL live stating local-only storage and optional user-encrypted backup files
- [ ] Exact-alarm / scheduled-alarm declaration submitted with inexact fallback verified (deny exact alarms and confirm reminders still arrive, possibly batched)
- [ ] Notification permission rationale verified on Android 13+
- [ ] Target API level meets current Play requirement; pre-launch report has no fatal issues

## Rollout
- [ ] Internal track -> closed track soak (watch Play Console Android vitals; no crash SDK by design)
- [ ] Production staged rollout at 10%; halt criterion: crash-free sessions drop or reminder/backup defect reports
- [ ] Rollback plan: halt rollout, ship hotfix with incremented versionCode (never reuse versionCode; never change applicationId after first upload)


## Testing
.\gradlew.bat lintDebug            # same as CI "Lint" step (0 errors required)
.\gradlew.bat testDebugUnitTest    # same as CI "Unit tests" step (24 tests)
.\gradlew.bat assembleDebug        # same as CI "Assemble debug" step
Run all three exactly as CI does, in one go:
.\gradlew.bat lintDebug testDebugUnitTest assembleDebug