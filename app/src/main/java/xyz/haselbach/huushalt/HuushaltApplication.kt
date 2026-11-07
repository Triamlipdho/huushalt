package xyz.haselbach.huushalt

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.haselbach.huushalt.data.ChoreRepository
import xyz.haselbach.huushalt.data.HuushaltDatabase
import xyz.haselbach.huushalt.reminders.ReminderNotifier
import xyz.haselbach.huushalt.reminders.ReminderScheduler
import xyz.haselbach.huushalt.reminders.Reminders
import xyz.haselbach.huushalt.settings.SettingsRepository

class HuushaltApplication : Application() {
    /** For work that must outlive a screen, e.g. in broadcast receivers. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: ChoreRepository by lazy { ChoreRepository(HuushaltDatabase.create(this)) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val notifier: ReminderNotifier by lazy { ReminderNotifier(this) }
    val reminders: Reminders by lazy { Reminders(repository, settings, ReminderScheduler(this), notifier) }

    override fun onCreate() {
        super.onCreate()
        notifier.createChannel()
        // (Re)schedule reminders on every start and whenever their settings change.
        scope.launch {
            settings.settings.map { it.reminders }.distinctUntilChanged().collect { reminders.rescheduleAll() }
        }
    }
}
