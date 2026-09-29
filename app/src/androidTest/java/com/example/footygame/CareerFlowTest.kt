package com.example.footygame

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CareerFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val slotThatFits = hasContentDescription("someone in this squad fits", substring = true)

    @Test
    fun aDynastyDraftsPlaysASeasonAndReachesTheBoard() {
        openFresh("career_dynasty", "start_dynasty")
        rule.onNodeWithTag("manager_name").performTextInput("Test Gaffer")
        rule.onNodeWithTag("start_dynasty").performClick()

        repeat(11) {
            waitFor(slotThatFits)
            rule.onAllNodes(slotThatFits).onFirst().performClick()
            waitFor(hasTestTag("eligible_player"))
            rule.onAllNodesWithTag("eligible_player").onFirst().performClick()
            rule.waitUntil(TIMEOUT) { rule.onAllNodesWithTag("eligible_player").fetchSemanticsNodes().isEmpty() }
        }
        waitFor(hasTestTag("play"))
        rule.onNodeWithTag("play").performClick()

        waitFor(hasTestTag("kick_off"))
        rule.onNodeWithTag("kick_off").performClick()
        playSeasonToReview()
        assertTrue(exists(hasTestTag("close_review")))
    }

    @Test
    fun aPlayerCareerSignsPlaysASeasonAndGetsOffers() {
        openFresh("career_pro", "start_pro")
        rule.onNodeWithTag("player_name").performTextInput("Test Striker")
        rule.onNodeWithTag("start_pro").performClick()

        waitFor(hasTestTag("offer"))
        rule.onAllNodesWithTag("offer").onFirst().performClick()
        waitFor(hasTestTag("kick_off"))
        rule.onNodeWithTag("kick_off").performClick()
        playSeasonToReview()

        rule.onNodeWithTag("close_review").performClick()
        rule.waitUntil(TIMEOUT) { exists(hasTestTag("stay")) || exists(hasTestTag("new_pro")) }
        if (exists(hasTestTag("stay"))) {
            rule.onNodeWithTag("stay").performClick()
            waitFor(hasTestTag("kick_off"))
        }
    }

    /** Opens a career mode from the menu, walking away from any career left over from earlier runs. */
    private fun openFresh(cardTag: String, startTag: String) {
        openCard(cardTag)
        val finished = hasTestTag("new_dynasty") or hasTestTag("new_pro")
        rule.waitUntil(TIMEOUT) {
            exists(hasTestTag(startTag)) || exists(hasTestTag("quit_career")) || exists(hasTestTag("pick_player")) || exists(finished)
        }
        if (exists(finished)) {
            // A finished career: its "new" button leads straight to the setup.
            rule.onNode(hasScrollToIndexAction()).performScrollToNode(finished)
            rule.onNode(finished).performClick()
        } else if (exists(hasTestTag("pick_player"))) {
            // An unfinished first draft: leaving it ends the dynasty.
            Espresso.pressBack()
            rule.onNodeWithText(rule.activity.getString(R.string.draft_leave_confirm)).performClick()
            openCard(cardTag)
        } else if (exists(hasTestTag("quit_career"))) {
            rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("quit_career"))
            rule.onNodeWithTag("quit_career").performClick()
            rule.onNodeWithText(rule.activity.getString(R.string.dynasty_quit_confirm)).performClick()
            openCard(cardTag)
        }
        waitFor(hasTestTag(startTag))
    }

    /** Menu cards sit in a lazy list, so they only exist once scrolled to. */
    private fun openCard(tag: String) {
        rule.waitForIdle()
        waitFor(hasTestTag("mode_EPL"))
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
        rule.onNodeWithTag(tag).performClick()
    }

    /** League, January, FA Cup and Europe as they come, until the hub shows the review. */
    private fun playSeasonToReview() {
        var guard = 0
        while (!exists(hasTestTag("close_review")) && guard++ < 40) {
            rule.waitUntil(TIMEOUT) {
                exists(hasTestTag("skip")) || exists(hasTestTag("january_option")) ||
                    exists(hasTestTag("season_continue") and isEnabled()) || exists(hasTestTag("close_review"))
            }
            when {
                exists(hasTestTag("january_option")) -> rule.onAllNodesWithTag("january_option").onFirst().performClick()
                exists(hasTestTag("season_continue")) -> rule.onNodeWithTag("season_continue").performClick()
                exists(hasTestTag("skip")) -> rule.onNodeWithTag("skip").performClick()
            }
            rule.waitForIdle()
        }
        waitFor(hasTestTag("close_review"))
    }

    private fun waitFor(matcher: SemanticsMatcher) = rule.waitUntil(TIMEOUT) { exists(matcher) }

    private fun exists(matcher: SemanticsMatcher) = rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val TIMEOUT = 20_000L
    }
}
