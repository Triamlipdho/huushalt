package xyz.haselbach.huushalt.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import xyz.haselbach.huushalt.data.Backup
import xyz.haselbach.huushalt.domain.ChoreColor
import xyz.haselbach.huushalt.domain.ChoreSymbol
import xyz.haselbach.huushalt.testing.resetSettings
import xyz.haselbach.huushalt.testing.screenshot

/** Symbols and colours for chores (F1.5). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SymbolFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = resetSettings()

    private val app get() = compose.activity.application as HuushaltApplication
    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMillis = 5_000)

    @Test
    fun symbolIsGuessedAndCanBeChosen() {
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput("Katzenklo leeren")
        waitFor("Passend zum Namen gewählt")
        compose.onNodeWithText("Ändern").performClick()
        waitFor("Symbol wählen")
        compose.onNodeWithContentDescription("Rosa").performClick()
        compose.screenshot("17-symbol-picker")
        compose.onNodeWithText("Übernehmen").performClick()
        waitFor("Selbst gewählt")
        compose.onNodeWithText("Speichern").performClick()

        waitFor("Katzenklo leeren")
        compose.waitUntil(5_000) { runBlocking { app.repository.chores.first().isNotEmpty() } }
        val look = runBlocking { app.repository.chores.first().single().look }
        assertEquals(ChoreSymbol.PETS, look.symbol)
        assertEquals(ChoreColor.PINK, look.color)
        compose.screenshot("18-heute-symbols")

        // Symbol and colour survive export and import.
        val json = runBlocking { app.backups.toJson(app.backups.read()) }
        val restored: Backup = app.backups.fromJson(json)
        assertEquals("PETS", restored.chores.single().symbol)
        assertEquals("PINK", restored.chores.single().color)
    }
}
