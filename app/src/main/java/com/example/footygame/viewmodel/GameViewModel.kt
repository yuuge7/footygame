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
import com.example.footygame.data.SharedPreferencesStatsStore
import com.example.footygame.data.StatsStore
import com.example.footygame.game.DraftEngine
import com.example.footygame.game.ReplayedRun
import com.example.footygame.game.SeasonSimulator
import com.example.footygame.game.Simulation
import com.example.footygame.game.replayed
import com.example.footygame.game.withRun
import com.example.footygame.models.CareerStats
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.JanuaryEvent
import com.example.footygame.models.MatchResult
import com.example.footygame.models.RunResult
import com.example.footygame.models.RunSummary
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
    /** How many matches the results screen has revealed so far: one per tap, or on their own with [AutoPlay]. */
    val revealed: Int = 0,
    val isNewBest: Boolean = false,
    /** The European campaign has been played on its own screen, so its result may show on the season's. */
    val europeSeen: Boolean = false,
) {
    val isAwaitingJanuary: Boolean get() = result == null && revealed >= matches.size
    val isRevealComplete: Boolean get() = result != null && revealed >= matches.size
}

data class GameUiState(
    val records: Map<DraftMode, ModeRecord> = emptyMap(),
    val stats: CareerStats = CareerStats(),
    val setup: SetupState? = null,
    val draft: DraftSession? = null,
    val run: RunState? = null,
)

class GameViewModel(
    private val recordsStore: RecordsStore,
    private val settingsStore: SettingsStore,
    private val statsStore: StatsStore,
    private val draftEngine: DraftEngine,
    private val simulator: SeasonSimulator,
    private val seeds: Random = Random.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(records = recordsStore.load(), stats = statsStore.load()))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var revealJob: Job? = null

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

    /**
     * Plays the XI through its competition. The run may stop at the January window for [chooseJanuary].
     * Nothing is revealed yet, with system animations off too: a tap per match is the pace, not an animation.
     */
    fun simulate() {
        val draft = _uiState.value.draft?.takeIf { it.isReadyToPlay } ?: return
        cancelReveal()
        val seed = seeds.nextLong()
        advance(simulator.simulate(draft, seed), RunState(draft.mode, seed, matches = emptyList()))
    }

    fun chooseJanuary(event: JanuaryEvent) {
        val state = _uiState.value
        val draft = state.draft ?: return
        val run = state.run?.takeIf { it.isAwaitingJanuary && event in it.januaryOffers } ?: return
        advance(simulator.simulate(draft, run.seed, event), run)
    }

    fun markEuropeSeen() {
        _uiState.update { state -> state.copy(run = state.run?.copy(europeSeen = true)) }
    }

    /** Shows every match known so far: up to the transfer window, or to the end. */
    fun skipReveal() {
        cancelReveal()
        _uiState.update { state -> state.copy(run = state.run?.let { it.copy(revealed = it.matches.size) }) }
    }

    /** Reveals one more match: the season played game by game. */
    fun nextMatch() {
        _uiState.update { state ->
            state.copy(run = state.run?.let { if (it.revealed < it.matches.size) it.copy(revealed = it.revealed + 1) else it })
        }
    }

    /** Lets the matches come in on their own, one every [AutoPlay.STEP_MS], or stops them. */
    fun toggleAutoPlay() {
        AutoPlay.toggle()
        if (AutoPlay.on.value) startReveal() else cancelReveal()
    }

    /** Plays a run from the record book again, for its stats. */
    fun replay(summary: RunSummary): ReplayedRun? = summary.replayed(simulator)

    private fun advance(step: Simulation, run: RunState) {
        when (step) {
            is Simulation.TransferWindow -> _uiState.update {
                it.copy(run = run.copy(matches = step.played, januaryOffers = step.offers))
            }

            is Simulation.Complete -> finish(step.result, run)
        }
        if (AutoPlay.on.value) startReveal()
    }

    /** Saves the record and the career stats as soon as the run is decided, whatever the reveal is doing. */
    private fun finish(result: RunResult, run: RunState) {
        val state = _uiState.value
        val previous = state.records[result.mode] ?: ModeRecord()
        val updated = previous + result
        recordsStore.save(result.mode, updated)
        val stats = state.draft?.let { state.stats.withRun(result, it, run.seed) } ?: state.stats
        statsStore.save(stats)
        _uiState.update {
            it.copy(
                records = it.records + (result.mode to updated),
                stats = stats,
                run = run.copy(
                    matches = result.matches,
                    result = result,
                    januaryOffers = emptyList(),
                    isNewBest = previous.runs > 0 && previous.isImprovedBy(result),
                ),
            )
        }
    }

    private fun startReveal() {
        cancelReveal()
        revealJob = viewModelScope.launch {
            while (true) {
                delay(AutoPlay.STEP_MS)
                val run = _uiState.value.run ?: break
                if (run.revealed >= run.matches.size) break
                nextMatch()
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
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                GameViewModel(
                    SharedPreferencesRecordsStore(application),
                    SharedPreferencesSettingsStore(application),
                    SharedPreferencesStatsStore(application),
                    DraftEngine(),
                    SeasonSimulator(),
                )
            }
        }
    }
}
