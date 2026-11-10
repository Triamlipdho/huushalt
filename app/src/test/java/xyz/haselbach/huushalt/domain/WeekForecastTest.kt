package xyz.haselbach.huushalt.domain

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Occurrences for the week view (F3.6). 2026-11-09 is a Monday. */
class WeekForecastTest {
    private fun d(text: String) = LocalDate.parse(text)

    private val today = d("2026-11-10") // Tuesday

    private fun chore(schedule: Schedule, due: LocalDate?, anchor: LocalDate = due ?: today, paused: Boolean = false, lastDone: LocalDate? = null) =
        Chore(1, "Test", "", schedule, anchor, due, lastDone, paused = paused)

    private fun dates(chore: Chore, from: String, to: String, holidays: List<Holiday> = emptyList()) =
        WeekForecast.occurrences(chore, d(from), d(to), today, holidays).map { it.date }

    @Test
    fun `fixed weekly on Monday and Thursday`() {
        val s = Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 1, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        val c = chore(s, due = d("2026-11-12"), anchor = d("2026-11-02"))
        val result = WeekForecast.occurrences(c, d("2026-11-09"), d("2026-11-22"), today)
        assertEquals(listOf(d("2026-11-12"), d("2026-11-16"), d("2026-11-19")), result.map { it.date })
        assertEquals("Only the real due date is not a forecast", listOf(false, true, true), result.map { it.projected })
    }

    @Test
    fun `every 2 weeks`() {
        val s = Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 2, setOf(DayOfWeek.WEDNESDAY))
        val c = chore(s, due = d("2026-11-11"))
        assertEquals(listOf(d("2026-11-11"), d("2026-11-25"), d("2026-12-09")), dates(c, "2026-11-09", "2026-12-13"))
        assertEquals("Off week is empty", emptyList<LocalDate>(), dates(c, "2026-11-16", "2026-11-22"))
    }

    @Test
    fun `flexible every 3 days counts from each projected due date`() {
        val s = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.DAYS, 3)
        val c = chore(s, due = d("2026-11-11"))
        assertEquals(listOf(d("2026-11-11"), d("2026-11-14"), d("2026-11-17"), d("2026-11-20")), dates(c, "2026-11-09", "2026-11-22"))
    }

    @Test
    fun `overdue chores are on today and the next ones count from today`() {
        val flexible = chore(Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.DAYS, 3), due = d("2026-11-05"))
        assertEquals(listOf(d("2026-11-10"), d("2026-11-13")), dates(flexible, "2026-11-09", "2026-11-15"))
        assertEquals(listOf(d("2026-11-16"), d("2026-11-19"), d("2026-11-22")), dates(flexible, "2026-11-16", "2026-11-22"))

        val fixed = chore(Schedule(ScheduleMode.FIXED, RepeatUnit.DAYS, 7), due = d("2026-11-03"))
        assertEquals(listOf(d("2026-11-10"), d("2026-11-17")), dates(fixed, "2026-11-09", "2026-11-22"))
    }

    @Test
    fun `monthly on the nth weekday`() {
        val s = Schedule(ScheduleMode.FIXED, RepeatUnit.MONTHS, 1, setOf(DayOfWeek.TUESDAY), weekOfMonth = 2)
        val c = chore(s, due = d("2026-11-10"))
        assertEquals(listOf(d("2026-11-10"), d("2026-12-08"), d("2027-01-12")), dates(c, "2026-11-09", "2027-01-31"))
        assertEquals(listOf(d("2026-12-08")), dates(c, "2026-12-07", "2026-12-13"))
    }

    @Test
    fun `date list uses its dates and ends`() {
        val s = Schedule(ScheduleMode.FIXED, RepeatUnit.DATES, dates = listOf(d("2026-11-25"), d("2026-11-11"), d("2026-11-13")))
        val c = chore(s, due = d("2026-11-11"), anchor = d("2026-11-01"))
        assertEquals(listOf(d("2026-11-11"), d("2026-11-13"), d("2026-11-25")), dates(c, "2026-11-09", "2027-12-31"))
    }

    @Test
    fun `paused finished and one-off chores`() {
        val weekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)
        assertTrue(dates(chore(weekly, due = d("2026-11-11"), paused = true), "2026-11-09", "2026-12-31").isEmpty())
        assertTrue(dates(chore(Schedule.ONCE, due = null), "2026-11-09", "2026-12-31").isEmpty())
        assertEquals(listOf(d("2026-11-12")), dates(chore(Schedule.ONCE, due = d("2026-11-12")), "2026-11-09", "2026-12-31"))
    }

    @Test
    fun `daily chore far ahead is capped and terminates`() {
        val daily = chore(Schedule(ScheduleMode.FIXED, RepeatUnit.DAYS, 1), due = today)
        val result = dates(daily, "2026-11-09", "2099-12-31")
        assertEquals(WeekForecast.MAX_OCCURRENCES, result.size)
    }

    @Test
    fun `holidays skip fixed occurrences and move flexible ones`() {
        val holiday = Holiday(1, d("2026-11-16"), d("2026-11-22"), applied = false)
        val fixed = chore(Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 1, setOf(DayOfWeek.WEDNESDAY)), due = d("2026-11-11"))
        assertEquals(listOf(d("2026-11-11"), d("2026-11-25")), dates(fixed, "2026-11-09", "2026-11-29", listOf(holiday)))

        val flexible = chore(Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.DAYS, 7), due = d("2026-11-18"))
        val moved = WeekForecast.occurrences(flexible, d("2026-11-09"), d("2026-12-06"), today, listOf(holiday))
        assertEquals("Moved back by the 7 holiday days", listOf(d("2026-11-25"), d("2026-12-02")), moved.map { it.date })
        assertTrue("A moved due date is a forecast", moved.first().projected)

        val applied = holiday.copy(applied = true)
        assertEquals(listOf(d("2026-11-18")), dates(flexible, "2026-11-16", "2026-11-22", listOf(applied)))
    }

    @Test
    fun `week has seven days from the first day of the week`() {
        val start = WeekForecast.weekStart(today, DayOfWeek.SUNDAY)
        assertEquals(d("2026-11-08"), start)
        val c = chore(Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 1, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)), due = d("2026-11-12"), anchor = d("2026-11-02"))
        val week = WeekForecast.week(listOf(c), start, today)
        assertEquals(7, week.size)
        assertEquals(listOf(d("2026-11-12")), week.filter { it.second.isNotEmpty() }.map { it.first })
        assertEquals(d("2026-11-09"), WeekForecast.weekStart(today, DayOfWeek.MONDAY))
    }
}
