package com.example.footygame.game

import com.example.footygame.completeSession
import com.example.footygame.models.CareerStats
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.Formation
import com.example.footygame.models.ModeTotals
import com.example.footygame.models.Scoreline
import com.example.footygame.models.Tally
import com.example.footygame.models.Verdict
import com.example.footygame.models.ranked
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerTest {

    private val quick = DraftSettings(managers = false, januaryWindow = false, europeanNights = false)
    private val simulator = SeasonSimulator()

    private fun play(mode: DraftMode, seed: Long, settings: DraftSettings = quick) =
        completeSession(mode, settings).let { session ->
            val result = (simulator.simulate(session, seed) as Simulation.Complete).result
            result to session
        }

    @Test
    fun aRunAddsItsMatchesGoalsAndPicks() {
        val (result, session) = play(DraftMode.WC, 7)
        val totals = CareerStats().withRun(result, session).totals(DraftMode.WC)

        assertEquals(1, totals.runs)
        assertEquals(result.matches.size, totals.played)
        assertEquals(result.wins, totals.won)
        assertEquals(result.goalsFor, totals.goalsFor)
        assertEquals(result.goalsFor, totals.scorers.values.sumOf { it.count })
        assertEquals(session.picks.size, totals.picks.size)
        assertEquals(session.picks.size, totals.squads.values.sumOf { it.count })
        assertEquals(mapOf(session.formation to 1), totals.formations)
    }

    @Test
    fun runsPileUpPerModeAndTheAllViewAddsThemTogether() {
        val (epl, eplSession) = play(DraftMode.EPL, 1)
        val (wc, wcSession) = play(DraftMode.WC, 2, quick.copy(formation = Formation.F352))
        val stats = CareerStats()
            .withRun(epl, eplSession)
            .withRun(epl, eplSession)
            .withRun(wc, wcSession)

        assertEquals(2, stats.totals(DraftMode.EPL).runs)
        assertEquals(1, stats.totals(DraftMode.WC).runs)
        val all = stats.totals(null)
        assertEquals(3, all.runs)
        assertEquals(epl.matches.size * 2 + wc.matches.size, all.played)
        assertEquals(2, all.formations[Formation.F433])
        assertEquals(1, all.formations[Formation.F352])
        // The same XI twice: each of its players was drafted twice.
        assertTrue(stats.totals(DraftMode.EPL).picks.values.all { it.count == 2 })
    }

    @Test
    fun recentRunsAreNewestFirstAndCapped() {
        val (result, session) = play(DraftMode.FAC, 3)
        var stats = CareerStats()
        repeat(RECENT_RUNS + 5) { stats = stats.withRun(result, session) }
        assertEquals(RECENT_RUNS, stats.recent.size)
        assertEquals(RECENT_RUNS + 5, stats.totals(DraftMode.FAC).runs)

        val (wc, wcSession) = play(DraftMode.WC, 4)
        assertEquals(DraftMode.WC, stats.withRun(wc, wcSession).recent.first().mode)
    }

    @Test
    fun finishRecordsLeaguePositionsAndKnockouts() {
        val (epl, eplSession) = play(DraftMode.EPL, 5)
        val eplFinish = CareerStats().withRun(epl, eplSession).recent.single().finish
        assertEquals((epl.verdict as Verdict.LeagueFinish).position, eplFinish.position)
        assertEquals(eplFinish.position == 1, eplFinish.trophy)

        val (cup, cupSession) = play(DraftMode.FAC, 6, quick)
        val cupFinish = CareerStats().withRun(cup, cupSession).recent.single().finish
        when (val verdict = cup.verdict) {
            Verdict.Champions -> assertTrue(cupFinish.trophy)
            is Verdict.Eliminated -> assertEquals(verdict.stage.type, cupFinish.stage)
            is Verdict.LeagueFinish -> error("a cup run can't finish in a league")
        }
    }

    @Test
    fun mergingKeepsTheBestRecordsAndSumsTallies() {
        val a = ModeTotals(
            biggestWin = Scoreline(4, 0, "Old"),
            longestWinStreak = 9,
            scorers = mapOf("x" to Tally("X", 3), "y" to Tally("Y", 1)),
        )
        val b = ModeTotals(
            biggestWin = Scoreline(5, 1, "New"),
            longestWinStreak = 4,
            scorers = mapOf("y" to Tally("Y", 5)),
        )
        val merged = a + b
        assertEquals("New", merged.biggestWin!!.opponent)
        assertEquals(9, merged.longestWinStreak)
        assertEquals(listOf(Tally("Y", 6), Tally("X", 3)), merged.scorers.ranked())
    }

    @Test
    fun statsSurviveAJsonRoundTrip() {
        val (result, session) = play(DraftMode.EPL, 9)
        val stats = CareerStats().withRun(result, session)
        val json = Json { ignoreUnknownKeys = true }
        assertEquals(stats, json.decodeFromString<CareerStats>(json.encodeToString(stats)))
    }

    @Test
    fun leaderboardsKeepOnlyTheirTopEntries() {
        val crowded = (1..MAX_TALLIES + 30).associate { "p$it" to Tally("Player $it", it) }
        val (result, session) = play(DraftMode.WC, 8)
        val stats = CareerStats(mapOf(DraftMode.WC to ModeTotals(picks = crowded))).withRun(result, session)
        val picks = stats.totals(DraftMode.WC).picks
        assertEquals(MAX_TALLIES, picks.size)
        assertTrue(picks.containsKey("p${MAX_TALLIES + 30}"))
    }
}
