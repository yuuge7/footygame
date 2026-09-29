package com.example.footygame.game

import com.example.footygame.models.ClubSeason
import com.example.footygame.models.Player
import com.example.footygame.models.Pool
import com.example.footygame.models.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerCurveTest {

    private fun squad(year: Int, pool: Pool = Pool.ENGLISH_CLUB, vararg players: Pair<String, Int>) = ClubSeason(
        id = "tst-$year-${pool.name}",
        club = "Test",
        startYear = year,
        code = "TST",
        primaryColor = 0xFFFFFFFF,
        secondaryColor = 0xFF000000,
        pool = pool,
        players = players.map { (id, rating) -> Player(id, id, id, Position.MID, rating) },
    )

    private val curve = CareerCurve(
        listOf(
            squad(2000, Pool.ENGLISH_CLUB, "star" to 70, "kid" to 60),
            squad(2004, Pool.ENGLISH_CLUB, "star" to 86),
            squad(2020, Pool.ENGLISH_CLUB, "prospect" to 78),
            squad(2021, Pool.ENGLISH_CLUB, "prospect" to 80),
            // A World Cup ends the season that started the year before.
            squad(2006, Pool.NATIONAL_TEAM, "keeper" to 84),
        ),
    )

    @Test
    fun knownSeasonsAreExactAndGapsInterpolate() {
        assertEquals(70, curve.ratingIn("star", 2000))
        assertEquals(78, curve.ratingIn("star", 2002))
        assertEquals(86, curve.ratingIn("star", 2004))
    }

    @Test
    fun pastTheDataThePeakHoldsThenFadesThenRetires() {
        // Debut 2000 counts as age 21: steady through 29, fading from 30.
        assertEquals(86, curve.ratingIn("star", 2008))
        assertEquals(84, curve.ratingIn("star", 2009))
        assertTrue(curve.ratingIn("star", 2014)!! < 80)
        assertNull(curve.ratingIn("star", 2017))
    }

    @Test
    fun youngPlayersKeepImprovingPastTheirLastSeason() {
        // Debut 2020 counts as age 21, so 2022 is 23: the last year of growth.
        assertEquals(81, curve.ratingIn("prospect", 2022))
        assertEquals(81, curve.ratingIn("prospect", 2026))
    }

    @Test
    fun worldCupsCountForTheSeasonBefore() {
        assertEquals(84, curve.ratingIn("keeper", 2005))
    }

    @Test
    fun unknownPlayersAndHopelessRatingsHaveNoSeason() {
        assertNull(curve.ratingIn("nobody", 2005))
        // Five years before a 60-rated debut would be 50: too weak to be a pro.
        assertNull(curve.ratingIn("kid", 1995))
    }

    @Test
    fun realDataFollowsARealCareer() {
        val ronaldo = "cristiano-ronaldo"
        val early = CareerCurve.standard.ratingIn(ronaldo, 2004)
        val peak = CareerCurve.standard.ratingIn(ronaldo, 2007)
        assertTrue("$early -> $peak", early != null && peak != null && peak >= early)
    }
}
