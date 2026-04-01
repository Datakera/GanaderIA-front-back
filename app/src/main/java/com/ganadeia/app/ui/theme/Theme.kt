package com.ganadeia.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    secondary = AccentOrange,
    background = BackgroundCream,
    surface = CardLight,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun GanadeIATheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
