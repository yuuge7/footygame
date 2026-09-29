package com.example.footygame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.models.DraftMode
import com.example.footygame.theme.BrandGradient
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.Hot
import com.example.footygame.theme.ResultDraw
import com.example.footygame.theme.ResultLoss
import com.example.footygame.theme.ResultWin
import com.example.footygame.ui.titleRes

private val ButtonHeight = 52.dp

/** Dark card over the night backdrop, with a hairline edge. */
fun Modifier.panel(shape: Shape = RoundedCornerShape(16.dp), color: Color = Dugout, edge: Color = ChalkLine): Modifier =
    this
        .clip(shape)
        .background(color.copy(alpha = 0.92f))
        .border(1.dp, edge, shape)

/** Back button, the challenge as an eyebrow, the screen's title, and an optional action on the right. */
@Composable
fun ScreenHeader(
    mode: DraftMode,
    title: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) = ScreenHeader("${mode.challenge} · ${stringResource(mode.titleRes)}", title, onBack, trailing)

/** Same header for screens that belong to no single challenge. */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareIconButton(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_back), onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Eyebrow(eyebrow)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

/** Small rounded-square button for the top bars. */
@Composable
fun SquareIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Chalk,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(DugoutRaised)
            .border(1.dp, ChalkLine, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Pink-to-violet pill with a soft glow: the one action a screen is asking for. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: List<Color> = BrandGradient,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(ButtonHeight)
            .then(if (enabled) Modifier.shadow(14.dp, shape, ambientColor = colors.first(), spotColor = colors.last()) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(Brush.horizontalGradient(colors))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Chalk)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(ButtonHeight)
            .alpha(if (enabled) 1f else 0.45f)
            .panel(shape, DugoutRaised, Chalk.copy(alpha = 0.16f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Chalk)
    }
}

/** Small outlined capsule for section kickers and states, gold by default. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Floodlight) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/** A challenge or record like "38-0": the first number in hot pink, the rest in gold. */
@Composable
fun ChallengeMark(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val split = text.indexOf('-').takeIf { it > 0 } ?: text.length
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Hot)) { append(text.substring(0, split)) }
            withStyle(SpanStyle(color = Floodlight)) { append(text.substring(split)) }
        },
        style = style,
        maxLines = 1,
        modifier = modifier,
    )
}

/** Won, drawn and lost as big green, grey and red numbers, like a results board. */
@Composable
fun RecordNumbers(
    wins: Int,
    draws: Int,
    losses: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    gap: Dp = 14.dp,
) {
    val description = stringResource(R.string.cd_record, wins, draws, losses)
    Row(
        modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        Text(wins.toString(), style = style, color = ResultWin)
        Text("-", style = style, color = ChalkMuted.copy(alpha = 0.4f))
        Text(draws.toString(), style = style, color = ResultDraw)
        Text("-", style = style, color = ChalkMuted.copy(alpha = 0.4f))
        Text(losses.toString(), style = style, color = ResultLoss)
    }
}

/** A compact stat in its own tile: value over a small label. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = Chalk) {
    Column(
        modifier
            .panel(RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor, maxLines = 1)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = ChalkMuted)
    }
}
