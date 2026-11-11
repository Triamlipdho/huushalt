package xyz.haselbach.huushalt.ui.progress

import xyz.haselbach.huushalt.domain.WeekCount
import xyz.haselbach.huushalt.domain.WeekForecast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import xyz.haselbach.huushalt.ui.theme.Spacing
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
import androidx.compose.material3.CardDefaults
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
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Area
import xyz.haselbach.huushalt.domain.Stats
import xyz.haselbach.huushalt.domain.Streaks
import xyz.haselbach.huushalt.ui.ProgressState
import xyz.haselbach.huushalt.ui.components.ChoreAvatar
import xyz.haselbach.huushalt.ui.components.SectionHeader
import xyz.haselbach.huushalt.ui.formatLong


/** Streaks, statistics, badges and everything done recently (F6.3, F7.2, F7.4, F7.7, F7.8). */
@Composable
fun ProgressScreen(
    state: ProgressState,
    motivationEnabled: Boolean,
    contentPadding: PaddingValues,
    onOpenChore: (Long) -> Unit,
    areas: List<Area> = emptyList(),
) {
    LazyColumn(contentPadding = contentPadding) {
        if (motivationEnabled) item(key = "streak") { StreakCard(state.streaks) }
        item(key = "stats") { StatsCards(state.stats) }
        item(key = "chart") { WeekChart(state.stats) }
        if (areas.isNotEmpty() && state.areaCounts.any { it.areaId != null }) {
            item(key = "areas") { AreaStatsSection(state.areaCounts, areas) }
        }
        if (motivationEnabled) item(key = "badges") { BadgesSection(state.badges) }
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
                leadingContent = { ChoreAvatar(entry.look) },
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
    // A running streak gets the warm accent; without one, the card stays quiet.
    val active = streaks.current > 0
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) colors.tertiaryContainer else colors.surfaceContainerLow,
            contentColor = if (active) colors.onTertiaryContainer else colors.onSurface,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.streak_title), style = MaterialTheme.typography.titleSmall)
            if (active) {
                Text("🔥 " + days(streaks.current), style = MaterialTheme.typography.headlineMedium)
            } else {
                // No guilt: a missing streak is an invitation, not a zero (F7).
                Text(stringResource(R.string.streak_none), style = MaterialTheme.typography.bodyLarge)
            }
            if (streaks.best > 0) {
                Text(stringResource(R.string.streak_best, days(streaks.best)), style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.streak_free_day_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatsCards(stats: Stats) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCard(stringResource(R.string.stats_last_7), stats.last7Days.toString(), stringResource(R.string.stats_previous_7, stats.previous7Days), Modifier.weight(1f))
        StatCard(stringResource(R.string.stats_last_30), stats.last30Days.toString(), stringResource(R.string.stats_previous_30, stats.previous30Days), Modifier.weight(1f))
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
    Card(
        modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(note, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Bar chart of the last 8 calendar weeks ("KW 46"), the current one highlighted by a terracotta
 * bar and a filled label, not by colour alone. Read out as a list for TalkBack (N6).
 */
@Composable
private fun WeekChart(stats: Stats) {
    val max = stats.weeks.maxOf { it.count }.coerceAtLeast(1)
    val current = stats.weeks.lastOrNull()?.weekStart
    val thisWeek = stringResource(R.string.stats_chart_this_week)
    val longWeekPattern = stringResource(R.string.calendar_week_long)
    val longWeek = { week: WeekCount -> longWeekPattern.format(WeekForecast.calendarWeek(week.weekStart)) }
    val description = stringResource(
        R.string.stats_chart_description,
        stats.weeks.joinToString(", ") { week ->
            val name = longWeek(week) + if (week.weekStart == current) " ($thisWeek)" else ""
            "$name: ${week.count}"
        },
    )
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Text(stringResource(R.string.stats_chart_title), style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = Spacing.sm)
                .clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            stats.weeks.forEach { week ->
                val isCurrent = week.weekStart == current
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    // Empty weeks get no "0" label, just the thin bar.
                    Text(
                        if (week.count > 0) week.count.toString() else "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else null,
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.8f)
                            .fillMaxHeight(0.7f * week.count / max + 0.02f)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (isCurrent) colors.tertiary else colors.primary),
                    )
                    Text(
                        stringResource(R.string.calendar_week, WeekForecast.calendarWeek(week.weekStart)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else null,
                        color = if (isCurrent) colors.onTertiaryContainer else colors.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .padding(top = Spacing.xs)
                            .then(
                                if (isCurrent) {
                                    Modifier.background(colors.tertiaryContainer, CircleShape).padding(horizontal = Spacing.xs)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}
