package com.example.footygame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.footygame.theme.GlowMagenta
import com.example.footygame.theme.GlowViolet
import com.example.footygame.theme.Night
import com.example.footygame.theme.Pitch
import com.example.footygame.theme.PitchLine
import com.example.footygame.theme.PitchStripe
import kotlin.math.max
import kotlin.math.min

/** Every screen's backdrop: near-black with a violet floodlight glow top right and a magenta one bottom left. */
fun Modifier.nightBackdrop(): Modifier = drawBehind {
    drawRect(Night)
    val reach = max(size.width, size.height)
    drawRect(
        Brush.radialGradient(
            listOf(GlowViolet.copy(alpha = 0.85f), Color.Transparent),
            center = Offset(size.width * 0.95f, 0f),
            radius = reach * 0.62f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            listOf(GlowMagenta.copy(alpha = 0.8f), Color.Transparent),
            center = Offset(0f, size.height),
            radius = reach * 0.5f,
        ),
    )
}

/** Mown grass: alternating horizontal bands, [bands] of them over the element's height. */
fun Modifier.mownStripes(bands: Int = 12): Modifier = drawBehind {
    drawRect(Pitch)
    val band = size.height / bands
    var top = band
    while (top < size.height) {
        drawRect(PitchStripe, topLeft = Offset(0f, top), size = Size(size.width, band))
        top += band * 2
    }
}

/** A striped, rounded pitch with its markings drawn under [content]. */
@Composable
fun PitchCard(
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .clip(shape)
            .mownStripes()
            .border(1.dp, PitchLine, shape),
    ) {
        PitchMarkings(Modifier.fillMaxSize())
        content()
    }
}

/** Chalk markings of a full pitch, attacking end at the top. */
@Composable
fun PitchMarkings(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 1.2.dp.toPx())
        val field = Rect(0f, 0f, size.width, size.height)
        drawLine(PitchLine, Offset(field.left, field.center.y), Offset(field.right, field.center.y), stroke.width)
        val circle = min(field.width, field.height) * 0.14f
        drawCircle(PitchLine, circle, field.center, style = stroke)
        drawCircle(PitchLine, 2.dp.toPx(), field.center)
        drawBox(field, top = true, stroke)
        drawBox(field, top = false, stroke)
    }
}

private fun DrawScope.drawBox(field: Rect, top: Boolean, stroke: Stroke) {
    val boxWidth = field.width * 0.44f
    val boxDepth = field.height * 0.11f
    val boxTop = if (top) field.top - stroke.width else field.bottom - boxDepth
    drawRect(
        PitchLine,
        Offset(field.center.x - boxWidth / 2, boxTop),
        Size(boxWidth, boxDepth + stroke.width),
        style = stroke,
    )
    drawCircle(PitchLine, 1.6.dp.toPx(), Offset(field.center.x, if (top) boxDepth * 0.72f else field.bottom - boxDepth * 0.72f))
}
