package xyz.haselbach.huushalt.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import xyz.haselbach.huushalt.data.CompletionEntity
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.ui.components.LocalDatePickerDialog
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.dueLabel
import xyz.haselbach.huushalt.ui.formatLong
import xyz.haselbach.huushalt.ui.scheduleLabel

/** Details of one chore with its history (F6.1, F6.2) and the actions done / postpone (F4). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreDetailScreen(
    chore: Chore,
    completions: List<CompletionEntity>,
    today: LocalDate,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onPostpone: (LocalDate) -> Unit,
    onChangeCompletionDate: (completionId: Long, LocalDate) -> Unit,
    onDeleteCompletion: (completionId: Long) -> Unit,
    /** Completions in a row done on time, or null when motivation is turned off (F7.3, F7.9). */
    streak: Int? = null,
) {
    var showPostpone by rememberSaveable { mutableStateOf(false) }
    var editingCompletionId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chore.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "facts") {
                Column {
                    Fact(stringResource(R.string.detail_next_due), dueLabel(chore, today))
                    Fact(stringResource(R.string.detail_schedule), scheduleLabel(chore.schedule))
                    Fact(
                        stringResource(R.string.detail_last_done),
                        chore.lastDone?.formatLong() ?: stringResource(R.string.detail_never_done),
                    )
                    if (chore.notes.isNotBlank()) Fact(stringResource(R.string.detail_notes), chore.notes)
                    if (streak != null && streak >= 2) {
                        Text(
                            stringResource(R.string.detail_chore_streak, streak),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            if (!chore.isFinished) {
                item(key = "actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(onClick = onComplete, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Text(stringResource(R.string.action_complete), modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = { showPostpone = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                            Text(stringResource(R.string.action_postpone), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            item(key = "history-header") {
                HorizontalDivider()
                SectionHeader(stringResource(R.string.detail_history))
            }
            if (completions.isEmpty()) {
                item(key = "history-empty") {
                    Text(
                        stringResource(R.string.detail_history_empty),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(completions, key = { it.id }) { completion ->
                ListItem(
                    modifier = Modifier.clickable { editingCompletionId = completion.id },
                    headlineContent = { Text(completion.doneDate.formatLong()) },
                )
            }
        }
    }

    completions.find { it.id == editingCompletionId }?.let { completion ->
        EditCompletionDialog(
            completion = completion,
            today = today,
            onDismiss = { editingCompletionId = null },
            onChangeDate = { date ->
                editingCompletionId = null
                onChangeCompletionDate(completion.id, date)
            },
            onDelete = {
                editingCompletionId = null
                onDeleteCompletion(completion.id)
            },
        )
    }

    if (showPostpone) {
        PostponeDialog(
            today = today,
            onDismiss = { showPostpone = false },
            onPick = { date ->
                showPostpone = false
                onPostpone(date)
            },
        )
    }
}

@Composable
private fun Fact(label: String, value: String) {
    ListItem(
        overlineContent = { Text(label) },
        headlineContent = { Text(value) },
    )
}

/** Postpone to tomorrow, in a week or to a chosen date (F4.4). */
@Composable
private fun PostponeDialog(today: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val tomorrow = today.plusDays(1)
    if (pickDate) {
        LocalDatePickerDialog(initial = tomorrow, earliest = tomorrow, onDismiss = onDismiss, onConfirm = onPick)
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.postpone_title)) },
        text = {
            Column {
                TextButton(onClick = { onPick(tomorrow) }) { Text(stringResource(R.string.postpone_tomorrow)) }
                TextButton(onClick = { onPick(today.plusWeeks(1)) }) { Text(stringResource(R.string.postpone_next_week)) }
                TextButton(onClick = { pickDate = true }) { Text(stringResource(R.string.postpone_pick_date)) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Change the date of a past completion or delete it (F6.4). */
@Composable
private fun EditCompletionDialog(
    completion: CompletionEntity,
    today: LocalDate,
    onDismiss: () -> Unit,
    onChangeDate: (LocalDate) -> Unit,
    onDelete: () -> Unit,
) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    if (pickDate) {
        LocalDatePickerDialog(initial = completion.doneDate, latest = today, onDismiss = onDismiss, onConfirm = onChangeDate)
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.completion_title, completion.doneDate.formatLong())) },
        text = {
            Column {
                TextButton(onClick = { pickDate = true }) { Text(stringResource(R.string.completion_change_date)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.completion_delete)) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
