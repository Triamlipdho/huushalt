package xyz.haselbach.huushalt.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntervalSuggestionTest {
    private val start = LocalDate.parse("2026-01-05")

    private fun chore(schedule: Schedule, paused: Boolean = false) =
        Chore(id = 7, name = "Kühlschrank putzen", notes = "", schedule = schedule, anchorDate = start, dueDate = start, lastDone = null, paused = paused)

    private fun flexible(unit: RepeatUnit, interval: Int) = chore(Schedule(ScheduleMode.FLEXIBLE, unit, interval))

    /** Completions starting at [start], separated by [gaps] days. */
    private fun doneAfterGaps(vararg gaps: Long): List<LocalDate> =
        gaps.fold(listOf(start)) { dates, gap -> dates + dates.last().plusDays(gap) }

    @Test
    fun `every 3 weeks instead of 4 is suggested in weeks`() {
        val suggestion = IntervalSuggestions.suggest(flexible(RepeatUnit.WEEKS, 4), doneAfterGaps(21, 20, 22))
        assertEquals(IntervalSuggestion(7, 3, RepeatUnit.WEEKS), suggestion)
        assertEquals("7:3:WEEKS", suggestion?.key)
    }

    @Test
    fun `less often than planned is suggested too`() {
        assertEquals(
            IntervalSuggestion(7, 10, RepeatUnit.DAYS),
            IntervalSuggestions.suggest(flexible(RepeatUnit.DAYS, 7), doneAfterGaps(10, 9, 11, 10)),
        )
    }

    @Test
    fun `three completions are not enough`() {
        assertNull(IntervalSuggestions.suggest(flexible(RepeatUnit.WEEKS, 4), doneAfterGaps(21, 21)))
        // Two on the same day count once.
        assertNull(IntervalSuggestions.suggest(flexible(RepeatUnit.WEEKS, 4), doneAfterGaps(21, 21, 0)))
    }

    @Test
    fun `fixed schedules, date lists, one-off and paused chores never get a suggestion`() {
        val dates = doneAfterGaps(21, 21, 21)
        assertNull(IntervalSuggestions.suggest(chore(Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 4, setOf(start.dayOfWeek))), dates))
        assertNull(IntervalSuggestions.suggest(chore(Schedule(ScheduleMode.FIXED, RepeatUnit.DAYS, 28)), dates))
        assertNull(IntervalSuggestions.suggest(chore(Schedule(ScheduleMode.FIXED, RepeatUnit.DATES, dates = listOf(start))), dates))
        assertNull(IntervalSuggestions.suggest(chore(Schedule.ONCE), dates))
        assertNull(IntervalSuggestions.suggest(chore(Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 4), paused = true), dates))
    }

    @Test
    fun `small differences are left alone`() {
        // 8 instead of 7 days: less than 2 days.
        assertNull(IntervalSuggestions.suggest(flexible(RepeatUnit.DAYS, 7), doneAfterGaps(8, 8, 8)))
        // 3 instead of 2 days: 50 %, but only 1 day.
        assertNull(IntervalSuggestions.suggest(flexible(RepeatUnit.DAYS, 2), doneAfterGaps(3, 3, 3)))
        // 11 instead of 10 weeks: 7 days, but less than 25 %.
        assertNull(IntervalSuggestions.suggest(flexible(RepeatUnit.WEEKS, 10), doneAfterGaps(77, 77, 77)))
        // 10 days rounds to 1 week: the same as now.
        assertNull(IntervalSuggestions.suggest(flexible(RepeatUnit.WEEKS, 1), doneAfterGaps(10, 10, 10)))
        // 9 instead of 7 days is enough (2 days, 29 %).
        assertEquals(9, IntervalSuggestions.suggest(flexible(RepeatUnit.DAYS, 7), doneAfterGaps(9, 9, 9))?.interval)
    }

    @Test
    fun `months use 30_44 days`() {
        // About every 2 months instead of every month.
        assertEquals(
            IntervalSuggestion(7, 2, RepeatUnit.MONTHS),
            IntervalSuggestions.suggest(flexible(RepeatUnit.MONTHS, 1), doneAfterGaps(60, 62, 59)),
        )
        // 3 weeks rounds to 1 month: no change.
        assertNull(IntervalSuggestions.suggest(flexible(RepeatUnit.MONTHS, 1), doneAfterGaps(21, 21, 21)))
        // 4 months instead of 6.
        assertEquals(4, IntervalSuggestions.suggest(flexible(RepeatUnit.MONTHS, 6), doneAfterGaps(120, 125, 118))?.interval)
    }

    @Test
    fun `the median ignores a single long break`() {
        assertEquals(21.0, IntervalSuggestions.typicalGapDays(doneAfterGaps(21, 90, 20))!!, 0.0)
        // Even number of gaps: the mean of the two in the middle.
        assertEquals(20.5, IntervalSuggestions.typicalGapDays(doneAfterGaps(20, 21, 90, 14))!!, 0.0)
        assertNull(IntervalSuggestions.typicalGapDays(emptyList()))
    }

    @Test
    fun `only the last 8 completions count`() {
        // Long ago every 7 days, lately every 14: the old ones don't count.
        val dates = doneAfterGaps(7, 7, 7, 7, 7, 7, 14, 14, 14, 14, 14, 14, 14)
        assertEquals(14.0, IntervalSuggestions.typicalGapDays(dates)!!, 0.0)
        assertEquals(2, IntervalSuggestions.suggest(flexible(RepeatUnit.WEEKS, 1), dates.shuffled())?.interval)
    }
}
