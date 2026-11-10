package xyz.haselbach.huushalt.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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
import xyz.haselbach.huushalt.domain.Badges
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode
import xyz.haselbach.huushalt.testing.resetSettings
import xyz.haselbach.huushalt.testing.screenshot

/** Badges (F7.7) and completions per room (F7.8) on "Fortschritt". */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BadgesFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app get() = compose.activity.application as HuushaltApplication

    @Before
    fun setUp() = resetSettings()

    private fun waitFor(text: String, timeout: Long = 5_000) =
        compose.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMillis = timeout)

    /** Scrolls the progress list to [text] and waits for it. */
    private fun scrollTo(text: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text, substring = true))
        waitFor(text)
    }

    /** Badge cards are read out as a whole, so they're found by their description. */
    private fun scrollToBadge(description: String) {
        val matcher = hasContentDescription(description, substring = true)
        compose.onNode(hasScrollAction()).performScrollToNode(matcher)
        compose.onNode(matcher).assertExists()
    }

    /**
     * Twelve completions of a kitchen chore, three of a chore without a room. The app may already
     * have seen the empty database, so all badges count as announced (see AnnounceNewBadgesTest
     * for the first run).
     */
    private fun seed() = runBlocking {
        val all = Badges.of(emptyList(), 0).map { it.id }.toSet()
        app.settings.update { it.copy(announcedBadges = all) }
        val repo = app.repository
        val today = LocalDate.now()
        val weekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)
        val kitchen = repo.addArea("Küche")
        repo.addArea("Bad")
        val fridge = repo.save(null, "Kühlschrank putzen", "", weekly, today.minusDays(100), kitchen)
        (12 downTo 1).forEach { repo.complete(fridge, today.minusDays(it * 2L)) }
        val vacuum = repo.save(null, "Staubsaugen", "", weekly, today.minusDays(20))
        listOf(9L, 6L, 3L).forEach { repo.complete(vacuum, today.minusDays(it)) }
    }

    @Test
    fun firstCompletionAnnouncesTheFirstBadge() {
        waitFor("Noch keine Aufgaben")
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        compose.onNodeWithText("Name").performTextInput("Staubsaugen")
        compose.onNodeWithText("Speichern").performClick()
        waitFor("0 von 1 erledigt")
        compose.onNodeWithContentDescription("«Staubsaugen» als erledigt markieren").performClick()
        // First the celebration with "Rückgängig", then the badge.
        waitFor("Alles erledigt für heute 🎉")
        waitFor("Neues Abzeichen: Der erste Schritt 🏅", timeout = 15_000)
        screenshot("badge-snackbar")

        compose.onNodeWithText("Fortschritt").performClick()
        scrollTo("Abzeichen")
        scrollToBadge("Der erste Schritt, erreicht")
        // The next ones, greyed with their progress.
        scrollToBadge("Am Ball, noch nicht erreicht: 10 Aufgaben erledigt, 1 von 10")
        // Without rooms, no room statistics.
        compose.onNodeWithText("Nach Raum").assertDoesNotExist()
    }

    @Test
    fun progressShowsBadgesAndRooms() {
        seed()
        waitFor("Kühlschrank putzen")
        compose.onNodeWithText("Fortschritt").performClick()
        scrollTo("Nach Raum")
        compose.onNode(hasContentDescription("Küche: 12-mal erledigt in den letzten 30 Tagen")).assertExists()
        compose.onNode(hasContentDescription("Ohne Raum: 3-mal erledigt in den letzten 30 Tagen")).assertExists()
        compose.onNode(hasContentDescription("Bad: 0-mal erledigt", substring = true)).assertDoesNotExist() // no chore there
        scrollTo("Abzeichen")
        scrollToBadge("Am Ball, erreicht")
        scrollToBadge("Verlässlich, erreicht")
        scrollToBadge("Fleissig, noch nicht erreicht: 50 Aufgaben erledigt, 15 von 50")
        screenshot("progress")
    }

    @Test
    fun withoutMotivationBadgesAreHiddenAndRoomsStay() {
        runBlocking { app.settings.update { it.copy(motivationEnabled = false) } }
        seed()
        waitFor("Kühlschrank putzen")
        compose.onNodeWithText("Fortschritt").performClick()
        // Room statistics are statistics, not motivation: they stay.
        scrollTo("Nach Raum")
        scrollTo("Zuletzt erledigt")
        compose.onNodeWithText("Abzeichen").assertDoesNotExist()
        compose.onNode(hasContentDescription("Am Ball", substring = true)).assertDoesNotExist()
        compose.onNodeWithText("Alles erledigt in Folge").assertDoesNotExist()
        screenshot("no-motivation")
    }

    private fun screenshot(name: String) = compose.screenshot("badges-$name")
}
