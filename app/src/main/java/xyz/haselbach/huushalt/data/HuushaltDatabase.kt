package xyz.haselbach.huushalt.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ChoreEntity::class,
        CompletionEntity::class,
        HolidayEntity::class,
        MissedPeriodEntity::class,
        AreaEntity::class,
        ChecklistItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // 2: holidays and missed periods (F10)
        AutoMigration(from = 1, to = 2),
        // 3: rooms, paused chores, checklists (F1.3, F1.4, F1.7)
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class HuushaltDatabase : RoomDatabase() {
    abstract fun choreDao(): ChoreDao

    abstract fun holidayDao(): HolidayDao

    companion object {
        const val NAME = "huushalt.db"

        fun create(context: Context): HuushaltDatabase =
            Room.databaseBuilder(context, HuushaltDatabase::class.java, NAME).build()
    }
}
