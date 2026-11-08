package xyz.haselbach.huushalt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HolidayRulesTest {
    private fun d(text: String) = LocalDate.parse(text)

    // Holiday Monday 2026-11-09 to Sunday 2026-11-15 (7 days); back on Monday the 16th.
    private val holiday = Holiday(1, d("2026-11-09"), d("2026-11-15"), applied = false)
    private val anchor = d("2026-11-02")

    private fun chore(schedule: Schedule, due: LocalDate?, lastDone: LocalDate? = null) =
        Chore(1, "x", "", schedule, anchor, due, lastDone)

    private val flexibleWeekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)
    private val everyTuesday = Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 1, setOf(DayOfWeek.TUESDAY))

    @Test
    fun `flexible chores move back by the length of the holiday`() {
        assertEquals(d("2026-11-17"), HolidayRules.dueDateAfterHoliday(chore(flexibleWeekly, d("2026-11-10")), holiday))
        assertEquals(d("2026-11-27"), HolidayRules.dueDateAfterHoliday(chore(flexibleWeekly, d("2026-11-20")), holiday))
    }

    @Test
    fun `flexible chores done during the holiday or overdue before it stay`() {
        val doneDuring = chore(flexibleWeekly, d("2026-11-18"), lastDone = d("2026-11-11"))
        assertEquals(d("2026-11-18"), HolidayRules.dueDateAfterHoliday(doneDuring, holiday))
        assertEquals(d("2026-11-05"), HolidayRules.dueDateAfterHoliday(chore(flexibleWeekly, d("2026-11-05")), holiday))
    }

    @Test
    fun `fixed chores skip occurrences inside the holiday`() {
        assertEquals(d("2026-11-17"), HolidayRules.dueDateAfterHoliday(chore(everyTuesday, d("2026-11-10")), holiday))
        assertEquals(d("2026-11-24"), HolidayRules.dueDateAfterHoliday(chore(everyTuesday, d("2026-11-24")), holiday))
    }

    @Test
    fun `one-off chores during the holiday are due on the day back`() {
        assertEquals(d("2026-11-16"), HolidayRules.dueDateAfterHoliday(chore(Schedule.ONCE, d("2026-11-12")), holiday))
        assertNull(HolidayRules.dueDateAfterHoliday(chore(Schedule.ONCE, null), holiday))
    }

    @Test
    fun `rescheduling counts flexible chores from today and moves fixed ones to the next date`() {
        val today = d("2026-11-12") // Thursday
        assertEquals(d("2026-11-19"), HolidayRules.dueDateWhenRescheduled(chore(flexibleWeekly, d("2026-11-01")), today))
        assertEquals(d("2026-11-17"), HolidayRules.dueDateWhenRescheduled(chore(everyTuesday, d("2026-11-03")), today))
        assertEquals(today, HolidayRules.dueDateWhenRescheduled(chore(Schedule.ONCE, d("2026-11-01")), today))
    }

    @Test
    fun `holiday days are neutral for the streak (F10_3)`() {
        val start = d("2026-11-02")
        val today = d("2026-11-18")
        val overdue = setOf(d("2026-11-10"), d("2026-11-11"), d("2026-11-12"))
        val holidayDays = holiday.daysUntil(today).toSet()
        // 7 days before, holiday skipped, 2 days after (16th, 17th); today not clean yet.
        assertEquals(Streaks(9, 9), Progress.streaks(overdue, start, today, todayClean = false, holidayDays = holidayDays))
    }

    @Test
    fun `missed periods count as overdue days`() {
        val days = Progress.overdueDays(
            completions = emptyList(),
            currentDueDates = emptyList(),
            start = d("2026-11-02"),
            today = d("2026-11-10"),
            missedPeriods = listOf(MissedPeriod(1, d("2026-11-03"), d("2026-11-05"))),
        )
        assertEquals(setOf(d("2026-11-03"), d("2026-11-04")), days)
    }
}
