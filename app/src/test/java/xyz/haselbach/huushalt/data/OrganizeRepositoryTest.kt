package xyz.haselbach.huushalt.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

/** Rooms, checklists and pausing (F1.3, F1.4, F1.7). */
@RunWith(RobolectricTestRunner::class)
class OrganizeRepositoryTest {
    private val repo = (RuntimeEnvironment.getApplication() as HuushaltApplication).repository
    private val weekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)
    private fun d(text: String) = LocalDate.parse(text)

    private suspend fun chore(id: Long) = repo.chores.first().single { it.id == id }

    @Test
    fun `checklist keeps ticks of kept items and resets when the chore is done`() = runBlocking {
        val id = repo.save(null, "Bad putzen", "", weekly, d("2026-11-10"), checklist = listOf(ChecklistDraft(null, "Lavabo"), ChecklistDraft(null, "WC")))
        val (lavabo, wc) = repo.checklist(id).first()
        repo.setChecked(lavabo.id, true)
        assertEquals(1, chore(id).checklistDone)
        assertTrue(chore(id).checklistUnfinished)

        // Edit: rename WC, add Dusche, keep Lavabo with its tick.
        repo.save(id, "Bad putzen", "", weekly, d("2026-11-10"), checklist = listOf(
            ChecklistDraft(lavabo.id, "Lavabo"),
            ChecklistDraft(wc.id, "WC und Boden"),
            ChecklistDraft(null, "Dusche"),
        ))
        assertEquals(listOf("Lavabo" to true, "WC und Boden" to false, "Dusche" to false), repo.checklist(id).first().map { it.text to it.checked })

        repo.complete(id, d("2026-11-10"))
        assertTrue(repo.checklist(id).first().none { it.checked })
        assertEquals(3, chore(id).checklistTotal)

        // Removing items from the list deletes them.
        repo.save(id, "Bad putzen", "", weekly, d("2026-11-17"), checklist = emptyList())
        assertTrue(repo.checklist(id).first().isEmpty())
    }

    @Test
    fun `rooms are shown on chores and deleting one keeps its chores`() = runBlocking {
        val kitchen = repo.addArea("Küche")
        val id = repo.save(null, "Kühlschrank putzen", "", weekly, d("2026-11-10"), areaId = kitchen)
        assertEquals("Küche", chore(id).areaName)

        repo.renameArea(kitchen, "Kochnische")
        assertEquals("Kochnische", chore(id).areaName)

        repo.deleteArea(kitchen)
        assertNull(chore(id).areaId)
        assertTrue(repo.areas.first().isEmpty())
    }

    @Test
    fun `paused chores are not rescheduled and are due today when resumed late`() = runBlocking {
        val id = repo.save(null, "Fenster", "", weekly, d("2026-11-01"))
        repo.setPaused(id, true)
        repo.rescheduleOverdue(today = d("2026-11-10"))
        assertEquals(d("2026-11-01"), chore(id).dueDate)
        assertTrue(chore(id).paused)

        repo.setPaused(id, false, today = d("2026-11-10"))
        assertEquals(d("2026-11-10"), chore(id).dueDate)
        assertTrue(repo.missedPeriods.first().isEmpty())
    }
}
