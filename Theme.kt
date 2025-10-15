package com.example.eflplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Светлая цветовая схема
private val LightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC5),
    background = Color(0xFFF2F2F2),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// Тёмная цветовая схема
private val DarkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC5),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

// Стандартная типографика Material3
private val AppTypography = Typography()

@Composable
fun EFLPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicPrimary: Color? = null, // динамический цвет по обложке
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val finalColors = colors.copy(
        primary = dynamicPrimary ?: colors.primary
    )

    MaterialTheme(
        colorScheme = finalColors,
        typography = AppTypography,
        content = content
    )
}
