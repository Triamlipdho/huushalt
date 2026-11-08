package xyz.haselbach.huushalt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import xyz.haselbach.huushalt.domain.ScheduleCalculator.dueDateAfterCompletion
import xyz.haselbach.huushalt.domain.ScheduleCalculator.firstOccurrenceOnOrAfter
import xyz.haselbach.huushalt.domain.ScheduleCalculator.initialDueDate

/** Nth weekday (F2.10), date lists (F2.11) and the tolerance window (F2.12). */
class MoreSchedulesTest {
    private fun d(text: String) = LocalDate.parse(text)

    private fun nthWeekday(week: Int, day: DayOfWeek, interval: Int = 1) =
        Schedule(ScheduleMode.FIXED, RepeatUnit.MONTHS, interval, setOf(day), weekOfMonth = week)

    @Test
    fun `second Tuesday of every month`() {
        val s = nthWeekday(2, DayOfWeek.TUESDAY)
        val anchor = d("2026-11-10") // the 2nd Tuesday of November
        assertEquals(d("2026-11-10"), initialDueDate(s, anchor))
        assertEquals(d("2026-12-08"), firstOccurrenceOnOrAfter(s, anchor, d("2026-11-11")))
        assertEquals(d("2027-01-12"), firstOccurrenceOnOrAfter(s, anchor, d("2026-12-09")))
    }

    @Test
    fun `last Friday every 3 months`() {
        val s = nthWeekday(Schedule.LAST_WEEK, DayOfWeek.FRIDAY, interval = 3)
        val anchor = d("2026-11-27") // last Friday of November
        assertEquals(d("2026-11-27"), initialDueDate(s, anchor))
        assertEquals(d("2027-02-26"), firstOccurrenceOnOrAfter(s, anchor, d("2026-11-28")))
    }

    @Test
    fun `nth weekday validation`() {
        assertNull(nthWeekday(4, DayOfWeek.MONDAY).validate())
        assertEquals(ScheduleError.INVALID_DAY_OF_MONTH, nthWeekday(5, DayOfWeek.MONDAY).validate())
        assertEquals(
            ScheduleError.INVALID_DAY_OF_MONTH,
            Schedule(ScheduleMode.FIXED, RepeatUnit.MONTHS, 1, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), weekOfMonth = 1).validate(),
        )
    }

    private val paper = Schedule(
        ScheduleMode.FIXED,
        RepeatUnit.DATES,
        dates = listOf(d("2026-11-25"), d("2026-11-11"), d("2026-12-09")),
    )

    @Test
    fun `date list moves from date to date and ends`() {
        assertEquals(d("2026-11-11"), initialDueDate(paper, d("2026-11-08")))
        assertEquals(d("2026-11-25"), dueDateAfterCompletion(paper, d("2026-11-08"), d("2026-11-11"), d("2026-11-11")))
        assertEquals(d("2026-12-09"), dueDateAfterCompletion(paper, d("2026-11-08"), d("2026-11-25"), d("2026-11-25")))
        assertNull("No dates left", dueDateAfterCompletion(paper, d("2026-11-08"), d("2026-12-09"), d("2026-12-09")))
        assertEquals(ScheduleError.NO_DATES, paper.copy(dates = emptyList()).validate())
    }

    @Test
    fun `missed dates of a date list are counted`() {
        assertEquals(2, ScheduleCalculator.missedOccurrences(paper, d("2026-11-08"), d("2026-11-11"), d("2026-11-26")))
    }

    @Test
    fun `tolerance window keeps a chore due instead of overdue`() {
        fun chore(due: LocalDate, tolerance: Int) = Chore(1, "x", "", Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1), due, due, null, toleranceDays = tolerance)
        val today = d("2026-11-12")
        assertEquals(DueStatus.InWindow(2), chore(d("2026-11-10"), 2).status(today))
        assertTrue(chore(d("2026-11-10"), 2).isDueNow(today))
        assertEquals(DueStatus.Overdue(days = 3, missed = 1), chore(d("2026-11-09"), 2).status(today))
    }

    @Test
    fun `tolerance window counts for punctuality and streaks`() {
        val inWindow = CompletionRecord(1, doneDate = d("2026-11-12"), dueDate = d("2026-11-10"), toleranceDays = 2)
        val late = CompletionRecord(1, doneDate = d("2026-11-14"), dueDate = d("2026-11-10"), toleranceDays = 2)
        assertTrue(inWindow.onTime)
        assertEquals(false, late.onTime)
        val days = Progress.overdueDays(listOf(inWindow, late), emptyList(), d("2026-11-01"), d("2026-11-20"))
        // Late one: on time through the 12th; overdue at the end of the 12th and 13th.
        assertEquals(setOf(d("2026-11-12"), d("2026-11-13")), days)
    }
}

class DateListParserTest {
    @Test
    fun `reads dates in Swiss formats and ignores the rest`() {
        val text = "Papier: Di 12.01.2027, Di 26.01.27; 9.2.2027 und 31.02.2027 (ungültig), 12.01.2027"
        assertEquals(
            listOf(LocalDate.parse("2027-01-12"), LocalDate.parse("2027-01-26"), LocalDate.parse("2027-02-09")),
            DateListParser.parse(text),
        )
        assertTrue(DateListParser.parse("nichts").isEmpty())
    }
}
