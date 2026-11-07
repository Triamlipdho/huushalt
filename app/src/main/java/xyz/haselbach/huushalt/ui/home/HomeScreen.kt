package xyz.haselbach.huushalt.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.ui.HomeState
import xyz.haselbach.huushalt.ui.components.ChoreRow
import xyz.haselbach.huushalt.ui.components.EmptyState
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.formatLong

/** Daily overview: overdue, today and the next 7 days (F3.1 to F3.3), with today's progress (F7.1). */
@Composable
fun HomeScreen(
    state: HomeState,
    contentPadding: PaddingValues,
    onOpen: (Chore) -> Unit,
    onComplete: (Chore) -> Unit,
) {
    LazyColumn(contentPadding = contentPadding) {
        item(key = "progress") { Progress(state) }
        when {
            !state.hasChores -> item(key = "empty") {
                EmptyState(stringResource(R.string.home_no_chores_title), stringResource(R.string.home_no_chores_body))
            }
            state.open == 0 -> item(key = "done") {
                EmptyState(stringResource(R.string.home_empty_title), stringResource(R.string.home_empty_body))
            }
        }
        section(R.string.home_section_overdue, state.overdue, state.today, onOpen, onComplete)
        section(R.string.home_section_today, state.dueToday, state.today, onOpen, onComplete)
        section(R.string.home_section_upcoming, state.upcoming, state.today, onOpen, onComplete)
    }
}

@Composable
private fun Progress(state: HomeState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(state.today.formatLong(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.total > 0) {
            Text(stringResource(R.string.home_progress, state.doneToday, state.total), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { state.doneToday.toFloat() / state.total },
                modifier = Modifier.fillMaxWidth(),
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
) {
    if (chores.isEmpty()) return
    item(key = "header-$title") { SectionHeader(stringResource(title)) }
    items(chores, key = { "chore-${it.id}" }) { chore ->
        ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = { onComplete(chore) })
    }
}
