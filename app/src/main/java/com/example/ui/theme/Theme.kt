package com.example.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

enum class ThemeMode(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System Default")
}

private val KinetiqDarkColorScheme = darkColorScheme(
    primary = KinetiqEmerald,
    onPrimary = Color.Black,
    primaryContainer = CardSurfaceElevated,
    onPrimaryContainer = KinetiqEmeraldGlow,
    secondary = KinetiqViolet,
    onSecondary = Color.White,
    secondaryContainer = CardSurfaceElevated,
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = KinetiqAmber,
    onTertiary = Color.Black,
    background = VoidBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = MissedRed,
    onError = Color.White
)

private val KinetiqLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun KinetiqHabitTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) KinetiqDarkColorScheme else KinetiqLightColorScheme
    val view = LocalView.current

    LaunchedEffect(isDark, colorScheme.background) {
        val activity = view.context.findActivity() ?: return@LaunchedEffect
        val transparent = AndroidColor.TRANSPARENT
        val barStyle = if (isDark) {
            SystemBarStyle.dark(transparent)
        } else {
            SystemBarStyle.light(transparent, transparent)
        }
        activity.enableEdgeToEdge(
            statusBarStyle = barStyle,
            navigationBarStyle = barStyle
        )
        activity.window.decorView.setBackgroundColor(colorScheme.background.toArgb())
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    KinetiqHabitTheme(themeMode = ThemeMode.DARK, content = content)
}
