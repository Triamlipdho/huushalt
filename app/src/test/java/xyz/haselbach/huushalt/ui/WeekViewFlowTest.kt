package xyz.haselbach.huushalt.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
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

    private fun waitFor(text: String, substring: Boolean = false) =
        compose.waitUntilAtLeastOneExists(hasText(text, substring = substring), timeoutMillis = 5_000)

    /** Inside the list, not the bottom bar ("Heute" is also a tab). */
    private val inList: SemanticsMatcher = hasAnyAncestor(hasScrollAction())

    private fun isHeading() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    /** A day of the week strip, by what TalkBack reads, e.g. "Mo., 16.11.: 1 Aufgabe". */
    private fun day(date: LocalDate) = hasContentDescription(date.formatShort(), substring = true)

    private fun kw(weekStart: LocalDate) = "KW ${WeekForecast.calendarWeek(weekStart)}"

    @Test
    fun weekViewShowsTheWeekAsAStripAndOneDayAtATime() {
        waitFor("Kehricht rausbringen")
        compose.onNodeWithText("Alle Aufgaben").performClick()
        compose.onNodeWithText("Woche").performClick()
        val thisWeek = WeekForecast.weekStart(today, DayOfWeek.MONDAY)
        waitFor(kw(thisWeek))
        compose.onNode(hasText("Diese Woche", substring = true) and inList).assertExists()
        // Today is selected and marked; its chores are listed below the strip.
        compose.onNode(day(today)).assertIsSelected()
        compose.onNode(hasContentDescription("Heute, ", substring = true)).assertExists()
        compose.onNode(hasText("Heute · ${today.formatLong()}") and isHeading()).assertExists()

        // Next week: Monday and Thursday have one chore each, the other days nothing.
        compose.onNodeWithContentDescription("Nächste Woche").performClick()
        waitFor(kw(nextWeek))
        compose.onNode(hasText("Nächste Woche", substring = true) and inList).assertExists()
        compose.onNodeWithText("2 Aufgaben in dieser Woche").assertExists()
        compose.onNode(day(nextWeek)).assertContentDescriptionContains("1 Aufgabe", substring = true)
        compose.onNode(day(nextWeek.plusDays(1))).assertContentDescriptionContains("Nichts fällig", substring = true)
        compose.onNode(day(nextWeek.plusDays(3))).assertContentDescriptionContains("1 Aufgabe", substring = true)

        // Tapping a day lists its chores.
        compose.onNode(day(nextWeek.plusDays(1))).performClick()
        compose.onNode(hasText(nextWeek.plusDays(1).formatLong()) and isHeading()).assertExists()
        compose.onNodeWithText("Nichts fällig").assertExists()
        compose.onNode(day(nextWeek.plusDays(3))).performClick()
        compose.onNode(day(nextWeek.plusDays(3))).assertIsSelected()
        compose.onNode(hasText("Kehricht rausbringen") and inList).assertExists()
        // A forecast, unless today is Sunday and Monday is the real next due date.
        assertTrue(compose.onAllNodesWithText("Voraussichtlich", substring = true).fetchSemanticsNodes().isNotEmpty())
        compose.screenshot("20-week-next")

        // The weekday stays selected when paging; "Heute" comes back to today.
        compose.onNodeWithContentDescription("Nächste Woche").performClick()
        waitFor(kw(nextWeek.plusWeeks(1)))
        compose.onNode(day(nextWeek.plusWeeks(1).plusDays(3))).assertIsSelected()
        compose.onNode(hasText("Heute") and inList).performClick()
        waitFor(kw(thisWeek))
        compose.onNode(day(today)).assertIsSelected()
        compose.onNodeWithContentDescription("Nächste Woche").performClick()
        waitFor(kw(nextWeek))
        compose.onNodeWithContentDescription("Vorherige Woche").performClick()
        waitFor(kw(thisWeek))

        compose.onNodeWithText("Liste").performClick()
        waitFor("Nach Fälligkeit")
    }
}
