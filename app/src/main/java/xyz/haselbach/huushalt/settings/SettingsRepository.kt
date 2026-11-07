package xyz.haselbach.huushalt.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Reminder settings (F5.1, F5.2, F5.7). */
data class ReminderSettings(
    val enabled: Boolean = true,
    val morningTime: LocalTime = LocalTime.of(8, 0),
    val eveningEnabled: Boolean = true,
    val eveningTime: LocalTime = LocalTime.of(19, 0),
)

data class AppSettings(
    val reminders: ReminderSettings = ReminderSettings(),
    /** The user tapped "Nicht jetzt" on the notification permission card (F8.3). */
    val permissionCardDismissed: Boolean = false,
    /** The system permission dialog was shown at least once. */
    val permissionRequested: Boolean = false,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.settingsDataStore)

    val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val new = transform(prefs.toSettings())
            prefs[REMINDERS_ENABLED] = new.reminders.enabled
            prefs[MORNING_MINUTE] = new.reminders.morningTime.toMinuteOfDay()
            prefs[EVENING_ENABLED] = new.reminders.eveningEnabled
            prefs[EVENING_MINUTE] = new.reminders.eveningTime.toMinuteOfDay()
            prefs[PERMISSION_CARD_DISMISSED] = new.permissionCardDismissed
            prefs[PERMISSION_REQUESTED] = new.permissionRequested
        }
    }

    private companion object {
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val MORNING_MINUTE = intPreferencesKey("reminders_morning_minute")
        val EVENING_ENABLED = booleanPreferencesKey("reminders_evening_enabled")
        val EVENING_MINUTE = intPreferencesKey("reminders_evening_minute")
        val PERMISSION_CARD_DISMISSED = booleanPreferencesKey("permission_card_dismissed")
        val PERMISSION_REQUESTED = booleanPreferencesKey("permission_requested")

        fun Preferences.toSettings(): AppSettings {
            val defaults = ReminderSettings()
            return AppSettings(
                reminders = ReminderSettings(
                    enabled = this[REMINDERS_ENABLED] ?: defaults.enabled,
                    morningTime = this[MORNING_MINUTE]?.toLocalTime() ?: defaults.morningTime,
                    eveningEnabled = this[EVENING_ENABLED] ?: defaults.eveningEnabled,
                    eveningTime = this[EVENING_MINUTE]?.toLocalTime() ?: defaults.eveningTime,
                ),
                permissionCardDismissed = this[PERMISSION_CARD_DISMISSED] ?: false,
                permissionRequested = this[PERMISSION_REQUESTED] ?: false,
            )
        }

        fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)
        fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
    }
}
