package com.example.footygame.game

import com.example.footygame.data.ClubSeasons
import com.example.footygame.models.CareerStats
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSession
import com.example.footygame.models.Finish
import com.example.footygame.models.MatchResult
import com.example.footygame.models.ModeTotals
import com.example.footygame.models.PickRef
import com.example.footygame.models.RunReplay
import com.example.footygame.models.RunResult
import com.example.footygame.models.RunSummary
import com.example.footygame.models.Scoreline
import com.example.footygame.models.Tally
import com.example.footygame.models.Verdict

/** The record book remembers this many recent runs. */
const val RECENT_RUNS = 20

/** Each leaderboard keeps its top entries only, so years of play can't bloat the saved stats. */
const val MAX_TALLIES = 100

/** Adds one finished run to the record book. [session] is the XI that played it, from [seed] when it's known. */
fun CareerStats.withRun(result: RunResult, session: DraftSession, seed: Long? = null): CareerStats {
    val mode = result.mode
    val totals = (modes[mode] ?: ModeTotals()) + result.toTotals(session)
    val summary = RunSummary(
        mode = mode,
        won = result.wins,
        drawn = result.draws,
        lost = result.losses,
        goalsFor = result.goalsFor,
        goalsAgainst = result.goalsAgainst,
        perfect = result.isFlawless,
        finish = result.finish(),
        replay = seed?.let { session.replay(it, result) },
    )
    return CareerStats(
        modes = modes + (mode to totals.trimmed()),
        recent = (listOf(summary) + recent).take(RECENT_RUNS),
    )
}

private fun DraftSession.replay(seed: Long, result: RunResult) = RunReplay(
    seed = seed,
    settings = settings,
    picks = picks.mapValues { (_, pick) -> PickRef(pick.clubSeason.id, pick.player.id, pick.player.rating) },
    manager = manager,
    january = result.january?.event,
)

/**
 * The XI a stored run was played with, rebuilt from the squads. Null when a squad or player is no longer in
 * the game, since the run could not play the same without them.
 */
fun RunReplay.session(mode: DraftMode): DraftSession? = DraftSession(
    mode = mode,
    settings = settings,
    picks = picks.mapValues { (_, ref) -> ClubSeasons.pick(ref.squadId, ref.playerId, ref.rating) ?: return null },
    manager = manager,
)

/** A run or career season played again: the XI and every result. */
data class ReplayedRun(val session: DraftSession, val result: RunResult)

/** Player id to shirt name for everyone who could score: the XI, and a January signing when [result] has one. */
fun DraftSession.shortNames(result: RunResult? = null): Map<String, String> =
    (picks.values.map { it.player } + listOfNotNull(result?.january?.signed?.player)).associate { it.id to it.shortName }

/**
 * Plays a stored run again. Null when it can't be rebuilt, or when it no longer ends the way it was booked
 * (the squads or the simulator changed since), so a replay never shows a different story from the record.
 */
fun RunSummary.replayed(simulator: SeasonSimulator): ReplayedRun? {
    val stored = replay ?: return null
    val session = stored.session(mode)?.takeIf { it.isComplete } ?: return null
    val result = (simulator.simulate(session, stored.seed, stored.january) as? Simulation.Complete)?.result ?: return null
    val same = result.wins == won && result.draws == drawn && result.losses == lost && result.goalsFor == goalsFor
    return if (same) ReplayedRun(session, result) else null
}

/** [scoring] are the matches whose goals go on the scorers' leaderboard: the main competition's by default. */
internal fun RunResult.toTotals(session: DraftSession, scoring: List<MatchResult> = matches): ModeTotals {
    val highlights = RunHighlights.of(matches)
    val players = session.picks.values.map { it.player } + listOfNotNull(january?.signed?.player)
    val names = players.associate { it.id to it.name }
    val goals = scoring.flatMap { it.scorerIds }.groupingBy { it }.eachCount()
    return ModeTotals(
        runs = 1,
        trophies = trophies,
        perfectRuns = if (isFlawless) 1 else 0,
        won = wins,
        drawn = draws,
        lost = losses,
        goalsFor = goalsFor,
        goalsAgainst = goalsAgainst,
        cleanSheets = cleanSheets,
        biggestWin = highlights.biggestWin?.let { Scoreline(it.goalsFor, it.goalsAgainst, it.opponent.name) },
        longestWinStreak = highlights.longestWinStreak,
        longestUnbeatenRun = highlights.longestUnbeatenRun,
        scorers = goals.mapNotNull { (id, count) -> names[id]?.let { id to Tally(it, count) } }.toMap(),
        picks = session.picks.values.associate { it.player.id to Tally(it.player.name, 1) },
        squads = session.picks.values
            .map { it.clubSeason }
            .groupingBy { it }
            .eachCount()
            .entries
            .associate { (squad, count) -> squad.id to Tally("${squad.club} ${squad.season}", count) },
        formations = mapOf(session.formation to 1),
    )
}

private fun RunResult.finish(): Finish = when (val verdict = verdict) {
    Verdict.Champions -> Finish(trophy = true)
    is Verdict.LeagueFinish -> Finish(trophy = verdict.position == 1, position = verdict.position)
    is Verdict.Eliminated -> Finish(trophy = false, stage = verdict.stage.type, stageNumber = verdict.stage.number)
}

internal fun ModeTotals.trimmed() = copy(scorers = scorers.top(), picks = picks.top(), squads = squads.top())

private fun Map<String, Tally>.top(): Map<String, Tally> =
    if (size <= MAX_TALLIES) this
    else entries.sortedWith(compareByDescending<Map.Entry<String, Tally>> { it.value.count }.thenBy { it.value.name })
        .take(MAX_TALLIES)
        .associate { it.key to it.value }
