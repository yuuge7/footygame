package com.example.footygame.data

import com.example.footygame.data.squads.EnglishClubs1992
import com.example.footygame.data.squads.EnglishClubs1999
import com.example.footygame.data.squads.EnglishClubs2006
import com.example.footygame.data.squads.EnglishClubs2012
import com.example.footygame.data.squads.EnglishClubs2018
import com.example.footygame.data.squads.EnglishSeasonsEarly
import com.example.footygame.data.squads.EnglishSeasonsLate
import com.example.footygame.data.squads.EuropeanCentral
import com.example.footygame.data.squads.EuropeanEast
import com.example.footygame.data.squads.EuropeanIberia
import com.example.footygame.data.squads.EuropeanItalyFrance
import com.example.footygame.data.squads.EuropeanNorth
import com.example.footygame.data.squads.EuropeanSeasons
import com.example.footygame.data.squads.NationalTeams
import com.example.footygame.data.squads.WorldCup1982
import com.example.footygame.data.squads.WorldCup1986
import com.example.footygame.data.squads.WorldCup1990
import com.example.footygame.data.squads.WorldCup1994
import com.example.footygame.data.squads.WorldCup1998
import com.example.footygame.data.squads.WorldCup2002
import com.example.footygame.data.squads.WorldCup2006
import com.example.footygame.data.squads.WorldCup2010
import com.example.footygame.data.squads.WorldCup2014
import com.example.footygame.data.squads.WorldCup2018
import com.example.footygame.data.squads.WorldCup2022
import com.example.footygame.models.ClubSeason
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftPick
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.EraPreset
import com.example.footygame.models.EraRange
import com.example.footygame.models.RatingMode

/** Every squad in the game, and the views of it a draft needs. */
object ClubSeasons {
    /** Smallest era that still guarantees a draft can fill every formation. */
    const val MIN_SQUADS = 6

    val all: List<ClubSeason> = listOf(
        EnglishSeasonsEarly.all, EnglishSeasonsLate.all,
        EnglishClubs1992.all, EnglishClubs1999.all, EnglishClubs2006.all, EnglishClubs2012.all, EnglishClubs2018.all,
        EuropeanSeasons.all, EuropeanIberia.all, EuropeanItalyFrance.all, EuropeanCentral.all, EuropeanEast.all,
        EuropeanNorth.all,
        NationalTeams.all,
        WorldCup1982.all, WorldCup1986.all, WorldCup1990.all, WorldCup1994.all, WorldCup1998.all, WorldCup2002.all,
        WorldCup2006.all, WorldCup2010.all, WorldCup2014.all, WorldCup2018.all, WorldCup2022.all,
    ).flatten()

    /** Every squad with each player at the best rating they have anywhere in the game. */
    private val primeSquads: Map<String, ClubSeason> by lazy {
        val prime = all.flatMap { it.players }
            .groupBy { it.id }
            .mapValues { (_, appearances) -> appearances.maxOf { it.rating } }
        all.associate { squad ->
            squad.id to squad.copy(players = squad.players.map { it.copy(rating = prime.getValue(it.id)) })
        }
    }

    private val byId: Map<String, ClubSeason> by lazy { all.associateBy { it.id } }

    /** A squad by id, for careers that store ids and rebuild squads on load. */
    fun squad(id: String): ClubSeason? = byId[id]

    /** A stored pick rebuilt: the player from that squad at the rating they had. Null once either is gone. */
    fun pick(squadId: String, playerId: String, rating: Int): DraftPick? {
        val squad = squad(squadId) ?: return null
        val player = squad.players.firstOrNull { it.id == playerId } ?: return null
        return DraftPick(player.copy(rating = rating), squad)
    }

    fun poolFor(mode: DraftMode): List<ClubSeason> = all.filter { it.pool in mode.pools }

    /** The squads a draft can spin: the mode's pool, narrowed to the era, at season or prime ratings. */
    fun poolFor(mode: DraftMode, settings: DraftSettings): List<ClubSeason> {
        val era = settings.era
        val squads = poolFor(mode).filter { era == null || it.startYear in era }
        return if (settings.ratingMode == RatingMode.PRIME) squads.map { primeSquads.getValue(it.id) } else squads
    }

    /** Distinct season start years (or tournament years) available in a mode, oldest first. */
    fun years(mode: DraftMode): List<Int> = poolFor(mode).map { it.startYear }.distinct().sorted()

    fun squadCount(mode: DraftMode, era: EraRange?): Int = poolFor(mode).count { era == null || it.startYear in era }

    /** Null for all-time; otherwise from the first season at or after the preset's year to the latest. */
    fun eraFor(mode: DraftMode, preset: EraPreset): EraRange? {
        if (preset == EraPreset.ALL_TIME) return null
        val years = years(mode)
        val from = years.firstOrNull { it >= preset.fromYear } ?: return null
        return EraRange(from, years.last())
    }

    /** A range that covers every season is stored as all-time. */
    fun normalise(mode: DraftMode, era: EraRange?): EraRange? {
        if (era == null) return null
        val years = years(mode)
        return if (era.from <= years.first() && era.to >= years.last()) null else era
    }
}
