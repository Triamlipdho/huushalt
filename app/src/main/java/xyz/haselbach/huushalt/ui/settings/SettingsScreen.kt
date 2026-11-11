package xyz.haselbach.huushalt.ui.settings

import xyz.haselbach.huushalt.ui.components.HuushaltSnackbarHost
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Holiday
import xyz.haselbach.huushalt.reminders.ExternalTriggerReceiver
import xyz.haselbach.huushalt.settings.AppSettings
import xyz.haselbach.huushalt.settings.ReminderSettings
import xyz.haselbach.huushalt.settings.ThemeMode
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.formatShort

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

private enum class TimeField { MORNING, EVENING }

/** Reminder settings (F5.1, F5.2, F5.7) and notification permission status (F8.3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    access: NotificationAccess,
    onBack: () -> Unit,
    onChange: ((ReminderSettings) -> ReminderSettings) -> Unit,
    onMotivationChange: (Boolean) -> Unit,
    /** Current or next holiday, shown in the "Ferien" section (F10.1). */
    holiday: Holiday?,
    onOpenHoliday: () -> Unit,
    onOpenAreas: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onFirstDayChange: (DayOfWeek) -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    onDeleteAll: () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
) {
    var choosing by rememberSaveable { mutableStateOf<Choice?>(null) }
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }
    val reminders = settings.reminders
    var editing by rememberSaveable { mutableStateOf<TimeField?>(null) }

    Scaffold(
        snackbarHost = { snackbarHostState?.let { HuushaltSnackbarHost(it) } },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_reminders))
            ListItem(
                modifier = Modifier.toggleable(
                    value = reminders.enabled,
                    role = Role.Switch,
                    onValueChange = { on -> onChange { it.copy(enabled = on) } },
                ),
                headlineContent = { Text(stringResource(R.string.settings_reminders)) },
                supportingContent = { Text(stringResource(R.string.settings_reminders_summary)) },
                trailingContent = { Switch(checked = reminders.enabled, onCheckedChange = null) },
            )
            if (reminders.enabled && !access.canNotify) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.settings_notifications_blocked), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = access.request, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(if (access.opensSettings) R.string.permission_open_settings else R.string.permission_allow))
                    }
                }
            }
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable(enabled = reminders.enabled) { editing = TimeField.MORNING },
                headlineContent = { Text(stringResource(R.string.settings_morning)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_morning_summary, reminders.morningTime.format(timeFormat)))
                },
            )
            ListItem(
                modifier = Modifier.clickable(enabled = reminders.enabled && reminders.eveningEnabled) {
                    editing = TimeField.EVENING
                },
                headlineContent = { Text(stringResource(R.string.settings_evening)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_evening_summary, reminders.eveningTime.format(timeFormat)))
                },
                trailingContent = {
                    Switch(
                        checked = reminders.eveningEnabled,
                        enabled = reminders.enabled,
                        onCheckedChange = { on -> onChange { it.copy(eveningEnabled = on) } },
                    )
                },
            )
            ListItem(
                modifier = Modifier.toggleable(
                    value = reminders.externalTriggerEnabled,
                    enabled = reminders.enabled,
                    role = Role.Switch,
                    onValueChange = { on -> onChange { it.copy(externalTriggerEnabled = on) } },
                ),
                headlineContent = { Text(stringResource(R.string.settings_external_trigger)) },
                supportingContent = { Text(stringResource(R.string.settings_external_trigger_summary)) },
                trailingContent = {
                    Switch(checked = reminders.externalTriggerEnabled, enabled = reminders.enabled, onCheckedChange = null)
                },
            )
            if (reminders.enabled && reminders.externalTriggerEnabled) {
                SelectionContainer {
                    Text(
                        stringResource(
                            R.string.settings_external_trigger_details,
                            LocalContext.current.packageName,
                            ExternalTriggerReceiver.ACTION_SHOW_TODAY,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            Text(
                stringResource(R.string.settings_timing_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_display))
            ListItem(
                modifier = Modifier.clickable { choosing = Choice.THEME },
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = { Text(stringResource(themeLabel(settings.theme))) },
            )
            // Colours from the wallpaper exist from Android 12 on.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    modifier = Modifier.toggleable(
                        value = settings.dynamicColor,
                        role = Role.Switch,
                        onValueChange = onDynamicColorChange,
                    ),
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_color_summary)) },
                    trailingContent = { Switch(checked = settings.dynamicColor, onCheckedChange = null) },
                )
            }
            ListItem(
                modifier = Modifier.clickable { choosing = Choice.FIRST_DAY },
                headlineContent = { Text(stringResource(R.string.settings_first_day)) },
                supportingContent = {
                    Text(stringArrayResource(R.array.weekdays_long)[settings.firstDayOfWeek.value - 1])
                },
            )
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_chores))
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenCatalog),
                headlineContent = { Text(stringResource(R.string.catalog_title)) },
                supportingContent = { Text(stringResource(R.string.catalog_settings_summary)) },
            )
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenAreas),
                headlineContent = { Text(stringResource(R.string.areas_title)) },
                supportingContent = { Text(stringResource(R.string.areas_summary)) },
            )
            HorizontalDivider()
            SectionHeader(stringResource(R.string.holiday_title))
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenHoliday),
                headlineContent = { Text(stringResource(R.string.holiday_mode)) },
                supportingContent = {
                    Text(
                        if (holiday == null) {
                            stringResource(R.string.holiday_none)
                        } else {
                            stringResource(R.string.holiday_period, holiday.start.formatShort(), holiday.end.formatShort())
                        },
                    )
                },
            )
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_motivation))
            ListItem(
                modifier = Modifier.toggleable(
                    value = settings.motivationEnabled,
                    role = Role.Switch,
                    onValueChange = onMotivationChange,
                ),
                headlineContent = { Text(stringResource(R.string.settings_motivation_switch)) },
                supportingContent = { Text(stringResource(R.string.settings_motivation_summary)) },
                trailingContent = { Switch(checked = settings.motivationEnabled, onCheckedChange = null) },
            )
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_data))
            ListItem(
                modifier = Modifier.clickable(onClick = onExport),
                headlineContent = { Text(stringResource(R.string.data_export)) },
                supportingContent = { Text(stringResource(R.string.data_export_summary)) },
            )
            ListItem(
                modifier = Modifier.clickable(onClick = onImport),
                headlineContent = { Text(stringResource(R.string.data_import)) },
                supportingContent = { Text(stringResource(R.string.data_import_summary)) },
            )
            ListItem(
                modifier = Modifier.clickable { confirmDeleteAll = true },
                headlineContent = { Text(stringResource(R.string.data_delete_all), color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text(stringResource(R.string.data_delete_all_summary)) },
            )
        }
    }

    when (choosing) {
        Choice.THEME -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to stringResource(themeLabel(it)) },
            selected = settings.theme,
            onDismiss = { choosing = null },
            onSelect = {
                choosing = null
                onThemeChange(it)
            },
        )
        Choice.FIRST_DAY -> {
            val names = stringArrayResource(R.array.weekdays_long)
            ChoiceDialog(
                title = stringResource(R.string.settings_first_day),
                options = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY).map { it to names[it.value - 1] },
                selected = settings.firstDayOfWeek,
                onDismiss = { choosing = null },
                onSelect = {
                    choosing = null
                    onFirstDayChange(it)
                },
            )
        }
        null -> Unit
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(stringResource(R.string.data_delete_all_title)) },
            text = { Text(stringResource(R.string.data_delete_all_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    onDeleteAll()
                }) { Text(stringResource(R.string.data_delete_all_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    editing?.let { field ->
        TimePickerDialog(
            initial = if (field == TimeField.MORNING) reminders.morningTime else reminders.eveningTime,
            onDismiss = { editing = null },
            onConfirm = { time ->
                editing = null
                onChange { if (field == TimeField.MORNING) it.copy(morningTime = time) else it.copy(eveningTime = time) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_pick_time)) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private enum class Choice { THEME, FIRST_DAY }

private fun themeLabel(theme: ThemeMode) = when (theme) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

/** A single choice from a short list, as radio buttons in a dialog. */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
