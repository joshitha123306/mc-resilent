package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = PureWhite,
    primaryContainer = CobaltContainerLight,
    onPrimaryContainer = NavyDeep,
    secondary = CobaltAccent,
    onSecondary = PureWhite,
    secondaryContainer = SoftSlateSurface,
    onSecondaryContainer = NavyDeep,
    tertiary = SignalGreen,
    onTertiary = PureWhite,
    tertiaryContainer = SignalGreenContainerLight,
    onTertiaryContainer = SignalGreenContainerDark,
    error = EmergencyRed,
    onError = PureWhite,
    errorContainer = EmergencyRedContainerLight,
    onErrorContainer = EmergencyRedContainerDark,
    background = IceBackground,
    onBackground = TextPrimaryLight,
    surface = PureWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SoftSlateSurface,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CobaltBright,
    onPrimary = NavyDeep,
    primaryContainer = NavyElevated,
    onPrimaryContainer = TextPrimaryDark,
    secondary = CobaltAccent,
    onSecondary = PureWhite,
    secondaryContainer = CobaltContainerDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = SignalGreenBright,
    onTertiary = NightNavyBackground,
    tertiaryContainer = SignalGreenContainerDark,
    onTertiaryContainer = SignalGreenContainerLight,
    error = EmergencyRedBright,
    onError = PureWhite,
    errorContainer = EmergencyRedContainerDark,
    onErrorContainer = EmergencyRedContainerLight,
    background = NightNavyBackground,
    onBackground = TextPrimaryDark,
    surface = NightNavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NightNavyCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ResilientConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
