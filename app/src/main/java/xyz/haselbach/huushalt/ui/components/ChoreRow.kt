package xyz.haselbach.huushalt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import java.time.LocalDate
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.DueStatus
import xyz.haselbach.huushalt.ui.dueLabel

/** One chore in a list: name, when it is due and a button to mark it done (F4.1, F4.7). */
@Composable
fun ChoreRow(
    chore: Chore,
    today: LocalDate,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overdue = chore.status(today) is DueStatus.Overdue
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(chore.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                dueLabel(chore, today),
                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = if (chore.isFinished) null else {
            {
                OutlinedIconButton(onClick = onComplete) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_complete_description, chore.name))
                }
            }
        },
    )
}
