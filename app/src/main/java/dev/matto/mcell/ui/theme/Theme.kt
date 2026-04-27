package dev.matto.mcell.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    background = Bg,
    surface = BgElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primary = Accent,
    onPrimary = Bg,
)

@Composable
fun McellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = McellTypography,
        content = content,
    )
}
