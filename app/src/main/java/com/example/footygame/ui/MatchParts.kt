package com.example.footygame.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.footygame.theme.ResultLoss
import com.example.footygame.theme.ResultWin
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.PrimaryButton
import com.example.footygame.ui.components.SecondaryButton
import com.example.footygame.ui.components.RecordNumbers
import com.example.footygame.ui.components.ResultPill
import com.example.footygame.ui.components.StatBlock
import com.example.footygame.ui.components.panel
import com.example.footygame.ui.components.rememberReducedMotion
import com.example.footygame.viewmodel.AutoPlay
import kotlinx.coroutines.delay

// Pieces every results screen shares: the challenges' season screen, European nights and the career modes.

/**
 * How many of [total] matches are on screen: one more per tap on "Next match", or one every
 * [AutoPlay.STEP_MS] with auto play on. Keyed by [key] so a new competition starts from zero, and
 * saved so it survives recreation. With system animations off everything shows at once.
 */
@Composable
fun rememberReveal(key: Any, total: Int): Reveal {
    val reducedMotion = rememberReducedMotion()
    val state = rememberSaveable(key) { mutableIntStateOf(if (reducedMotion) total else 0) }
    val autoState = AutoPlay.on.collectAsState()
    val auto by autoState
    LaunchedEffect(key, total, auto) {
        // The league grows after the January window, so a reveal without animations catches up to it too.
        if (reducedMotion) state.intValue = total
        while (auto && state.intValue < total) {
            delay(AutoPlay.STEP_MS)
            state.intValue++
        }
    }
    return remember(state, autoState, total) { Reveal(state, autoState, total) }
}

class Reveal(private val state: MutableIntState, private val auto: State<Boolean>, private val total: Int) {
    val shown: Int get() = state.intValue.coerceAtMost(total)
    val isComplete: Boolean get() = state.intValue >= total
    val autoPlay: Boolean get() = auto.value

    fun next() {
        if (state.intValue < total) state.intValue++
    }

    fun toggleAutoPlay() = AutoPlay.toggle()

    fun skip() {
        state.intValue = total
    }
}

/**
 * The match centre while results come in: the latest result large, with the fixture after it. Before the
 * first match it shows who's up first.
 */
@Composable
fun LatestMatchCard(
    shown: List<MatchResult>,
    upcoming: MatchResult?,
    names: Map<String, String>,
    modifier: Modifier = Modifier,
    highlightId: String? = null,
) {
    val last = shown.lastOrNull()
    if (last == null && upcoming == null) return
    Column(
        modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(18.dp), color = DugoutRaised)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("latest_match"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AnimatedContent(targetState = shown.size, label = "latest_match") { count ->
            val match = shown.getOrNull(count - 1)
            if (match == null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Eyebrow(stringResource(R.string.match_up_first), color = Floodlight)
                    upcoming?.let { FixtureHeadline(it) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Eyebrow("${stageLabel(match.stage)} · ${venueLong(match.venue)}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            match.opponent.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Chalk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("${match.goalsFor}-${match.goalsAgainst}", style = MaterialTheme.typography.displaySmall, color = Chalk)
                        Spacer(Modifier.width(10.dp))
                        ResultPill(match.outcome, outcomeLetter(match.outcome))
                    }
                    val scorers = scorerLine(match, names)
                    if (scorers.isNotEmpty()) {
                        val starred = highlightId != null && highlightId in match.scorerIds
                        Text(
                            scorers,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (starred) Floodlight else ChalkMuted,
                            fontWeight = if (starred) FontWeight.Bold else null,
                        )
                    }
                    scoreNotes(match)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ChalkMuted) }
                }
            }
        }
        if (last != null && upcoming != null) {
            HorizontalDivider(color = ChalkLine, modifier = Modifier.padding(vertical = 4.dp))
            Text(
                stringResource(R.string.match_next, upcoming.opponent.name, stageLabel(upcoming.stage), venueLong(upcoming.venue)),
                style = MaterialTheme.typography.bodySmall,
                color = ChalkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FixtureHeadline(match: MatchResult) {
    Text(match.opponent.name, style = MaterialTheme.typography.headlineSmall, color = Chalk)
    Text("${stageLabel(match.stage)} · ${venueLong(match.venue)}", style = MaterialTheme.typography.bodySmall, color = ChalkMuted)
}

/**
 * The buttons under results that are still coming in: the next match, or auto play. [canPlay] is false while
 * the season waits on the January window.
 */
@Composable
fun RevealControls(
    started: Boolean,
    canPlay: Boolean,
    autoPlay: Boolean,
    onNext: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PrimaryButton(
            stringResource(if (started) R.string.match_next_button else R.string.match_kick_off),
            onNext,
            Modifier
                .weight(1.6f)
                .testTag("next_match"),
            enabled = canPlay && !autoPlay,
        )
        SecondaryButton(
            stringResource(if (autoPlay) R.string.match_pause else R.string.match_auto),
            onToggleAutoPlay,
            Modifier
                .weight(1f)
                .testTag("auto_play"),
            enabled = canPlay,
        )
    }
}

/** The run can't continue until one gamble is chosen, so the dialog can't be dismissed. */
@Composable
fun JanuaryDialog(played: List<MatchResult>, offers: List<JanuaryEvent>, onChoose: (JanuaryEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = DugoutRaised,
        title = { Text(stringResource(R.string.january_title), style = MaterialTheme.typography.headlineMedium) },
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
fun JanuaryRow(outcome: JanuaryOutcome) {
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

/** W-D-L so far. [total] draws a progress bar for a fixed-length competition; cups leave it null so it can't give away how far they go. */
@Composable
fun Scoreboard(shown: List<MatchResult>, total: Int?, modifier: Modifier = Modifier) {
    val perfect = shown.wins == shown.size

    Column(
        modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Eyebrow(stringResource(R.string.run_record))
        RecordNumbers(
            shown.wins,
            shown.draws,
            shown.losses,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        if (total != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(ChalkLine),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(if (total == 0) 0f else shown.size / total.toFloat())
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(BrandGradient)),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (perfect) ResultWin else ResultLoss, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Eyebrow(
                stringResource(if (perfect) R.string.run_perfect_so_far else R.string.run_perfect_over),
                color = Chalk,
            )
        }
    }
}

/** How a cup run ended, gold-edged when it was won. */
@Composable
fun CupCard(run: CupRun, modifier: Modifier = Modifier) {
    val competition = stringResource(run.competition.titleRes)
    val headline = when (val verdict = run.verdict) {
        Verdict.Champions -> stringResource(R.string.europe_won, competition)
        is Verdict.Eliminated -> if (verdict.stage.type == StageType.LEAGUE_PHASE) {
            stringResource(R.string.europe_out_league_phase, competition)
        } else {
            stringResource(R.string.europe_knocked_out, competition, stageName(verdict.stage.type))
        }
        is Verdict.LeagueFinish -> competition
    }
    val won = run.won
    Column(
        modifier
            .fillMaxWidth()
            .panel(
                RoundedCornerShape(18.dp),
                color = DugoutRaised,
                edge = if (won) Floodlight.copy(alpha = 0.55f) else ChalkLine,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Eyebrow(
            if (run.competition.isEuropean) stringResource(R.string.europe_title) else competition,
            color = if (won) Floodlight else ChalkMuted,
        )
        Text(headline, style = MaterialTheme.typography.headlineSmall, color = Chalk)
        RecordNumbers(
            run.matches.wins,
            run.matches.draws,
            run.matches.losses,
            style = MaterialTheme.typography.headlineMedium,
            gap = 8.dp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryStats(result: RunResult) {
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

/** Stand-out numbers of the main competition; cups have their own card. */
@Composable
fun Highlights(result: RunResult) {
    val highlights = remember(result) { RunHighlights.of(result.matches) }
    Column(
        Modifier
            .padding(top = 24.dp)
            .testTag("highlights"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Eyebrow(stringResource(R.string.summary_highlights), modifier = Modifier.padding(bottom = 2.dp))
        highlights.biggestWin?.let {
            LabelledValue(
                stringResource(R.string.highlight_biggest_win),
                stringResource(R.string.highlight_score_against, it.goalsFor, it.goalsAgainst, it.opponent.name),
            )
        }
        highlights.heaviestDefeat?.let {
            LabelledValue(
                stringResource(R.string.highlight_heaviest_defeat),
                stringResource(R.string.highlight_score_against, it.goalsFor, it.goalsAgainst, it.opponent.name),
            )
        }
        LabelledValue(
            stringResource(R.string.highlight_win_streak),
            pluralStringResource(R.plurals.highlight_matches, highlights.longestWinStreak, highlights.longestWinStreak),
        )
        LabelledValue(
            stringResource(R.string.highlight_unbeaten),
            pluralStringResource(R.plurals.highlight_matches, highlights.longestUnbeatenRun, highlights.longestUnbeatenRun),
        )
        LabelledValue(
            stringResource(R.string.highlight_failed_to_score),
            pluralStringResource(R.plurals.highlight_matches, highlights.failedToScore, highlights.failedToScore),
        )
    }
}

/**
 * Top four, plus the user's row when they finished outside it, with a button for the whole table.
 * [userName] replaces "Your XI", e.g. a career's club.
 */
@Composable
fun TablePreview(table: List<TableRow>, userName: String? = null) {
    var full by rememberSaveable { mutableStateOf(false) }
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
        table.withIndex().filter { full || it.index < 4 || it.value.isUser }.forEach { (index, row) ->
            TableLine(
                position = (index + 1).toString(),
                team = if (row.isUser) userName ?: stringResource(R.string.your_xi) else row.name,
                played = row.played.toString(),
                goalDifference = row.goalDifference.let { if (it > 0) "+$it" else it.toString() },
                points = row.points.toString(),
                color = if (row.isUser) Floodlight else Chalk,
                bold = row.isUser,
            )
        }
        if (table.size > 5) {
            TextButton(onClick = { full = !full }, modifier = Modifier.testTag("full_table")) {
                Text(stringResource(if (full) R.string.table_show_less else R.string.table_show_all), color = Floodlight)
            }
        }
    }
}

@Composable
fun TableLine(
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
fun TopScorers(result: RunResult) {
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

/** "Shearer 2, Cole": each scorer once, with a count for more than one goal. */
private fun scorerLine(match: MatchResult, names: Map<String, String>): String = match.scorerIds
    .groupingBy { it }
    .eachCount()
    .entries
    .joinToString(", ") { (id, goals) -> names[id].orEmpty() + if (goals > 1) " $goals" else "" }

/** One result. Scorers read from [names]; a goal by [highlightId] (the player career's player) lights up the line. */
@Composable
fun FixtureRow(match: MatchResult, names: Map<String, String>, highlightId: String? = null) {
    val scorers = scorerLine(match, names)
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
                    val starred = highlightId != null && highlightId in match.scorerIds
                    Text(
                        scorers,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (starred) Floodlight else ChalkMuted,
                        fontWeight = if (starred) FontWeight.Bold else null,
                    )
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
