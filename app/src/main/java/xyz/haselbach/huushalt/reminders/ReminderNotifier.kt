package xyz.haselbach.huushalt.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import xyz.haselbach.huushalt.MainActivity
import xyz.haselbach.huushalt.R

/** Builds and shows the reminder notification. Morning and evening share one notification. */
class ReminderNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Notifications are allowed: runtime permission on Android 13+ and not blocked in the system settings. */
    fun canNotify(): Boolean = manager.areNotificationsEnabled() &&
        (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )

    fun show(kind: ReminderKind, open: OpenChores) {
        if (open.isEmpty || !canNotify()) return
        val res = context.resources
        val total = open.all.size
        val title = when {
            kind == ReminderKind.EVENING -> res.getQuantityString(R.plurals.notification_evening_title, total, total)
            open.overdue.isNotEmpty() && open.dueToday.isNotEmpty() ->
                context.getString(R.string.notification_morning_title_mixed, open.dueToday.size, open.overdue.size)
            open.overdue.isNotEmpty() ->
                res.getQuantityString(R.plurals.notification_morning_title_overdue, open.overdue.size, open.overdue.size)
            else -> res.getQuantityString(R.plurals.notification_morning_title_today, total, total)
        }
        val names = open.all.map { it.name }
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        names.take(MAX_LINES).forEach(style::addLine)
        if (names.size > MAX_LINES) style.setSummaryText(context.getString(R.string.notification_more, names.size - MAX_LINES))

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(names.joinToString(", "))
            .setStyle(style)
            .setContentIntent(openTodayIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        // Mark done from the notification when it is about exactly one chore (F5.4). Not with an
        // unfinished checklist: that needs the "done anyway?" question in the app (F1.8).
        open.all.singleOrNull()?.takeIf { !it.checklistUnfinished }?.let { chore ->
            builder.addAction(0, context.getString(R.string.action_complete), completeIntent(chore.id))
        }

        try {
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked between the check and notify(); nothing to show then.
        }
    }

    fun cancel() = manager.cancel(NOTIFICATION_ID)

    /** Tapping the notification opens the daily overview (F5.3). */
    private fun openTodayIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_TODAY)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun completeIntent(choreId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_COMPLETE,
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_COMPLETE)
            .putExtra(ReminderReceiver.EXTRA_CHORE_ID, choreId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_ID = "reminders"
        const val NOTIFICATION_ID = 1
        private const val REQUEST_COMPLETE = 100
        private const val MAX_LINES = 6
    }
}
