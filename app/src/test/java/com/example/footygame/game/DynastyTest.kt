package com.example.footygame.game

import com.example.footygame.data.ClubSeasons
import com.example.footygame.models.Competition
import com.example.footygame.models.CupRun
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftPick
import com.example.footygame.models.DynastyPhase
import com.example.footygame.models.DynastyState
import com.example.footygame.models.Formation
import com.example.footygame.models.JanuaryEvent
import com.example.footygame.models.JanuaryOutcome
import com.example.footygame.models.ManagerTrait
import com.example.footygame.models.RunResult
import com.example.footygame.models.SeasonPhase
import com.example.footygame.models.Stage
import com.example.footygame.models.StageType
import com.example.footygame.models.Trophy
import com.example.footygame.models.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynastyTest {

    private val arsenal = ClubSeasons.squad("ars-2003")!!

    /** Arsenal 2003/04's Invincibles as a dynasty XI. */
    private fun invincibles(): DynastyState {
        val picks = Lineup.best(arsenal.players.map { DraftPick(it, arsenal) }, Formation.F433)
        val state = DynastyState(
            managerName = "Test",
            trait = ManagerTrait.TACTICIAN,
            formation = Formation.F433,
            seed = 7,
            squad = picks.map { (slot, pick) -> Dynasty.member(slot, pick, season = 1) },
        )
        return Dynasty.preseason(state)
    }

    private fun league(position: Int, cupWon: Boolean = false) = RunResult(
        mode = DraftMode.EPL,
        matches = emptyList(),
        verdict = Verdict.LeagueFinish(position, points = 90 - position * 3),
        cup = CupRun(Competition.FA_CUP, emptyList(), if (cupWon) Verdict.Champions else Verdict.Eliminated(Stage(StageType.FOURTH_ROUND), null)),
    )

    @Test
    fun theBoardAsksMoreOfBetterSquads() {
        assertEquals(1, Dynasty.target(90))
        assertEquals(4, Dynasty.target(86))
        assertEquals(7, Dynasty.target(82))
        assertEquals(10, Dynasty.target(78))
        assertEquals(17, Dynasty.target(70))
        assertEquals(Dynasty.target(TeamRatings.of(Dynasty.picks(invincibles().squad).values).overall), invincibles().target)
    }

    @Test
    fun beatingTheTargetAndWinningTrophiesLiftsTheBoard() {
        val start = invincibles().copy(target = 4, confidence = 60)
        val reviewed = Dynasty.review(start, league(position = 1, cupWon = true))
        assertEquals(SeasonPhase.REVIEW, reviewed.seasonPhase)
        assertEquals(listOf(Trophy.LEAGUE, Trophy.FA_CUP), reviewed.history.single().trophies)
        assertEquals(60 + 3 * 3 + 2 * 8, reviewed.confidence)
        assertFalse(reviewed.sacked)
    }

    @Test
    fun relegationOrALostDressingRoomIsTheSack() {
        val start = invincibles().copy(target = 10, confidence = 60)
        assertTrue(Dynasty.review(start, league(position = 18)).sacked)
        val shaky = start.copy(target = 1, confidence = 30)
        assertTrue(Dynasty.review(shaky, league(position = 8)).sacked)
        assertEquals(DynastyPhase.FINISHED, Dynasty.afterReview(Dynasty.review(shaky, league(position = 8)), null).phase)
    }

    @Test
    fun theTenthSeasonEndsTheDynasty() {
        val last = invincibles().copy(season = Dynasty.SEASONS, target = 17)
        val reviewed = Dynasty.review(last, league(position = 3))
        assertEquals(DynastyPhase.FINISHED, Dynasty.afterReview(reviewed, null).phase)
        val legacy = Dynasty.legacy(reviewed)
        assertEquals(1, legacy.seasons)
        assertFalse(legacy.sacked)
    }

    @Test
    fun summerAgesTheSquadAndFundsSignings() {
        val reviewed = Dynasty.review(invincibles().copy(target = 1), league(position = 1))
        val summer = Dynasty.summer(reviewed, null)
        assertEquals(DynastyPhase.SUMMER, summer.phase)
        // A trophy buys a third signing.
        assertTrue(summer.signingsLeft >= Dynasty.BASE_SIGNINGS + 1)
        assertTrue(summer.changes.isNotEmpty())
        summer.squad.forEach { member ->
            val before = reviewed.squad.first { it.slotId == member.slotId }
            assertTrue("${member.playerId} moved too far", kotlin.math.abs(member.rating - before.rating) <= 8)
        }
    }

    @Test
    fun retiredPlayersLeaveGapsThatSigningsMustFill() {
        val base = invincibles()
        // Pretend two players were signed from long before their careers began: they can't play on.
        val ancient = base.copy(squad = base.squad.mapIndexed { i, m -> if (i < 2) m.copy(fromYear = 1950) else m })
        val summer = Dynasty.summer(Dynasty.review(ancient.copy(target = 17), league(position = 5)), null)
        assertEquals(base.squad.size - 2, summer.squad.size)
        assertEquals(2, summer.changes.count { it.to == null })
        assertTrue(summer.signingsLeft >= 2)
    }

    @Test
    fun aJanuarySigningStaysForNextSeason() {
        val state = invincibles()
        val departed = Dynasty.picks(state.squad).getValue("ST").player
        val chelsea = ClubSeasons.squad("che-2004")!!
        val star = DraftPick(chelsea.players.first { it.id == "didier-drogba" }, chelsea)
        val result = league(position = 2).copy(
            january = JanuaryOutcome(JanuaryEvent.MARQUEE_SIGNING, success = true, afterMatches = 19, signed = star, departed = departed),
        )
        val summer = Dynasty.summer(Dynasty.review(state.copy(target = 4), result), result)
        val striker = summer.squad.firstOrNull { it.slotId == "ST" }
        assertEquals("didier-drogba", striker?.playerId)
        assertNull(summer.squad.firstOrNull { it.playerId == departed.id })
    }

    @Test
    fun signingsReplaceASlotAndTheNextSeasonResets() {
        val summer = Dynasty.summer(Dynasty.review(invincibles().copy(target = 17), league(position = 3)), null)
        val chelsea = ClubSeasons.squad("che-2004")!!
        val signed = Dynasty.sign(summer, "GK", DraftPick(chelsea.players.first { it.id == "petr-cech" }, chelsea))
        assertEquals("petr-cech", signed.squad.first { it.slotId == "GK" }.playerId)
        assertEquals(summer.signingsLeft - 1, signed.signingsLeft)
        assertEquals(summer.season + 1, signed.squad.first { it.slotId == "GK" }.joinedSeason)

        val next = Dynasty.nextSeason(signed)
        assertEquals(2, next.season)
        assertEquals(DynastyPhase.SEASON, next.phase)
        assertEquals(SeasonPhase.PRESEASON, next.seasonPhase)
        assertNull(next.january)
    }

    @Test
    fun aDynastySeasonPlaysWithCupAndEurope() {
        val state = invincibles().copy(seasonPhase = SeasonPhase.LEAGUE)
        val simulator = SeasonSimulator()
        // The January window pauses the season at halfway; any choice resumes it.
        val window = simulator.simulate(Dynasty.session(state), Dynasty.seasonSeed(state)) as Simulation.TransferWindow
        assertEquals(19, window.played.size)
        val step = simulator.simulate(Dynasty.session(state), Dynasty.seasonSeed(state), window.offers.first())
        val result = (step as Simulation.Complete).result
        assertEquals(38, result.matches.size)
        assertEquals(Competition.FA_CUP, result.cup?.competition)
    }
}
