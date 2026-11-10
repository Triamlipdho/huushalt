package xyz.haselbach.huushalt.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

/** Changes can be undone, but undo never overwrites what happened after the change. */
@RunWith(RobolectricTestRunner::class)
class ChoreSnapshotTest {
    private val repo = (RuntimeEnvironment.getApplication() as HuushaltApplication).repository
    private val weekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)
    private val today = LocalDate.now()

    private suspend fun chore(id: Long) = repo.chores.first().singleOrNull { it.id == id }

    @Test
    fun `a deleted chore comes back with its history, checklist and room`() = runBlocking {
        val area = repo.addArea("Bad")
        val id = repo.save(null, "Bad putzen", "Notiz", weekly, today, area, listOf(ChecklistDraft(null, "WC")))
        repo.complete(id, today.minusDays(3))
        val before = chore(id)

        val token = repo.changeUndoably(id) { repo.delete(id) }
        assertNull(chore(id))

        assertTrue(repo.undo(token))
        assertEquals(before, chore(id))
        assertEquals(listOf("WC"), repo.checklist(id).first().map { it.text })
        assertEquals(1, repo.completions(id).first().size)
    }

    @Test
    fun `postpone is undone, including the missed days`() = runBlocking {
        val id = repo.save(null, "Staubsaugen", "", weekly, today.minusDays(5))
        val before = chore(id)
        val missedBefore = repo.missedPeriods.first().count { it.choreId == id }

        val token = repo.changeUndoably(id) { repo.postpone(id, today.plusDays(20)) }
        assertTrue(repo.undo(token))

        assertEquals(before, chore(id))
        assertEquals(missedBefore, repo.missedPeriods.first().count { it.choreId == id })
    }

    @Test
    fun `undo is refused when the chore was changed again`() = runBlocking {
        val id = repo.save(null, "Staubsaugen", "", weekly, today)
        val token = repo.changeUndoably(id) { repo.skip(id) }
        // Done from the notification while the snackbar was still showing.
        repo.complete(id, today)
        val afterCompletion = chore(id)

        assertFalse(repo.undo(token))
        assertEquals(afterCompletion, chore(id))
        assertEquals(1, repo.completions(id).first().size)
    }

    @Test
    fun `undo is refused after the editor saved the chore`() = runBlocking {
        val id = repo.save(null, "Staubsaugen", "", weekly, today)
        val token = repo.changeUndoably(id) { repo.postpone(id, today.plusDays(3)) }
        repo.save(id, "Staubsaugen und feucht wischen", "", weekly, today.plusDays(3))

        assertFalse(repo.undo(token))
        assertEquals("Staubsaugen und feucht wischen", chore(id)!!.name)
    }

    @Test
    fun `a room deleted in the meantime is left out`() = runBlocking {
        val area = repo.addArea("Keller")
        val id = repo.save(null, "Keller lüften", "", weekly, today, area)
        val token = repo.changeUndoably(id) { repo.delete(id) }
        repo.deleteArea(area)

        assertTrue(repo.undo(token))
        assertNull(chore(id)!!.areaId)
    }

    @Test
    fun `reschedule overdue is undone for all chores`() = runBlocking {
        val a = repo.save(null, "A", "", weekly, today.minusDays(3))
        val b = repo.save(null, "B", "", weekly, today.minusDays(9))
        val before = listOf(chore(a), chore(b))

        val token = repo.rescheduleOverdueUndoably(today)
        assertEquals(setOf(a, b), token.choreIds.toSet())
        assertTrue(repo.undo(token))

        assertEquals(before, listOf(chore(a), chore(b)))
    }
}
