package xyz.haselbach.huushalt.data

import androidx.room.withTransaction
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneOffset
import org.json.JSONArray
import org.json.JSONObject
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

/** Everything stored about chores, as written to and read from a backup file (F9.3). */
data class Backup(
    val areas: List<AreaEntity>,
    val chores: List<ChoreEntity>,
    val dates: List<ChoreDateEntity>,
    val checklist: List<ChecklistItemEntity>,
    val completions: List<CompletionEntity>,
    val holidays: List<HolidayEntity>,
    val missedPeriods: List<MissedPeriodEntity>,
) {
    class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /** The file is larger than [MAX_FILE_BYTES]; it was not read completely. */
    class TooLargeException : Exception("Backup file too large")

    /**
     * Checks that the backup can be stored and calculated with safely: valid schedules, dates in a
     * sane range and references that point to existing rows. Throws [InvalidBackupException] for
     * the first problem, so a crafted or damaged file is refused as a whole.
     */
    fun validate() {
        fun fail(message: String): Nothing = throw InvalidBackupException(message)
        fun check(ok: Boolean, message: () -> String) { if (!ok) fail(message()) }
        fun checkDate(date: LocalDate?, what: String) = check(date == null || date in DATE_RANGE) { "$what out of range: $date" }
        fun checkMillis(millis: Long, what: String) = check(millis in MILLIS_RANGE) { "$what out of range: $millis" }
        fun checkIds(ids: List<Long>, what: String) {
            check(ids.all { it > 0 }) { "$what: ids must be positive" }
            check(ids.toSet().size == ids.size) { "$what: duplicate ids" }
        }

        checkIds(areas.map { it.id }, "areas")
        checkIds(chores.map { it.id }, "chores")
        checkIds(dates.map { it.id }, "dates")
        checkIds(checklist.map { it.id }, "checklist")
        checkIds(completions.map { it.id }, "completions")
        checkIds(holidays.map { it.id }, "holidays")
        checkIds(missedPeriods.map { it.id }, "missedPeriods")

        val areaIds = areas.map { it.id }.toSet()
        val choreIds = chores.map { it.id }.toSet()
        fun checkChore(choreId: Long, what: String) = check(choreId in choreIds) { "$what points to missing chore $choreId" }

        dates.forEach {
            checkChore(it.choreId, "date ${it.id}")
            checkDate(it.date, "date ${it.id}")
        }
        check(dates.map { it.choreId to it.date }.toSet().size == dates.size) { "duplicate chore dates" }
        val datesByChore = dates.groupBy({ it.choreId }, { it.date })

        chores.forEach { chore ->
            val what = "chore ${chore.id}"
            check(chore.schedule(datesByChore[chore.id].orEmpty()).validate() == null) { "$what: invalid schedule" }
            check(chore.weekdays in 0..ALL_WEEKDAYS) { "$what: invalid weekdays" }
            check(chore.dayOfMonth == null || chore.dayOfMonth in 1..31) { "$what: invalid day of month" }
            check(chore.weekOfMonth == null || chore.weekOfMonth in 1..4 || chore.weekOfMonth == Schedule.LAST_WEEK) { "$what: invalid week of month" }
            check(chore.toleranceDays in 0..MAX_TOLERANCE_DAYS) { "$what: invalid tolerance" }
            check(chore.areaId == null || chore.areaId in areaIds) { "$what points to missing area ${chore.areaId}" }
            checkDate(chore.anchorDate, "$what anchor date")
            checkDate(chore.dueDate, "$what due date")
            checkMillis(chore.createdAt, "$what createdAt")
        }
        checklist.forEach { checkChore(it.choreId, "checklist item ${it.id}") }
        completions.forEach {
            checkChore(it.choreId, "completion ${it.id}")
            checkDate(it.doneDate, "completion ${it.id}")
            checkDate(it.previousDueDate, "completion ${it.id} previous due date")
            checkMillis(it.createdAt, "completion ${it.id} createdAt")
        }
        holidays.forEach {
            checkDate(it.startDate, "holiday ${it.id} start")
            checkDate(it.endDate, "holiday ${it.id} end")
            check(it.startDate <= it.endDate) { "holiday ${it.id} ends before it starts" }
            checkMillis(it.createdAt, "holiday ${it.id} createdAt")
        }
        missedPeriods.forEach {
            checkChore(it.choreId, "missed period ${it.id}")
            checkDate(it.fromDate, "missed period ${it.id} from")
            checkDate(it.untilDate, "missed period ${it.id} until")
            check(it.fromDate <= it.untilDate) { "missed period ${it.id} ends before it starts" }
        }
    }

    companion object {
        /** Format version; files from newer app versions are refused. */
        const val FORMAT = 1

        /** Backup files are a few hundred KB at most; larger files are refused before parsing. */
        const val MAX_FILE_BYTES = 5 * 1024 * 1024

        /** Dates outside this range are refused, so date calculations stay small and can't overflow. */
        val DATE_RANGE: ClosedRange<LocalDate> = LocalDate.of(1900, 1, 1)..LocalDate.of(2200, 12, 31)
        private val MILLIS_RANGE = DATE_RANGE.start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()..
            DATE_RANGE.endInclusive.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        private const val ALL_WEEKDAYS = 0b1111111

        /**
         * Reads [input] as UTF-8 text, at most [maxBytes]. Throws [TooLargeException] without
         * reading further if the file is larger.
         */
        fun readText(input: InputStream, maxBytes: Int = MAX_FILE_BYTES): String {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (out.size().toLong() + read > maxBytes) throw TooLargeException()
                out.write(buffer, 0, read)
            }
            return out.toString(Charsets.UTF_8.name())
        }
    }
}

/** Export, import and deleting all data (F9.3, F9.6). Settings are not part of it. */
class BackupStore(private val db: HuushaltDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.backupDao()

    suspend fun read(): Backup = db.withTransaction {
        Backup(
            areas = dao.areas(),
            chores = dao.chores(),
            dates = dao.dates(),
            checklist = dao.checklist(),
            completions = dao.completions(),
            holidays = dao.holidays(),
            missedPeriods = dao.missedPeriods(),
        )
    }

    /** Replaces all data with [backup], or changes nothing if it fails or [backup] is invalid. */
    suspend fun restore(backup: Backup) = db.withTransaction {
        backup.validate()
        deleteAllInTransaction()
        dao.insertAreas(backup.areas)
        dao.insertChores(backup.chores)
        dao.insertDates(backup.dates)
        dao.insertChecklist(backup.checklist)
        dao.insertCompletions(backup.completions)
        dao.insertHolidays(backup.holidays)
        dao.insertMissedPeriods(backup.missedPeriods)
    }

    /** Deletes all chores, their history, rooms and holidays (F9.6). */
    suspend fun deleteAll() = db.withTransaction { deleteAllInTransaction() }

    private suspend fun deleteAllInTransaction() {
        // Completions, checklists, dates and missed periods go with their chores (cascade).
        dao.deleteChores()
        dao.deleteAreas()
        dao.deleteHolidays()
    }

    fun toJson(backup: Backup): String = JSONObject().apply {
        put("app", "huushalt")
        put("format", Backup.FORMAT)
        put("exportedAt", now())
        put("areas", JSONArray(backup.areas.map { JSONObject().put("id", it.id).put("name", it.name) }))
        put("chores", JSONArray(backup.chores.map { it.toJson() }))
        put("dates", JSONArray(backup.dates.map { JSONObject().put("id", it.id).put("choreId", it.choreId).put("date", it.date.toString()) }))
        put(
            "checklist",
            JSONArray(
                backup.checklist.map {
                    JSONObject().put("id", it.id).put("choreId", it.choreId).put("text", it.text).put("position", it.position).put("checked", it.checked)
                },
            ),
        )
        put(
            "completions",
            JSONArray(
                backup.completions.map {
                    JSONObject().put("id", it.id).put("choreId", it.choreId).put("doneDate", it.doneDate.toString())
                        .put("previousDueDate", it.previousDueDate?.toString() ?: JSONObject.NULL).put("createdAt", it.createdAt)
                },
            ),
        )
        put(
            "holidays",
            JSONArray(
                backup.holidays.map {
                    JSONObject().put("id", it.id).put("startDate", it.startDate.toString()).put("endDate", it.endDate.toString())
                        .put("applied", it.applied).put("createdAt", it.createdAt)
                },
            ),
        )
        put(
            "missedPeriods",
            JSONArray(
                backup.missedPeriods.map {
                    JSONObject().put("id", it.id).put("choreId", it.choreId).put("fromDate", it.fromDate.toString()).put("untilDate", it.untilDate.toString())
                },
            ),
        )
    }.toString(2)

    /** Reads and validates a backup file completely before anything is changed. */
    fun fromJson(text: String): Backup = try {
        val root = JSONObject(text)
        if (root.optString("app") != "huushalt") throw Backup.InvalidBackupException("Not a huushalt backup")
        if (root.getInt("format") > Backup.FORMAT) throw Backup.InvalidBackupException("Backup from a newer app version")
        Backup(
            areas = root.getJSONArray("areas").objects().map { AreaEntity(it.getLong("id"), it.getString("name")) },
            chores = root.getJSONArray("chores").objects().map { it.toChore() },
            dates = root.getJSONArray("dates").objects().map { ChoreDateEntity(it.getLong("id"), it.getLong("choreId"), LocalDate.parse(it.getString("date"))) },
            checklist = root.getJSONArray("checklist").objects().map {
                ChecklistItemEntity(it.getLong("id"), it.getLong("choreId"), it.getString("text"), it.getInt("position"), it.getBoolean("checked"))
            },
            completions = root.getJSONArray("completions").objects().map {
                CompletionEntity(it.getLong("id"), it.getLong("choreId"), LocalDate.parse(it.getString("doneDate")), it.optDate("previousDueDate"), it.getLong("createdAt"))
            },
            holidays = root.getJSONArray("holidays").objects().map {
                HolidayEntity(it.getLong("id"), LocalDate.parse(it.getString("startDate")), LocalDate.parse(it.getString("endDate")), it.getBoolean("applied"), it.getLong("createdAt"))
            },
            missedPeriods = root.getJSONArray("missedPeriods").objects().map {
                MissedPeriodEntity(it.getLong("id"), it.getLong("choreId"), LocalDate.parse(it.getString("fromDate")), LocalDate.parse(it.getString("untilDate")))
            },
        ).also { it.validate() }
    } catch (e: Backup.InvalidBackupException) {
        throw e
    } catch (e: Exception) {
        throw Backup.InvalidBackupException("Unreadable backup", e)
    }

    private fun ChoreEntity.toJson() = JSONObject()
        .put("id", id).put("name", name).put("notes", notes).put("mode", mode.name).put("unit", unit.name)
        .put("interval", interval).put("weekdays", weekdays).put("dayOfMonth", dayOfMonth ?: JSONObject.NULL)
        .put("anchorDate", anchorDate.toString()).put("dueDate", dueDate?.toString() ?: JSONObject.NULL)
        .put("createdAt", createdAt).put("areaId", areaId ?: JSONObject.NULL).put("paused", paused)
        .put("weekOfMonth", weekOfMonth ?: JSONObject.NULL).put("toleranceDays", toleranceDays)
        .put("symbol", symbol ?: JSONObject.NULL).put("color", color ?: JSONObject.NULL)

    private fun JSONObject.toChore() = ChoreEntity(
        id = getLong("id"),
        name = getString("name"),
        notes = optString("notes", ""),
        mode = ScheduleMode.valueOf(getString("mode")),
        unit = RepeatUnit.valueOf(getString("unit")),
        interval = getInt("interval"),
        weekdays = getInt("weekdays"),
        dayOfMonth = optIntOrNull("dayOfMonth"),
        anchorDate = LocalDate.parse(getString("anchorDate")),
        dueDate = optDate("dueDate"),
        createdAt = getLong("createdAt"),
        areaId = if (isNull("areaId") || !has("areaId")) null else getLong("areaId"),
        paused = optBoolean("paused", false),
        weekOfMonth = optIntOrNull("weekOfMonth"),
        toleranceDays = optInt("toleranceDays", 0),
        symbol = optStringOrNull("symbol"),
        color = optStringOrNull("color"),
    )

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optDate(key: String): LocalDate? = if (!has(key) || isNull(key)) null else LocalDate.parse(getString(key))

    private fun JSONObject.optStringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)

    private fun JSONObject.optIntOrNull(key: String): Int? = if (!has(key) || isNull(key)) null else getInt(key)
}
