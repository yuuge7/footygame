package com.example.footygame.data

import com.example.footygame.models.Competition
import com.example.footygame.models.DraftMode
import com.example.footygame.models.CupRun
import com.example.footygame.models.MatchResult
import com.example.footygame.models.Opponent
import com.example.footygame.models.RunResult
import com.example.footygame.models.Stage
import com.example.footygame.models.StageType
import com.example.footygame.models.Venue
import com.example.footygame.models.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeRecordTest {

    @Test
    fun firstRunSetsTheBest() {
        val record = ModeRecord() + run(wins = 5, draws = 2, losses = 1)
        assertEquals(1, record.runs)
        assertEquals(5, record.bestWins)
        assertEquals(2, record.bestDraws)
        assertEquals(1, record.bestLosses)
    }

    @Test
    fun onlyBetterRunsReplaceTheBest() {
        val record = ModeRecord() + run(wins = 6, draws = 1, losses = 1)
        assertFalse(record.isImprovedBy(run(wins = 5, draws = 3, losses = 0)))
        assertTrue(record.isImprovedBy(run(wins = 6, draws = 2, losses = 0)))
        assertTrue(record.isImprovedBy(run(wins = 7, draws = 0, losses = 1)))

        val worse = record + run(wins = 2, draws = 0, losses = 6)
        assertEquals(2, worse.runs)
        assertEquals(6, worse.bestWins)
    }

    @Test
    fun perfectRunsAndTrophiesAreCounted() {
        val perfect = run(wins = 8, draws = 0, losses = 0, verdict = Verdict.Champions)
        assertTrue(perfect.isFlawless)
        val record = ModeRecord() + perfect + run(wins = 7, draws = 1, losses = 0, verdict = Verdict.Champions)
        assertEquals(1, record.perfectRuns)
        assertEquals(2, record.trophies)
    }

    @Test
    fun aEuropeanTrophyCountsToo() {
        val league = run(wins = 8, draws = 0, losses = 0, verdict = Verdict.LeagueFinish(position = 2, points = 24))
        val withCup = league.copy(europe = CupRun(Competition.EUROPA_LEAGUE, emptyList(), Verdict.Champions))
        assertEquals(0, (ModeRecord() + league).trophies)
        assertEquals(1, (ModeRecord() + withCup).trophies)
    }

    private fun run(
        wins: Int,
        draws: Int,
        losses: Int,
        verdict: Verdict = Verdict.Eliminated(Stage(StageType.FINAL), null),
    ): RunResult {
        val opponent = Opponent("Test", 80)
        val matches = List(wins) { MatchResult(Stage(StageType.GROUP_STAGE), opponent, Venue.NEUTRAL, 2, 0) } +
            List(draws) { MatchResult(Stage(StageType.GROUP_STAGE), opponent, Venue.NEUTRAL, 1, 1) } +
            List(losses) { MatchResult(Stage(StageType.GROUP_STAGE), opponent, Venue.NEUTRAL, 0, 1) }
        return RunResult(DraftMode.WC, matches, verdict)
    }
}
