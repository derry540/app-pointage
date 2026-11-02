package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ThermalCopper,
    onPrimary = Color.White,
    primaryContainer = ThermalCopperDark,
    onPrimaryContainer = Color.White,
    secondary = CryoSkyBlue,
    onSecondary = SlateDarkBg,
    secondaryContainer = SlateElevatedSurface,
    onSecondaryContainer = Color.White,
    tertiary = EmeraldSuccess,
    onTertiary = SlateDarkBg,
    background = SlateDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = SlateCardSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SlateElevatedSurface,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = CrimsonAlert,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ThermalCopperDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = CryoSteelBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    background = TechnicalLightBg,
    onBackground = SlateDarkBg,
    surface = TechnicalLightSurface,
    onSurface = SlateDarkBg,
    surfaceVariant = TechnicalLightVariant,
    onSurfaceVariant = Color(0xFF334155),
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
