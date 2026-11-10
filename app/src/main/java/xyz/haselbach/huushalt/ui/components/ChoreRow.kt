package xyz.haselbach.huushalt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
    /** Swipe left: postpone to tomorrow. Null: no swipe to the left. */
    onPostpone: (() -> Unit)? = null,
) {
    val status = chore.status(today)
    val overdue = status is DueStatus.Overdue
    val dueNow = overdue || chore.isDueNow(today)
    val due = dueLabel(chore, today).takeIf { showDueToday || status != DueStatus.Today }
    val checklist = if (chore.checklistTotal > 0) {
        stringResource(R.string.checklist_progress, chore.checklistDone, chore.checklistTotal)
    } else {
        null
    }
    val parts = listOfNotNull(chore.areaName.takeIf { showArea }, due, checklist)
    val errorColor = MaterialTheme.colorScheme.error
    val canComplete = !chore.isFinished && !chore.paused
    val haptics = LocalHapticFeedback.current
    val complete = {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onComplete()
    }
    val completeLabel = stringResource(R.string.action_complete_description, chore.name)
    val postponeLabel = stringResource(R.string.action_postpone_tomorrow_description, chore.name)
    val row = @Composable {
        ListItem(
            modifier = Modifier
                .clickable(onClick = onClick)
                .semantics {
                    // Swiping is a shortcut; TalkBack users get the same as actions.
                    customActions = listOfNotNull(
                        CustomAccessibilityAction(completeLabel) { complete(); true }.takeIf { canComplete },
                        onPostpone?.let { postpone -> CustomAccessibilityAction(postponeLabel) { postpone(); true } },
                    )
                },
            leadingContent = { ChoreAvatar(chore.look, Modifier.alpha(if (chore.paused || chore.isFinished) 0.5f else 1f)) },
            headlineContent = { Text(chore.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = if (parts.isEmpty()) null else {
                {
                    // e.g. "Küche · 2 Tage überfällig · 1/4"
                    Text(
                        buildAnnotatedString {
                            parts.forEachIndexed { index, part ->
                                // Lines only break inside a part, never at "·", so a short part
                                // like "0/4" never ends up alone on a line.
                                if (index > 0) append("\u00A0·\u00A0")
                                if (overdue && part == due) withStyle(SpanStyle(color = errorColor)) { append(part) } else append(part)
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            trailingContent = if (!canComplete) null else {
                {
                    // Due chores get a prominent button, upcoming ones a quiet one.
                    if (dueNow) {
                        FilledTonalIconButton(
                            onClick = complete,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = completeLabel)
                        }
                    } else {
                        OutlinedIconButton(
                            onClick = complete,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = completeLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
        )
    }
    if (!canComplete) {
        Box(modifier) { row() }
        return
    }
    // Swipe right: done; swipe left: tomorrow. The row snaps back, the list then updates.
    // The state keeps its callback for as long as the row is shown, so it must read the current
    // actions (the chore or "today" may have changed since), and act only once per swipe: the
    // callback can be asked more than once at the end of a gesture.
    val currentComplete by rememberUpdatedState(complete)
    val currentPostpone by rememberUpdatedState(onPostpone)
    var handled by remember { mutableStateOf(false) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.Settled) {
                handled = false
            } else if (!handled) {
                handled = true
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> currentComplete()
                    else -> currentPostpone?.invoke()
                }
            }
            false
        },
    )
    // Ready for the next swipe once the row is back in place.
    LaunchedEffect(state.currentValue, state.dismissDirection) {
        if (state.dismissDirection == SwipeToDismissBoxValue.Settled) handled = false
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromEndToStart = onPostpone != null,
        backgroundContent = { SwipeBackground(state.dismissDirection) },
    ) { Surface { row() } }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val colors = MaterialTheme.colorScheme
    val (color, icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(colors.primaryContainer, Icons.Filled.Check, Alignment.CenterStart)
        SwipeToDismissBoxValue.EndToStart -> Triple(colors.tertiaryContainer, Icons.Filled.Event, Alignment.CenterEnd)
        SwipeToDismissBoxValue.Settled -> Triple(Color.Transparent, null, Alignment.Center)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (direction == SwipeToDismissBoxValue.StartToEnd) colors.onPrimaryContainer else colors.onTertiaryContainer,
            )
        }
    }
}

/** Extra space below a list so the floating "+" button doesn't cover the last row. */
fun PaddingValues.withFabSpace(): PaddingValues = PaddingValues(
    start = calculateStartPadding(LayoutDirection.Ltr),
    top = calculateTopPadding(),
    end = calculateEndPadding(LayoutDirection.Ltr),
    bottom = calculateBottomPadding() + 88.dp,
)
