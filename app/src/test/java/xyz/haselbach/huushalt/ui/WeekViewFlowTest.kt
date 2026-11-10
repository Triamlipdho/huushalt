package xyz.haselbach.huushalt.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
import xyz.haselbach.huushalt.domain.WeekForecast
import xyz.haselbach.huushalt.testing.resetSettings
import xyz.haselbach.huushalt.testing.screenshot

/** The week view in "Alle Aufgaben" (F3.6). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class WeekViewFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val today = LocalDate.now()
    private val nextWeek = WeekForecast.weekStart(today, DayOfWeek.MONDAY).plusWeeks(1)

    @Before
    fun setUp() {
        resetSettings()
        runBlocking {
            val repo = (compose.activity.application as HuushaltApplication).repository
            repo.save(
                null, "Kehricht rausbringen", "",
                Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 1, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)), today,
            )
        }
    }

    private fun waitFor(text: String) =
        compose.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = 5_000)

    /** Inside the list, not the bottom bar ("Heute" is also a tab). */
    private val inList: SemanticsMatcher = hasAnyAncestor(hasScrollAction())

    private fun SemanticsNodeInteraction.top() = fetchSemanticsNode().boundsInRoot.top

    /** Tops of the rows with [name] that lie in the section of [day]. */
    private fun rowsOn(day: LocalDate, name: String): Int {
        val header = compose.onNode(hasText(day.formatShort(), substring = true) and isHeading()).top()
        val next = compose.onNode(hasText(day.plusDays(1).formatShort(), substring = true) and isHeading()).top()
        return compose.onAllNodesWithText(name).fetchSemanticsNodes().count { it.boundsInRoot.top in header..next }
    }

    private fun isHeading() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun weekViewShowsEveryOccurrenceAndPagesWeeks() {
        waitFor("Kehricht rausbringen")
        compose.onNodeWithText("Alle Aufgaben").performClick()
        compose.onNodeWithText("Woche").performClick()
        waitFor("Diese Woche")
        // The real due date is in this week; today is highlighted.
        compose.onNode(hasText("Heute · ${today.formatShort()}") and isHeading()).assertExists()

        compose.onNodeWithContentDescription("Nächste Woche").performClick()
        waitFor(nextWeek.formatShort())
        compose.onNode(hasText("Nächste Woche") and inList).assertExists()
        compose.onNodeWithText("2 Aufgaben in dieser Woche").assertExists()
        assertEquals(1, rowsOn(nextWeek, "Kehricht rausbringen"))
        assertEquals(0, rowsOn(nextWeek.plusDays(1), "Kehricht rausbringen"))
        assertEquals(1, rowsOn(nextWeek.plusDays(3), "Kehricht rausbringen"))
        // Both are a forecast, unless today is Sunday and Monday is the real next due date.
        assertTrue(compose.onAllNodesWithText("Voraussichtlich", substring = true).fetchSemanticsNodes().size in 1..2)
        assertTrue(compose.onAllNodesWithText("Nichts fällig").fetchSemanticsNodes().size == 5)
        compose.screenshot("20-week-next")

        compose.onNodeWithContentDescription("Nächste Woche").performClick()
        waitFor("In 2 Wochen")
        compose.onNode(hasText("Heute") and inList).performClick()
        waitFor("Diese Woche")
        compose.onNodeWithContentDescription("Nächste Woche").performClick()
        waitFor(nextWeek.formatShort())
        compose.onNodeWithContentDescription("Vorherige Woche").performClick()
        waitFor("Diese Woche")
        compose.onNode(hasText("Heute · ${today.formatShort()}") and isHeading()).assertExists()

        compose.onNodeWithText("Liste").performClick()
        waitFor("Nach Fälligkeit")
    }
}
