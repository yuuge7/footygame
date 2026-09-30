package com.example.footygame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.game.ProCareer
import com.example.footygame.game.ReplayedRun
import com.example.footygame.game.shortNames
import com.example.footygame.models.CupRun
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DynastySeason
import com.example.footygame.models.DynastyState
import com.example.footygame.models.League
import com.example.footygame.models.ProSeason
import com.example.footygame.models.ProState
import com.example.footygame.models.RunResult
import com.example.footygame.models.RunSummary
import com.example.footygame.models.Verdict
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.ResultWin
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.Pill
import com.example.footygame.ui.components.ScreenHeader
import com.example.footygame.ui.components.SquadPitch
import com.example.footygame.ui.components.nightBackdrop
import com.example.footygame.ui.components.panel

/** The numbers of a finished league or campaign: goals, highlights, the table and the scorers. */
fun LazyListScope.seasonStats(result: RunResult, teamName: String? = null) {
    item(key = "summary") { SummaryStats(result) }
    item(key = "highlights") { Highlights(result) }
    if (result.table.isNotEmpty()) item(key = "table") { TablePreview(result.table, teamName) }
    if (result.topScorers.isNotEmpty()) item(key = "scorers") { TopScorers(result) }
}

/**
 * A finished run or career season, looked back on: [header] (the XI, the player's season), the record and
 * [verdict], the season's stats, the cups beside it, then every result. [result] is null when the run can
 * no longer be played back, and [unavailable] says so.
 */
@Composable
fun ResultDetailScreen(
    eyebrow: String,
    title: String,
    result: RunResult?,
    names: Map<String, String>,
    onBack: () -> Unit,
    unavailable: String,
    modifier: Modifier = Modifier,
    /** Fixed length of the main competition, for the scoreboard's progress bar; null for cups. */
    total: Int? = null,
    highlightId: String? = null,
    teamName: String? = null,
    verdict: @Composable () -> Unit = {},
    header: LazyListScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .nightBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ScreenHeader(eyebrow = eyebrow, title = title, onBack = onBack)
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("result_detail"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            header()
            if (result == null) {
                item(key = "unavailable") {
                    Text(
                        unavailable,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChalkMuted,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            } else {
                item(key = "scoreboard") { Scoreboard(result.matches, total, Modifier.padding(top = 8.dp, bottom = 16.dp)) }
                item(key = "verdict") { verdict() }
                seasonStats(result, teamName)
                listOfNotNull(result.cup, result.europe).forEach { cup ->
                    item(key = "card_${cup.competition.name}") { CupCard(cup, Modifier.padding(top = 16.dp)) }
                }
                allResults(result, names, highlightId)
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** Every result of a run: the main competition with its January window, then the cup and Europe. */
fun LazyListScope.allResults(result: RunResult, names: Map<String, String>, highlightId: String? = null) {
    item(key = "fixtures_header") {
        Eyebrow(stringResource(R.string.summary_fixtures), modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
    }
    val january = result.january
    itemsIndexed(result.matches, key = { index, _ -> "match_$index" }) { index, match ->
        Column {
            if (january != null && index == january.afterMatches) JanuaryRow(january)
            FixtureRow(match, names, highlightId)
        }
    }
    listOfNotNull(result.cup, result.europe).forEach { cupResults(it, names, highlightId) }
}

private fun LazyListScope.cupResults(cup: CupRun, names: Map<String, String>, highlightId: String?) {
    item(key = "results_${cup.competition.name}") {
        Eyebrow(
            stringResource(R.string.results_of, stringResource(cup.competition.titleRes)),
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
        )
    }
    itemsIndexed(cup.matches, key = { index, _ -> "${cup.competition.name}_$index" }) { _, match ->
        FixtureRow(match, names, highlightId)
    }
}

/** A run from the record book, played back: the XI, then everything the season screen showed. */
@Composable
fun RunDetailScreen(summary: RunSummary, replayed: ReplayedRun?, onBack: () -> Unit) {
    val result = replayed?.result
    ResultDetailScreen(
        eyebrow = "${summary.mode.challenge} · ${stringResource(summary.mode.titleRes)}",
        title = finishText(summary.finish, summary.perfect, summary.mode),
        result = result,
        names = remember(replayed) { replayed?.run { session.shortNames(result) }.orEmpty() },
        onBack = onBack,
        unavailable = stringResource(R.string.detail_run_unavailable),
        total = summary.mode.matches,
        verdict = { result?.let { VerdictCard(it, isNewBest = false) } },
        header = { replayed?.let { xi(it.session) } },
    )
}

/** A booked dynasty season: the board's verdict, the XI that played it, then the season played back. */
@Composable
fun DynastySeasonDetailScreen(state: DynastyState, season: DynastySeason, replayed: ReplayedRun?, onBack: () -> Unit) {
    val result = replayed?.result
    ResultDetailScreen(
        eyebrow = "${stringResource(R.string.dynasty_title)} · ${state.managerName}",
        title = stringResource(R.string.career_season, season.season),
        result = result,
        names = remember(replayed) { replayed?.run { session.shortNames(result) }.orEmpty() },
        onBack = onBack,
        unavailable = stringResource(R.string.detail_season_unavailable),
        total = League.PREMIER_LEAGUE.matches,
        verdict = {
            LeagueVerdictCard(
                result?.verdict as? Verdict.LeagueFinish,
                stringResource(R.string.dynasty_board_wanted, targetLabel(season.target)),
            )
        },
        header = {
            item(key = "review") {
                SummaryPanel {
                    SeasonReview(season)
                    if (season.trophies.isNotEmpty()) Honours(season.trophies.groupingBy { it }.eachCount())
                }
            }
            replayed?.let { xi(it.session, R.string.detail_season_xi) }
        },
    )
}

/** A booked player season: the player's own numbers and honours, then their club's season played back. */
@Composable
fun ProSeasonDetailScreen(state: ProState, season: ProSeason, replayed: ReplayedRun?, onBack: () -> Unit) {
    val result = replayed?.result
    ResultDetailScreen(
        eyebrow = "${stringResource(R.string.pro_title)} · ${state.name}",
        title = stringResource(R.string.pro_history_title, season.season, season.club),
        result = result,
        names = remember(replayed) { replayed?.run { session.shortNames(result) }.orEmpty() },
        onBack = onBack,
        unavailable = stringResource(R.string.detail_season_unavailable),
        total = season.league.matches,
        highlightId = ProCareer.PLAYER_ID,
        teamName = season.club,
        verdict = { LeagueVerdictCard(result?.verdict as? Verdict.LeagueFinish, null, season.league) },
        header = {
            item(key = "player") {
                SummaryPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow(
                            "${stringResource(R.string.detail_your_season)} · " +
                                stringResource(R.string.pro_player_line, season.age, stringResource(state.position.singularRes)),
                            Modifier.weight(1f),
                        )
                        Pill(
                            stringResource(if (season.starter) R.string.pro_starter else R.string.pro_squad_player),
                            color = if (season.starter) ResultWin else ChalkMuted,
                        )
                    }
                    SeasonReport(season)
                }
            }
        },
    )
}

/** The raised card a detail screen opens with: the season from the manager's or the player's side. */
@Composable
private fun SummaryPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(20.dp), color = DugoutRaised)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** The XI that played, on the pitch, under an optional [titleRes]. */
private fun LazyListScope.xi(session: DraftSession, titleRes: Int? = null) {
    if (titleRes != null) item(key = "xi_header") { Eyebrow(stringResource(titleRes), Modifier.padding(top = 20.dp, bottom = 8.dp)) }
    item(key = "xi") {
        SquadPitch(
            formation = session.formation,
            picks = session.picks,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
        )
    }
}
