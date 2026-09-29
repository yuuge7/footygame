package com.example.footygame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.footygame.ui.DraftScreen
import com.example.footygame.ui.MainMenuScreen
import com.example.footygame.ui.SetupScreen
import com.example.footygame.ui.SimulationScreen
import com.example.footygame.ui.StatsScreen
import com.example.footygame.ui.components.rememberReducedMotion
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
data object StatsScreenKey : NavKey

@Composable
fun MainNavigation(viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)) {
    val backStack = rememberNavBackStack(MainMenuKey)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()

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
                )
            }

            entry<StatsScreenKey> {
                StatsScreen(
                    stats = state.stats,
                    records = state.records,
                    onBack = { if (backStack.isAt(StatsScreenKey)) backStack.removeAt(backStack.lastIndex) },
                )
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
                        onBack = { if (backStack.isAt(SetupScreenKey)) backStack.removeAt(backStack.lastIndex) },
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
                                viewModel.simulate(animate = !reducedMotion)
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
                        onRunItBack = {
                            if (backStack.isAt(SimulationScreenKey)) viewModel.simulate(animate = !reducedMotion)
                        },
                        onNewDraft = {
                            if (backStack.isAt(SimulationScreenKey)) {
                                viewModel.startDraft()
                                backStack.resetTo(MainMenuKey, SetupScreenKey, DraftScreenKey)
                            }
                        },
                        onMenu = { if (backStack.isAt(SimulationScreenKey)) backStack.resetTo(MainMenuKey) },
                        onBack = { if (backStack.isAt(SimulationScreenKey)) backStack.removeAt(backStack.lastIndex) },
                    )
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

/** Replaces the stack without ever leaving it empty, which NavDisplay doesn't allow. */
private fun NavBackStack<NavKey>.resetTo(vararg keys: NavKey) {
    addAll(0, keys.toList())
    while (size > keys.size) removeAt(lastIndex)
}
