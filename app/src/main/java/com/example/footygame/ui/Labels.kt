package com.example.footygame.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.footygame.R
import com.example.footygame.models.Competition
import com.example.footygame.models.Difficulty
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftStyle
import com.example.footygame.models.EraPreset
import com.example.footygame.models.Formation
import com.example.footygame.models.JanuaryEvent
import com.example.footygame.models.JanuaryOutcome
import com.example.footygame.models.ManagerTrait
import com.example.footygame.models.MatchResult
import com.example.footygame.models.Outcome
import com.example.footygame.models.Position
import com.example.footygame.models.RatingMode
import com.example.footygame.models.Stage
import com.example.footygame.models.StageType
import com.example.footygame.models.Venue

@get:StringRes
val DraftMode.titleRes: Int
    get() = when (this) {
        DraftMode.EPL -> R.string.mode_epl_title
        DraftMode.UCL -> R.string.mode_ucl_title
        DraftMode.WC -> R.string.mode_wc_title
        DraftMode.FAC -> R.string.mode_fac_title
    }

@get:StringRes
val DraftMode.descriptionRes: Int
    get() = when (this) {
        DraftMode.EPL -> R.string.mode_epl_description
        DraftMode.UCL -> R.string.mode_ucl_description
        DraftMode.WC -> R.string.mode_wc_description
        DraftMode.FAC -> R.string.mode_fac_description
    }

@get:StringRes
val DraftMode.playActionRes: Int
    get() = when (this) {
        DraftMode.EPL -> R.string.draft_play_epl
        DraftMode.UCL -> R.string.draft_play_ucl
        DraftMode.WC -> R.string.draft_play_wc
        DraftMode.FAC -> R.string.draft_play_fac
    }

@get:StringRes
val Position.groupRes: Int
    get() = when (this) {
        Position.GK -> R.string.position_gk
        Position.DEF -> R.string.position_def
        Position.MID -> R.string.position_mid
        Position.ATT -> R.string.position_att
    }

@get:StringRes
val Formation.descriptionRes: Int
    get() = when (this) {
        Formation.F433 -> R.string.formation_desc_433
        Formation.F442 -> R.string.formation_desc_442
        Formation.F4231 -> R.string.formation_desc_4231
        Formation.F451 -> R.string.formation_desc_451
        Formation.F343 -> R.string.formation_desc_343
        Formation.F352 -> R.string.formation_desc_352
        Formation.F541 -> R.string.formation_desc_541
        Formation.F41212 -> R.string.formation_desc_41212
        Formation.F4411 -> R.string.formation_desc_4411
        Formation.F532 -> R.string.formation_desc_532
        Formation.F3412 -> R.string.formation_desc_3412
        Formation.F4222 -> R.string.formation_desc_4222
    }

@get:StringRes
val Difficulty.titleRes: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.NORMAL -> R.string.difficulty_normal
        Difficulty.HARD -> R.string.difficulty_hard
    }

@get:StringRes
val Difficulty.detailRes: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy_detail
        Difficulty.NORMAL -> R.string.difficulty_normal_detail
        Difficulty.HARD -> R.string.difficulty_hard_detail
    }

@get:StringRes
val DraftStyle.titleRes: Int
    get() = when (this) {
        DraftStyle.SQUAD_FIRST -> R.string.style_squad_first
        DraftStyle.POSITION_FIRST -> R.string.style_position_first
    }

@get:StringRes
val DraftStyle.detailRes: Int
    get() = when (this) {
        DraftStyle.SQUAD_FIRST -> R.string.style_squad_first_detail
        DraftStyle.POSITION_FIRST -> R.string.style_position_first_detail
    }

@get:StringRes
val RatingMode.titleRes: Int
    get() = when (this) {
        RatingMode.SEASON -> R.string.rating_mode_season
        RatingMode.PRIME -> R.string.rating_mode_prime
    }

@get:StringRes
val RatingMode.detailRes: Int
    get() = when (this) {
        RatingMode.SEASON -> R.string.rating_mode_season_detail
        RatingMode.PRIME -> R.string.rating_mode_prime_detail
    }

@get:StringRes
val EraPreset.labelRes: Int
    get() = when (this) {
        EraPreset.ALL_TIME -> R.string.era_all_time
        EraPreset.SINCE_2000 -> R.string.era_2000s
        EraPreset.SINCE_2010 -> R.string.era_2010s
        EraPreset.MODERN -> R.string.era_modern
    }

@get:StringRes
val ManagerTrait.titleRes: Int
    get() = when (this) {
        ManagerTrait.ATTACKING -> R.string.trait_attacking
        ManagerTrait.DEFENSIVE -> R.string.trait_defensive
        ManagerTrait.BIG_GAMES -> R.string.trait_big_games
        ManagerTrait.MAN_MANAGER -> R.string.trait_man_manager
        ManagerTrait.TACTICIAN -> R.string.trait_tactician
    }

@get:StringRes
val ManagerTrait.detailRes: Int
    get() = when (this) {
        ManagerTrait.ATTACKING -> R.string.trait_attacking_detail
        ManagerTrait.DEFENSIVE -> R.string.trait_defensive_detail
        ManagerTrait.BIG_GAMES -> R.string.trait_big_games_detail
        ManagerTrait.MAN_MANAGER -> R.string.trait_man_manager_detail
        ManagerTrait.TACTICIAN -> R.string.trait_tactician_detail
    }

@get:StringRes
val JanuaryEvent.titleRes: Int
    get() = when (this) {
        JanuaryEvent.MARQUEE_SIGNING -> R.string.january_marquee
        JanuaryEvent.LOAN_PROSPECT -> R.string.january_loan
        JanuaryEvent.SELL_STAR -> R.string.january_sell
        JanuaryEvent.FITNESS_COACH -> R.string.january_fitness
    }

@get:StringRes
val JanuaryEvent.detailRes: Int
    get() = when (this) {
        JanuaryEvent.MARQUEE_SIGNING -> R.string.january_marquee_detail
        JanuaryEvent.LOAN_PROSPECT -> R.string.january_loan_detail
        JanuaryEvent.SELL_STAR -> R.string.january_sell_detail
        JanuaryEvent.FITNESS_COACH -> R.string.january_fitness_detail
    }

@get:StringRes
val Competition.titleRes: Int
    get() = when (this) {
        Competition.CHAMPIONS_LEAGUE -> R.string.competition_champions_league
        Competition.EUROPA_LEAGUE -> R.string.competition_europa_league
        Competition.CONFERENCE_LEAGUE -> R.string.competition_conference_league
        Competition.FA_CUP -> R.string.mode_fac_title
    }

/** One sentence on how the January gamble played out. */
@Composable
fun januaryStory(outcome: JanuaryOutcome): String {
    val signed = outcome.signed?.player?.name
    val departed = outcome.departed?.name
    return when (outcome.event) {
        JanuaryEvent.MARQUEE_SIGNING -> when {
            signed != null && departed != null && outcome.success -> stringResource(R.string.january_marquee_hit, signed, departed)
            signed != null && !outcome.success -> stringResource(R.string.january_marquee_flop, signed)
            outcome.success -> stringResource(R.string.january_marquee_generic_hit)
            else -> stringResource(R.string.january_marquee_generic_flop)
        }

        JanuaryEvent.LOAN_PROSPECT ->
            stringResource(if (outcome.success) R.string.january_loan_hit else R.string.january_loan_miss)

        JanuaryEvent.SELL_STAR -> when {
            signed != null && departed != null ->
                stringResource(if (outcome.success) R.string.january_sell_hit else R.string.january_sell_miss, departed, signed)
            else -> stringResource(if (outcome.success) R.string.january_sell_fell_through_hit else R.string.january_sell_fell_through_miss)
        }

        JanuaryEvent.FITNESS_COACH ->
            stringResource(if (outcome.success) R.string.january_fitness_hit else R.string.january_fitness_miss)
    }
}

/** Stage name without matchday or leg, e.g. "Quarter-final". */
@Composable
fun stageName(type: StageType, number: Int = 0): String = when (type) {
    StageType.MATCHDAY -> stringResource(R.string.stage_matchday, number)
    StageType.LEAGUE_PHASE -> stringResource(R.string.stage_league_phase, number)
    StageType.GROUP_STAGE -> stringResource(R.string.stage_group, number)
    StageType.KNOCKOUT_PLAYOFF -> stringResource(R.string.stage_playoff)
    StageType.ROUND_OF_32 -> stringResource(R.string.stage_round_of_32)
    StageType.ROUND_OF_16 -> stringResource(R.string.stage_round_of_16)
    StageType.QUARTER_FINAL -> stringResource(R.string.stage_quarter_final)
    StageType.SEMI_FINAL -> stringResource(R.string.stage_semi_final)
    StageType.FINAL -> stringResource(R.string.stage_final)
    StageType.EXTRA_PRELIMINARY_ROUND -> stringResource(R.string.stage_extra_preliminary)
    StageType.PRELIMINARY_ROUND -> stringResource(R.string.stage_preliminary)
    StageType.QUALIFYING_ROUND -> stringResource(R.string.stage_qualifying, number)
    StageType.FIRST_ROUND -> stringResource(R.string.stage_first_round)
    StageType.SECOND_ROUND -> stringResource(R.string.stage_second_round)
    StageType.THIRD_ROUND -> stringResource(R.string.stage_third_round)
    StageType.FOURTH_ROUND -> stringResource(R.string.stage_fourth_round)
    StageType.FIFTH_ROUND -> stringResource(R.string.stage_fifth_round)
}

@Composable
fun stageLabel(stage: Stage): String {
    val name = stageName(stage.type, stage.number)
    return if (stage.leg > 0) stringResource(R.string.stage_with_leg, name, stage.leg) else name
}

@Composable
fun venueLong(venue: Venue): String = stringResource(
    when (venue) {
        Venue.HOME -> R.string.venue_home_long
        Venue.AWAY -> R.string.venue_away_long
        Venue.NEUTRAL -> R.string.venue_neutral_long
    },
)

@Composable
fun outcomeLetter(outcome: Outcome): String = stringResource(
    when (outcome) {
        Outcome.WIN -> R.string.result_win
        Outcome.DRAW -> R.string.result_draw
        Outcome.LOSS -> R.string.result_loss
    },
)

/** Extra-time, penalty and aggregate notes, e.g. "aet · 4-3 pens · agg 3-3". Null when there's nothing to add. */
@Composable
fun scoreNotes(match: MatchResult): String? {
    val notes = buildList {
        if (match.extraTime) add(stringResource(R.string.score_aet))
        match.penalties?.let { (ours, theirs) -> add(stringResource(R.string.score_penalties, ours, theirs)) }
        match.aggregate?.let { (ours, theirs) -> add(stringResource(R.string.score_aggregate, ours, theirs)) }
    }
    return notes.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** English ordinal: 1st, 2nd, 3rd, 11th, 22nd. */
fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}
