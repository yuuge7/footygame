package com.example.footygame.viewmodel

import com.example.footygame.InMemoryRecordsStore
import com.example.footygame.InMemorySettingsStore
import com.example.footygame.InMemoryStatsStore
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
    private val stats = InMemoryStatsStore()
    private lateinit var viewModel: GameViewModel

    private val quick = DraftSettings(managers = false, januaryWindow = false, europeanNights = false)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // One choice for the whole app, so every test starts with matches waiting for a tap.
        AutoPlay.set(false)
        viewModel = GameViewModel(records, settings, stats, DraftEngine(Random(21)), SeasonSimulator(), Random(21))
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
        viewModel.simulate()
        assertNull(viewModel.uiState.value.run)

        val option = viewModel.uiState.value.draft!!.managerOptions.first()
        viewModel.appointManager(option.id)
        viewModel.simulate()
        assertEquals(option, viewModel.uiState.value.run!!.result!!.manager)
    }

    @Test
    fun simulatingSavesTheRecord() {
        startDraft(DraftMode.EPL, quick)
        draftEveryone()
        viewModel.simulate()

        // Decided and saved at once, while every match still waits to be shown.
        val run = viewModel.uiState.value.run
        assertNotNull(run)
        assertEquals(0, run!!.revealed)
        assertFalse(run.isRevealComplete)
        assertFalse(run.isNewBest)
        assertEquals(1, records.saved.getValue(DraftMode.EPL).runs)
        assertEquals(1, viewModel.uiState.value.records.getValue(DraftMode.EPL).runs)

        val totals = stats.saved.totals(DraftMode.EPL)
        assertEquals(1, totals.runs)
        assertEquals(38, totals.played)
        assertEquals(11, totals.picks.values.sumOf { it.count })
        assertEquals(run.result!!.goalsFor, totals.scorers.values.sumOf { it.count })
        assertEquals(stats.saved, viewModel.uiState.value.stats)
    }

    @Test
    fun januaryWindowHoldsTheRunUntilAGambleIsChosen() {
        startDraft(DraftMode.EPL, quick.copy(januaryWindow = true))
        draftEveryone()
        viewModel.simulate()
        viewModel.skipReveal()

        val paused = viewModel.uiState.value.run!!
        assertTrue(paused.isAwaitingJanuary)
        assertNull(paused.result)
        assertEquals(19, paused.matches.size)
        assertEquals(3, paused.januaryOffers.size)
        assertTrue(records.saved.isEmpty())

        viewModel.chooseJanuary(paused.januaryOffers.first())
        viewModel.skipReveal()
        val finished = viewModel.uiState.value.run!!
        assertTrue(finished.isRevealComplete)
        assertEquals(38, finished.matches.size)
        assertEquals(paused.matches, finished.matches.take(19))
        assertEquals(paused.januaryOffers.first(), finished.result!!.january!!.event)
        assertEquals(1, records.saved.getValue(DraftMode.EPL).runs)
    }

    @Test
    fun theSeasonNeverJumpsToTheWindowOrTheEndOnItsOwn() = runTest(dispatcher) {
        startDraft(DraftMode.EPL, quick.copy(januaryWindow = true))
        draftEveryone()
        viewModel.simulate()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.run!!.revealed)

        // The window opens with the tap that shows the 19th match, not before.
        repeat(18) { viewModel.nextMatch() }
        assertFalse(viewModel.uiState.value.run!!.isAwaitingJanuary)
        viewModel.chooseJanuary(viewModel.uiState.value.run!!.januaryOffers.first())
        assertNull(viewModel.uiState.value.run!!.result)
        viewModel.nextMatch()
        val paused = viewModel.uiState.value.run!!
        assertTrue(paused.isAwaitingJanuary)

        // The second half is decided with the gamble, and still shown one tap at a time.
        viewModel.chooseJanuary(paused.januaryOffers.first())
        advanceUntilIdle()
        assertEquals(19, viewModel.uiState.value.run!!.revealed)
        assertFalse(viewModel.uiState.value.run!!.isRevealComplete)
        repeat(18) { viewModel.nextMatch() }
        assertFalse(viewModel.uiState.value.run!!.isRevealComplete)
        viewModel.nextMatch()
        assertTrue(viewModel.uiState.value.run!!.isRevealComplete)
    }

    @Test
    fun revealGoesMatchByMatchUntilAutoPlayTakesOver() = runTest(dispatcher) {
        startDraft(DraftMode.WC, quick)
        draftEveryone()
        viewModel.simulate()
        advanceUntilIdle()
        // Nothing moves on its own: each match waits for a tap.
        assertEquals(0, viewModel.uiState.value.run!!.revealed)
        viewModel.nextMatch()
        viewModel.nextMatch()
        assertEquals(2, viewModel.uiState.value.run!!.revealed)

        viewModel.toggleAutoPlay()
        assertTrue(AutoPlay.on.value)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.run!!.isRevealComplete)

        // Auto play stays chosen for the next run, and Skip still jumps to the end.
        viewModel.simulate()
        assertTrue(AutoPlay.on.value)
        assertEquals(0, viewModel.uiState.value.run!!.revealed)
        viewModel.skipReveal()
        assertTrue(viewModel.uiState.value.run!!.isRevealComplete)
        assertEquals(2, records.saved.getValue(DraftMode.WC).runs)
    }

    @Test
    fun nextMatchNeverRunsPastWhatIsKnown() {
        startDraft(DraftMode.WC, quick)
        draftEveryone()
        viewModel.simulate()
        val total = viewModel.uiState.value.run!!.matches.size
        repeat(total + 3) { viewModel.nextMatch() }
        assertEquals(total, viewModel.uiState.value.run!!.revealed)
        assertTrue(viewModel.uiState.value.run!!.isRevealComplete)
    }

    @Test
    fun aRecentRunPlaysBackMatchForMatch() {
        startDraft(DraftMode.EPL, quick.copy(januaryWindow = true, europeanNights = true))
        draftEveryone()
        viewModel.simulate()
        viewModel.skipReveal()
        val paused = viewModel.uiState.value.run!!
        viewModel.chooseJanuary(paused.januaryOffers.last())
        val played = viewModel.uiState.value.run!!.result!!

        val summary = viewModel.uiState.value.stats.recent.first()
        val replay = viewModel.replay(summary)!!
        assertEquals(played.matches, replay.result.matches)
        assertEquals(played.table, replay.result.table)
        assertEquals(played.europe, replay.result.europe)
        assertEquals(played.january, replay.result.january)
    }

    @Test
    fun revealStopsAtTheWindowAndCarriesOnAfterTheChoice() = runTest(dispatcher) {
        startDraft(DraftMode.EPL, quick.copy(januaryWindow = true))
        draftEveryone()
        viewModel.simulate()
        viewModel.toggleAutoPlay()
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
        viewModel.simulate()
        viewModel.startDraft()

        val state = viewModel.uiState.value
        assertNull(state.run)
        assertEquals(Formation.F352, state.draft!!.formation)
        assertTrue(state.draft.picks.isEmpty())
    }

    @Test
    fun cannotSimulateAnIncompleteXi() {
        startDraft(DraftMode.EPL, quick)
        viewModel.simulate()
        assertNull(viewModel.uiState.value.run)
        assertTrue(records.saved.isEmpty())
        assertEquals(0, stats.saved.recent.size)
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
