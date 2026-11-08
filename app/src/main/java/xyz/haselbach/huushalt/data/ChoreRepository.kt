package xyz.haselbach.huushalt.data

import androidx.room.withTransaction
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleCalculator
import xyz.haselbach.huushalt.domain.ScheduleMode
import xyz.haselbach.huushalt.domain.RepeatUnit

class ChoreRepository(
    private val db: HuushaltDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.choreDao()

    val chores: Flow<List<Chore>> = dao.observeChores().map { list -> list.map { it.toDomain() } }

    fun completions(choreId: Long): Flow<List<CompletionEntity>> = dao.observeCompletions(choreId)

    /** Everything ever done, newest first (F6.3, F7). */
    val allCompletions: Flow<List<CompletionWithName>> = dao.observeAllCompletions()

    fun choreIdsDoneOn(date: LocalDate): Flow<Set<Long>> = dao.observeChoreIdsDoneOn(date).map { it.toSet() }

    /**
     * Creates a chore (id null) or updates one. [nextDue] is the date picked in the editor; it
     * becomes the anchor of a fixed schedule, and the due date is recalculated from it.
     */
    suspend fun save(id: Long?, name: String, notes: String, schedule: Schedule, nextDue: LocalDate): Long {
        val normalized = normalize(schedule, nextDue)
        val entity = ChoreEntity(
            id = id ?: 0,
            name = name.trim(),
            notes = notes.trim(),
            mode = normalized.mode,
            unit = normalized.unit,
            interval = normalized.interval,
            weekdays = normalized.weekdays.toBitmask(),
            dayOfMonth = normalized.dayOfMonth,
            anchorDate = nextDue,
            dueDate = ScheduleCalculator.initialDueDate(normalized, nextDue),
            createdAt = now(),
        )
        return db.withTransaction {
            if (id == null) {
                dao.insertChore(entity)
            } else {
                val existing = dao.getChore(id) ?: return@withTransaction id
                dao.updateChore(entity.copy(createdAt = existing.createdAt))
                id
            }
        }
    }

    /** Marks a chore as done on [doneDate]. Returns the id of the new completion, for undo. */
    suspend fun complete(choreId: Long, doneDate: LocalDate): Long? = db.withTransaction {
        val chore = dao.getChore(choreId) ?: return@withTransaction null
        val nextDue = ScheduleCalculator.dueDateAfterCompletion(chore.schedule(), chore.anchorDate, chore.dueDate, doneDate)
        val completionId = dao.insertCompletion(
            CompletionEntity(choreId = choreId, doneDate = doneDate, previousDueDate = chore.dueDate, createdAt = now()),
        )
        dao.updateDueDate(choreId, nextDue)
        completionId
    }

    /** Undoes a completion: removes it and restores the due date from before (F4.2). */
    suspend fun undoCompletion(completionId: Long) = db.withTransaction {
        val completion = dao.getCompletion(completionId) ?: return@withTransaction
        dao.deleteCompletion(completionId)
        dao.updateDueDate(completion.choreId, completion.previousDueDate)
    }

    /**
     * Moves a past completion to another day (F6.4). If it is the chore's latest completion, the
     * next due date is recalculated as if the chore had been done on [doneDate].
     */
    suspend fun changeCompletionDate(completionId: Long, doneDate: LocalDate) = db.withTransaction {
        val completion = dao.getCompletion(completionId) ?: return@withTransaction
        val chore = dao.getChore(completion.choreId) ?: return@withTransaction
        val isLatest = dao.getLatestCompletion(chore.id)?.id == completionId
        dao.updateCompletionDate(completionId, doneDate)
        if (isLatest) {
            val nextDue = ScheduleCalculator.dueDateAfterCompletion(
                chore.schedule(),
                chore.anchorDate,
                completion.previousDueDate,
                doneDate,
            )
            dao.updateDueDate(chore.id, nextDue)
        }
    }

    /**
     * Deletes a past completion (F6.4). Deleting the latest one works like undo and restores the
     * due date from before; older ones are only removed from the history.
     */
    suspend fun deleteCompletion(completionId: Long) = db.withTransaction {
        val completion = dao.getCompletion(completionId) ?: return@withTransaction
        val isLatest = dao.getLatestCompletion(completion.choreId)?.id == completionId
        dao.deleteCompletion(completionId)
        if (isLatest) dao.updateDueDate(completion.choreId, completion.previousDueDate)
    }

    /** Moves the next due date (F4.4). */
    suspend fun postpone(choreId: Long, date: LocalDate) = dao.updateDueDate(choreId, date)

    suspend fun delete(choreId: Long) = dao.deleteChore(choreId)

    /** Drops fields that don't apply to the schedule, so stored data stays consistent. */
    private fun normalize(schedule: Schedule, anchor: LocalDate): Schedule = when {
        !schedule.isRepeating -> Schedule.ONCE
        schedule.mode == ScheduleMode.FLEXIBLE -> schedule.copy(weekdays = emptySet(), dayOfMonth = null)
        schedule.unit == RepeatUnit.WEEKS -> schedule.copy(dayOfMonth = null)
        schedule.unit == RepeatUnit.MONTHS -> schedule.copy(weekdays = emptySet(), dayOfMonth = anchor.dayOfMonth)
        else -> schedule.copy(weekdays = emptySet(), dayOfMonth = null)
    }
}
