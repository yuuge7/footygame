package com.example.footygame.game

import com.example.footygame.data.ClubSeasons
import com.example.footygame.models.ClubSeason
import com.example.footygame.models.Pool

/**
 * Real career arcs from the squad data, for dynasty squads that play on for years. A player's rating in any
 * season comes from the seasons we have for them, interpolated between, then aged on past the last one: so a
 * teenage Ronaldo drafted from 2003/04 grows into his 2007/08 self, and a veteran fades and retires.
 *
 * The data has no birth dates, so age is estimated from the first season a player appears in.
 */
class CareerCurve(squads: List<ClubSeason>) {
    private data class Point(val year: Int, val rating: Int)

    private val points: Map<String, List<Point>> = squads
        .flatMap { squad -> squad.players.map { it.id to Point(seasonYear(squad), it.rating) } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, seen) ->
            seen.groupBy { it.year }.map { (year, same) -> Point(year, same.maxOf { it.rating }) }.sortedBy { it.year }
        }

    /** The player's rating in the season starting in [year], or null once they have retired. */
    fun ratingIn(playerId: String, year: Int): Int? {
        val known = points[playerId] ?: return null
        val debut = known.first()
        if (ageIn(debut.year, year) > RETIREMENT_AGE) return null
        val last = known.last()
        val rating = when {
            year <= debut.year -> debut.rating - (debut.year - year) * YOUTH_STEP
            year >= last.year -> agedOn(last, debut.year, year)
            else -> {
                val after = known.first { it.year >= year }
                val before = known.last { it.year <= year }
                if (after.year == before.year) {
                    before.rating
                } else {
                    val progress = (year - before.year).toDouble() / (after.year - before.year)
                    Math.round(before.rating + (after.rating - before.rating) * progress).toInt()
                }
            }
        }
        return rating.takeIf { it >= MIN_RATING }
    }

    /** Past the last season we know: youngsters still improve, the late twenties hold, the thirties fade. */
    private fun agedOn(last: Point, debut: Int, year: Int): Int {
        var rating = last.rating
        for (season in last.year + 1..year) {
            val age = ageIn(debut, season)
            rating += when {
                age <= 23 -> 1
                age <= 29 -> 0
                age <= 32 -> -2
                else -> -3
            }
        }
        return rating
    }

    private fun ageIn(debut: Int, year: Int) = DEBUT_AGE + (year - debut)

    companion object {
        private const val DEBUT_AGE = 21
        private const val RETIREMENT_AGE = 37
        private const val MIN_RATING = 58
        private const val YOUTH_STEP = 2

        val standard: CareerCurve by lazy { CareerCurve(ClubSeasons.all) }

        /** A World Cup is the end of the season that started the year before. */
        fun seasonYear(squad: ClubSeason): Int = if (squad.pool == Pool.NATIONAL_TEAM) squad.startYear - 1 else squad.startYear
    }
}
