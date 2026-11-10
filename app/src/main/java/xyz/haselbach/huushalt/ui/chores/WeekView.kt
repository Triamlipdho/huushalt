package xyz.haselbach.huushalt.ui.chores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.DueStatus
import xyz.haselbach.huushalt.domain.Holiday
import xyz.haselbach.huushalt.domain.Occurrence
import xyz.haselbach.huushalt.domain.WeekForecast
import xyz.haselbach.huushalt.ui.components.ChoreAvatar
import xyz.haselbach.huushalt.ui.dueLabel
import xyz.haselbach.huushalt.ui.formatShort

private val dayMonth = DateTimeFormatter.ofPattern("dd.MM.")
private val dayMonthYear = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/**
 * The week view of "Alle Aufgaben" (F3.6): one week from [weekStart], one section per day with
 * the chores due that day. Occurrences after the next due date are a forecast and look quieter.
 *
 * @param weekOffset weeks after the current one, 0 to [WeekForecast.MAX_WEEKS_AHEAD]
 */
fun LazyListScope.weekView(
    days: List<Pair<LocalDate, List<Occurrence>>>,
    today: LocalDate,
    weekOffset: Int,
    holidays: List<Holiday>,
    onWeekOffsetChange: (Int) -> Unit,
    onOpen: (Occurrence) -> Unit,
    onComplete: (Occurrence) -> Unit,
) {
    item(key = "week-nav") {
        WeekNavigation(days.first().first, days.last().first, weekOffset, onWeekOffsetChange)
    }
    item(key = "week-summary") {
        val count = days.sumOf { it.second.size }
        Text(
            if (count == 0) stringResource(R.string.week_summary_none) else pluralStringResource(R.plurals.week_summary, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    days.forEach { (day, occurrences) ->
        item(key = "day-$day") { DayHeader(day, isToday = day == today) }
        if (occurrences.isEmpty()) {
            item(key = "empty-$day") {
                val onHoliday = holidays.any { day in it }
                Text(
                    stringResource(if (onHoliday) R.string.week_holiday else R.string.week_nothing_due),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp).alpha(MUTED_ALPHA),
                )
            }
        } else {
            items(occurrences, key = { "occ-$day-${it.chore.id}" }) { occurrence ->
                WeekChoreRow(occurrence, today, onClick = { onOpen(occurrence) }, onComplete = { onComplete(occurrence) })
            }
        }
    }
}

@Composable
private fun WeekNavigation(start: LocalDate, end: LocalDate, weekOffset: Int, onWeekOffsetChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onWeekOffsetChange(weekOffset - 1) }, enabled = weekOffset > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.week_previous))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (weekOffset) {
                    0 -> stringResource(R.string.week_this)
                    1 -> stringResource(R.string.week_next)
                    else -> stringResource(R.string.week_in_weeks, weekOffset)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.week_range, dayMonth.format(start), dayMonthYear.format(end)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onWeekOffsetChange(weekOffset + 1) }, enabled = weekOffset < WeekForecast.MAX_WEEKS_AHEAD) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.week_next))
        }
        TextButton(onClick = { onWeekOffsetChange(0) }, enabled = weekOffset != 0) {
            Text(stringResource(R.string.week_today))
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, isToday: Boolean) {
    val date = day.formatShort()
    Text(
        if (isToday) stringResource(R.string.week_day_today, date) else date,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isToday) FontWeight.Bold else null,
        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

/** A compact chore row; the done button only for the real occurrence when it is due now. */
@Composable
private fun WeekChoreRow(occurrence: Occurrence, today: LocalDate, onClick: () -> Unit, onComplete: () -> Unit) {
    val chore = occurrence.chore
    val status = chore.status(today)
    val overdue = status is DueStatus.Overdue
    val dueNow = !occurrence.projected && occurrence.date == today && (overdue || chore.isDueNow(today))
    val detail = when {
        occurrence.projected -> stringResource(R.string.week_forecast)
        overdue || status is DueStatus.InWindow -> dueLabel(chore, today)
        else -> null
    }
    val parts = listOfNotNull(chore.areaName, detail)
    val muted = if (occurrence.projected) MUTED_ALPHA else 1f
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ChoreAvatar(chore.look, Modifier.alpha(muted), size = 32.dp)
        Column(Modifier.weight(1f)) {
            Text(
                chore.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (occurrence.projected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (parts.isNotEmpty()) {
                val errorColor = MaterialTheme.colorScheme.error
                Text(
                    // Only the due part is red, as in ChoreRow.
                    buildAnnotatedString {
                        parts.forEachIndexed { index, part ->
                            if (index > 0) append("\u00A0·\u00A0")
                            if (overdue && part == detail) withStyle(SpanStyle(color = errorColor)) { append(part) } else append(part)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(muted),
                )
            }
        }
        if (dueNow) {
            FilledTonalIconButton(
                onClick = onComplete,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_complete_description, chore.name))
            }
        }
    }
}

/** Forecast rows are a little quieter than real due dates, but still readable. */
private const val MUTED_ALPHA = 0.7f
