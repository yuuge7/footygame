package com.example.footygame.viewmodel

import com.example.footygame.data.CareerStore
import com.example.footygame.game.DraftEngine
import com.example.footygame.game.Dynasty
import com.example.footygame.game.ProCareer
import com.example.footygame.game.SeasonSimulator
import com.example.footygame.models.DynastyPhase
import com.example.footygame.models.DynastyState
import com.example.footygame.models.Formation
import com.example.footygame.models.Legacy
import com.example.footygame.models.ManagerTrait
import com.example.footygame.models.Position
import com.example.footygame.models.ProPhase
import com.example.footygame.models.ProState
import com.example.footygame.models.SeasonPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class InMemoryCareerStore : CareerStore {
    var dynasty: DynastyState? = null
    var pro: ProState? = null
    var legacies: List<Legacy> = emptyList()
    override fun loadDynasty() = dynasty
    override fun saveDynasty(state: DynastyState?) {
        dynasty = state
    }
    override fun loadPro() = pro
    override fun savePro(state: ProState?) {
        pro = state
    }
    override fun loadLegacies() = legacies
    override fun saveLegacies(legacies: List<Legacy>) {
        this.legacies = legacies
    }
}

class CareerViewModelTest {

    private val store = InMemoryCareerStore()

    private fun viewModel() = CareerViewModel(store, SeasonSimulator(), DraftEngine(Random(4)), DraftEngine(Random(5)), Random(9))

    private fun CareerViewModel.draftXi() {
        var guard = 0
        while (uiState.value.dynastyDraft?.isComplete == false && guard++ < 40) {
            val draft = uiState.value.dynastyDraft!!
            if (draft.awaitingSlotChoice) {
                chooseDynastySlot(draft.emptySlots.first().id)
                continue
            }
            val player = draft.spin!!.players.first(draft::isEligible)
            pickDynasty(player.id, null)
        }
    }

    /** Plays every step of the season on screen, choosing the first January gamble. */
    private fun CareerViewModel.playDynastySeason() {
        kickOffDynasty()
        uiState.value.dynastySeason?.takeIf { it.awaitingJanuary }?.let { chooseDynastyJanuary(it.januaryOffers.first()) }
        var guard = 0
        while (uiState.value.dynasty!!.seasonPhase != SeasonPhase.REVIEW && guard++ < 5) advanceDynasty()
    }

    @Test
    fun aDynastyDraftsPlaysASeasonAndOpensTheSummer() {
        val vm = viewModel()
        vm.startDynasty("Gaffer", ManagerTrait.TACTICIAN, Formation.F433)
        assertEquals(DynastyPhase.DRAFT, vm.uiState.value.dynasty!!.phase)
        vm.draftXi()
        vm.confirmDynastyDraft()

        val started = vm.uiState.value.dynasty!!
        assertEquals(DynastyPhase.SEASON, started.phase)
        assertEquals(SeasonPhase.PRESEASON, started.seasonPhase)
        assertEquals(11, started.squad.size)
        assertNull(vm.uiState.value.dynastyDraft)

        vm.kickOffDynasty()
        val window = vm.uiState.value.dynastySeason!!
        assertTrue(window.awaitingJanuary)
        assertEquals(19, window.league.size)
        vm.chooseDynastyJanuary(window.januaryOffers.first())
        assertEquals(38, vm.uiState.value.dynastySeason!!.league.size)

        var guard = 0
        while (vm.uiState.value.dynasty!!.seasonPhase != SeasonPhase.REVIEW && guard++ < 5) vm.advanceDynasty()
        assertEquals(1, vm.uiState.value.dynasty!!.history.size)

        if (!vm.uiState.value.dynasty!!.sacked) {
            vm.closeDynastyReview()
            val summer = vm.uiState.value.dynasty!!
            assertEquals(DynastyPhase.SUMMER, summer.phase)
            assertTrue(summer.signingsLeft >= Dynasty.BASE_SIGNINGS)

            val slot = summer.squad.first().slotId
            vm.startSigning(slot)
            assertNotNull(vm.uiState.value.dynastyDraft?.spin)
            vm.draftXi()
            vm.confirmDynastyDraft()
            assertEquals(summer.signingsLeft - 1, vm.uiState.value.dynasty!!.signingsLeft)

            // Fill any retirement gaps, then kick on.
            while (vm.uiState.value.dynasty!!.squad.size < 11) {
                val empty = Formation.F433.slots.first { s -> vm.uiState.value.dynasty!!.squad.none { it.slotId == s.id } }
                vm.startSigning(empty.id)
                vm.draftXi()
                vm.confirmDynastyDraft()
            }
            vm.startNextDynastySeason()
            assertEquals(2, vm.uiState.value.dynasty!!.season)
        }
    }

    @Test
    fun aSavedSeasonReplaysTheSameAfterARestart() {
        val vm = viewModel()
        vm.startDynasty("Gaffer", ManagerTrait.DEFENSIVE, Formation.F442)
        vm.draftXi()
        vm.confirmDynastyDraft()
        vm.playDynastySeason()
        val before = vm.uiState.value.dynastySeason!!.result!!

        val restarted = viewModel()
        assertEquals(before, restarted.uiState.value.dynastySeason!!.result)
        assertEquals(store.dynasty, restarted.uiState.value.dynasty)
    }

    @Test
    fun walkingAwayClearsTheDynasty() {
        val vm = viewModel()
        vm.startDynasty("Gaffer", ManagerTrait.ATTACKING, Formation.F433)
        vm.endDynasty()
        assertNull(store.dynasty)
        assertNull(vm.uiState.value.dynasty)
    }

    @Test
    fun aPlayerCareerRunsASeasonAndMovesOn() {
        val vm = viewModel()
        vm.startPro("Jamie Test", Position.MID, 2011)
        val fresh = vm.uiState.value.pro!!
        assertEquals(ProPhase.FIRST_CLUB, fresh.phase)
        vm.chooseFirstClub(fresh.offers.first())
        assertEquals(fresh.offers.first().club, vm.uiState.value.pro!!.club)

        vm.kickOffPro()
        assertNotNull(vm.uiState.value.proSeason?.result)
        var guard = 0
        while (vm.uiState.value.pro!!.seasonPhase != SeasonPhase.REVIEW && guard++ < 5) vm.advancePro()
        val reviewed = vm.uiState.value.pro!!
        assertEquals(1, reviewed.history.size)
        assertEquals(ProCareer.START_AGE, reviewed.age)

        vm.closeProReview()
        val summer = vm.uiState.value.pro!!
        assertEquals(ProPhase.TRANSFERS, summer.phase)
        assertEquals(ProCareer.START_AGE + 1, summer.age)
        vm.signPro(null)
        val next = vm.uiState.value.pro!!
        assertEquals(2, next.season)
        assertEquals(reviewed.club, next.club)
        assertEquals(SeasonPhase.PRESEASON, next.seasonPhase)
    }
}
