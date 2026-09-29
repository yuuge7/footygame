package com.example.footygame

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.footygame.models.DraftMode
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val slotThatFits = hasContentDescription("someone in this squad fits", substring = true)
    private val slotToSpinFor = hasContentDescription("tap to spin for this slot", substring = true)

    @Test
    fun menuListsEveryChallenge() {
        DraftMode.entries.forEach { mode ->
            rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("mode_${mode.name}"))
            rule.onNodeWithTag("mode_${mode.name}").assertIsDisplayed()
        }
    }

    @Test
    fun draftAWholeXiAndPlayTheWorldCup() {
        openSetup(DraftMode.WC)
        tapSetting("formation_3-5-2")
        tapSetting("difficulty_NORMAL")
        tapSetting("style_SQUAD_FIRST")
        startDraft()

        repeat(11) { draftSquadFirst() }
        appointGafferIfAsked()

        rule.onNodeWithTag("play").performClick()
        waitFor(hasTestTag("skip"))
        rule.onNodeWithTag("skip").performClick()
        waitFor(hasTestTag("verdict"))

        rule.onNodeWithTag("run_it_back").performClick()
        waitFor(hasTestTag("skip"))
        rule.onNodeWithTag("skip").performClick()
        waitFor(hasTestTag("new_draft"))

        // A new draft must stay on the draft screen, not bounce to the menu while results animate out.
        rule.onNodeWithTag("new_draft").performClick()
        waitFor(hasTestTag("pick_player") and isEnabled())
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.onNodeWithTag("pick_player").assertIsDisplayed()
        assertFalse("New draft bounced back to the menu", exists(hasTestTag("mode_WC")))
    }

    @Test
    fun aFinishedRunLandsInTheStats() {
        openSetup(DraftMode.WC)
        tapSetting("style_SQUAD_FIRST")
        startDraft()
        repeat(11) { draftSquadFirst() }
        appointGafferIfAsked()

        rule.onNodeWithTag("play").performClick()
        waitFor(hasTestTag("skip"))
        rule.onNodeWithTag("skip").performClick()
        waitFor(hasTestTag("menu"))
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("highlights"))
        rule.onNodeWithTag("highlights").assertIsDisplayed()
        rule.onNodeWithTag("menu").performClick()

        waitFor(hasTestTag("mode_WC"))
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("open_stats"))
        rule.onNodeWithTag("open_stats").performClick()
        waitFor(hasTestTag("stats_filter_WC"))
        rule.onNodeWithTag("stats_filter_WC").performClick()
        rule.onNodeWithTag("stats_overview").assertIsDisplayed()
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("stats_recent"))
        rule.onNodeWithTag("stats_recent").assertIsDisplayed()
    }

    @Test
    fun positionFirstPremierLeagueWithTheJanuaryWindow() {
        openSetup(DraftMode.EPL)
        tapSetting("formation_4-4-2")
        tapSetting("difficulty_EASY")
        tapSetting("style_POSITION_FIRST")
        startDraft()

        repeat(11) { draftPositionFirst() }
        appointGafferIfAsked()

        rule.onNodeWithTag("play").performClick()
        waitFor(hasTestTag("skip"))
        rule.onNodeWithTag("skip").performClick()
        rule.waitUntil(TIMEOUT) { exists(hasTestTag("january_option")) || exists(hasTestTag("verdict")) }
        if (exists(hasTestTag("january_option"))) {
            rule.onAllNodesWithTag("january_option").onFirst().performClick()
            rule.waitUntil(TIMEOUT) { exists(hasTestTag("skip")) || exists(hasTestTag("verdict")) }
            if (exists(hasTestTag("skip"))) rule.onNodeWithTag("skip").performClick()
        }
        waitFor(hasTestTag("verdict"))
        rule.onNodeWithTag("run_it_back").assertIsDisplayed()
    }

    @Test
    fun leavingADraftWithPicksAsksFirstAndReturnsToSetup() {
        openSetup(DraftMode.EPL)
        tapSetting("style_SQUAD_FIRST")
        startDraft()
        draftSquadFirst()

        Espresso.pressBack()
        rule.onNodeWithText(rule.activity.getString(R.string.draft_leave_title)).assertIsDisplayed()

        rule.onNodeWithText(rule.activity.getString(R.string.draft_leave_cancel)).performClick()
        rule.onNodeWithTag("pick_player").assertIsDisplayed()

        Espresso.pressBack()
        rule.onNodeWithText(rule.activity.getString(R.string.draft_leave_confirm)).performClick()
        waitFor(hasTestTag("start_draft"))
    }

    private fun openSetup(mode: DraftMode) {
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("mode_${mode.name}"))
        rule.onNodeWithTag("mode_${mode.name}").performClick()
        waitFor(hasTestTag("start_draft"))
    }

    private fun tapSetting(tag: String) {
        rule.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun startDraft() {
        rule.onNodeWithTag("start_draft").performClick()
        rule.waitUntil(TIMEOUT) { exists(hasTestTag("pick_player") and isEnabled()) || exists(slotToSpinFor) }
    }

    /** Taps a glowing slot, then the first player who can fill it. */
    private fun draftSquadFirst() {
        waitFor(slotThatFits)
        rule.onAllNodes(slotThatFits).onFirst().performClick()
        pickFirstEligible()
    }

    /** Chooses a slot, waits for the spin, opens the squad and picks the first player who fits. */
    private fun draftPositionFirst() {
        waitFor(slotToSpinFor)
        rule.onAllNodes(slotToSpinFor).onFirst().performClick()
        waitFor(hasTestTag("pick_player") and isEnabled())
        rule.onNodeWithTag("pick_player").performClick()
        pickFirstEligible()
    }

    private fun pickFirstEligible() {
        waitFor(hasTestTag("eligible_player"))
        rule.onAllNodesWithTag("eligible_player").onFirst().performClick()
        rule.waitUntil(TIMEOUT) { rule.onAllNodesWithTag("eligible_player").fetchSemanticsNodes().isEmpty() }
        rule.waitUntil(TIMEOUT) {
            exists(slotThatFits) || exists(slotToSpinFor) || exists(hasTestTag("play")) || exists(hasTestTag("manager_option"))
        }
    }

    private fun appointGafferIfAsked() {
        rule.waitUntil(TIMEOUT) { exists(hasTestTag("play")) || exists(hasTestTag("manager_option")) }
        if (exists(hasTestTag("manager_option"))) {
            rule.onAllNodesWithTag("manager_option").onFirst().performClick()
        }
        waitFor(hasTestTag("play"))
    }

    private fun waitFor(matcher: SemanticsMatcher) = rule.waitUntil(TIMEOUT) { exists(matcher) }

    private fun exists(matcher: SemanticsMatcher) = rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val TIMEOUT = 15_000L
    }
}
