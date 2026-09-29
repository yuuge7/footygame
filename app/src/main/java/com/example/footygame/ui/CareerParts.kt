package com.example.footygame.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.models.Award
import com.example.footygame.models.CupFinish
import com.example.footygame.models.JanuaryEvent
import com.example.footygame.models.Legacy
import com.example.footygame.models.LegacyKind
import com.example.footygame.models.SeasonPhase
import com.example.footygame.models.StageType
import com.example.footygame.models.Trophy
import com.example.footygame.models.Verdict
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.Hot
import com.example.footygame.theme.ResultLoss
import com.example.footygame.theme.ResultWin
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.Pill
import com.example.footygame.ui.components.PrimaryButton
import com.example.footygame.ui.components.panel
import com.example.footygame.viewmodel.CareerViewModel
import com.example.footygame.viewmodel.SeasonView

/** Green, like a contract being signed: the start button of a career. */
val CareerStartGradient = listOf(ResultWin, Color(0xFF15803D))

/**
 * One step of a career season on its own screen: the league (with its January window), the FA Cup, or
 * European nights. [phase] is fixed per screen, so moving the career on doesn't change what an exiting
 * screen shows. [onContinue] moves the career to the next step.
 */
@Composable
fun CareerSeasonScreen(
    eyebrow: String,
    phase: SeasonPhase,
    season: SeasonView,
    seed: Long,
    names: Map<String, String>,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    highlightId: String? = null,
    leagueDetail: String? = null,
    /** The club in the table's user row, for the player career; the dynasty keeps "Your XI". */
    teamName: String? = null,
    onChooseJanuary: (JanuaryEvent) -> Unit = {},
) {
    val result = season.result
    val cup = when (phase) {
        SeasonPhase.CUP -> result?.cup
        SeasonPhase.EUROPE -> result?.europe
        else -> null
    }
    val matches = cup?.matches ?: season.league
    val reveal = rememberReveal("${phase.name}_$seed", matches.size, stepMillis = if (phase == SeasonPhase.LEAGUE) 150L else 320L)
    val next = result?.let { CareerViewModel.nextPhase(phase, it) }
    val allNames = names + listOfNotNull(result?.january?.signed?.player).associate { it.id to it.shortName }

    CompetitionScreen(
        eyebrow = eyebrow,
        title = stringResource(cup?.competition?.titleRes ?: R.string.mode_epl_title),
        matches = matches,
        reveal = reveal,
        names = allNames,
        onBack = onBack,
        total = if (cup == null) LEAGUE_MATCHES else null,
        highlightId = highlightId,
        january = if (cup == null) result?.january else null,
        paused = cup == null && season.awaitingJanuary,
        celebrate = cup?.won ?: ((result?.verdict as? Verdict.LeagueFinish)?.position == 1),
        confettiSeed = seed,
        summary = {
            if (cup != null) {
                item(key = "verdict") { CupVerdictCard(cup) }
            } else if (result != null) {
                item(key = "verdict") { LeagueVerdictCard(result.verdict as? Verdict.LeagueFinish, leagueDetail) }
                if (result.table.isNotEmpty()) item(key = "table") { TablePreview(result.table, teamName) }
                if (result.topScorers.isNotEmpty()) item(key = "scorers") { TopScorers(result) }
            }
        },
        actions = {
            PrimaryButton(continueLabel(next), onContinue, Modifier.testTag("season_continue"))
        },
    )

    if (cup == null && season.awaitingJanuary && reveal.isComplete) {
        JanuaryDialog(season.league, season.januaryOffers, onChooseJanuary)
    }
}

private const val LEAGUE_MATCHES = 38

@Composable
private fun continueLabel(next: SeasonPhase?): String = stringResource(
    when (next) {
        SeasonPhase.CUP -> R.string.career_next_cup
        SeasonPhase.EUROPE -> R.string.career_next_europe
        else -> R.string.career_next_review
    },
)

@Composable
fun LeagueVerdictCard(finish: Verdict.LeagueFinish?, detail: String?, modifier: Modifier = Modifier) {
    val champions = finish?.position == 1
    Column(
        modifier
            .fillMaxWidth()
            .panel(
                RoundedCornerShape(22.dp),
                color = if (champions) DugoutRaised else Dugout,
                edge = if (champions) Floodlight.copy(alpha = 0.55f) else ChalkLine,
            )
            .padding(horizontal = 20.dp, vertical = 22.dp)
            .testTag("league_verdict"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Eyebrow(stringResource(R.string.mode_epl_title), color = if (champions) Floodlight else ChalkMuted)
        if (finish != null) {
            Text(
                if (champions) {
                    stringResource(R.string.verdict_league_champions, finish.points)
                } else {
                    stringResource(R.string.verdict_league_finish, ordinal(finish.position), finish.points)
                },
                style = MaterialTheme.typography.headlineLarge,
                color = Chalk,
                textAlign = TextAlign.Center,
            )
        }
        detail?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = ChalkMuted, textAlign = TextAlign.Center) }
        if (champions) GoldBadge(stringResource(R.string.verdict_badge_trophy))
    }
}

/** A hub's main card: what happens next, and the button to do it. */
@Composable
fun StepCard(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    kickerColor: Color = Floodlight,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(20.dp), color = DugoutRaised, edge = kickerColor.copy(alpha = 0.4f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(kicker, color = kickerColor)
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Chalk)
        body?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = ChalkMuted) }
        content()
    }
}

/** The board's faith, 0 to 100: green when safe, gold when wobbling, red near the sack. */
@Composable
fun ConfidenceMeter(confidence: Int, modifier: Modifier = Modifier) {
    val color = when {
        confidence >= 55 -> ResultWin
        confidence >= 35 -> Floodlight
        else -> ResultLoss
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.dynasty_board_confidence), Modifier.weight(1f))
            Text("$confidence%", style = MaterialTheme.typography.titleMedium, color = color)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(ChalkLine),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(confidence / 100f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@get:StringRes
val Trophy.titleRes: Int
    get() = when (this) {
        Trophy.LEAGUE -> R.string.trophy_league
        Trophy.FA_CUP -> R.string.mode_fac_title
        Trophy.CHAMPIONS_LEAGUE -> R.string.competition_champions_league
        Trophy.EUROPA_LEAGUE -> R.string.competition_europa_league
        Trophy.CONFERENCE_LEAGUE -> R.string.competition_conference_league
    }

@get:StringRes
val Award.titleRes: Int
    get() = when (this) {
        Award.GOLDEN_BOOT -> R.string.award_golden_boot
        Award.PLAYER_OF_THE_SEASON -> R.string.award_player_of_season
        Award.YOUNG_PLAYER -> R.string.award_young_player
        Award.BALLON_DOR -> R.string.award_ballon_dor
    }

/** What the board asks for, in words. */
@Composable
fun targetLabel(target: Int): String = stringResource(
    when {
        target <= 1 -> R.string.target_title
        target <= 4 -> R.string.target_top_four
        target <= 7 -> R.string.target_europe
        target <= 10 -> R.string.target_top_half
        else -> R.string.target_survive
    },
)

/** "Winners", or the round a cup ended in. */
@Composable
fun cupFinishText(finish: CupFinish): String {
    val name = stringResource(finish.competition.titleRes)
    val stage = finish.stage
    return when {
        finish.won -> stringResource(R.string.cup_winners, name)
        stage == StageType.LEAGUE_PHASE -> stringResource(R.string.europe_out_league_phase, name)
        stage != null -> stringResource(R.string.europe_knocked_out, name, stageName(stage))
        else -> name
    }
}

/** Every trophy won, as gold pills with a count; a quiet line when the cabinet is empty. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrophyCabinet(trophies: Map<Trophy, Int>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(stringResource(R.string.career_trophy_cabinet))
        if (trophies.isEmpty()) {
            Text(stringResource(R.string.career_cabinet_empty), style = MaterialTheme.typography.bodyMedium, color = ChalkMuted)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Trophy.entries.forEach { trophy ->
                    val count = trophies[trophy] ?: return@forEach
                    Pill("${count}× ${stringResource(trophy.titleRes)}")
                }
            }
        }
    }
}

/** A finished career: the headline numbers and the silverware. */
@Composable
fun LegacyCard(legacy: Legacy, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(20.dp), color = DugoutRaised, edge = Floodlight.copy(alpha = 0.45f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(legacy.name, style = MaterialTheme.typography.headlineMedium, color = Chalk)
        Text(legacySummary(legacy), style = MaterialTheme.typography.bodyMedium, color = ChalkMuted)
        if (legacy.trophyCount > 0) {
            Text(
                legacy.trophies.entries.sortedBy { it.key.ordinal }
                    .map { (trophy, count) -> "$count× ${stringResource(trophy.titleRes)}" }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Floodlight,
            )
        }
    }
}

@Composable
fun legacySummary(legacy: Legacy): String {
    val seasons = pluralStringResource(R.plurals.career_seasons, legacy.seasons, legacy.seasons)
    return when (legacy.kind) {
        LegacyKind.MANAGER -> {
            val played = legacy.won + legacy.drawn + legacy.lost
            val rate = if (played == 0) 0 else legacy.won * 100 / played
            val ending = stringResource(if (legacy.sacked) R.string.legacy_sacked else R.string.legacy_retired)
            stringResource(R.string.legacy_manager_line, seasons, rate, ending)
        }
        LegacyKind.PLAYER -> stringResource(
            R.string.legacy_player_line, seasons, legacy.appearances, legacy.goals, legacy.peakRating, legacy.clubs.size,
        )
    }
}

/** Past careers of one kind, newest first. */
@Composable
fun HallOfFame(legacies: List<Legacy>, modifier: Modifier = Modifier) {
    if (legacies.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(stringResource(R.string.career_hall_of_fame))
        legacies.forEach { LegacyCard(it) }
    }
}

@Composable
fun CareerTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(MAX_NAME)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Hot,
            unfocusedBorderColor = ChalkLine,
            focusedLabelColor = Hot,
            unfocusedLabelColor = ChalkMuted,
            cursorColor = Hot,
            focusedContainerColor = Dugout,
            unfocusedContainerColor = Dugout,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

private const val MAX_NAME = 24

/** A radio card: pink edge when chosen. */
@Composable
fun ChoiceCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) Hot.copy(alpha = 0.12f) else Dugout.copy(alpha = 0.9f))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Hot else ChalkLine, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Chalk)
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ChalkMuted) }
    }
}

/** Kicker line for a career screen, e.g. "Season 3 of 10". */
@Composable
fun seasonKicker(season: Int, of: Int?): String =
    if (of != null) stringResource(R.string.career_season_of, season, of) else stringResource(R.string.career_season, season)
