package xyz.haselbach.huushalt.ui.components

import androidx.compose.foundation.background
import xyz.haselbach.huushalt.ui.theme.Spacing
import xyz.haselbach.huushalt.ui.theme.Sizes
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Background for sticky headers, so rows scrolling underneath don't show through. */
@Composable
fun Modifier.stickyBackground(): Modifier = fillMaxWidth().background(MaterialTheme.colorScheme.background)

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = Spacing.xs)
            .semantics { heading() },
    )
}

/** A section header with an action on the right, e.g. "Überfällig … Neu planen", on one baseline. */
@Composable
fun SectionHeaderWithAction(text: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(end = Spacing.xs)) {
        SectionHeader(text, Modifier.weight(1f).alignByBaseline())
        Text(
            action,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .alignByBaseline()
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClick = onAction)
                // At least 48 dp tall, like every other touch target.
                .heightIn(min = Sizes.touchTarget)
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(horizontal = Spacing.md),
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = Spacing.sm)) { Text(actionLabel) }
        }
    }
}
