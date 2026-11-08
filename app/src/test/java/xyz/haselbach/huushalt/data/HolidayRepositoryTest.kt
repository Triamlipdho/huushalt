package xyz.haselbach.huushalt.data

import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

/** Holidays move chores once they are over (F10.2, F10.4); catching up and postponing (F10.6, F4.4). */
@RunWith(RobolectricTestRunner::class)
class HolidayRepositoryTest {
    private val repo = (RuntimeEnvironment.getApplication() as HuushaltApplication).repository
    private fun d(text: String) = LocalDate.parse(text)

    private val flexibleWeekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)
    private val everyTuesday = Schedule(ScheduleMode.FIXED, RepeatUnit.WEEKS, 1, setOf(DayOfWeek.TUESDAY))

    private suspend fun due(id: Long) = repo.chores.first().single { it.id == id }.dueDate

    @Test
    fun `chores move once the holiday is over, and only once`() = runBlocking {
        val flexible = repo.save(null, "Staubsaugen", "", flexibleWeekly, d("2026-11-10"))
        val fixed = repo.save(null, "Kehricht", "", everyTuesday, d("2026-11-10"))
        val once = repo.save(null, "Steuererklärung", "", Schedule.ONCE, d("2026-11-12"))
        // Holiday Monday 9th to Sunday 15th, planned on the 2nd.
        repo.saveHoliday(null, d("2026-11-09"), d("2026-11-15"), today = d("2026-11-02"))

        repo.applyFinishedHolidays(d("2026-11-12")) // still on holiday: nothing moves
        assertEquals(d("2026-11-10"), due(flexible))

        repo.applyFinishedHolidays(d("2026-11-16"))
        assertEquals(d("2026-11-17"), due(flexible))
        assertEquals(d("2026-11-17"), due(fixed))
        assertEquals(d("2026-11-16"), due(once))

        repo.applyFinishedHolidays(d("2026-11-17"))
        assertEquals("Applied only once", d("2026-11-17"), due(flexible))
    }

    @Test
    fun `ending a holiday early uses the real end`() = runBlocking {
        val flexible = repo.save(null, "Staubsaugen", "", flexibleWeekly, d("2026-11-10"))
        repo.saveHoliday(null, d("2026-11-09"), d("2026-11-22"), today = d("2026-11-02"))
        val holiday = repo.holidays.first().single()

        repo.endHoliday(holiday.id, today = d("2026-11-12")) // last holiday day: the 11th, 3 days
        assertEquals(d("2026-11-11"), repo.holidays.first().single().end)
        assertEquals(d("2026-11-13"), due(flexible))
    }

    @Test
    fun `ending a holiday on its first day deletes it`() = runBlocking {
        repo.saveHoliday(null, d("2026-11-09"), d("2026-11-15"), today = d("2026-11-09"))
        repo.endHoliday(repo.holidays.first().single().id, today = d("2026-11-09"))
        assertTrue(repo.holidays.first().isEmpty())
    }

    @Test
    fun `rescheduling moves overdue chores and remembers the missed days`() = runBlocking {
        val flexible = repo.save(null, "Staubsaugen", "", flexibleWeekly, d("2026-11-01"))
        val fixed = repo.save(null, "Kehricht", "", everyTuesday, d("2026-11-03"))
        val upcoming = repo.save(null, "Fenster", "", flexibleWeekly, d("2026-11-20"))

        repo.rescheduleOverdue(today = d("2026-11-12"))

        assertEquals(d("2026-11-19"), due(flexible))
        assertEquals(d("2026-11-17"), due(fixed))
        assertEquals(d("2026-11-20"), due(upcoming))
        assertEquals(
            setOf(MissedPeriod(flexible, d("2026-11-01"), d("2026-11-12")), MissedPeriod(fixed, d("2026-11-03"), d("2026-11-12"))),
            repo.missedPeriods.first().toSet(),
        )
        assertTrue(repo.chores.first().none { it.dueDate!! < d("2026-11-12") })
    }

    @Test
    fun `postponing counts as missed only when already overdue (F7_6)`() = runBlocking {
        val onTime = repo.save(null, "Staubsaugen", "", flexibleWeekly, d("2026-11-12"))
        val late = repo.save(null, "Fenster", "", flexibleWeekly, d("2026-11-10"))

        repo.postpone(onTime, d("2026-11-14"), today = d("2026-11-12"))
        repo.postpone(late, d("2026-11-14"), today = d("2026-11-12"))

        assertEquals(listOf(MissedPeriod(late, d("2026-11-10"), d("2026-11-12"))), repo.missedPeriods.first())
    }
}
