package com.apk.music.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

val SamsungBlue = Color(0xFF007BFF)
val SamsungPink = Color(0xFFE91E63)

private val DarkColorScheme = ColorScheme(
    primary = SamsungBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF002D5F),
    onPrimaryContainer = Color.White,
    secondary = SamsungPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4D001C),
    onSecondaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surfaceContainer = Color(0xFF1C1C1C),
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray,
    outline = Color.Gray
)

@Composable
fun MusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
