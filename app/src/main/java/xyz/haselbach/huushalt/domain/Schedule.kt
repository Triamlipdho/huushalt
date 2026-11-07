package xyz.haselbach.huushalt.domain

import java.time.DayOfWeek

/** How the next due date of a repeating chore is calculated (F2.5). */
enum class ScheduleMode {
    /** Due on set dates, regardless of when it was last done, e.g. every Tuesday. */
    FIXED,

    /** Next due date counts from the day the chore was actually done, e.g. 4 weeks after last time. */
    FLEXIBLE,
}

enum class RepeatUnit { NONE, DAYS, WEEKS, MONTHS }

/**
 * When a chore is due.
 *
 * - [RepeatUnit.NONE]: one-off chore (F2.1); [mode] and [interval] are ignored.
 * - [RepeatUnit.DAYS], [RepeatUnit.WEEKS], [RepeatUnit.MONTHS]: repeat every [interval] units (F2.2).
 * - [weekdays]: only for fixed weekly schedules, e.g. every 2nd week on Wednesday (F2.3, F2.9).
 * - [dayOfMonth]: only for fixed monthly schedules (F2.4). Months that are too short use their last day.
 */
data class Schedule(
    val mode: ScheduleMode,
    val unit: RepeatUnit,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int? = null,
) {
    val isRepeating: Boolean get() = unit != RepeatUnit.NONE

    /** Returns why this schedule can't be used, or null if it is valid. */
    fun validate(): ScheduleError? = when {
        !isRepeating -> null
        interval !in INTERVAL_RANGE -> ScheduleError.INTERVAL_OUT_OF_RANGE
        mode == ScheduleMode.FIXED && unit == RepeatUnit.WEEKS && weekdays.isEmpty() -> ScheduleError.NO_WEEKDAY
        mode == ScheduleMode.FIXED && unit == RepeatUnit.MONTHS && dayOfMonth !in 1..31 -> ScheduleError.INVALID_DAY_OF_MONTH
        else -> null
    }

    companion object {
        val INTERVAL_RANGE = 1..999
        val ONCE = Schedule(ScheduleMode.FIXED, RepeatUnit.NONE)
    }
}

enum class ScheduleError { INTERVAL_OUT_OF_RANGE, NO_WEEKDAY, INVALID_DAY_OF_MONTH }
