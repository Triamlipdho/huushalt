package xyz.haselbach.huushalt.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

/** Editing and deleting past completions recalculates the due date (F6.4). */
@RunWith(RobolectricTestRunner::class)
class ChoreRepositoryTest {
    private val repo = (RuntimeEnvironment.getApplication() as HuushaltApplication).repository
    private val weekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)
    private val day = LocalDate.parse("2026-11-02")

    private suspend fun due(id: Long) = repo.chores.first().single { it.id == id }.dueDate

    @Test
    fun `changing the latest completion date moves the next due date`() = runBlocking {
        val id = repo.save(null, "Staubsaugen", "", weekly, day)
        val completion = repo.complete(id, day)!!
        assertEquals(day.plusWeeks(1), due(id))

        repo.changeCompletionDate(completion, day.plusDays(2))
        assertEquals(day.plusDays(2).plusWeeks(1), due(id))
        assertEquals(day.plusDays(2), repo.completions(id).first().single().doneDate)
    }

    @Test
    fun `changing an older completion keeps the due date`() = runBlocking {
        val id = repo.save(null, "Staubsaugen", "", weekly, day)
        val first = repo.complete(id, day)!!
        repo.complete(id, day.plusWeeks(1))
        val dueBefore = due(id)

        repo.changeCompletionDate(first, day.minusDays(1))
        assertEquals(dueBefore, due(id))
    }

    @Test
    fun `deleting the latest completion restores the due date, older ones only leave the history`() = runBlocking {
        val id = repo.save(null, "Staubsaugen", "", weekly, day)
        val first = repo.complete(id, day)!!
        val second = repo.complete(id, day.plusWeeks(1))!!
        assertEquals(day.plusWeeks(2), due(id))

        repo.deleteCompletion(first)
        assertEquals(day.plusWeeks(2), due(id))

        repo.deleteCompletion(second)
        assertEquals(day.plusWeeks(1), due(id))
        assertEquals(emptyList<CompletionEntity>(), repo.completions(id).first())
    }

    @Test
    fun `a shorter interval never makes a chore overdue in the past`() = runBlocking {
        val today = LocalDate.now()
        val fourWeeks = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 4)
        val id = repo.save(null, "Kühlschrank putzen", "", fourWeeks, today.minusDays(25))
        repo.complete(id, today.minusDays(25))

        // Counted from the last time it would be 4 days ago; it is due today instead.
        repo.setInterval(id, 3, today)
        assertEquals(today, due(id))
    }

    @Test
    fun `ticking the last checklist item twice completes the chore once`() = runBlocking {
        val today = LocalDate.now()
        val id = repo.save(null, "Bad putzen", "", weekly, today, checklist = listOf(ChecklistDraft(null, "WC")))
        val item = repo.checklist(id).first().single()

        val first = repo.tickChecklistItem(id, item.id, today)
        val second = repo.tickChecklistItem(id, item.id, today)
        assertEquals(1, repo.completions(id).first().size)
        assertEquals(null, second)
        assertEquals(true, first != null)
    }
}
