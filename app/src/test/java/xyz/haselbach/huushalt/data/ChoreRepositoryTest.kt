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
}
