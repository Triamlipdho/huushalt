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
import xyz.haselbach.huushalt.domain.Area
import xyz.haselbach.huushalt.domain.ChecklistItem
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.ChoreLook
import xyz.haselbach.huushalt.domain.CompletionRecord
import xyz.haselbach.huushalt.domain.Holiday
import xyz.haselbach.huushalt.domain.MissedPeriod
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

@Entity(
    tableName = "chores",
    foreignKeys = [
        ForeignKey(
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("areaId")],
)
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
    /** Room or area (F1.4). */
    @ColumnInfo(defaultValue = "NULL") val areaId: Long? = null,
    /** Paused chores are not due and don't count for streaks (F1.3). */
    @ColumnInfo(defaultValue = "0") val paused: Boolean = false,
    /** Nth weekday of the month, see [Schedule.weekOfMonth] (F2.10). */
    @ColumnInfo(defaultValue = "NULL") val weekOfMonth: Int? = null,
    /** Days after the due date that still count as on time (F2.12). */
    @ColumnInfo(defaultValue = "0") val toleranceDays: Int = 0,
    /** Chosen symbol ([ChoreSymbol] name); null: guessed from the name (F1.5). */
    @ColumnInfo(defaultValue = "NULL") val symbol: String? = null,
    /** Chosen colour ([ChoreColor] name); null: the symbol's default colour (F1.5). */
    @ColumnInfo(defaultValue = "NULL") val color: String? = null,
)

/** One date of a chore with a list of dates (F2.11). */
@Entity(
    tableName = "chore_dates",
    foreignKeys = [
        ForeignKey(
            entity = ChoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["choreId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["choreId", "date"], unique = true)],
)
data class ChoreDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val choreId: Long,
    val date: LocalDate,
)

/** A room or area such as Küche or Bad (F1.4). */
@Entity(tableName = "areas")
data class AreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/** One sub-step of a chore's checklist (F1.7). [checked] resets when the chore is done. */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = ChoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["choreId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("choreId")],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val choreId: Long,
    val text: String,
    val position: Int,
    val checked: Boolean,
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

/** Holiday mode (F10.1). */
@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate,
    /** Chores were moved after the holiday (F10.2). */
    val applied: Boolean,
    val createdAt: Long,
)

/**
 * A chore was overdue from [fromDate] until the day before [untilDate] and then moved without
 * being done (postponed or rescheduled), so streaks still know about it (F7.2).
 */
@Entity(
    tableName = "missed_periods",
    foreignKeys = [
        ForeignKey(
            entity = ChoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["choreId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("choreId")],
)
data class MissedPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val choreId: Long,
    val fromDate: LocalDate,
    val untilDate: LocalDate,
)

fun HolidayEntity.toDomain() = Holiday(id, startDate, endDate, applied)

fun MissedPeriodEntity.toDomain() = MissedPeriod(choreId, fromDate, untilDate)

data class ChoreWithLastDone(
    @Embedded val chore: ChoreEntity,
    @ColumnInfo(name = "lastDone") val lastDone: LocalDate?,
    @ColumnInfo(name = "areaName") val areaName: String? = null,
    @ColumnInfo(name = "checklistTotal") val checklistTotal: Int = 0,
    @ColumnInfo(name = "checklistDone") val checklistDone: Int = 0,
    /** Dates of a date list, comma separated epoch days (F2.11). */
    @ColumnInfo(name = "dateList") val dateList: String? = null,
)

fun ChoreWithLastDone.toDomain() = Chore(
    id = chore.id,
    name = chore.name,
    notes = chore.notes,
    schedule = chore.schedule(dates()),
    anchorDate = chore.anchorDate,
    dueDate = chore.dueDate,
    lastDone = lastDone,
    createdDate = Instant.ofEpochMilli(chore.createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
    areaId = chore.areaId,
    areaName = areaName,
    paused = chore.paused,
    checklistTotal = checklistTotal,
    checklistDone = checklistDone,
    toleranceDays = chore.toleranceDays,
    symbolKey = chore.symbol,
    colorKey = chore.color,
)

fun ChoreWithLastDone.dates(): List<LocalDate> =
    dateList?.split(',')?.filter { it.isNotBlank() }?.map { LocalDate.ofEpochDay(it.trim().toLong()) }?.sorted().orEmpty()

fun AreaEntity.toDomain() = Area(id, name)

fun ChecklistItemEntity.toDomain() = ChecklistItem(id, text, checked)

/** A completion with the name of its chore, for the activity log (F6.3). */
data class CompletionWithName(
    @Embedded val completion: CompletionEntity,
    @ColumnInfo(name = "choreName") val choreName: String,
    @ColumnInfo(name = "choreSymbol") val choreSymbol: String? = null,
    @ColumnInfo(name = "choreColor") val choreColor: String? = null,
) {
    val look: ChoreLook get() = ChoreLook.of(choreName, choreSymbol, choreColor)
}

fun CompletionEntity.toRecord(toleranceDays: Int = 0) = CompletionRecord(choreId, doneDate, previousDueDate, toleranceDays)

fun ChoreEntity.schedule(dates: List<LocalDate> = emptyList()) = Schedule(
    mode = mode,
    unit = unit,
    interval = interval,
    weekdays = DayOfWeek.entries.filter { weekdays and (1 shl (it.value - 1)) != 0 }.toSet(),
    dayOfMonth = dayOfMonth,
    weekOfMonth = weekOfMonth,
    dates = dates,
)

fun Set<DayOfWeek>.toBitmask(): Int = fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }
