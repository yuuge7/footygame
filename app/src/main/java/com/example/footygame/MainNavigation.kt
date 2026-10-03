package com.example.footygame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.footygame.game.Dynasty
import com.example.footygame.game.ProCareer
import com.example.footygame.models.DynastyPhase
import com.example.footygame.models.ProPhase
import com.example.footygame.models.SeasonPhase
import com.example.footygame.ui.CareerSeasonScreen
import com.example.footygame.ui.DraftScreen
import com.example.footygame.ui.DynastyHubScreen
import com.example.footygame.ui.DynastySeasonDetailScreen
import com.example.footygame.ui.DynastySetupScreen
import com.example.footygame.ui.EuropeScreen
import com.example.footygame.ui.MainMenuScreen
import com.example.footygame.ui.ProHubScreen
import com.example.footygame.ui.ProSeasonDetailScreen
import com.example.footygame.ui.ProSetupScreen
import com.example.footygame.ui.RunDetailScreen
import com.example.footygame.ui.SetupScreen
import com.example.footygame.ui.SimulationScreen
import com.example.footygame.ui.StatsScreen
import com.example.footygame.ui.targetLabel
import com.example.footygame.viewmodel.AutoPlay
import com.example.footygame.viewmodel.CareerViewModel
import com.example.footygame.viewmodel.GameViewModel
import kotlinx.serialization.Serializable

@Serializable
data object MainMenuKey : NavKey

@Serializable
data object SetupScreenKey : NavKey

@Serializable
data object DraftScreenKey : NavKey

@Serializable
data object SimulationScreenKey : NavKey

@Serializable
data object EuropeScreenKey : NavKey

@Serializable
data object StatsScreenKey : NavKey

/** A run from the record book's recent runs, played back; runs are told apart by their seed. */
@Serializable
data class RunDetailKey(val seed: Long) : NavKey

@Serializable
data object DynastySetupKey : NavKey

@Serializable
data object DynastyDraftKey : NavKey

@Serializable
data object DynastyHubKey : NavKey

/** One step of a dynasty season on screen; each competition gets its own entry. */
@Serializable
data class DynastySeasonKey(val phase: SeasonPhase) : NavKey

/** A booked dynasty season, opened from the season-by-season list. */
@Serializable
data class DynastyHistoryKey(val season: Int) : NavKey

@Serializable
data object ProSetupKey : NavKey

@Serializable
data object ProHubKey : NavKey

@Serializable
data class ProSeasonKey(val phase: SeasonPhase) : NavKey

@Serializable
data class ProHistoryKey(val season: Int) : NavKey

@Composable
fun MainNavigation(
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
    careers: CareerViewModel = viewModel(factory = CareerViewModel.Factory),
) {
    val backStack = rememberNavBackStack(MainMenuKey)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val career by careers.uiState.collectAsStateWithLifecycle()
    val autoPlay by AutoPlay.on.collectAsStateWithLifecycle()
    val pop = { key: NavKey -> if (backStack.isAt(key)) backStack.removeAt(backStack.lastIndex) }

    // Screens stay on screen while they animate out, so every action first checks its screen is still
    // on top. That stops double taps from starting two drafts or saving the same run twice.
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryProvider = entryProvider {
            entry<MainMenuKey> {
                MainMenuScreen(
                    records = state.records,
                    onModeSelected = { mode ->
                        if (backStack.isAt(MainMenuKey)) {
                            viewModel.openSetup(mode)
                            backStack.add(SetupScreenKey)
                        }
                    },
                    onOpenStats = { if (backStack.isAt(MainMenuKey)) backStack.add(StatsScreenKey) },
                    dynasty = career.dynasty,
                    pro = career.pro,
                    onDynasty = {
                        if (backStack.isAt(MainMenuKey)) {
                            val dynasty = career.dynasty
                            backStack.add(
                                when {
                                    dynasty == null -> DynastySetupKey
                                    dynasty.phase == DynastyPhase.DRAFT -> DynastyDraftKey
                                    else -> DynastyHubKey
                                },
                            )
                        }
                    },
                    onPro = { if (backStack.isAt(MainMenuKey)) backStack.add(if (career.pro == null) ProSetupKey else ProHubKey) },
                )
            }

            entry<StatsScreenKey> {
                StatsScreen(
                    stats = state.stats,
                    records = state.records,
                    onBack = { pop(StatsScreenKey) },
                    careerStats = career.stats,
                    legacies = career.legacies,
                    managerSeasonsPlayed = career.managerSeasonsPlayed,
                    playerSeasonsPlayed = career.playerSeasonsPlayed,
                    onOpenRun = { run ->
                        val seed = run.replay?.seed
                        if (seed != null && backStack.isAt(StatsScreenKey)) backStack.add(RunDetailKey(seed))
                    },
                )
            }

            entry<RunDetailKey> { key ->
                val summary = state.stats.recent.firstOrNull { it.replay?.seed == key.seed }
                if (summary == null) {
                    ReturnToMenu(backStack, key)
                } else {
                    val replayed = remember(summary) { viewModel.replay(summary) }
                    RunDetailScreen(summary, replayed, onBack = { pop(key) })
                }
            }

            entry<SetupScreenKey> {
                val setup = state.setup
                if (setup == null) {
                    ReturnToMenu(backStack, SetupScreenKey)
                } else {
                    SetupScreen(
                        mode = setup.mode,
                        settings = setup.settings,
                        onSettingsChange = viewModel::updateSettings,
                        onStart = {
                            if (backStack.isAt(SetupScreenKey)) {
                                viewModel.startDraft()
                                backStack.add(DraftScreenKey)
                            }
                        },
                        onBack = { pop(SetupScreenKey) },
                    )
                }
            }

            entry<DraftScreenKey> {
                val draft = state.draft
                if (draft == null) {
                    // The draft lives in memory only; after process death the back stack outlives it.
                    ReturnToMenu(backStack, DraftScreenKey)
                } else {
                    DraftScreen(
                        session = draft,
                        onChooseSlot = viewModel::chooseSlot,
                        onPick = viewModel::pick,
                        onRespin = viewModel::respin,
                        onAppointManager = viewModel::appointManager,
                        onPlay = {
                            if (backStack.isAt(DraftScreenKey)) {
                                viewModel.simulate()
                                backStack.add(SimulationScreenKey)
                            }
                        },
                        // Popping without clearing the draft keeps the screen intact during the exit transition;
                        // the next startDraft replaces it.
                        onLeave = { if (backStack.isAt(DraftScreenKey)) backStack.resetTo(MainMenuKey, SetupScreenKey) },
                    )
                }
            }

            entry<SimulationScreenKey> {
                val run = state.run
                val draft = state.draft
                if (run == null || draft == null) {
                    ReturnToMenu(backStack, SimulationScreenKey)
                } else {
                    SimulationScreen(
                        run = run,
                        session = draft,
                        onSkip = viewModel::skipReveal,
                        onChooseJanuary = viewModel::chooseJanuary,
                        autoPlay = autoPlay,
                        onNextMatch = viewModel::nextMatch,
                        onToggleAutoPlay = viewModel::toggleAutoPlay,
                        onPlayEurope = { if (backStack.isAt(SimulationScreenKey)) backStack.add(EuropeScreenKey) },
                        onRunItBack = { if (backStack.isAt(SimulationScreenKey)) viewModel.simulate() },
                        onNewDraft = {
                            if (backStack.isAt(SimulationScreenKey)) {
                                viewModel.startDraft()
                                backStack.resetTo(MainMenuKey, SetupScreenKey, DraftScreenKey)
                            }
                        },
                        onMenu = { if (backStack.isAt(SimulationScreenKey)) backStack.resetTo(MainMenuKey) },
                        onBack = { pop(SimulationScreenKey) },
                    )
                }
            }

            entry<EuropeScreenKey> {
                val run = state.run
                val draft = state.draft
                if (run?.result?.europe == null || draft == null) {
                    ReturnToMenu(backStack, EuropeScreenKey)
                } else {
                    EuropeScreen(run = run, session = draft, onSeen = viewModel::markEuropeSeen, onBack = { pop(EuropeScreenKey) })
                }
            }

            // ---- Manager dynasty ----

            entry<DynastySetupKey> {
                DynastySetupScreen(
                    legacies = career.legacies,
                    onStart = { name, trait, formation ->
                        if (backStack.isAt(DynastySetupKey)) {
                            careers.startDynasty(name, trait, formation)
                            backStack.resetTo(MainMenuKey, DynastyDraftKey)
                        }
                    },
                    onBack = { pop(DynastySetupKey) },
                )
            }

            entry<DynastyDraftKey> {
                val draft = career.dynastyDraft
                val signing = career.signingSlotId != null
                if (draft == null) {
                    ReturnToMenu(backStack, DynastyDraftKey)
                } else {
                    DraftScreen(
                        session = draft,
                        onChooseSlot = careers::chooseDynastySlot,
                        onPick = careers::pickDynasty,
                        onRespin = careers::respinDynasty,
                        onAppointManager = {},
                        onPlay = {
                            if (backStack.isAt(DynastyDraftKey)) {
                                careers.confirmDynastyDraft()
                                if (signing) backStack.removeAt(backStack.lastIndex) else backStack.resetTo(MainMenuKey, DynastyHubKey)
                            }
                        },
                        onLeave = {
                            if (backStack.isAt(DynastyDraftKey)) {
                                if (signing) {
                                    careers.cancelSigning()
                                    backStack.removeAt(backStack.lastIndex)
                                } else {
                                    careers.endDynasty()
                                    backStack.resetTo(MainMenuKey)
                                }
                            }
                        },
                        title = stringResource(if (signing) R.string.dynasty_signing_title else R.string.dynasty_title),
                        playLabel = stringResource(if (signing) R.string.dynasty_confirm_signing else R.string.dynasty_confirm_xi),
                        confirmLeave = !signing,
                    )
                }
            }

            entry<DynastyHubKey> {
                val dynasty = career.dynasty
                if (dynasty == null) {
                    ReturnToMenu(backStack, DynastyHubKey)
                } else {
                    DynastyHubScreen(
                        state = dynasty,
                        season = career.dynastySeason,
                        onBack = { if (backStack.isAt(DynastyHubKey)) backStack.resetTo(MainMenuKey) },
                        onKickOff = {
                            if (backStack.isAt(DynastyHubKey)) {
                                careers.kickOffDynasty()
                                backStack.add(DynastySeasonKey(SeasonPhase.LEAGUE))
                            }
                        },
                        onResume = { if (backStack.isAt(DynastyHubKey)) backStack.add(DynastySeasonKey(dynasty.seasonPhase)) },
                        onCloseReview = careers::closeDynastyReview,
                        onStartSigning = { slotId ->
                            if (backStack.isAt(DynastyHubKey)) {
                                careers.startSigning(slotId)
                                if (careers.uiState.value.dynastyDraft != null) backStack.add(DynastyDraftKey)
                            }
                        },
                        onNextSeason = careers::startNextDynastySeason,
                        onOpenSeason = { season -> if (backStack.isAt(DynastyHubKey)) backStack.add(DynastyHistoryKey(season)) },
                        onNewDynasty = {
                            if (backStack.isAt(DynastyHubKey)) {
                                careers.endDynasty()
                                backStack.resetTo(MainMenuKey, DynastySetupKey)
                            }
                        },
                        onQuit = {
                            if (backStack.isAt(DynastyHubKey)) {
                                careers.endDynasty()
                                backStack.resetTo(MainMenuKey)
                            }
                        },
                    )
                }
            }

            entry<DynastySeasonKey> { key ->
                val dynasty = career.dynasty
                val season = career.dynastySeason
                if (dynasty == null || season == null) {
                    ReturnToMenu(backStack, key)
                } else {
                    val names = remember(dynasty.squad) {
                        Dynasty.picks(dynasty.squad).values.associate { it.player.id to it.player.shortName }
                    }
                    CareerSeasonScreen(
                        eyebrow = "${stringResource(R.string.career_season_of, dynasty.season, Dynasty.SEASONS)} · ${stringResource(R.string.dynasty_title)}",
                        phase = key.phase,
                        season = season,
                        seed = Dynasty.seasonSeed(dynasty),
                        names = names,
                        leagueDetail = stringResource(R.string.dynasty_board_wanted, targetLabel(dynasty.target)),
                        onChooseJanuary = careers::chooseDynastyJanuary,
                        onContinue = {
                            if (backStack.isAt(key)) {
                                val next = careers.advanceDynasty()
                                backStack.showNext(next, key) { DynastySeasonKey(it) }
                            }
                        },
                        onBack = { pop(key) },
                    )
                }
            }

            entry<DynastyHistoryKey> { key ->
                val dynasty = career.dynasty
                val booked = dynasty?.history?.firstOrNull { it.season == key.season }
                if (dynasty == null || booked == null) {
                    ReturnToMenu(backStack, key)
                } else {
                    val replayed = remember(dynasty.seed, booked) { careers.replayDynastySeason(booked) }
                    DynastySeasonDetailScreen(dynasty, booked, replayed, onBack = { pop(key) })
                }
            }

            // ---- Player career ----

            entry<ProSetupKey> {
                ProSetupScreen(
                    legacies = career.legacies,
                    onStart = { name, position, year, league ->
                        if (backStack.isAt(ProSetupKey)) {
                            careers.startPro(name, position, year, league)
                            backStack.resetTo(MainMenuKey, ProHubKey)
                        }
                    },
                    onBack = { pop(ProSetupKey) },
                )
            }

            entry<ProHubKey> {
                val pro = career.pro
                if (pro == null) {
                    ReturnToMenu(backStack, ProHubKey)
                } else {
                    ProHubScreen(
                        state = pro,
                        season = career.proSeason,
                        onBack = { if (backStack.isAt(ProHubKey)) backStack.resetTo(MainMenuKey) },
                        onChooseFirstClub = careers::chooseFirstClub,
                        onKickOff = {
                            if (backStack.isAt(ProHubKey)) {
                                careers.kickOffPro()
                                backStack.add(ProSeasonKey(SeasonPhase.LEAGUE))
                            }
                        },
                        onResume = { if (backStack.isAt(ProHubKey)) backStack.add(ProSeasonKey(pro.seasonPhase)) },
                        onCloseReview = careers::closeProReview,
                        onRetire = careers::retirePro,
                        onSign = careers::signPro,
                        onOpenSeason = { season -> if (backStack.isAt(ProHubKey)) backStack.add(ProHistoryKey(season)) },
                        onNewCareer = {
                            if (backStack.isAt(ProHubKey)) {
                                careers.endPro()
                                backStack.resetTo(MainMenuKey, ProSetupKey)
                            }
                        },
                        onQuit = {
                            if (backStack.isAt(ProHubKey)) {
                                careers.endPro()
                                backStack.resetTo(MainMenuKey)
                            }
                        },
                    )
                }
            }

            entry<ProSeasonKey> { key ->
                val pro = career.pro
                val season = career.proSeason
                if (pro == null || season == null || pro.phase != ProPhase.SEASON) {
                    ReturnToMenu(backStack, key)
                } else {
                    val names = remember(pro) {
                        ProCareer.clubSquad(pro.club, pro.year)
                            ?.let { squad -> ProCareer.lineup(pro, squad).values.associate { it.player.id to it.player.shortName } }
                            .orEmpty()
                    }
                    CareerSeasonScreen(
                        eyebrow = "${stringResource(R.string.career_season, pro.season)} · ${pro.club}",
                        phase = key.phase,
                        season = season,
                        seed = ProCareer.seasonSeed(pro),
                        names = names,
                        highlightId = ProCareer.PLAYER_ID,
                        teamName = pro.club,
                        league = pro.league,
                        onContinue = {
                            if (backStack.isAt(key)) {
                                val next = careers.advancePro()
                                backStack.showNext(next, key) { ProSeasonKey(it) }
                            }
                        },
                        onBack = { pop(key) },
                    )
                }
            }

            entry<ProHistoryKey> { key ->
                val pro = career.pro
                val booked = pro?.history?.firstOrNull { it.season == key.season }
                if (pro == null || booked == null) {
                    ReturnToMenu(backStack, key)
                } else {
                    val replayed = remember(pro.seed, booked) { careers.replayProSeason(booked) }
                    ProSeasonDetailScreen(pro, booked, replayed, onBack = { pop(key) })
                }
            }
        },
    )
}

/**
 * Sends a screen whose state is gone back to the menu. Only acts while [key] is on top: a screen
 * that is merely animating out (e.g. results after "New draft" cleared the run) must not reset the stack.
 */
@Composable
private fun ReturnToMenu(backStack: NavBackStack<NavKey>, key: NavKey) {
    LaunchedEffect(Unit) {
        if (backStack.isAt(key)) backStack.resetTo(MainMenuKey)
    }
}

private fun NavBackStack<NavKey>.isAt(key: NavKey): Boolean = lastOrNull() == key

/** After a season step: straight into the next competition's screen, or back to the hub for the review. */
private fun NavBackStack<NavKey>.showNext(next: SeasonPhase?, current: NavKey, keyFor: (SeasonPhase) -> NavKey) {
    if (next == SeasonPhase.CUP || next == SeasonPhase.EUROPE) add(keyFor(next))
    remove(current)
}

/** Replaces the stack without ever leaving it empty, which NavDisplay doesn't allow. */
private fun NavBackStack<NavKey>.resetTo(vararg keys: NavKey) {
    addAll(0, keys.toList())
    while (size > keys.size) removeAt(lastIndex)
}
