package com.example.footygame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.footygame.R
import com.example.footygame.game.shortNames
import com.example.footygame.models.Competition
import com.example.footygame.models.DraftSession
import com.example.footygame.theme.Chalk
import com.example.footygame.theme.ChalkMuted
import com.example.footygame.theme.DugoutRaised
import com.example.footygame.theme.Floodlight
import com.example.footygame.ui.components.Pill
import com.example.footygame.ui.components.PrimaryButton
import com.example.footygame.ui.components.panel
import com.example.footygame.viewmodel.RunState

/** The European campaign a top-seven finish earned, played out after the league on its own screen. */
@Composable
fun EuropeScreen(run: RunState, session: DraftSession, onSeen: () -> Unit, onBack: () -> Unit) {
    val result = run.result ?: return
    val europe = result.europe ?: return
    val reveal = rememberReveal("europe_${run.seed}", europe.matches.size)
    val names = remember(session, result) { session.shortNames(result) }
    LaunchedEffect(reveal.isComplete) { if (reveal.isComplete) onSeen() }

    CompetitionScreen(
        eyebrow = "${run.mode.challenge} · ${stringResource(R.string.europe_title)}",
        title = stringResource(europe.competition.titleRes),
        matches = europe.matches,
        reveal = reveal,
        names = names,
        onBack = onBack,
        celebrate = europe.won,
        confettiSeed = run.seed,
        summary = { item(key = "verdict") { CupVerdictCard(europe) } },
        actions = {
            PrimaryButton(stringResource(R.string.europe_back_to_season), onBack, Modifier.testTag("europe_done"))
        },
    )
}

/** Shown on the season's results until the European campaign has been played: the invitation to play it. */
@Composable
fun CupInvite(competition: Competition, onPlay: () -> Unit, modifier: Modifier = Modifier, buttonTag: String = "play_europe") {
    Column(
        modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(20.dp), color = DugoutRaised, edge = Floodlight.copy(alpha = 0.45f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(stringResource(if (competition.isEuropean) R.string.europe_qualified else R.string.cup_next))
        Text(stringResource(competition.titleRes), style = MaterialTheme.typography.headlineMedium, color = Chalk)
        Text(
            stringResource(if (competition.isEuropean) R.string.europe_invite_body else R.string.cup_invite_body),
            style = MaterialTheme.typography.bodyMedium,
            color = ChalkMuted,
        )
        PrimaryButton(
            stringResource(if (competition.isEuropean) R.string.europe_play else R.string.cup_play),
            onPlay,
            Modifier
                .padding(top = 6.dp)
                .testTag(buttonTag),
        )
    }
}
