package xyz.haselbach.huushalt.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.MainActivity
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode
import xyz.haselbach.huushalt.testing.resetSettings
import xyz.haselbach.huushalt.testing.screenshot

/** Holiday mode (F10.1, F10.4) and "Neu planen ab heute" (F10.6) on the screen. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class HolidayFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = resetSettings()

    private val repo get() = (compose.activity.application as HuushaltApplication).repository
    private val weekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)

    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = 5_000)

    @Test
    fun holidayFromTodayPausesHomeAndCanBeEnded() {
        runBlocking { repo.save(null, "Staubsaugen", "", weekly, LocalDate.now()) }
        waitFor("Staubsaugen")

        compose.onNodeWithContentDescription("Einstellungen").performClick()
        compose.onNodeWithText("Ferienmodus").performClick()
        waitFor("Bis und mit")
        compose.onNodeWithText("Speichern").performClick()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("Ferien beenden")
        compose.onNodeWithText("Staubsaugen").assertDoesNotExist()
        compose.screenshot("9-holiday")

        compose.onNodeWithText("Ferien beenden").performClick()
        waitFor("Staubsaugen")
        compose.onNodeWithText("Ferien beenden").assertDoesNotExist()
    }

    @Test
    fun rescheduleMovesOverdueChores() {
        runBlocking { repo.save(null, "Fenster putzen", "", weekly, LocalDate.now().minusDays(5)) }
        waitFor("Überfällig")
        compose.onNodeWithText("Neu planen").performClick()
        waitFor("Ab heute neu planen?")
        compose.screenshot("10-reschedule")
        // [0] is the button in the section header, [1] the confirm button in the dialog.
        compose.onAllNodes(hasText("Neu planen"))[1].performClick()
        compose.waitUntilDoesNotExist(hasText("Überfällig"), timeoutMillis = 5_000)
        waitFor("Demnächst")
    }
}
