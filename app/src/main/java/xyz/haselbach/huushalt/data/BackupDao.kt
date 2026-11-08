package xyz.haselbach.huushalt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Bulk access for export, import and deleting all data (F9.3, F9.6). */
@Dao
interface BackupDao {
    @Query("SELECT * FROM areas") suspend fun areas(): List<AreaEntity>
    @Query("SELECT * FROM chores") suspend fun chores(): List<ChoreEntity>
    @Query("SELECT * FROM chore_dates") suspend fun dates(): List<ChoreDateEntity>
    @Query("SELECT * FROM checklist_items") suspend fun checklist(): List<ChecklistItemEntity>
    @Query("SELECT * FROM completions") suspend fun completions(): List<CompletionEntity>
    @Query("SELECT * FROM holidays") suspend fun holidays(): List<HolidayEntity>
    @Query("SELECT * FROM missed_periods") suspend fun missedPeriods(): List<MissedPeriodEntity>

    @Insert suspend fun insertAreas(items: List<AreaEntity>)
    @Insert suspend fun insertChores(items: List<ChoreEntity>)
    @Insert suspend fun insertDates(items: List<ChoreDateEntity>)
    @Insert suspend fun insertChecklist(items: List<ChecklistItemEntity>)
    @Insert suspend fun insertCompletions(items: List<CompletionEntity>)
    @Insert suspend fun insertHolidays(items: List<HolidayEntity>)
    @Insert suspend fun insertMissedPeriods(items: List<MissedPeriodEntity>)

    @Query("DELETE FROM chores") suspend fun deleteChores()
    @Query("DELETE FROM areas") suspend fun deleteAreas()
    @Query("DELETE FROM holidays") suspend fun deleteHolidays()
}
