package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SoftLavender,
    onPrimary = DeepPurpleAccent,
    secondary = SoftRose,
    onSecondary = DeepPurpleAccent,
    tertiary = LightLavender,
    background = MatteBlack,
    onBackground = OnSurfaceWhite,
    surface = CharcoalDark,
    onSurface = OnSurfaceWhite,
    surfaceVariant = ObsidianSurface,
    onSurfaceVariant = TextGray,
    outline = BorderGlass
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    secondary = VibrantGreen,
    onSecondary = SpaceBlack,
    tertiary = FavoriteRed,
    background = Color(0xFFF6F8FC),
    onBackground = SpaceBlack,
    surface = Color.White,
    onSurface = SpaceBlack,
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFFC4C6D0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark mode as default for modern music atmosphere!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
