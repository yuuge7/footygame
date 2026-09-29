package com.example.footygame.models

private val BIG_FIVE_EUROPE = listOf(
    Competition.CHAMPIONS_LEAGUE, Competition.CHAMPIONS_LEAGUE, Competition.CHAMPIONS_LEAGUE, Competition.CHAMPIONS_LEAGUE,
    Competition.EUROPA_LEAGUE, Competition.CONFERENCE_LEAGUE, Competition.CONFERENCE_LEAGUE,
)
private val TWO_CHAMPIONS_LEAGUE_PLACES = listOf(
    Competition.CHAMPIONS_LEAGUE, Competition.CHAMPIONS_LEAGUE, Competition.EUROPA_LEAGUE, Competition.CONFERENCE_LEAGUE,
)
private val ONE_CHAMPIONS_LEAGUE_PLACE = listOf(
    Competition.CHAMPIONS_LEAGUE, Competition.EUROPA_LEAGUE, Competition.CONFERENCE_LEAGUE,
)

/**
 * A top flight a career season can be played in. [size] counts every club, so a season is (size - 1) * 2
 * matches; [europe] is what each finishing place from first down earns; [title] and [cup] name the silverware.
 */
enum class League(val size: Int, val cup: Competition, val title: Trophy, val europe: List<Competition>) {
    PREMIER_LEAGUE(20, Competition.FA_CUP, Trophy.LEAGUE, BIG_FIVE_EUROPE),
    LA_LIGA(20, Competition.COPA_DEL_REY, Trophy.LA_LIGA, BIG_FIVE_EUROPE),
    SERIE_A(20, Competition.COPPA_ITALIA, Trophy.SERIE_A, BIG_FIVE_EUROPE),
    BUNDESLIGA(18, Competition.DFB_POKAL, Trophy.BUNDESLIGA, BIG_FIVE_EUROPE),
    LIGUE_1(
        18,
        Competition.COUPE_DE_FRANCE,
        Trophy.LIGUE_1,
        listOf(
            Competition.CHAMPIONS_LEAGUE, Competition.CHAMPIONS_LEAGUE, Competition.CHAMPIONS_LEAGUE,
            Competition.EUROPA_LEAGUE, Competition.CONFERENCE_LEAGUE,
        ),
    ),
    EREDIVISIE(18, Competition.KNVB_CUP, Trophy.EREDIVISIE, TWO_CHAMPIONS_LEAGUE_PLACES),
    PRIMEIRA_LIGA(18, Competition.TACA_DE_PORTUGAL, Trophy.PRIMEIRA_LIGA, TWO_CHAMPIONS_LEAGUE_PLACES),
    SCOTTISH_PREMIERSHIP(12, Competition.SCOTTISH_CUP, Trophy.SCOTTISH_PREMIERSHIP, ONE_CHAMPIONS_LEAGUE_PLACE),
    SUPER_LIG(18, Competition.TURKISH_CUP, Trophy.SUPER_LIG, ONE_CHAMPIONS_LEAGUE_PLACE),
    RUSSIAN_PREMIER_LEAGUE(16, Competition.RUSSIAN_CUP, Trophy.RUSSIAN_PREMIER_LEAGUE, TWO_CHAMPIONS_LEAGUE_PLACES),
    UKRAINIAN_PREMIER_LEAGUE(16, Competition.UKRAINIAN_CUP, Trophy.UKRAINIAN_PREMIER_LEAGUE, ONE_CHAMPIONS_LEAGUE_PLACE),
    GREEK_SUPER_LEAGUE(14, Competition.GREEK_CUP, Trophy.GREEK_SUPER_LEAGUE, ONE_CHAMPIONS_LEAGUE_PLACE);

    val matches: Int get() = (size - 1) * 2
}

/** True for a league title, as opposed to a cup. */
val Trophy.isLeagueTitle: Boolean get() = League.entries.any { it.title == this }
