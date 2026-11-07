package xyz.haselbach.huushalt.data

import androidx.room.TypeConverter
import java.time.LocalDate

/** Dates are stored as epoch days: date based, independent of time zones (N7). */
class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}
