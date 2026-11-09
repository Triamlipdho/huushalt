package xyz.haselbach.huushalt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.DueStatus
import xyz.haselbach.huushalt.ui.dueLabel

/**
 * One chore in a list: name, room, when it is due, checklist progress and a button to mark it
 * done (F4.1, F4.7). Only the due part is red when overdue.
 *
 * @param showArea false where the list is already grouped by room
 * @param showDueToday false in the "Heute fällig" section, where "Heute fällig" would repeat itself
 */
@Composable
fun ChoreRow(
    chore: Chore,
    today: LocalDate,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    showArea: Boolean = true,
    showDueToday: Boolean = true,
) {
    val status = chore.status(today)
    val overdue = status is DueStatus.Overdue
    val due = dueLabel(chore, today).takeIf { showDueToday || status != DueStatus.Today }
    val checklist = if (chore.checklistTotal > 0) {
        stringResource(R.string.checklist_progress, chore.checklistDone, chore.checklistTotal)
    } else {
        null
    }
    val parts = listOfNotNull(chore.areaName.takeIf { showArea }, due, checklist)
    val errorColor = MaterialTheme.colorScheme.error
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { ChoreAvatar(chore.look, Modifier.alpha(if (chore.paused || chore.isFinished) 0.5f else 1f)) },
        headlineContent = { Text(chore.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (parts.isEmpty()) null else {
            {
                // e.g. "Küche · 2 Tage überfällig · 1/4"
                Text(
                    buildAnnotatedString {
                        parts.forEachIndexed { index, part ->
                            if (index > 0) append(" · ")
                            if (overdue && part == due) withStyle(SpanStyle(color = errorColor)) { append(part) } else append(part)
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = if (chore.isFinished || chore.paused) null else {
            {
                OutlinedIconButton(onClick = onComplete) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_complete_description, chore.name))
                }
            }
        },
    )
}

/** Extra space below a list so the floating "+" button doesn't cover the last row. */
fun PaddingValues.withFabSpace(): PaddingValues = PaddingValues(
    start = calculateStartPadding(LayoutDirection.Ltr),
    top = calculateTopPadding(),
    end = calculateEndPadding(LayoutDirection.Ltr),
    bottom = calculateBottomPadding() + 88.dp,
)
