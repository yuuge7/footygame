package com.example.footygame.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footygame.R
import com.example.footygame.data.ModeRecord
import com.example.footygame.game.Dynasty
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DynastyPhase
import com.example.footygame.models.DynastyState
import com.example.footygame.models.ProPhase
import com.example.footygame.models.ProState
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.HeadlineGradient
import com.example.footygame.theme.Hot
import com.example.footygame.theme.ResultWin
import com.example.footygame.ui.components.ChallengeMark
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.Pill
import com.example.footygame.ui.components.SecondaryButton
import com.example.footygame.ui.components.SquareIconButton
import com.example.footygame.ui.components.nightBackdrop

@Composable
fun MainMenuScreen(
    records: Map<DraftMode, ModeRecord>,
    onModeSelected: (DraftMode) -> Unit,
    onOpenStats: () -> Unit,
    dynasty: DynastyState? = null,
    pro: ProState? = null,
    onDynasty: () -> Unit = {},
    onPro: () -> Unit = {},
) {
    var showHowToPlay by rememberSaveable { mutableStateOf(false) }
    // Play the entrance once per visit to the app, not every time the user comes back to the menu.
    var hasEntered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasEntered = true }

    LazyColumn(
        // The backdrop fills the screen; cards read best at phone width, so on landscape and tablets the column stays centred.
        modifier = Modifier
            .fillMaxSize()
            .nightBackdrop()
            .wrapContentWidth()
            .widthIn(max = 640.dp),
        contentPadding = WindowInsets.safeDrawing
            .add(WindowInsets(left = 20.dp, top = 12.dp, right = 20.dp, bottom = 24.dp))
            .asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                SquareIconButton(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = stringResource(R.string.menu_how_to_play),
                    onClick = { showHowToPlay = true },
                    tint = ChalkMuted,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Pill(stringResource(R.string.menu_kicker))
                    Spacer(Modifier.height(18.dp))
                    Wordmark()
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.menu_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ChalkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp),
                    )
                    Spacer(Modifier.height(30.dp))
                    Eyebrow(stringResource(R.string.menu_choose), Modifier.fillMaxWidth())
                }
            }
        }
        itemsIndexed(DraftMode.entries, key = { _, mode -> mode.name }) { index, mode ->
            val visibility = remember { MutableTransitionState(hasEntered) }.apply { targetState = true }
            AnimatedVisibility(
                visibleState = visibility,
                enter = fadeIn(tween(350, delayMillis = 80 * index)) +
                    slideInVertically(tween(350, delayMillis = 80 * index)) { it / 3 },
            ) {
                ModeCard(mode = mode, record = records[mode] ?: ModeRecord(), onClick = { onModeSelected(mode) })
            }
        }
        item(key = "careers_header") {
            Eyebrow(stringResource(R.string.career_modes), Modifier.padding(top = 12.dp))
        }
        item(key = "career_dynasty") {
            CareerCard(
                kicker = stringResource(R.string.dynasty_kicker),
                title = stringResource(R.string.dynasty_title),
                description = stringResource(R.string.dynasty_card_description),
                status = dynasty?.let { dynastyStatus(it) },
                onClick = onDynasty,
                modifier = Modifier.testTag("career_dynasty"),
            )
        }
        item(key = "career_pro") {
            CareerCard(
                kicker = stringResource(R.string.pro_kicker),
                title = stringResource(R.string.pro_title),
                description = stringResource(R.string.pro_card_description),
                status = pro?.let { proStatus(it) },
                onClick = onPro,
                modifier = Modifier.testTag("career_pro"),
            )
        }
        item(key = "stats") {
            SecondaryButton(
                stringResource(R.string.stats_title),
                onOpenStats,
                Modifier
                    .padding(top = 4.dp)
                    .testTag("open_stats"),
            )
        }
    }

    if (showHowToPlay) HowToPlayDialog(onDismiss = { showHowToPlay = false })
}

/** The title: plain white first word, the second lit in the brand gradient. */
@Composable
private fun Wordmark() {
    val top = stringResource(R.string.menu_title_top)
    val bottom = stringResource(R.string.menu_title_bottom)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Chalk)) { append(top) }
            append("\n")
            withStyle(SpanStyle(brush = Brush.horizontalGradient(HeadlineGradient))) { append(bottom) }
        },
        style = MaterialTheme.typography.displayLarge.copy(textAlign = TextAlign.Center, lineHeight = 66.sp),
    )
}

@Composable
private fun ModeCard(mode: DraftMode, record: ModeRecord, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Dugout.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, ChalkLine),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mode_${mode.name}"),
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ChallengeMark(
                text = mode.challenge,
                style = MaterialTheme.typography.displaySmall,
                // Wide enough for "38-0" so every card's title starts on the same line.
                modifier = Modifier.widthIn(min = 84.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(mode.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Chalk,
                )
                Text(
                    text = stringResource(mode.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChalkMuted,
                )
                Spacer(Modifier.height(4.dp))
                Eyebrow(recordLine(record), color = if (record.runs > 0) Floodlight else ChalkMuted)
            }
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = ChalkMuted,
                modifier = Modifier.rotate(-90f),
            )
        }
    }
}

/** A career mode: green kicker like a contract, the title, and where a saved career stands. */
@Composable
private fun CareerCard(
    kicker: String,
    title: String,
    description: String,
    status: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Dugout.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, if (status != null) ResultWin.copy(alpha = 0.5f) else ChalkLine),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Eyebrow(kicker, color = ResultWin)
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Chalk)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = ChalkMuted)
                if (status != null) {
                    Spacer(Modifier.height(4.dp))
                    Eyebrow(status, color = Floodlight)
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = ChalkMuted,
                modifier = Modifier.rotate(-90f),
            )
        }
    }
}

@Composable
private fun dynastyStatus(state: DynastyState): String = when (state.phase) {
    DynastyPhase.DRAFT -> stringResource(R.string.career_status_draft)
    DynastyPhase.FINISHED -> stringResource(R.string.career_status_finished)
    else -> stringResource(R.string.career_status_season, state.season, Dynasty.SEASONS)
}

@Composable
private fun proStatus(state: ProState): String = when (state.phase) {
    ProPhase.RETIRED -> stringResource(R.string.career_status_finished)
    ProPhase.FIRST_CLUB -> stringResource(R.string.career_status_first_club)
    else -> stringResource(R.string.career_status_pro, state.season, state.club)
}

@Composable
private fun recordLine(record: ModeRecord): String {
    if (record.runs == 0) return stringResource(R.string.record_none)
    val parts = buildList {
        add(stringResource(R.string.record_best, record.bestWins, record.bestDraws, record.bestLosses))
        if (record.perfectRuns > 0) add(pluralStringResource(R.plurals.record_perfect, record.perfectRuns, record.perfectRuns))
        if (record.trophies > 0) add(pluralStringResource(R.plurals.record_trophies, record.trophies, record.trophies))
    }
    return parts.joinToString(" · ")
}

@Composable
private fun HowToPlayDialog(onDismiss: () -> Unit) {
    val steps = listOf(
        R.string.how_to_play_step_1,
        R.string.how_to_play_step_2,
        R.string.how_to_play_step_3,
        R.string.how_to_play_step_4,
        R.string.how_to_play_step_5,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DugoutRaised,
        title = { Text(stringResource(R.string.how_to_play_title), style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Hot,
                            modifier = Modifier.widthIn(min = 16.dp),
                        )
                        Text(stringResource(step), style = MaterialTheme.typography.bodyMedium, color = Chalk)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_got_it), color = Floodlight) }
        },
    )
}
