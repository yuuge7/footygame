package com.example.footygame.game

import com.example.footygame.models.DraftPick
import com.example.footygame.models.Formation

/** Picks a club's XI for the player career, where the squad is a real one rather than a draft. */
object Lineup {
    /**
     * Each slot takes the best player whose position it prefers, then the best it accepts at all. Slots with
     * the fewest candidates go first, so a lone keeper or the only left-sided player isn't used up elsewhere.
     */
    fun best(squad: List<DraftPick>, formation: Formation): Map<String, DraftPick> {
        val remaining = squad.sortedByDescending { it.player.rating }.toMutableList()
        val order = formation.slots.sortedBy { slot -> squad.count { it.player.position in slot.accepts } }
        val picks = mutableMapOf<String, DraftPick>()
        for (slot in order) {
            val choice = remaining.firstOrNull { it.player.position == slot.accepts.first() }
                ?: remaining.firstOrNull { it.player.position in slot.accepts }
                ?: continue
            picks[slot.id] = choice
            remaining -= choice
        }
        return picks
    }
}
