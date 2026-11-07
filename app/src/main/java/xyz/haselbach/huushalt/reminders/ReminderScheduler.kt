package xyz.haselbach.huushalt.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import java.time.ZonedDateTime
import xyz.haselbach.huushalt.settings.ReminderSettings

/**
 * Schedules the daily reminders with AlarmManager. The alarms are inexact but allowed while idle,
 * which is accurate to a few minutes and needs no special permission. Each alarm schedules the
 * next one when it fires; boot, time changes and app updates reschedule everything (N8).
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun reschedule(settings: ReminderSettings, now: ZonedDateTime = ZonedDateTime.now()) {
        schedule(ReminderKind.MORNING, settings.enabled, settings.morningTime, now)
        schedule(ReminderKind.EVENING, settings.enabled && settings.eveningEnabled, settings.eveningTime, now)
    }

    private fun schedule(kind: ReminderKind, enabled: Boolean, time: java.time.LocalTime, now: ZonedDateTime) {
        val intent = pendingIntent(kind)
        if (!enabled) {
            alarmManager.cancel(intent)
            return
        }
        val trigger = ReminderTimes.nextTrigger(now, time)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toInstant().toEpochMilli(), intent)
    }

    private fun pendingIntent(kind: ReminderKind): PendingIntent = PendingIntent.getBroadcast(
        context,
        kind.ordinal,
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMINDER)
            // Distinct data per kind: intents that differ only in extras count as the same alarm.
            .setData("huushalt://reminder/${kind.name.lowercase()}".toUri())
            .putExtra(ReminderReceiver.EXTRA_KIND, kind.name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
