package xyz.haselbach.huushalt.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Chore(
    val id: Long,
    val name: String,
    val notes: String,
    val schedule: Schedule,
    /** Start of the series of a fixed schedule; set whenever the next due date is set in the editor. */
    val anchorDate: LocalDate,
    /** Next due date; null for a one-off chore that is done. */
    val dueDate: LocalDate?,
    val lastDone: LocalDate?,
    /** Day the chore was created; streaks start with the first chore (F7.2). */
    val createdDate: LocalDate = anchorDate,
) {
    val isFinished: Boolean get() = dueDate == null

    fun status(today: LocalDate): DueStatus {
        val due = dueDate ?: return DueStatus.Finished
        return when {
            due < today -> DueStatus.Overdue(
                days = ChronoUnit.DAYS.between(due, today).toInt(),
                missed = ScheduleCalculator.missedOccurrences(schedule, anchorDate, due, today),
            )
            due == today -> DueStatus.Today
            due <= today.plusDays(UPCOMING_DAYS) -> DueStatus.Upcoming(due)
            else -> DueStatus.Later(due)
        }
    }

    companion object {
        /** The home screen shows chores due in the next 7 days as upcoming (F3.1). */
        const val UPCOMING_DAYS = 7L
    }
}

sealed interface DueStatus {
    data class Overdue(val days: Int, val missed: Int) : DueStatus
    data object Today : DueStatus
    data class Upcoming(val date: LocalDate) : DueStatus
    data class Later(val date: LocalDate) : DueStatus
    data object Finished : DueStatus
}
