package xyz.haselbach.huushalt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ChoreEntity::class, CompletionEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class HuushaltDatabase : RoomDatabase() {
    abstract fun choreDao(): ChoreDao

    companion object {
        fun create(context: Context): HuushaltDatabase =
            Room.databaseBuilder(context, HuushaltDatabase::class.java, "huushalt.db").build()
    }
}
