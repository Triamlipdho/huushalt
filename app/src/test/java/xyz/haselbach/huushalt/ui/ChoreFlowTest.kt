package xyz.haselbach.huushalt.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.haselbach.huushalt.MainActivity

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
        waitFor("«Staubsaugen» erledigt")
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
        waitFor("Zuletzt erledigt")
        compose.waitUntilDoesNotExist(hasText("Noch nicht erledigt."), timeoutMillis = 5_000)
        screenshot("5-detail")
    }

    private fun waitFor(text: String) {
        compose.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = 5_000)
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val dir = File("build/screenshots").apply { mkdirs() }
        // Draw the window directly; captureToImage() waits for a frame Robolectric doesn't deliver.
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bitmap)) }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
