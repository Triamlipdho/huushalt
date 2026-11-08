package xyz.haselbach.huushalt.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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

/** Starter set (F8.2) and display settings (F9.4). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SetupFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = resetSettings()

    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = 5_000)

    @Test
    fun starterSetFromTheEmptyState() {
        waitFor("Vorschläge ansehen")
        compose.onNodeWithText("Vorschläge ansehen").performClick()
        waitFor("Aufgaben auswählen")
        compose.onNodeWithText("Staubsaugen").performClick()
        // The list is lazy: scroll it until the entry exists.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Bad putzen"))
        compose.onNodeWithText("Bad putzen").performClick()
        compose.screenshot("14-catalog")
        compose.onNodeWithText("2 Aufgaben hinzufügen").performClick()

        waitFor("0 von 1 erledigt")
        compose.onNodeWithText("Staubsaugen").assertExists()
        compose.onNodeWithText("Alle Aufgaben").performClick()
        waitFor("Bad putzen")
    }

    @Test
    fun themeAndFirstDayOfWeek() {
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        compose.onNodeWithText("Farbschema").performScrollTo().performClick()
        compose.onNodeWithText("Dunkel").performClick()
        waitFor("Dunkel")
        compose.onNodeWithText("Wochenbeginn").performScrollTo().performClick()
        compose.onNodeWithText("Sonntag").performClick()
        waitFor("Sonntag")
        compose.screenshot("15-settings-dark")
    }
}
