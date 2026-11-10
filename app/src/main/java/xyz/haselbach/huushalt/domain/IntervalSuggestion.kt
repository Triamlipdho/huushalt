package xyz.haselbach.huushalt.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A suggestion to change the interval of a flexible chore to how often it is really done (F2.13),
 * e.g. every 3 weeks instead of every 4. [interval] is in the chore's own [unit].
 */
data class IntervalSuggestion(val choreId: Long, val interval: Int, val unit: RepeatUnit) {
    /**
     * Identifies this suggestion when it is dismissed ("Nein danke"), e.g. "12:3:WEEKS". A later
     * suggestion with another value has another key, so it may be shown again.
     */
    val key: String get() = "$choreId:$interval:${unit.name}"
}

/**
 * Suggests intervals from real behaviour (F2.13).
 *
 * Rules, kept deliberately simple:
 * - Only for repeating flexible chores (every n days, weeks or months after completion) that are
 *   not paused. Fixed schedules, date lists and one-off chores never get a suggestion: their dates
 *   don't depend on when the chore was done.
 * - Only the last [MAX_COMPLETIONS] days with a completion count (several on one day count once),
 *   so old habits fade out. At least [MIN_COMPLETIONS] are needed, i.e. 3 gaps.
 * - The typical gap is the median of the gaps between consecutive completions, so a single long
 *   break (a holiday, being ill) doesn't change the result.
 * - It is expressed in the chore's unit ([DAYS_PER_WEEK], [DAYS_PER_MONTH]) and rounded to a whole
 *   number, at least 1.
 * - Suggested only when that differs from the current interval by at least [MIN_DIFFERENCE_SHARE]
 *   of the current interval and at least [MIN_DIFFERENCE_DAYS] days, so small wobbles are left alone.
 */
object IntervalSuggestions {
    const val MIN_COMPLETIONS = 4
    const val MAX_COMPLETIONS = 8
    const val MIN_DIFFERENCE_SHARE = 0.25
    const val MIN_DIFFERENCE_DAYS = 2.0
    const val DAYS_PER_WEEK = 7.0
    const val DAYS_PER_MONTH = 30.44

    /** The suggestion for [chore], done on [completionDates] (any order), or null if there is none. */
    fun suggest(chore: Chore, completionDates: List<LocalDate>): IntervalSuggestion? {
        val schedule = chore.schedule
        if (chore.paused || schedule.mode != ScheduleMode.FLEXIBLE) return null
        val unitDays = daysPerUnit(schedule.unit) ?: return null
        val gap = typicalGapDays(completionDates) ?: return null
        val suggested = (gap / unitDays).roundToInt().coerceIn(1, Schedule.INTERVAL_RANGE.last)
        if (suggested == schedule.interval) return null
        val currentDays = schedule.interval * unitDays
        val difference = abs(suggested * unitDays - currentDays)
        if (difference < currentDays * MIN_DIFFERENCE_SHARE || difference < MIN_DIFFERENCE_DAYS) return null
        return IntervalSuggestion(chore.id, suggested, schedule.unit)
    }

    /**
     * Median gap in days between consecutive days with a completion, over the last
     * [MAX_COMPLETIONS] of them; null with fewer than [MIN_COMPLETIONS].
     */
    fun typicalGapDays(completionDates: List<LocalDate>): Double? {
        val dates = completionDates.distinct().sorted().takeLast(MAX_COMPLETIONS)
        if (dates.size < MIN_COMPLETIONS) return null
        val gaps = dates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) }.sorted()
        val middle = gaps.size / 2
        return if (gaps.size % 2 == 1) gaps[middle].toDouble() else (gaps[middle - 1] + gaps[middle]) / 2.0
    }

    private fun daysPerUnit(unit: RepeatUnit): Double? = when (unit) {
        RepeatUnit.DAYS -> 1.0
        RepeatUnit.WEEKS -> DAYS_PER_WEEK
        RepeatUnit.MONTHS -> DAYS_PER_MONTH
        RepeatUnit.NONE, RepeatUnit.DATES -> null
    }
}
