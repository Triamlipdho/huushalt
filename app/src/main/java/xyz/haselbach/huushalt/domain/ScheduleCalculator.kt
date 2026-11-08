package xyz.haselbach.huushalt.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Date logic for chore schedules. All calculations are date based (no times or time zones, N7).
 *
 * Fixed schedules form a series of occurrences anchored at an anchor date:
 * - days: anchor, anchor + n, anchor + 2n, ...
 * - weeks: the selected weekdays in every n-th week, counted from the week (Monday to Sunday)
 *   that contains the anchor, never before the anchor
 * - months: the day of month in every n-th month, counted from the anchor's month,
 *   never before the anchor
 */
object ScheduleCalculator {

    /**
     * Due date of a newly created or edited chore whose next due date was set to [anchor]. Null for
     * a date list without dates from [anchor] on.
     */
    fun initialDueDate(schedule: Schedule, anchor: LocalDate): LocalDate? =
        if (schedule.mode == ScheduleMode.FIXED && schedule.isRepeating) {
            firstOccurrenceOnOrAfter(schedule, anchor, anchor)
        } else {
            anchor
        }

    /**
     * Due date after the chore was done on [doneDate] (F4.1, F4.7).
     *
     * - One-off chores are finished: returns null.
     * - Flexible chores: [doneDate] plus the interval.
     * - Fixed chores: the completion counts for the current occurrence ([currentDue]), also when done
     *   early. The next due date is the first occurrence after both the current due date and the day
     *   it was done, so missed occurrences collapse into one (F10.5).
     */
    fun dueDateAfterCompletion(
        schedule: Schedule,
        anchor: LocalDate,
        currentDue: LocalDate?,
        doneDate: LocalDate,
    ): LocalDate? = when {
        !schedule.isRepeating -> null
        schedule.mode == ScheduleMode.FLEXIBLE -> addInterval(schedule, doneDate)
        else -> {
            val after = if (currentDue != null && currentDue > doneDate) currentDue else doneDate
            firstOccurrenceOnOrAfter(schedule, anchor, after.plusDays(1))
        }
    }

    /**
     * How many occurrences of a fixed chore were missed: occurrences from [due] up to and including
     * yesterday (F10.5). Flexible and one-off chores have at most one missed occurrence.
     * The result is capped at [MAX_MISSED_COUNT].
     */
    fun missedOccurrences(schedule: Schedule, anchor: LocalDate, due: LocalDate, today: LocalDate): Int {
        if (due >= today) return 0
        if (schedule.mode != ScheduleMode.FIXED || !schedule.isRepeating) return 1
        var count = 0
        var occurrence = due
        var current: LocalDate? = occurrence
        while (current != null && current < today && count < MAX_MISSED_COUNT) {
            count++
            current = firstOccurrenceOnOrAfter(schedule, anchor, current.plusDays(1))
        }
        return count
    }

    /**
     * First occurrence of a fixed repeating [schedule] that is on or after both [date] and [anchor].
     * Null only for a date list whose dates are all before [date].
     */
    fun firstOccurrenceOnOrAfter(schedule: Schedule, anchor: LocalDate, date: LocalDate): LocalDate? {
        require(schedule.isRepeating) { "One-off chores have no occurrences" }
        if (schedule.unit == RepeatUnit.DATES) return schedule.dates.sorted().firstOrNull { it >= date }
        val from = if (date > anchor) date else anchor
        val n = schedule.interval.toLong()
        return when (schedule.unit) {
            RepeatUnit.DAYS -> {
                val diff = ChronoUnit.DAYS.between(anchor, from)
                anchor.plusDays(ceilDiv(diff, n) * n)
            }
            RepeatUnit.WEEKS -> nextWeeklyOccurrence(schedule, anchor, from)
            RepeatUnit.MONTHS ->
                if (schedule.weekOfMonth != null) nextNthWeekdayOccurrence(schedule, anchor, from)
                else nextMonthlyOccurrence(schedule, anchor, from)
            RepeatUnit.NONE, RepeatUnit.DATES -> error("unreachable")
        }
    }

    /** E.g. "every month on the 2nd Tuesday" or "every 3 months on the last Friday" (F2.10). */
    private fun nextNthWeekdayOccurrence(schedule: Schedule, anchor: LocalDate, from: LocalDate): LocalDate {
        val weekday = schedule.weekdays.single()
        val week = schedule.weekOfMonth!!
        val anchorMonth = YearMonth.from(anchor)
        val n = schedule.interval.toLong()
        val diff = ChronoUnit.MONTHS.between(anchorMonth, YearMonth.from(from))
        var month = anchorMonth.plusMonths(ceilDiv(diff, n) * n)
        while (true) {
            val candidate = nthWeekdayOf(month, weekday, week)
            if (candidate >= from) return candidate
            month = month.plusMonths(n)
        }
    }

    fun nthWeekdayOf(month: YearMonth, weekday: DayOfWeek, week: Int): LocalDate =
        if (week == Schedule.LAST_WEEK) {
            month.atEndOfMonth().with(TemporalAdjusters.previousOrSame(weekday))
        } else {
            month.atDay(1).with(TemporalAdjusters.nextOrSame(weekday)).plusWeeks(week - 1L)
        }

    private fun nextWeeklyOccurrence(schedule: Schedule, anchor: LocalDate, from: LocalDate): LocalDate {
        val weekdays = schedule.weekdays.ifEmpty { setOf(anchor.dayOfWeek) }
        val anchorWeek = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val n = schedule.interval.toLong()
        var day = from
        // Within (n + 1) weeks there is always an active week containing every weekday.
        repeat(7 * (schedule.interval + 1)) {
            val week = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekIndex = ChronoUnit.WEEKS.between(anchorWeek, week)
            if (weekIndex % n == 0L && day.dayOfWeek in weekdays) return day
            day = day.plusDays(1)
        }
        error("No weekly occurrence found")
    }

    private fun nextMonthlyOccurrence(schedule: Schedule, anchor: LocalDate, from: LocalDate): LocalDate {
        val dayOfMonth = schedule.dayOfMonth ?: anchor.dayOfMonth
        val anchorMonth = YearMonth.from(anchor)
        val n = schedule.interval.toLong()
        val diff = ChronoUnit.MONTHS.between(anchorMonth, YearMonth.from(from))
        var month = anchorMonth.plusMonths(ceilDiv(diff, n) * n)
        while (true) {
            val candidate = month.atDay(minOf(dayOfMonth, month.lengthOfMonth()))
            if (candidate >= from) return candidate
            month = month.plusMonths(n)
        }
    }

    private fun addInterval(schedule: Schedule, date: LocalDate): LocalDate {
        val n = schedule.interval.toLong()
        return when (schedule.unit) {
            RepeatUnit.DAYS -> date.plusDays(n)
            RepeatUnit.WEEKS -> date.plusWeeks(n)
            RepeatUnit.MONTHS -> date.plusMonths(n)
            RepeatUnit.NONE, RepeatUnit.DATES -> date
        }
    }

    private fun ceilDiv(a: Long, b: Long): Long = Math.floorDiv(a + b - 1, b)

    const val MAX_MISSED_COUNT = 99
}
