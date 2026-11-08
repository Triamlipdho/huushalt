package xyz.haselbach.huushalt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ChoreDao {
    @Query(
        """
        SELECT $CHORE_COLUMNS
        FROM chores LEFT JOIN areas ON areas.id = chores.areaId
        ORDER BY chores.name COLLATE NOCASE
        """,
    )
    fun observeChores(): Flow<List<ChoreWithLastDone>>

    @Query("SELECT * FROM completions WHERE choreId = :choreId ORDER BY doneDate DESC, id DESC")
    fun observeCompletions(choreId: Long): Flow<List<CompletionEntity>>

    @Query(
        """
        SELECT completions.*, chores.name AS choreName
        FROM completions JOIN chores ON chores.id = completions.choreId
        ORDER BY completions.doneDate DESC, completions.id DESC
        """,
    )
    fun observeAllCompletions(): Flow<List<CompletionWithName>>

    @Query("SELECT * FROM completions WHERE choreId = :choreId ORDER BY doneDate DESC, id DESC LIMIT 1")
    suspend fun getLatestCompletion(choreId: Long): CompletionEntity?

    @Query("UPDATE completions SET doneDate = :doneDate WHERE id = :id")
    suspend fun updateCompletionDate(id: Long, doneDate: LocalDate)

    @Query("SELECT DISTINCT choreId FROM completions WHERE doneDate = :date")
    fun observeChoreIdsDoneOn(date: LocalDate): Flow<List<Long>>

    @Query("SELECT * FROM chores WHERE id = :id")
    suspend fun getChore(id: Long): ChoreEntity?

    @Query("SELECT * FROM completions WHERE id = :id")
    suspend fun getCompletion(id: Long): CompletionEntity?

    @Insert
    suspend fun insertChore(chore: ChoreEntity): Long

    @Update
    suspend fun updateChore(chore: ChoreEntity)

    @Query("UPDATE chores SET dueDate = :dueDate WHERE id = :id")
    suspend fun updateDueDate(id: Long, dueDate: LocalDate?)

    @Query("DELETE FROM chores WHERE id = :id")
    suspend fun deleteChore(id: Long)

    @Insert
    suspend fun insertCompletion(completion: CompletionEntity): Long

    @Query("DELETE FROM completions WHERE id = :id")
    suspend fun deleteCompletion(id: Long)

    @Query("SELECT * FROM missed_periods")
    fun observeMissedPeriods(): Flow<List<MissedPeriodEntity>>

    @Insert
    suspend fun insertMissedPeriod(period: MissedPeriodEntity)

    @Query("SELECT $CHORE_COLUMNS FROM chores LEFT JOIN areas ON areas.id = chores.areaId")
    suspend fun getChores(): List<ChoreWithLastDone>

    @Query("UPDATE chores SET paused = :paused WHERE id = :id")
    suspend fun updatePaused(id: Long, paused: Boolean)

    // Date lists (F2.11)

    @Query("SELECT date FROM chore_dates WHERE choreId = :choreId ORDER BY date")
    suspend fun getDates(choreId: Long): List<LocalDate>

    @Query("DELETE FROM chore_dates WHERE choreId = :choreId")
    suspend fun deleteDates(choreId: Long)

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertDates(dates: List<ChoreDateEntity>)

    // Areas (F1.4)

    @Query("SELECT * FROM areas ORDER BY name COLLATE NOCASE")
    fun observeAreas(): Flow<List<AreaEntity>>

    @Insert
    suspend fun insertArea(area: AreaEntity): Long

    @Query("UPDATE areas SET name = :name WHERE id = :id")
    suspend fun renameArea(id: Long, name: String)

    @Query("DELETE FROM areas WHERE id = :id")
    suspend fun deleteArea(id: Long)

    // Checklists (F1.7)

    @Query("SELECT * FROM checklist_items WHERE choreId = :choreId ORDER BY position")
    fun observeChecklist(choreId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE choreId = :choreId ORDER BY position")
    suspend fun getChecklist(choreId: Long): List<ChecklistItemEntity>

    @Insert
    suspend fun insertChecklistItem(item: ChecklistItemEntity): Long

    @Update
    suspend fun updateChecklistItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItem(id: Long)

    @Query("UPDATE checklist_items SET checked = :checked WHERE id = :id")
    suspend fun setChecklistItemChecked(id: Long, checked: Boolean)

    @Query("UPDATE checklist_items SET checked = 0 WHERE choreId = :choreId")
    suspend fun resetChecklist(choreId: Long)

    companion object {
        const val CHORE_COLUMNS = """
            chores.*,
            (SELECT MAX(doneDate) FROM completions WHERE completions.choreId = chores.id) AS lastDone,
            areas.name AS areaName,
            (SELECT COUNT(*) FROM checklist_items WHERE checklist_items.choreId = chores.id) AS checklistTotal,
            (SELECT COUNT(*) FROM checklist_items WHERE checklist_items.choreId = chores.id AND checked = 1) AS checklistDone,
            (SELECT GROUP_CONCAT(date) FROM chore_dates WHERE chore_dates.choreId = chores.id) AS dateList
        """
    }
}
