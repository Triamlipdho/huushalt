package xyz.haselbach.huushalt.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Stats
import xyz.haselbach.huushalt.domain.Streaks
import xyz.haselbach.huushalt.ui.ProgressState
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.formatLong

private val weekLabel = DateTimeFormatter.ofPattern("dd.MM.")

/** Streaks, statistics and everything done recently (F6.3, F7.2, F7.4). */
@Composable
fun ProgressScreen(
    state: ProgressState,
    motivationEnabled: Boolean,
    contentPadding: PaddingValues,
    onOpenChore: (Long) -> Unit,
) {
    LazyColumn(contentPadding = contentPadding) {
        if (motivationEnabled) item(key = "streak") { StreakCard(state.streaks) }
        item(key = "stats") { StatsCards(state.stats) }
        item(key = "chart") { WeekChart(state.stats) }
        item(key = "activity-header") { SectionHeader(stringResource(R.string.stats_activity)) }
        if (state.recent.isEmpty()) {
            item(key = "activity-empty") {
                Text(
                    stringResource(R.string.stats_activity_empty),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.recent, key = { it.completion.id }) { entry ->
            ListItem(
                modifier = Modifier.clickable { onOpenChore(entry.completion.choreId) },
                headlineContent = { Text(entry.choreName) },
                supportingContent = { Text(entry.completion.doneDate.formatLong()) },
            )
        }
    }
}

@Composable
private fun days(count: Int) = pluralStringResource(R.plurals.streak_days, count, count)

@Composable
private fun StreakCard(streaks: Streaks) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.streak_title), style = MaterialTheme.typography.titleSmall)
            if (streaks.current > 0) {
                Text("🔥 " + days(streaks.current), style = MaterialTheme.typography.headlineMedium)
            } else {
                // No guilt: a missing streak is an invitation, not a zero (F7).
                Text(stringResource(R.string.streak_none), style = MaterialTheme.typography.titleLarge)
            }
            if (streaks.best > 0) {
                Text(stringResource(R.string.streak_best, days(streaks.best)), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(R.string.streak_free_day_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsCards(stats: Stats) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCard(stringResource(R.string.stats_this_week), stats.thisWeek.toString(), stringResource(R.string.stats_last_week, stats.lastWeek), Modifier.weight(1f))
        StatCard(stringResource(R.string.stats_this_month), stats.thisMonth.toString(), stringResource(R.string.stats_last_month, stats.lastMonth), Modifier.weight(1f))
        StatCard(
            stringResource(R.string.stats_on_time),
            stats.onTimePercent?.let { stringResource(R.string.stats_on_time_value, it) } ?: stringResource(R.string.stats_no_value),
            stringResource(R.string.stats_on_time_note),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, note: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Simple bar chart of the last 8 weeks; read out as a list for TalkBack (N6). */
@Composable
private fun WeekChart(stats: Stats) {
    val max = stats.weeks.maxOf { it.count }.coerceAtLeast(1)
    val description = stringResource(
        R.string.stats_chart_description,
        stats.weeks.joinToString(", ") { "${it.weekStart.format(weekLabel)}: ${it.count}" },
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.stats_chart_title), style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(top = 8.dp)
                .clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            stats.weeks.forEach { week ->
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    // Empty weeks get no "0" label, just the thin bar.
                    Text(if (week.count > 0) week.count.toString() else "", style = MaterialTheme.typography.labelSmall)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.75f * week.count / max + 0.02f)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(week.weekStart.format(weekLabel), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
