package com.example.footygame.data

import com.example.footygame.data.squads.shortNameOf
import com.example.footygame.data.squads.slug
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.EraPreset
import com.example.footygame.models.EraRange
import com.example.footygame.models.Formation
import com.example.footygame.models.Pool
import com.example.footygame.models.Position
import com.example.footygame.models.RatingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubSeasonsTest {

    @Test
    fun everySquadCanFillEveryFormation() {
        ClubSeasons.all.forEach { squad ->
            val counts = squad.players.groupingBy { it.position }.eachCount()
            assertTrue("${squad.id} needs a goalkeeper", (counts[Position.GK] ?: 0) >= 1)
            assertTrue("${squad.id} needs defenders", (counts[Position.DEF] ?: 0) >= 3)
            assertTrue("${squad.id} needs midfielders", (counts[Position.MID] ?: 0) >= 3)
            assertTrue("${squad.id} needs forwards", (counts[Position.ATT] ?: 0) >= 2)
        }
    }

    @Test
    fun idsAndCodesAreWellFormed() {
        assertEquals(ClubSeasons.all.size, ClubSeasons.all.map { it.id }.toSet().size)
        ClubSeasons.all.forEach { squad ->
            assertEquals("${squad.id} has a duplicate player", squad.players.size, squad.players.map { it.id }.toSet().size)
            assertEquals(3, squad.code.length)
            squad.players.forEach { assertTrue("${it.name} rating", it.rating in 55..99) }
        }
    }

    @Test
    fun eachCodeBelongsToOneClub() {
        val clubsByCode = ClubSeasons.all.groupBy({ it.code }, { it.club })
        clubsByCode.forEach { (code, clubs) -> assertEquals("code $code", 1, clubs.toSet().size) }
    }

    @Test
    fun samePersonAlwaysHasTheSameName() {
        val namesById = ClubSeasons.all.flatMap { it.players }.groupBy({ it.id }, { it.name })
        namesById.forEach { (id, names) -> assertEquals("id $id", 1, names.toSet().size) }
    }

    @Test
    fun namesakesKeepSeparateIds() {
        val freds = ClubSeasons.all.flatMap { it.players }.filter { it.name == "Fred" }.map { it.id }.toSet()
        assertEquals(setOf("fred", "fred-1983"), freds)
    }

    @Test
    fun poolsAreBigAndMatchTheirModes() {
        assertTrue(ClubSeasons.poolFor(DraftMode.EPL).size >= 250)
        assertTrue(ClubSeasons.poolFor(DraftMode.UCL).size >= 190)
        assertTrue(ClubSeasons.poolFor(DraftMode.WC).size >= 300)
        assertTrue(ClubSeasons.poolFor(DraftMode.WC).all { it.pool == Pool.NATIONAL_TEAM })
        val english = setOf(Pool.ENGLISH_CLUB, Pool.ENGLISH_DOMESTIC)
        assertTrue(ClubSeasons.poolFor(DraftMode.EPL).all { it.pool in english })
        assertEquals(ClubSeasons.poolFor(DraftMode.EPL), ClubSeasons.poolFor(DraftMode.FAC))
        assertTrue(ClubSeasons.poolFor(DraftMode.UCL).none { it.pool == Pool.NATIONAL_TEAM || it.pool == Pool.ENGLISH_DOMESTIC })
    }

    @Test
    fun everyPremierLeagueSeasonIsCovered() {
        assertEquals((1992..2024).toList(), ClubSeasons.years(DraftMode.EPL))
    }

    @Test
    fun everyEraPresetHoldsEnoughSquads() {
        for (mode in DraftMode.entries) {
            for (preset in EraPreset.entries) {
                val count = ClubSeasons.squadCount(mode, ClubSeasons.eraFor(mode, preset))
                assertTrue("$mode $preset has $count squads", count >= ClubSeasons.MIN_SQUADS)
            }
        }
    }

    @Test
    fun eraPresetsStartAtTheFirstSeasonOnOrAfterTheirYear() {
        assertNull(ClubSeasons.eraFor(DraftMode.EPL, EraPreset.ALL_TIME))
        assertEquals(EraRange(2016, 2024), ClubSeasons.eraFor(DraftMode.EPL, EraPreset.MODERN))
        assertEquals(EraRange(2018, 2022), ClubSeasons.eraFor(DraftMode.WC, EraPreset.MODERN))
        assertEquals(EraRange(2002, 2022), ClubSeasons.eraFor(DraftMode.WC, EraPreset.SINCE_2000))
    }

    @Test
    fun eraNarrowsThePoolAndFullRangeMeansAllTime() {
        val era = EraRange(2010, 2015)
        val pool = ClubSeasons.poolFor(DraftMode.EPL, DraftSettings(era = era))
        assertTrue(pool.isNotEmpty())
        assertTrue(pool.all { it.startYear in era })
        assertNull(ClubSeasons.normalise(DraftMode.EPL, EraRange(1992, 2024)))
        assertEquals(era, ClubSeasons.normalise(DraftMode.EPL, era))
    }

    @Test
    fun primeRatingsAreEachPlayersCareerBest() {
        val best = ClubSeasons.all.flatMap { it.players }.groupBy { it.id }.mapValues { (_, all) -> all.maxOf { it.rating } }
        val prime = ClubSeasons.poolFor(DraftMode.WC, DraftSettings(ratingMode = RatingMode.PRIME))
        prime.flatMap { it.players }.forEach { assertEquals(it.name, best.getValue(it.id), it.rating) }
        val henry1998 = prime.first { it.id == "fra-1998" }.players.first { it.id == "thierry-henry" }
        assertEquals(96, henry1998.rating)
    }

    @Test
    fun everyFormationHasElevenUniqueSlots() {
        Formation.entries.forEach { formation ->
            assertEquals(formation.label, 11, formation.slots.size)
            assertEquals(formation.label, 11, formation.slots.map { it.id }.toSet().size)
            assertEquals(formation.label, 1, formation.slots.count { Position.GK in it.accepts })
            assertEquals(formation.label, formation.label.split('-').sumOf { it.toInt() }, 10)
        }
    }

    @Test
    fun shortNamesKeepSurnameParticles() {
        assertEquals("De Bruyne", shortNameOf("Kevin De Bruyne"))
        assertEquals("van der Sar", shortNameOf("Edwin van der Sar"))
        assertEquals("Mac Allister", shortNameOf("Alexis Mac Allister"))
        assertEquals("Henry", shortNameOf("Thierry Henry"))
        assertEquals("Xavi", shortNameOf("Xavi"))
    }

    @Test
    fun slugsStripAccents() {
        assertEquals("lukasz-piszczek", slug("Łukasz Piszczek"))
        assertEquals("n-golo-kante", slug("N'Golo Kanté"))
        assertEquals("ilkay-gundogan", slug("İlkay Gündoğan"))
        assertEquals("ole-gunnar-solskjaer", slug("Ole Gunnar Solskjær"))
        assertEquals("hermann-hreidarsson", slug("Hermann Hreiðarsson"))
        assertEquals("gary-neville", slug("Gary Neville"))
    }
}
