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
        SELECT chores.*, (SELECT MAX(doneDate) FROM completions WHERE completions.choreId = chores.id) AS lastDone
        FROM chores
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observeChores(): Flow<List<ChoreWithLastDone>>

    @Query("SELECT * FROM completions WHERE choreId = :choreId ORDER BY doneDate DESC, id DESC")
    fun observeCompletions(choreId: Long): Flow<List<CompletionEntity>>

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
}
