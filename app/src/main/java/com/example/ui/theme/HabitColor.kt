package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/** Single hex -> Color parser for habit accent colors. */
fun parseHabitColor(hex: String, fallback: Color = KinetiqEmerald): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { fallback }
