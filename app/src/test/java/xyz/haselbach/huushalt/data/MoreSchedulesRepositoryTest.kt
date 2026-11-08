package xyz.haselbach.huushalt.data

import java.time.DayOfWeek
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
import xyz.haselbach.huushalt.domain.MissedPeriod
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

/** Date lists, nth weekday and tolerance are stored; skipping and completing earlier (F2.10 to F2.12, F4.3, F4.5). */
@RunWith(RobolectricTestRunner::class)
class MoreSchedulesRepositoryTest {
    private val repo = (RuntimeEnvironment.getApplication() as HuushaltApplication).repository
    private fun d(text: String) = LocalDate.parse(text)
    private suspend fun chore(id: Long) = repo.chores.first().single { it.id == id }

    @Test
    fun `date list is stored and moves from date to date`() = runBlocking {
        val dates = listOf(d("2026-11-25"), d("2026-11-11"))
        val id = repo.save(null, "Papier", "", Schedule(ScheduleMode.FIXED, RepeatUnit.DATES, dates = dates), d("2026-11-08"))
        assertEquals(listOf(d("2026-11-11"), d("2026-11-25")), chore(id).schedule.dates)
        assertEquals(d("2026-11-11"), chore(id).dueDate)

        repo.complete(id, d("2026-11-11"))
        assertEquals(d("2026-11-25"), chore(id).dueDate)
        repo.complete(id, d("2026-11-25"))
        assertNull("No dates left", chore(id).dueDate)
    }

    @Test
    fun `nth weekday and tolerance are stored`() = runBlocking {
        val schedule = Schedule(ScheduleMode.FIXED, RepeatUnit.MONTHS, 1, setOf(DayOfWeek.TUESDAY), weekOfMonth = 2)
        val id = repo.save(null, "Boiler", "", schedule, d("2026-11-10"), toleranceDays = 3)
        assertEquals(2, chore(id).schedule.weekOfMonth)
        assertEquals(3, chore(id).toleranceDays)
        repo.complete(id, d("2026-11-10"))
        assertEquals(d("2026-12-08"), chore(id).dueDate)
    }

    @Test
    fun `done earlier counts from that day (F4_3)`() = runBlocking {
        val id = repo.save(null, "Staubsaugen", "", Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1), d("2026-11-10"))
        repo.complete(id, d("2026-11-08"))
        assertEquals(d("2026-11-15"), chore(id).dueDate)
        assertEquals(d("2026-11-08"), chore(id).lastDone)
    }

    @Test
    fun `skipping moves on without a completion (F4_5)`() = runBlocking {
        val tuesdays = Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 1, setOf(DayOfWeek.TUESDAY))
        val id = repo.save(null, "Kehricht", "", tuesdays, d("2026-11-10"))
        repo.skip(id, today = d("2026-11-09"))
        assertEquals(d("2026-11-17"), chore(id).dueDate)
        assertTrue(repo.completions(id).first().isEmpty())
        assertTrue(repo.missedPeriods.first().isEmpty())

        // Skipping while overdue keeps the overdue days for the streak.
        repo.skip(id, today = d("2026-11-19"))
        assertEquals(d("2026-11-24"), chore(id).dueDate)
        assertEquals(listOf(MissedPeriod(id, d("2026-11-17"), d("2026-11-19"))), repo.missedPeriods.first())
    }

    @Test
    fun `rescheduling leaves chores within their tolerance window alone`() = runBlocking {
        val weekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)
        val id = repo.save(null, "Pflanzen", "", weekly, d("2026-11-10"), toleranceDays = 3)
        repo.rescheduleOverdue(today = d("2026-11-12"))
        assertEquals(d("2026-11-10"), chore(id).dueDate)
    }
}
