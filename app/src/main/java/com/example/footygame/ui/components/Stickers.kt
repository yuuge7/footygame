package com.example.footygame.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.footygame.R
import com.example.footygame.models.ClubSeason
import com.example.footygame.models.DraftPick
import com.example.footygame.models.Outcome
import com.example.footygame.theme.BodyFamily
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.DisplayFamily
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.FoilHidden
import com.example.footygame.theme.Ink
import com.example.footygame.theme.InkMuted
import com.example.footygame.theme.ResultDraw
import com.example.footygame.theme.ResultLoss
import com.example.footygame.theme.ResultWin
import com.example.footygame.theme.StickerPaper
import com.example.footygame.theme.foilFor

const val STICKER_ASPECT = 1.32f

/**
 * True when the user has turned animations off in system settings. Compose animations already honour
 * that on their own; this is only for timed loops built on delay(), like the spin reel and match reveal.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

private fun readableOn(background: Color): Color = if (background.luminance() > 0.45f) Ink else Chalk

/** Album sticker look: a metallic foil border around cream paper. */
fun Modifier.foilFrame(foil: List<Color>, corner: Dp, thickness: Dp): Modifier = this
    .background(Brush.linearGradient(foil), RoundedCornerShape(corner))
    .padding(thickness)
    .background(StickerPaper, RoundedCornerShape((corner - thickness).coerceAtLeast(0.dp)))

@Composable
fun ClubBadge(clubSeason: ClubSeason, size: Dp, modifier: Modifier = Modifier) {
    val fill = Color(clubSeason.primaryColor)
    val textSize = with(LocalDensity.current) { (size * 0.3f).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .background(fill, CircleShape)
            .border(size * 0.07f, Color(clubSeason.secondaryColor), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = clubSeason.code,
            color = readableOn(fill),
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.Black,
            fontSize = textSize,
            maxLines = 1,
        )
    }
}

/**
 * An album sticker: cream card inside a foil frame whose metal shows the rating tier.
 * [showRating] false is blind mode: a "?" and a matte frame. [animateEntry] plays the "just stuck in" settle.
 */
@Composable
fun PlayerSticker(
    pick: DraftPick,
    slotLabel: String,
    width: Dp,
    modifier: Modifier = Modifier,
    showRating: Boolean = true,
    animateEntry: Boolean = false,
) {
    val settle = remember(pick.player.id) { Animatable(if (animateEntry) 0f else 1f) }
    LaunchedEffect(pick.player.id) {
        settle.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f))
    }
    val density = LocalDensity.current
    val ratingSize = with(density) { (width * 0.3f).toSp() }
    val labelSize = with(density) { (width * 0.12f).toSp() }
    val nameMax = with(density) { (width * 0.145f).toSp() }
    val nameMin = with(density) { (width * 0.09f).toSp() }

    Box(
        modifier = modifier
            .size(width, width * STICKER_ASPECT)
            .graphicsLayer {
                val progress = settle.value
                val scale = 1.35f - 0.35f * progress
                scaleX = scale
                scaleY = scale
                rotationZ = -9f * (1f - progress)
            }
            .shadow(6.dp, RoundedCornerShape(width * 0.1f))
            .foilFrame(
                if (showRating) foilFor(pick.player.rating) else FoilHidden,
                corner = width * 0.1f,
                thickness = width * 0.045f,
            )
            .padding(horizontal = width * 0.07f, vertical = width * 0.05f),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    text = if (showRating) pick.player.rating.toString() else stringResource(R.string.rating_hidden),
                    color = Ink,
                    fontFamily = DisplayFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = ratingSize,
                    lineHeight = ratingSize,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .padding(top = width * 0.04f)
                        .size(width * 0.14f)
                        .background(Color(pick.clubSeason.primaryColor), CircleShape)
                        .border(width * 0.02f, Color(pick.clubSeason.secondaryColor), CircleShape),
                )
            }
            Text(
                text = slotLabel,
                color = InkMuted,
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Bold,
                fontSize = labelSize,
                lineHeight = labelSize,
            )
            Spacer(Modifier.weight(1f))
            BasicText(
                text = pick.player.shortName.uppercase(),
                style = TextStyle(
                    color = Ink,
                    fontFamily = BodyFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = nameMin, maxFontSize = nameMax, stepSize = 0.5.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${pick.clubSeason.code} ${pick.clubSeason.season.takeLast(5)}",
                color = InkMuted,
                fontFamily = BodyFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = labelSize,
                lineHeight = labelSize,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * An unfilled album spot. [highlighted] marks slots someone in the current spin can fill; their border
 * pulses with [glow], which is only read while drawing so the pulse never recomposes the slot.
 */
@Composable
fun EmptySlot(
    label: String,
    width: Dp,
    highlighted: Boolean,
    glow: () -> Float,
    modifier: Modifier = Modifier,
) {
    val labelSize = with(LocalDensity.current) { (width * 0.24f).toSp() }
    val corner = width * 0.1f

    Box(
        modifier = modifier
            .size(width, width * STICKER_ASPECT)
            .drawWithCache {
                val radius = CornerRadius(corner.toPx())
                val stroke = Stroke(
                    width = (if (highlighted) 2.5f else 1.5f).dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx())),
                )
                val fill = Dugout.copy(alpha = if (highlighted) 0.55f else 0.35f)
                onDrawBehind {
                    drawRoundRect(fill, cornerRadius = radius)
                    drawRoundRect(
                        color = if (highlighted) Floodlight.copy(alpha = glow()) else ChalkMuted.copy(alpha = 0.55f),
                        cornerRadius = radius,
                        style = stroke,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (highlighted) Floodlight else ChalkMuted,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = labelSize,
        )
    }
}

@Composable
fun RatingTile(rating: Int, modifier: Modifier = Modifier, hidden: Boolean = false) {
    Box(
        modifier = modifier
            .size(44.dp)
            .foilFrame(if (hidden) FoilHidden else foilFor(rating), corner = 10.dp, thickness = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (hidden) stringResource(R.string.rating_hidden) else rating.toString(),
            color = Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
fun ResultPill(outcome: Outcome, label: String, modifier: Modifier = Modifier) {
    val color = when (outcome) {
        Outcome.WIN -> ResultWin
        Outcome.DRAW -> ResultDraw
        Outcome.LOSS -> ResultLoss
    }
    Box(
        modifier = modifier
            .size(28.dp)
            .background(color, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Ink, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = ChalkMuted) {
    Text(
        text = text.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier,
    )
}

/** Big scoreboard number over a small label. */
@Composable
fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Chalk,
    valueStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier, horizontalAlignment = horizontalAlignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, color = valueColor, style = valueStyle)
        Eyebrow(label)
    }
}
