package com.example.footygame.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether results come in on their own or wait for a tap on "Next match". One choice for every results
 * screen, the challenges' and the careers', so it carries from the league into the cups; kept while the app runs.
 */
object AutoPlay {
    /** Slow enough to take each result in, like a vidiprinter. */
    const val STEP_MS = 650L

    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()

    fun set(on: Boolean) {
        _on.value = on
    }

    fun toggle() = set(!_on.value)
}
