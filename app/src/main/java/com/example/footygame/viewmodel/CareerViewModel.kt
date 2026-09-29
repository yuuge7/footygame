package com.example.footygame.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.footygame.data.CareerStore
import com.example.footygame.data.ClubSeasons
import com.example.footygame.data.SharedPreferencesCareerStore
import com.example.footygame.game.CareerCurve
import com.example.footygame.game.DraftEngine
import com.example.footygame.game.Dynasty
import com.example.footygame.game.ProCareer
import com.example.footygame.game.SeasonSimulator
import com.example.footygame.game.Simulation
import com.example.footygame.models.Difficulty
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.DraftStyle
import com.example.footygame.models.DynastyPhase
import com.example.footygame.models.DynastyState
import com.example.footygame.models.Formation
import com.example.footygame.models.JanuaryEvent
import com.example.footygame.models.League
import com.example.footygame.models.Legacy
import com.example.footygame.models.ManagerTrait
import com.example.footygame.models.MatchResult
import com.example.footygame.models.Offer
import com.example.footygame.models.Position
import com.example.footygame.models.ProPhase
import com.example.footygame.models.ProState
import com.example.footygame.models.RunResult
import com.example.footygame.models.SeasonPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/** A career season as far as it's known: the whole thing, or the league's first half while January is open. */
data class SeasonView(
    val league: List<MatchResult>,
    val result: RunResult? = null,
    val januaryOffers: List<JanuaryEvent> = emptyList(),
) {
    val awaitingJanuary: Boolean get() = result == null
}

data class CareerUiState(
    val dynasty: DynastyState? = null,
    val dynastySeason: SeasonView? = null,
    /** The first XI's draft, or a summer signing into [signingSlotId]. */
    val dynastyDraft: DraftSession? = null,
    val signingSlotId: String? = null,
    val pro: ProState? = null,
    val proSeason: SeasonView? = null,
    /** Finished careers, newest first. */
    val legacies: List<Legacy> = emptyList(),
)

/**
 * Both career modes. Careers are saved after every step; a season is never stored, only its seed, so it's
 * replayed from the saved squad whenever it's needed again.
 */
class CareerViewModel(
    private val store: CareerStore,
    private val simulator: SeasonSimulator,
    private val draftEngine: DraftEngine,
    private val signingEngine: DraftEngine,
    private val seeds: Random = Random.Default,
    private val curve: () -> CareerCurve = { CareerCurve.standard },
) : ViewModel() {

    private val _uiState = MutableStateFlow(CareerUiState())
    val uiState: StateFlow<CareerUiState> = _uiState.asStateFlow()

    init {
        val dynasty = store.loadDynasty()
        val pro = store.loadPro()
        _uiState.value = CareerUiState(
            dynasty = dynasty,
            dynastySeason = dynasty?.let(::dynastySeasonFor),
            // A draft isn't saved; one interrupted by the app closing starts again.
            dynastyDraft = dynasty?.takeIf { it.phase == DynastyPhase.DRAFT }?.let { draftEngine.start(DraftMode.EPL, firstDraft(it.formation)) },
            pro = pro,
            proSeason = pro?.let(::proSeasonFor),
            legacies = store.loadLegacies(),
        )
    }

    // ---- Manager dynasty ----

    fun startDynasty(name: String, trait: ManagerTrait, formation: Formation) {
        val state = DynastyState(
            managerName = name.trim(),
            trait = trait,
            formation = formation,
            seed = seeds.nextLong(),
            confidence = Dynasty.START_CONFIDENCE,
        )
        store.saveDynasty(state)
        _uiState.update {
            it.copy(
                dynasty = state,
                dynastySeason = null,
                dynastyDraft = draftEngine.start(DraftMode.EPL, firstDraft(state.formation)),
                signingSlotId = null,
            )
        }
    }

    fun chooseDynastySlot(slotId: String) = updateDraft { engine().chooseSlot(it, slotId) }

    fun respinDynasty() = updateDraft { engine().respin(it) }

    fun pickDynasty(playerId: String, slotId: String?): Boolean {
        val draft = _uiState.value.dynastyDraft ?: return false
        val updated = engine().pick(draft, playerId, slotId) ?: return false
        _uiState.update { it.copy(dynastyDraft = updated) }
        return true
    }

    /** The first XI is drafted, or a summer signing is made: into the dynasty. */
    fun confirmDynastyDraft() {
        val state = _uiState.value.dynasty ?: return
        val draft = _uiState.value.dynastyDraft?.takeIf { it.isComplete } ?: return
        val slotId = _uiState.value.signingSlotId
        val next = if (slotId == null) {
            if (state.phase != DynastyPhase.DRAFT) return
            Dynasty.preseason(state.copy(squad = draft.picks.map { (id, pick) -> Dynasty.member(id, pick, season = 1) }))
        } else {
            Dynasty.sign(state, slotId, draft.picks[slotId] ?: return)
        }
        saveDynasty(next)
        _uiState.update { it.copy(dynastyDraft = null, signingSlotId = null) }
    }

    fun cancelSigning() {
        _uiState.update { it.copy(dynastyDraft = null, signingSlotId = null) }
    }

    fun kickOffDynasty() {
        val state = _uiState.value.dynasty?.takeIf { it.phase == DynastyPhase.SEASON && it.seasonPhase == SeasonPhase.PRESEASON } ?: return
        saveDynasty(state.copy(seasonPhase = SeasonPhase.LEAGUE))
    }

    fun chooseDynastyJanuary(event: JanuaryEvent) {
        val state = _uiState.value.dynasty ?: return
        if (_uiState.value.dynastySeason?.januaryOffers?.contains(event) != true) return
        saveDynasty(state.copy(january = event))
    }

    /** The competition on screen is done: on to the next one, or the board's review. Returns the new step. */
    fun advanceDynasty(): SeasonPhase? {
        val state = _uiState.value.dynasty?.takeIf { it.phase == DynastyPhase.SEASON } ?: return null
        val result = _uiState.value.dynastySeason?.result ?: return null
        val next = nextPhase(state.seasonPhase, result) ?: return null
        saveDynasty(if (next == SeasonPhase.REVIEW) Dynasty.review(state, result) else state.copy(seasonPhase = next))
        return next
    }

    /** Leaves the review: into the summer window, or to the end of the dynasty. */
    fun closeDynastyReview() {
        val state = _uiState.value.dynasty?.takeIf { it.seasonPhase == SeasonPhase.REVIEW && it.phase == DynastyPhase.SEASON } ?: return
        val next = Dynasty.afterReview(state, _uiState.value.dynastySeason?.result, curve())
        if (next.phase == DynastyPhase.FINISHED) addLegacy(Dynasty.legacy(next))
        saveDynasty(next)
    }

    /**
     * Opens a summer signing for [slotId]: the XI without that slot, spinning English and European squads
     * for it. Signings are kept back for empty slots, so a retirement can always be replaced.
     */
    fun startSigning(slotId: String) {
        val state = _uiState.value.dynasty?.takeIf { it.phase == DynastyPhase.SUMMER } ?: return
        val filled = state.squad.any { it.slotId == slotId }
        val empty = state.formation.slots.size - state.squad.size
        if (state.signingsLeft <= 0 || (filled && state.signingsLeft <= empty)) return
        val session = DraftSession(
            mode = DraftMode.EPL,
            settings = firstDraft(state.formation).copy(style = DraftStyle.POSITION_FIRST, difficulty = Difficulty.NORMAL),
            picks = Dynasty.picks(state.squad) - slotId,
            manager = Dynasty.manager(state),
        )
        _uiState.update { it.copy(dynastyDraft = signingEngine.chooseSlot(session, slotId), signingSlotId = slotId) }
    }

    fun startNextDynastySeason() {
        val state = _uiState.value.dynasty?.takeIf { it.phase == DynastyPhase.SUMMER } ?: return
        if (state.squad.size < state.formation.slots.size) return
        saveDynasty(Dynasty.nextSeason(state))
    }

    /** Clears the dynasty: after its legacy has been shown, or when the user walks away from it. */
    fun endDynasty() {
        store.saveDynasty(null)
        _uiState.update { it.copy(dynasty = null, dynastySeason = null, dynastyDraft = null, signingSlotId = null) }
    }

    private fun engine() = if (_uiState.value.signingSlotId != null) signingEngine else draftEngine

    private inline fun updateDraft(transform: (DraftSession) -> DraftSession) {
        _uiState.update { state -> state.dynastyDraft?.let { state.copy(dynastyDraft = transform(it)) } ?: state }
    }

    private fun saveDynasty(state: DynastyState) {
        store.saveDynasty(state)
        _uiState.update { it.copy(dynasty = state, dynastySeason = dynastySeasonFor(state)) }
    }

    private fun dynastySeasonFor(state: DynastyState): SeasonView? {
        if (state.phase != DynastyPhase.SEASON || state.seasonPhase == SeasonPhase.PRESEASON) return null
        val session = Dynasty.session(state).takeIf { it.isComplete } ?: return null
        return when (val step = simulator.simulate(session, Dynasty.seasonSeed(state), state.january)) {
            is Simulation.Complete -> SeasonView(step.result.matches, step.result)
            is Simulation.TransferWindow -> SeasonView(step.played, januaryOffers = step.offers)
        }
    }

    // ---- Player career ----

    fun startPro(name: String, position: Position, startYear: Int, league: League = League.PREMIER_LEAGUE) {
        val state = ProCareer.newCareer(name, position, startYear, seeds.nextLong(), league)
        savePro(state)
    }

    fun chooseFirstClub(offer: Offer) {
        val state = _uiState.value.pro?.takeIf { it.phase == ProPhase.FIRST_CLUB && offer in it.offers } ?: return
        savePro(ProCareer.chooseFirstClub(state, offer))
    }

    fun kickOffPro() {
        val state = _uiState.value.pro?.takeIf { it.phase == ProPhase.SEASON && it.seasonPhase == SeasonPhase.PRESEASON } ?: return
        savePro(state.copy(seasonPhase = SeasonPhase.LEAGUE))
    }

    /** Same as [advanceDynasty], for the player career. Returns the new step. */
    fun advancePro(): SeasonPhase? {
        val state = _uiState.value.pro?.takeIf { it.phase == ProPhase.SEASON } ?: return null
        val result = _uiState.value.proSeason?.result ?: return null
        val next = nextPhase(state.seasonPhase, result) ?: return null
        if (next == SeasonPhase.REVIEW) {
            val squad = ProCareer.clubSquad(state.club, state.year) ?: return null
            savePro(ProCareer.review(state, ProCareer.report(state, squad, result)))
        } else {
            savePro(state.copy(seasonPhase = next))
        }
        return next
    }

    /** Leaves the review: into the summer's offers, or into retirement when the body says so. */
    fun closeProReview() {
        val state = _uiState.value.pro?.takeIf { it.phase == ProPhase.SEASON && it.seasonPhase == SeasonPhase.REVIEW } ?: return
        val next = ProCareer.afterReview(state)
        if (next.phase == ProPhase.RETIRED) addLegacy(ProCareer.legacy(next))
        savePro(next)
    }

    /** Hangs up the boots, from the review or the summer. */
    fun retirePro() {
        val state = _uiState.value.pro ?: return
        val grown = when {
            state.phase == ProPhase.SEASON && state.seasonPhase == SeasonPhase.REVIEW -> ProCareer.grown(state)
            state.phase == ProPhase.TRANSFERS -> state
            else -> return
        }
        if (!ProCareer.canRetire(grown)) return
        val retired = ProCareer.retire(grown)
        addLegacy(ProCareer.legacy(retired))
        savePro(retired)
    }

    /** Next season at the offer's club, or at the current one when [offer] is null. */
    fun signPro(offer: Offer?) {
        val state = _uiState.value.pro?.takeIf { it.phase == ProPhase.TRANSFERS } ?: return
        if (offer != null && offer !in state.offers) return
        savePro(ProCareer.nextSeason(state, offer))
    }

    fun endPro() {
        store.savePro(null)
        _uiState.update { it.copy(pro = null, proSeason = null) }
    }

    private fun savePro(state: ProState) {
        store.savePro(state)
        _uiState.update { it.copy(pro = state, proSeason = proSeasonFor(state)) }
    }

    private fun proSeasonFor(state: ProState): SeasonView? {
        if (state.phase != ProPhase.SEASON || state.seasonPhase == SeasonPhase.PRESEASON) return null
        val squad = ProCareer.clubSquad(state.club, state.year) ?: return null
        val step = simulator.simulate(ProCareer.session(state, squad), ProCareer.seasonSeed(state))
        return (step as? Simulation.Complete)?.result?.let { SeasonView(it.matches, it) }
    }

    // ---- Shared ----

    private fun addLegacy(legacy: Legacy) {
        val legacies = (listOf(legacy) + _uiState.value.legacies).take(MAX_LEGACIES)
        store.saveLegacies(legacies)
        _uiState.update { it.copy(legacies = legacies) }
    }

    companion object {
        private const val MAX_LEGACIES = 20

        /** League, then the FA Cup, then Europe when the season earned it, then the review. */
        fun nextPhase(current: SeasonPhase, result: RunResult): SeasonPhase? = when (current) {
            SeasonPhase.LEAGUE -> when {
                result.cup != null -> SeasonPhase.CUP
                result.europe != null -> SeasonPhase.EUROPE
                else -> SeasonPhase.REVIEW
            }
            SeasonPhase.CUP -> if (result.europe != null) SeasonPhase.EUROPE else SeasonPhase.REVIEW
            SeasonPhase.EUROPE -> SeasonPhase.REVIEW
            SeasonPhase.PRESEASON, SeasonPhase.REVIEW -> null
        }

        /** The first dynasty draft: squad first with three re-spins. */
        private fun firstDraft(formation: Formation): DraftSettings =
            Dynasty.settings(formation).copy(difficulty = Difficulty.EASY)

        /** Summer targets come from England and Europe. */
        private val signingPool = { _: DraftMode, settings: DraftSettings ->
            (ClubSeasons.poolFor(DraftMode.EPL, settings) + ClubSeasons.poolFor(DraftMode.UCL, settings)).distinctBy { it.id }
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                CareerViewModel(
                    SharedPreferencesCareerStore(application),
                    SeasonSimulator(),
                    DraftEngine(),
                    DraftEngine(poolFor = signingPool),
                )
            }
        }
    }
}
