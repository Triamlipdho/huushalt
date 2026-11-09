package xyz.haselbach.huushalt.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Reminder settings (F5.1, F5.2, F5.7, F5.8). */
data class ReminderSettings(
    val enabled: Boolean = true,
    val morningTime: LocalTime = LocalTime.of(8, 0),
    val eveningEnabled: Boolean = true,
    val eveningTime: LocalTime = LocalTime.of(19, 0),
    /** Other apps, e.g. Home Assistant, may ask for the overview of today's chores (F5.8). Opt-in. */
    val externalTriggerEnabled: Boolean = false,
)

data class AppSettings(
    val reminders: ReminderSettings = ReminderSettings(),
    /** The user tapped "Nicht jetzt" on the notification permission card (F8.3). */
    val permissionCardDismissed: Boolean = false,
    /** The system permission dialog was shown at least once. */
    val permissionRequested: Boolean = false,
    /** Streaks and celebrations (F7.9). */
    val motivationEnabled: Boolean = true,
    /** Light, dark or as the system (F9.4). */
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Start of the week for statistics and weekday lists (F9.4). */
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
            prefs[EXTERNAL_TRIGGER_ENABLED] = new.reminders.externalTriggerEnabled
            prefs[PERMISSION_CARD_DISMISSED] = new.permissionCardDismissed
            prefs[PERMISSION_REQUESTED] = new.permissionRequested
            prefs[MOTIVATION_ENABLED] = new.motivationEnabled
            prefs[THEME] = new.theme.name
            prefs[FIRST_DAY_OF_WEEK] = new.firstDayOfWeek.value
        }
    }

    private companion object {
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val MORNING_MINUTE = intPreferencesKey("reminders_morning_minute")
        val EVENING_ENABLED = booleanPreferencesKey("reminders_evening_enabled")
        val EVENING_MINUTE = intPreferencesKey("reminders_evening_minute")
        val EXTERNAL_TRIGGER_ENABLED = booleanPreferencesKey("reminders_external_trigger_enabled")
        val PERMISSION_CARD_DISMISSED = booleanPreferencesKey("permission_card_dismissed")
        val PERMISSION_REQUESTED = booleanPreferencesKey("permission_requested")
        val MOTIVATION_ENABLED = booleanPreferencesKey("motivation_enabled")
        val THEME = stringPreferencesKey("theme")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")

        fun Preferences.toSettings(): AppSettings {
            val defaults = ReminderSettings()
            return AppSettings(
                reminders = ReminderSettings(
                    enabled = this[REMINDERS_ENABLED] ?: defaults.enabled,
                    morningTime = this[MORNING_MINUTE]?.toLocalTime() ?: defaults.morningTime,
                    eveningEnabled = this[EVENING_ENABLED] ?: defaults.eveningEnabled,
                    eveningTime = this[EVENING_MINUTE]?.toLocalTime() ?: defaults.eveningTime,
                    externalTriggerEnabled = this[EXTERNAL_TRIGGER_ENABLED] ?: defaults.externalTriggerEnabled,
                ),
                permissionCardDismissed = this[PERMISSION_CARD_DISMISSED] ?: false,
                permissionRequested = this[PERMISSION_REQUESTED] ?: false,
                motivationEnabled = this[MOTIVATION_ENABLED] ?: true,
                theme = this[THEME]?.let { name -> ThemeMode.entries.find { it.name == name } } ?: ThemeMode.SYSTEM,
                firstDayOfWeek = this[FIRST_DAY_OF_WEEK]?.let { DayOfWeek.of(it) } ?: DayOfWeek.MONDAY,
            )
        }

        fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)
        fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
    }
}
