package com.example.footygame.data

import android.content.Context
import androidx.core.content.edit
import com.example.footygame.models.CareerStats
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface StatsStore {
    fun load(): CareerStats
    fun save(stats: CareerStats)
}

class SharedPreferencesStatsStore(context: Context) : StatsStore {
    private val prefs = context.getSharedPreferences("career_stats", Context.MODE_PRIVATE)

    // Later versions may add or drop fields, so unknown keys are skipped and missing ones take their defaults.
    private val json = Json { ignoreUnknownKeys = true }

    /** Unreadable stats (e.g. an enum renamed between versions) start over rather than crash the app. */
    override fun load(): CareerStats {
        val text = prefs.getString(KEY, null) ?: return CareerStats()
        return try {
            json.decodeFromString<CareerStats>(text)
        } catch (_: SerializationException) {
            CareerStats()
        }
    }

    override fun save(stats: CareerStats) {
        prefs.edit { putString(KEY, json.encodeToString(stats)) }
    }

    private companion object {
        const val KEY = "stats"
    }
}
