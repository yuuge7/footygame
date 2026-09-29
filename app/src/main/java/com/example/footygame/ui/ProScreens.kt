package com.example.footygame.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.data.ClubSeasons
import com.example.footygame.game.ProCareer
import com.example.footygame.models.League
import com.example.footygame.models.Legacy
import com.example.footygame.models.LegacyKind
import com.example.footygame.models.Offer
import com.example.footygame.models.Position
import com.example.footygame.models.ProPhase
import com.example.footygame.models.ProSeason
import com.example.footygame.models.ProState
import com.example.footygame.models.SeasonPhase
import com.example.footygame.models.seasonLabel
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.Hot
import com.example.footygame.theme.ResultLoss
import com.example.footygame.theme.ResultWin
import com.example.footygame.ui.components.ClubBadge
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.Jersey
import com.example.footygame.ui.components.Pill
import com.example.footygame.ui.components.PrimaryButton
import com.example.footygame.ui.components.RatingBadge
import com.example.footygame.ui.components.ScreenHeader
import com.example.footygame.ui.components.SecondaryButton
import com.example.footygame.ui.components.SquadPitch
import com.example.footygame.ui.components.StatTile
import com.example.footygame.ui.components.kitInitials
import com.example.footygame.ui.components.nightBackdrop
import com.example.footygame.ui.components.panel
import com.example.footygame.viewmodel.SeasonView

/** Decades a career can start in, by the season it starts. */
enum class StartEra(val year: Int, @param:StringRes val labelRes: Int) {
    NINETIES(1995, R.string.pro_era_90s),
    NOUGHTIES(2003, R.string.pro_era_00s),
    TENS(2011, R.string.pro_era_10s),
    TWENTIES(2019, R.string.pro_era_20s),
}

@get:StringRes
val Position.singularRes: Int
    get() = when (this) {
        Position.GK -> R.string.pro_position_gk
        Position.DEF -> R.string.pro_position_def
        Position.MID -> R.string.pro_position_mid
        Position.ATT -> R.string.pro_position_att
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProSetupScreen(
    legacies: List<Legacy>,
    onStart: (name: String, position: Position, startYear: Int, league: League) -> Unit,
    onBack: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var position by rememberSaveable { mutableStateOf(Position.ATT) }
    var era by rememberSaveable { mutableStateOf(StartEra.NOUGHTIES) }
    var league by rememberSaveable { mutableStateOf(League.PREMIER_LEAGUE) }

    CareerSetupFrame(
        kicker = stringResource(R.string.pro_kicker),
        title = stringResource(R.string.pro_title),
        description = stringResource(R.string.pro_description),
        onBack = onBack,
        startLabel = stringResource(R.string.pro_start),
        startEnabled = name.isNotBlank(),
        onStart = { onStart(name, position, era.year, league) },
        startTag = "start_pro",
    ) {
        CareerTextField(name, { name = it }, stringResource(R.string.pro_name_label), Modifier.testTag("player_name"))
        Eyebrow(stringResource(R.string.pro_position))
        listOf(Position.ATT, Position.MID, Position.DEF, Position.GK).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { option ->
                    ChoiceCard(
                        title = stringResource(option.singularRes),
                        selected = option == position,
                        onClick = { position = option },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Eyebrow(stringResource(R.string.pro_league))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            League.entries.forEach { option ->
                ChoiceCard(
                    title = stringResource(option.titleRes),
                    detail = stringResource(option.countryRes),
                    selected = option == league,
                    onClick = { league = option },
                    modifier = Modifier.testTag("league_${option.name}"),
                )
            }
        }
        Eyebrow(stringResource(R.string.pro_era))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StartEra.entries.forEach { option ->
                ChoiceCard(stringResource(option.labelRes), selected = option == era, onClick = { era = option })
            }
        }
        HallOfFame(legacies.filter { it.kind == LegacyKind.PLAYER }, Modifier.padding(top = 12.dp))
    }
}

/** The player career between matches: the player, what's next, their club's XI and the story so far. */
@Composable
fun ProHubScreen(
    state: ProState,
    season: SeasonView?,
    onBack: () -> Unit,
    onChooseFirstClub: (Offer) -> Unit,
    onKickOff: () -> Unit,
    onResume: () -> Unit,
    onCloseReview: () -> Unit,
    onRetire: () -> Unit,
    onSign: (Offer?) -> Unit,
    onNewCareer: () -> Unit,
    onQuit: () -> Unit,
) {
    var confirmQuit by rememberSaveable { mutableStateOf(false) }
    val squad = remember(state.club, state.year) { if (state.club.isEmpty()) null else ProCareer.clubSquad(state.club, state.year) }
    val lineup = remember(state, squad) { squad?.let { ProCareer.lineup(state, it) } }
    val trophies = remember(state.history) { state.history.flatMap { it.trophies }.groupingBy { it }.eachCount() }

    Column(
        Modifier
            .fillMaxSize()
            .nightBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ScreenHeader(eyebrow = stringResource(R.string.pro_title), title = state.name, onBack = onBack)
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("career_hub"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "player") { PlayerCard(state, squad?.primaryColor, squad?.secondaryColor) }
            item(key = "step") {
                ProStep(state, season, onChooseFirstClub, onKickOff, onResume, onCloseReview, onRetire, onSign, onNewCareer, onBack)
            }
            if (lineup != null && state.phase != ProPhase.RETIRED) {
                item(key = "pitch_header") { Eyebrow(stringResource(R.string.pro_club_xi, state.club)) }
                item(key = "pitch") {
                    SquadPitch(
                        formation = ProCareer.FORMATION,
                        picks = lineup,
                        highlightId = ProCareer.PLAYER_ID,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                    )
                }
            }
            item(key = "totals") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(state.appearances.toString(), stringResource(R.string.pro_apps), Modifier.weight(1f))
                    StatTile(state.goals.toString(), stringResource(R.string.pro_goals), Modifier.weight(1f), valueColor = Floodlight)
                    StatTile(state.trophyCount.toString(), stringResource(R.string.stats_trophies), Modifier.weight(1f))
                    StatTile(state.history.sumOf { it.awards.size }.toString(), stringResource(R.string.pro_awards), Modifier.weight(1f))
                }
            }
            item(key = "cabinet") { TrophyCabinet(trophies) }
            if (state.history.isNotEmpty()) {
                item(key = "history_header") { Eyebrow(stringResource(R.string.career_history)) }
                items(state.history.reversed(), key = { "season_${it.season}" }) { ProHistoryRow(it) }
            }
            if (state.phase != ProPhase.RETIRED) {
                item(key = "quit") {
                    TextButton(
                        onClick = { confirmQuit = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quit_career"),
                    ) {
                        Text(stringResource(R.string.pro_quit), color = ChalkMuted)
                    }
                }
            }
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            containerColor = DugoutRaised,
            title = { Text(stringResource(R.string.pro_quit_title)) },
            text = { Text(stringResource(R.string.pro_quit_body)) },
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
private fun PlayerCard(state: ProState, primary: Long?, secondary: Long?) {
    Row(
        Modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Jersey(
            primary = primary?.let(::Color) ?: Hot,
            secondary = secondary?.let(::Color) ?: Floodlight,
            initials = kitInitials(state.name),
            width = 64.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(state.name, style = MaterialTheme.typography.headlineSmall, color = Chalk, maxLines = 1)
            Text(
                stringResource(R.string.pro_player_line, state.age, stringResource(state.position.singularRes)),
                style = MaterialTheme.typography.bodyMedium,
                color = ChalkMuted,
            )
            if (state.club.isNotEmpty()) {
                Text(state.club, style = MaterialTheme.typography.titleSmall, color = Chalk)
                Text(stringResource(state.league.titleRes), style = MaterialTheme.typography.bodySmall, color = ChalkMuted)
            }
        }
        RatingBadge(state.rating, 48.dp)
    }
}

@Composable
private fun ProStep(
    state: ProState,
    season: SeasonView?,
    onChooseFirstClub: (Offer) -> Unit,
    onKickOff: () -> Unit,
    onResume: () -> Unit,
    onCloseReview: () -> Unit,
    onRetire: () -> Unit,
    onSign: (Offer?) -> Unit,
    onNewCareer: () -> Unit,
    onMenu: () -> Unit,
) {
    val kicker = "${seasonKicker(state.season, null)} · ${seasonYearLabel(state.year)}"
    when (state.phase) {
        ProPhase.FIRST_CLUB -> StepCard(
            kicker = stringResource(R.string.pro_first_club_kicker),
            title = stringResource(R.string.pro_first_club_title),
            body = stringResource(R.string.pro_first_club_body),
        ) {
            state.offers.forEach { offer -> OfferCard(offer, onClick = { onChooseFirstClub(offer) }) }
        }

        ProPhase.SEASON -> when (state.seasonPhase) {
            SeasonPhase.PRESEASON -> {
                val squad = ProCareer.clubSquad(state.club, state.year)
                val starts = squad != null && ProCareer.starts(state, squad)
                StepCard(
                    kicker = kicker,
                    title = stringResource(R.string.pro_kickoff_title, state.club),
                    body = stringResource(if (starts) R.string.pro_role_starter else R.string.pro_role_bench),
                ) {
                    PrimaryButton(stringResource(R.string.career_kick_off), onKickOff, Modifier.testTag("kick_off"))
                }
            }

            SeasonPhase.REVIEW -> {
                val report = state.history.lastOrNull()
                StepCard(kicker = kicker, title = stringResource(R.string.pro_review_title)) {
                    report?.let { SeasonReport(it) }
                    PrimaryButton(stringResource(R.string.pro_open_transfers), onCloseReview, Modifier.testTag("close_review"))
                    if (ProCareer.canRetire(ProCareer.grown(state))) SecondaryButton(stringResource(R.string.pro_retire), onRetire)
                }
            }

            else -> StepCard(
                kicker = kicker,
                title = phaseTitle(state.seasonPhase, state.league),
                body = stringResource(R.string.career_up_next),
            ) {
                PrimaryButton(playLabel(state.seasonPhase, state.league), onResume, Modifier.testTag("resume_season"), enabled = season != null)
            }
        }

        ProPhase.TRANSFERS -> StepCard(
            kicker = stringResource(R.string.pro_transfers_kicker),
            title = stringResource(if (state.offers.isEmpty()) R.string.pro_no_offers else R.string.pro_offers_title),
            body = stringResource(R.string.pro_offers_body),
        ) {
            state.offers.forEach { offer -> OfferCard(offer, onClick = { onSign(offer) }) }
            SecondaryButton(stringResource(R.string.pro_stay, state.club), { onSign(null) }, Modifier.testTag("stay"))
            if (ProCareer.canRetire(state)) {
                TextButton(onClick = onRetire, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.pro_retire), color = ChalkMuted)
                }
            }
        }

        ProPhase.RETIRED -> StepCard(
            kicker = stringResource(R.string.legacy_retired),
            title = stringResource(R.string.pro_over_title),
            kickerColor = ResultWin,
        ) {
            LegacyCard(ProCareer.legacy(state))
            PrimaryButton(stringResource(R.string.pro_new), onNewCareer, Modifier.testTag("new_pro"), colors = CareerStartGradient)
            SecondaryButton(stringResource(R.string.action_menu), onMenu)
        }
    }
}

private fun seasonYearLabel(year: Int): String = seasonLabel(year, isTournament = false)

/** A club that wants the player: their shirt, how strong they are, and whether he'd start. */
@Composable
private fun OfferCard(offer: Offer, onClick: () -> Unit) {
    val squad = remember(offer.squadId) { ClubSeasons.squad(offer.squadId) }
    Row(
        Modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(14.dp), color = Dugout)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("offer"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (squad != null) ClubBadge(squad, 44.dp, showCode = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(offer.club, style = MaterialTheme.typography.titleMedium, color = Chalk)
            Text(
                stringResource(R.string.pro_offer_line, stringResource(offer.league.titleRes), offer.strength),
                style = MaterialTheme.typography.bodySmall,
                color = ChalkMuted,
            )
        }
        Pill(
            stringResource(if (offer.starter) R.string.pro_starter else R.string.pro_squad_player),
            color = if (offer.starter) ResultWin else ChalkMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonReport(season: ProSeason) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            stringResource(R.string.pro_report_line, season.appearances, season.goals),
            style = MaterialTheme.typography.titleMedium,
            color = Chalk,
        )
        if (season.position > 0) {
            Text(
                stringResource(R.string.pro_report_finish, season.club, ordinal(season.position)),
                style = MaterialTheme.typography.bodyMedium,
                color = ChalkMuted,
            )
        }
        val delta = season.ratingAfter - season.ratingBefore
        Text(
            stringResource(R.string.pro_rating_change, season.ratingBefore, season.ratingAfter),
            style = MaterialTheme.typography.bodyMedium,
            color = if (delta >= 0) ResultWin else ResultLoss,
        )
        if (season.trophies.isNotEmpty() || season.awards.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                season.trophies.forEach { Pill(trophyName(it)) }
                season.awards.forEach { Pill(stringResource(it.titleRes), color = Hot) }
            }
        }
    }
}

@Composable
private fun ProHistoryRow(season: ProSeason) {
    Column(
        Modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.pro_history_title, season.season, season.club),
                style = MaterialTheme.typography.titleMedium,
                color = Chalk,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            RatingBadge(season.ratingAfter, 30.dp)
        }
        Text(
            stringResource(R.string.pro_history_line, stringResource(season.league.titleRes), season.age, season.appearances, season.goals),
            style = MaterialTheme.typography.bodySmall,
            color = ChalkMuted,
        )
        val honours = season.trophies.map { trophyName(it) } + season.awards.map { stringResource(it.titleRes) }
        if (honours.isNotEmpty()) {
            Text(honours.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Floodlight)
        }
    }
}
