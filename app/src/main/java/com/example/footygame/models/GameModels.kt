package com.example.footygame.models

enum class Position { GK, DEF, MID, ATT }

data class Player(
    /** Stable per person, so the same player can't be drafted twice from different seasons. */
    val id: String,
    val name: String,
    val shortName: String,
    val position: Position,
    val rating: Int,
)

enum class Pool {
    ENGLISH_CLUB,

    /** Premier League squads from the whole table, relegated sides included: too weak for the Champions League draw. */
    ENGLISH_DOMESTIC,
    EUROPEAN_CLUB,
    NATIONAL_TEAM,
}

data class ClubSeason(
    val id: String,
    val club: String,
    /** First calendar year of a club season (2003 for 2003/04), or the tournament year for a national team. */
    val startYear: Int,
    val code: String,
    val primaryColor: Long,
    val secondaryColor: Long,
    val pool: Pool,
    val players: List<Player>,
) {
    val season: String get() = seasonLabel(startYear, isTournament = pool == Pool.NATIONAL_TEAM)
}

/** "2003/04" for a club season starting in 2003, or just the year for a tournament. */
fun seasonLabel(startYear: Int, isTournament: Boolean): String =
    if (isTournament) startYear.toString() else "$startYear/${((startYear + 1) % 100).toString().padStart(2, '0')}"

enum class DraftMode(val matches: Int, val pools: Set<Pool>) {
    EPL(38, setOf(Pool.ENGLISH_CLUB, Pool.ENGLISH_DOMESTIC)),
    UCL(17, setOf(Pool.ENGLISH_CLUB, Pool.EUROPEAN_CLUB)),
    WC(8, setOf(Pool.NATIONAL_TEAM)),
    FAC(14, setOf(Pool.ENGLISH_CLUB, Pool.ENGLISH_DOMESTIC));

    val challenge: String get() = "$matches-0"

    val isTournament: Boolean get() = pools == setOf(Pool.NATIONAL_TEAM)

    /** Club competitions pause for the January transfer window; national teams can't sign anyone. */
    val hasJanuaryWindow: Boolean get() = !isTournament

    /** Only a league season can finish high enough to qualify for Europe. */
    val hasEuropeanNights: Boolean get() = this == EPL
}

/**
 * A position on the pitch. [row] counts from the attacking end (0) down to the goalkeeper,
 * [x] is the horizontal centre as a fraction of pitch width. [accepts] is ordered by preference.
 */
data class Slot(
    val id: String,
    val label: String,
    val accepts: List<Position>,
    val row: Int,
    val x: Float,
)

private fun goalkeeper(row: Int) = Slot("GK", "GK", listOf(Position.GK), row, 0.5f)

private fun backFour(row: Int) = listOf(
    Slot("LCB", "CB", listOf(Position.DEF), row, 0.37f),
    Slot("RCB", "CB", listOf(Position.DEF), row, 0.63f),
    Slot("LB", "LB", listOf(Position.DEF), row, 0.11f),
    Slot("RB", "RB", listOf(Position.DEF), row, 0.89f),
)

private fun backThree(row: Int) = listOf(
    Slot("LCB", "CB", listOf(Position.DEF), row, 0.22f),
    Slot("CB", "CB", listOf(Position.DEF), row, 0.5f),
    Slot("RCB", "CB", listOf(Position.DEF), row, 0.78f),
)

private fun backFive(row: Int) = listOf(
    Slot("LCB", "CB", listOf(Position.DEF), row, 0.3f),
    Slot("CB", "CB", listOf(Position.DEF), row, 0.5f),
    Slot("RCB", "CB", listOf(Position.DEF), row, 0.7f),
    Slot("LWB", "LWB", listOf(Position.DEF, Position.MID), row, 0.09f),
    Slot("RWB", "RWB", listOf(Position.DEF, Position.MID), row, 0.91f),
)

private fun strikePair(row: Int) = listOf(
    Slot("LST", "ST", listOf(Position.ATT), row, 0.33f),
    Slot("RST", "ST", listOf(Position.ATT), row, 0.67f),
)

private fun wideMidfield(row: Int) = listOf(
    Slot("LM", "LM", listOf(Position.MID, Position.ATT), row, 0.11f),
    Slot("RM", "RM", listOf(Position.MID, Position.ATT), row, 0.89f),
)

private fun wingBacks(row: Int) = listOf(
    Slot("LWB", "LWB", listOf(Position.DEF, Position.MID), row, 0.11f),
    Slot("RWB", "RWB", listOf(Position.DEF, Position.MID), row, 0.89f),
)

private fun midfield(row: Int, vararg xs: Float): List<Slot> {
    val ids = when (xs.size) {
        1 -> listOf("CM")
        2 -> listOf("LCM", "RCM")
        else -> listOf("LCM", "CM", "RCM")
    }
    return xs.mapIndexed { index, x -> Slot(ids[index], "CM", listOf(Position.MID), row, x) }
}

/** Slots are listed central-first, which is the order an auto-placed player tries them in. */
enum class Formation(val label: String, val slots: List<Slot>) {
    F433(
        "4-3-3",
        listOf(
            Slot("ST", "ST", listOf(Position.ATT), 0, 0.5f),
            Slot("LW", "LW", listOf(Position.ATT, Position.MID), 0, 0.17f),
            Slot("RW", "RW", listOf(Position.ATT, Position.MID), 0, 0.83f),
        ) + midfield(1, 0.2f, 0.5f, 0.8f) + backFour(2) + goalkeeper(3),
    ),
    F442(
        "4-4-2",
        strikePair(0) + midfield(1, 0.37f, 0.63f) + wideMidfield(1) + backFour(2) + goalkeeper(3),
    ),
    F4231(
        "4-2-3-1",
        listOf(
            Slot("ST", "ST", listOf(Position.ATT), 0, 0.5f),
            Slot("CAM", "CAM", listOf(Position.MID, Position.ATT), 1, 0.5f),
            Slot("LAM", "LW", listOf(Position.ATT, Position.MID), 1, 0.17f),
            Slot("RAM", "RW", listOf(Position.ATT, Position.MID), 1, 0.83f),
            Slot("LDM", "DM", listOf(Position.MID), 2, 0.35f),
            Slot("RDM", "DM", listOf(Position.MID), 2, 0.65f),
        ) + backFour(3) + goalkeeper(4),
    ),
    F451(
        "4-5-1",
        listOf(Slot("ST", "ST", listOf(Position.ATT), 0, 0.5f)) +
            midfield(1, 0.3f, 0.5f, 0.7f) + wideMidfield(1) + backFour(2) + goalkeeper(3),
    ),
    F343(
        "3-4-3",
        listOf(
            Slot("ST", "ST", listOf(Position.ATT), 0, 0.5f),
            Slot("LW", "LW", listOf(Position.ATT, Position.MID), 0, 0.17f),
            Slot("RW", "RW", listOf(Position.ATT, Position.MID), 0, 0.83f),
        ) + midfield(1, 0.37f, 0.63f) + wingBacks(1) + backThree(2) + goalkeeper(3),
    ),
    F352(
        "3-5-2",
        strikePair(0) + midfield(1, 0.3f, 0.5f, 0.7f) + wingBacks(1) + backThree(2) + goalkeeper(3),
    ),
    F541(
        "5-4-1",
        listOf(Slot("ST", "ST", listOf(Position.ATT), 0, 0.5f)) +
            midfield(1, 0.37f, 0.63f) + wideMidfield(1) + backFive(2) + goalkeeper(3),
    ),
    F41212(
        "4-1-2-1-2",
        strikePair(0) +
            Slot("CAM", "CAM", listOf(Position.MID, Position.ATT), 1, 0.5f) +
            midfield(2, 0.3f, 0.7f) +
            Slot("CDM", "DM", listOf(Position.MID), 3, 0.5f) +
            backFour(4) + goalkeeper(5),
    ),
    F4411(
        "4-4-1-1",
        listOf(
            Slot("ST", "ST", listOf(Position.ATT), 0, 0.5f),
            Slot("CF", "CF", listOf(Position.ATT, Position.MID), 1, 0.5f),
        ) + midfield(2, 0.37f, 0.63f) + wideMidfield(2) + backFour(3) + goalkeeper(4),
    ),
    F532(
        "5-3-2",
        strikePair(0) + midfield(1, 0.25f, 0.5f, 0.75f) + backFive(2) + goalkeeper(3),
    ),
    F3412(
        "3-4-1-2",
        strikePair(0) +
            Slot("CAM", "CAM", listOf(Position.MID, Position.ATT), 1, 0.5f) +
            midfield(2, 0.37f, 0.63f) + wingBacks(2) + backThree(3) + goalkeeper(4),
    ),
    F4222(
        "4-2-2-2",
        strikePair(0) + listOf(
            Slot("LAM", "AM", listOf(Position.MID, Position.ATT), 1, 0.25f),
            Slot("RAM", "AM", listOf(Position.MID, Position.ATT), 1, 0.75f),
            Slot("LDM", "DM", listOf(Position.MID), 2, 0.35f),
            Slot("RDM", "DM", listOf(Position.MID), 2, 0.65f),
        ) + backFour(3) + goalkeeper(4),
    );

    val rows: Int get() = slots.maxOf { it.row } + 1
}

data class DraftPick(val player: Player, val clubSeason: ClubSeason)

data class DraftSession(
    val mode: DraftMode,
    val settings: DraftSettings = DraftSettings(),
    /** Slot id to the player drafted into it. */
    val picks: Map<String, DraftPick> = emptyMap(),
    val spin: ClubSeason? = null,
    /** Increments on every spin, so the UI can replay the reel even when the same squad comes up. */
    val spinNumber: Int = 0,
    val respinsLeft: Int = settings.difficulty.respins,
    val recentSpinIds: List<String> = emptyList(),
    /** Position first: the slot the current spin has to fill. */
    val targetSlotId: String? = null,
    /** Gaffers to choose from once the XI is complete. */
    val managerOptions: List<Manager> = emptyList(),
    val manager: Manager? = null,
) {
    val formation: Formation get() = settings.formation
    val emptySlots: List<Slot> get() = formation.slots.filter { it.id !in picks }
    val isComplete: Boolean get() = emptySlots.isEmpty()
    val needsManager: Boolean get() = isComplete && settings.managers && manager == null
    val isReadyToPlay: Boolean get() = isComplete && !needsManager

    /** Blind drafts reveal ratings once the XI is locked in. */
    val ratingsVisible: Boolean get() = settings.ratingsShown || isComplete

    val targetSlot: Slot? get() = targetSlotId?.let { id -> formation.slots.firstOrNull { it.id == id } }

    /** Position first, between picks: the user has to choose which slot to spin for. */
    val awaitingSlotChoice: Boolean
        get() = settings.style == DraftStyle.POSITION_FIRST && !isComplete && targetSlotId == null

    fun isDrafted(player: Player): Boolean = picks.values.any { it.player.id == player.id }

    fun canPlace(player: Player, slot: Slot): Boolean =
        slot.id !in picks && player.position in slot.accepts && !isDrafted(player)

    fun isEligible(player: Player): Boolean {
        val target = targetSlot
        return if (target != null) canPlace(player, target) else emptySlots.any { canPlace(player, it) }
    }

    /** Every open slot this player could go into, respecting a position-first target. */
    fun slotsFor(player: Player): List<Slot> {
        val target = targetSlot
        return if (target != null) listOfNotNull(target.takeIf { canPlace(player, it) }) else emptySlots.filter { canPlace(player, it) }
    }

    /**
     * The slot a player lands in. A position-first target always wins. Otherwise a preferred slot wins when
     * the player fits it, then the first open slot where their position is the natural choice, then any.
     */
    fun slotFor(player: Player, preferredSlotId: String? = null): Slot? {
        val open = slotsFor(player)
        if (targetSlotId != null) return open.firstOrNull()
        if (preferredSlotId != null) return open.firstOrNull { it.id == preferredSlotId }
        return open.firstOrNull { it.accepts.first() == player.position } ?: open.firstOrNull()
    }
}
