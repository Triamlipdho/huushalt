package xyz.haselbach.huushalt.ui.areas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Area

/**
 * Add, rename and delete rooms (F1.4). Names are typed directly on the page, not in dialogs, so
 * the keyboard never covers a dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasScreen(
    areas: List<Area>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var newName by rememberSaveable { mutableStateOf("") }
    var renamingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun add() {
        val name = newName.trim()
        if (name.isEmpty() || areas.any { it.name.equals(name, ignoreCase = true) }) return
        onAdd(name)
        newName = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.areas_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "add") {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.area_new_title)) },
                        placeholder = { Text(stringResource(R.string.area_name_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { add() }),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = ::add, enabled = newName.isNotBlank()) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.area_add))
                    }
                }
            }
            if (areas.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.areas_empty),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(areas, key = { it.id }) { area ->
                if (renamingId == area.id) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            singleLine = true,
                            label = { Text(stringResource(R.string.area_name)) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            enabled = renameText.isNotBlank(),
                            onClick = {
                                onRename(area.id, renameText)
                                renamingId = null
                            },
                        ) { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save)) }
                        IconButton(onClick = { renamingId = null }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                        }
                    }
                } else {
                    ListItem(
                        headlineContent = { Text(area.name) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    renamingId = area.id
                                    renameText = area.name
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.area_rename_description, area.name))
                                }
                                IconButton(onClick = { deletingId = area.id }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.area_delete_description, area.name))
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    areas.find { it.id == deletingId }?.let { area ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(stringResource(R.string.area_delete_title, area.name)) },
            text = { Text(stringResource(R.string.area_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    deletingId = null
                    onDelete(area.id)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
