package com.example.footygame.data.squads

import com.example.footygame.models.ClubSeason
import com.example.footygame.models.Player
import com.example.footygame.models.Pool
import com.example.footygame.models.Position
import java.text.Normalizer

private val surnameParticles = setOf("de", "van", "der", "di", "da", "dos", "del", "mac", "le")
private val combiningMarks = "\\p{M}".toRegex()
private val nonAlphanumeric = "[^a-z0-9]+".toRegex()

/** A club's or nation's identity: badge code and kit colours. */
internal class Kit(val name: String, val code: String, val primary: Long, val secondary: Long)

/** Squad ids are the badge code plus start year, e.g. "ars-2003". */
internal fun squad(kit: Kit, startYear: Int, pool: Pool, vararg players: Player) = ClubSeason(
    id = "${kit.code.lowercase()}-$startYear",
    club = kit.name,
    startYear = startYear,
    code = kit.code,
    primaryColor = kit.primary,
    secondaryColor = kit.secondary,
    pool = pool,
    players = players.toList(),
)

internal fun gk(name: String, rating: Int, short: String? = null) = player(name, Position.GK, rating, short)
internal fun def(name: String, rating: Int, short: String? = null) = player(name, Position.DEF, rating, short)
internal fun mid(name: String, rating: Int, short: String? = null) = player(name, Position.MID, rating, short)
internal fun att(name: String, rating: Int, short: String? = null) = player(name, Position.ATT, rating, short)

/** The id comes from the full name, so the same spelling in two squads is the same person. */
private fun player(name: String, position: Position, rating: Int, short: String?) =
    Player(slug(name), name, short ?: shortNameOf(name), position, rating)

internal fun shortNameOf(name: String): String {
    val parts = name.split(' ')
    if (parts.size == 1) return name
    val particle = (1 until parts.size).firstOrNull { parts[it].lowercase() in surnameParticles }
    return if (particle != null) parts.drop(particle).joinToString(" ") else parts.last()
}

internal fun slug(name: String): String =
    Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase()
        .replace("ł", "l")
        .replace("ı", "i")
        .replace("ð", "d")
        .replace("ø", "o")
        .replace(nonAlphanumeric, "-")
        .trim('-')
