package com.example.footygame.models

import kotlinx.serialization.Serializable

/** How often a player or squad came up. The name is kept because an id alone can't be shown. */
@Serializable
data class Tally(val name: String, val count: Int)

/** A final score for the record book, ours first. */
@Serializable
data class Scoreline(val goalsFor: Int, val goalsAgainst: Int, val opponent: String) {
    val margin: Int get() = goalsFor - goalsAgainst
}

/** How a run ended, flattened from [Verdict] so it can be stored. */
@Serializable
data class Finish(
    val trophy: Boolean,
    /** League position, when the run was a league season. */
    val position: Int? = null,
    /** The stage the run went out in, when it didn't win. */
    val stage: StageType? = null,
    val stageNumber: Int = 0,
)

@Serializable
data class RunSummary(
    val mode: DraftMode,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val perfect: Boolean,
    val finish: Finish,
)

/**
 * Everything a challenge has seen across its runs. Matches and goals come from the main competition only;
 * trophies include a European one, the same as the menu's record.
 */
@Serializable
data class ModeTotals(
    val runs: Int = 0,
    val trophies: Int = 0,
    val perfectRuns: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val cleanSheets: Int = 0,
    val biggestWin: Scoreline? = null,
    val longestWinStreak: Int = 0,
    val longestUnbeatenRun: Int = 0,
    /** Player id to goals scored. */
    val scorers: Map<String, Tally> = emptyMap(),
    /** Player id to the number of XIs they were drafted into. */
    val picks: Map<String, Tally> = emptyMap(),
    /** Squad id to the number of players drafted from it. */
    val squads: Map<String, Tally> = emptyMap(),
    val formations: Map<Formation, Int> = emptyMap(),
) {
    val played: Int get() = won + drawn + lost

    /** Adds two challenges together, for the all-modes view. */
    operator fun plus(other: ModeTotals) = ModeTotals(
        runs = runs + other.runs,
        trophies = trophies + other.trophies,
        perfectRuns = perfectRuns + other.perfectRuns,
        won = won + other.won,
        drawn = drawn + other.drawn,
        lost = lost + other.lost,
        goalsFor = goalsFor + other.goalsFor,
        goalsAgainst = goalsAgainst + other.goalsAgainst,
        cleanSheets = cleanSheets + other.cleanSheets,
        biggestWin = listOfNotNull(biggestWin, other.biggestWin).maxWithOrNull(scorelineOrder),
        longestWinStreak = maxOf(longestWinStreak, other.longestWinStreak),
        longestUnbeatenRun = maxOf(longestUnbeatenRun, other.longestUnbeatenRun),
        scorers = scorers.mergeTallies(other.scorers),
        picks = picks.mergeTallies(other.picks),
        squads = squads.mergeTallies(other.squads),
        formations = (formations.keys + other.formations.keys)
            .associateWith { (formations[it] ?: 0) + (other.formations[it] ?: 0) },
    )
}

@Serializable
data class CareerStats(
    val modes: Map<DraftMode, ModeTotals> = emptyMap(),
    /** Newest first. */
    val recent: List<RunSummary> = emptyList(),
) {
    /** One challenge, or every challenge added together when [mode] is null. */
    fun totals(mode: DraftMode?): ModeTotals =
        if (mode != null) modes[mode] ?: ModeTotals() else modes.values.fold(ModeTotals(), ModeTotals::plus)
}

/** Bigger margin first, then more goals scored. */
val scorelineOrder: Comparator<Scoreline> = compareBy<Scoreline>({ it.margin }, { it.goalsFor })

fun Map<String, Tally>.mergeTallies(other: Map<String, Tally>): Map<String, Tally> {
    val merged = toMutableMap()
    other.forEach { (id, tally) ->
        merged[id] = merged[id]?.let { it.copy(count = it.count + tally.count) } ?: tally
    }
    return merged
}

/** Most first; ties go alphabetically so the order is stable. */
fun Map<String, Tally>.ranked(): List<Tally> =
    values.sortedWith(compareByDescending<Tally> { it.count }.thenBy { it.name })
