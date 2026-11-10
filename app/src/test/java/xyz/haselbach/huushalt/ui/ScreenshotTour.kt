package xyz.haselbach.huushalt.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
import xyz.haselbach.huushalt.data.ChecklistDraft
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode
import xyz.haselbach.huushalt.settings.AppSettings
import xyz.haselbach.huushalt.settings.ThemeMode
import xyz.haselbach.huushalt.testing.screenshot

/**
 * Screenshots of every screen with realistic data, for visual reviews. Writes to
 * app/build/screenshots/tour-<variant>-*.png; asserts only that the screens open.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreenshotTourBase(private val variant: String, private val theme: ThemeMode) {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app get() = compose.activity.application as HuushaltApplication

    @Before
    fun seed() = runBlocking {
        app.settings.update { AppSettings(theme = theme, permissionCardDismissed = true) }
        val repo = app.repository
        val today = LocalDate.now()
        val weekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)
        val kitchen = repo.addArea("Küche")
        val bath = repo.addArea("Bad")
        val bathId = repo.save(
            null, "Bad putzen", "Essigreiniger für die Dusche", weekly, today.minusDays(10), bath,
            listOf(ChecklistDraft(null, "Lavabo"), ChecklistDraft(null, "WC"), ChecklistDraft(null, "Dusche"), ChecklistDraft(null, "Spiegel")),
        )
        listOf(30L, 22L, 15L).forEach { repo.complete(bathId, today.minusDays(it)) }
        repo.save(null, "Kühlschrank putzen", "", Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.MONTHS, 1), today.minusDays(2), kitchen)
        val vacuum = repo.save(null, "Staubsaugen", "", weekly, today.minusDays(7))
        listOf(21L, 14L, 7L).forEach { repo.complete(vacuum, today.minusDays(it)) }
        repo.save(null, "Pflanzen giessen", "", Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.DAYS, 4), today)
        repo.save(
            null, "Kehricht rausbringen", "",
            Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 1, setOf(today.plusDays(2).dayOfWeek)), today.plusDays(2),
        )
        repo.save(null, "Bettwäsche wechseln", "", Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 2), today.plusDays(4))
        repo.save(null, "Fenster putzen", "", Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.MONTHS, 3), today.plusDays(40))
        repo.saveHoliday(null, today.plusDays(20), today.plusDays(27), today)
    }

    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMillis = 5_000)
    private fun shot(name: String) = compose.screenshot("tour-$variant-$name")

    @Test
    fun tour() {
        waitFor("Bad putzen")
        shot("01-heute")
        compose.onNodeWithText("Alle Aufgaben").performClick()
        waitFor("Nach Fälligkeit")
        shot("02-alle")
        compose.onNodeWithText("Nach Raum").performClick()
        shot("03-alle-raum")
        compose.onNodeWithText("Fortschritt").performClick()
        waitFor("Erledigt pro Woche")
        shot("04-fortschritt")
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(hasText("Zuletzt erledigt"))
        shot("04b-abzeichen")
        compose.onNodeWithText("Heute").performClick()
        waitFor("Bad putzen")
        compose.onNodeWithText("Bad putzen").performClick()
        waitFor("Checkliste")
        shot("05-detail")
        compose.onNodeWithContentDescription("Bearbeiten").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Aufgabe löschen"), timeoutMillis = 5_000)
        shot("06-editor")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithContentDescription("Neue Aufgabe").performClick()
        waitFor("Neue Aufgabe")
        shot("07-neu")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        waitFor("Einstellungen")
        shot("08-einstellungen")
        compose.onNodeWithText("Vorschläge").performScrollTo().performClick()
        waitFor("Ganze Wohnung")
        shot("09-vorschlaege")
    }

    private fun hasContentDescriptionText(text: String) = androidx.compose.ui.test.hasContentDescription(text)
}

@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTourLight : ScreenshotTourBase("light", ThemeMode.LIGHT)

@Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
class ScreenshotTourDark : ScreenshotTourBase("dark", ThemeMode.DARK)

/** Large font (Android's largest standard size before 2x), to check that nothing breaks (N6). */
@Config(qualifiers = "w411dp-h891dp-xxhdpi", fontScale = 1.5f)
class ScreenshotTourLargeFont : ScreenshotTourBase("large", ThemeMode.LIGHT)
