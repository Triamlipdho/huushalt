package xyz.haselbach.huushalt.ui.progress

import xyz.haselbach.huushalt.ui.theme.Spacing
import xyz.haselbach.huushalt.ui.theme.Sizes
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.haselbach.huushalt.R
import xyz.haselbach.huushalt.domain.Area
import xyz.haselbach.huushalt.domain.AreaCount
import xyz.haselbach.huushalt.domain.Badge
import xyz.haselbach.huushalt.domain.BadgeFamily
import xyz.haselbach.huushalt.domain.Badges
import xyz.haselbach.huushalt.ui.components.SectionHeader

private fun badgeTitleRes(id: String) = when (id) {
    "done_1" -> R.string.badge_done_1
    "done_10" -> R.string.badge_done_10
    "done_50" -> R.string.badge_done_50
    "done_100" -> R.string.badge_done_100
    "done_250" -> R.string.badge_done_250
    "done_500" -> R.string.badge_done_500
    "streak_7" -> R.string.badge_streak_7
    "streak_28" -> R.string.badge_streak_28
    "streak_100" -> R.string.badge_streak_100
    else -> R.string.badge_chore_streak_10
}

private fun badgeDescriptionRes(family: BadgeFamily) = when (family) {
    BadgeFamily.DONE -> R.plurals.badge_done_description
    BadgeFamily.STREAK -> R.plurals.badge_streak_description
    BadgeFamily.CHORE_STREAK -> R.plurals.badge_chore_streak_description
}

private fun Resources.badgeTitle(badge: Badge) = getString(badgeTitleRes(badge.id))

@Composable
private fun badgeDescription(badge: Badge) =
    pluralStringResource(badgeDescriptionRes(badge.family), badge.target, badge.target)

/** Earned badges in the warm accent, then the next one of each kind, greyed with progress (F7.7). */
@Composable
fun BadgesSection(badges: List<Badge>) {
    val earned = badges.filter { it.earned }
    val next = Badges.next(badges)
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.badges_title))
        if (earned.isEmpty()) {
            // An invitation, not a zero.
            Text(
                stringResource(R.string.badges_none_yet),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BadgeGrid(earned)
        if (next.isNotEmpty()) {
            Text(
                stringResource(R.string.badges_next),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BadgeGrid(next)
        }
    }
}

/** Two columns; a single card in the last row stays at half width. */
@Composable
private fun BadgeGrid(badges: List<Badge>) {
    if (badges.isEmpty()) return
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        badges.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { BadgeCard(it, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: Badge, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val title = stringResource(badgeTitleRes(badge.id))
    val description = badgeDescription(badge)
    val progress = stringResource(R.string.badge_progress, badge.progress, badge.target)
    val spoken = if (badge.earned) {
        stringResource(R.string.badge_earned_description, title, description)
    } else {
        stringResource(R.string.badge_next_description, title, description, progress)
    }
    Card(
        modifier.clearAndSetSemantics { contentDescription = spoken },
        colors = CardDefaults.cardColors(
            containerColor = if (badge.earned) colors.tertiaryContainer else colors.surfaceContainerLow,
            contentColor = if (badge.earned) colors.onTertiaryContainer else colors.onSurfaceVariant,
        ),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                badge.emoji,
                style = MaterialTheme.typography.headlineSmall,
                modifier = if (badge.earned) Modifier else Modifier.alpha(0.4f),
            )
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(description, style = MaterialTheme.typography.bodySmall)
            if (!badge.earned) {
                LinearProgressIndicator(
                    progress = { badge.progress.toFloat() / badge.target },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    color = colors.outline,
                    trackColor = colors.surfaceContainerHighest,
                    drawStopIndicator = {},
                )
                Text(progress, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Completions per room in the last 30 days, as horizontal bars (F7.8). Statistics, not motivation,
 * so it stays when motivation is turned off.
 */
@Composable
fun AreaStatsSection(counts: List<AreaCount>, areas: List<Area>) {
    val names = areas.associate { it.id to it.name }
    val noRoom = stringResource(R.string.area_stats_none)
    val max = counts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.area_stats_title))
        Text(
            stringResource(R.string.area_stats_note),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            counts.forEach { entry ->
                val name = entry.areaId?.let { names[it] } ?: noRoom
                val spoken = pluralStringResource(R.plurals.area_stats_description, entry.count, name, entry.count)
                Row(
                    Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        name,
                        modifier = Modifier.weight(0.35f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(Modifier.weight(0.65f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Empty rooms get just a sliver, like empty weeks in the chart.
                            Box(
                                Modifier
                                    .fillMaxWidth(0.85f * entry.count / max + 0.01f)
                                    .height(Sizes.bar)
                                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(entry.count.toString(), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

/** Gives the "erledigt – Rückgängig" snackbar time to come first; the badge then waits for it to end. */
private const val ANNOUNCE_DELAY_MS = 700L

/** Time for an undo tapped on that snackbar to reach the database and the badges. */
private const val SETTLE_DELAY_MS = 1_000L

/**
 * Announces newly earned badges once with a snackbar (F7.7). [announced] is what was announced
 * before (null: never checked, then everything earned so far is remembered silently);
 * [onRemember] adds ids to it. Without motivation, badges are remembered without a snackbar.
 */
@Composable
fun AnnounceNewBadges(
    badges: List<Badge>,
    announced: Set<String>?,
    motivationEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    /** Removes ids again: the completion that earned them was undone before they were shown. */
    onForget: (Set<String>) -> Unit = {},
    onRemember: (Set<String>) -> Unit,
) {
    // Not the effect's scope: the effect restarts once the ids are saved, the snackbar must stay.
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val motivation by rememberUpdatedState(motivationEnabled)
    val save by rememberUpdatedState(onRemember)
    val forget by rememberUpdatedState(onForget)
    val current by rememberUpdatedState(badges)
    // Saving takes a moment; this keeps a badge from being announced twice meanwhile.
    val shown = remember { HashSet<String>() }
    val earnedIds = badges.filter { it.earned }.map { it.id }
    LaunchedEffect(earnedIds, announced) {
        val announcement = Badges.toAnnounce(badges, announced?.plus(shown))
        val new = announcement.new
        if (announcement.remember == announced) return@LaunchedEffect
        shown += announcement.remember
        save(announcement.remember)
        if (!motivation || new.isEmpty()) return@LaunchedEffect
        scope.launch {
            // After the "erledigt" snackbar, and only if that wasn't undone meanwhile. An undo
            // tapped just now takes a moment to reach the badges, hence the second wait.
            delay(ANNOUNCE_DELAY_MS)
            snapshotFlow { snackbarHostState.currentSnackbarData }.first { it == null }
            delay(SETTLE_DELAY_MS)
            val stillEarned = current.filter { it.earned }.map { it.id }.toSet()
            val (keep, gone) = new.partition { it.id in stillEarned }
            if (gone.isNotEmpty()) {
                // Earned again later, they are announced then.
                val ids = gone.map { it.id }.toSet()
                shown -= ids
                forget(ids)
            }
            if (keep.isEmpty()) return@launch
            val message = if (keep.size == 1) {
                resources.getString(R.string.snackbar_new_badge, resources.badgeTitle(keep.single()))
            } else {
                resources.getQuantityString(R.plurals.snackbar_new_badges, keep.size, keep.size)
            }
            snackbarHostState.showSnackbar(message)
        }
    }
}
