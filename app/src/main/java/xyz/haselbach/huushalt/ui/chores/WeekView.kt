package xyz.haselbach.huushalt.ui.chores

import xyz.haselbach.huushalt.ui.theme.Spacing
import xyz.haselbach.huushalt.ui.theme.Sizes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import xyz.haselbach.huushalt.ui.components.colors
import xyz.haselbach.huushalt.ui.formatLong
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
 * The week view of "Alle Aufgaben" (F3.6), a planner rather than another list: the calendar week,
 * a strip of its 7 days showing how much is due on each (today marked), and the chores of the
 * selected day. Occurrences after the next due date are a forecast and look quieter.
 *
 * @param weekOffset weeks after the current one, 0 to [WeekForecast.MAX_WEEKS_AHEAD]
 * @param selectedDay a day of [days]; its chores are listed below the strip
 */
fun LazyListScope.weekView(
    days: List<Pair<LocalDate, List<Occurrence>>>,
    today: LocalDate,
    weekOffset: Int,
    holidays: List<Holiday>,
    selectedDay: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    onWeekOffsetChange: (Int) -> Unit,
    onOpen: (Occurrence) -> Unit,
    onComplete: (Occurrence) -> Unit,
) {
    item(key = "week-nav") {
        WeekNavigation(days.first().first, days.last().first, weekOffset, onWeekOffsetChange)
    }
    item(key = "week-strip") {
        WeekStrip(
            days = days,
            today = today,
            selectedDay = selectedDay,
            holidays = holidays,
            onSelect = onSelectDay,
            onSwipe = { delta -> onWeekOffsetChange(weekOffset + delta) },
        )
    }
    item(key = "week-summary") {
        val count = days.sumOf { it.second.size }
        val busiest = days.maxByOrNull { it.second.size }?.takeIf { it.second.size >= 2 }
        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)) {
            Text(
                if (count == 0) stringResource(R.string.week_summary_none) else pluralStringResource(R.plurals.week_summary, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Helps to spread the week: on which day does most come together?
            if (busiest != null) {
                Text(
                    stringResource(R.string.week_busiest, busiest.first.formatShort(), busiest.second.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    val occurrences = days.firstOrNull { it.first == selectedDay }?.second.orEmpty()
    item(key = "day-header") { DayHeader(selectedDay, isToday = selectedDay == today) }
    if (occurrences.isEmpty()) {
        item(key = "day-empty") {
            val onHoliday = holidays.any { selectedDay in it }
            Text(
                stringResource(if (onHoliday) R.string.week_holiday else R.string.week_nothing_due),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
    } else {
        items(occurrences, key = { "occ-$selectedDay-${it.chore.id}" }) { occurrence ->
            WeekChoreRow(
                occurrence,
                today,
                onClick = { onOpen(occurrence) },
                onComplete = { onComplete(occurrence) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/** "KW 46" with the dates and "Diese Woche"; arrows to page, "Heute" to come back. */
@Composable
private fun WeekNavigation(start: LocalDate, end: LocalDate, weekOffset: Int, onWeekOffsetChange: (Int) -> Unit) {
    val week = WeekForecast.calendarWeek(start)
    val relative = when (weekOffset) {
        0 -> stringResource(R.string.week_this)
        1 -> stringResource(R.string.week_next)
        else -> stringResource(R.string.week_in_weeks, weekOffset)
    }
    val spokenWeek = stringResource(R.string.calendar_week_long, week)
    Row(
        Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
            Text(
                stringResource(R.string.calendar_week, week),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { contentDescription = spokenWeek },
            )
            Text(
                relative + " · " + stringResource(R.string.week_range, dayMonth.format(start), dayMonthYear.format(end)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { onWeekOffsetChange(0) }, enabled = weekOffset != 0) {
            Text(stringResource(R.string.week_today))
        }
        IconButton(onClick = { onWeekOffsetChange(weekOffset - 1) }, enabled = weekOffset > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.week_previous))
        }
        IconButton(onClick = { onWeekOffsetChange(weekOffset + 1) }, enabled = weekOffset < WeekForecast.MAX_WEEKS_AHEAD) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.week_next))
        }
    }
}

/**
 * The 7 days side by side, each with the number of chores as dots in their colours. Swiping
 * sideways pages weeks (the arrows do the same, also for TalkBack).
 */
@Composable
private fun WeekStrip(
    days: List<Pair<LocalDate, List<Occurrence>>>,
    today: LocalDate,
    selectedDay: LocalDate,
    holidays: List<Holiday>,
    onSelect: (LocalDate) -> Unit,
    onSwipe: (Int) -> Unit,
) {
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
            .selectableGroup()
            .pointerInput(Unit) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        if (drag <= -threshold) currentOnSwipe(1) else if (drag >= threshold) currentOnSwipe(-1)
                    },
                ) { _, amount -> drag += amount }
            },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        days.forEach { (day, occurrences) ->
            DayCell(
                day = day,
                occurrences = occurrences,
                isToday = day == today,
                selected = day == selectedDay,
                onHoliday = occurrences.isEmpty() && holidays.any { day in it },
                onClick = { onSelect(day) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    occurrences: List<Occurrence>,
    isToday: Boolean,
    selected: Boolean,
    onHoliday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val names = stringArrayResource(R.array.weekdays_short)
    val count = occurrences.size
    val load = if (count == 0) stringResource(R.string.week_nothing_due) else pluralStringResource(R.plurals.count_chores, count, count)
    val spoken = when {
        onHoliday -> stringResource(R.string.week_day_holiday_description, day.formatShort())
        isToday -> stringResource(R.string.week_day_today_description, day.formatShort(), load)
        else -> stringResource(R.string.week_day_description, day.formatShort(), load)
    }
    val content = if (selected) colors.onPrimaryContainer else colors.onSurface
    Column(
        modifier
            .heightIn(min = 88.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.primaryContainer else colors.surfaceContainerLow)
            // Today has a ring, so it stands out without relying on colour alone.
            .then(if (isToday) Modifier.border(2.dp, colors.primary, MaterialTheme.shapes.medium) else Modifier)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = spoken
                this.selected = selected
                role = Role.Tab
            }
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            names[day.dayOfWeek.value - 1],
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        )
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isToday) FontWeight.Bold else null,
            color = if (isToday && !selected) colors.primary else content,
        )
        Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
            when {
                onHoliday -> Icon(Icons.Filled.BeachAccess, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
                count == 0 -> Unit
                count <= MAX_DOTS -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    occurrences.forEach { occurrence ->
                        Box(
                            Modifier
                                .size(6.dp)
                                .alpha(if (occurrence.projected) MUTED_ALPHA else 1f)
                                .background(occurrence.chore.look.color.colors().content, CircleShape),
                        )
                    }
                }
                else -> Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = content)
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, isToday: Boolean) {
    val date = day.formatLong()
    Text(
        if (isToday) stringResource(R.string.week_day_today, date) else date,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = Spacing.xs)
            .semantics { heading() },
    )
}

/** A compact chore row; the done button only for the real occurrence when it is due now. */
@Composable
private fun WeekChoreRow(occurrence: Occurrence, today: LocalDate, onClick: () -> Unit, onComplete: () -> Unit, modifier: Modifier = Modifier) {
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
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ChoreAvatar(chore.look, Modifier.alpha(muted), size = Sizes.avatarSmall)
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

/** Up to this many chores a day are shown as dots, more as a number. */
private const val MAX_DOTS = 4
