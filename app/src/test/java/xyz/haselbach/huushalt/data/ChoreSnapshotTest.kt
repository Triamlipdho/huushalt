package xyz.haselbach.huushalt.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

/** Any change can be undone by restoring a snapshot taken before it. */
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
        val snapshot = repo.snapshot(listOf(id))

        repo.delete(id)
        assertNull(chore(id))

        repo.restore(snapshot)
        assertEquals(before, chore(id))
        assertEquals(listOf("WC"), repo.checklist(id).first().map { it.text })
        assertEquals(1, repo.completions(id).first().size)
    }

    @Test
    fun `skip and postpone are undone, including the missed days`() = runBlocking {
        val id = repo.save(null, "Staubsaugen", "", weekly, today.minusDays(5))
        val before = chore(id)
        val missedBefore = repo.missedPeriods.first().count { it.choreId == id }

        val snapshot = repo.snapshot(listOf(id))
        repo.skip(id)
        repo.postpone(id, today.plusDays(20))
        repo.restore(snapshot)

        assertEquals(before, chore(id))
        assertEquals(missedBefore, repo.missedPeriods.first().count { it.choreId == id })
    }

    @Test
    fun `a room deleted in the meantime is left out`() = runBlocking {
        val area = repo.addArea("Keller")
        val id = repo.save(null, "Keller lüften", "", weekly, today, area)
        val snapshot = repo.snapshot(listOf(id))
        repo.delete(id)
        repo.deleteArea(area)

        repo.restore(snapshot)
        assertNull(chore(id)!!.areaId)
    }

    @Test
    fun `reschedule overdue is undone for all chores`() = runBlocking {
        val a = repo.save(null, "A", "", weekly, today.minusDays(3))
        val b = repo.save(null, "B", "", weekly, today.minusDays(9))
        val before = listOf(chore(a), chore(b))

        val snapshot = repo.snapshotOverdue(today)
        repo.rescheduleOverdue(today)
        repo.restore(snapshot)

        assertEquals(before, listOf(chore(a), chore(b)))
    }
}
