package xyz.haselbach.huushalt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import xyz.haselbach.huushalt.domain.ScheduleCalculator.dueDateAfterCompletion
import xyz.haselbach.huushalt.domain.ScheduleCalculator.firstOccurrenceOnOrAfter
import xyz.haselbach.huushalt.domain.ScheduleCalculator.initialDueDate
import xyz.haselbach.huushalt.domain.ScheduleCalculator.missedOccurrences

class ScheduleCalculatorTest {

    private fun date(text: String) = LocalDate.parse(text)

    private fun fixed(unit: RepeatUnit, interval: Int = 1, vararg weekdays: java.time.DayOfWeek, dayOfMonth: Int? = null) =
        Schedule(ScheduleMode.FIXED, unit, interval, weekdays.toSet(), dayOfMonth)

    private fun flexible(unit: RepeatUnit, interval: Int) = Schedule(ScheduleMode.FLEXIBLE, unit, interval)

    // 2026-11-02 is a Monday.

    @Test
    fun `fixed every 3 days from anchor`() {
        val s = fixed(RepeatUnit.DAYS, 3)
        val anchor = date("2026-11-02")
        assertEquals(date("2026-11-02"), firstOccurrenceOnOrAfter(s, anchor, date("2026-11-01")))
        assertEquals(date("2026-11-02"), firstOccurrenceOnOrAfter(s, anchor, date("2026-11-02")))
        assertEquals(date("2026-11-05"), firstOccurrenceOnOrAfter(s, anchor, date("2026-11-03")))
        assertEquals(date("2026-11-05"), firstOccurrenceOnOrAfter(s, anchor, date("2026-11-05")))
    }

    @Test
    fun `fixed weekly on several weekdays`() {
        val s = fixed(RepeatUnit.WEEKS, 1, MONDAY, THURSDAY)
        val anchor = date("2026-11-03") // Tuesday
        assertEquals(date("2026-11-05"), initialDueDate(s, anchor))
        assertEquals(date("2026-11-09"), firstOccurrenceOnOrAfter(s, anchor, date("2026-11-06")))
    }

    @Test
    fun `every second week on Wednesday counts weeks from the anchor week (F2_9)`() {
        val s = fixed(RepeatUnit.WEEKS, 2, WEDNESDAY)
        val anchor = date("2026-11-04") // Wednesday
        assertEquals(date("2026-11-04"), initialDueDate(s, anchor))
        assertEquals(date("2026-11-18"), firstOccurrenceOnOrAfter(s, anchor, date("2026-11-05")))
        assertEquals(date("2026-12-02"), firstOccurrenceOnOrAfter(s, anchor, date("2026-11-19")))
    }

    @Test
    fun `every second week when the anchor is after the weekday in its week`() {
        val s = fixed(RepeatUnit.WEEKS, 2, MONDAY)
        val anchor = date("2026-11-04") // Wednesday; Monday of this week is before the anchor
        assertEquals(date("2026-11-16"), initialDueDate(s, anchor))
    }

    @Test
    fun `fixed monthly uses the last day in short months`() {
        val s = fixed(RepeatUnit.MONTHS, 1, dayOfMonth = 31)
        val anchor = date("2026-01-31")
        assertEquals(date("2026-02-28"), firstOccurrenceOnOrAfter(s, anchor, date("2026-02-01")))
        assertEquals(date("2026-03-31"), firstOccurrenceOnOrAfter(s, anchor, date("2026-03-01")))
        assertEquals(date("2028-02-29"), firstOccurrenceOnOrAfter(s, anchor, date("2028-02-01")))
    }

    @Test
    fun `fixed every 3 months`() {
        val s = fixed(RepeatUnit.MONTHS, 3, dayOfMonth = 15)
        val anchor = date("2026-01-15")
        assertEquals(date("2026-04-15"), firstOccurrenceOnOrAfter(s, anchor, date("2026-01-16")))
        assertEquals(date("2026-04-15"), firstOccurrenceOnOrAfter(s, anchor, date("2026-03-20")))
        assertEquals(date("2026-07-15"), firstOccurrenceOnOrAfter(s, anchor, date("2026-04-16")))
    }

    @Test
    fun `one-off chore is finished after completion`() {
        assertNull(dueDateAfterCompletion(Schedule.ONCE, date("2026-11-02"), date("2026-11-02"), date("2026-11-02")))
    }

    @Test
    fun `flexible chore counts from the day it was done`() {
        val s = flexible(RepeatUnit.WEEKS, 4)
        assertEquals(
            date("2026-12-08"),
            dueDateAfterCompletion(s, date("2026-11-01"), currentDue = date("2026-11-01"), doneDate = date("2026-11-10")),
        )
        assertEquals(
            date("2027-01-30"),
            dueDateAfterCompletion(flexible(RepeatUnit.MONTHS, 2), date("2026-11-01"), date("2026-11-30"), date("2026-11-30")),
        )
    }

    @Test
    fun `fixed chore done on time moves to the next occurrence`() {
        val s = fixed(RepeatUnit.WEEKS, 1, TUESDAY)
        val anchor = date("2026-11-03")
        assertEquals(date("2026-11-10"), dueDateAfterCompletion(s, anchor, date("2026-11-03"), date("2026-11-03")))
    }

    @Test
    fun `fixed chore done early counts for the current occurrence (F4_7)`() {
        val s = fixed(RepeatUnit.WEEKS, 1, TUESDAY)
        val anchor = date("2026-11-03")
        // Due Tuesday the 10th, done on Sunday the 8th.
        assertEquals(date("2026-11-17"), dueDateAfterCompletion(s, anchor, date("2026-11-10"), date("2026-11-08")))
    }

    @Test
    fun `fixed chore done late skips missed occurrences (F10_5)`() {
        val s = fixed(RepeatUnit.WEEKS, 1, TUESDAY)
        val anchor = date("2026-11-03")
        // Due on the 3rd, done three weeks later on Thursday the 26th.
        assertEquals(date("2026-12-01"), dueDateAfterCompletion(s, anchor, date("2026-11-03"), date("2026-11-26")))
    }

    @Test
    fun `missed occurrences of a fixed chore`() {
        val s = fixed(RepeatUnit.WEEKS, 1, TUESDAY)
        val anchor = date("2026-11-03")
        val due = date("2026-11-03")
        assertEquals(0, missedOccurrences(s, anchor, due, date("2026-11-03")))
        assertEquals(1, missedOccurrences(s, anchor, due, date("2026-11-04")))
        assertEquals(1, missedOccurrences(s, anchor, due, date("2026-11-10")))
        assertEquals(2, missedOccurrences(s, anchor, due, date("2026-11-11")))
        assertEquals(3, missedOccurrences(s, anchor, due, date("2026-11-20")))
    }

    @Test
    fun `missed occurrences are capped`() {
        val s = fixed(RepeatUnit.DAYS, 1)
        val anchor = date("2020-01-01")
        assertEquals(ScheduleCalculator.MAX_MISSED_COUNT, missedOccurrences(s, anchor, anchor, date("2026-01-01")))
    }

    @Test
    fun `validation`() {
        assertNull(Schedule.ONCE.validate())
        assertEquals(ScheduleError.INTERVAL_OUT_OF_RANGE, flexible(RepeatUnit.DAYS, 0).validate())
        assertEquals(ScheduleError.NO_WEEKDAY, fixed(RepeatUnit.WEEKS, 1).validate())
        assertNull(flexible(RepeatUnit.WEEKS, 2).validate())
        assertEquals(ScheduleError.INVALID_DAY_OF_MONTH, fixed(RepeatUnit.MONTHS, 1).validate())
    }

    @Test
    fun `status of a chore`() {
        val today = date("2026-11-10")
        fun chore(due: LocalDate?) = Chore(1, "x", "", fixed(RepeatUnit.DAYS, 1), date("2026-11-01"), due, null)
        assertEquals(DueStatus.Overdue(days = 2, missed = 2), chore(date("2026-11-08")).status(today))
        assertEquals(DueStatus.Today, chore(today).status(today))
        assertEquals(DueStatus.Upcoming(date("2026-11-17")), chore(date("2026-11-17")).status(today))
        assertEquals(DueStatus.Later(date("2026-11-18")), chore(date("2026-11-18")).status(today))
        assertEquals(DueStatus.Finished, chore(null).status(today))
    }
}
