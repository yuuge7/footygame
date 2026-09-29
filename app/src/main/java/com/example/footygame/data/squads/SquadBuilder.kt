package com.example.footygame.data.squads

import com.example.footygame.models.ClubSeason
import com.example.footygame.models.Player
import com.example.footygame.models.Pool
import com.example.footygame.models.Position
import java.text.Normalizer

private val surnameParticles = setOf("de", "van", "der", "di", "da", "dos", "del", "mac", "le")
private val combiningMarks = "\\p{M}".toRegex()
private val nonAlphanumeric = "[^a-z0-9]+".toRegex()
private val plainName = "[A-Za-z0-9 ]+".toRegex()

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

/**
 * [key] overrides the id only for namesakes: two different people who share a name (e.g. both players
 * called Fred) need distinct keys, otherwise the game treats them as one person.
 */
internal fun gk(name: String, rating: Int, short: String? = null, key: String? = null) = player(name, Position.GK, rating, short, key)
internal fun def(name: String, rating: Int, short: String? = null, key: String? = null) = player(name, Position.DEF, rating, short, key)
internal fun mid(name: String, rating: Int, short: String? = null, key: String? = null) = player(name, Position.MID, rating, short, key)
internal fun att(name: String, rating: Int, short: String? = null, key: String? = null) = player(name, Position.ATT, rating, short, key)

/** The id comes from the full name, so the same spelling in two squads is the same person. */
private fun player(name: String, position: Position, rating: Int, short: String?, key: String?) =
    Player(key ?: slug(name), name, short ?: shortNameOf(name), position, rating)

internal fun shortNameOf(name: String): String {
    val parts = name.split(' ')
    if (parts.size == 1) return name
    val particle = (1 until parts.size).firstOrNull { parts[it].lowercase() in surnameParticles }
    return if (particle != null) parts.drop(particle).joinToString(" ") else parts.last()
}

/** Thousands of players are built on first use, so plain ASCII names skip Unicode normalisation. */
internal fun slug(name: String): String {
    if (plainName.matches(name)) return name.lowercase().replace(nonAlphanumeric, "-").trim('-')
    return Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase()
        .replace("ł", "l")
        .replace("ı", "i")
        .replace("ð", "d")
        .replace("ø", "o")
        .replace("æ", "ae")
        .replace("ß", "ss")
        .replace("þ", "th")
        .replace(nonAlphanumeric, "-")
        .trim('-')
}
