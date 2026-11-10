package xyz.haselbach.huushalt.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.haselbach.huushalt.domain.Badge
import xyz.haselbach.huushalt.domain.Badges
import xyz.haselbach.huushalt.domain.CompletionRecord
import xyz.haselbach.huushalt.ui.progress.AnnounceNewBadges

/** The "Neues Abzeichen" snackbar comes once per badge, never for badges from before (F7.7). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class AnnounceNewBadgesTest {

    @get:Rule
    val compose = createComposeRule()

    private fun badges(done: Int): List<Badge> =
        Badges.of(List(done) { CompletionRecord(1, LocalDate.of(2026, 1, 1).plusDays(it.toLong()), null) }, bestStreak = 0)

    private var badges by mutableStateOf(badges(0))
    private var announced by mutableStateOf<Set<String>?>(null)

    private fun show(motivation: Boolean = true) {
        val host = SnackbarHostState()
        compose.setContent {
            AnnounceNewBadges(badges, announced, motivation, host) { ids -> announced = announced.orEmpty() + ids }
            SnackbarHost(host)
        }
    }

    private fun assertNoBadgeSnackbar() {
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onNode(hasText("Abzeichen", substring = true)).assertDoesNotExist()
    }

    @Test
    fun theFirstRunRemembersEarnedBadgesSilently() {
        badges = badges(60)
        show()
        compose.waitForIdle()
        assertEquals(setOf("done_1", "done_10", "done_50", "chore_streak_10"), announced)
        assertNoBadgeSnackbar()
    }

    @Test
    fun aNewBadgeIsAnnouncedOnce() {
        announced = emptySet()
        show()
        compose.waitForIdle()
        badges = badges(1)
        compose.waitUntilAtLeastOneExists(hasText("Neues Abzeichen: Der erste Schritt 🏅"), timeoutMillis = 5_000)
        assertEquals(setOf("done_1"), announced)
        compose.onNodeWithText("Neues Abzeichen: Der erste Schritt 🏅").assertExists()
        compose.mainClock.advanceTimeBy(10_000)
        compose.waitUntilDoesNotExist(hasText("Neues Abzeichen", substring = true), timeoutMillis = 5_000)
        // A second completion: nothing new.
        badges = badges(2)
        assertNoBadgeSnackbar()
    }

    @Test
    fun severalNewBadgesShareOneSnackbar() {
        announced = emptySet()
        show()
        compose.waitForIdle()
        badges = badges(12)
        compose.waitUntilAtLeastOneExists(hasText("3 neue Abzeichen 🏅"), timeoutMillis = 5_000)
    }

    @Test
    fun withoutMotivationBadgesAreRememberedSilently() {
        announced = emptySet()
        show(motivation = false)
        compose.waitForIdle()
        badges = badges(1)
        compose.waitForIdle()
        assertEquals(setOf("done_1"), announced)
        assertNoBadgeSnackbar()
    }
}
