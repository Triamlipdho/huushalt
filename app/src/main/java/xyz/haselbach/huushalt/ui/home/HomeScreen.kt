package xyz.haselbach.huushalt.ui.home

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
    header: @Composable () -> Unit = {},
    /** Current "Alles erledigt" streak, or null when motivation is turned off (F7.2, F7.9). */
    streak: Int? = null,
) {
    LazyColumn(contentPadding = contentPadding) {
        item(key = "header") { header() }
        item(key = "progress") { Progress(state, streak) }
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
) {
    if (chores.isEmpty()) return
    item(key = "header-$title") { SectionHeader(stringResource(title)) }
    items(chores, key = { "chore-${it.id}" }) { chore ->
        ChoreRow(chore, today, onClick = { onOpen(chore) }, onComplete = { onComplete(chore) })
    }
}
