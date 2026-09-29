package com.example.footygame.game

import com.example.footygame.completeSession
import com.example.footygame.data.ClubSeasons
import com.example.footygame.data.Leagues
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.League
import com.example.footygame.models.Position
import com.example.footygame.models.ProPhase
import com.example.footygame.models.StageType
import com.example.footygame.models.Verdict
import com.example.footygame.models.isLeagueTitle
import com.example.footygame.models.seasonTrophies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaguesTest {

    private val simulator = SeasonSimulator()

    @Test
    fun everyLeagueHasAFullFieldAndSquadDataToJoin() {
        val expectedClubs = mapOf(
            League.LA_LIGA to 10, League.SERIE_A to 8, League.BUNDESLIGA to 10, League.LIGUE_1 to 8,
            League.EREDIVISIE to 3, League.PRIMEIRA_LIGA to 3, League.SCOTTISH_PREMIERSHIP to 2, League.SUPER_LIG to 3,
            League.RUSSIAN_PREMIER_LEAGUE to 4, League.UKRAINIAN_PREMIER_LEAGUE to 2, League.GREEK_SUPER_LEAGUE to 2,
        )
        val dataClubs = ClubSeasons.all.map { it.club }.distinct().groupBy { Leagues.leagueOf(it) }
        League.entries.forEach { league ->
            val teams = Leagues.teams(league)
            assertEquals(league.name, league.size, teams.size)
            assertEquals(league.name, teams.size, teams.map { it.name }.toSet().size)
            assertTrue(league.name, league.europe.isNotEmpty() && league.europe.first().isEuropean)
            assertTrue(league.title.isLeagueTitle)
            expectedClubs[league]?.let { assertEquals("${league.name} clubs in the data", it, dataClubs[league]?.size) }
            assertTrue(league.name, ProCareer.clubsIn(league, 2010).isNotEmpty())
        }
        assertTrue(dataClubs.getValue(League.PREMIER_LEAGUE).size > 40)
    }

    @Test
    fun aSeasonAbroadPlaysThatLeaguesSizeCupAndEuropeanPlaces() {
        val base = completeSession(DraftMode.EPL, DraftSettings(managers = false, januaryWindow = false))
        League.entries.filter { it != League.PREMIER_LEAGUE }.forEach { league ->
            val home = Leagues.teams(league).first().name
            val session = base.copy(settings = base.settings.copy(league = league, homeClub = home, domesticCup = true))
            repeat(8) { seed ->
                val result = (simulator.simulate(session, seed.toLong()) as Simulation.Complete).result
                assertEquals(league, result.league)
                assertEquals(league.name, league.matches, result.matches.size)
                assertTrue(result.matches.none { it.opponent.name == home })
                assertEquals(league.size, result.table.size)

                val cup = result.cup!!
                assertEquals(league.cup, cup.competition)
                assertEquals(StageType.ROUND_OF_32, cup.matches.first().stage.type)
                assertTrue(cup.matches.none { it.opponent.name == home })
                if (cup.won) assertEquals(StageType.FINAL, cup.matches.last().stage.type)

                val position = (result.verdict as Verdict.LeagueFinish).position
                val earned = league.europe.getOrNull(position - 1)
                assertEquals(earned, result.europe?.competition)
                result.europe?.let { europe -> assertTrue(europe.matches.none { it.opponent.name == home }) }
                if (position == 1) assertTrue(league.title in result.seasonTrophies)
            }
        }
    }

    @Test
    fun theHomeClubIsKeptOutOfEnglishFixturesToo() {
        val base = completeSession(DraftMode.EPL, DraftSettings(managers = false, januaryWindow = false))
        val session = base.copy(settings = base.settings.copy(homeClub = "Arsenal", domesticCup = true))
        repeat(10) { seed ->
            val result = (simulator.simulate(session, seed.toLong()) as Simulation.Complete).result
            assertEquals(38, result.matches.size)
            val all = result.matches + result.cup!!.matches + result.europe?.matches.orEmpty()
            assertTrue(all.none { it.opponent.name == "Arsenal" })
        }
    }

    @Test
    fun aCareerCanStartAbroadAndMoveBetweenLeagues() {
        val spanish = ProCareer.newCareer("Test", Position.MID, 2005, seed = 4, league = League.LA_LIGA)
        assertTrue(spanish.offers.isNotEmpty())
        spanish.offers.forEach { assertEquals(League.LA_LIGA, it.league) }
        val started = ProCareer.chooseFirstClub(spanish, spanish.offers.first())
        assertEquals(League.LA_LIGA, started.league)
        assertEquals(ProPhase.SEASON, started.phase)

        // Across many summers, offers come from more than one league.
        val leagues = (0 until 30).flatMap { seed ->
            val star = ProCareer.newCareer("Test", Position.ATT, 2008, seed.toLong()).copy(club = "Arsenal", rating = 84)
            ProCareer.transfers(star).offers.map { it.league }
        }.toSet()
        assertTrue("offers only from $leagues", leagues.size >= 3)

        // Youngsters too: big clubs abroad sign prospects well above their current level.
        val prospectLeagues = (0 until 30).flatMap { seed ->
            val kid = ProCareer.newCareer("Test", Position.ATT, 2004, seed.toLong()).copy(club = "Blackpool", age = 18, rating = 68)
            ProCareer.transfers(kid).offers.map { it.league }
        }.toSet()
        assertTrue("prospect offers only from $prospectLeagues", prospectLeagues.any { it != League.PREMIER_LEAGUE })
    }

    @Test
    fun seasonsAbroadAreBookedWithTheirLeague() {
        val fresh = ProCareer.newCareer("Test", Position.ATT, 2010, seed = 8, league = League.BUNDESLIGA)
        val start = ProCareer.chooseFirstClub(fresh, fresh.offers.first()).copy(rating = 90)
        val squad = ProCareer.clubSquad(start.club, start.year)!!
        val result = (simulator.simulate(ProCareer.session(start, squad), ProCareer.seasonSeed(start)) as Simulation.Complete).result
        assertEquals(League.BUNDESLIGA, result.league)
        assertEquals(34, result.matches.size)
        val report = ProCareer.report(start, squad, result)
        assertEquals(League.BUNDESLIGA, report.league)
        assertNull(report.trophies.firstOrNull { it == League.PREMIER_LEAGUE.title })
    }
}
