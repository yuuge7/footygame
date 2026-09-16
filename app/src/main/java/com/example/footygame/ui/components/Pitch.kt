package com.example.footygame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.Pitch
import com.example.footygame.theme.PitchStripe
import kotlin.math.min

private val StripeHeight = 64.dp

/** Mown grass: alternating horizontal bands across the whole element. */
fun Modifier.mownStripes(): Modifier = drawBehind {
    drawRect(Pitch)
    val band = StripeHeight.toPx()
    var top = band
    while (top < size.height) {
        drawRect(PitchStripe, topLeft = Offset(0f, top), size = Size(size.width, band))
        top += band * 2
    }
}

/** Chalk markings of a full pitch, attacking end at the top. */
@Composable
fun PitchMarkings(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 1.5.dp.toPx())
        val inset = 6.dp.toPx()
        val field = Rect(inset, inset, size.width - inset, size.height - inset)
        drawRect(ChalkLine, field.topLeft, field.size, style = stroke)
        drawLine(ChalkLine, Offset(field.left, field.center.y), Offset(field.right, field.center.y), stroke.width)
        val circle = min(field.width, field.height) * 0.14f
        drawCircle(ChalkLine, circle, field.center, style = stroke)
        drawCircle(ChalkLine, 2.dp.toPx(), field.center)
        drawBox(field, top = true, stroke)
        drawBox(field, top = false, stroke)
    }
}

private fun DrawScope.drawBox(field: Rect, top: Boolean, stroke: Stroke) {
    val boxWidth = field.width * 0.56f
    val boxDepth = field.height * 0.13f
    val goalWidth = field.width * 0.26f
    val goalDepth = field.height * 0.045f
    val boxTop = if (top) field.top else field.bottom - boxDepth
    val goalTop = if (top) field.top else field.bottom - goalDepth
    drawRect(ChalkLine, Offset(field.center.x - boxWidth / 2, boxTop), Size(boxWidth, boxDepth), style = stroke)
    drawRect(ChalkLine, Offset(field.center.x - goalWidth / 2, goalTop), Size(goalWidth, goalDepth), style = stroke)
    val arcRadius = min(field.width, field.height) * 0.1f
    val arcCentreY = if (top) boxTop + boxDepth - arcRadius * 0.4f else boxTop + arcRadius * 0.4f
    drawArc(
        color = ChalkLine,
        startAngle = if (top) 25f else 205f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(field.center.x - arcRadius, arcCentreY - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = stroke,
    )
}
