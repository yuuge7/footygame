package com.example.footygame.game

import com.example.footygame.models.DraftSession
import com.example.footygame.models.DynastySeason
import com.example.footygame.models.Legacy
import com.example.footygame.models.MatchResult
import com.example.footygame.models.ManagerTotals
import com.example.footygame.models.PlayerTotals
import com.example.footygame.models.ProSeason
import com.example.footygame.models.RunResult
import com.example.footygame.models.Tally
import com.example.footygame.models.mergeTallies

/**
 * Books a dynasty season into the managers' record book. [session] is the XI that played it. The record is
 * the league's, the way a challenge's is; goals go to the scorers from every competition, like the season's
 * top scorer in the board's review, and the trophies are the season's, cups included.
 */
fun ManagerTotals.withSeason(result: RunResult, session: DraftSession, season: DynastySeason): ManagerTotals = copy(
    seasons = (seasons + result.toTotals(session, scoring = result.allMatches)).trimmed(),
    trophies = trophies.plusEach(season.trophies),
    bestFinish = minOf(bestFinish ?: season.position, season.position),
    targetsMet = targetsMet + if (season.position <= season.target) 1 else 0,
)

/** A dynasty reached the hall of fame. */
fun ManagerTotals.withLegacy(legacy: Legacy): ManagerTotals =
    copy(dynasties = dynasties + 1, sackings = sackings + if (legacy.sacked) 1 else 0)

fun PlayerTotals.withSeason(season: ProSeason): PlayerTotals = copy(
    seasons = seasons + 1,
    starterSeasons = starterSeasons + if (season.starter) 1 else 0,
    appearances = appearances + season.appearances,
    goals = goals + season.goals,
    bestSeasonGoals = maxOf(bestSeasonGoals, season.goals),
    peakRating = maxOf(peakRating, season.ratingBefore, season.ratingAfter),
    trophies = trophies.plusEach(season.trophies),
    awards = awards.plusEach(season.awards),
    clubs = clubs.mergeTallies(mapOf(season.club to Tally(season.club, 1))),
    leagues = leagues.plusEach(listOf(season.league)),
)

/** A player career reached the hall of fame. */
fun PlayerTotals.withLegacy(): PlayerTotals = copy(careers = careers + 1)

private val RunResult.allMatches: List<MatchResult>
    get() = matches + cup?.matches.orEmpty() + europe?.matches.orEmpty()

private fun <T> Map<T, Int>.plusEach(items: List<T>): Map<T, Int> =
    items.fold(this) { counts, item -> counts + (item to (counts[item] ?: 0) + 1) }
