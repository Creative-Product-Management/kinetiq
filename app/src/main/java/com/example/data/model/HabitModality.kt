package com.example.data.model

enum class HabitModality(val displayName: String, val defaultUnit: String) {
    YES_NO("Yes / No", ""),
    NUMERIC("Numeric Goal", "units"),
    TIMER("Duration Timer", "mins"),
    TALLY("Daily Tally", "times")
}
