package com.example.footygame.game

import com.example.footygame.data.ClubSeasons
import com.example.footygame.data.Opponents
import com.example.footygame.models.ClubSeason
import com.example.footygame.models.Competition
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftPick
import com.example.footygame.models.DraftSession
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.EuropeanRun
import com.example.footygame.models.JanuaryEvent
import com.example.footygame.models.JanuaryOutcome
import com.example.footygame.models.Manager
import com.example.footygame.models.ManagerTrait
import com.example.footygame.models.MatchResult
import com.example.footygame.models.Opponent
import com.example.footygame.models.Player
import com.example.footygame.models.Position
import com.example.footygame.models.RunResult
import com.example.footygame.models.ScorerTally
import com.example.footygame.models.Stage
import com.example.footygame.models.StageType
import com.example.footygame.models.TableRow
import com.example.footygame.models.Venue
import com.example.footygame.models.Verdict
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

sealed interface Simulation {
    data class Complete(val result: RunResult) : Simulation

    /** The run stopped at the January transfer window: the matches so far and the gambles on offer. */
    data class TransferWindow(val played: List<MatchResult>, val offers: List<JanuaryEvent>) : Simulation
}

/**
 * Plays a drafted XI through a competition. Goals are Poisson draws whose means depend on the gap
 * between our attack/defence and the opponent's rating, so stronger teams win more but never always.
 */
class SeasonSimulator(
    private val poolFor: (DraftMode, DraftSettings) -> List<ClubSeason> = ClubSeasons::poolFor,
) {
    /**
     * The same [seed] always produces the same run. That is how the transfer window works: the first call
     * stops at the window, and calling again with the user's [january] choice replays the first half exactly
     * before applying it.
     */
    fun simulate(session: DraftSession, seed: Long, january: JanuaryEvent? = null): Simulation {
        require(session.isComplete) { "Can't simulate an incomplete XI" }
        return try {
            Simulation.Complete(Run(session, Random(seed), january).play())
        } catch (pause: WindowPause) {
            pause.window
        }
    }

    /** Unwinds a run to the transfer window. Never escapes [simulate]. */
    private class WindowPause(val window: Simulation.TransferWindow) : RuntimeException(null, null, false, false)

    private class Team(
        val picks: Map<String, DraftPick>,
        val manager: Manager?,
        private val bonusAttack: Double = 0.0,
        private val bonusDefence: Double = 0.0,
    ) {
        private val ratings = TeamRatings.of(picks.values)
        private val chemistryAgain =
            if (manager?.trait == ManagerTrait.MAN_MANAGER) ratings.chemistry * TeamRatings.CHEMISTRY_BOOST else 0.0
        private val traitAttack = when (manager?.trait) {
            ManagerTrait.ATTACKING -> TRAIT_BOOST
            ManagerTrait.TACTICIAN -> TACTICIAN_BOOST
            else -> 0.0
        }
        private val traitDefence = when (manager?.trait) {
            ManagerTrait.DEFENSIVE -> TRAIT_BOOST
            ManagerTrait.TACTICIAN -> TACTICIAN_BOOST
            else -> 0.0
        }
        val attack = ratings.attackBoosted + chemistryAgain + traitAttack + bonusAttack
        val defence = ratings.defenceBoosted + chemistryAgain + traitDefence + bonusDefence
        val players: List<Player> = picks.values.map { it.player }
        val goalkeeper: Int = players.filter { it.position == Position.GK }.maxOfOrNull { it.rating } ?: 70
        val scoringWeights: List<Pair<Player, Double>> = players.map { it to scoringWeight(it) }
        val totalScoringWeight = scoringWeights.sumOf { it.second }

        fun bigGameBonus(opponent: Opponent, stage: Stage): Double =
            if (manager?.trait == ManagerTrait.BIG_GAMES && (opponent.rating >= BIG_GAME_RATING || stage.type.isKnockout)) {
                BIG_GAME_BOOST
            } else {
                0.0
            }

        fun outfield(): List<Map.Entry<String, DraftPick>> = picks.entries.filter { it.value.player.position != Position.GK }

        fun with(slotId: String, pick: DraftPick) = Team(picks + (slotId to pick), manager, bonusAttack, bonusDefence)

        fun plus(attack: Double, defence: Double) = Team(picks, manager, bonusAttack + attack, bonusDefence + defence)

        private fun scoringWeight(player: Player): Double {
            val base = when (player.position) {
                Position.ATT -> 6.0
                Position.MID -> 2.2
                Position.DEF -> 0.5
                Position.GK -> 0.01
            }
            return base * (player.rating / 85.0).pow(3)
        }
    }

    private data class Expectation(val forUs: Double, val againstUs: Double)

    private inner class Run(
        private val session: DraftSession,
        private val random: Random,
        private val januaryChoice: JanuaryEvent?,
    ) {
        private val mode = session.mode
        private var team = Team(session.picks, session.manager)
        private val matches = mutableListOf<MatchResult>()
        private val goals = mutableMapOf<String, Int>()
        private val knownPlayers = team.players.associateBy { it.id }.toMutableMap()
        private var januaryOutcome: JanuaryOutcome? = null

        fun play(): RunResult = when (mode) {
            DraftMode.EPL -> league()
            DraftMode.UCL -> result(campaign(CHAMPIONS_LEAGUE, matches, windowAfterLeaguePhase = true))
            DraftMode.WC -> worldCup()
            DraftMode.FAC -> faCup()
        }

        private fun league(): RunResult {
            val opponents = Opponents.premierLeague.shuffled(random).take(mode.matches / 2)
            val firstHalf = opponents.shuffled(random)
                .mapIndexed { index, opponent -> opponent to if (index % 2 == 0) Venue.HOME else Venue.AWAY }
            val secondHalf = firstHalf.shuffled(random).map { (opponent, venue) -> opponent to venue.opposite() }

            val table = (opponents.map { TableRow(it.name, isUser = false) } + TableRow(USER_ROW, isUser = true))
                .associateBy { it.name }
                .toMutableMap()

            (firstHalf + secondHalf).forEachIndexed { index, (opponent, venue) ->
                if (index == firstHalf.size) januaryCheckpoint()
                val match = play(Stage(StageType.MATCHDAY, number = index + 1), opponent, venue)
                table[USER_ROW] = table.getValue(USER_ROW).plus(match.goalsFor, match.goalsAgainst)
                table[opponent.name] = table.getValue(opponent.name).plus(match.goalsAgainst, match.goalsFor)
            }

            for (home in opponents) {
                for (away in opponents) {
                    if (home == away) continue
                    val homeGoals = poisson(xg(home.rating.toDouble(), away.rating.toDouble()) * HOME_EDGE)
                    val awayGoals = poisson(xg(away.rating.toDouble(), home.rating.toDouble()) / HOME_EDGE)
                    table[home.name] = table.getValue(home.name).plus(homeGoals, awayGoals)
                    table[away.name] = table.getValue(away.name).plus(awayGoals, homeGoals)
                }
            }

            val standings = table.values.sortedWith(
                compareByDescending<TableRow> { it.points }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.goalsFor },
            )
            val user = standings.first { it.isUser }
            val position = standings.indexOf(user) + 1
            val europe = if (session.settings.europeanNights && position <= EUROPEAN_PLACES) europeanNights(position) else null
            return result(Verdict.LeagueFinish(position, user.points), standings, europe)
        }

        /** Top four go to the Champions League, fifth to the Europa League, sixth and seventh to the Conference League. */
        private fun europeanNights(position: Int): EuropeanRun {
            val competition = when {
                position <= 4 -> Competition.CHAMPIONS_LEAGUE
                position == 5 -> Competition.EUROPA_LEAGUE
                else -> Competition.CONFERENCE_LEAGUE
            }
            val format = when (competition) {
                Competition.CHAMPIONS_LEAGUE -> CHAMPIONS_LEAGUE
                Competition.EUROPA_LEAGUE -> EUROPA_LEAGUE
                Competition.CONFERENCE_LEAGUE -> CONFERENCE_LEAGUE
            }
            val europeMatches = mutableListOf<MatchResult>()
            val verdict = campaign(format, europeMatches, windowAfterLeaguePhase = false)
            return EuropeanRun(competition, europeMatches, verdict)
        }

        /** League phase, two-legged knockout ties, one final. */
        private fun campaign(format: CampaignFormat, into: MutableList<MatchResult>, windowAfterLeaguePhase: Boolean): Verdict {
            val used = mutableSetOf<String>()
            val leaguePhase = format.leaguePhase.flatMap { (tier, count) -> draw(tier, count, used) }.shuffled(random)
            val homeGames = leaguePhase.size / 2
            val venues = (List(homeGames) { Venue.HOME } + List(leaguePhase.size - homeGames) { Venue.AWAY }).shuffled(random)
            var record = TableRow(USER_ROW, isUser = true)
            leaguePhase.forEachIndexed { index, opponent ->
                val match = play(Stage(StageType.LEAGUE_PHASE, number = index + 1), opponent, venues[index], into)
                record = record.plus(match.goalsFor, match.goalsAgainst)
            }
            if (record.points < format.knockoutPoints) return Verdict.Eliminated(Stage(StageType.LEAGUE_PHASE), null)
            if (windowAfterLeaguePhase) januaryCheckpoint()

            val knockoutUsed = mutableSetOf<String>()
            for ((type, tier) in format.ties) {
                val opponent = draw(tier, 1, knockoutUsed).single()
                if (!twoLegged(type, opponent, into)) return Verdict.Eliminated(Stage(type), opponent)
            }
            val finalist = draw(format.final, 1, knockoutUsed).single()
            return if (knockout(Stage(StageType.FINAL), finalist, Venue.NEUTRAL, into = into)) {
                Verdict.Champions
            } else {
                Verdict.Eliminated(Stage(StageType.FINAL), finalist)
            }
        }

        private fun worldCup(): RunResult {
            val used = mutableSetOf<String>()
            val group = (
                draw(Opponents.Nations.strong, 1, used) + draw(Opponents.Nations.mid, 1, used) +
                    draw(Opponents.Nations.low, 1, used)
                ).shuffled(random)
            var record = TableRow(USER_ROW, isUser = true)
            group.forEachIndexed { index, opponent ->
                val match = play(Stage(StageType.GROUP_STAGE, number = index + 1), opponent, Venue.NEUTRAL)
                record = record.plus(match.goalsFor, match.goalsAgainst)
            }
            // Top two plus the best third-placed sides go through: four points is safe, three needs a tidy goal difference.
            val advances = record.points >= 4 || (record.points == 3 && record.goalDifference >= 0)
            if (!advances) return result(Verdict.Eliminated(Stage(StageType.GROUP_STAGE), null))

            val rounds = listOf(
                StageType.ROUND_OF_32 to Opponents.Nations.mid,
                StageType.ROUND_OF_16 to Opponents.Nations.strong,
                StageType.QUARTER_FINAL to Opponents.Nations.strong + Opponents.Nations.elite,
                StageType.SEMI_FINAL to Opponents.Nations.elite,
                StageType.FINAL to Opponents.Nations.elite,
            )
            for ((type, tier) in rounds) {
                val opponent = draw(tier, 1, used).single()
                if (!knockout(Stage(type), opponent, Venue.NEUTRAL)) {
                    return result(Verdict.Eliminated(Stage(type), opponent))
                }
            }
            return result(Verdict.Champions)
        }

        private fun faCup(): RunResult {
            val used = mutableSetOf<String>()
            for (round in FA_CUP_ROUNDS) {
                // The third round, where the big clubs enter, is played in January.
                if (round.stage.type == StageType.THIRD_ROUND) januaryCheckpoint()
                val name = round.clubs.filter { it !in used }.random(random).also { used += it }
                val opponent = Opponent(name, round.rating + random.nextInt(-2, 3))
                val venue = when {
                    round.stage.type == StageType.SEMI_FINAL || round.stage.type == StageType.FINAL -> Venue.NEUTRAL
                    random.nextBoolean() -> Venue.HOME
                    else -> Venue.AWAY
                }
                if (!knockout(round.stage, opponent, venue)) return result(Verdict.Eliminated(round.stage, opponent))
            }
            return result(Verdict.Champions)
        }

        /** Stops the run for the user's choice, or applies the choice they already made. */
        private fun januaryCheckpoint() {
            if (!session.settings.januaryWindow || !mode.hasJanuaryWindow) return
            val offers = JanuaryEvent.entries.shuffled(random).take(JANUARY_OFFERS)
            val choice = januaryChoice?.takeIf { it in offers }
                ?: throw WindowPause(Simulation.TransferWindow(matches.toList(), offers))
            applyJanuary(choice)
        }

        private fun applyJanuary(event: JanuaryEvent) {
            val afterMatches = matches.size
            val success = random.nextDouble() < successChance(event)
            val outcome = when (event) {
                JanuaryEvent.MARQUEE_SIGNING -> {
                    val weakest = team.outfield().minBy { it.value.player.rating }
                    val star = signingCandidates(weakest.key) { it >= STAR_RATING }.randomOrNull(random)
                    if (star == null) {
                        JanuaryOutcome(event, success, afterMatches, attackChange = if (success) 2.0 else -1.5)
                    } else {
                        // A flop still arrives, but never plays anywhere near their rating.
                        val signed = if (success) star else star.copy(player = star.player.copy(rating = FLOP_RATING))
                        sign(weakest.key, signed)
                        JanuaryOutcome(event, success, afterMatches, signed = signed, departed = weakest.value.player)
                    }
                }

                JanuaryEvent.LOAN_PROSPECT -> {
                    val lift = if (success) LOAN_BOOST else 0.0
                    JanuaryOutcome(event, success, afterMatches, attackChange = lift, defenceChange = lift)
                }

                JanuaryEvent.SELL_STAR -> {
                    val best = team.outfield().maxBy { it.value.player.rating }
                    val replacement = signingCandidates(best.key) { it in REPLACEMENT_RATINGS }.randomOrNull(random)
                    if (replacement != null) sign(best.key, replacement)
                    JanuaryOutcome(
                        event, success, afterMatches,
                        signed = replacement,
                        departed = best.value.player.takeIf { replacement != null },
                        attackChange = if (success) 2.5 else -1.5,
                        defenceChange = if (success) 2.0 else -1.5,
                    )
                }

                JanuaryEvent.FITNESS_COACH ->
                    JanuaryOutcome(event, success, afterMatches, defenceChange = if (success) 2.5 else -2.0)
            }
            team = team.plus(outcome.attackChange, outcome.defenceChange)
            januaryOutcome = outcome
        }

        private fun successChance(event: JanuaryEvent) = when (event) {
            JanuaryEvent.MARQUEE_SIGNING -> 0.55
            JanuaryEvent.LOAN_PROSPECT -> 0.7
            JanuaryEvent.SELL_STAR -> 0.5
            JanuaryEvent.FITNESS_COACH -> 0.55
        }

        /** Players from this draft's pool who could take over [slotId] and aren't already in the XI. */
        private fun signingCandidates(slotId: String, ratingFits: (Int) -> Boolean): List<DraftPick> {
            val slot = session.formation.slots.first { it.id == slotId }
            val inTeam = team.players.mapTo(HashSet()) { it.id }
            return poolFor(mode, session.settings)
                .flatMap { squad -> squad.players.map { DraftPick(it, squad) } }
                .filter { it.player.position in slot.accepts && it.player.id !in inTeam && ratingFits(it.player.rating) }
                .distinctBy { it.player.id }
        }

        private fun sign(slotId: String, pick: DraftPick) {
            team = team.with(slotId, pick)
            knownPlayers[pick.player.id] = pick.player
        }

        private fun play(stage: Stage, opponent: Opponent, venue: Venue, into: MutableList<MatchResult> = matches): MatchResult {
            val expected = expectation(opponent, venue, stage)
            return record(MatchResult(stage, opponent, venue, poisson(expected.forUs), poisson(expected.againstUs)), into)
        }

        /**
         * A match that must produce a winner: extra time if level, then penalties. On the second leg of a
         * tie, [firstLeg] goals count towards the aggregate. Returns true if we go through.
         */
        private fun knockout(
            stage: Stage,
            opponent: Opponent,
            venue: Venue,
            firstLeg: MatchResult? = null,
            into: MutableList<MatchResult> = matches,
        ): Boolean {
            val expected = expectation(opponent, venue, stage)
            val carriedFor = firstLeg?.goalsFor ?: 0
            val carriedAgainst = firstLeg?.goalsAgainst ?: 0
            var scored = poisson(expected.forUs)
            var conceded = poisson(expected.againstUs)
            var extraTime = false
            var penalties: Pair<Int, Int>? = null
            if (carriedFor + scored == carriedAgainst + conceded) {
                extraTime = true
                scored += poisson(expected.forUs * EXTRA_TIME_SHARE)
                conceded += poisson(expected.againstUs * EXTRA_TIME_SHARE)
                if (carriedFor + scored == carriedAgainst + conceded) penalties = shootout()
            }
            val total = (carriedFor + scored) to (carriedAgainst + conceded)
            record(
                MatchResult(
                    stage, opponent, venue, scored, conceded, extraTime, penalties,
                    aggregate = total.takeIf { firstLeg != null },
                ),
                into,
            )
            return total.first > total.second || (penalties != null && penalties.first > penalties.second)
        }

        private fun twoLegged(type: StageType, opponent: Opponent, into: MutableList<MatchResult>): Boolean {
            val firstVenue = if (random.nextBoolean()) Venue.HOME else Venue.AWAY
            val firstLeg = play(Stage(type, leg = 1), opponent, firstVenue, into)
            return knockout(Stage(type, leg = 2), opponent, firstVenue.opposite(), firstLeg, into)
        }

        private fun expectation(opponent: Opponent, venue: Venue, stage: Stage): Expectation {
            val (ourEdge, theirEdge) = when (venue) {
                Venue.HOME -> HOME_EDGE to 1 / HOME_EDGE
                Venue.AWAY -> 1 / HOME_EDGE to HOME_EDGE
                Venue.NEUTRAL -> 1.0 to 1.0
            }
            val rating = opponent.rating.toDouble()
            val lift = team.bigGameBonus(opponent, stage)
            return Expectation(xg(team.attack + lift, rating) * ourEdge, xg(rating, team.defence + lift) * theirEdge)
        }

        /** Best of five, stopping once one side can't catch up, then sudden death. */
        private fun shootout(): Pair<Int, Int> {
            val theirConversion = (PENALTY_CONVERSION - (team.goalkeeper - 80) * 0.006).coerceIn(0.6, 0.85)
            var ours = 0
            var theirs = 0
            for (round in 1..5) {
                if (random.nextDouble() < PENALTY_CONVERSION) ours++
                if (ours > theirs + (6 - round) || theirs > ours + (5 - round)) break
                if (random.nextDouble() < theirConversion) theirs++
                if (ours > theirs + (5 - round) || theirs > ours + (5 - round)) break
            }
            while (ours == theirs) {
                if (random.nextDouble() < PENALTY_CONVERSION) ours++
                if (random.nextDouble() < theirConversion) theirs++
            }
            return ours to theirs
        }

        private fun record(match: MatchResult, into: MutableList<MatchResult>): MatchResult {
            val scorers = List(match.goalsFor) { pickScorer().id }
            scorers.forEach { goals[it] = (goals[it] ?: 0) + 1 }
            return match.copy(scorerIds = scorers).also { into += it }
        }

        private fun pickScorer(): Player {
            var roll = random.nextDouble() * team.totalScoringWeight
            for ((player, weight) in team.scoringWeights) {
                roll -= weight
                if (roll <= 0) return player
            }
            return team.scoringWeights.last().first
        }

        private fun draw(tier: List<Opponent>, count: Int, used: MutableSet<String>): List<Opponent> {
            val available = tier.filter { it.name !in used }.ifEmpty { tier }
            return available.shuffled(random).take(count).also { picked -> used += picked.map { it.name } }
        }

        private fun xg(attack: Double, defence: Double): Double =
            (BASE_GOALS * exp((attack - defence) / RATING_SCALE)).coerceIn(MIN_XG, MAX_XG)

        private fun poisson(mean: Double): Int {
            val limit = exp(-mean)
            var count = 0
            var product = random.nextDouble()
            while (product > limit) {
                count++
                product *= random.nextDouble()
            }
            return count
        }

        private fun result(verdict: Verdict, table: List<TableRow> = emptyList(), europe: EuropeanRun? = null): RunResult {
            val topScorers = goals.entries
                .sortedByDescending { it.value }
                .take(TOP_SCORERS)
                .map { ScorerTally(knownPlayers.getValue(it.key), it.value) }
            return RunResult(
                mode = mode,
                matches = matches.toList(),
                verdict = verdict,
                table = table,
                topScorers = topScorers,
                manager = session.manager,
                january = januaryOutcome,
                europe = europe,
            )
        }
    }

    private data class FaCupRound(val stage: Stage, val rating: Int, val clubs: List<String>)

    private data class CampaignFormat(
        val leaguePhase: List<Pair<List<Opponent>, Int>>,
        val knockoutPoints: Int,
        val ties: List<Pair<StageType, List<Opponent>>>,
        val final: List<Opponent>,
    )

    companion object {
        /** Name of the user's row in a league table. */
        const val USER_ROW = ""

        // Tuned so the best possible XI averages ~33 league wins and a perfect season stays well under 1 in 20.
        private const val BASE_GOALS = 1.2
        private const val RATING_SCALE = 15.0
        private const val MIN_XG = 0.08
        private const val MAX_XG = 5.5
        private const val HOME_EDGE = 1.1
        private const val EXTRA_TIME_SHARE = 1 / 3.0
        private const val PENALTY_CONVERSION = 0.76
        private const val TOP_SCORERS = 3
        private const val EUROPEAN_PLACES = 7

        private const val TRAIT_BOOST = 2.5
        private const val TACTICIAN_BOOST = 1.5
        private const val BIG_GAME_BOOST = 3.0
        private const val BIG_GAME_RATING = 84

        private const val JANUARY_OFFERS = 3
        private const val STAR_RATING = 89
        private const val FLOP_RATING = 74
        private const val LOAN_BOOST = 1.5
        private val REPLACEMENT_RATINGS = 80..85

        private val CHAMPIONS_LEAGUE = CampaignFormat(
            leaguePhase = listOf(
                Opponents.Europe.elite to 2, Opponents.Europe.strong to 2,
                Opponents.Europe.mid to 2, Opponents.Europe.low to 2,
            ),
            knockoutPoints = 10,
            ties = listOf(
                StageType.KNOCKOUT_PLAYOFF to Opponents.Europe.strong + Opponents.Europe.mid,
                StageType.ROUND_OF_16 to Opponents.Europe.strong,
                StageType.QUARTER_FINAL to Opponents.Europe.elite + Opponents.Europe.strong,
                StageType.SEMI_FINAL to Opponents.Europe.elite,
            ),
            final = Opponents.Europe.elite,
        )

        private val EUROPA_LEAGUE = CampaignFormat(
            leaguePhase = listOf(
                Opponents.EuropaLeague.strong to 2, Opponents.EuropaLeague.mid to 3, Opponents.EuropaLeague.low to 3,
            ),
            knockoutPoints = 10,
            ties = listOf(
                StageType.KNOCKOUT_PLAYOFF to Opponents.EuropaLeague.mid,
                StageType.ROUND_OF_16 to Opponents.EuropaLeague.strong + Opponents.EuropaLeague.mid,
                StageType.QUARTER_FINAL to Opponents.EuropaLeague.strong,
                StageType.SEMI_FINAL to Opponents.EuropaLeague.strong,
            ),
            final = Opponents.EuropaLeague.strong,
        )

        private val CONFERENCE_LEAGUE = CampaignFormat(
            leaguePhase = listOf(
                Opponents.ConferenceLeague.strong to 2, Opponents.ConferenceLeague.mid to 2,
                Opponents.ConferenceLeague.low to 2,
            ),
            knockoutPoints = 8,
            ties = listOf(
                StageType.KNOCKOUT_PLAYOFF to Opponents.ConferenceLeague.mid,
                StageType.ROUND_OF_16 to Opponents.ConferenceLeague.strong + Opponents.ConferenceLeague.mid,
                StageType.QUARTER_FINAL to Opponents.ConferenceLeague.strong,
                StageType.SEMI_FINAL to Opponents.ConferenceLeague.strong,
            ),
            final = Opponents.ConferenceLeague.strong,
        )

        private val FA_CUP_ROUNDS = listOf(
            FaCupRound(Stage(StageType.EXTRA_PRELIMINARY_ROUND), 52, Opponents.FaCup.nonLeague),
            FaCupRound(Stage(StageType.PRELIMINARY_ROUND), 55, Opponents.FaCup.nonLeague),
            FaCupRound(Stage(StageType.QUALIFYING_ROUND, number = 1), 58, Opponents.FaCup.nonLeague),
            FaCupRound(Stage(StageType.QUALIFYING_ROUND, number = 2), 60, Opponents.FaCup.nonLeague),
            FaCupRound(Stage(StageType.QUALIFYING_ROUND, number = 3), 63, Opponents.FaCup.nonLeague),
            FaCupRound(Stage(StageType.QUALIFYING_ROUND, number = 4), 65, Opponents.FaCup.nonLeague),
            FaCupRound(Stage(StageType.FIRST_ROUND), 68, Opponents.FaCup.footballLeague),
            FaCupRound(Stage(StageType.SECOND_ROUND), 70, Opponents.FaCup.footballLeague),
            FaCupRound(Stage(StageType.THIRD_ROUND), 75, Opponents.FaCup.championship),
            FaCupRound(Stage(StageType.FOURTH_ROUND), 77, Opponents.FaCup.championship),
            FaCupRound(Stage(StageType.FIFTH_ROUND), 79, Opponents.FaCup.premierLeague),
            FaCupRound(Stage(StageType.QUARTER_FINAL), 81, Opponents.FaCup.premierLeague),
            FaCupRound(Stage(StageType.SEMI_FINAL), 84, Opponents.FaCup.giants),
            FaCupRound(Stage(StageType.FINAL), 86, Opponents.FaCup.giants),
        )
    }
}
