package com.example.footygame.game

import com.example.footygame.completeSession
import com.example.footygame.models.Award
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.DynastySeason
import com.example.footygame.models.League
import com.example.footygame.models.Legacy
import com.example.footygame.models.LegacyKind
import com.example.footygame.models.ManagerTotals
import com.example.footygame.models.PlayerTotals
import com.example.footygame.models.ProSeason
import com.example.footygame.models.Trophy
import org.junit.Assert.assertEquals
import org.junit.Test

class CareerTotalsTest {

    private fun dynastySeason(position: Int, target: Int, trophies: List<Trophy> = emptyList()) = DynastySeason(
        season = 1, position = position, points = 70, won = 20, drawn = 10, lost = 8, target = target,
        trophies = trophies, confidenceBefore = 60, confidenceAfter = 60,
    )

    private fun proSeason(club: String, goals: Int, starter: Boolean, awards: List<Award> = emptyList()) = ProSeason(
        season = 1, age = 20, club = club, squadId = "x-2000", league = League.LA_LIGA, starter = starter,
        appearances = 30, goals = goals, ratingBefore = 80, ratingAfter = 83, position = 3,
        trophies = listOf(Trophy.LA_LIGA), awards = awards,
    )

    @Test
    fun dynastySeasonsAddUpLikeRunsWithTheirTrophiesAndTargets() {
        val session = completeSession(DraftMode.EPL, DraftSettings(managers = false, januaryWindow = false, europeanNights = false))
        val result = (SeasonSimulator().simulate(session, 5) as Simulation.Complete).result
        val totals = ManagerTotals()
            .withSeason(result, session, dynastySeason(position = 3, target = 4, trophies = listOf(Trophy.FA_CUP)))
            .withSeason(result, session, dynastySeason(position = 6, target = 4, trophies = listOf(Trophy.FA_CUP, Trophy.LEAGUE)))
            .withLegacy(Legacy(LegacyKind.MANAGER, "Gaffer", seasons = 2, sacked = true))

        assertEquals(2, totals.seasons.runs)
        assertEquals(result.wins * 2, totals.seasons.won)
        assertEquals(mapOf(Trophy.FA_CUP to 2, Trophy.LEAGUE to 1), totals.trophies)
        assertEquals(3, totals.bestFinish)
        assertEquals(1, totals.targetsMet)
        assertEquals(1, totals.dynasties)
        assertEquals(1, totals.sackings)
        assertEquals(2, totals.seasons.formations.values.sum())
    }

    @Test
    fun playerSeasonsAddUpGoalsHonoursAndClubs() {
        val totals = PlayerTotals()
            .withSeason(proSeason("Sevilla", goals = 12, starter = false))
            .withSeason(proSeason("Sevilla", goals = 25, starter = true, awards = listOf(Award.GOLDEN_BOOT)))
            .withSeason(proSeason("Valencia", goals = 18, starter = true, awards = listOf(Award.GOLDEN_BOOT)))
            .withLegacy()

        assertEquals(3, totals.seasons)
        assertEquals(2, totals.starterSeasons)
        assertEquals(55, totals.goals)
        assertEquals(90, totals.appearances)
        assertEquals(25, totals.bestSeasonGoals)
        assertEquals(83, totals.peakRating)
        assertEquals(mapOf(Trophy.LA_LIGA to 3), totals.trophies)
        assertEquals(mapOf(Award.GOLDEN_BOOT to 2), totals.awards)
        assertEquals(2, totals.clubs.getValue("Sevilla").count)
        assertEquals(1, totals.clubs.getValue("Valencia").count)
        assertEquals(mapOf(League.LA_LIGA to 3), totals.leagues)
        assertEquals(1, totals.careers)
    }
}
