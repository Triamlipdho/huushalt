package xyz.haselbach.huushalt.data

import androidx.room.withTransaction
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import xyz.haselbach.huushalt.domain.RepeatUnit
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

    companion object {
        /** Format version; files from newer app versions are refused. */
        const val FORMAT = 1
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

    /** Replaces all data with [backup], or changes nothing if it fails. */
    suspend fun restore(backup: Backup) = db.withTransaction {
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

    /** Reads a backup file completely before anything is changed. */
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
        )
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
    )

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optDate(key: String): LocalDate? = if (!has(key) || isNull(key)) null else LocalDate.parse(getString(key))

    private fun JSONObject.optIntOrNull(key: String): Int? = if (!has(key) || isNull(key)) null else getInt(key)
}
