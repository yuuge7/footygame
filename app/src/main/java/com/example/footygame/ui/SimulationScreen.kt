package com.example.footygame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.footygame.R
import com.example.footygame.game.RunHighlights
import com.example.footygame.game.shortNames
import com.example.footygame.models.DraftSession
import com.example.footygame.models.CupRun
import com.example.footygame.models.JanuaryEvent
import com.example.footygame.models.JanuaryOutcome
import com.example.footygame.models.MatchResult
import com.example.footygame.models.RunResult
import com.example.footygame.models.StageType
import com.example.footygame.models.TableRow
import com.example.footygame.models.Verdict
import com.example.footygame.models.draws
import com.example.footygame.models.losses
import com.example.footygame.models.wins
import com.example.footygame.theme.BrandGradient
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.GoldGradient
import com.example.footygame.theme.Ink
import com.example.footygame.theme.ResultLoss
import com.example.footygame.theme.ResultWin
import com.example.footygame.ui.components.ChallengeMark
import com.example.footygame.ui.components.Confetti
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.Pill
import com.example.footygame.ui.components.PrimaryButton
import com.example.footygame.ui.components.RecordNumbers
import com.example.footygame.ui.components.ResultPill
import com.example.footygame.ui.components.ScreenHeader
import com.example.footygame.ui.components.SecondaryButton
import com.example.footygame.ui.components.StatBlock
import com.example.footygame.ui.components.nightBackdrop
import com.example.footygame.ui.components.panel
import com.example.footygame.viewmodel.RunState

@Composable
fun SimulationScreen(
    run: RunState,
    session: DraftSession,
    onSkip: () -> Unit,
    autoPlay: Boolean,
    onNextMatch: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onChooseJanuary: (JanuaryEvent) -> Unit,
    onPlayEurope: () -> Unit,
    onRunItBack: () -> Unit,
    onNewDraft: () -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
) {
    val result = run.result
    val shown = run.matches.take(run.revealed)
    val complete = run.isRevealComplete
    val listState = remember(run.seed) { LazyListState() }
    val names = remember(session, result) { session.shortNames(result) }

    LaunchedEffect(run.seed, run.revealed, complete) {
        when {
            complete -> listState.scrollToItem(0)
            shown.isNotEmpty() -> listState.animateScrollToItem(shown.lastIndex)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .nightBackdrop()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            ScreenHeader(
                mode = run.mode,
                title = stringResource(R.string.run_progress, shown.size, run.mode.matches),
                onBack = onBack,
            ) {
                // Nothing takes Skip's place once the reveal ends, so a late tap on Skip can't leave the results.
                if (!complete) {
                    TextButton(onClick = onSkip, modifier = Modifier.testTag("skip")) {
                        Text(stringResource(R.string.run_skip), color = Floodlight)
                    }
                }
            }

            // Pinned while matches tick in; once the run is over it scrolls away with the summary to give results room.
            if (!complete) {
                Scoreboard(shown, run.mode.matches, Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
                LatestMatchCard(
                    shown = shown,
                    upcoming = run.matches.getOrNull(shown.size),
                    names = names,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (complete && result != null) {
                    item(key = "scoreboard") { Scoreboard(shown, run.mode.matches, Modifier.padding(bottom = 16.dp)) }
                    item(key = "verdict") { VerdictCard(result, run.isNewBest) }
                    // The season's stats come first; Europe is its own screen, invited after them until played.
                    seasonStats(result)
                    result.europe?.let { europe ->
                        item(key = "europe") {
                            if (run.europeSeen) {
                                CupCard(
                                    europe,
                                    Modifier
                                        .padding(top = 24.dp)
                                        .testTag("europe"),
                                )
                            } else {
                                CupInvite(europe.competition, onPlayEurope, Modifier.padding(top = 24.dp))
                            }
                        }
                    }
                    item(key = "fixtures_header") {
                        Eyebrow(stringResource(R.string.summary_fixtures), modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
                    }
                }
                val january = result?.january
                itemsIndexed(shown, key = { index, _ -> "match_$index" }) { index, match ->
                    Column {
                        if (january != null && index == january.afterMatches) JanuaryRow(january)
                        FixtureRow(match, names)
                    }
                }
                val europe = result?.europe
                if (complete && europe != null && run.europeSeen) {
                    item(key = "europe_header") {
                        Eyebrow(stringResource(R.string.europe_fixtures), modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
                    }
                    itemsIndexed(europe.matches, key = { index, _ -> "europe_$index" }) { _, match ->
                        FixtureRow(match, names)
                    }
                }
            }

            Surface(color = Dugout, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                if (complete) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton(
                                stringResource(R.string.action_run_it_back),
                                onRunItBack,
                                Modifier
                                    .weight(1f)
                                    .testTag("run_it_back"),
                            )
                            SecondaryButton(
                                stringResource(R.string.action_new_draft),
                                onNewDraft,
                                Modifier
                                    .weight(1f)
                                    .testTag("new_draft"),
                            )
                        }
                        TextButton(onClick = onMenu, modifier = Modifier.testTag("menu")) {
                            Text(stringResource(R.string.action_menu), color = ChalkMuted)
                        }
                    }
                } else {
                    RevealControls(
                        started = shown.isNotEmpty(),
                        canPlay = run.revealed < run.matches.size,
                        autoPlay = autoPlay,
                        onNext = onNextMatch,
                        onToggleAutoPlay = onToggleAutoPlay,
                    )
                }
            }
        }
        // Perfect runs and trophies rain confetti over the results, like the final whistle of a title win.
        if (complete && result != null && (result.isFlawless || result.wonTrophy)) {
            Confetti(seed = run.seed, modifier = Modifier.fillMaxSize())
        }
    }

    if (run.isAwaitingJanuary) {
        JanuaryDialog(shown, run.januaryOffers, onChooseJanuary)
    }
}

/** The result. A flawless run gets the full scoreboard treatment: the record huge in pink and gold. */
@Composable
fun VerdictCard(result: RunResult, isNewBest: Boolean) {
    val golden = result.isFlawless || result.wonTrophy

    Column(
        Modifier
            .fillMaxWidth()
            .panel(
                RoundedCornerShape(22.dp),
                color = if (golden) DugoutRaised else Dugout,
                edge = if (golden) Floodlight.copy(alpha = 0.55f) else ChalkLine,
            )
            .padding(horizontal = 20.dp, vertical = 22.dp)
            .testTag("verdict"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isNewBest) Pill(stringResource(R.string.new_best))
        result.manager?.let {
            Eyebrow(stringResource(R.string.verdict_manager, it.name, stringResource(it.trait.titleRes)))
        }
        if (result.isFlawless) {
            ChallengeMark(
                "${result.matches.wins}-${result.matches.draws}-${result.matches.losses}",
                MaterialTheme.typography.displayLarge,
            )
        }
        Text(
            verdictHeadline(result),
            style = if (result.isFlawless) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
            color = Chalk,
            textAlign = TextAlign.Center,
        )
        verdictDetail(result)?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = ChalkMuted, textAlign = TextAlign.Center)
        }
        if (golden) {
            Text(
                stringResource(if (result.isFlawless) R.string.verdict_badge_perfect else R.string.verdict_badge_trophy),
                style = MaterialTheme.typography.labelLarge,
                color = Ink,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(GoldGradient))
                    .padding(horizontal = 26.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun verdictHeadline(result: RunResult): String {
    if (result.isFlawless) return stringResource(R.string.verdict_perfect, result.mode.challenge)
    return when (val verdict = result.verdict) {
        is Verdict.LeagueFinish -> if (verdict.position == 1) {
            stringResource(R.string.verdict_league_champions, verdict.points)
        } else {
            stringResource(R.string.verdict_league_finish, ordinal(verdict.position), verdict.points)
        }

        Verdict.Champions -> stringResource(R.string.verdict_champions)
        is Verdict.Eliminated -> eliminationText(verdict)
    }
}

@Composable
private fun eliminationText(verdict: Verdict.Eliminated): String = when (verdict.stage.type) {
    StageType.LEAGUE_PHASE -> stringResource(R.string.verdict_out_in, stringResource(R.string.stage_league_phase_short))
    StageType.GROUP_STAGE -> stringResource(R.string.verdict_out_in, stringResource(R.string.stage_group_short))
    else -> stringResource(R.string.verdict_knocked_out, stageName(verdict.stage.type, verdict.stage.number))
}

@Composable
private fun verdictDetail(result: RunResult): String? {
    if (result.isFlawless) return stringResource(R.string.verdict_perfect_detail)
    val beatenBy = (result.verdict as? Verdict.Eliminated)?.by?.let { stringResource(R.string.verdict_beaten_by, it.name) }
    val dropped = result.firstDropped?.let {
        stringResource(R.string.verdict_first_dropped, stageLabel(it.stage), "${it.goalsFor}-${it.goalsAgainst}", it.opponent.name)
    }
    return listOfNotNull(beatenBy, dropped).joinToString(" ").ifEmpty { null }
}
