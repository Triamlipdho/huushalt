package xyz.haselbach.huushalt.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.data.ChecklistDraft
import xyz.haselbach.huushalt.data.toBitmask
import xyz.haselbach.huushalt.domain.Area
import xyz.haselbach.huushalt.domain.ChecklistItem
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleCalculator
import xyz.haselbach.huushalt.domain.ScheduleError
import xyz.haselbach.huushalt.domain.ScheduleMode
import xyz.haselbach.huushalt.ui.components.LocalDatePickerDialog
import xyz.haselbach.huushalt.ui.formatLong
import xyz.haselbach.huushalt.ui.formatShort

private enum class Repeat { ONCE, FIXED, FLEXIBLE }

/** Create or edit a chore (F1.1, F1.2) with its schedule (F2.1 to F2.6, F2.9). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChoreEditScreen(
    chore: Chore?,
    today: LocalDate,
    onClose: () -> Unit,
    onSave: (ChoreDraft) -> Unit,
    onDelete: () -> Unit,
    areas: List<Area> = emptyList(),
    initialChecklist: List<ChecklistItem> = emptyList(),
) {
    val initial = chore?.schedule
    var name by rememberSaveable { mutableStateOf(chore?.name ?: "") }
    var notes by rememberSaveable { mutableStateOf(chore?.notes ?: "") }
    var repeat by rememberSaveable {
        mutableStateOf(
            when {
                initial == null -> Repeat.FLEXIBLE
                !initial.isRepeating -> Repeat.ONCE
                initial.mode == ScheduleMode.FIXED -> Repeat.FIXED
                else -> Repeat.FLEXIBLE
            },
        )
    }
    var unit by rememberSaveable { mutableStateOf(initial?.unit?.takeIf { it != RepeatUnit.NONE } ?: RepeatUnit.WEEKS) }
    var intervalText by rememberSaveable { mutableStateOf((initial?.interval ?: 1).toString()) }
    var weekdayMask by rememberSaveable { mutableIntStateOf(initial?.weekdays?.toBitmask() ?: 0) }
    var dueEpochDay by rememberSaveable { mutableLongStateOf((chore?.dueDate ?: today).toEpochDay()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var areaId by rememberSaveable { mutableStateOf(chore?.areaId) }
    // A new room typed in directly, created together with the chore. Null: an existing room or none.
    var newAreaName by rememberSaveable { mutableStateOf<String?>(null) }
    val checklist = rememberSaveable(saver = checklistSaver) {
        initialChecklist.map { ChecklistDraft(it.id, it.text) }.toMutableStateList()
    }

    val due = LocalDate.ofEpochDay(dueEpochDay)
    val weekdays = DayOfWeek.entries.filter { weekdayMask and (1 shl (it.value - 1)) != 0 }.toSet()
    val interval = intervalText.toIntOrNull() ?: 0
    val schedule = when (repeat) {
        Repeat.ONCE -> Schedule.ONCE
        Repeat.FIXED -> Schedule(ScheduleMode.FIXED, unit, interval, weekdays, due.dayOfMonth)
        Repeat.FLEXIBLE -> Schedule(ScheduleMode.FLEXIBLE, unit, interval)
    }
    val scheduleError = schedule.validate()
    val nameMissing = name.isBlank()
    val adjustedDue = if (scheduleError == null) ScheduleCalculator.initialDueDate(schedule, due) else due

    fun ensureWeekday() {
        if (repeat == Repeat.FIXED && unit == RepeatUnit.WEEKS && weekdayMask == 0) {
            weekdayMask = setOf(due.dayOfWeek).toBitmask()
        }
    }

    fun save() {
        if (nameMissing || scheduleError != null) {
            showErrors = true
            return
        }
        // An existing room with the typed name is reused instead of creating a second one.
        val typed = newAreaName?.trim()?.takeIf { it.isNotEmpty() }
        val existing = typed?.let { t -> areas.find { it.name.equals(t, ignoreCase = true) } }
        onSave(
            ChoreDraft(
                name,
                notes,
                schedule,
                due,
                areaId = existing?.id ?: areaId.takeIf { typed == null },
                newAreaName = typed.takeIf { existing == null },
                checklist = checklist.toList(),
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (chore == null) R.string.edit_title_new else R.string.edit_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    if (chore != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    TextButton(onClick = ::save) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.edit_name)) },
                placeholder = { Text(stringResource(R.string.edit_name_hint)) },
                singleLine = true,
                isError = showErrors && nameMissing,
                supportingText = if (showErrors && nameMissing) {
                    { Text(stringResource(R.string.edit_error_name)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.edit_notes)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            AreaPicker(
                areas = areas,
                selectedId = areaId,
                creatingNew = newAreaName != null,
                onSelect = {
                    areaId = it
                    newAreaName = null
                },
                onNew = {
                    areaId = null
                    newAreaName = ""
                },
            )
            newAreaName?.let { typed ->
                OutlinedTextField(
                    value = typed,
                    onValueChange = { newAreaName = it },
                    label = { Text(stringResource(R.string.area_name)) },
                    placeholder = { Text(stringResource(R.string.area_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(stringResource(R.string.edit_repeat), style = MaterialTheme.typography.titleSmall)
            Column(Modifier.selectableGroup()) {
                RepeatOption(repeat == Repeat.ONCE, R.string.edit_repeat_once, R.string.edit_repeat_once_help) {
                    repeat = Repeat.ONCE
                }
                RepeatOption(repeat == Repeat.FIXED, R.string.edit_repeat_fixed, R.string.edit_repeat_fixed_help) {
                    repeat = Repeat.FIXED
                    ensureWeekday()
                }
                RepeatOption(repeat == Repeat.FLEXIBLE, R.string.edit_repeat_flexible, R.string.edit_repeat_flexible_help) {
                    repeat = Repeat.FLEXIBLE
                }
            }

            if (repeat != Repeat.ONCE) {
                val intervalError = showErrors && scheduleError == ScheduleError.INTERVAL_OUT_OF_RANGE
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.edit_every))
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { text -> intervalText = text.filter(Char::isDigit).take(3) },
                        singleLine = true,
                        isError = intervalError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                    )
                }
                if (intervalError) {
                    Text(stringResource(R.string.edit_error_interval), color = MaterialTheme.colorScheme.error)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        RepeatUnit.DAYS to R.string.edit_unit_days,
                        RepeatUnit.WEEKS to R.string.edit_unit_weeks,
                        RepeatUnit.MONTHS to R.string.edit_unit_months,
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = unit == value,
                            onClick = {
                                unit = value
                                ensureWeekday()
                            },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                if (repeat == Repeat.FIXED && unit == RepeatUnit.WEEKS) {
                    Text(stringResource(R.string.edit_weekdays), style = MaterialTheme.typography.titleSmall)
                    val names = stringArrayResource(R.array.weekdays_short)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            val bit = 1 shl (day.value - 1)
                            FilterChip(
                                selected = weekdayMask and bit != 0,
                                onClick = { weekdayMask = weekdayMask xor bit },
                                label = { Text(names[day.value - 1]) },
                            )
                        }
                    }
                    if (showErrors && scheduleError == ScheduleError.NO_WEEKDAY) {
                        Text(stringResource(R.string.edit_error_weekday), color = MaterialTheme.colorScheme.error)
                    }
                }
                if (repeat == Repeat.FIXED && unit == RepeatUnit.MONTHS) {
                    Text(stringResource(R.string.edit_month_day, due.dayOfMonth), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text(
                stringResource(if (repeat == Repeat.ONCE) R.string.edit_due_once else R.string.edit_due_next),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Text(due.formatLong(), modifier = Modifier.padding(start = 8.dp))
            }
            if (adjustedDue != due) {
                Text(
                    stringResource(R.string.edit_due_adjusted, adjustedDue.formatShort()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Checklist (F1.7)
            Text(stringResource(R.string.checklist), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.checklist_optional),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            checklist.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = item.text,
                        onValueChange = { checklist[index] = item.copy(text = it) },
                        placeholder = { Text(stringResource(R.string.checklist_item_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { checklist.removeAt(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.checklist_remove))
                    }
                }
            }
            TextButton(onClick = { checklist.add(ChecklistDraft(null, "")) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.checklist_add), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showDatePicker) {
        LocalDatePickerDialog(
            initial = due,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                dueEpochDay = it.toEpochDay()
                showDatePicker = false
            },
        )
    }

    if (showDeleteConfirm && chore != null) {
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
}

@Composable
private fun RepeatOption(selected: Boolean, title: Int, help: Int, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Everything the editor produces. [newAreaName] is a room to create for this chore. */
data class ChoreDraft(
    val name: String,
    val notes: String,
    val schedule: Schedule,
    val nextDue: LocalDate,
    val areaId: Long?,
    val newAreaName: String?,
    val checklist: List<ChecklistDraft>,
)

private val checklistSaver = listSaver<SnapshotStateList<ChecklistDraft>, String>(
    save = { list -> list.flatMap { listOf(it.id?.toString() ?: "", it.text) } },
    restore = { flat -> flat.chunked(2).map { (id, text) -> ChecklistDraft(id.toLongOrNull(), text) }.toMutableStateList() },
)

/** Room selection (F1.4): existing rooms, none, or a new one. */
@Composable
private fun AreaPicker(
    areas: List<Area>,
    selectedId: Long?,
    creatingNew: Boolean,
    onSelect: (Long?) -> Unit,
    onNew: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val label = when {
        creatingNew -> stringResource(R.string.area_new_title)
        else -> areas.find { it.id == selectedId }?.name ?: stringResource(R.string.area_none)
    }
    Text(stringResource(R.string.area), style = MaterialTheme.typography.titleSmall)
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(label)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.area_none)) }, onClick = {
                expanded = false
                onSelect(null)
            })
            areas.forEach { area ->
                DropdownMenuItem(text = { Text(area.name) }, onClick = {
                    expanded = false
                    onSelect(area.id)
                })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.area_new)) }, onClick = {
                expanded = false
                onNew()
            })
        }
    }
}
