package com.example.footygame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.Hot
import com.example.footygame.theme.ResultWin
import com.example.footygame.theme.Sky
import com.example.footygame.theme.Violet
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(Hot, Floodlight, Violet, Sky, ResultWin)

/** Slowest fall, in screen heights per second; every flake has cleared the screen by [BURST_SECONDS]. */
private const val MIN_SPEED = 0.28f
private const val BURST_SECONDS = 2f / MIN_SPEED

private class Flake(
    val x: Float,
    /** How far above the top edge the flake starts, in screen heights, so the burst arrives in waves. */
    val lead: Float,
    val speed: Float,
    val sway: Float,
    val size: Float,
    val spin: Float,
    val colorIndex: Int,
)

/**
 * One burst of paper confetti falling through the element, for perfect runs and trophies. [seed] fixes the
 * pattern so a recomposition doesn't reshuffle it. Stops drawing once the last flake has fallen, and is
 * skipped entirely with system animations off.
 */
@Composable
fun Confetti(seed: Long, modifier: Modifier = Modifier, count: Int = 80) {
    val flakes = remember(seed) {
        val random = Random(seed)
        List(count) {
            Flake(
                x = random.nextFloat(),
                lead = random.nextFloat(),
                speed = MIN_SPEED + random.nextFloat() * 0.2f,
                sway = 4f + random.nextFloat() * 10f,
                size = 4f + random.nextFloat() * 4f,
                spin = random.nextFloat() * 360f,
                colorIndex = random.nextInt(ConfettiColors.size),
            )
        }
    }
    val reducedMotion = rememberReducedMotion()
    // The clock is only read while drawing, so the fall redraws the canvas without recomposing it.
    var seconds by remember(seed) { mutableFloatStateOf(0f) }
    var done by remember(seed) { mutableStateOf(false) }
    LaunchedEffect(seed, reducedMotion) {
        if (reducedMotion) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (seconds < BURST_SECONDS) {
            withFrameMillis { seconds = (it - start) / 1000f }
        }
        done = true
    }
    if (reducedMotion || done) return

    Canvas(modifier) {
        val unit = 1.dp.toPx()
        flakes.forEach { flake ->
            val side = flake.size * unit
            val y = (seconds * flake.speed - flake.lead) * size.height
            if (y < -side || y > size.height + side) return@forEach
            val x = flake.x * size.width + sin(seconds * 2f + flake.lead * 6f) * flake.sway * unit
            rotate(flake.spin + seconds * 120f, pivot = Offset(x, y)) {
                drawRect(
                    color = ConfettiColors[flake.colorIndex],
                    topLeft = Offset(x - side / 2, y - side / 2),
                    size = Size(side, side * 1.4f),
                )
            }
        }
    }
}
