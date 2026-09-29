package com.example.footygame.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.models.DraftPick
import com.example.footygame.models.Formation
import com.example.footygame.models.Slot
import com.example.footygame.theme.Floodlight

/**
 * A settled XI on the pitch, for the career screens. [onSlotClick] makes every slot tappable (summer
 * signings); empty slots glow then. [highlightId] haloes one player, the player career's own.
 */
@Composable
fun SquadPitch(
    formation: Formation,
    picks: Map<String, DraftPick>,
    modifier: Modifier = Modifier,
    highlightId: String? = null,
    onSlotClick: ((Slot) -> Unit)? = null,
) {
    val glow = rememberInfiniteTransition(label = "squadGlow").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "squadGlowAlpha",
    )
    PitchCard(modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            val perRow = formation.slots.groupingBy { it.row }.eachCount().values.max()
            val gap = 6.dp
            val rowHeight = maxHeight / formation.rows
            val slotWidth = minOf((maxWidth - gap * (perRow + 1)) / perRow, (rowHeight - gap) / TOKEN_ASPECT, 84.dp)
                .coerceAtLeast(36.dp)
            val slotHeight = slotWidth * TOKEN_ASPECT

            formation.slots.forEach { slot ->
                val left = (maxWidth * slot.x - slotWidth / 2).coerceIn(0.dp, maxWidth - slotWidth)
                val top = rowHeight * slot.row + (rowHeight - slotHeight) / 2
                val pick = picks[slot.id]
                val tappable = if (onSlotClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(slotWidth * 0.3f))
                        .clickable { onSlotClick(slot) }
                        .testTag("squad_slot_${slot.id}")
                } else {
                    Modifier
                }
                if (pick != null) {
                    val description = stringResource(R.string.cd_slot_filled, slot.label, pick.player.name, pick.player.rating)
                    val star = pick.player.id == highlightId
                    PlayerToken(
                        pick = pick,
                        width = slotWidth,
                        modifier = Modifier
                            .offset(left, top)
                            .then(if (star) Modifier.starHalo() else Modifier)
                            .then(tappable)
                            .semantics { contentDescription = description },
                    )
                } else {
                    val description = stringResource(R.string.cd_slot_empty, slot.label)
                    EmptySlot(
                        label = slot.label,
                        width = slotWidth,
                        highlighted = onSlotClick != null,
                        glow = glow::value,
                        modifier = Modifier
                            .offset(left, top)
                            .then(tappable)
                            .semantics { contentDescription = description },
                    )
                }
            }
        }
    }
}

/** A gold glow behind the shirt, centred on it. */
private fun Modifier.starHalo(): Modifier = drawWithCache {
    val centre = Offset(size.width / 2, size.height * 0.4f)
    val radius = size.minDimension * 0.62f
    val halo = Brush.radialGradient(listOf(Floodlight.copy(alpha = 0.55f), Color.Transparent), centre, radius)
    onDrawBehind { drawCircle(halo, radius, centre) }
}
