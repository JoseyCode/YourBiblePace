package com.example.biblepaceproject.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LibraryLight = lightColorScheme(
    primary = Leather,
    onPrimary = Parchment,
    primaryContainer = Vellum,
    onPrimaryContainer = LeatherDeep,
    secondary = Gilt,
    onSecondary = Parchment,
    secondaryContainer = Vellum,
    onSecondaryContainer = LeatherDeep,
    tertiary = Sepia,
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
    surfaceVariant = ParchmentDark,
    onSurfaceVariant = Leather,
    surfaceContainer = ParchmentDark,
    surfaceContainerLow = ParchmentDark,
    surfaceContainerHigh = Vellum,
    outline = Sepia,
    outlineVariant = Vellum,
)

private val LibraryDark = darkColorScheme(
    primary = Brass,
    onPrimary = Espresso,
    primaryContainer = WalnutLight,
    onPrimaryContainer = Lamplight,
    secondary = Amber,
    onSecondary = Espresso,
    secondaryContainer = WalnutLight,
    onSecondaryContainer = Lamplight,
    tertiary = Sepia,
    background = Espresso,
    onBackground = Lamplight,
    surface = Espresso,
    onSurface = Lamplight,
    surfaceVariant = Walnut,
    onSurfaceVariant = Brass,
    surfaceContainer = Walnut,
    surfaceContainerLow = Walnut,
    surfaceContainerHigh = WalnutLight,
    outline = Sepia,
    outlineVariant = WalnutLight,
)

/** Deliberately no dynamic (wallpaper) color: the brown library look is the brand. */
@Composable
fun BiblePaceProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LibraryDark else LibraryLight,
        typography = Typography,
        content = content
    )
}
