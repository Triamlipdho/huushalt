package xyz.haselbach.huushalt.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import xyz.haselbach.huushalt.ui.DoneToday
import xyz.haselbach.huushalt.ui.components.ChoreAvatar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import xyz.haselbach.huushalt.domain.IntervalSuggestion
import xyz.haselbach.huushalt.ui.components.IntervalSuggestionHint
import xyz.haselbach.huushalt.ui.HomeState
import xyz.haselbach.huushalt.ui.components.ChoreRow
import xyz.haselbach.huushalt.ui.components.EmptyState
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.components.SectionHeaderWithAction
import xyz.haselbach.huushalt.ui.components.withFabSpace
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
    onEditChore: (Chore) -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    /** Takes back a completion from today. */
    onReopen: (DoneToday) -> Unit = {},
    /** Swipe left: one day later (due chores: tomorrow). */
    onPostpone: (Chore) -> Unit = {},
    /** Open interval suggestions (F2.13); the first one is shown as a hint that opens its chore. */
    intervalSuggestions: List<Pair<Chore, IntervalSuggestion>> = emptyList(),
) {
    var confirmReschedule by rememberSaveable { mutableStateOf(false) }
    // Filter the lists by room (F3.5); null shows all.
    var areaFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDone by rememberSaveable { mutableStateOf(false) }
    val filter: (List<Chore>) -> List<Chore> = { list -> areaFilter?.let { id -> list.filter { it.areaId == id } } ?: list }
    val holiday = state.activeHoliday
    val rescheduleLabel = stringResource(R.string.reschedule_action)
    LazyColumn(contentPadding = contentPadding.withFabSpace()) {
        if (holiday != null) {
            // On holiday: nothing to do, nothing to show (F10.1).
            item(key = "holiday") {
                HolidayCard(holiday, onEnd = { onEndHoliday(holiday) }, onChange = onChangeHoliday)
            }
            return@LazyColumn
        }
        item(key = "header") { header() }
        // Date lists about to run out (F2.11).
        items(state.datesRunningOut, key = { "dates-${it.id}" }) { chore ->
            DatesRunningOutCard(chore, state.today, onClick = { onEditChore(chore) })
        }
        if (state.total > 0 || (streak != null && streak >= 2)) {
            item(key = "progress") { Progress(state, streak) }
        }
        state.plannedHoliday?.let { planned ->
            item(key = "planned-holiday") { PlannedHolidayChip(planned, onClick = onChangeHoliday) }
        }
        intervalSuggestions.firstOrNull()?.let { (chore, suggestion) ->
            item(key = "interval-suggestion") {
                IntervalSuggestionHint(chore, suggestion, more = intervalSuggestions.size - 1, onClick = { onOpen(chore) })
            }
        }
        if (areas.isNotEmpty() && state.hasChores) {
            item(key = "areas") {
                AreaFilter(areas, areaFilter, onSelect = { areaFilter = it })
            }
        }
        when {
            !state.hasChores -> item(key = "empty") {
                // First start: offer the starter set (F8.2).
                EmptyState(
                    stringResource(R.string.home_no_chores_title),
                    stringResource(R.string.home_no_chores_body),
                    actionLabel = stringResource(R.string.catalog_open),
                    onAction = onOpenCatalog,
                )
            }
            state.open == 0 -> item(key = "done") {
                EmptyState(stringResource(R.string.home_empty_title), stringResource(R.string.home_empty_body))
            }
        }
        section(
            R.string.home_section_overdue,
            filter(state.overdue),
            state.today,
            onOpen,
            onComplete,
            action = rescheduleLabel to { confirmReschedule = true },
            onPostpone = onPostpone,
        )
        section(
            R.string.home_section_today,
            filter(state.dueToday),
            state.today,
            onOpen,
            onComplete,
            showDueToday = false,
            onPostpone = onPostpone,
        )
        section(R.string.home_section_upcoming, filter(state.upcoming), state.today, onOpen, onComplete, onPostpone = onPostpone)
        // What was done today stays visible, and can be taken back (folded away by default).
        val done = state.doneTodayList.filter { item -> areaFilter == null || item.chore.areaId == areaFilter }
        if (done.isNotEmpty()) {
            item(key = "header-done") {
                DoneHeader(done.size, expanded = showDone, onToggle = { showDone = !showDone })
            }
            if (showDone) {
                items(done, key = { "done-${it.completionId}" }) { item ->
                    DoneRow(item, onClick = { onOpen(item.chore) }, onReopen = { onReopen(item) })
                }
            }
        }
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

@Composable
private fun AreaFilter(areas: List<Area>, selected: Long?, onSelect: (Long?) -> Unit) {
    // One scrollable line, however many rooms there are.
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(stringResource(R.string.filter_all)) }) }
        items(areas, key = { it.id }) { area ->
            FilterChip(selected = selected == area.id, onClick = { onSelect(area.id) }, label = { Text(area.name) })
        }
    }
}

@Composable
private fun DatesRunningOutCard(chore: Chore, today: LocalDate, onClick: () -> Unit) {
    val last = chore.schedule.dates.lastOrNull { it >= today }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.dates_running_out_title, chore.name), style = MaterialTheme.typography.titleSmall)
            Text(
                if (last == null) {
                    stringResource(R.string.dates_running_out_none)
                } else {
                    stringResource(R.string.dates_running_out_last, last.formatShort())
                },
                style = MaterialTheme.typography.bodyMedium,
            )
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

/** Today's progress as a ring (F7.1) and the streak (F7.2). The date is in the title bar. */
@Composable
private fun Progress(state: HomeState, streak: Int?) {
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.total > 0) {
                val progress by animateFloatAsState(state.doneToday.toFloat() / state.total, label = "progress")
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 6.dp,
                        trackColor = colors.surface.copy(alpha = 0.6f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                    )
                    Text(
                        "${state.doneToday}/${state.total}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.total > 0) {
                    Text(
                        stringResource(R.string.home_progress, state.doneToday, state.total),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (streak != null && streak >= 2) {
                    Text(
                        stringResource(R.string.streak_home, pluralStringResource(R.plurals.streak_days, streak, streak)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.tertiary,
                    )
                }
            }
        }
    }
}

/** A planned holiday, as a small chip that opens the holiday settings. */
@Composable
private fun PlannedHolidayChip(planned: Holiday, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                stringResource(
                    R.string.holiday_planned,
                    stringResource(R.string.holiday_period, planned.start.formatShort(), planned.end.formatShort()),
                ),
            )
        },
        leadingIcon = { Icon(Icons.Filled.BeachAccess, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

private fun LazyListScope.section(
    title: Int,
    chores: List<Chore>,
    today: LocalDate,
    onOpen: (Chore) -> Unit,
    onComplete: (Chore) -> Unit,
    action: Pair<String, () -> Unit>? = null,
    showDueToday: Boolean = true,
    onPostpone: ((Chore) -> Unit)? = null,
) {
    if (chores.isEmpty()) return
    item(key = "header-$title") {
        if (action == null) SectionHeader(stringResource(title)) else SectionHeaderWithAction(stringResource(title), action.first, action.second)
    }
    items(chores, key = { "chore-${it.id}" }) { chore ->
        ChoreRow(
            chore,
            today,
            onClick = { onOpen(chore) },
            onComplete = { onComplete(chore) },
            modifier = Modifier.animateItem(),
            showDueToday = showDueToday,
            onPostpone = onPostpone?.let { postpone -> { postpone(chore) } },
        )
    }
}

@Composable
private fun DoneHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val action = stringResource(if (expanded) R.string.home_section_done_hide else R.string.home_section_done_show)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = action, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.home_section_done, count),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun DoneRow(item: DoneToday, onClick: () -> Unit, onReopen: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { ChoreAvatar(item.chore.look, Modifier.alpha(0.6f)) },
        headlineContent = {
            Text(
                item.chore.name,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingContent = { Text(stringResource(R.string.done_today), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            IconButton(onClick = onReopen) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.action_reopen_description, item.chore.name),
                )
            }
        },
    )
}
