package xyz.haselbach.huushalt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.haselbach.huushalt.ui.theme.Spacing

/** What a snackbar shows; [icon] says at a glance what happened (done, postponed, deleted, ...). */
class IconSnackbarVisuals(
    override val message: String,
    val icon: ImageVector? = null,
    override val actionLabel: String? = null,
    override val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
    override val withDismissAction: Boolean = false,
) : SnackbarVisuals

/** Shows a snackbar with an icon; returns whether its action was used. */
suspend fun SnackbarHostState.show(
    message: String,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
): SnackbarResult = showSnackbar(IconSnackbarVisuals(message, icon, actionLabel, duration))

/**
 * huushalt's snackbar host: a floating, rounded snackbar with an icon, that can be swiped away
 * to either side. SnackbarHost itself announces it to TalkBack and offers "dismiss" there.
 */
@Composable
fun HuushaltSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        // A fresh swipe state per snackbar.
        key(data) {
            val swipe = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value != SwipeToDismissBoxValue.Settled) data.dismiss()
                    true
                },
            )
            SwipeToDismissBox(state = swipe, backgroundContent = {}) { HuushaltSnackbar(data) }
        }
    }
}

@Composable
fun HuushaltSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val icon = (data.visuals as? IconSnackbarVisuals)?.icon
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        shape = MaterialTheme.shapes.large,
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier
                .heightIn(min = 56.dp)
                .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (icon != null) {
                Box(
                    Modifier.size(32.dp).background(colors.inversePrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = colors.inverseSurface, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            data.visuals.actionLabel?.let { label ->
                TextButton(
                    onClick = data::performAction,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.inversePrimary),
                ) { Text(label, style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
