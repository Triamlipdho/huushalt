package xyz.haselbach.huushalt.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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

/**
 * End-to-end flow on the JVM: create a chore, mark it done, undo (F1.1, F3.1, F4.1, F4.2, F7.1).
 * Screenshots of each step are written to app/build/screenshots/.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ChoreFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = resetSettings()

    @Test
    fun createCompleteAndUndo() {
        waitFor("Noch keine Aufgaben")
        screenshot("1-empty")

        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput("Staubsaugen")
        screenshot("2-edit")
        compose.onNodeWithText("Speichern").performClick()

        waitFor("0 von 1 erledigt")
        compose.onNodeWithText("Staubsaugen").assertExists()
        compose.onNodeWithText("Heute fällig").assertExists()
        screenshot("3-today")

        compose.onNodeWithContentDescription("«Staubsaugen» als erledigt markieren").performClick()
        waitFor("1 von 1 erledigt")
        // The last open chore of the day is celebrated (F7.5).
        waitFor("Alles erledigt für heute 🎉")
        screenshot("4-done")

        compose.onNodeWithText("Rückgängig").performClick()
        waitFor("0 von 1 erledigt")
        compose.onNodeWithText("Heute fällig").assertExists()
    }

    @Test
    fun detailShowsScheduleAndHistory() {
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput("Bad putzen")
        compose.onNodeWithText("Nach festem Plan").performClick()
        compose.onNodeWithText("Speichern").performClick()

        waitFor("Bad putzen")
        compose.onNodeWithText("Bad putzen").performClick()
        waitFor("Noch nicht erledigt.")
        compose.onNode(hasText("Jede Woche am ", substring = true)).assertExists()

        compose.onNodeWithText("Erledigt").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Zuletzt erledigt am", substring = true), timeoutMillis = 5_000)
        compose.waitUntilDoesNotExist(hasText("Noch nicht erledigt."), timeoutMillis = 5_000)
        screenshot("5-detail")

        // Delete the history entry again (F6.4).
        compose.onNodeWithText("Verlauf").assertExists()
        compose.onAllNodesWithText(today()).onLast().performClick()
        compose.onNodeWithText("Eintrag löschen").performClick()
        waitFor("Noch nicht erledigt.")
    }

    @Test
    fun progressTabShowsStatsAndActivity() {
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput("Staubsaugen")
        compose.onNodeWithText("Speichern").performClick()
        waitFor("0 von 1 erledigt")
        compose.onNodeWithContentDescription("«Staubsaugen» als erledigt markieren").performClick()
        waitFor("1 von 1 erledigt")

        compose.onNodeWithText("Fortschritt").performClick()
        waitFor("Alles erledigt in Folge")
        waitFor("🔥 1 Tag")
        compose.onNodeWithText("100 %").assertExists()
        // Below the badges (F7.7).
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(hasText("Zuletzt erledigt"))
        compose.onNodeWithText("Zuletzt erledigt").assertExists()
        compose.onNodeWithText("Alle Aufgaben").assertExists() // only the tab label, not the title
        compose.onAllNodesWithText("Fortschritt").assertCountEquals(2) // title and tab label
        screenshot("8-progress")

        // Without motivation, the streak is hidden (F7.9).
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        // The celebration snackbar may still cover the bottom of the screen: use the click action
        // instead of a tap at a screen position.
        compose.onNodeWithText("Serien und Feiern").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("Zuletzt erledigt")
        compose.waitUntilDoesNotExist(hasText("Alles erledigt in Folge"), timeoutMillis = 5_000)
    }

    private fun today() = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EE, dd.MM.yyyy", java.util.Locale.forLanguageTag("de-CH")))

    private fun waitFor(text: String) {
        compose.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = 5_000)
    }

    private fun screenshot(name: String) = compose.screenshot(name)

    @Test
    fun editorOpensDatePickerAndDeletesAfterConfirmation() {
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput("Fenster putzen")
        compose.onNodeWithText(today()).performScrollTo().performClick()
        waitFor("OK")
        compose.onNodeWithText("Abbrechen").performClick()
        compose.onNodeWithText("Speichern").performClick()

        waitFor("Fenster putzen")
        compose.onNodeWithText("Fenster putzen").performClick()
        compose.onNodeWithContentDescription("Bearbeiten").performClick()
        // The editor appears once the checklist is loaded from the database.
        compose.waitUntilAtLeastOneExists(hasText("Aufgabe löschen"), timeoutMillis = 5_000)
        compose.onNodeWithText("Aufgabe löschen").performScrollTo().performClick()
        waitFor("«Fenster putzen» löschen?")
        compose.onNodeWithText("Löschen").performClick()
        waitFor("Noch keine Aufgaben")
    }
}
