package xyz.haselbach.huushalt.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Holiday mode from [start] to [end], both inclusive (F10.1). */
data class Holiday(
    val id: Long,
    val start: LocalDate,
    val end: LocalDate,
    /** Chores were already moved after the holiday ended (F10.2). */
    val applied: Boolean,
) {
    val lengthInDays: Long get() = ChronoUnit.DAYS.between(start, end) + 1

    operator fun contains(date: LocalDate): Boolean = date in start..end

    fun isActive(today: LocalDate) = today in this
    fun isPlanned(today: LocalDate) = start > today
    fun isOver(today: LocalDate) = end < today

    /** Days of this holiday up to and including [until]. */
    fun daysUntil(until: LocalDate): Sequence<LocalDate> =
        generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end && it <= until }
}

object HolidayRules {
    /**
     * New due date of a chore after [holiday] is over (F10.2), or the current one if nothing changes:
     * - flexible chores move back by the length of the holiday, unless they were done during it
     *   anyway or were already overdue before it started
     * - fixed chores skip occurrences inside the holiday and continue with the next regular date
     * - one-off chores due during the holiday become due on the day after it
     */
    fun dueDateAfterHoliday(chore: Chore, holiday: Holiday): LocalDate? {
        val due = chore.dueDate ?: return null
        val returnDay = holiday.end.plusDays(1)
        val schedule = chore.schedule
        return when {
            !schedule.isRepeating -> if (due in holiday) returnDay else due
            schedule.mode == ScheduleMode.FLEXIBLE -> {
                val doneDuringHoliday = chore.lastDone != null && chore.lastDone >= holiday.start
                if (due >= holiday.start && !doneDuringHoliday) due.plusDays(holiday.lengthInDays) else due
            }
            else ->
                if (due in holiday) ScheduleCalculator.firstOccurrenceOnOrAfter(schedule, chore.anchorDate, returnDay) else due
        }
    }

    /**
     * Next due date when catching up with "Neu planen ab heute" (F10.6): flexible chores count
     * from today, fixed chores jump to their next regular date (today or later), one-off chores
     * are due today.
     */
    fun dueDateWhenRescheduled(chore: Chore, today: LocalDate): LocalDate {
        val schedule = chore.schedule
        return when {
            !schedule.isRepeating -> today
            schedule.mode == ScheduleMode.FLEXIBLE ->
                ScheduleCalculator.dueDateAfterCompletion(schedule, chore.anchorDate, chore.dueDate, today) ?: today
            else -> ScheduleCalculator.firstOccurrenceOnOrAfter(schedule, chore.anchorDate, today)
        }
    }
}
