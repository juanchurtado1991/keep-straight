package com.keepstraight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnBackground,
    secondary = Secondary,
    onSecondary = OnPrimary,
    background = Background,
    surface = Surface,
    surfaceVariant = Color(0xFFE4EFEC),
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    onBackground = OnBackground,
    onSurface = OnBackground,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryContainer,
    onPrimary = OnBackground,
    primaryContainer = Primary,
    onPrimaryContainer = OnPrimary,
    secondary = PrimaryContainer,
    onSecondary = OnBackground,
    background = Color(0xFF101414),
    surface = Color(0xFF171C1B),
    surfaceVariant = Color(0xFF24302E),
    onSurfaceVariant = Color(0xFFB6C5C1),
    error = Error,
    onBackground = Color(0xFFE6F0ED),
    onSurface = Color(0xFFE6F0ED),
)

@Composable
fun KeepStraightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
