package com.example.footygame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.models.CupRun
import com.example.footygame.models.JanuaryOutcome
import com.example.footygame.models.MatchResult
import com.example.footygame.models.StageType
import com.example.footygame.models.Verdict
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkLine
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.Dugout
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.Floodlight
import com.example.footygame.theme.GoldGradient
import com.example.footygame.theme.Ink
import com.example.footygame.ui.components.Confetti
import com.example.footygame.ui.components.Eyebrow
import com.example.footygame.ui.components.ScreenHeader
import com.example.footygame.ui.components.nightBackdrop
import com.example.footygame.ui.components.panel

/**
 * One competition played out on its own screen: results tick in under a pinned scoreboard, then the
 * [summary] (verdict, table and the like) and the [actions] appear. Used for European nights and for
 * every step of a career season.
 */
@Composable
fun CompetitionScreen(
    eyebrow: String,
    title: String,
    matches: List<MatchResult>,
    reveal: Reveal,
    names: Map<String, String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Fixed length for a progress bar (a league); null for cups. */
    total: Int? = null,
    highlightId: String? = null,
    january: JanuaryOutcome? = null,
    /** More matches are still to come (the January window is open), so the summary waits. */
    paused: Boolean = false,
    celebrate: Boolean = false,
    confettiSeed: Long = 0L,
    summary: LazyListScope.() -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val shown = matches.take(reveal.shown)
    val complete = reveal.isComplete && !paused
    val listState = rememberLazyListState()

    LaunchedEffect(shown.size, complete) {
        when {
            complete -> listState.scrollToItem(0)
            shown.isNotEmpty() -> listState.animateScrollToItem(shown.lastIndex)
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .nightBackdrop()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            ScreenHeader(eyebrow = eyebrow, title = title, onBack = onBack) {
                if (!reveal.isComplete) {
                    TextButton(onClick = reveal::skip, modifier = Modifier.testTag("skip")) {
                        Text(stringResource(R.string.run_skip), color = Floodlight)
                    }
                }
            }
            if (!complete) {
                Scoreboard(shown, total, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (complete) {
                    item(key = "scoreboard") { Scoreboard(shown, total, Modifier.padding(bottom = 16.dp)) }
                    summary()
                    item(key = "fixtures_header") {
                        Eyebrow(stringResource(R.string.summary_fixtures), modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
                    }
                }
                itemsIndexed(shown, key = { index, _ -> "match_$index" }) { index, match ->
                    Column {
                        if (january != null && index == january.afterMatches) JanuaryRow(january)
                        FixtureRow(match, names, highlightId)
                    }
                }
            }
            if (complete) {
                Surface(color = Dugout, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = actions,
                    )
                }
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        if (complete && celebrate) Confetti(seed = confettiSeed, modifier = Modifier.fillMaxSize())
    }
}

/** The big card at the top of a finished cup: won it, or where and to whom it ended. */
@Composable
fun CupVerdictCard(run: CupRun, modifier: Modifier = Modifier) {
    val competition = stringResource(run.competition.titleRes)
    val eliminated = run.verdict as? Verdict.Eliminated
    val headline = when {
        run.won -> stringResource(R.string.cup_won_headline, competition)
        eliminated?.stage?.type == StageType.LEAGUE_PHASE -> stringResource(R.string.verdict_out_in, stringResource(R.string.stage_league_phase_short))
        eliminated != null -> stringResource(R.string.verdict_knocked_out, stageName(eliminated.stage.type, eliminated.stage.number))
        else -> competition
    }
    Column(
        modifier
            .fillMaxWidth()
            .panel(
                RoundedCornerShape(22.dp),
                color = if (run.won) DugoutRaised else Dugout,
                edge = if (run.won) Floodlight.copy(alpha = 0.55f) else ChalkLine,
            )
            .padding(horizontal = 20.dp, vertical = 22.dp)
            .testTag("cup_verdict"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Eyebrow(competition, color = if (run.won) Floodlight else ChalkMuted)
        Text(headline, style = MaterialTheme.typography.headlineLarge, color = Chalk, textAlign = TextAlign.Center)
        eliminated?.by?.let {
            Text(
                stringResource(R.string.verdict_beaten_by, it.name),
                style = MaterialTheme.typography.bodyLarge,
                color = ChalkMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (run.won) GoldBadge(stringResource(R.string.verdict_badge_trophy))
    }
}

/** Gold-to-violet capsule for a trophy moment. */
@Composable
fun GoldBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Ink,
        modifier = modifier
            .padding(top = 6.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(GoldGradient))
            .padding(horizontal = 26.dp, vertical = 8.dp),
    )
}
