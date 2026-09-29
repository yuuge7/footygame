package com.example.footygame.models

import kotlinx.serialization.Serializable

/** Silverware a career can win in a season. */
enum class Trophy { LEAGUE, FA_CUP, CHAMPIONS_LEAGUE, EUROPA_LEAGUE, CONFERENCE_LEAGUE }

val Competition.trophy: Trophy
    get() = when (this) {
        Competition.CHAMPIONS_LEAGUE -> Trophy.CHAMPIONS_LEAGUE
        Competition.EUROPA_LEAGUE -> Trophy.EUROPA_LEAGUE
        Competition.CONFERENCE_LEAGUE -> Trophy.CONFERENCE_LEAGUE
        Competition.FA_CUP -> Trophy.FA_CUP
    }

/** Everything a career season won: the league title plus any cup. */
val RunResult.seasonTrophies: List<Trophy>
    get() = listOfNotNull(
        Trophy.LEAGUE.takeIf { (verdict as? Verdict.LeagueFinish)?.position == 1 },
        cup?.takeIf { it.won }?.competition?.trophy,
        europe?.takeIf { it.won }?.competition?.trophy,
    )

/** How far a cup went, flattened so a season can be stored. [stage] is where it ended when it wasn't won. */
@Serializable
data class CupFinish(val competition: Competition, val won: Boolean, val stage: StageType? = null)

fun CupRun.finish(): CupFinish =
    CupFinish(competition, won, (verdict as? Verdict.Eliminated)?.stage?.type)

/** The career modes' season steps. Each competition is its own step, played and watched in turn. */
enum class SeasonPhase { PRESEASON, LEAGUE, CUP, EUROPE, REVIEW }

// ---- Manager dynasty ----

enum class DynastyPhase {
    /** Drafting the first XI. */
    DRAFT,

    /** Somewhere in a season: see [DynastyState.seasonPhase]. */
    SEASON,

    /** Between seasons: ratings move, players retire, signings come in. */
    SUMMER,

    /** Over: ten seasons done, or the board has had enough. */
    FINISHED,
}

/**
 * One player in the dynasty XI. Ratings follow the player's real career from the season they were signed
 * from ([fromYear], at dynasty season [joinedSeason]), so the squad ages as the seasons go by.
 */
@Serializable
data class SquadMember(
    val slotId: String,
    val squadId: String,
    val playerId: String,
    val rating: Int,
    val fromYear: Int,
    val joinedSeason: Int,
)

/** A rating that moved over the summer; [to] null means the player retired. */
@Serializable
data class RatingChange(val playerId: String, val name: String, val from: Int, val to: Int?)

@Serializable
data class DynastySeason(
    val season: Int,
    val position: Int,
    val points: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    /** The league position the board asked for. */
    val target: Int,
    val cup: CupFinish? = null,
    val europe: CupFinish? = null,
    val trophies: List<Trophy> = emptyList(),
    val confidenceBefore: Int,
    val confidenceAfter: Int,
    val topScorer: String? = null,
    val topScorerGoals: Int = 0,
)

@Serializable
data class DynastyState(
    val managerName: String,
    val trait: ManagerTrait,
    val formation: Formation,
    /** Every season's seed derives from this, so a season replays identically after the app restarts. */
    val seed: Long,
    val season: Int = 1,
    val phase: DynastyPhase = DynastyPhase.DRAFT,
    val seasonPhase: SeasonPhase = SeasonPhase.PRESEASON,
    val squad: List<SquadMember> = emptyList(),
    /** The board's faith in the manager, 0..100. */
    val confidence: Int = 60,
    /** League position the board expects this season. */
    val target: Int = 10,
    /** This season's January gamble, once chosen. */
    val january: JanuaryEvent? = null,
    val history: List<DynastySeason> = emptyList(),
    val signingsLeft: Int = 0,
    /** The last summer's rating moves and retirements. */
    val changes: List<RatingChange> = emptyList(),
    val sacked: Boolean = false,
) {
    val trophyCount: Int get() = history.sumOf { it.trophies.size }
}

// ---- Player career ----

enum class ProPhase {
    /** A new player picking their first club. */
    FIRST_CLUB,

    /** Somewhere in a season: see [ProState.seasonPhase]. */
    SEASON,

    /** The summer: offers in, a decision to make. */
    TRANSFERS,

    /** Hung up the boots. */
    RETIRED,
}

enum class Award { GOLDEN_BOOT, PLAYER_OF_THE_SEASON, YOUNG_PLAYER, BALLON_DOR }

@Serializable
data class ProSeason(
    val season: Int,
    val age: Int,
    val club: String,
    val squadId: String,
    val starter: Boolean,
    val appearances: Int,
    val goals: Int,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val position: Int,
    val trophies: List<Trophy> = emptyList(),
    val awards: List<Award> = emptyList(),
)

/** A transfer offer: the club's squad for next season and whether the player would start there. */
@Serializable
data class Offer(val squadId: String, val club: String, val strength: Int, val starter: Boolean)

@Serializable
data class ProState(
    val name: String,
    val position: Position,
    val seed: Long,
    val startYear: Int,
    val season: Int = 1,
    val age: Int,
    val rating: Int,
    /** The ceiling the player grows towards; never shown. */
    val potential: Int,
    val club: String = "",
    val phase: ProPhase = ProPhase.FIRST_CLUB,
    val seasonPhase: SeasonPhase = SeasonPhase.PRESEASON,
    val offers: List<Offer> = emptyList(),
    val history: List<ProSeason> = emptyList(),
) {
    val year: Int get() = startYear + season - 1
    val goals: Int get() = history.sumOf { it.goals }
    val appearances: Int get() = history.sumOf { it.appearances }
    val trophyCount: Int get() = history.sumOf { it.trophies.size }
}

// ---- Hall of fame ----

enum class LegacyKind { MANAGER, PLAYER }

/** A finished career, kept for the hall of fame. */
@Serializable
data class Legacy(
    val kind: LegacyKind,
    val name: String,
    val seasons: Int,
    val trophies: Map<Trophy, Int> = emptyMap(),
    val sacked: Boolean = false,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goals: Int = 0,
    val appearances: Int = 0,
    val peakRating: Int = 0,
    val awards: Int = 0,
    val clubs: List<String> = emptyList(),
) {
    val trophyCount: Int get() = trophies.values.sum()
}
