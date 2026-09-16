package com.example.footygame.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// One committed look: a floodlit night pitch. The game ignores the system light/dark setting on purpose.
private val FootyColors = darkColorScheme(
    primary = Floodlight,
    onPrimary = Ink,
    primaryContainer = DugoutRaised,
    onPrimaryContainer = Chalk,
    secondary = StickerPaper,
    onSecondary = Ink,
    secondaryContainer = DugoutRaised,
    onSecondaryContainer = Chalk,
    background = Pitch,
    onBackground = Chalk,
    surface = Dugout,
    onSurface = Chalk,
    surfaceVariant = DugoutRaised,
    onSurfaceVariant = ChalkMuted,
    surfaceContainerLowest = Dugout,
    surfaceContainerLow = Dugout,
    surfaceContainer = Dugout,
    surfaceContainerHigh = DugoutRaised,
    surfaceContainerHighest = DugoutRaised,
    outline = ChalkMuted,
    outlineVariant = ChalkLine,
    error = ResultLoss,
    onError = Ink,
    scrim = Color.Black,
)

private val FootyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FootyGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FootyColors, typography = Typography, shapes = FootyShapes, content = content)
}
