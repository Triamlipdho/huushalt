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

/** All chores, sortable by due date or name (F3.4). Any chore can be marked done here (F4.7). */
@Composable
fun AllChoresScreen(
    chores: List<Chore>,
    today: LocalDate,
    contentPadding: PaddingValues,
    onOpen: (Chore) -> Unit,
    onComplete: (Chore) -> Unit,
) {
    var sortByName by rememberSaveable { mutableStateOf(false) }
    val (open, finished) = chores.partition { !it.isFinished }
    val sorted = if (sortByName) {
        open.sortedBy { it.name.lowercase() }
    } else {
        open.sortedWith(compareBy<Chore> { it.dueDate }.thenBy { it.name.lowercase() })
    }

    LazyColumn(contentPadding = contentPadding) {
        item(key = "sort") {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !sortByName, onClick = { sortByName = false }, label = { Text(stringResource(R.string.all_sort_due)) })
                FilterChip(selected = sortByName, onClick = { sortByName = true }, label = { Text(stringResource(R.string.all_sort_name)) })
            }
        }
        if (chores.isEmpty()) {
            item(key = "empty") {
                EmptyState(stringResource(R.string.home_no_chores_title), stringResource(R.string.home_no_chores_body))
            }
        }
        items(sorted, key = { it.id }) { chore ->
            ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = { onComplete(chore) })
        }
        if (finished.isNotEmpty()) {
            item(key = "finished-header") { SectionHeader(stringResource(R.string.all_section_finished)) }
            items(finished.sortedBy { it.name.lowercase() }, key = { it.id }) { chore ->
                ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = {})
            }
        }
    }
}
