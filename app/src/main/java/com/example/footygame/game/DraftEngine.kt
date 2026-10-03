package com.example.footygame.game

import com.example.footygame.data.ClubSeasons
import com.example.footygame.data.Managers
import com.example.footygame.models.ClubSeason
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftPick
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.DraftStyle
import com.example.footygame.models.Manager
import kotlin.math.pow
import kotlin.random.Random

class DraftEngine(
    private val random: Random = Random.Default,
    private val poolFor: (DraftMode, DraftSettings) -> List<ClubSeason> = ClubSeasons::poolFor,
    private val managersFor: (DraftMode) -> List<Manager> = Managers::forMode,
) {
    /** Squad first spins straight away; position first waits for a slot. */
    fun start(mode: DraftMode, settings: DraftSettings = DraftSettings()): DraftSession {
        val session = DraftSession(mode = mode, settings = settings)
        return if (settings.style == DraftStyle.SQUAD_FIRST) spin(session) else session
    }

    /** Position first: commit to an open slot and spin for a squad that can fill it. */
    fun chooseSlot(session: DraftSession, slotId: String): DraftSession {
        if (!session.awaitingSlotChoice) return session
        val slot = session.emptySlots.firstOrNull { it.id == slotId } ?: return session
        return spin(session.copy(targetSlotId = slot.id))
    }

    /** Drafts a player from the current spin. Returns null when the pick isn't allowed. */
    fun pick(session: DraftSession, playerId: String, preferredSlotId: String? = null): DraftSession? {
        val spin = session.spin ?: return null
        val player = spin.players.firstOrNull { it.id == playerId } ?: return null
        val slot = session.slotFor(player, preferredSlotId) ?: return null
        val drafted = session.copy(
            picks = session.picks + (slot.id to DraftPick(player, spin)),
            targetSlotId = null,
        )
        return when {
            drafted.isComplete -> drafted.copy(spin = null, managerOptions = offerManagers(drafted))
            drafted.settings.style == DraftStyle.POSITION_FIRST -> drafted.copy(spin = null)
            else -> spin(drafted)
        }
    }

    fun respin(session: DraftSession): DraftSession {
        if (session.respinsLeft <= 0 || session.spin == null) return session
        return spin(session.copy(respinsLeft = session.respinsLeft - 1))
    }

    fun appointManager(session: DraftSession, managerId: String): DraftSession {
        if (!session.needsManager) return session
        val manager = session.managerOptions.firstOrNull { it.id == managerId } ?: return session
        return session.copy(manager = manager)
    }

    private fun offerManagers(session: DraftSession): List<Manager> =
        if (session.settings.managers) managersFor(session.mode).shuffled(random).take(MANAGER_CHOICES) else emptyList()

    /**
     * Only lands on squads with someone who fits (the target slot, or any open slot), so a draft can
     * never get stuck. A narrow era that runs dry falls back to every season. Recent squads are skipped
     * while there are alternatives.
     */
    private fun spin(session: DraftSession): DraftSession {
        val fits = { squad: ClubSeason -> squad.players.any(session::isEligible) }
        val candidates = poolFor(session.mode, session.settings).filter(fits)
            .ifEmpty { poolFor(session.mode, session.settings.copy(era = null)).filter(fits) }
        val fresh = candidates.filter { it.id !in session.recentSpinIds && it.id != session.spin?.id }
        val choices = fresh.ifEmpty { candidates.filter { it.id != session.spin?.id } }.ifEmpty { candidates }
        val next = choices.weightedRandomOrNull()
        return session.copy(
            spin = next,
            spinNumber = session.spinNumber + 1,
            recentSpinIds = (session.recentSpinIds + listOfNotNull(next?.id)).takeLast(RECENT_MEMORY),
        )
    }

    /**
     * Each rating point of [strength] above the weakest choice makes a squad [STRENGTH_ODDS] times as
     * likely, in every mode: the pools hold whole league tables and every World Cup nation, far more
     * strugglers than a perfect run can use. Title-calibre sides (84+) go from about one spin in six to
     * nearly one in three in the English pools and from one in seven to one in four at the World Cup; the
     * Champions League pool is tighter, so it leans least. Every squad keeps a chance.
     */
    private fun List<ClubSeason>.weightedRandomOrNull(): ClubSeason? {
        if (isEmpty()) return null
        val strengths = map(::strength)
        val weakest = strengths.min()
        val weights = strengths.map { STRENGTH_ODDS.pow(it - weakest) }
        var roll = random.nextDouble() * weights.sum()
        weights.forEachIndexed { index, weight ->
            roll -= weight
            if (roll < 0) return this[index]
        }
        return last()
    }

    companion object {
        private const val RECENT_MEMORY = 4
        private const val MANAGER_CHOICES = 3
        private const val STRENGTH_ODDS = 1.08

        /** Average rating of a squad's best eleven: how good an XI drafted from it could be. */
        fun strength(squad: ClubSeason): Double =
            squad.players.map { it.rating }.sortedDescending().take(11).average()
    }
}
