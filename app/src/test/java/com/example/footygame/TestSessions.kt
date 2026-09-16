package com.example.footygame

import com.example.footygame.data.ClubSeasons
import com.example.footygame.data.ModeRecord
import com.example.footygame.data.RecordsStore
import com.example.footygame.data.SettingsStore
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftPick
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.Manager

/** Builds a complete XI by filling each slot with the best (or worst) unused player who fits. */
fun completeSession(
    mode: DraftMode,
    settings: DraftSettings = DraftSettings(),
    strongest: Boolean = true,
    manager: Manager? = null,
): DraftSession {
    val candidates = ClubSeasons.poolFor(mode, settings)
        .flatMap { squad -> squad.players.map { DraftPick(it, squad) } }
        .let { picks -> if (strongest) picks.sortedByDescending { it.player.rating } else picks.sortedBy { it.player.rating } }
    val picks = mutableMapOf<String, DraftPick>()
    for (slot in settings.formation.slots) {
        val pick = candidates.first { candidate ->
            candidate.player.position == slot.accepts.first() &&
                picks.values.none { it.player.id == candidate.player.id }
        }
        picks[slot.id] = pick
    }
    return DraftSession(mode = mode, settings = settings, picks = picks, manager = manager)
}

class InMemoryRecordsStore : RecordsStore {
    val saved = mutableMapOf<DraftMode, ModeRecord>()
    override fun load(): Map<DraftMode, ModeRecord> = saved.toMap()
    override fun save(mode: DraftMode, record: ModeRecord) {
        saved[mode] = record
    }
}

class InMemorySettingsStore : SettingsStore {
    val saved = mutableMapOf<DraftMode, DraftSettings>()
    override fun load(mode: DraftMode): DraftSettings = saved[mode] ?: DraftSettings()
    override fun save(mode: DraftMode, settings: DraftSettings) {
        saved[mode] = settings
    }
}
