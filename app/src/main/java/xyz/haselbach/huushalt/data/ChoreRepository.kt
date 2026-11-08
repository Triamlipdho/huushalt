package xyz.haselbach.huushalt.data

import androidx.room.withTransaction
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.haselbach.huushalt.domain.Area
import xyz.haselbach.huushalt.domain.ChecklistItem
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.Holiday
import xyz.haselbach.huushalt.domain.HolidayRules
import xyz.haselbach.huushalt.domain.MissedPeriod
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleCalculator
import xyz.haselbach.huushalt.domain.ScheduleMode
import xyz.haselbach.huushalt.domain.RepeatUnit

class ChoreRepository(
    private val db: HuushaltDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.choreDao()
    private val holidayDao = db.holidayDao()

    val holidays: Flow<List<Holiday>> = holidayDao.observeHolidays().map { list -> list.map { it.toDomain() } }

    val missedPeriods: Flow<List<MissedPeriod>> = dao.observeMissedPeriods().map { list -> list.map { it.toDomain() } }

    val chores: Flow<List<Chore>> = dao.observeChores().map { list -> list.map { it.toDomain() } }

    fun completions(choreId: Long): Flow<List<CompletionEntity>> = dao.observeCompletions(choreId)

    /** Everything ever done, newest first (F6.3, F7). */
    val allCompletions: Flow<List<CompletionWithName>> = dao.observeAllCompletions()

    fun choreIdsDoneOn(date: LocalDate): Flow<Set<Long>> = dao.observeChoreIdsDoneOn(date).map { it.toSet() }

    /**
     * Creates a chore (id null) or updates one. [nextDue] is the date picked in the editor; it
     * becomes the anchor of a fixed schedule, and the due date is recalculated from it.
     */
    suspend fun save(
        id: Long?,
        name: String,
        notes: String,
        schedule: Schedule,
        nextDue: LocalDate,
        areaId: Long? = null,
        /** Checklist in order; items with an id are kept (with their ticks), others are new (F1.7). */
        checklist: List<ChecklistDraft>? = null,
    ): Long {
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
            areaId = areaId,
        )
        return db.withTransaction {
            val choreId = if (id == null) {
                dao.insertChore(entity)
            } else {
                val existing = dao.getChore(id) ?: return@withTransaction id
                dao.updateChore(entity.copy(createdAt = existing.createdAt, paused = existing.paused))
                id
            }
            if (checklist != null) saveChecklist(choreId, checklist)
            choreId
        }
    }

    private suspend fun saveChecklist(choreId: Long, drafts: List<ChecklistDraft>) {
        val existing = dao.getChecklist(choreId).associateBy { it.id }
        val keep = drafts.mapNotNull { it.id }.toSet()
        existing.keys.filter { it !in keep }.forEach { dao.deleteChecklistItem(it) }
        drafts.filter { it.text.isNotBlank() }.forEachIndexed { position, draft ->
            val old = draft.id?.let { existing[it] }
            if (old != null) {
                dao.updateChecklistItem(old.copy(text = draft.text.trim(), position = position))
            } else {
                dao.insertChecklistItem(ChecklistItemEntity(choreId = choreId, text = draft.text.trim(), position = position, checked = false))
            }
        }
        // Items emptied in the editor are removed.
        drafts.filter { it.text.isBlank() && it.id != null }.forEach { dao.deleteChecklistItem(it.id!!) }
    }

    fun checklist(choreId: Long): Flow<List<ChecklistItem>> = dao.observeChecklist(choreId).map { list -> list.map { it.toDomain() } }

    suspend fun setChecked(itemId: Long, checked: Boolean) = dao.setChecklistItemChecked(itemId, checked)

    // Areas (F1.4)

    val areas: Flow<List<Area>> = dao.observeAreas().map { list -> list.map { it.toDomain() } }

    suspend fun addArea(name: String): Long = dao.insertArea(AreaEntity(name = name.trim()))

    suspend fun renameArea(id: Long, name: String) = dao.renameArea(id, name.trim())

    /** Chores in the room keep existing, without a room. */
    suspend fun deleteArea(id: Long) = dao.deleteArea(id)

    /**
     * Pauses or resumes a chore (F1.3). Paused chores are not due and don't count for streaks;
     * a chore resumed after its due date is due today, without counting the pause as missed.
     */
    suspend fun setPaused(choreId: Long, paused: Boolean, today: LocalDate = LocalDate.now()) = db.withTransaction {
        val chore = dao.getChore(choreId) ?: return@withTransaction
        dao.updatePaused(choreId, paused)
        if (!paused && chore.dueDate != null && chore.dueDate < today) dao.updateDueDate(choreId, today)
    }

    /** Marks a chore as done on [doneDate]. Returns the id of the new completion, for undo. */
    suspend fun complete(choreId: Long, doneDate: LocalDate): Long? = db.withTransaction {
        val chore = dao.getChore(choreId) ?: return@withTransaction null
        val nextDue = ScheduleCalculator.dueDateAfterCompletion(chore.schedule(), chore.anchorDate, chore.dueDate, doneDate)
        val completionId = dao.insertCompletion(
            CompletionEntity(choreId = choreId, doneDate = doneDate, previousDueDate = chore.dueDate, createdAt = now()),
        )
        dao.updateDueDate(choreId, nextDue)
        // The checklist starts fresh for the next time (F1.7).
        dao.resetChecklist(choreId)
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

    /**
     * Moves the next due date (F4.4). Postponing an overdue chore keeps its overdue days for the
     * streak; postponing before the due date doesn't count as missed (F7.6).
     */
    suspend fun postpone(choreId: Long, date: LocalDate, today: LocalDate = LocalDate.now()) = db.withTransaction {
        val chore = dao.getChore(choreId) ?: return@withTransaction
        recordMissed(chore, today)
        dao.updateDueDate(choreId, date)
    }

    /** "Neu planen ab heute" (F10.6): moves all overdue chores without marking them done. */
    suspend fun rescheduleOverdue(today: LocalDate = LocalDate.now()) = db.withTransaction {
        dao.getChores().map { it.toDomain() to it.chore }
            .filter { (chore, _) -> !chore.paused && chore.dueDate != null && chore.dueDate < today }
            .forEach { (chore, entity) ->
                recordMissed(entity, today)
                dao.updateDueDate(chore.id, HolidayRules.dueDateWhenRescheduled(chore, today))
            }
    }

    private suspend fun recordMissed(chore: ChoreEntity, today: LocalDate) {
        val due = chore.dueDate ?: return
        if (due < today) dao.insertMissedPeriod(MissedPeriodEntity(choreId = chore.id, fromDate = due, untilDate = today))
    }

    /** Plans a holiday (id null) or changes one (F10.1, F10.4). */
    suspend fun saveHoliday(id: Long?, start: LocalDate, end: LocalDate, today: LocalDate = LocalDate.now()) {
        db.withTransaction {
            if (id == null) {
                holidayDao.insert(HolidayEntity(startDate = start, endDate = end, applied = false, createdAt = now()))
            } else {
                val existing = holidayDao.getHolidays().find { it.id == id } ?: return@withTransaction
                holidayDao.update(existing.copy(startDate = start, endDate = end))
            }
        }
        applyFinishedHolidays(today)
    }

    /** Ends an active holiday: yesterday was its last day; started today means it never happened (F10.4). */
    suspend fun endHoliday(id: Long, today: LocalDate = LocalDate.now()) {
        val holiday = holidayDao.getHolidays().find { it.id == id } ?: return
        if (holiday.startDate >= today) {
            holidayDao.delete(id)
        } else {
            holidayDao.update(holiday.copy(endDate = minOf(holiday.endDate, today.minusDays(1))))
            applyFinishedHolidays(today)
        }
    }

    suspend fun deleteHoliday(id: Long) = holidayDao.delete(id)

    /**
     * Moves chores once a holiday is over (F10.2). Runs whenever the app or a reminder sees a new
     * day, so it doesn't matter whether the app was opened during the holiday.
     */
    suspend fun applyFinishedHolidays(today: LocalDate = LocalDate.now()) = db.withTransaction {
        holidayDao.getHolidays()
            .filter { !it.applied && it.endDate < today }
            .forEach { holiday ->
                val domain = holiday.toDomain()
                dao.getChores().forEach { row ->
                    val chore = row.toDomain()
                    val newDue = HolidayRules.dueDateAfterHoliday(chore, domain)
                    if (newDue != chore.dueDate) dao.updateDueDate(chore.id, newDue)
                }
                holidayDao.update(holiday.copy(applied = true))
            }
    }

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

/** A checklist item as edited in the editor: [id] null for new items. */
data class ChecklistDraft(val id: Long?, val text: String)
