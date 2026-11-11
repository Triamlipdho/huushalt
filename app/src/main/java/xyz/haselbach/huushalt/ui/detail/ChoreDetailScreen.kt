package xyz.haselbach.huushalt.ui.detail

import xyz.haselbach.huushalt.ui.theme.Spacing
import xyz.haselbach.huushalt.ui.theme.Sizes
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.res.pluralStringResource
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.data.CompletionEntity
import xyz.haselbach.huushalt.domain.ChecklistItem
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.DueStatus
import xyz.haselbach.huushalt.domain.IntervalSuggestion
import xyz.haselbach.huushalt.ui.components.ChoreAvatar
import xyz.haselbach.huushalt.ui.components.IntervalSuggestionCard
import xyz.haselbach.huushalt.ui.components.LocalDatePickerDialog
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.dueLabel
import xyz.haselbach.huushalt.ui.formatLong
import xyz.haselbach.huushalt.ui.scheduleLabel

/** Details of one chore with its history (F6.1, F6.2) and the actions done / postpone (F4). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    checklist: List<ChecklistItem> = emptyList(),
    onToggleItem: (ChecklistItem) -> Unit = {},
    onPausedChange: (Boolean) -> Unit = {},
    /** Done on an earlier day (F4.3). */
    onCompleteOn: (LocalDate) -> Unit = {},
    /** Skip this occurrence (F4.5). */
    onSkip: () -> Unit = {},
    /** Create a new chore with the same settings (F1.10). */
    onDuplicate: () -> Unit = {},
    onDelete: () -> Unit = {},
    /** Open interval suggestion (F2.13), with "Übernehmen" and "Nein danke". */
    intervalSuggestion: IntervalSuggestion? = null,
    onAcceptSuggestion: () -> Unit = {},
    onDismissSuggestion: () -> Unit = {},
) {
    var showPostpone by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var pickDoneDate by rememberSaveable { mutableStateOf(false) }
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
                    // Rarely used actions live in the menu, so the screen stays calm.
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (!chore.paused && !chore.isFinished) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_pause)) },
                                    leadingIcon = { Icon(Icons.Filled.Pause, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onPausedChange(true)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_duplicate)) },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "summary") { Summary(chore, today, streak) }
            if (chore.paused) {
                item(key = "paused") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.detail_paused), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { onPausedChange(false) }) { Text(stringResource(R.string.action_resume)) }
                    }
                }
            } else if (!chore.isFinished) {
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
                    FlowRow(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TextButton(onClick = { pickDoneDate = true }) {
                            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Text(stringResource(R.string.action_done_earlier), modifier = Modifier.padding(start = 8.dp))
                        }
                        if (chore.schedule.isRepeating) {
                            TextButton(onClick = onSkip) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Text(stringResource(R.string.action_skip), modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
            if (intervalSuggestion != null && !chore.paused) {
                item(key = "interval-suggestion") {
                    IntervalSuggestionCard(chore, intervalSuggestion, onAccept = onAcceptSuggestion, onDismiss = onDismissSuggestion)
                }
            }
            if (checklist.isNotEmpty()) {
                item(key = "checklist-header") {
                    SectionHeader(
                        stringResource(R.string.checklist) + " " +
                            stringResource(R.string.checklist_progress, checklist.count { it.checked }, checklist.size),
                    )
                }
                items(checklist, key = { "item-${it.id}" }) { item ->
                    ListItem(
                        modifier = Modifier.toggleable(
                            value = item.checked,
                            enabled = !chore.paused,
                            role = Role.Checkbox,
                            onValueChange = { onToggleItem(item) },
                        ),
                        leadingContent = { Checkbox(checked = item.checked, onCheckedChange = null, enabled = !chore.paused) },
                        headlineContent = { Text(item.text) },
                    )
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
            itemsIndexed(completions, key = { _, it -> it.id }) { index, completion ->
                HistoryRow(
                    completion = completion,
                    // Completions are newest first: the one before this is the next in the list.
                    previous = completions.getOrNull(index + 1),
                    toleranceDays = chore.toleranceDays,
                    onClick = { editingCompletionId = completion.id },
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_title, chore.name)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (pickDoneDate) {
        LocalDatePickerDialog(
            initial = today.minusDays(1),
            latest = today,
            onDismiss = { pickDoneDate = false },
            onConfirm = {
                pickDoneDate = false
                onCompleteOn(it)
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

/**
 * Compact summary: the status large (red when overdue), then schedule, room, last done, notes and
 * the chore's streak as short rows with icons.
 */
@Composable
private fun Summary(chore: Chore, today: LocalDate, streak: Int?) {
    val overdue = chore.status(today) is DueStatus.Overdue
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChoreAvatar(chore.look, size = Sizes.avatarLarge)
                Text(
                    dueLabel(chore, today),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            InfoRow(Icons.Filled.Refresh, scheduleLabel(chore.schedule, chore.toleranceDays, today))
            chore.areaName?.let { InfoRow(Icons.Filled.Place, it) }
            InfoRow(
                Icons.Filled.CheckCircle,
                chore.lastDone?.let { stringResource(R.string.detail_last_done_on, it.formatLong()) }
                    ?: stringResource(R.string.detail_never_done_long),
            )
            if (chore.notes.isNotBlank()) InfoRow(Icons.Filled.Info, chore.notes)
            if (streak != null && streak >= 2) {
                Text(
                    "🔥 " + stringResource(R.string.detail_chore_streak, streak),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

/**
 * One entry of the history: the date, whether it was on time (by the due date plus tolerance,
 * F7.3) and how long after the previous time.
 */
@Composable
private fun HistoryRow(completion: CompletionEntity, previous: CompletionEntity?, toleranceDays: Int, onClick: () -> Unit) {
    val lateDays = completion.previousDueDate?.let { due ->
        ChronoUnit.DAYS.between(due.plusDays(toleranceDays.toLong()), completion.doneDate).toInt()
    }
    val parts = listOfNotNull(
        when {
            lateDays == null -> null
            lateDays <= 0 -> stringResource(R.string.history_on_time)
            else -> pluralStringResource(R.plurals.history_late, lateDays, lateDays)
        },
        previous?.let {
            val gap = ChronoUnit.DAYS.between(it.doneDate, completion.doneDate).toInt()
            pluralStringResource(R.plurals.history_gap, gap, gap)
        },
    )
    val late = lateDays != null && lateDays > 0
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                if (late) Icons.Filled.Schedule else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (late) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(completion.doneDate.formatLong()) },
        supportingContent = if (parts.isEmpty()) null else {
            { Text(parts.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
    )
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
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
