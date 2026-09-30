package com.example.footygame.data

import android.content.Context
import androidx.core.content.edit
import com.example.footygame.models.CareerModeStats
import com.example.footygame.models.DynastyState
import com.example.footygame.models.Legacy
import com.example.footygame.models.ProState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** The career in progress for each career mode, the finished ones for the hall of fame, and the record book of both. */
interface CareerStore {
    fun loadDynasty(): DynastyState?
    fun saveDynasty(state: DynastyState?)
    fun loadPro(): ProState?
    fun savePro(state: ProState?)
    fun loadLegacies(): List<Legacy>
    fun saveLegacies(legacies: List<Legacy>)
    fun loadStats(): CareerModeStats
    fun saveStats(stats: CareerModeStats)
}

class SharedPreferencesCareerStore(context: Context) : CareerStore {
    private val prefs = context.getSharedPreferences("careers", Context.MODE_PRIVATE)

    // Later versions may add or drop fields, so unknown keys are skipped and missing ones take their defaults.
    private val json = Json { ignoreUnknownKeys = true }

    override fun loadDynasty(): DynastyState? = read(DYNASTY, DynastyState.serializer())

    override fun saveDynasty(state: DynastyState?) = write(DYNASTY, DynastyState.serializer(), state)

    override fun loadPro(): ProState? = read(PRO, ProState.serializer())

    override fun savePro(state: ProState?) = write(PRO, ProState.serializer(), state)

    override fun loadLegacies(): List<Legacy> = read(LEGACIES, ListSerializer(Legacy.serializer())).orEmpty()

    override fun saveLegacies(legacies: List<Legacy>) = write(LEGACIES, ListSerializer(Legacy.serializer()), legacies)

    override fun loadStats(): CareerModeStats = read(STATS, CareerModeStats.serializer()) ?: CareerModeStats()

    override fun saveStats(stats: CareerModeStats) = write(STATS, CareerModeStats.serializer(), stats)

    /** A save that can't be read any more (e.g. an enum renamed between versions) starts over rather than crash. */
    private fun <T> read(key: String, serializer: KSerializer<T>): T? {
        val text = prefs.getString(key, null) ?: return null
        return try {
            json.decodeFromString(serializer, text)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun <T> write(key: String, serializer: KSerializer<T>, value: T?) {
        prefs.edit {
            if (value == null) remove(key) else putString(key, json.encodeToString(serializer, value))
        }
    }

    private companion object {
        const val DYNASTY = "dynasty"
        const val PRO = "pro"
        const val LEGACIES = "legacies"
        const val STATS = "stats"
    }
}
