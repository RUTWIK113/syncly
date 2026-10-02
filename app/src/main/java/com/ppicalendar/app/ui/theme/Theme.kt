package com.ppicalendar.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SynclyPrimaryAmber,             // #FFC212 - Vibrant Golden Amber
    onPrimary = SynclyOnPrimary,              // #171721 - Deep Charcoal on Amber
    primaryContainer = SynclySurfaceVariant,  // #2D2D32
    onPrimaryContainer = SynclyPrimaryAmber,  // #FFC212
    secondary = SynclyPrimaryAmber,
    onSecondary = SynclyOnPrimary,
    secondaryContainer = SynclyDarkSurface,   // #1F222B
    onSecondaryContainer = SynclyTextPrimary,
    tertiary = AccentGreen,
    onTertiary = PureWhite,
    background = SynclyDarkBg,                // #171721 - Deep Canvas
    onBackground = SynclyTextPrimary,         // #FFFFFF
    surface = SynclyDarkSurface,              // #1F222B - Card / Modal / Nav surface
    onSurface = SynclyTextPrimary,            // #FFFFFF
    surfaceVariant = SynclySurfaceVariant,    // #2D2D32 - Chip / Container
    onSurfaceVariant = SynclyTextSecondary,   // #B0B4C3
    outline = SynclyBorder,                   // #383A42 - Crisp Card Border
    outlineVariant = SynclyBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = SynclyPrimaryAmber,
    onPrimary = PureBlack,
    primaryContainer = Color(0xFFFFF4D4),
    onPrimaryContainer = PureBlack,
    secondary = SynclyDarkBg,
    onSecondary = PureWhite,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = PureBlack,
    tertiary = AccentGreen,
    onTertiary = PureWhite,
    background = OffWhite,
    onBackground = PureBlack,
    surface = LightSurface,
    onSurface = PureBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Gray600,
    outline = LightBorder,
    outlineVariant = SynclyBorderLight
)

@Composable
fun PPICalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Pure monochrome theme looks best without dynamic wallpaper tinting
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
