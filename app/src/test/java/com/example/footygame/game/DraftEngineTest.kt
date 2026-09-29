package com.example.footygame.game

import com.example.footygame.data.ClubSeasons
import com.example.footygame.models.ClubSeason
import com.example.footygame.models.Difficulty
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.DraftStyle
import com.example.footygame.models.EraPreset
import com.example.footygame.models.EraRange
import com.example.footygame.models.Formation
import com.example.footygame.models.RatingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DraftEngineTest {

    private fun squad(id: String) = ClubSeasons.all.first { it.id == id }

    @Test
    fun startingRespinsFollowDifficulty() {
        Difficulty.entries.forEach { difficulty ->
            val session = DraftEngine(Random(1)).start(DraftMode.EPL, DraftSettings(difficulty = difficulty))
            assertNotNull(session.spin)
            assertEquals(1, session.spinNumber)
            assertEquals(difficulty.respins, session.respinsLeft)
        }
    }

    @Test
    fun everyModeFormationStyleAndEraDraftsToCompletion() {
        for (mode in DraftMode.entries) {
            for (formation in Formation.entries) {
                for (style in DraftStyle.entries) {
                    for (preset in listOf(EraPreset.ALL_TIME, EraPreset.MODERN)) {
                        val settings = DraftSettings(formation = formation, style = style, era = ClubSeasons.eraFor(mode, preset))
                        repeat(3) { seed ->
                            val done = draftToCompletion(DraftEngine(Random(seed)), mode, settings)
                            val label = "$mode ${formation.label} $style $preset seed $seed"
                            assertTrue(label, done.isComplete)
                            assertNull(done.spin)
                            assertEquals(label, 11, done.picks.values.map { it.player.id }.toSet().size)
                            done.formation.slots.forEach { slot ->
                                assertTrue(label, done.picks.getValue(slot.id).player.position in slot.accepts)
                            }
                            if (settings.era != null) {
                                assertTrue(label, done.picks.values.all { it.clubSeason.startYear in settings.era })
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun positionFirstWaitsForASlotThenFillsExactlyThatSlot() {
        val engine = DraftEngine(Random(4))
        var session = engine.start(DraftMode.EPL, DraftSettings(style = DraftStyle.POSITION_FIRST))
        assertNull(session.spin)
        assertTrue(session.awaitingSlotChoice)

        session = engine.chooseSlot(session, "GK")
        assertEquals("GK", session.targetSlotId)
        val spin = session.spin!!
        val keeper = spin.players.first(session::isEligible)
        assertEquals("GK", session.slotFor(keeper, preferredSlotId = "ST")?.id)

        session = engine.pick(session, keeper.id, preferredSlotId = "ST")!!
        assertEquals(keeper.id, session.picks.getValue("GK").player.id)
        assertNull(session.spin)
        assertTrue(session.awaitingSlotChoice)
        assertEquals(session, engine.chooseSlot(session, "GK"))
    }

    @Test
    fun positionFirstRespinKeepsTheSlot() {
        val engine = DraftEngine(Random(9))
        var session = engine.start(DraftMode.UCL, DraftSettings(style = DraftStyle.POSITION_FIRST, difficulty = Difficulty.EASY))
        session = engine.chooseSlot(session, "ST")
        repeat(3) {
            session = engine.respin(session)
            assertEquals("ST", session.targetSlotId)
            assertTrue(session.spin!!.players.any(session::isEligible))
        }
        assertEquals(0, session.respinsLeft)
    }

    @Test
    fun squadFirstIgnoresSlotChoice() {
        val engine = DraftEngine(Random(0))
        val session = engine.start(DraftMode.EPL)
        assertEquals(session, engine.chooseSlot(session, "GK"))
    }

    @Test
    fun respinUsesAllowanceAndChangesSquad() {
        val engine = DraftEngine(Random(3))
        var session = engine.start(DraftMode.UCL, DraftSettings(difficulty = Difficulty.EASY))
        repeat(Difficulty.EASY.respins) {
            val before = session.spin
            session = engine.respin(session)
            assertNotEquals(before?.id, session.spin?.id)
        }
        assertEquals(0, session.respinsLeft)
        assertEquals(session, engine.respin(session))

        val hard = engine.start(DraftMode.UCL, DraftSettings(difficulty = Difficulty.HARD))
        assertEquals(hard, engine.respin(hard))
    }

    @Test
    fun hardHidesRatingsUntilTheXiIsComplete() {
        val settings = DraftSettings(difficulty = Difficulty.HARD, managers = false)
        val engine = DraftEngine(Random(2))
        val started = engine.start(DraftMode.WC, settings)
        assertFalse(started.ratingsVisible)
        assertTrue(draftToCompletion(engine, DraftMode.WC, settings).ratingsVisible)
        assertFalse(DraftSession(DraftMode.WC, DraftSettings(showRatings = false)).ratingsVisible)
    }

    @Test
    fun cannotDraftTheSamePersonTwice() {
        val engine = DraftEngine(Random(0), poolFor = { _, _ -> listOf(squad("ars-2003"), squad("ars-1997")) })
        val session = DraftSession(DraftMode.EPL, spin = squad("ars-2003"))
        val afterVieira = engine.pick(session, "patrick-vieira")!!
        assertNull(engine.pick(afterVieira.copy(spin = squad("ars-1997")), "patrick-vieira"))
    }

    @Test
    fun rejectsPlayersNotInTheSpinOrWithoutAFreeSlot() {
        val engine = DraftEngine(Random(0))
        assertNull(engine.pick(engine.start(DraftMode.EPL), "not-a-player"))

        val afterEderson = engine.pick(DraftSession(DraftMode.EPL, spin = squad("mci-2022")), "ederson")!!
        assertNull(engine.pick(afterEderson.copy(spin = squad("liv-2019")), "alisson"))
    }

    @Test
    fun preferredSlotIsUsedWhenThePlayerFits() {
        val session = DraftSession(DraftMode.EPL, spin = squad("liv-2019"))
        val engine = DraftEngine(Random(0))
        assertEquals("andy-robertson", engine.pick(session, "andy-robertson", "LB")!!.picks.getValue("LB").player.id)
        assertNull(engine.pick(session, "andy-robertson", "ST"))
    }

    @Test
    fun widePlayersFallBackToFlexibleSlots() {
        val arsenal = squad("ars-2003")
        val engine = DraftEngine(Random(0))
        var session = DraftSession(DraftMode.EPL, DraftSettings(formation = Formation.F442), spin = arsenal)
        session = engine.pick(session, "thierry-henry")!!.copy(spin = arsenal)
        session = engine.pick(session, "dennis-bergkamp")!!.copy(spin = arsenal)
        val reyes = engine.pick(session, "jose-antonio-reyes")!!
        val slot = reyes.picks.entries.first { it.value.player.id == "jose-antonio-reyes" }.key
        assertTrue(slot == "LM" || slot == "RM")
    }

    @Test
    fun managersAreOfferedAndMustBeAppointedWhenEnabled() {
        val engine = DraftEngine(Random(5))
        val withGaffer = draftToCompletion(engine, DraftMode.EPL, DraftSettings())
        assertEquals(3, withGaffer.managerOptions.size)
        assertTrue(withGaffer.needsManager)
        assertFalse(withGaffer.isReadyToPlay)
        assertEquals(withGaffer, engine.appointManager(withGaffer, "not-a-manager"))

        val appointed = engine.appointManager(withGaffer, withGaffer.managerOptions.first().id)
        assertEquals(withGaffer.managerOptions.first(), appointed.manager)
        assertTrue(appointed.isReadyToPlay)

        val noGaffer = draftToCompletion(engine, DraftMode.EPL, DraftSettings(managers = false))
        assertTrue(noGaffer.managerOptions.isEmpty())
        assertTrue(noGaffer.isReadyToPlay)
    }

    @Test
    fun anEmptyEraFallsBackToEverySeasonInsteadOfGettingStuck() {
        val engine = DraftEngine(Random(0), poolFor = { mode, settings ->
            if (settings.era != null) emptyList() else ClubSeasons.poolFor(mode, settings)
        })
        val session = engine.start(DraftMode.EPL, DraftSettings(era = EraRange(2024, 2024)))
        assertNotNull(session.spin)
    }

    @Test
    fun primeRatingsReachTheDraft() {
        val session = DraftEngine(Random(0)).start(DraftMode.WC, DraftSettings(ratingMode = RatingMode.PRIME))
        val prime = ClubSeasons.poolFor(DraftMode.WC, DraftSettings(ratingMode = RatingMode.PRIME))
            .first { it.id == session.spin!!.id }
        assertEquals(prime.players, session.spin!!.players)
    }

    @Test
    fun englishModesLeanTowardsStrongSquads() {
        for (mode in listOf(DraftMode.EPL, DraftMode.FAC)) {
            val pool = ClubSeasons.poolFor(mode)
            val strong = { squad: ClubSeason -> DraftEngine.strength(squad) >= STRONG }
            val uniformShare = pool.count(strong) / pool.size.toDouble()

            val engine = DraftEngine(Random(11))
            var session = engine.start(mode, DraftSettings(difficulty = Difficulty.EASY))
            var strongSpins = 0
            repeat(SPINS) {
                if (strong(session.spin!!)) strongSpins++
                session = engine.respin(session.copy(respinsLeft = 1))
            }
            val share = strongSpins / SPINS.toDouble()
            assertTrue("$mode strong share $share vs uniform $uniformShare", share > uniformShare * 1.5)
            // Still a lean, not a lock: most spins are not title sides.
            assertTrue("$mode strong share $share", share < 0.45)
        }
    }

    @Test
    fun otherModesSpinEverySquadEvenly() {
        assertFalse(DraftMode.UCL.favoursStrongSquads)
        assertFalse(DraftMode.WC.favoursStrongSquads)
        assertTrue(DraftMode.EPL.favoursStrongSquads)
        assertTrue(DraftMode.FAC.favoursStrongSquads)
    }

    @Test
    fun teamRatingsReflectPicks() {
        val barcelona = squad("bar-2010")
        val engine = DraftEngine(Random(0), poolFor = { _, _ -> listOf(barcelona) })
        var session = DraftSession(DraftMode.UCL, spin = barcelona)
        session = engine.pick(session, "lionel-messi")!!.copy(spin = barcelona)
        session = engine.pick(session, "xavi")!!
        val ratings = TeamRatings.of(session.picks.values)
        assertEquals(95, ratings.overall)
        assertEquals(1, ratings.chemistry)
        assertTrue(ratings.attack > ratings.defence)
        assertEquals(TeamRatings.EMPTY, TeamRatings.of(emptyList()))
    }

    private fun draftToCompletion(engine: DraftEngine, mode: DraftMode, settings: DraftSettings): DraftSession {
        var session = engine.start(mode, settings)
        var guard = 0
        while (!session.isComplete && guard++ < 60) {
            if (session.awaitingSlotChoice) {
                session = engine.chooseSlot(session, session.emptySlots.first().id)
                continue
            }
            val spin = session.spin ?: break
            session = engine.pick(session, spin.players.first(session::isEligible).id)!!
        }
        return session
    }

    private companion object {
        const val STRONG = 84.0
        const val SPINS = 2_000
    }
}
