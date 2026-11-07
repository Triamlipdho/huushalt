package xyz.haselbach.huushalt.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

class ReminderPlanTest {
    private val zurich = ZoneId.of("Europe/Zurich")

    private fun at(text: String) = ZonedDateTime.of(java.time.LocalDateTime.parse(text), zurich)

    @Test
    fun `next trigger is later today when the time has not passed`() {
        assertEquals(at("2026-11-10T08:00"), ReminderTimes.nextTrigger(at("2026-11-10T07:30"), LocalTime.of(8, 0)))
    }

    @Test
    fun `next trigger is tomorrow when the time has passed or is now`() {
        assertEquals(at("2026-11-11T08:00"), ReminderTimes.nextTrigger(at("2026-11-10T08:00"), LocalTime.of(8, 0)))
        assertEquals(at("2026-11-11T08:00"), ReminderTimes.nextTrigger(at("2026-11-10T21:00"), LocalTime.of(8, 0)))
    }

    @Test
    fun `daylight saving time switch keeps the local time`() {
        // Clocks go forward on 2027-03-28 in Zurich; the reminder still fires at 08:00 local time.
        val next = ReminderTimes.nextTrigger(at("2027-03-27T09:00"), LocalTime.of(8, 0))
        assertEquals(at("2027-03-28T08:00"), next)
        assertEquals(2, next.offset.totalSeconds / 3600)
    }

    @Test
    fun `a time inside the daylight saving gap moves to after the gap`() {
        val next = ReminderTimes.nextTrigger(at("2027-03-27T09:00"), LocalTime.of(2, 30))
        assertEquals(LocalTime.of(3, 30), next.toLocalTime())
        assertTrue(next.isAfter(at("2027-03-27T09:00")))
    }

    @Test
    fun `open chores are overdue and due today, overdue first`() {
        val today = LocalDate.parse("2026-11-10")
        fun chore(id: Long, name: String, due: LocalDate?) =
            Chore(id, name, "", Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1), today, due, null)
        val open = OpenChores.of(
            listOf(
                chore(1, "Morgen", today.plusDays(1)),
                chore(2, "Heute B", today),
                chore(3, "Alt", today.minusDays(3)),
                chore(4, "Heute A", today),
                chore(5, "Fertig", null),
            ),
            today,
        )
        assertEquals(listOf("Alt", "Heute A", "Heute B"), open.all.map { it.name })
        assertEquals(1, open.overdue.size)
    }
}
