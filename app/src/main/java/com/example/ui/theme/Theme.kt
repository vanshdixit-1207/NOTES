package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IOSDarkColorScheme = darkColorScheme(
    primary = IOSYellowDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3A2E00),
    onPrimaryContainer = IOSYellowDark,
    secondary = IOSYellowLight,
    onSecondary = Color.Black,
    tertiary = IOSBlue,
    background = IOSDarkGroupedBg,
    onBackground = IOSDarkTextPrimary,
    surface = IOSDarkCardSurface,
    onSurface = IOSDarkTextPrimary,
    surfaceVariant = IOSDarkCardSecondary,
    onSurfaceVariant = IOSDarkTextSecondary,
    outline = IOSDarkSeparator,
    error = IOSRed
)

private val IOSLightColorScheme = lightColorScheme(
    primary = IOSYellow,
    onPrimary = Color.White,
    primaryContainer = IOSYellowBg,
    onPrimaryContainer = Color(0xFF6B4800),
    secondary = IOSYellowLight,
    onSecondary = Color.Black,
    tertiary = IOSBlue,
    background = IOSLightGroupedBg,
    onBackground = IOSLightTextPrimary,
    surface = IOSLightCardSurface,
    onSurface = IOSLightTextPrimary,
    surfaceVariant = IOSLightSearchBg,
    onSurfaceVariant = IOSLightTextSecondary,
    outline = IOSLightSeparator,
    error = IOSRed
)

@Composable
fun IOSNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IOSDarkColorScheme else IOSLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IOSTypography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    IOSNotesTheme(darkTheme = darkTheme, content = content)
}
