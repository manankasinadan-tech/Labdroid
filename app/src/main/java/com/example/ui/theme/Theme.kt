package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val DarkColorScheme = darkColorScheme(
    primary = LabdroidDarkPrimary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = LabdroidDarkSecondary,
    onSecondary = Color(0xFF0F172A),
    tertiary = LabdroidDarkTertiary,
    onTertiary = Color(0xFF0F172A),
    background = LabdroidDarkBg,
    onBackground = Color(0xFFF8FAFC),
    surface = LabdroidDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = LabdroidDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = LabdroidDarkOutline,
    outlineVariant = LabdroidDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LabdroidLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = LabdroidLightSecondary,
    onSecondary = Color.White,
    tertiary = LabdroidLightTertiary,
    onTertiary = Color.White,
    background = LabdroidLightBg,
    onBackground = Color(0xFF0F172A),
    surface = LabdroidLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LabdroidLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF334155),
    outline = LabdroidLightOutline,
    outlineVariant = LabdroidLightOutlineVariant
)

@Composable
fun LabdroidTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
