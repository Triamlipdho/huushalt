package xyz.haselbach.huushalt.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/**
 * A chore on a day of the week view (F3.6).
 *
 * @param projected true for a forecast: an occurrence after the next due date, or a due date that
 *   a holiday will still move
 */
data class Occurrence(val chore: Chore, val date: LocalDate, val projected: Boolean)

/** Upcoming occurrences of chores for the week view (F3.6). Pure date logic, no times (N7). */
object WeekForecast {

    /** At most this many occurrences are projected per chore, so a broken schedule can't loop forever. */
    const val MAX_OCCURRENCES = 500

    /** The week view goes this many weeks ahead of the current one. */
    const val MAX_WEEKS_AHEAD = 12

    /** First day of the week that contains [date]. */
    fun weekStart(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    /**
     * Calendar week ("KW") of the week starting on [weekStart], as used in Switzerland (ISO 8601).
     * Weeks that start on another day than Monday get the number of the ISO week they share most
     * days with, i.e. the one containing their fourth day.
     */
    fun calendarWeek(weekStart: LocalDate): Int = weekStart.plusDays(3).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    /**
     * The days on which [chore] will be due from [from] to [to] (both inclusive), as seen on [today].
     *
     * - The first occurrence is the real next due date. Overdue chores (and those in their
     *   tolerance window) are shown on today, or on the day after a holiday that is on today.
     * - Later occurrences are projected as if each one were done on the day it is shown:
     *   fixed schedules continue with their series, flexible ones count from that day, date lists
     *   use their dates.
     * - Paused, finished and one-off chores have at most their one due date.
     * - Holidays not yet applied move occurrences like [HolidayRules.dueDateAfterHoliday] will.
     */
    fun occurrences(
        chore: Chore,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate,
        holidays: List<Holiday> = emptyList(),
    ): List<Occurrence> {
        if (chore.paused) return emptyList()
        val realDue = chore.dueDate ?: return emptyList()
        // Applied holidays have already moved the chores; past ones are applied on the next refresh.
        val pending = holidays.filter { !it.applied && !it.isOver(today) }.sortedBy { it.start }
        val result = mutableListOf<Occurrence>()
        var due: LocalDate = realDue
        var lastDone: LocalDate? = chore.lastDone
        for (index in 0 until MAX_OCCURRENCES) {
            val moved = pending.fold(due) { date, holiday ->
                HolidayRules.dueDateAfterHoliday(chore.copy(dueDate = date, lastDone = lastDone), holiday) ?: date
            }
            val shown = if (moved < today) dayToCatchUp(today, pending) else moved
            if (shown > to) break
            if (shown >= from) result += Occurrence(chore, shown, projected = index > 0 || moved != due)
            if (!chore.schedule.isRepeating) break
            val next = ScheduleCalculator.dueDateAfterCompletion(chore.schedule, chore.anchorDate, due, shown) ?: break
            if (next <= due) break
            lastDone = shown
            due = next
        }
        return result
    }

    /** The occurrences of all [chores] on each of the 7 days from [weekStart] on, sorted by name. */
    fun week(
        chores: List<Chore>,
        weekStart: LocalDate,
        today: LocalDate,
        holidays: List<Holiday> = emptyList(),
    ): List<Pair<LocalDate, List<Occurrence>>> {
        val end = weekStart.plusDays(6)
        val byDay = chores
            .flatMap { occurrences(it, weekStart, end, today, holidays) }
            .sortedWith(compareBy<Occurrence> { it.projected }.thenBy { it.chore.name.lowercase() })
            .groupBy { it.date }
        return (0L..6L).map { weekStart.plusDays(it) }.map { day -> day to byDay[day].orEmpty() }
    }

    /** Overdue chores are shown on today; during a holiday on the day after it. */
    private fun dayToCatchUp(today: LocalDate, holidays: List<Holiday>): LocalDate =
        holidays.firstOrNull { it.isActive(today) }?.end?.plusDays(1) ?: today
}
