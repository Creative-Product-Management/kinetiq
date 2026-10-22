# Kinetic Momentum Score

Every habit has a **Momentum score (0–100%)**, shown as the gauge on the
habit detail screen. It answers one question: *"How strong is this habit
right now?"* — not how perfect your all-time record is.

## The short version

- Momentum is a **rolling score with memory**. Recent days count far more
  than old ones.
- Hitting your **target** is the fastest way to build it.
- Hitting your **floor** on a hard day still builds it — at 65% of the
  value of a full completion. A floor-only streak tops out around 65%.
- A **missed day** doesn't reset you to zero. Your score decays gently
  (×0.93 per missed day, roughly halving every 10 days).
- **Rest days (Rest Shield) freeze the score** — no gain, no loss.
- Days before you created the habit never count against you.

## How it's calculated

Momentum is an exponential moving average (EMA) over your daily results:

    momentum(today) = 0.93 × momentum(yesterday) + 0.07 × value(today)

| Day result             | Value |
|------------------------|-------|
| Target completed       | 1.00  |
| Floor sustained        | 0.65  |
| Missed / below floor   | 0.00  |
| Rest day, not logged   | Score frozen (no update) |

Only the **last 90 days** are considered, and the score is calculated
starting from the day the habit was created.

### What this feels like in practice

Logging your target every day from a brand-new habit:

| Days of target hits | Momentum |
|---------------------|----------|
| 1                   | ~7%      |
| 7                   | ~40%     |
| 14                  | ~64%     |
| 30                  | ~89%     |
| 60                  | ~99%     |

Recovery is fast, too: because recent days dominate, about two solid
weeks rebuild most of a decayed score.

## Trajectory labels

Under the percentage you'll see one of four labels, based on how your
score changed over the last 7 days:

| Label           | Meaning                          |
|-----------------|----------------------------------|
| Accelerating    | Up more than 3 points in 7 days  |
| Rebuilding      | Down more than 3 points in 7 days|
| Steady High     | Holding at 70% or above          |
| Forming Routine | Everything else — still building |

## What does *not* affect Momentum

- Exceeding your target (150% of goal = same as 100%)
- Partial progress below your floor (same as a miss — so always log
  at least the floor!)
- Your all-time streak or total completions
- Time of day, cues, or friction tags

## Design philosophy

Momentum rewards **showing up recently and consistently**, forgives
slips gradually instead of resetting you to zero, and treats a floor
day as a real win — because keeping the chain alive on a hard day is
what makes habits stick.

## Basis & limitations

**The math is standard; the exact recipe is Kinetic's own.** There is
no universally accepted "habit momentum score" standard.

- **Standard technique.** Momentum is an *exponentially weighted moving
  average* (EWMA/EMA), a well-established method in statistics, signal
  processing, and time-series analysis. Its defining property — recent
  behaviour matters more than older behaviour — is exactly what a
  "current strength" score needs. With Kinetic's weighting, the score
  has an effective memory of roughly 28 days and a decay half-life of
  about 9.5 days after missed days.
- **Philosophy aligned with behaviour-change practice.** Several choices
  reflect widely used habit-building principles:
  - Floor days earning real credit reflects "shrink the habit" /
    minimum-viable-effort design (popularised by BJ Fogg's Tiny Habits
    approach and elastic habit design).
  - Gentle decay instead of a reset-to-zero avoids the all-or-nothing
    abandonment that follows a broken streak. Research on habit
    formation (Lally et al., 2010) likewise found that missing a single
    occasion did not materially affect habit formation.
  - The ~90-day horizon is loosely consistent with habit-formation
    timescales: Lally et al. found automaticity took on average ~66
    days, with a very wide range (18–254 days). Kinetic's score reaches
    ~89% after 30 perfect days and matures over the following weeks.
- **What is *not* standardised.** The specific numbers — 0.93 damping,
  floor = 0.65, "Steady High" at 70%, trajectory thresholds of ±3 points
  in 7 days, and the 90-day window — are product tuning choices selected
  for feel (builds in ~2–4 weeks, forgives over ~2 weeks). No study
  prescribes them.
- **Not a clinical measure.** Momentum is an engagement and feedback
  metric — a mirror of your recent consistency. It does not measure
  habit automaticity the way research instruments such as the
  Self-Report Habit Index (SRHI) do, and it should not be interpreted
  as a medical or psychological assessment.

In one sentence: *Momentum uses a standard statistical technique,
combined with mainstream behaviour-design principles; its specific
weights are tuned for Kinetic and are not a clinical measure of habit
strength.*

---

## Technical appendix (for contributors)

Momentum is implemented as `habitStrengthPercent` in
`HabitAnalyticsResult`, computed by `HabitAnalyticsEngine` in
`app/src/main/java/com/example/domain/analytics/HabitAnalyticsEngine.kt`.

### Algorithm

1. The engine walks day by day from
   `max(today − 90 days, habit.createdDate)` to today, starting with
   `strength = 0`.
2. Each day maps to a value: `1.0` if the day's log has
   `isCompleted`, `0.65` if it has `isFloorSustained`, otherwise `0.0`.
   The `isCompleted` / `isFloorSustained` flags are set by
   `LogRepository` when a log is written (amount ≥ target → completed;
   floor ≤ amount < target → floor sustained), per modality
   (numeric value, tally count, or timer duration).
3. The score updates as an EMA with damping `0.93`:
   `strength = strength × 0.93 + 0.07 × value`.
4. On a scheduled rest day (`habit.isRestDay(...)` using the habit's
   `restDaysMask`) with no completed/floor log, the update is **skipped**
   when the global Rest Shield setting is enabled — the score is frozen.
   Logging on a rest day counts normally.
5. The final score is `(strength × 100)` rounded to the nearest integer
   and clamped to `0..100`.

### Trajectory

The engine also records the last 30 days of daily scores
(`strengthHistory`). The trajectory compares today's score with the
score 7 days ago: `> +3` → `Accelerating`, `< −3` → `Rebuilding`,
otherwise `≥ 70` → `Steady High`, else `Forming Routine`.
Note: `strengthHistory` is currently computed but not displayed in the
UI (available for a future momentum chart).

### Delivery pipeline

- `KineticViewModel.selectedAnalytics` combines the selected habit, its
  log records, and settings, then computes on `Dispatchers.Default`.
- Results are memoized in `AnalyticsCache` (keyed by habit ID + habit
  hash + a hash of the exact records list used + the two settings above;
  max 32 entries), so reopening a detail screen is instant unless the
  underlying data changed. Keying on the records themselves (rather than
  a separately observed version stamp) guarantees a cached result can
  never have been computed from a different, stale record list.
- `KineticGauge` renders the percentage as a 270° arc, animating to the
  value over 900 ms. The trajectory badge is emerald at ≥ 70, gold
  at ≥ 40, and violet below 40.

### Behaviour guaranteed by tests

`AnalyticsDeterministicTest` pins down the key properties:

- A new habit is not penalized for days before its creation.
- A floor sustain scores strictly below a target completion and at or
  above a miss.
- Rest Shield days preserve the streak/score without inflating them.
- The analytics cache only hits for the exact records list a result was
  computed from, and a Yes/No day-tap (toggle) counts as a target
  completion worth 7% Momentum on its first day.
