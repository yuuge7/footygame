package com.example.footygame.game

import com.example.footygame.data.ClubSeasons
import com.example.footygame.models.DraftPick
import com.example.footygame.models.Formation
import com.example.footygame.models.Position
import com.example.footygame.models.ProPhase
import com.example.footygame.models.ProState
import com.example.footygame.models.SeasonPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ProCareerTest {

    private val simulator = SeasonSimulator()

    private fun newCareer(position: Position = Position.ATT, seed: Long = 3) =
        ProCareer.newCareer("Jamie Test", position, startYear = 2003, seed = seed)

    @Test
    fun aNewCareerIsASeventeenYearOldWithThreeModestClubs() {
        repeat(10) { seed ->
            val state = newCareer(seed = seed.toLong())
            assertEquals(ProCareer.START_AGE, state.age)
            assertTrue(state.rating in 60..64)
            assertTrue(state.potential in 80..94)
            assertEquals(ProPhase.FIRST_CLUB, state.phase)
            assertEquals(3, state.offers.size)
            val strengths = ProCareer.clubsIn(state.year).map { DraftEngine.strength(it) }.sorted()
            val median = strengths[strengths.size / 2]
            state.offers.forEach { assertTrue("${it.club} ${it.strength}", it.strength <= median + 1) }
        }
    }

    @Test
    fun onlyAGoodEnoughPlayerMakesTheXi() {
        val state = newCareer()
        val arsenal = ClubSeasons.squad("ars-2003")!!
        assertFalse(ProCareer.starts(state, arsenal))
        assertTrue(ProCareer.starts(state.copy(rating = 99), arsenal))
        assertEquals(11, ProCareer.lineup(state, arsenal).size)
    }

    @Test
    fun growthStopsAtPotentialAndAgeBringsDecline() {
        val random = Random(1)
        repeat(50) {
            assertTrue(ProCareer.develop(age = 18, rating = 79, potential = 80, starter = true, random = random) <= 80)
            assertTrue(ProCareer.develop(age = 35, rating = 80, potential = 90, starter = true, random = random) < 80)
        }
    }

    @Test
    fun theBodyForcesRetirementAtThirtyEight() {
        val veteran = newCareer().copy(age = 37, rating = 80)
        assertTrue(ProCareer.canRetire(veteran))
        assertFalse(ProCareer.mustRetire(veteran))
        assertTrue(ProCareer.mustRetire(veteran.copy(age = 38)))
        assertFalse(ProCareer.canRetire(newCareer()))
    }

    @Test
    fun aWholeCareerPlaysThroughToRetirement() {
        var state: ProState = ProCareer.chooseFirstClub(newCareer(), newCareer().offers.first())
        var guard = 0
        while (state.phase != ProPhase.RETIRED && guard++ < 30) {
            assertEquals(ProPhase.SEASON, state.phase)
            val squad = ProCareer.clubSquad(state.club, state.year)!!
            val result = (simulator.simulate(ProCareer.session(state, squad), ProCareer.seasonSeed(state)) as Simulation.Complete).result
            val report = ProCareer.report(state, squad, result)
            if (report.starter) {
                val all = result.matches + result.cup?.matches.orEmpty() + result.europe?.matches.orEmpty()
                assertEquals(all.size, report.appearances)
                assertEquals(all.sumOf { m -> m.scorerIds.count { it == ProCareer.PLAYER_ID } }, report.goals)
            } else {
                assertTrue(report.appearances <= 19)
            }
            state = ProCareer.review(state.copy(seasonPhase = SeasonPhase.LEAGUE), report)
            assertEquals(SeasonPhase.REVIEW, state.seasonPhase)
            state = ProCareer.afterReview(state)
            if (state.phase == ProPhase.TRANSFERS) {
                assertTrue(state.offers.size <= 3)
                assertTrue(state.offers.none { it.club == state.club })
                state = ProCareer.nextSeason(state, state.offers.firstOrNull()?.club ?: state.club)
            }
        }
        assertEquals(ProPhase.RETIRED, state.phase)
        assertTrue(state.age in 34..38)
        val legacy = ProCareer.legacy(state)
        assertEquals(state.history.size, legacy.seasons)
        assertTrue(legacy.peakRating >= 64)
        assertTrue(legacy.clubs.isNotEmpty())
    }
}

class LineupTest {

    @Test
    fun aRealSquadFillsEveryFormation() {
        val squad = ClubSeasons.squad("liv-2019")!!
        val candidates = squad.players.map { DraftPick(it, squad) }
        Formation.entries.forEach { formation ->
            val xi = Lineup.best(candidates, formation)
            assertEquals(formation.label, 11, xi.size)
            assertEquals(11, xi.values.map { it.player.id }.toSet().size)
            formation.slots.forEach { slot -> assertTrue(xi.getValue(slot.id).player.position in slot.accepts) }
        }
    }

    @Test
    fun theBestKeeperStarts() {
        val squad = ClubSeasons.squad("liv-2019")!!
        val xi = Lineup.best(squad.players.map { DraftPick(it, squad) }, Formation.F433)
        val bestKeeper = squad.players.filter { it.position == Position.GK }.maxBy { it.rating }
        assertEquals(bestKeeper.id, xi.getValue("GK").player.id)
    }
}
