package com.example.footygame.viewmodel

import com.example.footygame.InMemoryRecordsStore
import com.example.footygame.InMemorySettingsStore
import com.example.footygame.game.DraftEngine
import com.example.footygame.game.SeasonSimulator
import com.example.footygame.models.Difficulty
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.DraftStyle
import com.example.footygame.models.Formation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val records = InMemoryRecordsStore()
    private val settings = InMemorySettingsStore()
    private lateinit var viewModel: GameViewModel

    private val quick = DraftSettings(managers = false, januaryWindow = false, europeanNights = false)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = GameViewModel(records, settings, DraftEngine(Random(21)), SeasonSimulator(), Random(21))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setupRemembersSettingsPerChallenge() {
        viewModel.openSetup(DraftMode.EPL)
        assertEquals(DraftSettings(), viewModel.uiState.value.setup!!.settings)

        val chosen = DraftSettings(formation = Formation.F352, difficulty = Difficulty.HARD)
        viewModel.updateSettings(chosen)
        assertEquals(chosen, settings.saved[DraftMode.EPL])

        viewModel.openSetup(DraftMode.UCL)
        assertEquals(DraftSettings(), viewModel.uiState.value.setup!!.settings)
        viewModel.openSetup(DraftMode.EPL)
        assertEquals(chosen, viewModel.uiState.value.setup!!.settings)
    }

    @Test
    fun draftingFillsTheXiWithTheChosenFormation() {
        startDraft(DraftMode.FAC, quick.copy(formation = Formation.F4231))
        draftEveryone()
        val draft = viewModel.uiState.value.draft!!
        assertTrue(draft.isComplete)
        assertEquals(Formation.F4231, draft.formation)
        assertFalse(viewModel.pick("anyone"))
    }

    @Test
    fun positionFirstDraftsThroughTheViewModel() {
        startDraft(DraftMode.WC, quick.copy(style = DraftStyle.POSITION_FIRST))
        assertTrue(viewModel.uiState.value.draft!!.awaitingSlotChoice)
        draftEveryone()
        assertTrue(viewModel.uiState.value.draft!!.isComplete)
    }

    @Test
    fun aGafferMustBeAppointedBeforePlaying() {
        startDraft(DraftMode.EPL, quick.copy(managers = true))
        draftEveryone()
        viewModel.simulate(animate = false)
        assertNull(viewModel.uiState.value.run)

        val option = viewModel.uiState.value.draft!!.managerOptions.first()
        viewModel.appointManager(option.id)
        viewModel.simulate(animate = false)
        assertEquals(option, viewModel.uiState.value.run!!.result!!.manager)
    }

    @Test
    fun simulatingSavesTheRecord() {
        startDraft(DraftMode.EPL, quick)
        draftEveryone()
        viewModel.simulate(animate = false)

        val run = viewModel.uiState.value.run
        assertNotNull(run)
        assertTrue(run!!.isRevealComplete)
        assertFalse(run.isNewBest)
        assertEquals(1, records.saved.getValue(DraftMode.EPL).runs)
        assertEquals(1, viewModel.uiState.value.records.getValue(DraftMode.EPL).runs)
    }

    @Test
    fun januaryWindowHoldsTheRunUntilAGambleIsChosen() {
        startDraft(DraftMode.EPL, quick.copy(januaryWindow = true))
        draftEveryone()
        viewModel.simulate(animate = false)

        val paused = viewModel.uiState.value.run!!
        assertTrue(paused.isAwaitingJanuary)
        assertNull(paused.result)
        assertEquals(19, paused.matches.size)
        assertEquals(3, paused.januaryOffers.size)
        assertTrue(records.saved.isEmpty())

        viewModel.chooseJanuary(paused.januaryOffers.first())
        val finished = viewModel.uiState.value.run!!
        assertTrue(finished.isRevealComplete)
        assertEquals(38, finished.matches.size)
        assertEquals(paused.matches, finished.matches.take(19))
        assertEquals(paused.januaryOffers.first(), finished.result!!.january!!.event)
        assertEquals(1, records.saved.getValue(DraftMode.EPL).runs)
    }

    @Test
    fun revealPlaysOutOverTimeAndCanBeSkipped() = runTest(dispatcher) {
        startDraft(DraftMode.WC, quick)
        draftEveryone()
        viewModel.simulate(animate = true)
        assertEquals(0, viewModel.uiState.value.run!!.revealed)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.run!!.isRevealComplete)

        viewModel.simulate(animate = true)
        assertEquals(0, viewModel.uiState.value.run!!.revealed)
        viewModel.skipReveal()
        assertTrue(viewModel.uiState.value.run!!.isRevealComplete)
        assertEquals(2, records.saved.getValue(DraftMode.WC).runs)
    }

    @Test
    fun revealStopsAtTheWindowAndCarriesOnAfterTheChoice() = runTest(dispatcher) {
        startDraft(DraftMode.EPL, quick.copy(januaryWindow = true))
        draftEveryone()
        viewModel.simulate(animate = true)
        advanceUntilIdle()
        val paused = viewModel.uiState.value.run!!
        assertTrue(paused.isAwaitingJanuary)
        assertEquals(19, paused.revealed)

        viewModel.chooseJanuary(paused.januaryOffers.first())
        assertEquals(19, viewModel.uiState.value.run!!.revealed)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.run!!.isRevealComplete)
        assertEquals(38, viewModel.uiState.value.run!!.revealed)
    }

    @Test
    fun newDraftClearsTheRunAndKeepsTheSetup() {
        startDraft(DraftMode.UCL, quick.copy(formation = Formation.F352))
        draftEveryone()
        viewModel.simulate(animate = false)
        viewModel.startDraft()

        val state = viewModel.uiState.value
        assertNull(state.run)
        assertEquals(Formation.F352, state.draft!!.formation)
        assertTrue(state.draft.picks.isEmpty())
    }

    @Test
    fun cannotSimulateAnIncompleteXi() {
        startDraft(DraftMode.EPL, quick)
        viewModel.simulate(animate = false)
        assertNull(viewModel.uiState.value.run)
        assertTrue(records.saved.isEmpty())
    }

    private fun startDraft(mode: DraftMode, chosen: DraftSettings) {
        viewModel.openSetup(mode)
        viewModel.updateSettings(chosen)
        viewModel.startDraft()
    }

    private fun draftEveryone() {
        var guard = 0
        while (viewModel.uiState.value.draft?.isComplete == false && guard++ < 60) {
            val draft = viewModel.uiState.value.draft!!
            if (draft.awaitingSlotChoice) {
                viewModel.chooseSlot(draft.emptySlots.first().id)
                continue
            }
            val player = draft.spin!!.players.first(draft::isEligible)
            assertTrue(viewModel.pick(player.id))
        }
    }
}
