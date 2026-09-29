package com.example.footygame.game

import com.example.footygame.data.ClubSeasons
import com.example.footygame.models.Award
import com.example.footygame.models.ClubSeason
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftPick
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.Formation
import com.example.footygame.models.Legacy
import com.example.footygame.models.LegacyKind
import com.example.footygame.models.Offer
import com.example.footygame.models.Player
import com.example.footygame.models.Position
import com.example.footygame.models.ProPhase
import com.example.footygame.models.ProSeason
import com.example.footygame.models.ProState
import com.example.footygame.models.RunResult
import com.example.footygame.models.SeasonPhase
import com.example.footygame.models.Trophy
import com.example.footygame.models.Verdict
import com.example.footygame.models.seasonTrophies
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Player career rules: a 17-year-old joins a real Premier League squad, fights for a place in its XI,
 * grows towards a hidden potential and moves clubs each summer until retiring.
 */
object ProCareer {
    const val PLAYER_ID = "you"
    const val START_AGE = 17
    const val CAN_RETIRE_AGE = 33
    val FORMATION = Formation.F433

    private const val RETIREMENT_AGE = 38
    private const val MIN_RATING = 60
    private const val ERA_SPREAD = 8
    private const val OFFERS = 3

    fun settings() = DraftSettings(
        formation = FORMATION,
        managers = false,
        europeanNights = true,
        januaryWindow = false,
        domesticCup = true,
    )

    fun newCareer(name: String, position: Position, startYear: Int, seed: Long): ProState {
        val random = Random(seed)
        val state = ProState(
            name = name.trim(),
            position = position,
            seed = seed,
            startYear = startYear,
            age = START_AGE,
            rating = 60 + random.nextInt(0, 5),
            potential = 80 + random.nextInt(0, 15),
        )
        return state.copy(offers = firstClubs(state, random))
    }

    /** The English clubs of [year]: each club at its nearest season we have, if that's within [ERA_SPREAD] years. */
    fun clubsIn(year: Int): List<ClubSeason> {
        val nearest = ClubSeasons.poolFor(DraftMode.EPL)
            .groupBy { it.club }
            .map { (_, seasons) -> seasons.minBy { abs(it.startYear - year) } }
        return nearest.filter { abs(it.startYear - year) <= ERA_SPREAD }.ifEmpty { nearest }
    }

    fun clubSquad(club: String, year: Int): ClubSeason? =
        ClubSeasons.poolFor(DraftMode.EPL).filter { it.club == club }.minByOrNull { abs(it.startYear - year) }

    fun player(state: ProState, rating: Int = state.rating) = Player(
        id = PLAYER_ID,
        name = state.name,
        shortName = state.name.split(' ').last { it.isNotBlank() },
        position = state.position,
        rating = rating,
    )

    /** The club's best XI with the player in the squad; they start only if they're good enough. */
    fun lineup(state: ProState, squad: ClubSeason, rating: Int = state.rating): Map<String, DraftPick> {
        val candidates = squad.players.map { DraftPick(it, squad) } + DraftPick(player(state, rating), squad)
        return Lineup.best(candidates, FORMATION)
    }

    fun starts(state: ProState, squad: ClubSeason, rating: Int = state.rating): Boolean =
        lineup(state, squad, rating).values.any { it.player.id == PLAYER_ID }

    fun session(state: ProState, squad: ClubSeason) =
        DraftSession(mode = DraftMode.EPL, settings = settings(), picks = lineup(state, squad))

    fun seasonSeed(state: ProState): Long = Random(state.seed + state.season * 7919L).nextLong()

    private fun offer(state: ProState, squad: ClubSeason) =
        Offer(squad.id, squad.club, DraftEngine.strength(squad).roundToInt(), starts(state, squad))

    /** A youngster's first move: three of the weaker half of the league. */
    private fun firstClubs(state: ProState, random: Random): List<Offer> {
        val clubs = clubsIn(state.year).sortedBy { DraftEngine.strength(it) }
        return clubs.take((clubs.size / 2).coerceAtLeast(OFFERS)).shuffled(random).take(OFFERS)
            .map { offer(state, it) }
            .sortedByDescending { it.strength }
    }

    fun chooseFirstClub(state: ProState, offer: Offer): ProState = state.copy(
        club = offer.club,
        phase = ProPhase.SEASON,
        seasonPhase = SeasonPhase.PRESEASON,
        offers = emptyList(),
    )

    /**
     * What a season meant for the player. Starters play every match and score what the simulator gave them;
     * squad players get a share of league games that shrinks with the gap to the starter ahead of them.
     */
    fun report(state: ProState, squad: ClubSeason, result: RunResult): ProSeason {
        val random = Random(state.seed xor state.season.toLong() * 104_729)
        val picks = lineup(state, squad)
        val starter = picks.values.any { it.player.id == PLAYER_ID }
        val all = result.matches + result.cup?.matches.orEmpty() + result.europe?.matches.orEmpty()
        val appearances: Int
        val goals: Int
        if (starter) {
            appearances = all.size
            goals = all.sumOf { match -> match.scorerIds.count { it == PLAYER_ID } }
        } else {
            val ahead = picks.values.filter { it.player.position == state.position }.minOfOrNull { it.player.rating } ?: state.rating
            val share = (0.5 - 0.05 * (ahead - state.rating)).coerceIn(0.15, 0.5)
            appearances = (result.matches.size * share).roundToInt()
            val perGame = goalRate(state.position) * (state.rating / 80.0).pow(2) * 0.5
            goals = (1..appearances).count { random.nextDouble() < perGame }
        }
        val leagueGoals = if (starter) result.matches.sumOf { m -> m.scorerIds.count { it == PLAYER_ID } } else goals
        val position = (result.verdict as? Verdict.LeagueFinish)?.position ?: 0
        val trophies = result.seasonTrophies
        val after = develop(state.age, state.rating, state.potential, starter, random)
        return ProSeason(
            season = state.season,
            age = state.age,
            club = squad.club,
            squadId = squad.id,
            starter = starter,
            appearances = appearances,
            goals = goals,
            ratingBefore = state.rating,
            ratingAfter = after,
            position = position,
            trophies = trophies,
            awards = awards(state, starter, leagueGoals, position, trophies, random),
        )
    }

    private fun goalRate(position: Position) = when (position) {
        Position.ATT -> 0.45
        Position.MID -> 0.15
        Position.DEF -> 0.04
        Position.GK -> 0.0
    }

    /** Growth by age, a push for regular football, a little luck, and never past potential. */
    fun develop(age: Int, rating: Int, potential: Int, starter: Boolean, random: Random): Int {
        val base = when {
            age <= 18 -> 5
            age <= 20 -> 4
            age <= 22 -> 3
            age <= 24 -> 2
            age <= 27 -> 1
            age <= 29 -> 0
            age <= 31 -> -1
            age <= 33 -> -2
            else -> -3
        }
        val minutes = when {
            starter -> 1
            age <= 23 -> -1
            else -> 0
        }
        val change = base + minutes + random.nextInt(-1, 2)
        return if (change > 0) minOf(rating + change, maxOf(potential, rating)) else rating + change
    }

    private fun awards(
        state: ProState,
        starter: Boolean,
        leagueGoals: Int,
        position: Int,
        trophies: List<Trophy>,
        random: Random,
    ): List<Award> {
        if (!starter) return emptyList()
        // The best of the rest scores somewhere between 19 and 27 in a season.
        val rivalGoals = 19 + random.nextInt(0, 9)
        return listOfNotNull(
            Award.GOLDEN_BOOT.takeIf { leagueGoals > rivalGoals },
            Award.PLAYER_OF_THE_SEASON.takeIf { state.rating >= 87 && position <= 2 },
            Award.YOUNG_PLAYER.takeIf { state.age <= 21 && state.rating >= 76 },
            Award.BALLON_DOR.takeIf {
                state.rating >= 91 && (Trophy.LEAGUE in trophies || Trophy.CHAMPIONS_LEAGUE in trophies)
            },
        )
    }

    /**
     * Books the season. Age and rating stay as they were until the summer, so the season still replays the
     * same from its seed while the review is on screen.
     */
    fun review(state: ProState, season: ProSeason): ProState =
        state.copy(history = state.history + season, seasonPhase = SeasonPhase.REVIEW)

    /** A year older with the season's new rating. */
    fun grown(state: ProState): ProState =
        state.copy(age = state.age + 1, rating = state.history.lastOrNull()?.ratingAfter ?: state.rating)

    /** Out of the review: into the summer's offers, or retirement when the body says so. */
    fun afterReview(state: ProState): ProState {
        val grown = grown(state)
        return if (mustRetire(grown)) retire(grown) else transfers(grown)
    }

    fun mustRetire(state: ProState): Boolean = state.age >= RETIREMENT_AGE || state.rating < MIN_RATING

    fun canRetire(state: ProState): Boolean = state.age >= CAN_RETIRE_AGE || mustRetire(state)

    /**
     * Summer offers from clubs next season whose level suits the player: from a little below their rating
     * up to a little above, so a rising star gets bigger clubs and a fading one smaller.
     */
    fun transfers(state: ProState): ProState {
        val random = Random(state.seed + state.season * 31L)
        val next = state.copy(season = state.season + 1)
        val clubs = clubsIn(next.year).filter { it.club != state.club }
        val fitting = clubs.filter { DraftEngine.strength(it).roundToInt() in (state.rating - 6)..(state.rating + 3) }
        val pool = fitting.ifEmpty { clubs.sortedBy { abs(DraftEngine.strength(it) - state.rating) }.take(5) }
        val offers = pool.shuffled(random).take(OFFERS).map { offer(next, it) }.sortedByDescending { it.strength }
        return state.copy(phase = ProPhase.TRANSFERS, offers = offers)
    }

    /** Next season at [club]: the current one to stay, or an offer's. */
    fun nextSeason(state: ProState, club: String): ProState = state.copy(
        season = state.season + 1,
        club = club,
        phase = ProPhase.SEASON,
        seasonPhase = SeasonPhase.PRESEASON,
        offers = emptyList(),
    )

    fun retire(state: ProState): ProState = state.copy(phase = ProPhase.RETIRED, offers = emptyList())

    fun legacy(state: ProState) = Legacy(
        kind = LegacyKind.PLAYER,
        name = state.name,
        seasons = state.history.size,
        trophies = state.history.flatMap { it.trophies }.groupingBy { it }.eachCount(),
        goals = state.goals,
        appearances = state.appearances,
        peakRating = state.history.maxOfOrNull { maxOf(it.ratingBefore, it.ratingAfter) } ?: state.rating,
        awards = state.history.sumOf { it.awards.size },
        clubs = state.history.map { it.club }.distinct(),
    )
}
