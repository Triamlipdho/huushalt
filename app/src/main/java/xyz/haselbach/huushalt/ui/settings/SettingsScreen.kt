package xyz.haselbach.huushalt.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.settings.AppSettings
import xyz.haselbach.huushalt.settings.ReminderSettings
import xyz.haselbach.huushalt.ui.components.SectionHeader

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
) {
    val reminders = settings.reminders
    var editing by rememberSaveable { mutableStateOf<TimeField?>(null) }

    Scaffold(
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
            Text(
                stringResource(R.string.settings_timing_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
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
