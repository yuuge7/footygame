package com.example.footygame.game

import com.example.footygame.models.MatchResult
import com.example.footygame.models.Opponent
import com.example.footygame.models.Stage
import com.example.footygame.models.StageType
import com.example.footygame.models.Venue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunHighlightsTest {

    private fun match(goalsFor: Int, goalsAgainst: Int, opponent: String = "Rivals", penalties: Pair<Int, Int>? = null) =
        MatchResult(Stage(StageType.MATCHDAY), Opponent(opponent, 80), Venue.HOME, goalsFor, goalsAgainst, penalties = penalties)

    @Test
    fun picksTheBiggestMarginsAndTheFirstOfEquals() {
        val highlights = RunHighlights.of(
            listOf(match(3, 0, "First"), match(4, 1, "Second"), match(0, 2, "Beat us"), match(1, 3, "Beat us again"), match(4, 1)),
        )
        assertEquals("Second", highlights.biggestWin!!.opponent.name)
        assertEquals("Beat us again", highlights.heaviestDefeat!!.opponent.name)
    }

    @Test
    fun streaksCountConsecutiveMatches() {
        val highlights = RunHighlights.of(
            listOf(match(1, 0), match(2, 0), match(1, 1), match(3, 0), match(2, 1), match(1, 0), match(0, 1), match(0, 0)),
        )
        assertEquals(3, highlights.longestWinStreak)
        assertEquals(6, highlights.longestUnbeatenRun)
        assertEquals(2, highlights.failedToScore)
    }

    @Test
    fun aShootoutEndsAWinningRunButNotAnUnbeatenOne() {
        val highlights = RunHighlights.of(listOf(match(1, 0), match(1, 1, penalties = 5 to 4), match(2, 0)))
        assertEquals(1, highlights.longestWinStreak)
        assertEquals(3, highlights.longestUnbeatenRun)
    }

    @Test
    fun noMatchesMeansNoHighlights() {
        val highlights = RunHighlights.of(emptyList())
        assertNull(highlights.biggestWin)
        assertNull(highlights.heaviestDefeat)
        assertEquals(0, highlights.longestWinStreak)
    }
}
