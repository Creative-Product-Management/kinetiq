package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Popular habit accent presets (bright, saturated — not muted).
 * Stored on the habit as `#RRGGBB` and shown on cards / activity chrome only.
 */
val HABIT_COLOR_PRESETS: List<String> = listOf(
    "#10B981", // emerald
    "#14B8A6", // teal
    "#06B6D4", // cyan
    "#0EA5E9", // sky
    "#3B82F6", // blue
    "#6366F1", // indigo
    "#8B5CF6", // violet
    "#A855F7", // purple
    "#EC4899", // pink
    "#F43F5E", // rose
    "#EF4444", // red
    "#F97316", // orange
    "#F59E0B", // amber
    "#EAB308", // yellow
    "#84CC16", // lime
    "#22C55E"  // green
)

const val DEFAULT_HABIT_COLOR_HEX = "#10B981"

/** Single hex -> Color parser for habit accent colors. */
fun parseHabitColor(hex: String, fallback: Color = KineticEmerald): Color =
    try { Color(android.graphics.Color.parseColor(normalizeHex(hex))) } catch (_: Exception) { fallback }

fun normalizeHex(raw: String): String {
    val cleaned = raw.trim().removePrefix("#").uppercase()
    return when (cleaned.length) {
        6 -> "#$cleaned"
        3 -> "#${cleaned.map { "$it$it" }.joinToString("")}"
        else -> raw.trim().let { if (it.startsWith("#")) it else "#$it" }
    }
}

fun colorToHex(color: Color): String {
    val a = android.graphics.Color.argb(
        (color.alpha * 255).toInt().coerceIn(0, 255),
        (color.red * 255).toInt().coerceIn(0, 255),
        (color.green * 255).toInt().coerceIn(0, 255),
        (color.blue * 255).toInt().coerceIn(0, 255)
    )
    return String.format("#%06X", 0xFFFFFF and a)
}

fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hsv = floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            (color.alpha * 255).toInt().coerceIn(0, 255),
            (color.red * 255).toInt().coerceIn(0, 255),
            (color.green * 255).toInt().coerceIn(0, 255),
            (color.blue * 255).toInt().coerceIn(0, 255)
        ),
        hsv
    )
    return hsv
}
