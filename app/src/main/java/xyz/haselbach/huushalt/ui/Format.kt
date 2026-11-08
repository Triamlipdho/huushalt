package xyz.haselbach.huushalt.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.DueStatus
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleMode

private val swissGerman: Locale = Locale.forLanguageTag("de-CH")
private val shortDate = DateTimeFormatter.ofPattern("EE, dd.MM.", swissGerman)
private val longDate = DateTimeFormatter.ofPattern("EE, dd.MM.yyyy", swissGerman)

/** "Do., 12.11." */
fun LocalDate.formatShort(): String = shortDate.format(this)

/** "Do., 12.11.2026" */
fun LocalDate.formatLong(): String = longDate.format(this)

/** Short text when a chore is due, e.g. "3 Tage überfällig · 2× verpasst" or "Morgen". */
@Composable
@ReadOnlyComposable
fun dueLabel(chore: Chore, today: LocalDate): String = when (val status = chore.status(today)) {
    is DueStatus.Overdue -> {
        val days = pluralStringResource(R.plurals.due_overdue_days, status.days, status.days)
        if (status.missed > 1) days + " · " + stringResource(R.string.due_missed, status.missed) else days
    }
    DueStatus.Today -> stringResource(R.string.due_today)
    is DueStatus.InWindow -> pluralStringResource(R.plurals.due_in_window, status.daysSinceDue, status.daysSinceDue)
    is DueStatus.Upcoming ->
        if (status.date == today.plusDays(1)) stringResource(R.string.due_tomorrow) else status.date.formatShort()
    is DueStatus.Later -> stringResource(R.string.due_on, status.date.formatLong())
    DueStatus.Finished -> stringResource(R.string.due_finished)
    DueStatus.Paused -> stringResource(R.string.due_paused)
}

/** Human readable schedule, e.g. "Alle 2 Wochen am Mi" or "4 Wochen nach dem Erledigen". */
@Composable
@ReadOnlyComposable
fun scheduleLabel(schedule: Schedule, toleranceDays: Int = 0, today: LocalDate = LocalDate.now()): String {
    val base = baseScheduleLabel(schedule, today)
    if (toleranceDays == 0) return base
    return stringResource(R.string.schedule_tolerance, base, pluralStringResource(R.plurals.interval_days, toleranceDays, toleranceDays))
}

@Composable
@ReadOnlyComposable
private fun baseScheduleLabel(schedule: Schedule, today: LocalDate): String {
    val n = schedule.interval
    if (!schedule.isRepeating) return stringResource(R.string.schedule_once)
    if (schedule.unit == RepeatUnit.DATES) {
        val left = schedule.dates.count { it >= today }
        return pluralStringResource(R.plurals.schedule_dates_left, left, left)
    }
    if (schedule.mode == ScheduleMode.FLEXIBLE) {
        val interval = when (schedule.unit) {
            RepeatUnit.DAYS -> pluralStringResource(R.plurals.interval_days, n, n)
            RepeatUnit.WEEKS -> pluralStringResource(R.plurals.interval_weeks, n, n)
            else -> pluralStringResource(R.plurals.interval_months, n, n)
        }
        return stringResource(R.string.schedule_flexible, interval)
    }
    return when (schedule.unit) {
        RepeatUnit.DAYS ->
            if (n == 1) stringResource(R.string.schedule_daily) else stringResource(R.string.schedule_every_days, n)
        RepeatUnit.WEEKS -> {
            val names = stringArrayResource(R.array.weekdays_short)
            val days = schedule.weekdays.sorted().joinToString(", ") { names[it.value - 1] }
            if (n == 1) stringResource(R.string.schedule_weekly_on, days)
            else stringResource(R.string.schedule_every_weeks_on, n, days)
        }
        else -> if (schedule.weekOfMonth != null) {
            val dayName = stringArrayResource(R.array.weekdays_long)[schedule.weekdays.single().value - 1]
            val week = schedule.weekOfMonth
            when {
                week == Schedule.LAST_WEEK && n == 1 -> stringResource(R.string.schedule_monthly_last, dayName)
                week == Schedule.LAST_WEEK -> stringResource(R.string.schedule_every_months_last, n, dayName)
                n == 1 -> stringResource(R.string.schedule_monthly_nth, week, dayName)
                else -> stringResource(R.string.schedule_every_months_nth, n, week, dayName)
            }
        } else {
            val day = schedule.dayOfMonth ?: 1
            if (n == 1) stringResource(R.string.schedule_monthly_on, day)
            else stringResource(R.string.schedule_every_months_on, n, day)
        }
    }
}
