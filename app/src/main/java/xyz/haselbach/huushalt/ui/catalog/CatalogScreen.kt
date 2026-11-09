package xyz.haselbach.huushalt.ui.catalog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.ChoreCatalog
import xyz.haselbach.huushalt.domain.ChoreLook
import xyz.haselbach.huushalt.domain.ChoreTemplate
import xyz.haselbach.huushalt.ui.components.ChoreAvatar
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.scheduleLabel

/**
 * Pick common chores to start with (F8.2). Chores that already exist (same name) are shown but
 * can't be picked again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    existingNames: Set<String>,
    onClose: () -> Unit,
    onAdd: (List<ChoreTemplate>) -> Unit,
) {
    val selected = rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver(
        save = { it.toList() },
        restore = { mutableStateListOf<String>().apply { addAll(it) } },
    )) { mutableStateListOf<String>() }
    val wholeHome = stringResource(R.string.catalog_whole_home)
    val groups = ChoreCatalog.templates.groupBy { it.area }
    val existing = existingNames.map { it.lowercase() }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    enabled = selected.isNotEmpty(),
                    onClick = { onAdd(ChoreCatalog.templates.filter { it.name in selected }) },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                ) {
                    Text(
                        if (selected.isEmpty()) stringResource(R.string.catalog_add_none)
                        else pluralStringResource(R.plurals.catalog_add, selected.size, selected.size),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.catalog_intro),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            groups.forEach { (area, templates) ->
                item(key = "area-$area") { SectionHeader(area ?: wholeHome) }
                items(templates, key = { it.name }) { template ->
                    val exists = template.name.lowercase() in existing
                    val checked = template.name in selected
                    ListItem(
                        modifier = Modifier.toggleable(
                            value = checked,
                            enabled = !exists,
                            role = Role.Checkbox,
                            onValueChange = { if (it) selected.add(template.name) else selected.remove(template.name) },
                        ),
                        leadingContent = { ChoreAvatar(ChoreLook.of(template.name, null, null), Modifier.alpha(if (exists) 0.5f else 1f)) },
                        trailingContent = { Checkbox(checked = checked || exists, onCheckedChange = null, enabled = !exists) },
                        headlineContent = { Text(template.name) },
                        supportingContent = {
                            Text(if (exists) stringResource(R.string.catalog_exists) else scheduleLabel(template.schedule))
                        },
                    )
                }
            }
        }
    }
}
