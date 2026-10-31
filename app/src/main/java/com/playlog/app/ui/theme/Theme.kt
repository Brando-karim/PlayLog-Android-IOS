package com.playlog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PlaylogColors = darkColorScheme(
    primary = Accent,
    onPrimary = JungleInk,
    primaryContainer = Color(0xFF123222),
    onPrimaryContainer = PaleMint,
    secondary = Teal,
    onSecondary = JungleInk,
    secondaryContainer = Color(0xFF0E2A20),
    onSecondaryContainer = PaleMint,
    tertiary = FocusRing,
    onTertiary = JungleInk,
    background = Obsidian,
    onBackground = MintWhite,
    surface = Pine,
    onSurface = MintWhite,
    surfaceVariant = Moss,
    onSurfaceVariant = Sage,
    surfaceContainer = Color(0xFF0C1610),
    surfaceContainerHigh = Pine,
    surfaceContainerHighest = Color(0xFF16221A),
    surfaceContainerLow = Color(0xFF09110C),
    surfaceContainerLowest = Obsidian,
    outline = Color(0xFF2A3A30),
    outlineVariant = Color(0xFF1A2A20),
    error = RoseMuted,
    onError = JungleInk,
    scrim = Color(0xCC020503),
    surfaceTint = Color.Transparent,
)

@Composable
fun PlaylogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PlaylogColors,
        typography = PlaylogTypography,
        shapes = PlaylogShapes,
        content = content,
    )
}
