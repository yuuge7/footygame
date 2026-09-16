package com.example.footygame.models

enum class Difficulty(val respins: Int, val hidesRatings: Boolean) {
    EASY(respins = 3, hidesRatings = false),
    NORMAL(respins = 1, hidesRatings = false),
    HARD(respins = 0, hidesRatings = true),
}

enum class DraftStyle {
    /** Spin a squad, pick anyone, choose where they play. */
    SQUAD_FIRST,

    /** Choose a slot, then spin for a squad that can fill it. */
    POSITION_FIRST,
}

enum class RatingMode {
    /** As rated in that exact season. */
    SEASON,

    /** Every player at their best rating anywhere in the game. */
    PRIME,
}

/** Inclusive range of season start years (or tournament years) a spin can land on. */
data class EraRange(val from: Int, val to: Int) {
    operator fun contains(year: Int): Boolean = year in from..to
}

/** Quick era choices: everything from [fromYear] to the latest season. */
enum class EraPreset(val fromYear: Int) {
    ALL_TIME(0),
    SINCE_2000(2000),
    SINCE_2010(2010),
    MODERN(2016),
}

data class DraftSettings(
    val formation: Formation = Formation.F433,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val showRatings: Boolean = true,
    val style: DraftStyle = DraftStyle.SQUAD_FIRST,
    val ratingMode: RatingMode = RatingMode.SEASON,
    /** Null means every season in the pool. */
    val era: EraRange? = null,
    val managers: Boolean = true,
    val europeanNights: Boolean = true,
    val januaryWindow: Boolean = true,
) {
    /** Hard difficulty hides ratings whatever the toggle says. */
    val ratingsShown: Boolean get() = showRatings && !difficulty.hidesRatings
}

enum class ManagerTrait {
    /** Extra attack. */
    ATTACKING,

    /** Extra defence. */
    DEFENSIVE,

    /** A lift against top sides and in knockout ties. */
    BIG_GAMES,

    /** Chemistry counts double. */
    MAN_MANAGER,

    /** A small lift everywhere. */
    TACTICIAN,
}

data class Manager(val id: String, val name: String, val trait: ManagerTrait)
