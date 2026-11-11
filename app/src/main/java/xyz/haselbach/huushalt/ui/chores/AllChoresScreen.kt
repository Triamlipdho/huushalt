package xyz.haselbach.huushalt.ui.chores

import xyz.haselbach.huushalt.ui.components.stickyBackground
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.Holiday
import xyz.haselbach.huushalt.domain.WeekForecast
import xyz.haselbach.huushalt.ui.components.ChoreRow
import xyz.haselbach.huushalt.ui.components.EmptyState
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.components.withFabSpace

/**
 * All chores, sortable by due date or name (F3.4), or as a week view (F3.6). Any chore can be
 * marked done here (F4.7).
 */
@Composable
fun AllChoresScreen(
    chores: List<Chore>,
    today: LocalDate,
    contentPadding: PaddingValues,
    onOpen: (Chore) -> Unit,
    onComplete: (Chore) -> Unit,
    /** Swipe left: one day later (due chores: tomorrow). */
    onPostpone: ((Chore) -> Unit)? = null,
    showAreaSort: Boolean = true,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    holidays: List<Holiday> = emptyList(),
) {
    // List or week view (F3.6).
    var weekMode by rememberSaveable { mutableStateOf(false) }
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    // Selected day of the week view, as days since the week's start (0..6); null: today, or the
    // first day in other weeks.
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var sort by rememberSaveable { mutableStateOf(Sort.DUE) }
    var query by rememberSaveable { mutableStateOf("") }
    // Search by name and notes (F3.7).
    val visible = if (query.isBlank()) chores else chores.filter {
        it.name.contains(query.trim(), ignoreCase = true) || it.notes.contains(query.trim(), ignoreCase = true) ||
            it.areaName?.contains(query.trim(), ignoreCase = true) == true
    }
    val paused = visible.filter { it.paused }.sortedBy { it.name.lowercase() }
    val (open, finished) = visible.filterNot { it.paused }.partition { !it.isFinished }
    val byDue = compareBy<Chore> { it.dueDate }.thenBy { it.name.lowercase() }
    val sorted = when (sort) {
        Sort.NAME -> open.sortedBy { it.name.lowercase() }
        else -> open.sortedWith(byDue)
    }
    val noAreaLabel = stringResource(R.string.all_section_no_area)
    // Grouped by room (F3.4), rooms alphabetically, chores without a room last.
    val byArea = open.groupBy { it.areaName }
        .toSortedMap(compareBy<String?> { it == null }.thenBy { it?.lowercase() })

    val viewSwitch: LazyListScope.() -> Unit = {
        item(key = "view-switch") {
            ViewSwitch(weekMode, onChange = { weekMode = it })
        }
    }
    if (weekMode) {
        val start = WeekForecast.weekStart(today, firstDayOfWeek).plusWeeks(weekOffset.toLong())
        val days = WeekForecast.week(chores, start, today, holidays)
        val selectedDay = selectedIndex?.let { start.plusDays(it.toLong()) }
            ?: if (weekOffset == 0) today else start
        LazyColumn(contentPadding = contentPadding.withFabSpace()) {
            viewSwitch()
            weekView(
                days = days,
                today = today,
                weekOffset = weekOffset,
                holidays = holidays,
                selectedDay = selectedDay,
                onSelectDay = { selectedIndex = (it.toEpochDay() - start.toEpochDay()).toInt() },
                onWeekOffsetChange = { offset ->
                    val next = offset.coerceIn(0, WeekForecast.MAX_WEEKS_AHEAD)
                    // Back to this week: show today again; other weeks keep the weekday.
                    if (next == 0) selectedIndex = null
                    weekOffset = next
                },
                onOpen = { onOpen(it.chore) },
                onComplete = { onComplete(it.chore) },
            )
        }
        return
    }

    LazyColumn(contentPadding = contentPadding.withFabSpace()) {
        viewSwitch()
        if (chores.size > SEARCH_FROM) {
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.all_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (query.isEmpty()) null else {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.all_search_clear))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        item(key = "sort") {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sort == Sort.DUE, onClick = { sort = Sort.DUE }, label = { Text(stringResource(R.string.all_sort_due)) })
                FilterChip(selected = sort == Sort.NAME, onClick = { sort = Sort.NAME }, label = { Text(stringResource(R.string.all_sort_name)) })
                if (showAreaSort) {
                    FilterChip(selected = sort == Sort.AREA, onClick = { sort = Sort.AREA }, label = { Text(stringResource(R.string.all_sort_area)) })
                }
            }
        }
        if (chores.isNotEmpty() && visible.isEmpty()) {
            item(key = "no-results") {
                EmptyState(stringResource(R.string.all_search_empty_title), stringResource(R.string.all_search_empty_body, query.trim()))
            }
        }
        if (chores.isEmpty()) {
            item(key = "empty") {
                EmptyState(stringResource(R.string.home_no_chores_title), stringResource(R.string.home_no_chores_body))
            }
        }
        if (sort == Sort.AREA && showAreaSort) {
            byArea.forEach { (area, list) ->
                stickyHeader(key = "area-$area") { SectionHeader(area ?: noAreaLabel, Modifier.stickyBackground()) }
                items(list.sortedWith(byDue), key = { it.id }) { chore ->
                    ChoreRow(
                        chore,
                        today,
                        onClick = { onOpen(chore) },
                        onComplete = { onComplete(chore) },
                        showArea = false,
                        onPostpone = onPostpone?.let { postpone -> { postpone(chore) } },
                    )
                }
            }
        } else {
            items(sorted, key = { it.id }) { chore ->
                ChoreRow(
                    chore,
                    today,
                    onClick = { onOpen(chore) },
                    onComplete = { onComplete(chore) },
                    onPostpone = onPostpone?.let { postpone -> { postpone(chore) } },
                )
            }
        }
        if (paused.isNotEmpty()) {
            stickyHeader(key = "paused-header") { SectionHeader(stringResource(R.string.all_section_paused), Modifier.stickyBackground()) }
            items(paused, key = { it.id }) { chore ->
                ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = {})
            }
        }
        if (finished.isNotEmpty()) {
            stickyHeader(key = "finished-header") { SectionHeader(stringResource(R.string.all_section_finished), Modifier.stickyBackground()) }
            items(finished.sortedBy { it.name.lowercase() }, key = { it.id }) { chore ->
                ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = {})
            }
        }
    }
}

private enum class Sort { DUE, NAME, AREA }

/** "Liste | Woche" (F3.6). */
@Composable
private fun ViewSwitch(weekMode: Boolean, onChange: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        listOf(false to R.string.all_view_list, true to R.string.all_view_week).forEachIndexed { index, (week, label) ->
            SegmentedButton(
                selected = weekMode == week,
                onClick = { onChange(week) },
                shape = SegmentedButtonDefaults.itemShape(index, 2),
            ) { Text(stringResource(label)) }
        }
    }
}

/** The search field appears once there are enough chores to need it. */
private const val SEARCH_FROM = 8
