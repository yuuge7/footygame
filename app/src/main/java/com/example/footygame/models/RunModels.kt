package com.example.footygame.models

enum class Venue {
    HOME,
    AWAY,
    NEUTRAL;

    fun opposite(): Venue = when (this) {
        HOME -> AWAY
        AWAY -> HOME
        NEUTRAL -> NEUTRAL
    }
}

enum class Outcome { WIN, DRAW, LOSS }

enum class StageType {
    MATCHDAY,
    LEAGUE_PHASE,
    GROUP_STAGE,
    KNOCKOUT_PLAYOFF,
    ROUND_OF_32,
    ROUND_OF_16,
    QUARTER_FINAL,
    SEMI_FINAL,
    FINAL,
    EXTRA_PRELIMINARY_ROUND,
    PRELIMINARY_ROUND,
    QUALIFYING_ROUND,
    FIRST_ROUND,
    SECOND_ROUND,
    THIRD_ROUND,
    FOURTH_ROUND,
    FIFTH_ROUND;

    /** Ties that must produce a winner, as opposed to league and group matches. */
    val isKnockout: Boolean get() = this != MATCHDAY && this != LEAGUE_PHASE && this != GROUP_STAGE
}

/** [number] is the matchday, group match or qualifying round; [leg] is 1 or 2 for two-legged ties. */
data class Stage(val type: StageType, val number: Int = 0, val leg: Int = 0)

data class Opponent(val name: String, val rating: Int)

data class MatchResult(
    val stage: Stage,
    val opponent: Opponent,
    val venue: Venue,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val extraTime: Boolean = false,
    /** Shootout score, ours first, when the tie went to penalties. */
    val penalties: Pair<Int, Int>? = null,
    /** Aggregate score, ours first, on the deciding leg of a two-legged tie. */
    val aggregate: Pair<Int, Int>? = null,
    val scorerIds: List<String> = emptyList(),
) {
    val outcome: Outcome
        get() = when {
            goalsFor > goalsAgainst -> Outcome.WIN
            goalsFor < goalsAgainst -> Outcome.LOSS
            else -> Outcome.DRAW
        }
}

val List<MatchResult>.wins: Int get() = count { it.outcome == Outcome.WIN }
val List<MatchResult>.draws: Int get() = count { it.outcome == Outcome.DRAW }
val List<MatchResult>.losses: Int get() = count { it.outcome == Outcome.LOSS }

data class TableRow(
    val name: String,
    val isUser: Boolean,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
) {
    val played: Int get() = won + drawn + lost
    val points: Int get() = won * 3 + drawn
    val goalDifference: Int get() = goalsFor - goalsAgainst

    fun plus(scored: Int, conceded: Int): TableRow = copy(
        won = won + if (scored > conceded) 1 else 0,
        drawn = drawn + if (scored == conceded) 1 else 0,
        lost = lost + if (scored < conceded) 1 else 0,
        goalsFor = goalsFor + scored,
        goalsAgainst = goalsAgainst + conceded,
    )
}

sealed interface Verdict {
    data class LeagueFinish(val position: Int, val points: Int) : Verdict
    data object Champions : Verdict

    /** [by] is null when the run ended in a league or group phase rather than a tie. */
    data class Eliminated(val stage: Stage, val by: Opponent?) : Verdict
}

data class ScorerTally(val player: Player, val goals: Int)

enum class JanuaryEvent {
    /** Break the bank for a star: transformative or a flop. */
    MARQUEE_SIGNING,

    /** Low-risk loan for fresh legs. */
    LOAN_PROSPECT,

    /** Cash in on the best player and hope the squad rallies. */
    SELL_STAR,

    /** A new fitness regime: fewer injuries or a backfire. */
    FITNESS_COACH,
}

data class JanuaryOutcome(
    val event: JanuaryEvent,
    val success: Boolean,
    /** How many matches had been played when the window opened. */
    val afterMatches: Int,
    val signed: DraftPick? = null,
    val departed: Player? = null,
    val attackChange: Double = 0.0,
    val defenceChange: Double = 0.0,
)

enum class Competition {
    CHAMPIONS_LEAGUE,
    EUROPA_LEAGUE,
    CONFERENCE_LEAGUE,

    // Career seasons only: each league's domestic cup, entered at the stage its top-flight clubs join.
    FA_CUP,
    COPA_DEL_REY,
    COPPA_ITALIA,
    DFB_POKAL,
    COUPE_DE_FRANCE,
    KNVB_CUP,
    TACA_DE_PORTUGAL,
    SCOTTISH_CUP,
    TURKISH_CUP,
    RUSSIAN_CUP,
    UKRAINIAN_CUP,
    GREEK_CUP;

    val isEuropean: Boolean get() = this == CHAMPIONS_LEAGUE || this == EUROPA_LEAGUE || this == CONFERENCE_LEAGUE
}

/** A cup campaign beside the league: the European run a high finish earns, or a career side's domestic cup. */
data class CupRun(val competition: Competition, val matches: List<MatchResult>, val verdict: Verdict) {
    val won: Boolean get() = verdict == Verdict.Champions
}

data class RunResult(
    val mode: DraftMode,
    val matches: List<MatchResult>,
    val verdict: Verdict,
    val table: List<TableRow> = emptyList(),
    val topScorers: List<ScorerTally> = emptyList(),
    val manager: Manager? = null,
    val january: JanuaryOutcome? = null,
    val europe: CupRun? = null,
    /** The domestic cup run of a career season; null in the challenges. */
    val cup: CupRun? = null,
    val league: League = League.PREMIER_LEAGUE,
) {
    val wins: Int get() = matches.wins
    val draws: Int get() = matches.draws
    val losses: Int get() = matches.losses
    val goalsFor: Int get() = matches.sumOf { it.goalsFor }
    val goalsAgainst: Int get() = matches.sumOf { it.goalsAgainst }
    val goalDifference: Int get() = goalsFor - goalsAgainst
    val cleanSheets: Int get() = matches.count { it.goalsAgainst == 0 }
    val isFlawless: Boolean get() = matches.size == mode.matches && wins == matches.size
    val wonTrophy: Boolean
        get() = verdict == Verdict.Champions || (verdict as? Verdict.LeagueFinish)?.position == 1

    /** The main trophy plus any cup won beside it. */
    val trophies: Int get() = listOf(wonTrophy, europe?.won == true, cup?.won == true).count { it }

    /** The first match that wasn't a win, i.e. where the perfect run ended. */
    val firstDropped: MatchResult? get() = matches.firstOrNull { it.outcome != Outcome.WIN }
}
