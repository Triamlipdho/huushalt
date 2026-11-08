package xyz.haselbach.huushalt.ui.chores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.ui.components.ChoreRow
import xyz.haselbach.huushalt.ui.components.EmptyState
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.components.withFabSpace

/** All chores, sortable by due date or name (F3.4). Any chore can be marked done here (F4.7). */
@Composable
fun AllChoresScreen(
    chores: List<Chore>,
    today: LocalDate,
    contentPadding: PaddingValues,
    onOpen: (Chore) -> Unit,
    onComplete: (Chore) -> Unit,
) {
    var sort by rememberSaveable { mutableStateOf(Sort.DUE) }
    val paused = chores.filter { it.paused }.sortedBy { it.name.lowercase() }
    val (open, finished) = chores.filterNot { it.paused }.partition { !it.isFinished }
    val byDue = compareBy<Chore> { it.dueDate }.thenBy { it.name.lowercase() }
    val sorted = when (sort) {
        Sort.NAME -> open.sortedBy { it.name.lowercase() }
        else -> open.sortedWith(byDue)
    }
    val noAreaLabel = stringResource(R.string.all_section_no_area)
    // Grouped by room (F3.4), rooms alphabetically, chores without a room last.
    val byArea = open.groupBy { it.areaName }
        .toSortedMap(compareBy<String?> { it == null }.thenBy { it?.lowercase() })

    LazyColumn(contentPadding = contentPadding.withFabSpace()) {
        item(key = "sort") {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sort == Sort.DUE, onClick = { sort = Sort.DUE }, label = { Text(stringResource(R.string.all_sort_due)) })
                FilterChip(selected = sort == Sort.NAME, onClick = { sort = Sort.NAME }, label = { Text(stringResource(R.string.all_sort_name)) })
                FilterChip(selected = sort == Sort.AREA, onClick = { sort = Sort.AREA }, label = { Text(stringResource(R.string.all_sort_area)) })
            }
        }
        if (chores.isEmpty()) {
            item(key = "empty") {
                EmptyState(stringResource(R.string.home_no_chores_title), stringResource(R.string.home_no_chores_body))
            }
        }
        if (sort == Sort.AREA) {
            byArea.forEach { (area, list) ->
                item(key = "area-$area") { SectionHeader(area ?: noAreaLabel) }
                items(list.sortedWith(byDue), key = { it.id }) { chore ->
                    ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = { onComplete(chore) }, showArea = false)
                }
            }
        } else {
            items(sorted, key = { it.id }) { chore ->
                ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = { onComplete(chore) })
            }
        }
        if (paused.isNotEmpty()) {
            item(key = "paused-header") { SectionHeader(stringResource(R.string.all_section_paused)) }
            items(paused, key = { it.id }) { chore ->
                ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = {})
            }
        }
        if (finished.isNotEmpty()) {
            item(key = "finished-header") { SectionHeader(stringResource(R.string.all_section_finished)) }
            items(finished.sortedBy { it.name.lowercase() }, key = { it.id }) { chore ->
                ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = {})
            }
        }
    }
}

private enum class Sort { DUE, NAME, AREA }
