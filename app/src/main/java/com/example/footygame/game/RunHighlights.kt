package com.example.footygame.game

import com.example.footygame.models.MatchResult
import com.example.footygame.models.Outcome

/** The stand-out numbers of one run, in the order the matches were played. */
data class RunHighlights(
    val biggestWin: MatchResult?,
    val heaviestDefeat: MatchResult?,
    val longestWinStreak: Int,
    /** A shootout counts as a draw here, so it keeps an unbeaten run going but ends a winning one. */
    val longestUnbeatenRun: Int,
    val failedToScore: Int,
) {
    companion object {
        fun of(matches: List<MatchResult>): RunHighlights {
            // Bigger margin first, then more goals; maxWith keeps the earliest of equals.
            val byMargin = compareBy<MatchResult>({ it.goalsFor - it.goalsAgainst }, { it.goalsFor })
            return RunHighlights(
                biggestWin = matches.filter { it.outcome == Outcome.WIN }.maxWithOrNull(byMargin),
                heaviestDefeat = matches.filter { it.outcome == Outcome.LOSS }
                    .maxWithOrNull(compareBy({ it.goalsAgainst - it.goalsFor }, { it.goalsAgainst })),
                longestWinStreak = matches.longestRun { it.outcome == Outcome.WIN },
                longestUnbeatenRun = matches.longestRun { it.outcome != Outcome.LOSS },
                failedToScore = matches.count { it.goalsFor == 0 },
            )
        }

        private inline fun List<MatchResult>.longestRun(counts: (MatchResult) -> Boolean): Int {
            var best = 0
            var current = 0
            for (match in this) {
                current = if (counts(match)) current + 1 else 0
                best = maxOf(best, current)
            }
            return best
        }
    }
}
