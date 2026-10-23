package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/** Darker, desaturated presets for habit accents. */
val HABIT_COLOR_PRESETS: List<String> = listOf(
    "#2F6F5E", // deep teal
    "#4A5568", // slate
    "#3D4F6F", // slate blue
    "#6B4E31", // olive brown
    "#4A5D4E", // charcoal green
    "#6B3F5A", // burgundy mauve
    "#3F5A6B", // steel
    "#5A5346", // warm charcoal
    "#4A3D6B", // muted violet
    "#6B5A3D", // khaki
    "#3D4A6B", // evening blue
    "#5A3D3D"  // dusty maroon
)

const val DEFAULT_HABIT_COLOR_HEX = "#2F6F5E"

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
