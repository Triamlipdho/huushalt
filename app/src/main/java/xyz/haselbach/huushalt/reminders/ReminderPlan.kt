package xyz.haselbach.huushalt.reminders

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.DueStatus

enum class ReminderKind {
    /** Daily overview in the morning (F5.1). */
    MORNING,

    /** Evening reminder, only while chores are still open (F5.7). */
    EVENING,
}

/** Chores a reminder is about: overdue and due today, overdue first. */
data class OpenChores(val overdue: List<Chore>, val dueToday: List<Chore>) {
    val all: List<Chore> get() = overdue + dueToday
    val isEmpty: Boolean get() = all.isEmpty()

    companion object {
        fun of(chores: List<Chore>, today: LocalDate): OpenChores {
            val order = compareBy<Chore> { it.dueDate }.thenBy { it.name.lowercase() }
            return OpenChores(
                overdue = chores.filter { it.status(today) is DueStatus.Overdue }.sortedWith(order),
                dueToday = chores.filter { it.isDueNow(today) }.sortedWith(order),
            )
        }
    }
}

object ReminderTimes {
    /**
     * Next time a daily reminder at [time] fires, strictly after [now]. If [time] doesn't exist on
     * that day because of the switch to daylight saving time, it moves to the first time after the
     * gap (e.g. 02:30 becomes 03:30), as [java.time.LocalDateTime.atZone] does.
     */
    fun nextTrigger(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        var day = now.toLocalDate()
        while (true) {
            val candidate = day.atTime(time).atZone(now.zone)
            if (candidate.isAfter(now)) return candidate
            day = day.plusDays(1)
        }
    }
}
