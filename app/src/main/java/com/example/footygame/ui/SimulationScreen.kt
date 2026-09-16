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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.footygame.R
import com.example.footygame.models.DraftSession
import com.example.footygame.models.EuropeanRun
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
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.FoilGold
import com.example.footygame.theme.Ink
import com.example.footygame.theme.InkMuted
import com.example.footygame.theme.ResultLoss
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.PrimaryButton
import com.example.footygame.ui.components.ResultPill
import com.example.footygame.ui.components.ScreenHeader
import com.example.footygame.ui.components.SecondaryButton
import com.example.footygame.ui.components.StatBlock
import com.example.footygame.ui.components.foilFrame
import com.example.footygame.ui.components.mownStripes
import com.example.footygame.viewmodel.RunState

@Composable
fun SimulationScreen(
    run: RunState,
    session: DraftSession,
    onSkip: () -> Unit,
    onChooseJanuary: (JanuaryEvent) -> Unit,
    onRunItBack: () -> Unit,
    onNewDraft: () -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
) {
    val result = run.result
    val shown = run.matches.take(run.revealed)
    val complete = run.isRevealComplete
    val listState = remember(run.seed) { LazyListState() }
    val names = remember(session, result) {
        (session.picks.values.map { it.player } + listOfNotNull(result?.january?.signed?.player))
            .associate { it.id to it.shortName }
    }

    LaunchedEffect(run.seed, run.revealed, complete) {
        when {
            complete -> listState.scrollToItem(0)
            shown.isNotEmpty() -> listState.animateScrollToItem(shown.lastIndex)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .mownStripes()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ScreenHeader(
            mode = run.mode,
            title = stringResource(R.string.run_progress, shown.size, run.mode.matches),
            onBack = onBack,
        ) {
            // Nothing takes Skip's place once the reveal ends, so a late tap on Skip can't leave the results.
            if (!complete) {
                TextButton(onClick = onSkip, modifier = Modifier.testTag("skip")) { Text(stringResource(R.string.run_skip)) }
            }
        }

        // Pinned while matches tick in; once the run is over it scrolls away with the summary to give results room.
        if (!complete) {
            Scoreboard(shown, run.mode.matches, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
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
                item(key = "summary") { SummaryStats(result) }
                result.europe?.let { europe -> item(key = "europe") { EuropeCard(europe) } }
                if (result.table.isNotEmpty()) item(key = "table") { TablePreview(result.table) }
                if (result.topScorers.isNotEmpty()) item(key = "scorers") { TopScorers(result) }
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
            if (complete && europe != null) {
                item(key = "europe_header") {
                    Eyebrow(stringResource(R.string.europe_fixtures), modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
                }
                itemsIndexed(europe.matches, key = { index, _ -> "europe_$index" }) { _, match ->
                    FixtureRow(match, names)
                }
            }
        }

        if (complete) {
            Surface(color = Dugout, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
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
                        Text(stringResource(R.string.action_menu))
                    }
                }
            }
        } else {
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (run.isAwaitingJanuary) {
        JanuaryDialog(shown, run.januaryOffers, onChooseJanuary)
    }
}

/** The run can't continue until one gamble is chosen, so the dialog can't be dismissed. */
@Composable
private fun JanuaryDialog(played: List<MatchResult>, offers: List<JanuaryEvent>, onChoose: (JanuaryEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.january_title).uppercase(), style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.january_body, played.wins, played.draws, played.losses),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChalkMuted,
                )
                offers.forEach { event ->
                    val shape = MaterialTheme.shapes.medium
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Dugout)
                            .border(1.dp, ChalkLine, shape)
                            .clickable { onChoose(event) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("january_option"),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(stringResource(event.titleRes), style = MaterialTheme.typography.titleMedium, color = Floodlight)
                        Text(stringResource(event.detailRes), style = MaterialTheme.typography.bodySmall, color = Chalk)
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun JanuaryRow(outcome: JanuaryOutcome) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(DugoutRaised)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("january_outcome"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Eyebrow(
            "${stringResource(R.string.january_row_label)} · ${stringResource(outcome.event.titleRes)}",
            color = if (outcome.success) Floodlight else ResultLoss,
        )
        Text(januaryStory(outcome), style = MaterialTheme.typography.bodyMedium, color = Chalk)
    }
}

@Composable
private fun Scoreboard(shown: List<MatchResult>, total: Int, modifier: Modifier = Modifier) {
    val perfect = shown.wins == shown.size
    val description = stringResource(R.string.cd_record, shown.wins, shown.draws, shown.losses)

    Surface(
        color = Dugout,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = description },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ScoreboardNumber(shown.wins, stringResource(R.string.run_wins))
                ScoreboardNumber(shown.draws, stringResource(R.string.run_draws))
                ScoreboardNumber(shown.losses, stringResource(R.string.run_losses))
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else shown.size / total.toFloat() },
                color = Floodlight,
                trackColor = ChalkLine,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (perfect) Floodlight else ResultLoss, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Eyebrow(
                    stringResource(if (perfect) R.string.run_perfect_so_far else R.string.run_perfect_over),
                    color = Chalk,
                )
            }
        }
    }
}

@Composable
private fun ScoreboardNumber(value: Int, label: String) = StatBlock(
    value = value.toString(),
    label = label,
    modifier = Modifier.widthIn(min = 72.dp),
    valueColor = Floodlight,
    valueStyle = MaterialTheme.typography.displayMedium,
    horizontalAlignment = Alignment.CenterHorizontally,
)

@Composable
private fun VerdictCard(result: RunResult, isNewBest: Boolean) {
    val golden = result.isFlawless || result.wonTrophy
    val card = if (golden) {
        Modifier.foilFrame(FoilGold, corner = 22.dp, thickness = 5.dp)
    } else {
        Modifier.background(DugoutRaised, RoundedCornerShape(22.dp))
    }
    val ink = if (golden) Ink else Chalk
    val subtle = if (golden) InkMuted else ChalkMuted

    Column(
        Modifier
            .fillMaxWidth()
            .then(card)
            .padding(20.dp)
            .testTag("verdict"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isNewBest) Eyebrow(stringResource(R.string.new_best), color = if (golden) InkMuted else Floodlight)
        result.manager?.let {
            Eyebrow(stringResource(R.string.verdict_manager, it.name, stringResource(it.trait.titleRes)), color = subtle)
        }
        Text(verdictHeadline(result).uppercase(), style = MaterialTheme.typography.displaySmall, color = ink)
        verdictDetail(result)?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = subtle) }
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

@Composable
private fun EuropeCard(europe: EuropeanRun) {
    val competition = stringResource(europe.competition.titleRes)
    val headline = when (val verdict = europe.verdict) {
        Verdict.Champions -> stringResource(R.string.europe_won, competition)
        is Verdict.Eliminated -> if (verdict.stage.type == StageType.LEAGUE_PHASE) {
            stringResource(R.string.europe_out_league_phase, competition)
        } else {
            stringResource(R.string.europe_knocked_out, competition, stageName(verdict.stage.type))
        }
        is Verdict.LeagueFinish -> competition
    }
    val won = europe.verdict == Verdict.Champions
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .then(
                if (won) {
                    Modifier.foilFrame(FoilGold, corner = 18.dp, thickness = 4.dp)
                } else {
                    Modifier.background(DugoutRaised, RoundedCornerShape(18.dp))
                },
            )
            .padding(16.dp)
            .testTag("europe"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Eyebrow(stringResource(R.string.europe_title), color = if (won) InkMuted else ChalkMuted)
        Text(headline.uppercase(), style = MaterialTheme.typography.headlineSmall, color = if (won) Ink else Chalk)
        Text(
            stringResource(R.string.cd_record, europe.matches.wins, europe.matches.draws, europe.matches.losses),
            style = MaterialTheme.typography.bodyMedium,
            color = if (won) InkMuted else ChalkMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryStats(result: RunResult) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatBlock(result.goalsFor.toString(), stringResource(R.string.summary_goals_for))
        StatBlock(result.goalsAgainst.toString(), stringResource(R.string.summary_goals_against))
        StatBlock(result.cleanSheets.toString(), stringResource(R.string.summary_clean_sheets))
        (result.verdict as? Verdict.LeagueFinish)?.let {
            StatBlock(it.points.toString(), stringResource(R.string.summary_points), valueColor = Floodlight)
        }
    }
}

/** Top four, plus the user's row when they finished outside it. */
@Composable
private fun TablePreview(table: List<TableRow>) {
    Column(Modifier.padding(top = 24.dp)) {
        Eyebrow(stringResource(R.string.summary_table), modifier = Modifier.padding(bottom = 8.dp))
        TableLine(
            position = stringResource(R.string.table_position),
            team = stringResource(R.string.table_team),
            played = stringResource(R.string.table_played),
            goalDifference = stringResource(R.string.table_goal_difference),
            points = stringResource(R.string.table_points),
            color = ChalkMuted,
            bold = false,
        )
        table.withIndex().filter { it.index < 4 || it.value.isUser }.forEach { (index, row) ->
            TableLine(
                position = (index + 1).toString(),
                team = if (row.isUser) stringResource(R.string.your_xi) else row.name,
                played = row.played.toString(),
                goalDifference = row.goalDifference.let { if (it > 0) "+$it" else it.toString() },
                points = row.points.toString(),
                color = if (row.isUser) Floodlight else Chalk,
                bold = row.isUser,
            )
        }
    }
}

@Composable
private fun TableLine(
    position: String,
    team: String,
    played: String,
    goalDifference: String,
    points: String,
    color: Color,
    bold: Boolean,
) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(position, style = style, color = color, modifier = Modifier.width(28.dp))
        Text(team, style = style, color = color, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(played, style = style, color = color, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
        Text(goalDifference, style = style, color = color, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        Text(points, style = style, color = color, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun TopScorers(result: RunResult) {
    Column(Modifier.padding(top = 24.dp)) {
        Eyebrow(stringResource(R.string.summary_top_scorers), modifier = Modifier.padding(bottom = 8.dp))
        result.topScorers.forEach { tally ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tally.player.name, style = MaterialTheme.typography.titleMedium, color = Chalk, modifier = Modifier.weight(1f))
                Text(tally.goals.toString(), style = MaterialTheme.typography.headlineSmall, color = Floodlight)
            }
        }
    }
}

@Composable
private fun FixtureRow(match: MatchResult, names: Map<String, String>) {
    val scorers = match.scorerIds
        .groupingBy { it }
        .eachCount()
        .entries
        .joinToString(", ") { (id, goals) -> names[id].orEmpty() + if (goals > 1) " $goals" else "" }
    val notes = scoreNotes(match)

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Eyebrow("${stageLabel(match.stage)} · ${venueLong(match.venue)}")
                Text(match.opponent.name, style = MaterialTheme.typography.titleMedium, color = Chalk)
                if (scorers.isNotEmpty()) {
                    Text(scorers, style = MaterialTheme.typography.bodySmall, color = ChalkMuted)
                }
                if (notes != null) {
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = ChalkMuted)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${match.goalsFor}-${match.goalsAgainst}",
                style = MaterialTheme.typography.headlineMedium,
                color = Chalk,
            )
            Spacer(Modifier.width(12.dp))
            ResultPill(match.outcome, outcomeLetter(match.outcome))
        }
        HorizontalDivider(color = ChalkLine)
    }
}
