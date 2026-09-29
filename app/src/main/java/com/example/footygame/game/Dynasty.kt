package com.example.footygame.game

import com.example.footygame.data.ClubSeasons
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftPick
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.DynastyPhase
import com.example.footygame.models.DynastySeason
import com.example.footygame.models.DynastyState
import com.example.footygame.models.Formation
import com.example.footygame.models.Legacy
import com.example.footygame.models.LegacyKind
import com.example.footygame.models.Manager
import com.example.footygame.models.RatingChange
import com.example.footygame.models.RunResult
import com.example.footygame.models.SeasonPhase
import com.example.footygame.models.SquadMember
import com.example.footygame.models.Verdict
import com.example.footygame.models.finish
import com.example.footygame.models.seasonTrophies
import kotlin.random.Random

/**
 * Manager dynasty rules: ten Premier League seasons with one drafted XI. The board sets a target from the
 * squad's quality and loses faith with every place short of it; the squad ages along real careers and is
 * topped up each summer.
 */
object Dynasty {
    const val SEASONS = 10
    const val START_CONFIDENCE = 60
    const val BASE_SIGNINGS = 2
    const val MANAGER_ID = "you"

    private const val SACK_BELOW = 20
    private const val RELEGATION = 18
    private const val PLACE_WEIGHT = 3
    private const val TROPHY_WEIGHT = 8

    /** Every dynasty season: the league with its January window, the FA Cup, and Europe for a top-seven finish. */
    fun settings(formation: Formation) = DraftSettings(
        formation = formation,
        managers = false,
        europeanNights = true,
        januaryWindow = true,
        domesticCup = true,
    )

    /** What the board asks of a squad this good: the title, the top four, Europe, the top half or survival. */
    fun target(overall: Int): Int = when {
        overall >= 88 -> 1
        overall >= 85 -> 4
        overall >= 81 -> 7
        overall >= 77 -> 10
        else -> 17
    }

    fun manager(state: DynastyState) = Manager(MANAGER_ID, state.managerName, state.trait)

    fun seasonSeed(state: DynastyState): Long = Random(state.seed + state.season).nextLong()

    fun picks(squad: List<SquadMember>): Map<String, DraftPick> = squad.mapNotNull { member ->
        val clubSeason = ClubSeasons.squad(member.squadId) ?: return@mapNotNull null
        val player = clubSeason.players.firstOrNull { it.id == member.playerId } ?: return@mapNotNull null
        member.slotId to DraftPick(player.copy(rating = member.rating), clubSeason)
    }.toMap()

    /** The XI as a finished draft, the shape the season simulator plays. */
    fun session(state: DynastyState) = DraftSession(
        mode = DraftMode.EPL,
        settings = settings(state.formation),
        picks = picks(state.squad),
        manager = manager(state),
    )

    fun member(slotId: String, pick: DraftPick, season: Int) = SquadMember(
        slotId = slotId,
        squadId = pick.clubSeason.id,
        playerId = pick.player.id,
        rating = pick.player.rating,
        fromYear = CareerCurve.seasonYear(pick.clubSeason),
        joinedSeason = season,
    )

    /** Kick-off of a season: the board looks at the XI and sets its target. */
    fun preseason(state: DynastyState): DynastyState = state.copy(
        phase = DynastyPhase.SEASON,
        seasonPhase = SeasonPhase.PRESEASON,
        target = target(TeamRatings.of(picks(state.squad).values).overall),
        january = null,
    )

    /** Books a played season and lets the board react. Relegation, or confidence below [SACK_BELOW], is the sack. */
    fun review(state: DynastyState, result: RunResult): DynastyState {
        val finish = result.verdict as? Verdict.LeagueFinish ?: return state
        val trophies = result.seasonTrophies
        val delta = (state.target - finish.position) * PLACE_WEIGHT + trophies.size * TROPHY_WEIGHT
        val after = (state.confidence + delta).coerceIn(0, 100)
        val top = result.topScorers.firstOrNull()
        val season = DynastySeason(
            season = state.season,
            position = finish.position,
            points = finish.points,
            won = result.wins,
            drawn = result.draws,
            lost = result.losses,
            target = state.target,
            cup = result.cup?.finish(),
            europe = result.europe?.finish(),
            trophies = trophies,
            confidenceBefore = state.confidence,
            confidenceAfter = after,
            topScorer = top?.player?.name,
            topScorerGoals = top?.goals ?: 0,
        )
        return state.copy(
            seasonPhase = SeasonPhase.REVIEW,
            confidence = after,
            history = state.history + season,
            sacked = finish.position >= RELEGATION || after < SACK_BELOW,
        )
    }

    /** After the review: the dynasty ends on the sack or after the last season, otherwise the summer window opens. */
    fun afterReview(state: DynastyState, result: RunResult?, curve: CareerCurve = CareerCurve.standard): DynastyState =
        if (state.sacked || state.season >= SEASONS) state.copy(phase = DynastyPhase.FINISHED) else summer(state, result, curve)

    /**
     * The summer: a January signing stays on, every player moves a season along their career curve (retiring
     * when it ends), and the board funds [BASE_SIGNINGS] signings, one more after a trophy, and always enough
     * to replace the retired.
     */
    fun summer(state: DynastyState, result: RunResult?, curve: CareerCurve = CareerCurve.standard): DynastyState {
        val squad = keepJanuarySigning(state.squad, result, state.season)
        val random = Random(state.seed * 31 + state.season)
        val names = picks(squad).mapValues { it.value.player.name }
        val changes = mutableListOf<RatingChange>()
        val kept = squad.mapNotNull { member ->
            val year = member.fromYear + (state.season + 1 - member.joinedSeason)
            val next = curve.ratingIn(member.playerId, year)?.let { it + random.nextInt(-1, 2) }
            val name = names[member.slotId] ?: member.playerId
            if (next != member.rating) changes += RatingChange(member.playerId, name, member.rating, next)
            next?.let { member.copy(rating = it) }
        }
        val retired = squad.size - kept.size
        val bonus = if (state.history.lastOrNull()?.trophies.isNullOrEmpty()) 0 else 1
        return state.copy(
            phase = DynastyPhase.SUMMER,
            squad = kept,
            // Retirements first, then the biggest risers down to the biggest fallers.
            changes = changes.sortedWith(compareBy<RatingChange> { it.to != null }.thenByDescending { (it.to ?: 0) - it.from }),
            signingsLeft = maxOf(BASE_SIGNINGS + bonus, retired),
        )
    }

    private fun keepJanuarySigning(squad: List<SquadMember>, result: RunResult?, season: Int): List<SquadMember> {
        val january = result?.january ?: return squad
        val signed = january.signed ?: return squad
        val departed = january.departed ?: return squad
        return squad.map { if (it.playerId == departed.id) member(it.slotId, signed, season) else it }
    }

    /** A summer signing into [slotId], replacing whoever was there. */
    fun sign(state: DynastyState, slotId: String, pick: DraftPick): DynastyState = state.copy(
        squad = state.squad.filter { it.slotId != slotId } + member(slotId, pick, state.season + 1),
        signingsLeft = (state.signingsLeft - 1).coerceAtLeast(0),
    )

    fun nextSeason(state: DynastyState): DynastyState =
        preseason(state.copy(season = state.season + 1, changes = emptyList(), signingsLeft = 0))

    fun legacy(state: DynastyState) = Legacy(
        kind = LegacyKind.MANAGER,
        name = state.managerName,
        seasons = state.history.size,
        trophies = state.history.flatMap { it.trophies }.groupingBy { it }.eachCount(),
        sacked = state.sacked,
        won = state.history.sumOf { it.won },
        drawn = state.history.sumOf { it.drawn },
        lost = state.history.sumOf { it.lost },
    )
}
