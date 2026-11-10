package xyz.haselbach.huushalt.ui.components

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Chore
import xyz.haselbach.huushalt.domain.IntervalSuggestion
import xyz.haselbach.huushalt.domain.RepeatUnit

/** "alle 3 Wochen", "jeden Monat" (F2.13). */
@Composable
@ReadOnlyComposable
fun everyLabel(interval: Int, unit: RepeatUnit): String = pluralStringResource(everyPlural(unit), interval, interval)

/** [everyLabel] outside of composition, e.g. for a snackbar. */
fun Resources.everyLabel(interval: Int, unit: RepeatUnit): String = getQuantityString(everyPlural(unit), interval, interval)

private fun everyPlural(unit: RepeatUnit): Int = when (unit) {
    RepeatUnit.DAYS -> R.plurals.suggestion_every_days
    RepeatUnit.WEEKS -> R.plurals.suggestion_every_weeks
    else -> R.plurals.suggestion_every_months
}

/**
 * On the detail screen: the chore is done more or less often than planned, change the interval?
 * Friendly, with an easy "Nein danke" (F2.13).
 */
@Composable
fun IntervalSuggestionCard(
    chore: Chore,
    suggestion: IntervalSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = colors.tertiary)
                Text(stringResource(R.string.suggestion_title), style = MaterialTheme.typography.titleSmall)
            }
            Text(
                stringResource(
                    R.string.suggestion_body,
                    everyLabel(suggestion.interval, suggestion.unit),
                    everyLabel(chore.schedule.interval, chore.schedule.unit),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.onTertiaryContainer),
                ) { Text(stringResource(R.string.suggestion_dismiss)) }
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary, contentColor = colors.onTertiary),
                ) { Text(stringResource(R.string.suggestion_accept)) }
            }
        }
    }
}

/**
 * A small, calm hint on the home screen: "Vorschlag für «Kühlschrank putzen»: alle 3 Wochen",
 * plus how many more there are. Tapping it opens the chore (F2.13).
 */
@Composable
fun IntervalSuggestionHint(chore: Chore, suggestion: IntervalSuggestion, more: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow, contentColor = colors.onSurface),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = colors.tertiary)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.suggestion_home, chore.name, everyLabel(suggestion.interval, suggestion.unit)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (more > 0) {
                    Text(
                        pluralStringResource(R.plurals.suggestion_home_more, more, more),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }
}
