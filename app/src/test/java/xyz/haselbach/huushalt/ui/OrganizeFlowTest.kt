package xyz.haselbach.huushalt.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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

/** Rooms, checklists and pausing on the screen (F1.3, F1.4, F1.7, F1.8, F3.5). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class OrganizeFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = resetSettings()

    private fun waitFor(text: String, substring: Boolean = false) =
        compose.waitUntilAtLeastOneExists(hasText(text, substring = substring), timeoutMillis = 5_000)

    private fun createChore(name: String, room: String? = null, checklist: List<String> = emptyList()) {
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput(name)
        if (room != null) {
            compose.onNodeWithText("Kein Raum").performClick()
            compose.onNodeWithText("Neuer Raum …").performClick()
            compose.onNodeWithText("Name des Raums").performTextInput(room)
        }
        checklist.forEach { item ->
            compose.onNodeWithText("Punkt hinzufügen").performScrollTo().performClick()
            compose.onAllNodesWithText("z. B. Lavabo").onFirst().performTextInput(item)
        }
        compose.onNodeWithText("Speichern").performClick()
    }

    @Test
    fun checklistAsksBeforeCompletingAndResets() {
        waitFor("Noch keine Aufgaben")
        createChore("Bad putzen", room = "Bad", checklist = listOf("Lavabo", "WC"))
        waitFor("Bad · Heute fällig · 0/2")

        compose.onNodeWithText("Bad putzen").performClick()
        waitFor("Checkliste 0/2")
        compose.onNodeWithText("Lavabo").performClick()
        waitFor("Checkliste 1/2")
        compose.screenshot("11-checklist")

        compose.onNodeWithText("Erledigt").performClick()
        waitFor("Checkliste nicht fertig")
        compose.onNodeWithText("Trotzdem erledigt").performClick()
        waitFor("Checkliste 0/2")
        compose.onNodeWithText("Noch nicht erledigt.").assertDoesNotExist()
    }

    @Test
    fun roomFilterAndPausing() {
        waitFor("Noch keine Aufgaben")
        createChore("Kühlschrank putzen", room = "Küche")
        waitFor("Küche · Heute fällig")
        createChore("Staubsaugen")
        waitFor("Staubsaugen")

        // Filter Heute by room (F3.5).
        compose.onAllNodesWithText("Küche").onFirst().performClick()
        compose.waitUntilDoesNotExist(hasText("Staubsaugen"), timeoutMillis = 5_000)
        compose.onNodeWithText("Kühlschrank putzen").assertExists()
        compose.screenshot("12-room-filter")
        compose.onNodeWithText("Alle").performClick()
        waitFor("Staubsaugen")

        // Pause (F1.3): gone from Heute, listed as paused in Alle Aufgaben.
        compose.onNodeWithText("Staubsaugen").performClick()
        compose.onNodeWithText("Pausieren").performClick()
        waitFor("Fortsetzen")
        compose.onNodeWithContentDescription("Zurück").performClick()
        waitFor("Kühlschrank putzen")
        compose.onNodeWithText("Staubsaugen").assertDoesNotExist()
        compose.onNodeWithText("Alle Aufgaben").performClick()
        waitFor("Pausiert")
        compose.onNodeWithText("Staubsaugen").assertExists()
    }

    @Test
    fun roomsCanBeAddedRenamedAndDeleted() {
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        compose.onNodeWithText("Aufgaben nach Räumen ordnen und filtern").performClick()
        waitFor("Noch keine Räume", substring = true)
        compose.onNodeWithText("Neuer Raum").performTextInput("Kuche")
        compose.onNodeWithContentDescription("Raum hinzufügen").performClick()
        waitFor("Kuche")
        compose.onNodeWithContentDescription("«Kuche» umbenennen").performClick()
        compose.onNodeWithText("Kuche").performTextReplacement("Küche")
        compose.onNodeWithContentDescription("Speichern").performClick()
        waitFor("Küche")
        compose.onNodeWithContentDescription("«Küche» löschen").performClick()
        compose.onNodeWithText("Löschen").performClick()
        waitFor("Noch keine Räume", substring = true)
    }

    @Test
    fun dateListFromPastedText() {
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput("Papier")
        compose.onNodeWithText("Nach festem Plan").performClick()
        compose.onNodeWithText("Daten").performClick()
        val year = java.time.LocalDate.now().year + 1
        compose.onNodeWithText("Mehrere Daten einfügen").performScrollTo().performTextInput("Di 12.01.$year, Di 26.01.$year")
        compose.onNodeWithText("2 Daten übernehmen").performScrollTo().performClick()
        waitFor("12.01.$year", substring = true)
        compose.screenshot("13-date-list")
        compose.onNodeWithText("Speichern").performClick()

        // The dates are next year, so the chore isn't on Heute yet.
        waitFor("Alles erledigt für heute")
        compose.onNodeWithText("Alle Aufgaben").performClick()
        waitFor("Papier")
        compose.onNodeWithText("Papier").performClick()
        waitFor("An bestimmten Daten (noch 2)")
    }
}
