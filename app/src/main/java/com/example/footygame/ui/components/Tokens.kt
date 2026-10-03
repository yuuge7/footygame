package com.example.footygame.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footygame.R
import com.example.footygame.models.ClubSeason
import com.example.footygame.models.DraftPick
import com.example.footygame.models.Outcome
import com.example.footygame.theme.BodyFamily
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.DisplayFamily
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.Ink
import com.example.footygame.theme.PitchSlot
import com.example.footygame.theme.ResultDraw
import com.example.footygame.theme.ResultLoss
import com.example.footygame.theme.ResultWin
import kotlin.math.abs

/** Height of a pitch token (jersey, badge and name) as a multiple of its width. */
const val TOKEN_ASPECT = 0.95f

private const val JERSEY_SHARE = 0.62f
private const val JERSEY_ASPECT = 0.92f
private const val JERSEY_TOP = 0.08f

private val RatingSilver = Color(0xFFD9DDE6)
private val RatingBronze = Color(0xFFE39B5C)
private val RatingHidden = Color(0xFF55556B)

/** Surname particles that stay off the shirt, so "van Dijk" wears a D. Capitalised ones ("De Gea") stay. */
private val Particles = setOf("van", "von", "de", "der", "den", "da", "das", "di", "do", "dos", "del", "della", "le", "la", "el", "al", "du", "ter", "ten")

/**
 * True when the user has turned animations off in system settings. Compose animations already honour
 * that on their own; this is only for motion timed by hand, like the spin reel and the confetti. The match
 * reveal is not one of them: it moves at the player's taps, so it stays match by match.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

private fun readableOn(background: Color): Color = if (background.luminance() > 0.45f) Ink else Chalk

/** Badge tier: gold 90+, silver 85+, bronze below. */
fun ratingColor(rating: Int): Color = when {
    rating >= 90 -> Floodlight
    rating >= 85 -> RatingSilver
    else -> RatingBronze
}

/**
 * Letters printed on a shirt: one initial per name word, or the start of a short all-caps nickname ("KDB" -> "KD").
 * Abbreviated first names are dropped, so "D. Silva" wears an S.
 */
fun kitInitials(name: String): String {
    val words = name.split(' ', '-').filter { it.isNotBlank() }
    val kept = words.filterNot { it in Particles || it.endsWith('.') }.ifEmpty { words }
    val only = kept.singleOrNull() ?: return kept.take(2).joinToString("") { it.take(1).uppercase() }
    return if (only.length in 2..4 && only.all { it.isUpperCase() }) only.take(2) else only.take(1).uppercase()
}

/** Shirt lettering in the club's second colour, unless it would vanish into the shirt. */
private fun kitLettering(primary: Color, secondary: Color): Color =
    if (abs(primary.luminance() - secondary.luminance()) > 0.28f) secondary else readableOn(primary)

/** A flat football shirt in club colours, [initials] on the chest. */
@Composable
fun Jersey(primary: Color, secondary: Color, initials: String, width: Dp, modifier: Modifier = Modifier) {
    val letterSize = with(LocalDensity.current) { (width * if (initials.length > 1) 0.3f else 0.4f).toSp() }
    Box(
        modifier
            .size(width, width * JERSEY_ASPECT)
            .drawWithCache {
                val shirt = jerseyPath(size)
                val round = Stroke(width = size.width * 0.07f, join = StrokeJoin.Round)
                val collar = Path().apply {
                    moveTo(size.width * 0.36f, size.height * 0.07f)
                    quadraticTo(size.width * 0.5f, size.height * 0.22f, size.width * 0.64f, size.height * 0.07f)
                }
                val collarStroke = Stroke(width = size.width * 0.06f, cap = StrokeCap.Round)
                val sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), endY = size.height * 0.7f)
                val shade = Color.Black.copy(alpha = 0.28f)
                onDrawBehind {
                    translate(top = size.height * 0.05f) {
                        drawPath(shirt, shade)
                        drawPath(shirt, shade, style = round)
                    }
                    drawPath(shirt, primary)
                    drawPath(shirt, primary, style = round)
                    drawPath(shirt, sheen)
                    drawPath(collar, secondary, style = collarStroke)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = kitLettering(primary, secondary),
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = letterSize,
            lineHeight = letterSize,
            letterSpacing = (-0.5).sp,
            maxLines = 1,
            modifier = Modifier.offset(y = width * 0.06f),
        )
    }
}

private fun jerseyPath(size: Size): Path {
    val w = size.width
    val h = size.height
    return Path().apply {
        moveTo(w * 0.36f, h * 0.07f)
        quadraticTo(w * 0.5f, h * 0.2f, w * 0.64f, h * 0.07f)
        lineTo(w * 0.8f, h * 0.12f)
        lineTo(w * 0.96f, h * 0.36f)
        lineTo(w * 0.83f, h * 0.47f)
        lineTo(w * 0.76f, h * 0.4f)
        lineTo(w * 0.76f, h * 0.94f)
        lineTo(w * 0.24f, h * 0.94f)
        lineTo(w * 0.24f, h * 0.4f)
        lineTo(w * 0.17f, h * 0.47f)
        lineTo(w * 0.04f, h * 0.36f)
        lineTo(w * 0.2f, h * 0.12f)
        close()
    }
}

/** Round rating badge; [hidden] is blind mode's "?". */
@Composable
fun RatingBadge(rating: Int, size: Dp, modifier: Modifier = Modifier, hidden: Boolean = false) {
    val textSize = with(LocalDensity.current) { (size * 0.46f).toSp() }
    Box(
        modifier
            .size(size)
            .shadow(3.dp, CircleShape)
            .background(if (hidden) RatingHidden else ratingColor(rating), CircleShape)
            .border(size * 0.06f, Color.Black.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (hidden) stringResource(R.string.rating_hidden) else rating.toString(),
            color = if (hidden) Chalk else Ink,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = textSize,
            lineHeight = textSize,
            letterSpacing = (-0.3).sp,
        )
    }
}

/** The squad's shirt; [showCode] prints the club code on it, which only reads at larger sizes. */
@Composable
fun ClubBadge(clubSeason: ClubSeason, size: Dp, modifier: Modifier = Modifier, showCode: Boolean = true) {
    Jersey(
        primary = Color(clubSeason.primaryColor),
        secondary = Color(clubSeason.secondaryColor),
        initials = if (showCode) clubSeason.code else "",
        width = size,
        modifier = modifier,
    )
}

/**
 * A drafted player on the pitch: shirt in their squad's colours, rating badge on the shoulder, name below.
 * [showRating] false is blind mode. [animateEntry] plays the "just pulled on the shirt" pop.
 */
@Composable
fun PlayerToken(
    pick: DraftPick,
    width: Dp,
    modifier: Modifier = Modifier,
    showRating: Boolean = true,
    animateEntry: Boolean = false,
) {
    val settle = remember(pick.player.id) { Animatable(if (animateEntry) 0f else 1f) }
    LaunchedEffect(pick.player.id) {
        settle.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
    }
    val density = LocalDensity.current
    val nameMax = with(density) { (width * 0.16f).toSp() }
    val nameMin = with(density) { (width * 0.1f).toSp() }
    val jerseyWidth = width * JERSEY_SHARE
    val badge = width * 0.32f

    Box(
        modifier
            .size(width, width * TOKEN_ASPECT)
            .graphicsLayer {
                val progress = settle.value
                val scale = 1.4f - 0.4f * progress
                scaleX = scale
                scaleY = scale
                alpha = progress.coerceIn(0f, 1f)
            },
    ) {
        Jersey(
            primary = Color(pick.clubSeason.primaryColor),
            secondary = Color(pick.clubSeason.secondaryColor),
            initials = kitInitials(pick.player.shortName),
            width = jerseyWidth,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = width * JERSEY_TOP),
        )
        RatingBadge(
            rating = pick.player.rating,
            size = badge,
            hidden = !showRating,
            modifier = Modifier.offset(x = (width + jerseyWidth) / 2 - badge * 0.7f, y = 0.dp),
        )
        BasicText(
            text = pick.player.shortName,
            style = TextStyle(
                color = Chalk,
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 6f),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = nameMin, maxFontSize = nameMax, stepSize = 0.5.sp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

/**
 * An open position: a ring on the grass. [highlighted] marks slots someone in the current spin can fill;
 * their ring pulses with [glow], which is only read while drawing so the pulse never recomposes the slot.
 */
@Composable
fun EmptySlot(
    label: String,
    width: Dp,
    highlighted: Boolean,
    glow: () -> Float,
    modifier: Modifier = Modifier,
) {
    val ring = width * 0.5f
    val labelSize = with(LocalDensity.current) { (width * 0.14f).toSp() }
    val centreY = width * JERSEY_TOP + width * JERSEY_SHARE * JERSEY_ASPECT / 2

    Box(modifier.size(width, width * TOKEN_ASPECT)) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = centreY - ring / 2)
                .size(ring)
                .drawWithCache {
                    val radius = size.minDimension / 2
                    val stroke = Stroke(width = (if (highlighted) 2.2f else 1.2f).dp.toPx())
                    val halo = Brush.radialGradient(
                        listOf(Floodlight.copy(alpha = 0.45f), Color.Transparent),
                        radius = radius * 1.7f,
                    )
                    onDrawBehind {
                        if (highlighted) drawCircle(halo, radius * 1.7f, alpha = glow())
                        drawCircle(if (highlighted) Floodlight.copy(alpha = 0.16f) else PitchSlot, radius)
                        drawCircle(
                            color = if (highlighted) Floodlight.copy(alpha = glow()) else Color.White.copy(alpha = 0.55f),
                            radius = radius - stroke.width / 2,
                            style = stroke,
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (highlighted) Floodlight else Chalk,
                fontFamily = BodyFamily,
                fontWeight = FontWeight.Bold,
                fontSize = labelSize,
                lineHeight = labelSize,
                maxLines = 1,
            )
        }
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
            .background(color, RoundedCornerShape(8.dp)),
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
