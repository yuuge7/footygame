package com.example.footygame.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.game.Dynasty
import com.example.footygame.models.DynastyPhase
import com.example.footygame.models.DynastySeason
import com.example.footygame.models.DynastyState
import com.example.footygame.models.Formation
import com.example.footygame.models.League
import com.example.footygame.models.Legacy
import com.example.footygame.models.LegacyKind
import com.example.footygame.models.ManagerTrait
import com.example.footygame.models.RatingChange
import com.example.footygame.models.SeasonPhase
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
import com.example.footygame.ui.components.PrimaryButton
import com.example.footygame.ui.components.ScreenHeader
import com.example.footygame.ui.components.SecondaryButton
import com.example.footygame.ui.components.SquadPitch
import com.example.footygame.ui.components.nightBackdrop
import com.example.footygame.ui.components.panel
import com.example.footygame.viewmodel.SeasonView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynastySetupScreen(
    legacies: List<Legacy>,
    onStart: (name: String, trait: ManagerTrait, formation: Formation) -> Unit,
    onBack: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var trait by rememberSaveable { mutableStateOf(ManagerTrait.TACTICIAN) }
    var formation by rememberSaveable { mutableStateOf(Formation.F433) }

    CareerSetupFrame(
        kicker = stringResource(R.string.dynasty_kicker),
        title = stringResource(R.string.dynasty_title),
        description = stringResource(R.string.dynasty_description),
        onBack = onBack,
        startLabel = stringResource(R.string.dynasty_start),
        startEnabled = name.isNotBlank(),
        onStart = { onStart(name, trait, formation) },
        startTag = "start_dynasty",
    ) {
        CareerTextField(name, { name = it }, stringResource(R.string.dynasty_name_label), Modifier.testTag("manager_name"))
        Eyebrow(stringResource(R.string.dynasty_style))
        ManagerTrait.entries.forEach { option ->
            ChoiceCard(
                title = stringResource(option.titleRes),
                detail = stringResource(option.detailRes),
                selected = option == trait,
                onClick = { trait = option },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Eyebrow(stringResource(R.string.setup_section_formation))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Formation.entries.forEach { option ->
                FilterChip(
                    selected = option == formation,
                    onClick = { formation = option },
                    label = { Text(option.label, style = MaterialTheme.typography.labelLarge) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = Chalk,
                        selectedContainerColor = Hot,
                        selectedLabelColor = Chalk,
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = option == formation, borderColor = ChalkLine),
                )
            }
        }
        HallOfFame(legacies.filter { it.kind == LegacyKind.MANAGER }, Modifier.padding(top = 12.dp))
    }
}

/** The shared look of a career's setup: green career kicker, big title, the form, and a start button. */
@Composable
fun CareerSetupFrame(
    kicker: String,
    title: String,
    description: String,
    onBack: () -> Unit,
    startLabel: String,
    startEnabled: Boolean,
    onStart: () -> Unit,
    startTag: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .nightBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ScreenHeader(eyebrow = stringResource(R.string.career_modes), title = stringResource(R.string.career_new), onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(kicker.uppercase(), style = MaterialTheme.typography.labelMedium, color = ResultWin)
                Text(title, style = MaterialTheme.typography.displaySmall, color = Chalk, textAlign = TextAlign.Center)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChalkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            content()
        }
        Surface(color = Dugout, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                PrimaryButton(startLabel, onStart, Modifier.testTag(startTag), enabled = startEnabled, colors = CareerStartGradient)
            }
        }
    }
}

/** The dynasty between matches: what's next, the board, the XI, the silverware and the seasons so far. */
@Composable
fun DynastyHubScreen(
    state: DynastyState,
    season: SeasonView?,
    onBack: () -> Unit,
    onKickOff: () -> Unit,
    onResume: () -> Unit,
    onCloseReview: () -> Unit,
    onStartSigning: (slotId: String) -> Unit,
    onNextSeason: () -> Unit,
    onNewDynasty: () -> Unit,
    onQuit: () -> Unit,
) {
    var confirmQuit by rememberSaveable { mutableStateOf(false) }
    val picks = remember(state.squad) { Dynasty.picks(state.squad) }
    val empty = state.formation.slots.size - state.squad.size
    val trophies = remember(state.history) { state.history.flatMap { it.trophies }.groupingBy { it }.eachCount() }

    Column(
        Modifier
            .fillMaxSize()
            .nightBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ScreenHeader(eyebrow = stringResource(R.string.dynasty_title), title = state.managerName, onBack = onBack)
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("career_hub"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "step") {
                DynastyStep(state, season, empty, onKickOff, onResume, onCloseReview, onNextSeason, onNewDynasty, onBack)
            }
            if (state.phase != DynastyPhase.FINISHED) {
                item(key = "board") {
                    Column(
                        Modifier
                            .panel()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ConfidenceMeter(state.confidence)
                        if (state.phase == DynastyPhase.SEASON) {
                            Text(
                                stringResource(R.string.dynasty_board_wants, targetLabel(state.target)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Chalk,
                            )
                        }
                    }
                }
            }
            item(key = "pitch") {
                SquadPitch(
                    formation = state.formation,
                    picks = picks,
                    onSlotClick = if (state.phase == DynastyPhase.SUMMER && state.signingsLeft > 0) {
                        { slot -> onStartSigning(slot.id) }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                )
            }
            item(key = "cabinet") { TrophyCabinet(trophies) }
            if (state.history.isNotEmpty()) {
                item(key = "history_header") { Eyebrow(stringResource(R.string.career_history)) }
                items(state.history.reversed(), key = { "season_${it.season}" }) { HistoryRow(it) }
            }
            if (state.phase != DynastyPhase.FINISHED) {
                item(key = "quit") {
                    TextButton(
                        onClick = { confirmQuit = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quit_career"),
                    ) {
                        Text(stringResource(R.string.dynasty_quit), color = ChalkMuted)
                    }
                }
            }
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            containerColor = DugoutRaised,
            title = { Text(stringResource(R.string.dynasty_quit_title)) },
            text = { Text(stringResource(R.string.dynasty_quit_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmQuit = false
                    onQuit()
                }) { Text(stringResource(R.string.dynasty_quit_confirm), color = Hot) }
            },
            dismissButton = {
                TextButton(onClick = { confirmQuit = false }) { Text(stringResource(R.string.draft_leave_cancel), color = Chalk) }
            },
        )
    }
}

@Composable
private fun DynastyStep(
    state: DynastyState,
    season: SeasonView?,
    empty: Int,
    onKickOff: () -> Unit,
    onResume: () -> Unit,
    onCloseReview: () -> Unit,
    onNextSeason: () -> Unit,
    onNewDynasty: () -> Unit,
    onMenu: () -> Unit,
) {
    val kicker = seasonKicker(state.season, Dynasty.SEASONS)
    when (state.phase) {
        DynastyPhase.DRAFT -> Unit

        DynastyPhase.SEASON -> when (state.seasonPhase) {
            SeasonPhase.PRESEASON -> StepCard(
                kicker = kicker,
                title = stringResource(R.string.dynasty_kickoff_title, state.season),
                body = stringResource(R.string.dynasty_kickoff_body),
            ) {
                PrimaryButton(stringResource(R.string.career_kick_off), onKickOff, Modifier.testTag("kick_off"))
            }

            SeasonPhase.REVIEW -> {
                val last = state.history.lastOrNull()
                StepCard(
                    kicker = kicker,
                    title = stringResource(if (state.sacked) R.string.dynasty_sacked_title else R.string.dynasty_review_title),
                    kickerColor = if (state.sacked) ResultLoss else Floodlight,
                ) {
                    last?.let { SeasonReview(it) }
                    val ends = state.sacked || state.season >= Dynasty.SEASONS
                    PrimaryButton(
                        stringResource(if (ends) R.string.dynasty_see_legacy else R.string.dynasty_open_summer),
                        onCloseReview,
                        Modifier.testTag("close_review"),
                    )
                }
            }

            else -> StepCard(
                kicker = kicker,
                title = phaseTitle(state.seasonPhase),
                body = stringResource(R.string.career_up_next),
            ) {
                PrimaryButton(playLabel(state.seasonPhase), onResume, Modifier.testTag("resume_season"), enabled = season != null)
            }
        }

        DynastyPhase.SUMMER -> StepCard(
            kicker = stringResource(R.string.dynasty_summer),
            title = pluralStringResource(R.plurals.dynasty_signings_left, state.signingsLeft, state.signingsLeft),
            body = stringResource(if (empty > 0) R.string.dynasty_summer_fill else R.string.dynasty_summer_body),
        ) {
            state.changes.forEach { ChangeRow(it) }
            PrimaryButton(
                stringResource(R.string.dynasty_next_season, state.season + 1),
                onNextSeason,
                Modifier
                    .padding(top = 6.dp)
                    .testTag("next_season"),
                enabled = empty == 0,
            )
        }

        DynastyPhase.FINISHED -> StepCard(
            kicker = stringResource(if (state.sacked) R.string.legacy_sacked else R.string.legacy_retired),
            title = stringResource(R.string.dynasty_over_title),
            kickerColor = if (state.sacked) ResultLoss else ResultWin,
        ) {
            LegacyCard(Dynasty.legacy(state))
            PrimaryButton(stringResource(R.string.dynasty_new), onNewDynasty, Modifier.testTag("new_dynasty"), colors = CareerStartGradient)
            SecondaryButton(stringResource(R.string.action_menu), onMenu)
        }
    }
}

/** The hub button for a season step that's under way. */
@Composable
fun playLabel(phase: SeasonPhase, league: League = League.PREMIER_LEAGUE): String = when (phase) {
    SeasonPhase.CUP -> stringResource(R.string.career_play_competition, stringResource(league.cup.titleRes))
    SeasonPhase.EUROPE -> stringResource(R.string.europe_play)
    else -> stringResource(R.string.career_back_to_league)
}

/** The name of a season step: the league, its cup, or European nights. */
@Composable
fun phaseTitle(phase: SeasonPhase, league: League = League.PREMIER_LEAGUE): String = stringResource(
    when (phase) {
        SeasonPhase.CUP -> league.cup.titleRes
        SeasonPhase.EUROPE -> R.string.europe_title
        else -> league.titleRes
    },
)

@Composable
private fun SeasonReview(season: DynastySeason) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (season.position == 1) {
                stringResource(R.string.verdict_league_champions, season.points)
            } else {
                stringResource(R.string.verdict_league_finish, ordinal(season.position), season.points)
            },
            style = MaterialTheme.typography.titleMedium,
            color = Chalk,
        )
        val met = season.position <= season.target
        Text(
            stringResource(if (met) R.string.dynasty_target_met else R.string.dynasty_target_missed, targetLabel(season.target)),
            style = MaterialTheme.typography.bodyMedium,
            color = if (met) ResultWin else ResultLoss,
        )
        season.cup?.let { Text(cupFinishText(it), style = MaterialTheme.typography.bodyMedium, color = ChalkMuted) }
        season.europe?.let { Text(cupFinishText(it), style = MaterialTheme.typography.bodyMedium, color = ChalkMuted) }
        season.topScorer?.let {
            Text(
                stringResource(R.string.dynasty_top_scorer, it, season.topScorerGoals),
                style = MaterialTheme.typography.bodyMedium,
                color = ChalkMuted,
            )
        }
        Text(
            stringResource(R.string.dynasty_confidence_change, season.confidenceBefore, season.confidenceAfter),
            style = MaterialTheme.typography.bodyMedium,
            color = if (season.confidenceAfter >= season.confidenceBefore) ResultWin else ResultLoss,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun ChangeRow(change: RatingChange) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(change.name, style = MaterialTheme.typography.bodyMedium, color = Chalk, modifier = Modifier.weight(1f), maxLines = 1)
        Spacer(Modifier.width(8.dp))
        val to = change.to
        if (to == null) {
            Text(stringResource(R.string.dynasty_retired), style = MaterialTheme.typography.labelLarge, color = ResultLoss)
        } else {
            val delta = to - change.from
            Text(
                "${change.from} → $to",
                style = MaterialTheme.typography.labelLarge,
                color = if (delta >= 0) ResultWin else ResultLoss,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryRow(season: DynastySeason) {
    Column(
        Modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.career_season, season.season),
                style = MaterialTheme.typography.titleMedium,
                color = Chalk,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.dynasty_history_finish, ordinal(season.position), season.points),
                style = MaterialTheme.typography.titleSmall,
                color = if (season.position <= season.target) ResultWin else ResultLoss,
            )
        }
        if (season.trophies.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                season.trophies.forEach {
                    Text(trophyName(it), style = MaterialTheme.typography.labelMedium, color = Floodlight)
                }
            }
        }
    }
}
