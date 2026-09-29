package com.example.footygame.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// One committed look: a floodlit stadium at night. The game ignores the system light/dark setting on purpose.
private val FootyColors = darkColorScheme(
    primary = Hot,
    onPrimary = Chalk,
    primaryContainer = DugoutRaised,
    onPrimaryContainer = Chalk,
    secondary = Floodlight,
    onSecondary = Ink,
    secondaryContainer = DugoutRaised,
    onSecondaryContainer = Chalk,
    tertiary = Violet,
    onTertiary = Chalk,
    background = Night,
    onBackground = Chalk,
    surface = Dugout,
    onSurface = Chalk,
    surfaceVariant = DugoutRaised,
    onSurfaceVariant = ChalkMuted,
    surfaceContainerLowest = Night,
    surfaceContainerLow = Dugout,
    surfaceContainer = Dugout,
    surfaceContainerHigh = DugoutRaised,
    surfaceContainerHighest = DugoutRaised,
    outline = ChalkMuted,
    outlineVariant = ChalkLine,
    error = ResultLoss,
    onError = Chalk,
    scrim = Color.Black,
)

private val FootyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun FootyGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FootyColors, typography = Typography, shapes = FootyShapes, content = content)
}
