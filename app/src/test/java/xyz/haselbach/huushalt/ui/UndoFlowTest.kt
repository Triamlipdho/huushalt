package xyz.haselbach.huushalt.ui

import xyz.haselbach.huushalt.testing.openDetails
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.haselbach.huushalt.MainActivity
import xyz.haselbach.huushalt.testing.resetSettings
import xyz.haselbach.huushalt.testing.screenshot

/** Nothing is lost by accident: changes can be undone, unsaved edits are not dropped silently. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class UndoFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = resetSettings()

    private fun waitFor(text: String, substring: Boolean = false) {
        compose.waitUntilAtLeastOneExists(hasText(text, substring = substring), timeoutMillis = 5_000)
    }

    private fun createChore(name: String) {
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput(name)
        compose.onNodeWithText("Speichern").performClick()
        waitFor(name)
    }

    @Test
    fun leavingTheEditorWithChangesAsksFirst() {
        waitFor("Noch keine Aufgaben")
        // Without changes, closing just closes.
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithContentDescription("Abbrechen").performClick()
        waitFor("Noch keine Aufgaben")

        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput("Staubsaugen")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("Änderungen verwerfen?")
        screenshot("discard")
        compose.onNodeWithText("Weiter bearbeiten").performClick()
        compose.onNodeWithText("Name").assertTextContains("Staubsaugen")

        compose.onNodeWithContentDescription("Abbrechen").performClick()
        compose.onNodeWithText("Verwerfen").performClick()
        waitFor("Noch keine Aufgaben")
    }

    @Test
    fun skipCanBeUndone() {
        waitFor("Noch keine Aufgaben")
        createChore("Staubsaugen")
        compose.onNodeWithText("Staubsaugen").performClick()
        waitFor("Überspringen")
        compose.onNodeWithText("Überspringen").performClick()
        waitFor("«Staubsaugen» übersprungen")
        compose.onNodeWithText("Rückgängig").performClick()
        waitFor("Heute fällig")
    }

    @Test
    fun todaysCompletionsStayVisibleAndCanBeReopened() {
        waitFor("Noch keine Aufgaben")
        createChore("Staubsaugen")
        compose.onNodeWithContentDescription("«Staubsaugen» als erledigt markieren").performClick()
        waitFor("Heute erledigt (1)")
        // The celebration snackbar may cover the header: use the click action, not a tap.
        compose.onNodeWithText("Heute erledigt (1)").performSemanticsAction(SemanticsActions.OnClick)
        val reopen = hasContentDescription("«Staubsaugen» wieder als offen markieren")
        compose.onNode(hasScrollAction()).performScrollToNode(reopen)
        screenshot("done")
        compose.onNode(reopen).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("«Staubsaugen» wieder offen")
        waitFor("0 von 1 erledigt")
        compose.waitUntilDoesNotExist(hasText("Heute erledigt", substring = true), timeoutMillis = 5_000)
    }

    @Test
    fun deletingFromTheMenuCanBeUndone() {
        waitFor("Noch keine Aufgaben")
        createChore("Staubsaugen")
        compose.openDetails("Staubsaugen")
        compose.onNodeWithContentDescription("Weitere Aktionen").performClick()
        compose.onNodeWithText("Löschen").performClick()
        waitFor("«Staubsaugen» löschen?")
        compose.onNodeWithText("Löschen").performClick()
        waitFor("«Staubsaugen» gelöscht")
        compose.onNodeWithText("Rückgängig").performClick()
        waitFor("Heute fällig")
        compose.onNodeWithText("Staubsaugen").assertExists()
    }

    @Test
    fun swipingCompletesOrPostpones() {
        waitFor("Noch keine Aufgaben")
        createChore("Staubsaugen")
        // Swipe left: tomorrow, with undo.
        compose.onNodeWithText("Staubsaugen").performTouchInput { swipeLeft() }
        waitFor("«Staubsaugen» verschoben auf", substring = true)
        compose.onNodeWithText("Rückgängig").performClick()
        waitFor("Heute fällig")
        // Swipe right: done.
        compose.onNodeWithText("Staubsaugen").performTouchInput { swipeRight() }
        waitFor("1 von 1 erledigt")
    }

    @Test
    fun swipingLeftPostponesInAllChoresToo() {
        waitFor("Noch keine Aufgaben")
        createChore("Staubsaugen")
        compose.onNodeWithText("Alle Aufgaben").performClick()
        compose.onNodeWithText("Staubsaugen").performTouchInput { swipeLeft() }
        waitFor("«Staubsaugen» verschoben auf", substring = true)
    }

    private fun screenshot(name: String) = compose.screenshot("undo-$name")
}
