package com.example.footygame.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.footygame.data.ModeRecord
import com.example.footygame.data.RecordsStore
import com.example.footygame.data.SettingsStore
import com.example.footygame.data.SharedPreferencesRecordsStore
import com.example.footygame.data.SharedPreferencesSettingsStore
import com.example.footygame.game.DraftEngine
import com.example.footygame.game.SeasonSimulator
import com.example.footygame.game.Simulation
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.JanuaryEvent
import com.example.footygame.models.MatchResult
import com.example.footygame.models.RunResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class SetupState(val mode: DraftMode, val settings: DraftSettings)

data class RunState(
    val mode: DraftMode,
    /** Replaying the same seed is how a run resumes after the transfer window. */
    val seed: Long,
    /** Matches known so far: the whole run, or the first half while the transfer window is open. */
    val matches: List<MatchResult>,
    val result: RunResult? = null,
    val januaryOffers: List<JanuaryEvent> = emptyList(),
    /** How many matches the results screen has revealed so far. */
    val revealed: Int = 0,
    val isNewBest: Boolean = false,
) {
    val isAwaitingJanuary: Boolean get() = result == null && revealed >= matches.size
    val isRevealComplete: Boolean get() = result != null && revealed >= matches.size
}

data class GameUiState(
    val records: Map<DraftMode, ModeRecord> = emptyMap(),
    val setup: SetupState? = null,
    val draft: DraftSession? = null,
    val run: RunState? = null,
)

class GameViewModel(
    private val recordsStore: RecordsStore,
    private val settingsStore: SettingsStore,
    private val draftEngine: DraftEngine,
    private val simulator: SeasonSimulator,
    private val seeds: Random = Random.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(records = recordsStore.load()))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var revealJob: Job? = null
    private var animateReveal = true

    /** Loads the last setup used for this challenge. */
    fun openSetup(mode: DraftMode) {
        _uiState.update { it.copy(setup = SetupState(mode, settingsStore.load(mode))) }
    }

    fun updateSettings(settings: DraftSettings) {
        val setup = _uiState.value.setup ?: return
        settingsStore.save(setup.mode, settings)
        _uiState.update { it.copy(setup = setup.copy(settings = settings)) }
    }

    fun startDraft() {
        val setup = _uiState.value.setup ?: return
        cancelReveal()
        _uiState.update { it.copy(draft = draftEngine.start(setup.mode, setup.settings), run = null) }
    }

    fun chooseSlot(slotId: String) = updateDraft { draftEngine.chooseSlot(it, slotId) }

    fun respin() = updateDraft { draftEngine.respin(it) }

    fun appointManager(managerId: String) = updateDraft { draftEngine.appointManager(it, managerId) }

    /** Returns false when the player can't go into the XI (already drafted, or no fitting slot). */
    fun pick(playerId: String, slotId: String? = null): Boolean {
        val draft = _uiState.value.draft ?: return false
        val updated = draftEngine.pick(draft, playerId, slotId) ?: return false
        _uiState.update { it.copy(draft = updated) }
        return true
    }

    /** Plays the XI through its competition. The run may stop at the January window for [chooseJanuary]. */
    fun simulate(animate: Boolean) {
        val draft = _uiState.value.draft?.takeIf { it.isReadyToPlay } ?: return
        cancelReveal()
        animateReveal = animate
        val seed = seeds.nextLong()
        advance(simulator.simulate(draft, seed), RunState(draft.mode, seed, matches = emptyList()))
    }

    fun chooseJanuary(event: JanuaryEvent) {
        val state = _uiState.value
        val draft = state.draft ?: return
        val run = state.run?.takeIf { it.isAwaitingJanuary && event in it.januaryOffers } ?: return
        advance(simulator.simulate(draft, run.seed, event), run)
    }

    fun skipReveal() {
        cancelReveal()
        _uiState.update { state -> state.copy(run = state.run?.let { it.copy(revealed = it.matches.size) }) }
    }

    private fun advance(step: Simulation, run: RunState) {
        when (step) {
            is Simulation.TransferWindow -> _uiState.update {
                it.copy(
                    run = run.copy(
                        matches = step.played,
                        januaryOffers = step.offers,
                        revealed = if (animateReveal) run.revealed else step.played.size,
                    ),
                )
            }

            is Simulation.Complete -> finish(step.result, run)
        }
        if (animateReveal) startReveal()
    }

    /** Saves the record as soon as the run is decided, whatever the reveal is doing. */
    private fun finish(result: RunResult, run: RunState) {
        val previous = _uiState.value.records[result.mode] ?: ModeRecord()
        val updated = previous + result
        recordsStore.save(result.mode, updated)
        _uiState.update {
            it.copy(
                records = it.records + (result.mode to updated),
                run = run.copy(
                    matches = result.matches,
                    result = result,
                    januaryOffers = emptyList(),
                    revealed = if (animateReveal) run.revealed else result.matches.size,
                    isNewBest = previous.runs > 0 && previous.isImprovedBy(result),
                ),
            )
        }
    }

    private fun startReveal() {
        cancelReveal()
        revealJob = viewModelScope.launch {
            val start = _uiState.value.run ?: return@launch
            val step = (REVEAL_DURATION_MS / start.mode.matches).coerceIn(MIN_STEP_MS, MAX_STEP_MS)
            delay(if (start.revealed == 0) REVEAL_START_DELAY_MS else step)
            while (true) {
                val run = _uiState.value.run ?: break
                if (run.revealed >= run.matches.size) break
                _uiState.update { state -> state.copy(run = state.run?.let { it.copy(revealed = it.revealed + 1) }) }
                delay(step)
            }
        }
    }

    private fun cancelReveal() {
        revealJob?.cancel()
        revealJob = null
    }

    private inline fun updateDraft(transform: (DraftSession) -> DraftSession) {
        _uiState.update { state -> state.draft?.let { state.copy(draft = transform(it)) } ?: state }
    }

    companion object {
        private const val REVEAL_DURATION_MS = 4_500L
        private const val MIN_STEP_MS = 110L
        private const val MAX_STEP_MS = 520L
        private const val REVEAL_START_DELAY_MS = 350L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                GameViewModel(
                    SharedPreferencesRecordsStore(application),
                    SharedPreferencesSettingsStore(application),
                    DraftEngine(),
                    SeasonSimulator(),
                )
            }
        }
    }
}
