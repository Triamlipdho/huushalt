package xyz.haselbach.huushalt.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.CompletionRecord
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

@Entity(tableName = "chores")
data class ChoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String,
    val mode: ScheduleMode,
    val unit: RepeatUnit,
    val interval: Int,
    /** Bit i set = weekday with value i + 1 (Monday = bit 0). */
    val weekdays: Int,
    val dayOfMonth: Int?,
    val anchorDate: LocalDate,
    val dueDate: LocalDate?,
    val createdAt: Long,
)

@Entity(
    tableName = "completions",
    foreignKeys = [
        ForeignKey(
            entity = ChoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["choreId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("choreId"), Index("doneDate")],
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val choreId: Long,
    val doneDate: LocalDate,
    /** Due date before this completion, so the completion can be undone. */
    val previousDueDate: LocalDate?,
    val createdAt: Long,
)

data class ChoreWithLastDone(
    @Embedded val chore: ChoreEntity,
    @ColumnInfo(name = "lastDone") val lastDone: LocalDate?,
)

fun ChoreWithLastDone.toDomain() = Chore(
    id = chore.id,
    name = chore.name,
    notes = chore.notes,
    schedule = chore.schedule(),
    anchorDate = chore.anchorDate,
    dueDate = chore.dueDate,
    lastDone = lastDone,
    createdDate = Instant.ofEpochMilli(chore.createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
)

/** A completion with the name of its chore, for the activity log (F6.3). */
data class CompletionWithName(
    @Embedded val completion: CompletionEntity,
    @ColumnInfo(name = "choreName") val choreName: String,
)

fun CompletionEntity.toRecord() = CompletionRecord(choreId, doneDate, previousDueDate)

fun ChoreEntity.schedule() = Schedule(
    mode = mode,
    unit = unit,
    interval = interval,
    weekdays = DayOfWeek.entries.filter { weekdays and (1 shl (it.value - 1)) != 0 }.toSet(),
    dayOfMonth = dayOfMonth,
)

fun Set<DayOfWeek>.toBitmask(): Int = fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }
