package xyz.haselbach.huushalt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY startDate")
    fun observeHolidays(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays ORDER BY startDate")
    suspend fun getHolidays(): List<HolidayEntity>

    @Insert
    suspend fun insert(holiday: HolidayEntity): Long

    @Update
    suspend fun update(holiday: HolidayEntity)

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun delete(id: Long)
}
