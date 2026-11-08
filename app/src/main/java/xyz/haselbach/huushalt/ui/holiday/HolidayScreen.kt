package xyz.haselbach.huushalt.ui.holiday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Holiday
import xyz.haselbach.huushalt.ui.components.LocalDatePickerDialog
import xyz.haselbach.huushalt.ui.formatLong

private enum class Field { START, END }

/** Plan, change, end or delete the holiday (F10.1, F10.4). Only one holiday can be planned at a time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayScreen(
    holiday: Holiday?,
    today: LocalDate,
    onClose: () -> Unit,
    onSave: (start: LocalDate, end: LocalDate) -> Unit,
    onEnd: () -> Unit,
    onDelete: () -> Unit,
) {
    val active = holiday?.isActive(today) == true
    var startDay by rememberSaveable { mutableLongStateOf((holiday?.start ?: today).toEpochDay()) }
    var endDay by rememberSaveable { mutableLongStateOf((holiday?.end ?: today.plusWeeks(1)).toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf<Field?>(null) }
    val start = LocalDate.ofEpochDay(startDay)
    val end = LocalDate.ofEpochDay(endDay)
    val valid = end >= start

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.holiday_mode)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    TextButton(enabled = valid, onClick = { onSave(start, end) }) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.holiday_from), style = MaterialTheme.typography.titleSmall)
            OutlinedButton(enabled = !active, onClick = { picking = Field.START }) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Text(start.formatLong(), modifier = Modifier.padding(start = 8.dp))
            }
            Text(stringResource(R.string.holiday_until), style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = { picking = Field.END }) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Text(end.formatLong(), modifier = Modifier.padding(start = 8.dp))
            }
            if (!valid) Text(stringResource(R.string.holiday_error_order), color = MaterialTheme.colorScheme.error)
            Text(
                stringResource(R.string.holiday_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (holiday != null) {
                if (active) {
                    OutlinedButton(onClick = onEnd) { Text(stringResource(R.string.holiday_end)) }
                } else {
                    OutlinedButton(onClick = onDelete) { Text(stringResource(R.string.holiday_delete)) }
                }
            }
        }
    }

    when (picking) {
        Field.START -> LocalDatePickerDialog(
            initial = start,
            earliest = today,
            onDismiss = { picking = null },
            onConfirm = {
                startDay = it.toEpochDay()
                if (end < it) endDay = it.toEpochDay()
                picking = null
            },
        )
        Field.END -> LocalDatePickerDialog(
            initial = end,
            earliest = maxOf(start, today),
            onDismiss = { picking = null },
            onConfirm = {
                endDay = it.toEpochDay()
                picking = null
            },
        )
        null -> Unit
    }
}
