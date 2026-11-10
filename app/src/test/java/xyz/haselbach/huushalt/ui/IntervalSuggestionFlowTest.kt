package xyz.haselbach.huushalt.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.LocalDate
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
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode
import xyz.haselbach.huushalt.settings.AppSettings
import xyz.haselbach.huushalt.testing.screenshot

/** Interval suggestions from real behaviour (F2.13): shown on Heute and the detail screen. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class IntervalSuggestionFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app get() = compose.activity.application as HuushaltApplication
    private var choreId = 0L

    private val hint = "Vorschlag für «Kühlschrank putzen»: alle 3 Wochen"
    private val sentence = "Du machst das etwa alle 3 Wochen statt alle 4 Wochen. Anpassen?"

    /** Planned every 4 weeks, but done every 3 weeks, four times. */
    @Before
    fun seed() = runBlocking {
        app.settings.update { AppSettings(permissionCardDismissed = true) }
        val repo = app.repository
        val today = LocalDate.now()
        choreId = repo.save(null, "Kühlschrank putzen", "", Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 4), today.minusDays(84))
        listOf(84L, 63L, 42L, 21L).forEach { repo.complete(choreId, today.minusDays(it)) }
    }

    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMillis = 5_000)
    private fun waitUntilGone(text: String) = compose.waitUntilDoesNotExist(hasText(text, substring = true), timeoutMillis = 5_000)

    private fun openFromHint() {
        waitFor(hint)
        compose.screenshot("suggestion-heute")
        compose.onNodeWithText(hint).performClick()
        waitFor(sentence)
        compose.screenshot("suggestion-detail")
    }

    @Test
    fun acceptingChangesTheInterval() {
        openFromHint()
        compose.onNodeWithText("4 Wochen nach dem Erledigen").assertExists()
        compose.onNodeWithText("Übernehmen").performClick()
        waitFor("«Kühlschrank putzen» jetzt alle 3 Wochen")
        waitFor("3 Wochen nach dem Erledigen")
        waitUntilGone(sentence)
        // Counted again from the last time, 3 weeks ago: due today.
        waitFor("Heute fällig")
        compose.screenshot("suggestion-accepted")
        val chore = runBlocking { app.repository.chores.first().single { it.id == choreId } }
        assertEquals(Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 3), chore.schedule)
        assertEquals(LocalDate.now(), chore.dueDate)

        // Undo puts the old interval back.
        compose.onNodeWithText("Rückgängig").performClick()
        waitFor("4 Wochen nach dem Erledigen")
    }

    @Test
    fun dismissingIsRemembered() {
        openFromHint()
        compose.onNodeWithText("Nein danke").performClick()
        waitUntilGone(sentence)
        compose.onNodeWithText("4 Wochen nach dem Erledigen").assertExists()
        val settings = runBlocking { app.settings.current() }
        assertEquals(setOf("$choreId:3:WEEKS"), settings.dismissedIntervalSuggestions)

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("Kühlschrank putzen")
        waitUntilGone("Vorschlag für")
    }
}
