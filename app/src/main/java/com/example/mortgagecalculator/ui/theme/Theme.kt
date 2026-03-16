package com.example.mortgagecalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    secondary = Color(0xFF5856D6),
    tertiary = Color(0xFFFF2D55),
    background = Color(0xFFF2F2F7),
    surface = Color.White,
)

@Composable
fun MortgageCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
