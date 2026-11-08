package xyz.haselbach.huushalt.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Area
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.Holiday
import xyz.haselbach.huushalt.ui.HomeState
import xyz.haselbach.huushalt.ui.components.ChoreRow
import xyz.haselbach.huushalt.ui.components.EmptyState
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.formatLong
import xyz.haselbach.huushalt.ui.formatShort

/** Daily overview: overdue, today and the next 7 days (F3.1 to F3.3), with today's progress (F7.1). */
@Composable
fun HomeScreen(
    state: HomeState,
    contentPadding: PaddingValues,
    onOpen: (Chore) -> Unit,
    onComplete: (Chore) -> Unit,
    header: @Composable () -> Unit = {},
    /** Current "Alles erledigt" streak, or null when motivation is turned off (F7.2, F7.9). */
    streak: Int? = null,
    onReschedule: () -> Unit = {},
    onEndHoliday: (Holiday) -> Unit = {},
    onChangeHoliday: () -> Unit = {},
    areas: List<Area> = emptyList(),
) {
    var confirmReschedule by rememberSaveable { mutableStateOf(false) }
    // Filter the lists by room (F3.5); null shows all.
    var areaFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    val filter: (List<Chore>) -> List<Chore> = { list -> areaFilter?.let { id -> list.filter { it.areaId == id } } ?: list }
    val holiday = state.activeHoliday
    LazyColumn(contentPadding = contentPadding) {
        if (holiday != null) {
            // On holiday: nothing to do, nothing to show (F10.1).
            item(key = "holiday") {
                HolidayCard(holiday, onEnd = { onEndHoliday(holiday) }, onChange = onChangeHoliday)
            }
            return@LazyColumn
        }
        item(key = "header") { header() }
        item(key = "progress") { Progress(state, streak) }
        if (areas.isNotEmpty() && state.hasChores) {
            item(key = "areas") {
                AreaFilter(areas, areaFilter, onSelect = { areaFilter = it })
            }
        }
        when {
            !state.hasChores -> item(key = "empty") {
                EmptyState(stringResource(R.string.home_no_chores_title), stringResource(R.string.home_no_chores_body))
            }
            state.open == 0 -> item(key = "done") {
                EmptyState(stringResource(R.string.home_empty_title), stringResource(R.string.home_empty_body))
            }
        }
        section(R.string.home_section_overdue, filter(state.overdue), state.today, onOpen, onComplete) {
            TextButton(onClick = { confirmReschedule = true }) { Text(stringResource(R.string.reschedule_action)) }
        }
        section(R.string.home_section_today, filter(state.dueToday), state.today, onOpen, onComplete)
        section(R.string.home_section_upcoming, filter(state.upcoming), state.today, onOpen, onComplete)
    }

    if (confirmReschedule) {
        AlertDialog(
            onDismissRequest = { confirmReschedule = false },
            title = { Text(stringResource(R.string.reschedule_title)) },
            text = { Text(stringResource(R.string.reschedule_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReschedule = false
                    onReschedule()
                }) { Text(stringResource(R.string.reschedule_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReschedule = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AreaFilter(areas: List<Area>, selected: Long?, onSelect: (Long?) -> Unit) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(stringResource(R.string.filter_all)) })
        areas.forEach { area ->
            FilterChip(selected = selected == area.id, onClick = { onSelect(area.id) }, label = { Text(area.name) })
        }
    }
}

@Composable
private fun HolidayCard(holiday: Holiday, onEnd: () -> Unit, onChange: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🏖️ " + stringResource(R.string.holiday_active_title, holiday.end.formatShort()), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.holiday_active_body), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onChange) { Text(stringResource(R.string.holiday_change)) }
                Button(onClick = onEnd) { Text(stringResource(R.string.holiday_end)) }
            }
        }
    }
}

@Composable
private fun Progress(state: HomeState, streak: Int?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(state.today.formatLong(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.total > 0) {
            Text(stringResource(R.string.home_progress, state.doneToday, state.total), style = MaterialTheme.typography.titleMedium)
            val progress by animateFloatAsState(state.doneToday.toFloat() / state.total, label = "progress")
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        state.plannedHoliday?.let { planned ->
            Text(
                stringResource(
                    R.string.holiday_planned,
                    stringResource(R.string.holiday_period, planned.start.formatShort(), planned.end.formatShort()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (streak != null && streak >= 2) {
            Text(
                stringResource(R.string.streak_home, pluralStringResource(R.plurals.streak_days, streak, streak)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun LazyListScope.section(
    title: Int,
    chores: List<Chore>,
    today: LocalDate,
    onOpen: (Chore) -> Unit,
    onComplete: (Chore) -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    if (chores.isEmpty()) return
    item(key = "header-$title") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(stringResource(title), Modifier.weight(1f))
            if (action != null) Box(Modifier.padding(end = 8.dp)) { action() }
        }
    }
    items(chores, key = { "chore-${it.id}" }) { chore ->
        ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = { onComplete(chore) })
    }
}
