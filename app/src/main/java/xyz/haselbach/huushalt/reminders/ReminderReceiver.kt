package xyz.haselbach.huushalt.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import xyz.haselbach.huushalt.HuushaltApplication

/** Receives the reminder alarms and the "Erledigt" action from the notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HuushaltApplication
        val pending = goAsync()
        app.scope.launch {
            try {
                when (intent.action) {
                    ACTION_REMINDER -> {
                        val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() }
                        if (kind != null) app.reminders.onAlarm(kind)
                    }
                    ACTION_COMPLETE -> {
                        val id = intent.getLongExtra(EXTRA_CHORE_ID, -1)
                        if (id >= 0) app.reminders.onComplete(id)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER = "xyz.haselbach.huushalt.action.REMINDER"
        const val ACTION_COMPLETE = "xyz.haselbach.huushalt.action.COMPLETE"
        const val EXTRA_KIND = "kind"
        const val EXTRA_CHORE_ID = "choreId"
    }
}

/**
 * Lets other apps on the phone ask for the overview of today's chores (F5.8), e.g. the Home
 * Assistant Companion app with a `command_broadcast_intent` notification when you arrive home.
 * Exported without a permission, because such apps can't hold one of ours. That is acceptable:
 * the only effect is a local notification, and it does nothing unless turned on in the settings.
 */
class ExternalTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW_TODAY) return
        val app = context.applicationContext as HuushaltApplication
        val pending = goAsync()
        app.scope.launch {
            try {
                app.reminders.onExternalTrigger()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Kept stable: it is typed into Home Assistant automations. */
        const val ACTION_SHOW_TODAY = "xyz.haselbach.huushalt.action.SHOW_TODAY"
    }
}

/** Alarms are lost on reboot and must follow time and time zone changes (N8). */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> Unit
            else -> return
        }
        val app = context.applicationContext as HuushaltApplication
        val pending = goAsync()
        app.scope.launch {
            try {
                app.reminders.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
