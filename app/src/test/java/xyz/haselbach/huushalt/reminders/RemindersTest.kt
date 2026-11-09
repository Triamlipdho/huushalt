package xyz.haselbach.huushalt.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import xyz.haselbach.huushalt.HuushaltApplication
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode
import xyz.haselbach.huushalt.testing.resetSettings

/** Reminder notifications and alarms (F5.1, F5.2, F5.4, F5.7, N8). */
@RunWith(RobolectricTestRunner::class)
class RemindersTest {

    private lateinit var app: HuushaltApplication
    private val today = LocalDate.now()
    private val weekly = Schedule(ScheduleMode.FLEXIBLE, RepeatUnit.WEEKS, 1)

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as HuushaltApplication
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        resetSettings()
    }

    private fun notifications(): List<Notification> =
        shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications

    private fun Notification.title() = extras.getCharSequence(Notification.EXTRA_TITLE).toString()

    @Test
    fun `morning overview lists open chores and schedules both reminders`() = runBlocking {
        app.repository.save(null, "Staubsaugen", "", weekly, today)
        app.repository.save(null, "Pflanzen giessen", "", weekly, today.minusDays(2))
        app.repository.save(null, "Fenster putzen", "", weekly, today.plusDays(3))

        val now = ZonedDateTime.now()
        app.reminders.onAlarm(ReminderKind.MORNING, now)

        val notification = notifications().single()
        assertEquals("1 heute fällig, 1 überfällig", notification.title())
        assertEquals("Pflanzen giessen, Staubsaugen", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertNull("No action with several chores", notification.actions)

        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms
        assertEquals(2, alarms.size)
        val nowMillis = now.toInstant().toEpochMilli()
        alarms.forEach { assertTrue(it.triggerAtMs in nowMillis..nowMillis + 25 * 3_600_000L) }
    }

    @Test
    fun `no notification when nothing is open`() = runBlocking {
        app.repository.save(null, "Fenster putzen", "", weekly, today.plusDays(3))
        app.reminders.onAlarm(ReminderKind.EVENING)
        assertTrue(notifications().isEmpty())
    }

    @Test
    fun `single chore can be marked done from the notification`() = runBlocking {
        val id = app.repository.save(null, "Staubsaugen", "", weekly, today)
        app.reminders.onAlarm(ReminderKind.EVENING)

        val notification = notifications().single()
        assertEquals("Noch 1 Aufgabe offen", notification.title())
        assertEquals("Erledigt", notification.actions.single().title.toString())

        app.reminders.onComplete(id, today)
        assertTrue(notifications().isEmpty())
        assertEquals(today.plusWeeks(1), app.repository.chores.first().single().dueDate)
    }

    @Test
    fun `disabled reminders show nothing and cancel the alarms`() = runBlocking {
        app.repository.save(null, "Staubsaugen", "", weekly, today)
        app.settings.update { it.copy(reminders = it.reminders.copy(enabled = false)) }

        app.reminders.onAlarm(ReminderKind.MORNING)

        assertTrue(notifications().isEmpty())
        assertTrue(shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms.isEmpty())
    }

    @Test
    fun `evening reminder can be turned off on its own`() = runBlocking {
        app.repository.save(null, "Staubsaugen", "", weekly, today)
        app.settings.update { it.copy(reminders = it.reminders.copy(eveningEnabled = false)) }

        app.reminders.onAlarm(ReminderKind.EVENING)
        assertTrue(notifications().isEmpty())
        assertEquals(1, shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms.size)
    }

    @Test
    fun `no reminders during holidays (F10_1)`() = runBlocking {
        app.repository.save(null, "Staubsaugen", "", weekly, today)
        app.repository.saveHoliday(null, today, today.plusDays(3), today)

        app.reminders.onAlarm(ReminderKind.MORNING)

        assertTrue(notifications().isEmpty())
    }

    @Test
    fun `other apps can show today's overview once allowed (F5_8)`() = runBlocking {
        app.repository.save(null, "Staubsaugen", "", weekly, today)
        app.repository.save(null, "Pflanzen giessen", "", weekly, today.minusDays(2))
        app.repository.save(null, "Fenster putzen", "", weekly, today.plusDays(3))

        app.reminders.onExternalTrigger()
        assertTrue("Off by default", notifications().isEmpty())

        app.settings.update { it.copy(reminders = it.reminders.copy(externalTriggerEnabled = true)) }
        app.reminders.onExternalTrigger()

        val notification = notifications().single()
        assertEquals("1 heute fällig, 1 überfällig", notification.title())
        assertEquals("Pflanzen giessen, Staubsaugen", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun `other apps can't show anything with reminders off or during holidays`() = runBlocking {
        app.repository.save(null, "Staubsaugen", "", weekly, today)
        app.settings.update { it.copy(reminders = it.reminders.copy(enabled = false, externalTriggerEnabled = true)) }
        app.reminders.onExternalTrigger()
        assertTrue(notifications().isEmpty())

        app.settings.update { it.copy(reminders = it.reminders.copy(enabled = true)) }
        app.repository.saveHoliday(null, today, today.plusDays(3), today)
        app.reminders.onExternalTrigger()
        assertTrue(notifications().isEmpty())
    }

    @Test
    fun `broadcast from another app reaches the receiver (F5_8)`() {
        runBlocking {
            app.repository.save(null, "Staubsaugen", "", weekly, today)
            app.settings.update { it.copy(reminders = it.reminders.copy(externalTriggerEnabled = true)) }
        }
        // Like Home Assistant's command_broadcast_intent: action plus package, no class name.
        val intent = Intent(ExternalTriggerReceiver.ACTION_SHOW_TODAY).setPackage(app.packageName)
        assertEquals(
            ExternalTriggerReceiver::class.java.name,
            app.packageManager.queryBroadcastReceivers(intent, 0).single().activityInfo.name,
        )
        ExternalTriggerReceiver().onReceive(app, intent)
        runBlocking { app.scope.coroutineContext[Job]!!.children.forEach { it.join() } }

        assertEquals("1 Aufgabe heute", notifications().single().title())
    }
}
