package com.example.footygame.game

import com.example.footygame.models.DraftPick
import com.example.footygame.models.Player
import com.example.footygame.models.Position
import kotlin.math.roundToInt

data class TeamRatings(
    val overall: Int,
    val attack: Double,
    val defence: Double,
    /** 0..[MAX_CHEMISTRY]: one point for every extra player from a club or nation already in the XI. */
    val chemistry: Int,
) {
    val attackBoosted: Double get() = attack + chemistry * CHEMISTRY_BOOST
    val defenceBoosted: Double get() = defence + chemistry * CHEMISTRY_BOOST

    companion object {
        const val MAX_CHEMISTRY = 10
        const val CHEMISTRY_BOOST = 0.3

        val EMPTY = TeamRatings(0, 0.0, 0.0, 0)

        fun of(picks: Collection<DraftPick>): TeamRatings {
            if (picks.isEmpty()) return EMPTY
            val players = picks.map { it.player }
            val chemistry = picks.groupingBy { it.clubSeason.club }.eachCount()
                .values.sumOf { it - 1 }
                .coerceAtMost(MAX_CHEMISTRY)
            return TeamRatings(
                overall = players.map { it.rating }.average().roundToInt(),
                attack = players.weightedRating { attackWeight(it) },
                defence = players.weightedRating { defenceWeight(it) },
                chemistry = chemistry,
            )
        }

        private fun attackWeight(position: Position) = when (position) {
            Position.ATT -> 3.0
            Position.MID -> 2.0
            Position.DEF -> 0.6
            Position.GK -> 0.0
        }

        private fun defenceWeight(position: Position) = when (position) {
            Position.GK -> 3.0
            Position.DEF -> 2.5
            Position.MID -> 1.4
            Position.ATT -> 0.3
        }

        private fun List<Player>.weightedRating(
            weight: (Position) -> Double,
        ): Double {
            val total = sumOf { weight(it.position) }
            return if (total == 0.0) 0.0 else sumOf { it.rating * weight(it.position) } / total
        }
    }
}
