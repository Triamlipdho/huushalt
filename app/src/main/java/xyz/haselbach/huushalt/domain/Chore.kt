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
    val areaId: Long? = null,
    val areaName: String? = null,
    /** Paused chores are not due and don't count for streaks (F1.3). */
    val paused: Boolean = false,
    val checklistTotal: Int = 0,
    val checklistDone: Int = 0,
    /** Days after the due date that still count as on time, e.g. "±2 Tage" (F2.12). */
    val toleranceDays: Int = 0,
) {
    val isFinished: Boolean get() = dueDate == null

    /** Due today or within the tolerance window: belongs to today's list. */
    fun isDueNow(today: LocalDate): Boolean = status(today).let { it == DueStatus.Today || it is DueStatus.InWindow }

    /** Some checklist items are not ticked yet (F1.8). */
    val checklistUnfinished: Boolean get() = checklistDone < checklistTotal

    fun status(today: LocalDate): DueStatus {
        if (paused) return DueStatus.Paused
        val due = dueDate ?: return DueStatus.Finished
        return when {
            due < today && today <= due.plusDays(toleranceDays.toLong()) ->
                DueStatus.InWindow(ChronoUnit.DAYS.between(due, today).toInt())
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

    /** Past the due date but within its tolerance window: still "fällig", not overdue (F2.12). */
    data class InWindow(val daysSinceDue: Int) : DueStatus
    data class Upcoming(val date: LocalDate) : DueStatus
    data class Later(val date: LocalDate) : DueStatus
    data object Finished : DueStatus
    data object Paused : DueStatus
}

/** A room or area (F1.4). */
data class Area(val id: Long, val name: String)

data class ChecklistItem(val id: Long, val text: String, val checked: Boolean)
