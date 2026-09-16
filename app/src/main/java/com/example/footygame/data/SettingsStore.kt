package com.example.footygame.data

import android.content.Context
import androidx.core.content.edit
import com.example.footygame.models.DraftMode
import com.example.footygame.models.DraftSettings
import com.example.footygame.models.EraRange

/** Remembers the last setup used for each challenge. */
interface SettingsStore {
    fun load(mode: DraftMode): DraftSettings
    fun save(mode: DraftMode, settings: DraftSettings)
}

class SharedPreferencesSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("draft_settings", Context.MODE_PRIVATE)

    override fun load(mode: DraftMode): DraftSettings {
        val defaults = DraftSettings()
        val from = prefs.getInt(key(mode, ERA_FROM), NO_ERA)
        val to = prefs.getInt(key(mode, ERA_TO), NO_ERA)
        // Data can change between versions; an era that no longer holds enough squads falls back to all-time.
        val era = EraRange(from, to).takeIf {
            from != NO_ERA && to != NO_ERA && ClubSeasons.squadCount(mode, it) >= ClubSeasons.MIN_SQUADS
        }
        return DraftSettings(
            formation = enumOr(prefs.getString(key(mode, FORMATION), null), defaults.formation),
            difficulty = enumOr(prefs.getString(key(mode, DIFFICULTY), null), defaults.difficulty),
            showRatings = prefs.getBoolean(key(mode, SHOW_RATINGS), defaults.showRatings),
            style = enumOr(prefs.getString(key(mode, STYLE), null), defaults.style),
            ratingMode = enumOr(prefs.getString(key(mode, RATING_MODE), null), defaults.ratingMode),
            era = ClubSeasons.normalise(mode, era),
            managers = prefs.getBoolean(key(mode, MANAGERS), defaults.managers),
            europeanNights = prefs.getBoolean(key(mode, EUROPEAN_NIGHTS), defaults.europeanNights),
            januaryWindow = prefs.getBoolean(key(mode, JANUARY_WINDOW), defaults.januaryWindow),
        )
    }

    override fun save(mode: DraftMode, settings: DraftSettings) {
        prefs.edit {
            putString(key(mode, FORMATION), settings.formation.name)
            putString(key(mode, DIFFICULTY), settings.difficulty.name)
            putBoolean(key(mode, SHOW_RATINGS), settings.showRatings)
            putString(key(mode, STYLE), settings.style.name)
            putString(key(mode, RATING_MODE), settings.ratingMode.name)
            putInt(key(mode, ERA_FROM), settings.era?.from ?: NO_ERA)
            putInt(key(mode, ERA_TO), settings.era?.to ?: NO_ERA)
            putBoolean(key(mode, MANAGERS), settings.managers)
            putBoolean(key(mode, EUROPEAN_NIGHTS), settings.europeanNights)
            putBoolean(key(mode, JANUARY_WINDOW), settings.januaryWindow)
        }
    }

    private fun key(mode: DraftMode, field: String) = "${mode.name}_$field"

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    private companion object {
        const val NO_ERA = -1
        const val FORMATION = "formation"
        const val DIFFICULTY = "difficulty"
        const val SHOW_RATINGS = "show_ratings"
        const val STYLE = "style"
        const val RATING_MODE = "rating_mode"
        const val ERA_FROM = "era_from"
        const val ERA_TO = "era_to"
        const val MANAGERS = "managers"
        const val EUROPEAN_NIGHTS = "european_nights"
        const val JANUARY_WINDOW = "january_window"
    }
}
