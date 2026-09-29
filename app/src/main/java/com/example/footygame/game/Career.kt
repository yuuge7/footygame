package com.example.footygame.game

import com.example.footygame.models.CareerStats
import com.example.footygame.models.DraftSession
import com.example.footygame.models.Finish
import com.example.footygame.models.ModeTotals
import com.example.footygame.models.RunResult
import com.example.footygame.models.RunSummary
import com.example.footygame.models.Scoreline
import com.example.footygame.models.Tally
import com.example.footygame.models.Verdict

/** The record book remembers this many recent runs. */
const val RECENT_RUNS = 20

/** Each leaderboard keeps its top entries only, so years of play can't bloat the saved stats. */
const val MAX_TALLIES = 100

/** Adds one finished run to the record book. [session] is the XI that played it. */
fun CareerStats.withRun(result: RunResult, session: DraftSession): CareerStats {
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
    )
    return CareerStats(
        modes = modes + (mode to totals.trimmed()),
        recent = (listOf(summary) + recent).take(RECENT_RUNS),
    )
}

private fun RunResult.toTotals(session: DraftSession): ModeTotals {
    val highlights = RunHighlights.of(matches)
    val players = session.picks.values.map { it.player } + listOfNotNull(january?.signed?.player)
    val names = players.associate { it.id to it.name }
    val goals = matches.flatMap { it.scorerIds }.groupingBy { it }.eachCount()
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

private fun ModeTotals.trimmed() = copy(scorers = scorers.top(), picks = picks.top(), squads = squads.top())

private fun Map<String, Tally>.top(): Map<String, Tally> =
    if (size <= MAX_TALLIES) this
    else entries.sortedWith(compareByDescending<Map.Entry<String, Tally>> { it.value.count }.thenBy { it.value.name })
        .take(MAX_TALLIES)
        .associate { it.key to it.value }
