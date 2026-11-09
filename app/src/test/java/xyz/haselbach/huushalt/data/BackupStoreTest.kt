package xyz.haselbach.huushalt.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.domain.ChoreCatalog
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

/** Export, import, deleting everything (F9.3, F9.6) and the starter set (F8.2). */
@RunWith(RobolectricTestRunner::class)
class BackupStoreTest {
    private val app = RuntimeEnvironment.getApplication() as HuushaltApplication
    private val repo = app.repository
    private val backups = app.backups
    private fun d(text: String) = LocalDate.parse(text)

    private suspend fun fillWithData() {
        val bath = repo.addArea("Bad")
        val id = repo.save(
            null, "Bad putzen", "Mit Essig", Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1), d("2026-11-10"),
            areaId = bath, checklist = listOf(ChecklistDraft(null, "Lavabo"), ChecklistDraft(null, "WC")), toleranceDays = 2,
        )
        repo.complete(id, d("2026-11-10"))
        repo.save(null, "Papier", "", Schedule(ScheduleMode.FIXED, RepeatUnit.DATES, dates = listOf(d("2026-12-01"), d("2026-12-15"))), d("2026-11-10"))
        repo.saveHoliday(null, d("2026-12-20"), d("2026-12-27"), today = d("2026-11-10"))
        repo.postpone(id, d("2026-11-30"), today = d("2026-11-25"))
    }

    @Test
    fun `export and import restore everything`() = runBlocking {
        fillWithData()
        val before = backups.read()
        val json = backups.toJson(before)

        backups.deleteAll()
        assertTrue(repo.chores.first().isEmpty())
        assertTrue(repo.areas.first().isEmpty())
        assertTrue(repo.holidays.first().isEmpty())

        backups.restore(backups.fromJson(json))
        assertEquals(before, backups.read())
        val bath = repo.chores.first().single { it.name == "Bad putzen" }
        assertEquals("Bad", bath.areaName)
        assertEquals(2, bath.checklistTotal)
        assertEquals(2, bath.toleranceDays)
        assertEquals(2, repo.chores.first().single { it.name == "Papier" }.schedule.dates.size)
    }

    @Test
    fun `invalid files are refused without changing anything`() = runBlocking {
        fillWithData()
        val before = backups.read()
        assertThrows(Backup.InvalidBackupException::class.java) { backups.fromJson("not json") }
        assertThrows(Backup.InvalidBackupException::class.java) { backups.fromJson("""{"app":"other","format":1}""") }
        assertThrows(Backup.InvalidBackupException::class.java) {
            backups.fromJson(backups.toJson(before).replace("\"format\": 1", "\"format\": 99"))
        }
        assertEquals(before, backups.read())
    }

    /** A valid export of [fillWithData], changed by [change]. */
    private suspend fun exportChanged(change: JSONObject.() -> Unit): String =
        JSONObject(backups.toJson(backups.read())).apply(change).toString()

    private fun JSONObject.chore(name: String): JSONObject {
        val chores = getJSONArray("chores")
        return (0 until chores.length()).map { chores.getJSONObject(it) }.single { it.getString("name") == name }
    }

    private fun JSONObject.first(array: String): JSONObject = getJSONArray(array).getJSONObject(0)

    /** Every change must make the whole file invalid, and nothing may be written. */
    private fun assertRefused(vararg changes: Pair<String, JSONObject.() -> Unit>) = runBlocking {
        fillWithData()
        val before = backups.read()
        for ((what, change) in changes) {
            val json = exportChanged(change)
            assertThrows(what, Backup.InvalidBackupException::class.java) { backups.fromJson(json) }
        }
        assertEquals(before, backups.read())
    }

    @Test
    fun `valid export passes validation and round-trips`() = runBlocking {
        fillWithData()
        val before = backups.read()
        val parsed = backups.fromJson(backups.toJson(before))
        assertEquals(before, parsed)
        parsed.validate()
        backups.restore(parsed)
        assertEquals(before, backups.read())
    }

    @Test
    fun `invalid repeat intervals are refused`() = assertRefused(
        "interval 0" to { chore("Bad putzen").put("interval", 0) },
        "negative interval" to { chore("Bad putzen").put("interval", -3) },
        "huge interval" to { chore("Bad putzen").put("interval", Int.MAX_VALUE) },
    )

    @Test
    fun `invalid monthly schedules are refused`() = assertRefused(
        "week of month with several weekdays" to {
            chore("Bad putzen").put("mode", "FIXED").put("unit", "MONTHS").put("weekOfMonth", 2).put("weekdays", 0b101)
        },
        "week of month without weekday" to {
            chore("Bad putzen").put("mode", "FIXED").put("unit", "MONTHS").put("weekOfMonth", 2).put("weekdays", 0)
        },
        "week of month 7" to {
            chore("Bad putzen").put("mode", "FIXED").put("unit", "MONTHS").put("weekOfMonth", 7).put("weekdays", 1)
        },
        "day of month 0" to { chore("Bad putzen").put("mode", "FIXED").put("unit", "MONTHS").put("dayOfMonth", 0) },
        "day of month 32" to { chore("Bad putzen").put("mode", "FIXED").put("unit", "MONTHS").put("dayOfMonth", 32) },
        "fixed weekly without weekday" to { chore("Bad putzen").put("mode", "FIXED").put("unit", "WEEKS").put("weekdays", 0) },
        "unknown weekday bits" to { chore("Bad putzen").put("weekdays", 1 shl 9) },
        "date list without dates" to { put("dates", org.json.JSONArray()) },
    )

    @Test
    fun `dates outside the sane range are refused`() = assertRefused(
        "anchor date" to { chore("Bad putzen").put("anchorDate", "+999999999-12-31") },
        "due date" to { chore("Bad putzen").put("dueDate", "1899-12-31") },
        "date list entry" to { first("dates").put("date", "2201-01-01") },
        "completion done date" to { first("completions").put("doneDate", "-999999999-01-01") },
        "completion previous due date" to { first("completions").put("previousDueDate", "+50000-01-01") },
        "holiday start" to { first("holidays").put("startDate", "0001-01-01") },
        "holiday end" to { first("holidays").put("endDate", "+999999999-12-31") },
        "missed period" to { first("missedPeriods").put("fromDate", "1000-01-01") },
        "chore createdAt" to { chore("Bad putzen").put("createdAt", Long.MIN_VALUE) },
        "completion createdAt" to { first("completions").put("createdAt", -100_000_000_000_000L) },
        "holiday createdAt" to { first("holidays").put("createdAt", Long.MAX_VALUE) },
    )

    @Test
    fun `holidays and missed periods must not end before they start`() = assertRefused(
        "holiday" to { first("holidays").put("startDate", "2026-12-28").put("endDate", "2026-12-20") },
        "missed period" to { first("missedPeriods").put("fromDate", "2026-12-01").put("untilDate", "2026-11-01") },
    )

    @Test
    fun `invalid tolerance is refused`() = assertRefused(
        "negative" to { chore("Bad putzen").put("toleranceDays", -1) },
        "too large" to { chore("Bad putzen").put("toleranceDays", 100_000) },
    )

    @Test
    fun `inconsistent ids and references are refused`() = assertRefused(
        "missing area" to { chore("Bad putzen").put("areaId", 999) },
        "checklist of missing chore" to { first("checklist").put("choreId", 999) },
        "date of missing chore" to { first("dates").put("choreId", 999) },
        "completion of missing chore" to { first("completions").put("choreId", 999) },
        "missed period of missing chore" to { first("missedPeriods").put("choreId", 999) },
        "duplicate chore id" to { chore("Papier").put("id", chore("Bad putzen").getLong("id")) },
        "id 0" to { first("areas").put("id", 0) },
    )

    @Test
    fun `a refused backup leaves the existing data unchanged`() = runBlocking {
        fillWithData()
        val before = backups.read()
        val invalid = before.copy(holidays = before.holidays.map { it.copy(startDate = it.endDate.plusDays(1)) })
        // restore() validates too, inside the transaction, before deleting anything.
        val error = runCatching { backups.restore(invalid) }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error is Backup.InvalidBackupException)
        assertEquals(before, backups.read())
    }

    @Test
    fun `starter set creates rooms once and spreads due dates`() = runBlocking {
        repo.addArea("bad")
        val picked = ChoreCatalog.templates.filter { it.name in setOf("Bad putzen", "Handtücher wechseln", "Staubsaugen") }
        repo.addFromTemplates(picked, today = d("2026-11-10"))

        assertEquals(listOf("bad"), repo.areas.first().map { it.name })
        val chores = repo.chores.first()
        assertEquals(3, chores.size)
        assertEquals(5, chores.single { it.name == "Bad putzen" }.checklistTotal)
        assertEquals(setOf(d("2026-11-10"), d("2026-11-11"), d("2026-11-12")), chores.mapNotNull { it.dueDate }.toSet())
    }
}
