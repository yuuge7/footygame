package com.example.footygame.game

import com.example.footygame.completeSession
import com.example.footygame.models.Competition
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.JanuaryEvent
import com.example.footygame.models.Manager
import com.example.footygame.models.ManagerTrait
import com.example.footygame.models.Outcome
import com.example.footygame.models.RunResult
import com.example.footygame.models.StageType
import com.example.footygame.models.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonSimulatorTest {

    private val plain = DraftSettings(managers = false, europeanNights = false, januaryWindow = false)
    private val elite = DraftMode.entries.associateWith { completeSession(it, plain, strongest = true) }
    private val weak = DraftMode.entries.associateWith { completeSession(it, plain, strongest = false) }
    private val simulator = SeasonSimulator()

    private fun complete(session: DraftSession, seed: Long, january: JanuaryEvent? = null): RunResult =
        (simulator.simulate(session, seed, january) as Simulation.Complete).result

    @Test
    fun premierLeagueSeasonPlaysEveryoneTwice() {
        repeat(20) { seed ->
            val result = complete(elite.getValue(DraftMode.EPL), seed.toLong())
            assertEquals(38, result.matches.size)
            assertEquals(20, result.table.size)
            result.table.forEach { assertEquals(38, it.played) }
            assertEquals(19, result.matches.map { it.opponent.name }.toSet().size)
            result.matches.groupBy { it.opponent.name }.values.forEach { pair ->
                assertEquals(2, pair.size)
                assertTrue(pair[0].venue != pair[1].venue)
            }
            val verdict = result.verdict as Verdict.LeagueFinish
            val user = result.table[verdict.position - 1]
            assertTrue(user.isUser)
            assertEquals(user.points, verdict.points)
            assertEquals(result.wins * 3 + result.draws, verdict.points)
            assertNull(result.europe)
            assertNull(result.january)
        }
    }

    @Test
    fun cupRunsEndWhereTheVerdictSays() {
        for (mode in listOf(DraftMode.UCL, DraftMode.WC, DraftMode.FAC)) {
            for (session in listOf(elite.getValue(mode), weak.getValue(mode))) {
                repeat(60) { seed ->
                    val result = complete(session, seed.toLong())
                    assertTrue(result.matches.size <= mode.matches)
                    when (val verdict = result.verdict) {
                        Verdict.Champions -> {
                            assertEquals(mode.matches, result.matches.size)
                            assertEquals(StageType.FINAL, result.matches.last().stage.type)
                        }
                        is Verdict.Eliminated -> {
                            assertEquals(verdict.stage.type, result.matches.last().stage.type)
                            assertTrue(result.matches.size < mode.matches || verdict.stage.type == StageType.FINAL)
                        }
                        is Verdict.LeagueFinish -> error("Cups don't finish in a league position")
                    }
                }
            }
        }
    }

    @Test
    fun knockoutsAlwaysProduceAWinner() {
        repeat(80) { seed ->
            for (mode in listOf(DraftMode.UCL, DraftMode.WC, DraftMode.FAC)) {
                complete(weak.getValue(mode), seed.toLong()).matches
                    .filter { it.stage.type.isKnockout && it.stage.leg != 1 }
                    .forEach { match ->
                        val (scored, conceded) = match.aggregate ?: (match.goalsFor to match.goalsAgainst)
                        if (scored == conceded) {
                            assertTrue(match.extraTime)
                            val penalties = match.penalties
                            assertNotNull(penalties)
                            assertTrue(penalties!!.first != penalties.second)
                        }
                    }
            }
        }
    }

    @Test
    fun everyGoalHasAScorerFromTheXi() {
        val session = elite.getValue(DraftMode.EPL)
        val ids = session.picks.values.map { it.player.id }.toSet()
        val result = complete(session, 11)
        result.matches.forEach { match ->
            assertEquals(match.goalsFor, match.scorerIds.size)
            assertTrue(ids.containsAll(match.scorerIds))
        }
        assertTrue(result.topScorers.zipWithNext().all { (a, b) -> a.goals >= b.goals })
    }

    @Test
    fun sameSeedSameSeason() {
        val session = elite.getValue(DraftMode.UCL)
        assertEquals(complete(session, 5), complete(session, 5))
    }

    @Test
    fun strongerTeamsWinMoreButPerfectionStaysRare() {
        val eliteWins = averageWins(elite.getValue(DraftMode.EPL))
        val weakWins = averageWins(weak.getValue(DraftMode.EPL))
        println("EPL average wins: elite=$eliteWins weak=$weakWins")
        assertTrue(eliteWins > weakWins + 8)
        assertTrue("elite XI should dominate but not always win", eliteWins in 27.0..36.5)

        val perfectRate = (0 until 400).count { complete(elite.getValue(DraftMode.EPL), it.toLong()).isFlawless } / 400.0
        println("EPL perfect rate for the best possible XI: $perfectRate")
        assertTrue(perfectRate < 0.25)
    }

    @Test
    fun flawlessMeansEveryScheduledMatchWon() {
        val session = elite.getValue(DraftMode.FAC)
        repeat(100) { seed ->
            val result = complete(session, seed.toLong())
            val allWon = result.matches.size == DraftMode.FAC.matches && result.matches.all { it.outcome == Outcome.WIN }
            assertEquals(allWon, result.isFlawless)
            if (result.isFlawless) assertTrue(result.wonTrophy)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompleteXiCannotPlay() {
        simulator.simulate(DraftSession(DraftMode.EPL), seed = 1)
    }

    @Test
    fun weakTeamsCanFallInTheGroupStage() {
        val verdicts = (0 until 200).map { complete(weak.getValue(DraftMode.WC), it.toLong()).verdict }
        assertFalse(verdicts.all { it == Verdict.Champions })
    }

    @Test
    fun transferWindowPausesAtHalfwayAndResumesIdentically() {
        val session = elite.getValue(DraftMode.EPL).withSettings { copy(januaryWindow = true) }
        val pause = simulator.simulate(session, 42) as Simulation.TransferWindow
        assertEquals(19, pause.played.size)
        assertEquals(3, pause.offers.toSet().size)

        pause.offers.forEach { offer ->
            val result = complete(session, 42, offer)
            assertEquals(pause.played, result.matches.take(19))
            assertEquals(38, result.matches.size)
            assertEquals(offer, result.january!!.event)
            assertEquals(19, result.january.afterMatches)
        }

        val notOffered = JanuaryEvent.entries.first { it !in pause.offers }
        assertTrue(simulator.simulate(session, 42, notOffered) is Simulation.TransferWindow)
    }

    @Test
    fun marqueeSigningChangesWhoCanScore() {
        val session = elite.getValue(DraftMode.EPL).withSettings { copy(januaryWindow = true) }
        val seed = (0L until 200L).first { seed ->
            (simulator.simulate(session, seed) as Simulation.TransferWindow).offers.contains(JanuaryEvent.MARQUEE_SIGNING)
        }
        val result = complete(session, seed, JanuaryEvent.MARQUEE_SIGNING)
        val january = result.january!!
        val signed = january.signed
        if (signed != null) {
            val departed = january.departed!!
            val secondHalfIds = session.picks.values.map { it.player.id }.toSet() - departed.id + signed.player.id
            assertTrue(result.matches.drop(19).flatMap { it.scorerIds }.all { it in secondHalfIds })
            assertTrue(signed.player.id !in session.picks.values.map { it.player.id })
        }
    }

    @Test
    fun cupWindowsOpenAtTheirJanuaryPoint() {
        val faCup = elite.getValue(DraftMode.FAC).withSettings { copy(januaryWindow = true) }
        val faPause = (0L until 50L).map { simulator.simulate(faCup, it) }.filterIsInstance<Simulation.TransferWindow>().first()
        assertEquals(8, faPause.played.size)
        assertEquals(StageType.SECOND_ROUND, faPause.played.last().stage.type)

        val ucl = elite.getValue(DraftMode.UCL).withSettings { copy(januaryWindow = true) }
        val uclPause = (0L until 50L).map { simulator.simulate(ucl, it) }.filterIsInstance<Simulation.TransferWindow>().first()
        assertEquals(8, uclPause.played.size)
        assertTrue(uclPause.played.all { it.stage.type == StageType.LEAGUE_PHASE })

        val worldCup = elite.getValue(DraftMode.WC).withSettings { copy(januaryWindow = true) }
        repeat(30) { assertTrue(simulator.simulate(worldCup, it.toLong()) is Simulation.Complete) }
    }

    @Test
    fun bigGameManagersWinMore() {
        val base = elite.getValue(DraftMode.UCL)
        val withGaffer = base.copy(manager = Manager("test", "Test", ManagerTrait.BIG_GAMES))
        val plainWins = (0L until 200L).sumOf { complete(base, it).wins }
        val gafferWins = (0L until 200L).sumOf { complete(withGaffer, it).wins }
        assertTrue("with $gafferWins vs without $plainWins", gafferWins > plainWins)
        assertEquals(withGaffer.manager, complete(withGaffer, 1).manager)
    }

    @Test
    fun leagueFinishDecidesTheEuropeanCompetition() {
        val sessions = listOf(elite.getValue(DraftMode.EPL), weak.getValue(DraftMode.EPL))
            .map { session -> session.withSettings { copy(europeanNights = true) } }
        var sawEurope = false
        for (session in sessions) {
            repeat(60) { seed ->
                val result = complete(session, seed.toLong())
                val position = (result.verdict as Verdict.LeagueFinish).position
                val europe = result.europe
                val expected = when {
                    position <= 4 -> Competition.CHAMPIONS_LEAGUE
                    position == 5 -> Competition.EUROPA_LEAGUE
                    position <= 7 -> Competition.CONFERENCE_LEAGUE
                    else -> null
                }
                assertEquals("position $position", expected, europe?.competition)
                if (europe != null) {
                    sawEurope = true
                    assertTrue(europe.matches.isNotEmpty())
                    assertEquals(38, result.matches.size)
                    assertTrue(europe.verdict is Verdict.Champions || europe.verdict is Verdict.Eliminated)
                }
            }
        }
        assertTrue(sawEurope)
    }

    private fun DraftSession.withSettings(change: DraftSettings.() -> DraftSettings) = copy(settings = settings.change())

    private fun averageWins(session: DraftSession): Double = (0 until 200).map { complete(session, it.toLong()).wins }.average()
}
