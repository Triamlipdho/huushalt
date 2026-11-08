package xyz.haselbach.huushalt.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
