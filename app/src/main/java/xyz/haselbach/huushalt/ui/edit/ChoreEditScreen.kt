package xyz.haselbach.huushalt.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import xyz.haselbach.huushalt.ui.scheduleLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.InputChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
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
import xyz.haselbach.huushalt.domain.ChoreCatalog
import xyz.haselbach.huushalt.domain.ChoreColor
import xyz.haselbach.huushalt.domain.ChoreLook
import xyz.haselbach.huushalt.domain.ChoreSymbol
import xyz.haselbach.huushalt.domain.ChoreTemplate
import xyz.haselbach.huushalt.domain.DateListParser
import xyz.haselbach.huushalt.domain.RepeatUnit
import xyz.haselbach.huushalt.domain.Schedule
import xyz.haselbach.huushalt.domain.ScheduleCalculator
import xyz.haselbach.huushalt.domain.ScheduleError
import xyz.haselbach.huushalt.domain.ScheduleMode
import xyz.haselbach.huushalt.ui.components.ChoreAvatar
import xyz.haselbach.huushalt.ui.components.LocalDatePickerDialog
import xyz.haselbach.huushalt.ui.components.colors
import xyz.haselbach.huushalt.ui.components.icon
import xyz.haselbach.huushalt.ui.components.label
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
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    /** A chore to copy when creating a new one ("Duplizieren", F1.10). */
    copyOf: Chore? = null,
    /** Names of existing chores; their catalogue suggestions are not offered again. */
    existingNames: Set<String> = emptySet(),
    /** Counts presses of the system back button; each one is handled like the close button. */
    backRequests: Int = 0,
) {
    // Values come from the chore being edited, or the one being copied.
    val source = chore ?: copyOf
    val initial = source?.schedule
    var name by rememberSaveable {
        mutableStateOf(chore?.name ?: copyOf?.name?.let { it + " (Kopie)" } ?: "")
    }
    var notes by rememberSaveable { mutableStateOf(source?.notes ?: "") }
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
    var suggestionPicked by rememberSaveable { mutableStateOf(false) }
    // Monthly: on a day of the month, the nth weekday or the last weekday (F2.10).
    var monthlyRule by rememberSaveable {
        mutableStateOf(
            when (initial?.weekOfMonth) {
                null -> MonthlyRule.DAY
                Schedule.LAST_WEEK -> MonthlyRule.LAST
                else -> MonthlyRule.NTH
            },
        )
    }
    // Date list (F2.11), as epoch days.
    val dateList = rememberSaveable(saver = dateListSaver) {
        initial?.dates.orEmpty().map { it.toEpochDay() }.toMutableStateList()
    }
    var pasteText by rememberSaveable { mutableStateOf("") }
    var showDateListPicker by rememberSaveable { mutableStateOf(false) }
    var toleranceDays by rememberSaveable { mutableIntStateOf(source?.toleranceDays ?: 0) }
    // Chosen symbol and colour (F1.5); null: guessed from the name.
    var symbolKey by rememberSaveable { mutableStateOf(source?.symbolKey) }
    var colorKey by rememberSaveable { mutableStateOf(source?.colorKey) }
    var showSymbolPicker by rememberSaveable { mutableStateOf(false) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var areaId by rememberSaveable { mutableStateOf(source?.areaId) }
    // A new room typed in directly, created together with the chore. Null: an existing room or none.
    var newAreaName by rememberSaveable { mutableStateOf<String?>(null) }
    val checklist = rememberSaveable(saver = checklistSaver) {
        // A copy gets new checklist items (no ids), the edited chore keeps its own.
        initialChecklist.map { ChecklistDraft(if (chore != null) it.id else null, it.text) }.toMutableStateList()
    }

    // Everything the form holds, to tell whether leaving would lose changes.
    val formState = listOf(
        name, notes, repeat, unit, intervalText, weekdayMask, dueEpochDay, monthlyRule, dateList.toList(),
        toleranceDays, symbolKey, colorKey, areaId, newAreaName, checklist.toList(),
    ).toString()
    val initialFormState by rememberSaveable { mutableStateOf(formState) }
    val hasChanges = formState != initialFormState
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }
    fun close() {
        if (hasChanges) showDiscardConfirm = true else onClose()
    }
    val handledBackRequests = remember { backRequests }
    LaunchedEffect(backRequests) {
        if (backRequests != handledBackRequests) close()
    }

    /** Quick create: fill the form from a catalogue suggestion. */
    fun applyTemplate(template: ChoreTemplate) {
        name = template.name
        repeat = if (template.schedule.mode == ScheduleMode.FIXED) Repeat.FIXED else Repeat.FLEXIBLE
        unit = template.schedule.unit
        intervalText = template.schedule.interval.toString()
        checklist.clear()
        checklist.addAll(template.checklist.map { ChecklistDraft(null, it) })
        val existingArea = template.area?.let { a -> areas.find { it.name.equals(a, ignoreCase = true) } }
        areaId = existingArea?.id
        newAreaName = if (existingArea == null) template.area else null
        suggestionPicked = true
    }

    val due = LocalDate.ofEpochDay(dueEpochDay)
    val weekdays = DayOfWeek.entries.filter { weekdayMask and (1 shl (it.value - 1)) != 0 }.toSet()
    val interval = intervalText.toIntOrNull() ?: 0
    val dates = dateList.map(LocalDate::ofEpochDay).sorted()
    val nthWeek = (due.dayOfMonth - 1) / 7 + 1
    val isLastWeek = due.dayOfMonth + 7 > due.lengthOfMonth()
    val schedule = when {
        repeat == Repeat.ONCE -> Schedule.ONCE
        repeat == Repeat.FIXED && unit == RepeatUnit.DATES -> Schedule(ScheduleMode.FIXED, RepeatUnit.DATES, dates = dates)
        repeat == Repeat.FIXED && unit == RepeatUnit.MONTHS && monthlyRule != MonthlyRule.DAY -> Schedule(
            ScheduleMode.FIXED,
            RepeatUnit.MONTHS,
            interval,
            setOf(due.dayOfWeek),
            weekOfMonth = if (monthlyRule == MonthlyRule.LAST || nthWeek > 4) Schedule.LAST_WEEK else nthWeek,
        )
        repeat == Repeat.FIXED -> Schedule(ScheduleMode.FIXED, unit, interval, weekdays, due.dayOfMonth)
        else -> Schedule(ScheduleMode.FLEXIBLE, unit, interval)
    }
    val isDateList = schedule.unit == RepeatUnit.DATES
    val scheduleError = schedule.validate()
    val nameMissing = name.isBlank()
    val adjustedDue = if (scheduleError == null) ScheduleCalculator.initialDueDate(schedule, if (isDateList) today else due) else due

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
                if (isDateList) today else due,
                areaId = existing?.id ?: areaId.takeIf { typed == null },
                newAreaName = typed.takeIf { existing == null },
                checklist = checklist.toList(),
                toleranceDays = if (schedule.isRepeating) toleranceDays else 0,
                symbol = ChoreSymbol.byKey(symbolKey),
                color = ChoreColor.entries.firstOrNull { it.name == colorKey },
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (chore == null) R.string.edit_title_new else R.string.edit_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = ::close) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
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
            if (chore == null && copyOf == null && !suggestionPicked) {
                QuickSuggestions(query = name, existingNames = existingNames, onPick = ::applyTemplate)
            }
            SymbolRow(
                look = ChoreLook.of(name, symbolKey, colorKey),
                automatic = symbolKey == null && colorKey == null,
                onChange = { showSymbolPicker = true },
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
            // The schedule in one sentence, updated while editing.
            if (scheduleError == null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.EventRepeat, contentDescription = null)
                        Column {
                            Text(scheduleLabel(schedule, if (schedule.isRepeating) toleranceDays else 0, today), style = MaterialTheme.typography.bodyLarge)
                            adjustedDue?.let {
                                Text(stringResource(R.string.edit_summary_next, it.formatLong()), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
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
                    if (unit == RepeatUnit.DATES) unit = RepeatUnit.WEEKS
                }
            }

            if (repeat != Repeat.ONCE) {
                val intervalError = showErrors && scheduleError == ScheduleError.INTERVAL_OUT_OF_RANGE
                // "Alle [2] [Tage | Wochen | Monate]" on one line where it fits.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isDateList) {
                        Text(stringResource(R.string.edit_every))
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { text -> intervalText = text.filter(Char::isDigit).take(3) },
                            singleLine = true,
                            isError = intervalError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp),
                        )
                    }
                    listOfNotNull(
                        RepeatUnit.DAYS to R.string.edit_unit_days,
                        RepeatUnit.WEEKS to R.string.edit_unit_weeks,
                        RepeatUnit.MONTHS to R.string.edit_unit_months,
                        (RepeatUnit.DATES to R.string.edit_unit_dates).takeIf { repeat == Repeat.FIXED },
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
                if (intervalError) {
                    Text(stringResource(R.string.edit_error_interval), color = MaterialTheme.colorScheme.error)
                }
                if (repeat == Repeat.FIXED && unit == RepeatUnit.WEEKS) {
                    Text(stringResource(R.string.edit_weekdays), style = MaterialTheme.typography.titleSmall)
                    val names = stringArrayResource(R.array.weekdays_short)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Weekdays in the order of the chosen first day of the week (F9.4).
                        (0L until 7L).map { firstDayOfWeek.plus(it) }.forEach { day ->
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
                    val dayName = stringArrayResource(R.array.weekdays_long)[due.dayOfWeek.value - 1]
                    Column(Modifier.selectableGroup()) {
                        RepeatOption(
                            monthlyRule == MonthlyRule.DAY,
                            stringResource(R.string.edit_month_on_day, due.dayOfMonth),
                        ) { monthlyRule = MonthlyRule.DAY }
                        if (nthWeek <= 4) {
                            RepeatOption(
                                monthlyRule == MonthlyRule.NTH,
                                stringResource(R.string.edit_month_on_nth_weekday, nthWeek, dayName),
                            ) { monthlyRule = MonthlyRule.NTH }
                        }
                        if (isLastWeek) {
                            RepeatOption(
                                monthlyRule == MonthlyRule.LAST || (monthlyRule == MonthlyRule.NTH && nthWeek > 4),
                                stringResource(R.string.edit_month_on_last_weekday, dayName),
                            ) { monthlyRule = MonthlyRule.LAST }
                        }
                    }
                    Text(
                        stringResource(R.string.edit_month_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isDateList) {
                    DateListEditor(
                        dates = dates,
                        today = today,
                        pasteText = pasteText,
                        onPasteTextChange = { pasteText = it },
                        onApplyPaste = {
                            val parsed = DateListParser.parse(pasteText).map { it.toEpochDay() }
                            dateList.addAll(parsed.filter { it !in dateList })
                            pasteText = ""
                        },
                        onAdd = { showDateListPicker = true },
                        onRemove = { dateList.remove(it.toEpochDay()) },
                        showError = showErrors && scheduleError == ScheduleError.NO_DATES,
                    )
                }

                // Tolerance window (F2.12)
                Text(stringResource(R.string.edit_tolerance), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TOLERANCE_OPTIONS.forEach { days ->
                        FilterChip(
                            selected = toleranceDays == days,
                            onClick = { toleranceDays = days },
                            label = { Text(toleranceLabel(days)) },
                        )
                    }
                }
                Text(
                    stringResource(R.string.edit_tolerance_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isDateList) {
                Text(
                    stringResource(if (repeat == Repeat.ONCE) R.string.edit_due_once else R.string.edit_due_next),
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Text(due.formatLong(), modifier = Modifier.padding(start = 8.dp))
                }
                if (adjustedDue != null && adjustedDue != due) {
                    Text(
                        stringResource(R.string.edit_due_adjusted, adjustedDue.formatShort()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

            // Deleting is at the end of the form, far away from "Speichern".
            if (chore != null) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(stringResource(R.string.edit_delete), modifier = Modifier.padding(start = 8.dp))
                }
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

    if (showSymbolPicker) {
        SymbolPickerDialog(
            current = ChoreLook.of(name, symbolKey, colorKey),
            onDismiss = { showSymbolPicker = false },
            onAutomatic = {
                symbolKey = null
                colorKey = null
                showSymbolPicker = false
            },
            onPick = { look ->
                symbolKey = look.symbol.name
                colorKey = look.color.name
                showSymbolPicker = false
            },
        )
    }

    if (showDateListPicker) {
        LocalDatePickerDialog(
            initial = dates.lastOrNull()?.plusWeeks(2)?.takeIf { it >= today } ?: today,
            earliest = today,
            onDismiss = { showDateListPicker = false },
            onConfirm = {
                if (it.toEpochDay() !in dateList) dateList.add(it.toEpochDay())
                showDateListPicker = false
            },
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onClose()
                }) { Text(stringResource(R.string.discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text(stringResource(R.string.discard_keep_editing)) }
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
private fun RepeatOption(selected: Boolean, title: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

/** Dates of a date list (F2.11): added one by one or pasted in bulk. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DateListEditor(
    dates: List<LocalDate>,
    today: LocalDate,
    pasteText: String,
    onPasteTextChange: (String) -> Unit,
    onApplyPaste: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (LocalDate) -> Unit,
    showError: Boolean,
) {
    Text(stringResource(R.string.edit_dates), style = MaterialTheme.typography.titleSmall)
    if (dates.isEmpty()) {
        Text(
            stringResource(if (showError) R.string.edit_error_dates else R.string.edit_dates_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        dates.forEach { date ->
            InputChip(
                selected = false,
                enabled = true,
                onClick = { onRemove(date) },
                label = { Text(if (date.year == today.year) date.formatShort() else date.formatLong()) },
                trailingIcon = {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.edit_dates_remove, date.formatLong()),
                    )
                },
            )
        }
    }
    TextButton(onClick = onAdd) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text(stringResource(R.string.edit_dates_add), modifier = Modifier.padding(start = 8.dp))
    }
    OutlinedTextField(
        value = pasteText,
        onValueChange = onPasteTextChange,
        label = { Text(stringResource(R.string.edit_dates_paste)) },
        placeholder = { Text("12.01.2027, 26.01.2027, …") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = onApplyPaste, enabled = DateListParser.parse(pasteText).isNotEmpty()) {
        val count = DateListParser.parse(pasteText).size
        Text(
            if (count == 0) stringResource(R.string.edit_dates_paste_apply_empty)
            else pluralStringResource(R.plurals.edit_dates_paste_apply, count, count),
        )
    }
}

@Composable
private fun toleranceLabel(days: Int): String = when (days) {
    0 -> stringResource(R.string.edit_tolerance_none)
    7 -> stringResource(R.string.edit_tolerance_week)
    else -> pluralStringResource(R.plurals.interval_days, days, days)
}

private val TOLERANCE_OPTIONS = listOf(0, 1, 2, 3, 7)

private enum class MonthlyRule { DAY, NTH, LAST }

private val dateListSaver = listSaver<SnapshotStateList<Long>, Long>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

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
            // Only the chosen option is explained, so the list stays short.
            if (selected) {
                Text(stringResource(help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
    val toleranceDays: Int = 0,
    val symbol: ChoreSymbol? = null,
    val color: ChoreColor? = null,
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

/**
 * Quick create: catalogue suggestions matching what is typed (or the first few while the name is
 * empty). One tap fills name, schedule, room and checklist.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickSuggestions(query: String, existingNames: Set<String>, onPick: (ChoreTemplate) -> Unit) {
    val existing = existingNames.map { it.lowercase() }.toSet()
    val available = ChoreCatalog.templates.filter { it.name.lowercase() !in existing }
    val matches = if (query.isBlank()) {
        available.take(QUICK_SUGGESTIONS)
    } else {
        available.filter { it.name.contains(query.trim(), ignoreCase = true) }.take(QUICK_SUGGESTIONS)
    }
    if (matches.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.edit_suggestions),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            matches.forEach { template ->
                SuggestionChip(
                    onClick = { onPick(template) },
                    label = { Text(template.name) },
                    icon = { Icon(ChoreSymbol.guess(template.name).icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}

private const val QUICK_SUGGESTIONS = 6

/** Current symbol with a button to change it (F1.5). */
@Composable
private fun SymbolRow(look: ChoreLook, automatic: Boolean, onChange: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ChoreAvatar(look, size = 48.dp)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.symbol), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(if (automatic) R.string.symbol_automatic else R.string.symbol_chosen),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onChange) { Text(stringResource(R.string.symbol_change)) }
    }
}

/** Pick a symbol and a colour, or go back to the automatic one (F1.5). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymbolPickerDialog(
    current: ChoreLook,
    onDismiss: () -> Unit,
    onAutomatic: () -> Unit,
    onPick: (ChoreLook) -> Unit,
) {
    var symbol by rememberSaveable { mutableStateOf(current.symbol) }
    var color by rememberSaveable { mutableStateOf(current.color) }
    val symbolNames = ChoreSymbol.entries.associateWith { stringResource(it.label) }
    val colorNames = ChoreColor.entries.associateWith { stringResource(it.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.symbol_choose)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoreSymbol.entries.forEach { option ->
                        val selected = option == symbol
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .selectable(selected = selected, role = Role.RadioButton, onClick = {
                                    // A new symbol brings its own colour, unless one was picked explicitly before.
                                    if (color == symbol.defaultColor) color = option.defaultColor
                                    symbol = option
                                })
                                .padding(4.dp)
                                .semantics { contentDescription = symbolNames.getValue(option) },
                        ) { ChoreAvatar(ChoreLook(option, color), size = 44.dp) }
                    }
                }
                Text(stringResource(R.string.symbol_color), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoreColor.entries.forEach { option ->
                        val selected = option == color
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(option.colors().container)
                                .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .selectable(selected = selected, role = Role.RadioButton, onClick = { color = option })
                                .semantics { contentDescription = colorNames.getValue(option) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(ChoreLook(symbol, color)) }) { Text(stringResource(R.string.symbol_apply)) } },
        dismissButton = { TextButton(onClick = onAutomatic) { Text(stringResource(R.string.symbol_reset)) } },
    )
}
