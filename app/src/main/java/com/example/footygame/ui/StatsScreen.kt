package com.example.footygame.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.data.ModeRecord
import com.example.footygame.models.CareerStats
import com.example.footygame.models.DraftMode
import com.example.footygame.models.Finish
import com.example.footygame.models.ModeTotals
import com.example.footygame.models.RunSummary
import com.example.footygame.models.StageType
import com.example.footygame.models.Tally
import com.example.footygame.models.ranked
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.Ink
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.ScreenHeader
import com.example.footygame.ui.components.StatBlock
import com.example.footygame.ui.components.mownStripes
import kotlin.math.roundToInt

/** How many rows each leaderboard shows. */
private const val LEADERBOARD_ROWS = 5

@Composable
fun StatsScreen(
    stats: CareerStats,
    records: Map<DraftMode, ModeRecord>,
    onBack: () -> Unit,
) {
    // Null means every challenge added together.
    var mode by rememberSaveable { mutableStateOf<DraftMode?>(null) }
    val totals = remember(stats, mode) { stats.totals(mode) }
    val recent = remember(stats, mode) { stats.recent.filter { mode == null || it.mode == mode } }
    val recordedRuns = if (mode != null) records[mode]?.runs ?: 0 else records.values.sumOf { it.runs }

    Column(
        Modifier
            .fillMaxSize()
            .mownStripes()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ScreenHeader(eyebrow = stringResource(R.string.stats_eyebrow), title = stringResource(R.string.stats_title), onBack = onBack)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .wrapContentWidth()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "filters") { ModeFilter(mode, onSelect = { mode = it }) }
            // Records predate the stats, so older runs show up on the menu but not here.
            if (recordedRuns > totals.runs) {
                item(key = "since_update") {
                    Text(stringResource(R.string.stats_since_update), style = MaterialTheme.typography.bodySmall, color = ChalkMuted)
                }
            }
            if (totals.runs == 0) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.stats_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ChalkMuted,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .testTag("stats_empty"),
                    )
                }
            } else {
                item(key = "overview") { Overview(totals) }
                item(key = "record") { RecordCard(totals) }
                item(key = "bests") { BestsCard(totals) }
                leaderboard("scorers", R.string.summary_top_scorers, totals.scorers.ranked(), R.string.cd_stats_goals)
                leaderboard("drafted", R.string.stats_most_drafted, totals.picks.ranked(), R.string.cd_stats_drafted)
                leaderboard("squads", R.string.stats_favourite_squads, totals.squads.ranked(), R.string.cd_stats_squad)
                item(key = "formations") { FormationsCard(totals) }
                if (recent.isNotEmpty()) item(key = "recent") { RecentRuns(recent, showMode = mode == null) }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeFilter(selected: DraftMode?, onSelect: (DraftMode?) -> Unit) {
    val colors = FilterChipDefaults.filterChipColors(
        containerColor = Dugout,
        labelColor = Chalk,
        selectedContainerColor = Floodlight,
        selectedLabelColor = Ink,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.stats_filter_all)) },
            colors = colors,
            modifier = Modifier.testTag("stats_filter_ALL"),
        )
        DraftMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(mode.titleRes)) },
                colors = colors,
                modifier = Modifier.testTag("stats_filter_${mode.name}"),
            )
        }
    }
}

@Composable
private fun StatsCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Dugout.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, ChalkLine),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow(title)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Overview(totals: ModeTotals) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("stats_overview"),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val big = MaterialTheme.typography.displayMedium
        StatBlock(totals.runs.toString(), stringResource(R.string.stats_runs), valueStyle = big)
        StatBlock(totals.trophies.toString(), stringResource(R.string.stats_trophies), valueStyle = big, valueColor = Floodlight)
        StatBlock(totals.perfectRuns.toString(), stringResource(R.string.stats_perfect), valueStyle = big)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordCard(totals: ModeTotals) {
    val winRate = if (totals.played == 0) 0 else (totals.won * 100f / totals.played).roundToInt()
    StatsCard(stringResource(R.string.stats_record)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(totals.played.toString(), stringResource(R.string.stats_played))
            StatBlock(totals.won.toString(), stringResource(R.string.run_wins))
            StatBlock(totals.drawn.toString(), stringResource(R.string.run_draws))
            StatBlock(totals.lost.toString(), stringResource(R.string.run_losses))
        }
        // Win rate is one ratio against a whole, so it reads as a meter rather than a chart.
        val description = stringResource(R.string.cd_stats_win_rate, winRate, totals.played)
        Column(
            Modifier.clearAndSetSemantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Eyebrow(stringResource(R.string.stats_win_rate), modifier = Modifier.weight(1f))
                Text(stringResource(R.string.stats_percent, winRate), style = MaterialTheme.typography.titleMedium, color = Chalk)
            }
            MagnitudeBar(winRate / 100f, thickness = 8.dp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(totals.goalsFor.toString(), stringResource(R.string.summary_goals_for))
            StatBlock(totals.goalsAgainst.toString(), stringResource(R.string.summary_goals_against))
            StatBlock(totals.cleanSheets.toString(), stringResource(R.string.summary_clean_sheets))
            val perGame = if (totals.played == 0) 0f else totals.goalsFor.toFloat() / totals.played
            StatBlock(stringResource(R.string.stats_one_decimal, perGame), stringResource(R.string.stats_goals_per_game))
        }
    }
}

@Composable
private fun BestsCard(totals: ModeTotals) {
    StatsCard(stringResource(R.string.stats_bests)) {
        totals.biggestWin?.let {
            LabelledValue(
                stringResource(R.string.highlight_biggest_win),
                stringResource(R.string.highlight_score_against, it.goalsFor, it.goalsAgainst, it.opponent),
            )
        }
        LabelledValue(
            stringResource(R.string.highlight_win_streak),
            pluralStringResource(R.plurals.highlight_matches, totals.longestWinStreak, totals.longestWinStreak),
        )
        LabelledValue(
            stringResource(R.string.highlight_unbeaten),
            pluralStringResource(R.plurals.highlight_matches, totals.longestUnbeatenRun, totals.longestUnbeatenRun),
        )
    }
}

/** A label on the left, its value on the right: for facts that aren't numbers to compare. */
@Composable
internal fun LabelledValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ChalkMuted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun LazyListScope.leaderboard(
    key: String,
    titleRes: Int,
    ranked: List<Tally>,
    descriptionRes: Int,
) {
    if (ranked.isEmpty()) return
    item(key = key) {
        val top = ranked.take(LEADERBOARD_ROWS)
        StatsCard(stringResource(titleRes), Modifier.testTag("stats_$key")) {
            top.forEach { tally ->
                RankedRow(tally.name, tally.count, top.first().count, stringResource(descriptionRes, tally.name, tally.count))
            }
        }
    }
}

@Composable
private fun FormationsCard(totals: ModeTotals) {
    val used = totals.formations.entries.sortedWith(compareByDescending<Map.Entry<*, Int>> { it.value })
    if (used.isEmpty()) return
    StatsCard(stringResource(R.string.stats_formations)) {
        used.forEach { (formation, count) ->
            RankedRow(formation.label, count, used.first().value, stringResource(R.string.cd_stats_formation, formation.label, count))
        }
    }
}

/**
 * One leaderboard entry: name and count, with a bar scaled to the leader so the gaps read at a glance.
 * One series in one hue, so the section title names it and no legend is needed.
 */
@Composable
private fun RankedRow(name: String, count: Int, leader: Int, description: String) {
    Column(
        Modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = Chalk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = Chalk)
        }
        MagnitudeBar(if (leader == 0) 0f else count / leader.toFloat(), thickness = 4.dp)
    }
}

/** A thin bar from a square baseline to a rounded end, over a faint track of the full width. */
@Composable
private fun MagnitudeBar(fraction: Float, thickness: Dp) {
    val end = RoundedCornerShape(topEnd = thickness / 2, bottomEnd = thickness / 2)
    Box(
        Modifier
            .fillMaxWidth()
            .height(thickness)
            .background(ChalkLine, end),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(thickness)
                    .background(Floodlight, end),
            )
        }
    }
}

@Composable
private fun RecentRuns(runs: List<RunSummary>, showMode: Boolean) {
    StatsCard(stringResource(R.string.stats_recent), Modifier.testTag("stats_recent")) {
        runs.forEachIndexed { index, run ->
            if (index > 0) HorizontalDivider(color = ChalkLine)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (showMode) Eyebrow(stringResource(run.mode.titleRes))
                    Text(
                        finishText(run.finish, run.perfect, run.mode),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (run.finish.trophy || run.perfect) Floodlight else Chalk,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${run.won}-${run.drawn}-${run.lost}", style = MaterialTheme.typography.titleMedium, color = Chalk)
                    Text("${run.goalsFor}:${run.goalsAgainst}", style = MaterialTheme.typography.bodySmall, color = ChalkMuted)
                }
            }
        }
    }
}

@Composable
private fun finishText(finish: Finish, perfect: Boolean, mode: DraftMode): String {
    if (perfect) return stringResource(R.string.verdict_perfect, mode.challenge)
    if (finish.trophy) return stringResource(R.string.verdict_champions)
    finish.position?.let { return stringResource(R.string.stats_finish_position, ordinal(it)) }
    val stage = finish.stage ?: return ""
    return when (stage) {
        StageType.LEAGUE_PHASE -> stringResource(R.string.verdict_out_in, stringResource(R.string.stage_league_phase_short))
        StageType.GROUP_STAGE -> stringResource(R.string.verdict_out_in, stringResource(R.string.stage_group_short))
        else -> stringResource(R.string.verdict_knocked_out, stageName(stage, finish.stageNumber))
    }
}
