package xyz.haselbach.huushalt.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * One completion: when it was done and the due date it fulfilled (null for unknown).
 * [toleranceDays] after the due date still count as on time (F2.12).
 */
data class CompletionRecord(
    val choreId: Long,
    val doneDate: LocalDate,
    val dueDate: LocalDate?,
    val toleranceDays: Int = 0,
) {
    /** Last day that still counts as on time. */
    val lastOnTimeDay: LocalDate? get() = dueDate?.plusDays(toleranceDays.toLong())
    val onTime: Boolean get() = lastOnTimeDay.let { it == null || doneDate <= it }
}

data class Streaks(val current: Int, val best: Int)

/** A chore was overdue from [from] until the day before [until] without being done. */
data class MissedPeriod(val choreId: Long, val from: LocalDate, val until: LocalDate)

/**
 * Motivation figures, all calculated from the history so they stay right when past completions
 * are edited or deleted (F6.4).
 */
object Progress {

    /**
     * Days before [today] on which at least one chore was overdue at the end of the day (F7.2):
     * a chore due on day p and done on day d > p was overdue from p to d - 1; a chore that is
     * overdue now has been since its due date. Postponing before the due date leaves no overdue
     * days, so it doesn't break a streak (F7.6). With a tolerance window, overdue starts on its last
     * day (F2.12). [currentDueDates] are the days chores became overdue, tolerance included.
     */
    fun overdueDays(
        completions: List<CompletionRecord>,
        currentDueDates: List<LocalDate>,
        start: LocalDate,
        today: LocalDate,
        /** Overdue periods that ended without a completion, e.g. postponed while overdue (F4.4, F10.6). */
        missedPeriods: List<MissedPeriod> = emptyList(),
    ): Set<LocalDate> {
        val days = HashSet<LocalDate>()
        fun addRange(from: LocalDate, toExclusive: LocalDate) {
            var day = maxOf(from, start)
            val end = minOf(toExclusive, today)
            while (day < end) {
                days += day
                day = day.plusDays(1)
            }
        }
        completions.forEach { c -> c.lastOnTimeDay?.let { last -> if (c.doneDate > last) addRange(last, c.doneDate) } }
        currentDueDates.forEach { due -> addRange(due, today) }
        missedPeriods.forEach { addRange(it.from, it.until) }
        return days
    }

    /**
     * "Alles erledigt" streak (F7.2): days in a row without an overdue chore at the end of the day,
     * from [start] (the day the first chore was created). Today only counts once nothing is open
     * ([todayClean]); until then the streak ending yesterday is still alive. With
     * [freeDayPerWeek], one overdue day per week (starting on [weekStart]) doesn't break it (F7.6).
     * [holidayDays] are neutral: they neither count nor break the streak (F10.3).
     */
    fun streaks(
        overdueDays: Set<LocalDate>,
        start: LocalDate,
        today: LocalDate,
        todayClean: Boolean,
        freeDayPerWeek: Boolean = true,
        holidayDays: Set<LocalDate> = emptySet(),
        weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ): Streaks {
        val last = if (todayClean) today else today.minusDays(1)
        var run = 0
        var best = 0
        val freeDaysUsed = HashSet<LocalDate>()
        var day = start
        while (day <= last) {
            if (day in holidayDays) {
                day = day.plusDays(1)
                continue
            }
            if (day in overdueDays) {
                val week = day.with(TemporalAdjusters.previousOrSame(weekStart))
                if (freeDayPerWeek && freeDaysUsed.add(week)) {
                    run++
                } else {
                    run = 0
                    freeDaysUsed.clear()
                }
            } else {
                run++
            }
            best = maxOf(best, run)
            day = day.plusDays(1)
        }
        return Streaks(current = run, best = best)
    }

    /** How many times in a row a chore was done on time, newest first (F7.3). */
    fun choreStreak(completionsNewestFirst: List<CompletionRecord>, overdueNow: Boolean): Int =
        if (overdueNow) 0 else completionsNewestFirst.takeWhile { it.onTime }.size

    fun stats(completions: List<CompletionRecord>, today: LocalDate, weekStart: DayOfWeek = DayOfWeek.MONDAY): Stats {
        val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(weekStart))
        val thisMonth = YearMonth.from(today)
        fun countWeek(weekStart: LocalDate) =
            completions.count { it.doneDate >= weekStart && it.doneDate < weekStart.plusWeeks(1) }
        fun countMonth(month: YearMonth) = completions.count { YearMonth.from(it.doneDate) == month }
        val recent = completions.filter { it.doneDate > today.minusDays(ON_TIME_DAYS) && it.doneDate <= today }
        return Stats(
            thisWeek = countWeek(thisWeekStart),
            lastWeek = countWeek(thisWeekStart.minusWeeks(1)),
            thisMonth = countMonth(thisMonth),
            lastMonth = countMonth(thisMonth.minusMonths(1)),
            onTimePercent = if (recent.isEmpty()) null else recent.count { it.onTime } * 100 / recent.size,
            weeks = (WEEKS_IN_CHART - 1 downTo 0).map { ago ->
                val start = thisWeekStart.minusWeeks(ago.toLong())
                WeekCount(start, countWeek(start))
            },
        )
    }

    /** Punctuality is measured over the last 30 days. */
    const val ON_TIME_DAYS = 30L
    const val WEEKS_IN_CHART = 8
}

data class WeekCount(val weekStart: LocalDate, val count: Int)

data class Stats(
    val thisWeek: Int,
    val lastWeek: Int,
    val thisMonth: Int,
    val lastMonth: Int,
    /** Share of completions done on time in the last 30 days; null without completions. */
    val onTimePercent: Int?,
    val weeks: List<WeekCount>,
)
