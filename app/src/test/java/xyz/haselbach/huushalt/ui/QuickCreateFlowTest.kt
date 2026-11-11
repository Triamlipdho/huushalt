package xyz.haselbach.huushalt.ui

import xyz.haselbach.huushalt.testing.openDetails
import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.MainActivity
import xyz.haselbach.huushalt.domain.ChoreCatalog
import xyz.haselbach.huushalt.testing.resetSettings
import xyz.haselbach.huushalt.testing.screenshot

/** Quick create from suggestions, duplicating, searching and app shortcuts. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class QuickCreateFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = resetSettings()

    private val app get() = compose.activity.application as HuushaltApplication
    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMillis = 5_000)

    @Test
    fun suggestionFillsTheEditor() {
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        waitFor("Vorschläge – antippen")
        compose.onNodeWithText("Name").performTextInput("bad")
        compose.screenshot("16-quick-create")
        compose.onNodeWithText("Bad putzen").performClick()
        // Name, room and checklist are filled in; the suggestions disappear.
        compose.onNodeWithText("Vorschläge – antippen zum Übernehmen").assertDoesNotExist()
        compose.onNodeWithText("Lavabo").assertExists()
        compose.onNodeWithText("Speichern").performClick()

        waitFor("Bad\u00A0·\u00A00/5")
        compose.onNodeWithText("Bad putzen").assertExists()
    }

    @Test
    fun duplicateCopiesTheSettings() {
        runBlocking { app.repository.addFromTemplates(ChoreCatalog.templates.filter { it.name == "Bad putzen" }) }
        waitFor("Bad putzen")
        compose.openDetails("Bad putzen")
        compose.onNodeWithContentDescription("Weitere Aktionen").performClick()
        compose.onNodeWithText("Duplizieren").performClick()
        waitFor("Bad putzen (Kopie)")
        compose.onNodeWithText("Speichern").performClick()
        waitFor("Bad putzen (Kopie)")
        compose.waitUntil(5_000) { runBlocking { app.repository.chores.first().size == 2 } }
        val copy = runBlocking { app.repository.chores.first().single { it.name == "Bad putzen (Kopie)" } }
        assertEquals(5, copy.checklistTotal)
        assertEquals("Bad", copy.areaName)
    }

    @Test
    fun searchFiltersAllChores() {
        runBlocking { app.repository.addFromTemplates(ChoreCatalog.templates.take(10)) }
        waitFor("Staubsaugen")
        compose.onNodeWithText("Alle Aufgaben").performClick()
        waitFor("Aufgaben durchsuchen")
        compose.onNodeWithText("Aufgaben durchsuchen").performTextInput("küche")
        compose.waitUntilDoesNotExist(hasText("Staubsaugen"), timeoutMillis = 5_000)
        compose.onNodeWithText("Kühlschrank putzen").assertExists()
    }

    @Test
    fun appShortcutOpensNewChore() {
        waitFor("Noch keine Aufgaben")
        compose.activityRule.scenario.onActivity { it.handle(Intent(MainActivity.ACTION_NEW_CHORE)) }
        waitFor("Neue Aufgabe")
        compose.onAllNodesWithText("Speichern").fetchSemanticsNodes().let { assertEquals(1, it.size) }
    }
}
