package xyz.haselbach.huushalt.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import java.time.LocalDate
import kotlinx.coroutines.launch
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.ChecklistItem
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.DueStatus
import xyz.haselbach.huushalt.ui.components.ChoreAvatar
import xyz.haselbach.huushalt.ui.dueLabel
import xyz.haselbach.huushalt.ui.formatLong
import xyz.haselbach.huushalt.ui.scheduleLabel
import xyz.haselbach.huushalt.ui.theme.Sizes
import xyz.haselbach.huushalt.ui.theme.Spacing

/**
 * Tapping a chore opens this sheet: what it is, when it's due, its checklist, and the everyday
 * actions (done, postpone, skip). Editing and the full history are one step further, so a quick
 * look never leaves the list.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChoreSheet(
    chore: Chore,
    today: LocalDate,
    checklist: List<ChecklistItem>,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onPostpone: (LocalDate) -> Unit,
    onSkip: () -> Unit,
    onResume: () -> Unit,
    onToggleItem: (ChecklistItem) -> Unit,
    onEdit: () -> Unit,
    onDetails: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showPostpone by rememberSaveable { mutableStateOf(false) }
    // Actions close the sheet first, so the snackbar or the next screen isn't hidden behind it.
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }
    val canAct = !chore.paused && !chore.isFinished
    val overdue = chore.status(today) is DueStatus.Overdue
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.lg),
        ) {
            Row(
                Modifier.padding(horizontal = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                ChoreAvatar(chore.look, size = Sizes.avatarLarge)
                Column(Modifier.weight(1f)) {
                    Text(
                        chore.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        if (chore.paused) stringResource(R.string.all_section_paused) else dueLabel(chore, today),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (overdue && !chore.paused) colors.error else colors.onSurfaceVariant,
                    )
                }
            }
            Column(
                Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                InfoRow(Icons.Filled.Refresh, scheduleLabel(chore.schedule, chore.toleranceDays, today))
                chore.areaName?.let { InfoRow(Icons.Filled.Place, it) }
                InfoRow(
                    Icons.Filled.CheckCircle,
                    chore.lastDone?.let { stringResource(R.string.sheet_last_done, it.formatLong()) }
                        ?: stringResource(R.string.sheet_never_done),
                )
                if (chore.notes.isNotBlank()) InfoRow(Icons.Filled.Info, chore.notes)
            }

            if (checklist.isNotEmpty()) {
                Text(
                    stringResource(R.string.checklist) + " " +
                        stringResource(R.string.checklist_progress, checklist.count { it.checked }, checklist.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = Spacing.lg).semantics { heading() },
                )
                checklist.forEach { item ->
                    ListItem(
                        modifier = Modifier.toggleable(
                            value = item.checked,
                            enabled = canAct,
                            role = Role.Checkbox,
                            onValueChange = { onToggleItem(item) },
                        ),
                        colors = ListItemDefaults.colors(containerColor = colors.surfaceContainerLow),
                        leadingContent = { Checkbox(checked = item.checked, onCheckedChange = null, enabled = canAct) },
                        headlineContent = { Text(item.text) },
                    )
                }
            }

            // The everyday actions, within thumb reach at the bottom of the sheet.
            when {
                chore.paused -> Button(
                    onClick = { closeThen(onResume) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(stringResource(R.string.action_resume), modifier = Modifier.padding(start = Spacing.sm))
                }
                canAct -> {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Button(onClick = { closeThen(onComplete) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Text(stringResource(R.string.action_complete), modifier = Modifier.padding(start = Spacing.sm))
                        }
                        OutlinedButton(onClick = { showPostpone = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Text(stringResource(R.string.action_postpone), modifier = Modifier.padding(start = Spacing.sm))
                        }
                    }
                    if (chore.schedule.isRepeating) {
                        FlowRow(Modifier.padding(horizontal = Spacing.sm)) {
                            TextButton(onClick = { closeThen(onSkip) }) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Text(stringResource(R.string.action_skip), modifier = Modifier.padding(start = Spacing.sm))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = Spacing.sm))
            SheetLink(Icons.Filled.Edit, stringResource(R.string.action_edit)) { closeThen(onEdit) }
            SheetLink(Icons.Filled.History, stringResource(R.string.sheet_details)) { closeThen(onDetails) }
        }
    }

    if (showPostpone) {
        PostponeDialog(
            today = today,
            onDismiss = { showPostpone = false },
            onPick = { date ->
                showPostpone = false
                closeThen { onPostpone(date) }
            },
        )
    }
}

/** A row that leads to another screen. */
@Composable
private fun SheetLink(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(label) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
    )
}
