package xyz.haselbach.huushalt.ui

import android.Manifest
import android.app.NotificationManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.haselbach.huushalt.MainActivity
import xyz.haselbach.huushalt.testing.resetSettings
import xyz.haselbach.huushalt.testing.screenshot

/** Notification permission card (F8.3) and the reminder settings (F5.2). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = resetSettings()

    /** Robolectric allows notifications by default; simulate a phone where they are not allowed yet. */
    private fun blockNotifications() {
        val app = compose.activity.application
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(app.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        compose.activityRule.scenario.recreate()
    }

    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = 5_000)

    @Test
    fun permissionCardAppearsAfterTheFirstChoreAndCanBeDismissed() {
        blockNotifications()
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithText("Erinnerungen erlauben?").assertDoesNotExist()

        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput("Staubsaugen")
        compose.onNodeWithText("Speichern").performClick()

        waitFor("Erinnerungen erlauben?")
        compose.screenshot("6-permission-card")
        compose.onNodeWithText("Nicht jetzt").performClick()
        compose.waitUntilDoesNotExist(hasText("Erinnerungen erlauben?"), timeoutMillis = 5_000)
    }

    @Test
    fun settingsShowDefaultTimesAndWarnWhenNotificationsAreBlocked() {
        blockNotifications()
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        waitFor("Um 08:00, wenn etwas fällig ist")
        compose.onNodeWithText("Um 19:00, nur wenn noch etwas offen ist").assertExists()
        compose.onNodeWithText("Mitteilungen sind für huushalt ausgeschaltet. Ohne sie gibt es keine Erinnerungen.").assertExists()
        compose.screenshot("7-settings")

        // Turning reminders off hides the warning, since no notifications are needed then.
        compose.onNodeWithText("Tägliche Übersicht und Abend-Erinnerung").performClick()
        compose.waitUntilDoesNotExist(hasText("Mitteilungen sind für huushalt", substring = true), timeoutMillis = 5_000)
    }
}
