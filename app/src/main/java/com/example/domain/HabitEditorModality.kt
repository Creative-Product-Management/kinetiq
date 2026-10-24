package com.example.domain

import com.example.data.model.HabitModality

/** Top-level editor tab: Check-in (yes/no) vs Measurable (target/floor/unit). */
enum class EditorKind { CHECK_IN, MEASURABLE }

/** How a Measurable habit is logged on the day strip / quick log. */
enum class MeasurableLogStyle {
    AMOUNT,
    COUNT,
    TIMER;

    fun toModality(): HabitModality = when (this) {
        AMOUNT -> HabitModality.NUMERIC
        COUNT -> HabitModality.TALLY
        TIMER -> HabitModality.TIMER
    }

    companion object {
        fun fromModality(modality: HabitModality): MeasurableLogStyle? = when (modality) {
            HabitModality.NUMERIC -> AMOUNT
            HabitModality.TALLY -> COUNT
            HabitModality.TIMER -> TIMER
            HabitModality.YES_NO -> null
        }
    }
}

fun HabitModality.toEditorKind(): EditorKind =
    if (this == HabitModality.YES_NO) EditorKind.CHECK_IN else EditorKind.MEASURABLE

fun resolveEditorModality(kind: EditorKind, logStyle: MeasurableLogStyle): HabitModality =
    if (kind == EditorKind.CHECK_IN) HabitModality.YES_NO else logStyle.toModality()

/** Read-only label for the habit editor (type is locked after create). */
fun modalityDisplayLabel(modality: HabitModality): String = when (modality) {
    HabitModality.YES_NO -> "Check-in"
    HabitModality.NUMERIC -> "Measurable · Amount"
    HabitModality.TALLY -> "Measurable · Count"
    HabitModality.TIMER -> "Measurable · Timer"
}
