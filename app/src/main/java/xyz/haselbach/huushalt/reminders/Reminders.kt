package xyz.haselbach.huushalt.reminders

import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import xyz.haselbach.huushalt.data.ChoreRepository
import xyz.haselbach.huushalt.settings.SettingsRepository

/** What happens when a reminder alarm fires or the notification's "Erledigt" button is tapped. */
class Reminders(
    private val chores: ChoreRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
) {
    suspend fun onAlarm(kind: ReminderKind, now: ZonedDateTime = ZonedDateTime.now()) {
        val today = now.toLocalDate()
        // The first reminder after a holiday must already see the moved chores (F10.2).
        chores.applyFinishedHolidays(today)
        val reminderSettings = settings.current().reminders
        val onHoliday = chores.holidays.first().any { it.isActive(today) }
        val enabled = reminderSettings.enabled && (kind == ReminderKind.MORNING || reminderSettings.eveningEnabled)
        if (onHoliday) {
            notifier.cancel() // No reminders during holidays (F10.1).
        } else if (enabled) {
            val open = OpenChores.of(chores.chores.first(), now.toLocalDate())
            if (open.isEmpty) notifier.cancel() else notifier.show(kind, open)
        }
        // Schedule the next one, a minute ahead so a slightly early alarm can't fire twice.
        scheduler.reschedule(reminderSettings, now.plusMinutes(1))
    }

    suspend fun onComplete(choreId: Long, today: LocalDate = LocalDate.now()) {
        chores.complete(choreId, today)
        notifier.cancel()
    }

    suspend fun rescheduleAll() = scheduler.reschedule(settings.current().reminders)
}
