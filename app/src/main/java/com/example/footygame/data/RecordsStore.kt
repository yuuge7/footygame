package com.example.footygame.data

import android.content.Context
import androidx.core.content.edit
import com.example.footygame.models.DraftMode
import com.example.footygame.models.RunResult

data class ModeRecord(
    val runs: Int = 0,
    val perfectRuns: Int = 0,
    val trophies: Int = 0,
    val bestWins: Int = 0,
    val bestDraws: Int = 0,
    val bestLosses: Int = 0,
    val bestGoalDifference: Int = 0,
) {
    /** Best means most wins, then fewest losses, then best goal difference. */
    fun isImprovedBy(run: RunResult): Boolean {
        if (runs == 0) return true
        if (run.wins != bestWins) return run.wins > bestWins
        if (run.losses != bestLosses) return run.losses < bestLosses
        return run.goalDifference > bestGoalDifference
    }

    operator fun plus(run: RunResult): ModeRecord {
        val improved = isImprovedBy(run)
        return ModeRecord(
            runs = runs + 1,
            perfectRuns = perfectRuns + if (run.isFlawless) 1 else 0,
            trophies = trophies + run.trophies,
            bestWins = if (improved) run.wins else bestWins,
            bestDraws = if (improved) run.draws else bestDraws,
            bestLosses = if (improved) run.losses else bestLosses,
            bestGoalDifference = if (improved) run.goalDifference else bestGoalDifference,
        )
    }
}

interface RecordsStore {
    fun load(): Map<DraftMode, ModeRecord>
    fun save(mode: DraftMode, record: ModeRecord)
}

class SharedPreferencesRecordsStore(context: Context) : RecordsStore {
    private val prefs = context.getSharedPreferences("records", Context.MODE_PRIVATE)

    override fun load(): Map<DraftMode, ModeRecord> = DraftMode.entries.associateWith { mode ->
        ModeRecord(
            runs = prefs.getInt(key(mode, RUNS), 0),
            perfectRuns = prefs.getInt(key(mode, PERFECT), 0),
            trophies = prefs.getInt(key(mode, TROPHIES), 0),
            bestWins = prefs.getInt(key(mode, WINS), 0),
            bestDraws = prefs.getInt(key(mode, DRAWS), 0),
            bestLosses = prefs.getInt(key(mode, LOSSES), 0),
            bestGoalDifference = prefs.getInt(key(mode, GOAL_DIFFERENCE), 0),
        )
    }

    override fun save(mode: DraftMode, record: ModeRecord) {
        prefs.edit {
            putInt(key(mode, RUNS), record.runs)
            putInt(key(mode, PERFECT), record.perfectRuns)
            putInt(key(mode, TROPHIES), record.trophies)
            putInt(key(mode, WINS), record.bestWins)
            putInt(key(mode, DRAWS), record.bestDraws)
            putInt(key(mode, LOSSES), record.bestLosses)
            putInt(key(mode, GOAL_DIFFERENCE), record.bestGoalDifference)
        }
    }

    private fun key(mode: DraftMode, field: String) = "${mode.name}_$field"

    private companion object {
        const val RUNS = "runs"
        const val PERFECT = "perfect"
        const val TROPHIES = "trophies"
        const val WINS = "best_wins"
        const val DRAWS = "best_draws"
        const val LOSSES = "best_losses"
        const val GOAL_DIFFERENCE = "best_goal_difference"
    }
}
